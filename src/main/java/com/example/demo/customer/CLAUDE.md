# customer Context

顧客管理。管理顧客資料與地址。

## Package 結構（Onion Architecture）

```
customer/
  package-info.java          @BoundedContext
  domain/                    @DomainModelRing
    Address.java
    Customer.java
    CustomerId.java
    CustomerRepository.java
  application/               @ApplicationServiceRing
    CustomerService.java
    CustomerQueryModel.java
    command/
      CreateCustomerCommand.java
  infrastructure/web/        @InfrastructureRing
    CustomerController.java
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Customer` | domain | AggregateRoot | 顧客，含姓名、email、`Address` |
| `CustomerId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `Address` | domain | ValueObject | 地址（`@ValueObject record`） |
| `CustomerRepository` | domain | Repository | `MongoRepository<Customer, CustomerId>` |
| `CustomerService` | application | ApplicationService | `@CommandHandler` 處理 `CreateCustomerCommand` |
| `CustomerQueryModel` | application | QueryModel | `@QueryModel` 只讀，不觸發狀態改變 |
| `CreateCustomerCommand` | application/command | Command | `@Command` 封裝建立顧客意圖 |
| `CustomerController` | infrastructure | Controller | REST 端點，委派給 `CustomerService` / `CustomerQueryModel` |

## 對外暴露

其他 Context 不可 import `CustomerId`，改用 Reference Object：
```java
// ordering/domain/CustomerReference.java
@ValueObject public record CustomerReference(UUID id) {}
```
