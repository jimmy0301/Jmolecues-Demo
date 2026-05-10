---
description: jMolecules DDD annotation 使用規則，編輯 src/main/java 時自動載入
paths:
  - src/main/java/**/*.java
---

# jMolecules Annotation 規則

## AggregateRoot
```java
@AggregateRoot                          // org.jmolecules.ddd.annotation.AggregateRoot
@Document(collection = "orders")        // Spring Data MongoDB
public class Order implements org.jmolecules.ddd.types.AggregateRoot<Order, OrderId> {
    @Id @Identity private OrderId id;   // @Id (Spring Data) + @Identity (jMolecules) 兩者都要
}
```
- 用 annotation style（`@AggregateRoot`）為主
- 當內部 Entity 需要 `Entity<A,ID>` typed interface 時，才加 `implements AggregateRoot<T,ID>`

## Entity（Aggregate 內部）

Entity 需要同時使用 annotation **和** typed interface，兩者缺一不可：

```java
// OrderItem.java
@org.jmolecules.ddd.annotation.Entity          // annotation（給 ArchUnit 辨識）
public class OrderItem implements Entity<Order, OrderItemId> {  // typed interface（給 Spring Modulith 辨識所屬 Aggregate）

    private OrderItemId id = OrderItemId.create();  // 不加 @Id（不是 MongoDB document root）
    private ProductId productId;
    private Quantity quantity;
    private Money unitPrice;

    protected OrderItem() {}  // MongoDB 反序列化用

    public OrderItem(ProductId productId, Quantity quantity, Money unitPrice) { ... }
}
```

**必要條件：** 若 Entity 實作 `Entity<A, ID>`，其 owning Aggregate 也必須加 `implements AggregateRoot<A, ID>`：
```java
public class Order implements org.jmolecules.ddd.types.AggregateRoot<Order, OrderId> { ... }
```
否則編譯失敗（`Entity<A,ID>` 的 type bound 要求 `A extends AggregateRoot<A,?>`）。

**注意：**
- Entity 的 id 欄位加 `@Identity`（ArchUnit 的 `JMoleculesDddRules.all()` 會檢查），但**不加** `@Id`（只有 MongoDB document root 的 AggregateRoot 才加）
- Spring Modulith 不會在文件中列出 Entity — 這是正確的（內部實作細節）
- 禁止為 Entity 建立 `@Repository`（只有 AggregateRoot 才能有 Repository）

### Entity 需要獨立 MongoDB collection 時的選擇

**訊號：** 若 Entity 需要自己的 collection、或需要獨立查詢，代表它可能已不是內部實作細節。

#### 方向 A — 升格為 AggregateRoot（DDD 正確，推薦）

適用：Entity 需要獨立的生命週期、或跨 Aggregate 查詢。

```java
// 1. 改為 AggregateRoot
@AggregateRoot
@Document(collection = "order_items")       // 自己的 collection
public class OrderItem {
    @Id @Identity private OrderItemId id;   // 加回 @Id
    private OrderId orderId;                // 改存 parent ID，不持有 Order 物件
    // ...
}

// 2. Order 改存 ID 參照，不持有 OrderItem 物件
public class Order {
    private List<OrderItemId> itemIds = new ArrayList<>();
}

// 3. OrderItemRepository 成為合法的 Repository
@Repository
public interface OrderItemRepository extends CrudRepository<OrderItem, OrderItemId> {}
```

**影響：**
- `OrderItem` 從 violation demo 變成正式 AggregateRoot
- `Order.addItem()` 改為 `order.addItemId(savedItem.getId())`
- 取得完整 items 需要透過 `OrderItemRepository.findByOrderId()`

---

#### 方向 B — 保留 Entity 語意，用 MongoTemplate 寫入額外 collection

適用：需要寫入 audit log / 快照，但 `OrderItem` 仍由 `Order` 統一管理。

```java
// OrderItem 維持 @Entity，不加 @Document
// 在 OrderService 手動寫入額外 collection
@Service
public class OrderService {

    private final MongoTemplate mongoTemplate;

    public Order placeOrder(OrderId orderId) {
        Order order = findOrder(orderId);
        events.publishEvent(order.place());
        order.getItems().forEach(item ->
            mongoTemplate.save(item, "order_item_snapshots")); // 寫入快照 collection
        return orderRepository.save(order);
    }
}
```

**影響：**
- `OrderItem` 維持 Entity 語意，`Order` 仍是唯一存取入口
- 額外 collection 只能透過 `MongoTemplate` 操作，無法用 Spring Data Repository
- 架構較複雜，適合 audit / snapshot 場景，不適合一般查詢需求

## ID class
```java
@ValueObject
public record OrderId(@Identity UUID id) implements Identifier {
    public static OrderId create() { return new OrderId(UUID.randomUUID()); }
}
```
- `@ValueObject` 標記 record，`@Identity` 標記 UUID 欄位
- 實作 `org.jmolecules.ddd.types.Identifier`
- AggregateRoot 和 Entity 都需要各自的 ID class（例如 `OrderId`、`OrderItemId`）

## ValueObject
```java
@ValueObject
public record Money(BigDecimal amount, String currency) { ... }
```
- 必須不可變：用 record，或所有欄位 final
- 禁止 `@Setter`、非 final 欄位

## Repository
```java
@Repository   // org.jmolecules.ddd.annotation.Repository（不是 Spring 的）
public interface OrderRepository extends CrudRepository<Order, OrderId> { ... }
```
- 只為 `@AggregateRoot` 建立，禁止為內部 Entity 建立

## DomainEvent
```java
@DomainEvent   // org.jmolecules.event.annotation.DomainEvent
public record OrderPlaced(OrderId orderId, Instant occurredOn) { ... }
```
- 命名用**過去式**
- 用 `@DomainEventHandler` 標記發出 event 的 Aggregate 方法

## DomainService
```java
@Service   // org.jmolecules.ddd.annotation.Service（沒有 @DomainService，不存在）
public class PricingService { ... }
```

## BoundedContext
```java
// 每個 context 的 package-info.java
@BoundedContext(name = "ordering")
package com.example.demo.ordering;
```
