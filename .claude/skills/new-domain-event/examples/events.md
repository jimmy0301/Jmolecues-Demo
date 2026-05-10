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

## Order Aggregate 的 handler 方法

```java
@AggregateRoot
@Document(collection = "orders")
public class Order {

    @DomainEventHandler
    public OrderPlaced place() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is already " + status);
        }
        this.status = OrderStatus.PLACED;
        return new OrderPlaced(this.id, Instant.now());
    }

    @DomainEventHandler
    public OrderCancelled cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        return new OrderCancelled(this.id, Instant.now());
    }
}
```
