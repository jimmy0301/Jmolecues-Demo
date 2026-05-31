# Chapter 1.2：一致性、並發與流程協調

本章說明 Aggregate 交易邊界、Domain Event、listener 冪等性、並發鎖策略，以及 Process Manager / Saga。
核心目標是判斷哪些狀態變更必須同步完成，哪些流程應交給事件或流程協調處理。

---

## 交易邊界與 Aggregate 邊界

一個 Command Handler 原則上只修改一個 Aggregate。
Aggregate 是一致性和交易的最小邊界；同一個交易中同時修改多個 Aggregate，通常代表邊界切錯，或需要改用事件驅動流程。

```java
// ✅ 正確：同步交易內只修改 Order
Order order = orderRepository.findById(command.orderId()).orElseThrow();
order.place();
orderRepository.save(order);

// ❌ 錯誤：一個 use case 直接修改多個 Context / Aggregate
order.place();
product.decreaseStock();
customer.addRewardPoints();
```

若下單後需要扣庫存、發通知、更新點數，應用 `OrderPlaced` 事件讓各自 owner Context 自己處理。
需要跨多步驟協調時，使用 Process Manager / Saga；若某步失敗，設計補償動作，而不是把所有資料鎖在同一個大交易裡。

## Domain Event vs Integration Event

Domain Event 是 Context 內部的領域事實；Integration Event / Public Event 是跨 Context 或跨系統的公開合約。

| 類型 | 用途 | 型別限制 |
|---|---|---|
| Domain Event | Context 內部的領域事實 | 可使用本 Context 的 ID / ValueObject，但仍需不可變 |
| Integration Event / Public Event | 跨 Context 或跨系統的公開合約 | 只使用穩定型別，如 `UUID`、`String`、`Instant`、`BigDecimal`、public DTO |

當事件會被其他 Context 消費時，就要把它當成公開合約管理，不要包含 owner Context 的 Aggregate、Entity、Repository、internal QueryModel 或 API DTO。

## Event Listener 規範

Event listener 是事件消費者，不是 Aggregate invariant 的保護者。
它可以觸發後續 use case、更新 projection、發通知或寫入整合資料，但要遵守以下規則：

- listener 必須可重入且具備冪等性：同一事件重送不應造成重複扣庫存、重複寄信或重複建立資料
- listener 不假設事件一定只來一次，也不假設跨 Aggregate / 跨 Context 事件一定按順序抵達
- listener 失敗時要能重試；若無法重試，需有補償或人工處理路徑
- 複雜流程不要塞在 listener 裡，抽成 Application Service、Process Manager 或 Saga
- listener 不直接修改非 owner Context 的資料庫

## 樂觀鎖與並發

Aggregate 是交易邊界，但仍可能同時收到多個 Command。
例如兩個請求同時對同一張訂單執行 `place()` / `cancel()`，或使用者連點造成多次 `PlaceOrderCommand`。
這時不能只靠 application service 的 if 判斷；持久化層也要能偵測並發衝突。

規則：

- 對可被並發修改的 Aggregate，加版本欄位或使用資料庫支援的 optimistic locking
- 並發衝突不自動吞掉；Application Service 要回傳可理解的錯誤、要求重試，或重新載入後套用明確規則
- Command 的冪等語意要和並發策略一致，例如「已成立再次 place 是成功」或「已成立再次 place 是固定錯誤」
- 預設不用悲觀鎖或跨 Aggregate 大交易來掩蓋邊界問題；先檢查 Aggregate 邊界是否正確
- 只有在高競爭、低延遲、單一資料庫交易可控，且重試 / 補償不適合時，才允許使用悲觀鎖
- 使用悲觀鎖必須寫明原因、鎖範圍、timeout、deadlock 處理與測試案例
- 並發行為要有測試，至少覆蓋同一 Aggregate 的重複 Command 或版本衝突處理

悲觀鎖範例只放在文件做教學用途；本專案實際程式碼仍以 optimistic locking 為預設。
若底層是 SQL/JPA，常見形式是用 repository 查詢時帶 lock：

```java
public interface OrderJpaRepository extends JpaRepository<Order, OrderId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(OrderId id);
}

@Transactional(timeout = 3)
public void placeWithPessimisticLock(PlaceOrderCommand command) {
    Order order = orderRepository.findByIdForUpdate(command.orderId())
            .orElseThrow(OrderNotFoundException::new);

    order.place();
    orderRepository.save(order);
}
```

若底層是 MongoDB，沒有 SQL row lock；可用短租約 lock document 示範「同一 Aggregate 同時間只允許一個 command 進入臨界區」：

```java
public interface PessimisticOrderLock {
    LockToken acquire(OrderId orderId, Duration waitTimeout, Duration leaseTtl);
    void release(LockToken token);
}

public void placeWithLeaseLock(PlaceOrderCommand command) {
    LockToken token = lock.acquire(command.orderId(), Duration.ofSeconds(2), Duration.ofSeconds(10));
    try {
        Order order = orderRepository.findById(command.orderId()).orElseThrow();
        order.place();
        orderRepository.save(order);
    } finally {
        lock.release(token);
    }
}
```

Mongo lease lock 必須有唯一 lock key、`expiresAt`、owner token，以及 `finally` release；測試要覆蓋 acquire 失敗、timeout、TTL 過期後可重新取得、release 不可釋放別人的 lock。

## Process Manager / Saga

當一個業務流程需要跨多個 Aggregate、Context 或外部系統，且流程本身有狀態，就不要把邏輯塞進 event listener。
這時使用 Process Manager / Saga。

Process Manager / Saga 的責任是協調流程，不是擁有別人的資料：

- 訂閱事件或接收 command，記錄流程狀態
- 根據目前狀態發出下一個 command 或呼叫 owner Context 的公開合約
- 處理 timeout、重試、失敗與補償
- 保存 correlation id / process id，讓同一流程可追蹤、可重送、可恢復
- 不直接修改其他 Context 的資料庫，不把多個 Aggregate 包進同一交易

命名上，只有真的保存流程狀態並協調多步驟時，才使用 `*ProcessManager` 或 `*Saga`。
單純聽到事件後做一件事的 class 仍是 listener，例如 `OrderPlacedListener`。

## 常見反模式

- 一個 Command Handler 在同一交易內修改多個 Aggregate 或多個 Context
- 用 listener 承載有狀態長流程，卻沒有 Process Manager / Saga 的狀態、重試與補償設計
- 忽略同一 Aggregate 的並發修改，沒有 optimistic locking、重試或明確錯誤策略
- listener 沒有冪等設計，事件重送時造成重複 side effect
- 用悲觀鎖包住外部 API 呼叫、跨 Context 長流程或使用者互動

---

相關 pattern：
[Aggregate Transaction Boundary](../patterns/aggregate-transaction-boundary.md)、
[Optimistic Locking and Concurrency](../patterns/optimistic-locking-concurrency.md)、
[Process Manager Saga](../patterns/process-manager-saga.md)。
