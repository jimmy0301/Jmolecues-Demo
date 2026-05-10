# Task: customer Context

## 職責
顧客管理。管理顧客基本資料與地址。

## Classes

### AggregateRoot
- `Customer` — 顧客主體，含姓名、email、`Address`

### ValueObjects
- `CustomerId` — Customer 的 ID，`@ValueObject record` implements `Identifier`
- `Address` — 地址，`@ValueObject record`（street、city、zipCode）

### Repository
- `CustomerRepository` — `@Repository`，`CrudRepository<Customer, CustomerId>`

## 對外暴露
其他 Context 只能使用 `CustomerId`，不可持有 `Customer` 物件。
ordering context 的 `Order` 持有 `CustomerId` 作為跨 Context 的 ID 參照。

## 注意事項
- `Customer` 使用 annotation-only style（`@AggregateRoot`），不需實作 typed interface
  （因為 customer context 沒有內部 Entity）
