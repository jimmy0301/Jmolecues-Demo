# ordering Context

訂單管理。負責訂單建立、下單、取消。

## Package 結構（Onion Architecture）

```
ordering/
  package-info.java              @BoundedContext
  BadOrder.java                  ⚠️ Violation Demo #1
  OrderItemRepository.java       ⚠️ Violation Demo #2
  domain/                        @DomainModelRing
    Order.java
    OrderId.java
    OrderItem.java
    OrderItemId.java
    OrderStatus.java
    Quantity.java
    OrderPlaced.java
    OrderCancelled.java
    OrderRepository.java
  domainservice/                 @DomainServiceRing
    PricingService.java
  application/                   @ApplicationServiceRing
    OrderService.java
    OrderEventListener.java
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Order` | domain | AggregateRoot | 訂單主體，持有 `CustomerId`（跨 Context ID） |
| `OrderId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `OrderItem` | domain | Entity | `implements Entity<Order, OrderItemId>`，含 `ProductId` |
| `OrderItemId` | domain | ID / ValueObject | OrderItem 的 ID |
| `Quantity` | domain | ValueObject | 數量（不可為負） |
| `OrderStatus` | domain | Enum | PENDING / PLACED / CANCELLED |
| `OrderPlaced` | domain | DomainEvent | `@DomainEvent record` |
| `OrderCancelled` | domain | DomainEvent | `@DomainEvent record` |
| `OrderRepository` | domain | Repository | `MongoRepository<Order, OrderId>` |
| `PricingService` | domainservice | DomainService | `@Service`，計算訂單總價 |
| `OrderService` | application | ApplicationService | 發布 domain event，協調 repository |
| `OrderEventListener` | application | EventListener | `@ApplicationModuleListener` 接收事件 |
| `BadOrder` | — | ⚠️ Violation Demo | 違規 #1：直接持有 `Customer` 物件 |
| `OrderItemRepository` | — | ⚠️ Violation Demo | 違規 #2：`@Repository` 管理非 AggregateRoot |

## 跨 Context 依賴
- `com.example.demo.catalog.domain.ProductId` — OrderItem 記錄商品 ID
- `com.example.demo.catalog.domain.Money` — OrderItem 使用金額 VO
- `com.example.demo.customer.domain.CustomerId` — Order 記錄顧客 ID

## 注意
`OrderItem` 不會出現在 Spring Modulith 文件，這是正確的行為（內部 Entity）。
