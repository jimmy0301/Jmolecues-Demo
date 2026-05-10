# Task: catalog Context

## 職責
商品目錄。管理商品資訊與定價。

## Classes

### AggregateRoot
- `Product` — 商品主體，含名稱、`Money` 定價

### ValueObjects
- `ProductId` — Product 的 ID，`@ValueObject record` implements `Identifier`
- `Money` — 金額，`BigDecimal amount` + `String currency`，支援 `add()`、`multiply()`

### Repository
- `ProductRepository` — `@Repository`，`CrudRepository<Product, ProductId>`

### Violation Demo
- `MutablePrice` — 違規 #3：`@ValueObject` 有非 final 欄位 + Lombok `@Setter`

## 對外暴露
其他 Context 只能使用 `ProductId`，不可持有 `Product` 物件。

## 注意事項
- `Money` 是跨 Context 共用的 ValueObject（ordering 的 OrderItem 也使用）
- `MutablePrice` 的 `@Setter` 用 Lombok 實作，Lombok annotation 的 `RetentionPolicy.SOURCE` 讓 bytecode 中看不到 annotation，但非 final 欄位檢查仍能抓到
