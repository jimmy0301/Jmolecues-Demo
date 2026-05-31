# DomainEvent 範例

## ordering context 的 event pair

```java
// OrderPlaced.java
@DomainEvent
public record OrderPlaced(OrderId orderId, Instant occurredOn) {}

// OrderCancelled.java
@DomainEvent
public record OrderCancelled(OrderId orderId, Instant occurredOn) {}
```

## Order Aggregate 的 producer 方法

Producer 方法不加任何 annotation，只負責更新狀態並登記 event：

```java
@AggregateRoot
@Document(collection = "orders")
public class Order {

    // ✅ 不加 @DomainEventHandler — 這是 producer，不是 consumer
    public void place() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is already " + status);
        }
        this.status = OrderStatus.PLACED;
        registerEvent(new OrderPlaced(this.id, Instant.now()));
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCancelled(this.id, Instant.now()));
    }
}
```

## Application Service 儲存 Aggregate

```java
@Service
public class OrderService {

    private final OrderRepository repository;

    public Order place(OrderId id) {
        Order order = repository.findById(id).orElseThrow();
        order.place();              // event 已登記在 aggregate
        return repository.save(order); // Spring Data 發布已登記的 event
    }
}
```

## Event Consumer（接收方）

接收方才使用 `@DomainEventHandler`（或 Spring Modulith 的 `@ApplicationModuleListener`）：

```java
@Component
class OrderPlacedListener {

    @ApplicationModuleListener  // consumer — 接收 OrderPlaced event
    void on(OrderPlaced event) {
        // 通知倉庫備貨、寄送確認信...
        // 必須具備冪等性：同一 event 重送時不可重複 side effect
    }
}
```
