# Task: ordering Context

## 職責
管理訂單生命週期（建立、下單、取消）。

## 依賴
- `catalog.ProductId` — 商品參照（只存 ID）
- `customer.CustomerId` — 顧客參照（只存 ID）

## 已實作的 Classes

### AggregateRoot
- `Order` — 訂單主體，持有 `CustomerId`（跨 Context ID 參照）、`OrderItem` list
  - `place()` → 發出 `OrderPlaced`
  - `cancel()` → 發出 `OrderCancelled`

### Internal Entity
- `OrderItem` — 實作 `Entity<Order, OrderItemId>`，包含 `ProductId`、`Quantity`、`Money`

### ValueObjects
- `OrderId` — Order 的 ID，`@ValueObject record` implements `Identifier`
- `OrderItemId` — OrderItem 的 ID
- `Quantity` — 數量，`int value`，不可為負
- `OrderStatus` — enum（PENDING, PLACED, CANCELLED）

### DomainEvents
- `OrderPlaced(OrderId, Instant)`
- `OrderCancelled(OrderId, Instant)`

### DomainService
- `PricingService` — `@Service`，計算 `List<OrderItem>` 的總價

### Repository
- `OrderRepository` — `@Repository`，`CrudRepository<Order, OrderId>`

### Violation Demos
- `BadOrder` — 違規 #1：直接持有 `Customer` 物件（不是 `CustomerId`）
- `OrderItemRepository` — 違規 #2：`@Repository` 管理非 `@AggregateRoot` 的 `OrderItem`

## 注意事項
- `OrderItem` 使用 typed interface `Entity<Order, OrderItemId>`
  - 這要求 `Order` 也要 `implements AggregateRoot<Order, OrderId>`
  - `OrderItem` 不會出現在 Spring Modulith 文件（正確行為，內部實作細節）
- `Order.addItem()` 會檢查 status，只有 PENDING 狀態才能加入 item
