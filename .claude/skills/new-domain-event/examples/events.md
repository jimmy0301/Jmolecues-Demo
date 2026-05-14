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

Producer 方法不加任何 annotation，只負責更新狀態並回傳 event：

```java
@AggregateRoot
@Document(collection = "orders")
public class Order {

    // ✅ 不加 @DomainEventHandler — 這是 producer，不是 consumer
    public OrderPlaced place() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is already " + status);
        }
        this.status = OrderStatus.PLACED;
        return new OrderPlaced(this.id, Instant.now());
    }

    public OrderCancelled cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        return new OrderCancelled(this.id, Instant.now());
    }
}
```

## Application Service 發布 event

```java
@Service
public class OrderService {

    private final OrderRepository repository;
    private final ApplicationEventPublisher events;

    public Order place(OrderId id) {
        Order order = repository.findById(id).orElseThrow();
        events.publishEvent(order.place());   // producer 方法回傳 event，交給 publisher 發布
        return repository.save(order);
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
    }
}
```