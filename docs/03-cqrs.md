# Chapter 3：CQRS — 命令與查詢分離

## 什麼是 CQRS？

CQRS（Command Query Responsibility Segregation）的核心原則：

> **改變狀態的操作（Command）與讀取狀態的操作（Query）要分開。**

這個原則源自 Bertrand Meyer 的 CQS（Command Query Separation）：
- **Command**：執行動作，改變系統狀態，不回傳值（或只回傳執行結果）
- **Query**：讀取資料，不改變任何狀態，可以安全重複呼叫

CQRS 把這兩條路徑提升為架構層面的分離。

---

## 為什麼需要 CQRS？

傳統 Service 常見的問題：

```java
// 讀寫混雜在同一個 Service，越長越難維護
public class OrderService {
    public Order createOrder(...) { ... }           // 寫
    public Order findById(...) { ... }              // 讀
    public Order placeOrder(...) { ... }            // 寫
    public List<Order> findByCustomer(...) { ... }  // 讀
    public void cancel(...) { ... }                 // 寫
}
```

問題：
- 看到一個方法，不確定它有沒有 side effect
- 讀的邏輯和寫的邏輯互相影響，難以個別優化
- 測試困難——測試讀的邏輯時，可能意外觸發寫

CQRS 讓這個問題消失：**看到 Command 就知道有 side effect，看到 Query 就知道安全。**

---

## 核心概念

### Command — 操作意圖

代表「系統應該做什麼」，用**祈使句**命名（`CreateOrder`、`PlaceOrder`、`CancelOrder`）。

- 必須不可變——代表某一時刻確定的意圖，不應被修改後重送
- 通常不回傳業務資料（或只回傳 ID / 執行狀態）
- 一個 Command 對應一個明確的業務操作

### Query — 讀取請求

代表「系統應該回傳什麼」，用**疑問式**或**讀取式**命名（`FindOrderById`、`GetCustomerOrders`）。

- 不改變任何狀態
- 可以安全重複呼叫，結果相同
- 可以針對讀取需求獨立優化（快取、索引、read model）

### Command Handler — 執行者

接收 Command，協調領域物件完成操作：

1. 從 Repository 取出相關的 Aggregate
2. 呼叫 Aggregate 上的業務方法
3. 儲存並發布 Domain Event

Handler 本身不含業務邏輯，業務邏輯在 Aggregate 裡。

---

## 執行流程

```mermaid
sequenceDiagram
    participant C as 呼叫端
    participant CMD as PlaceOrderCommand
    participant H as CommandHandler
    participant A as Order (Aggregate)
    participant R as Repository

    C->>CMD: new PlaceOrderCommand(orderId)
    C->>H: handle(command)
    H->>R: findById(orderId)
    R-->>H: order
    H->>A: order.place()
    Note over A: registerEvent(OrderPlaced)
    H->>R: save(order)
    Note over R: Spring Data 自動發布已登記的 event
    H-->>C: saved order
```

Handler 是薄薄的協調層，真正的業務規則（「PENDING 才能 place」）在 `Order.place()` 裡。`Order` 繼承 `AbstractAggregateRoot`，`place()` 用 `registerEvent()` 在 domain 內部登記事件；Spring Data 在 `save()` 時自動發布，`OrderService` 無需注入 `ApplicationEventPublisher`。

---

→ 本專案的 jMolecules 實作方式見 [CQRS 實作說明](03-cqrs-impl.md)
