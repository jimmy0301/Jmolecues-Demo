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
    OrderService.java              @CommandHandler methods
    OrderQueryModel.java           @QueryModel
    OrderSummary.java              read model DTO
    OrderSummaryProjection.java    @QueryModel + @ApplicationModuleListener
    OrderEventListener.java
    command/
      CreateOrderCommand.java      @Command
      PlaceOrderCommand.java       @Command
      CancelOrderCommand.java      @Command
  infrastructure/web/            @InfrastructureRing
    OrderController.java
  BadCommand.java                ⚠️ Violation Demo #4
  BadDomainHandler.java          ⚠️ Violation Demo #5
  BadQueryModel.java             ⚠️ Violation Demo #6
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Order` | domain | AggregateRoot | 訂單主體，持有 `CustomerReference`（跨 Context 參照），以 `@Version` 示範樂觀鎖 |
| `OrderId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `OrderItem` | domain | Entity | `implements Entity<Order, OrderItemId>`，含 `ProductReference` 與商品 snapshot |
| `OrderItemId` | domain | ID / ValueObject | OrderItem 的 ID |
| `Quantity` | domain | ValueObject | 數量（不可為負） |
| `OrderStatus` | domain | Enum | PENDING / PLACED / CANCELLED |
| `OrderPlaced` | domain | DomainEvent | `@DomainEvent record` |
| `OrderCancelled` | domain | DomainEvent | `@DomainEvent record` |
| `OrderRepository` | domain | Repository | `MongoRepository<Order, OrderId>` |
| `PricingService` | domainservice | DomainService | `@Service`，計算訂單總價 |
| `OrderService` | application | ApplicationService | `@CommandHandler` 接收 Command，協調 repository，發布 domain event |
| `OrderQueryModel` | application | QueryModel | `@QueryModel` 只讀，不觸發狀態改變 |
| `OrderSummary` | application | ReadModel | 訂單查詢 projection 的摘要 DTO |
| `OrderSummaryProjection` | application | QueryModel / Projection | 透過 `@ApplicationModuleListener` 消費事件，建立可重複處理的 read model |
| `CreateOrderCommand` | application/command | `@Command` | 建立新訂單 |
| `PlaceOrderCommand` | application/command | `@Command` | 確認下單 |
| `CancelOrderCommand` | application/command | `@Command` | 取消訂單 |
| `OrderEventListener` | application | EventListener | `@ApplicationModuleListener` 接收事件 |
| `OrderController` | infrastructure | Controller | REST 端點，委派給 `OrderService` / `OrderQueryModel` |
| `BadOrder` | — | ⚠️ Violation Demo | 違規 #1：直接持有 `Customer` 物件 |
| `OrderItemRepository` | — | ⚠️ Violation Demo | 違規 #2：`@Repository` 管理非 AggregateRoot |
| `BadCommand` | — | ⚠️ Violation Demo | 違規 #4：non-final 欄位、放在 context root、import `CustomerId` |
| `BadDomainHandler` | — | ⚠️ Violation Demo | 違規 #5：`@CommandHandler` 在 domain 層 |
| `BadQueryModel` | — | ⚠️ Violation Demo | 違規 #6：`@QueryModel` 呼叫 `@CommandHandler` |

## 跨 Context 依賴
- `com.example.demo.shared.Money` — OrderItem 使用金額 VO（Shared Kernel）
- 跨 Context ID 不直接 import，改用 Reference Object：
  - `CustomerReference(UUID id)` — 取代 `CustomerId`
  - `ProductReference(UUID id)` — 取代 `ProductId`

## 注意
`OrderItem` 不會出現在 Spring Modulith 文件，這是正確的行為（內部 Entity）。
`Order.version` 是 repository 用來偵測 stale write 的 persistence metadata，不應作為領域規則或 API contract。
