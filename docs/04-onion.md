# Chapter 4：Onion Architecture — 洋蔥分層

Onion Architecture 定義 **Context 內部**的分層規則：依賴方向只能由外往內，內層不知道外層的存在。

---

## 為什麼需要分層？

沒有分層時，Application Service 可能直接操作資料庫，Domain Service 可能呼叫外部 API，最終業務邏輯散落各處，難以測試與替換。

分層的核心價值：
- **最內層純業務邏輯**，不依賴任何框架或 I/O
- **外層處理技術細節**（資料庫、HTTP、訊息佇列），可以被替換而不影響業務邏輯
- **每層只知道比自己更內層的存在**

---

## 四個環（Ring）

```mermaid
graph TD
    I["Infrastructure Ring（最外層）\nController · DB Adapter · 外部系統整合"]
    A["Application Service Ring\nCommand Handler · Application Service · Event Listener"]
    DS["Domain Service Ring\n跨 Aggregate 無狀態領域邏輯"]
    DM["Domain Model Ring（最內層）\nAggregate Root · Entity · Value Object\nDomain Event · Repository 介面"]

    I -->|依賴| A
    A -->|依賴| DS
    DS -->|依賴| DM
```

| Ring | 職責 | 典型內容 |
|---|---|---|
| **Domain Model** | 純業務邏輯，不依賴任何框架 | Aggregate Root、Entity、Value Object、Domain Event、Repository 介面 |
| **Domain Service** | 跨 Aggregate 的無狀態領域邏輯 | 計算服務（定價、折扣），只依賴 Domain Model |
| **Application Service** | 協調 Use Case 流程 | Command Handler、Application Service、Event Listener |
| **Infrastructure** | 技術細節實作 | Controller、資料庫 adapter、外部系統整合 |

---

## 依賴方向規則

```mermaid
graph LR
    I[Infrastructure Ring] -->|可依賴| A[Application Service Ring]
    A -->|可依賴| DS[Domain Service Ring]
    DS -->|可依賴| DM["Domain Model Ring\n（不依賴任何外層）"]
```

**違規範例：**

```java
// ❌ Domain Model 依賴框架（外層）
@AggregateRoot
public class Order {
    @Autowired  // 不可在 Domain Model 注入 Spring Bean
    private OrderRepository repository;
}

// ✅ 正確：Domain Model 只操作自己的狀態，Repository 由 Application Service 呼叫
@AggregateRoot
public class Order extends AbstractAggregateRoot<Order> {
    public void place() {
        this.status = OrderStatus.PLACED;
        registerEvent(new OrderPlaced(this.id, Instant.now())); // 登記事件，save() 時自動發布
    }
}
```

---

→ 本專案的分層實作見 [Onion Architecture 實作說明](04-onion-impl.md)
