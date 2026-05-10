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
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Customer` | domain | AggregateRoot | 顧客，含姓名、email、`Address` |
| `CustomerId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `Address` | domain | ValueObject | 地址（`@ValueObject record`） |
| `CustomerRepository` | domain | Repository | `MongoRepository<Customer, CustomerId>` |

## 對外暴露

其他 Context 只能使用 `CustomerId`，import 路徑：
```java
import com.example.demo.customer.domain.CustomerId;
```
