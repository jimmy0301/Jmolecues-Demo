# Optimistic Locking and Concurrency

## Context

Aggregate 是一致性邊界，但多個 Command 仍可能同時修改同一個 Aggregate。
若沒有版本檢查，後寫入的 Command 可能覆蓋先寫入的狀態，造成 lost update。

## Pattern

對可被並發修改的 Aggregate 使用 optimistic locking：

```java
@Version
private Long version;
```

Command Handler 載入 Aggregate、呼叫 domain method、儲存。
若儲存時發生版本衝突，Application Service 依 use case 選擇：

- 重新載入後套用固定規則
- 回傳 conflict 錯誤
- 要求呼叫端重試
- 若 command 具備冪等語意，回傳既有結果

## Rules

- 不用跨 Aggregate 大交易掩蓋並發問題。
- Command 的冪等語意要和 optimistic locking 策略一致。
- 同一 Aggregate 的 concurrent command 要有測試。
- 版本欄位是 persistence concern，但它保護的是 Aggregate invariant。
- 悲觀鎖是例外手段，不是預設策略。
- 使用悲觀鎖前必須確認：高競爭、單一資料庫交易可控、鎖時間短、retry / compensation 不適合。
- 悲觀鎖必須定義 lock scope、timeout、deadlock handling、監控與壓力測試。

## Example

`PlaceOrderCommand` 若與 `CancelOrderCommand` 同時進來，其中一方成功後，另一方需要重新讀取最新狀態再依 domain rule 決定結果。

## Pessimistic Locking Example

悲觀鎖是少數高競爭 command 的例外策略。它的目的不是取代 Aggregate invariant，而是避免同一 Aggregate 的多個 command 同時進入最短寫入臨界區。

SQL/JPA 常見寫法：

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

MongoDB 沒有 SQL row lock。若專案真的需要悲觀鎖語意，可用 lock collection 做短租約：

```java
public record LockToken(String lockKey, String owner, Instant expiresAt) {}

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

Mongo lease lock 實作要點：

- lock key 以 Aggregate id 建唯一索引，例如 `ordering:order:{orderId}`
- acquire 必須使用原子 upsert / conditional update，只能取得不存在或已過期的 lock
- lock document 保存 owner token，release 時只能刪除同 owner 的 lock
- 每個 lock 必須有 `expiresAt`，避免程序中斷造成永久鎖
- 持鎖期間只做載入 Aggregate、呼叫 domain method、儲存；不可呼叫外部系統
- 測試需覆蓋 acquire 失敗、等待 timeout、TTL 過期後可重新取得、不能 release 別人的 lock
