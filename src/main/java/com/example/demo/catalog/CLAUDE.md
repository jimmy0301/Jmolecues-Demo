# catalog Context

商品目錄。管理商品資訊與定價。

## Package 結構（Onion Architecture）

```
catalog/
  package-info.java          @BoundedContext
  MutablePrice.java          ⚠️ Violation Demo #3
  domain/                    @DomainModelRing
    Money.java
    Product.java
    ProductId.java
    ProductRepository.java
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Product` | domain | AggregateRoot | 商品，含名稱、`Money` 定價 |
| `ProductId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `Money` | domain | ValueObject | 金額，`BigDecimal amount` + `String currency` |
| `ProductRepository` | domain | Repository | `MongoRepository<Product, ProductId>` |
| `MutablePrice` | — | ⚠️ Violation Demo | 違規 #3：`@ValueObject` 有可變狀態（`@Setter`） |

## 對外暴露

其他 Context 只能使用 `ProductId`，import 路徑：
```java
import com.example.demo.catalog.domain.ProductId;
import com.example.demo.catalog.domain.Money;  // 若需使用金額 VO
```
