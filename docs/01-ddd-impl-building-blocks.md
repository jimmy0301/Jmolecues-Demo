# DDD Building Blocks 實作對應

本文件對應 [DDD 核心概念](01-ddd.md) 的 Building Blocks。

---

## Building Blocks 對照

| 概念 | 本專案範例 |
|---|---|
| **Aggregate Root** | [`Order`](../src/main/java/com/example/demo/ordering/domain/Order.java)、[`Product`](../src/main/java/com/example/demo/catalog/domain/Product.java)、[`Customer`](../src/main/java/com/example/demo/customer/domain/Customer.java) |
| **Entity** | [`OrderItem`](../src/main/java/com/example/demo/ordering/domain/OrderItem.java)（隸屬 `Order`） |
| **Value Object** | [`Money`](../src/main/java/com/example/demo/shared/Money.java)、[`Address`](../src/main/java/com/example/demo/customer/domain/Address.java)、[`Quantity`](../src/main/java/com/example/demo/ordering/domain/Quantity.java) |
| **Domain Event** | [`OrderPlaced`](../src/main/java/com/example/demo/ordering/domain/OrderPlaced.java)、[`OrderCancelled`](../src/main/java/com/example/demo/ordering/domain/OrderCancelled.java) |
| **Repository** | [`OrderRepository`](../src/main/java/com/example/demo/ordering/domain/OrderRepository.java)、[`ProductRepository`](../src/main/java/com/example/demo/catalog/domain/ProductRepository.java) |
| **Domain Service** | [`PricingService`](../src/main/java/com/example/demo/ordering/domainservice/PricingService.java) |

---

## 建議學習路徑

按以下順序閱讀，從最小的概念疊加到完整架構。

### Step 1：Value Object — 最小的不可變積木

```
shared/Money.java                      金額，有 add() / multiply()
catalog/domain/ProductId.java          UUID 包裝成有語意的型別（catalog context 自用）
ordering/domain/Quantity.java          數量，帶業務驗證（不能為 0 或負數）
ordering/domain/ProductReference.java  ordering 對商品的跨 Context 參照（包裝 UUID）
```

**關鍵問題**：為什麼要把 `BigDecimal` 包成 `Money`？

`BigDecimal` 是純數字，`Money` 可以帶業務規則：不同幣別不能相加、乘法回傳新實例保持不可變。
`Money` 在 `shared` 而非 `catalog.domain`，是因為 `ordering` 的 `OrderItem` 也需要它。
`ProductId` 放在 `catalog/domain/`，只有 `catalog` 自己使用；`ordering` 改用 `ProductReference` 間接參照，不 import `ProductId`。
`Quantity` 只在 `ordering` 內部用，留在 `ordering.domain` 即可。

**對應測試：** [`MoneyTest`](../src/test/java/com/example/demo/shared/MoneyTest.java) — 驗證 `add()` 同/異幣別與 `multiply()` 行為。

### Step 2：Entity — 有 ID、隸屬 Aggregate

```
ordering/domain/OrderItem.java
ordering/domain/OrderItemId.java
```

**關鍵問題**：`OrderItem` 跟 `Order` 差在哪？

`OrderItem` 有自己的 `OrderItemId`，但它不是獨立的 MongoDB document（沒有 `@Id`）。
外部無法直接查詢 `OrderItem`，必須透過 `Order` 存取；這就是「Aggregate 是一致性邊界」的意義。

**對應測試：** [`OrderItemTest`](../src/test/java/com/example/demo/ordering/domain/OrderItemTest.java) — 驗證 `subtotal()` 正確計算。

### Step 3：Aggregate Root — 一群物件的守門員

```
ordering/domain/Order.java
ordering/domain/OrderStatus.java
```

**對應測試：** [`OrderTest`](../src/test/java/com/example/demo/ordering/domain/OrderTest.java) — 驗證 `addItem` / `place` / `cancel` 狀態機與 guard 條件（9 個案例）。

```java
public void place() {
    if (status != OrderStatus.PENDING) {
        throw new IllegalStateException("Order is already " + status);
    }
    this.status = OrderStatus.PLACED;
    registerEvent(new OrderPlaced(this.id, Instant.now()));
}
```

### Step 4：Domain Event — 狀態改變的記錄

```
ordering/domain/OrderPlaced.java
ordering/domain/OrderCancelled.java
ordering/application/OrderEventListener.java
```

**關鍵問題**：為什麼要用 Event，直接呼叫不行嗎？

直接呼叫讓 `ordering` 必須知道「下單之後要通知誰」，耦合度高。
用 Event 後，`ordering` 只說「我下單了」，其他模組自己訂閱。
**過去式命名**（`OrderPlaced` 而非 `PlaceOrder`）代表「已發生的事實」，不可被拒絕。

### Step 5：Application Service — 協調者，不含業務邏輯

```
ordering/application/command/PlaceOrderCommand.java
ordering/application/OrderService.java
```

**關鍵問題**：`OrderService` 跟 `Order` 的職責怎麼分？

`OrderService` 只做三件事：取 Aggregate → 呼叫方法 → 儲存。
**業務規則在 `Order.place()` 裡**，不在 Service 裡。

```java
@CommandHandler
public Order handle(PlaceOrderCommand command) {
    Order order = findOrder(command.orderId());   // 1. 取 Aggregate
    order.place();                                // 2. 執行業務方法
    return orderRepository.save(order);           // 3. 持久化
}
```

**對應測試：** [`OrderServiceTest`](../src/test/java/com/example/demo/ordering/application/OrderServiceTest.java) — mock Repository，驗證三個 command handler 與 not-found 例外。
