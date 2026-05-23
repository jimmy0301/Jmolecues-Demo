# catalog Context

商品目錄。管理商品資訊與定價。

## Package 結構（Onion Architecture）

```
catalog/
  package-info.java          @BoundedContext
  MutablePrice.java          ⚠️ Violation Demo #3
  domain/                    @DomainModelRing
    Product.java
    ProductId.java
    ProductRepository.java
  application/               @ApplicationServiceRing
    ProductService.java
    ProductQueryModel.java
    command/
      CreateProductCommand.java
  publicapi/                 @NamedInterface("public-api") + @ApplicationServiceRing
    ProductQueryFacade.java
    ProductSummary.java
  infrastructure/web/        @InfrastructureRing
    ProductController.java
```

## Classes

| Class | Ring | 類型 | 說明 |
|---|---|---|---|
| `Product` | domain | AggregateRoot | 商品，含名稱、`Money` 定價 |
| `ProductId` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
| `ProductRepository` | domain | Repository | `MongoRepository<Product, ProductId>` |
| `ProductService` | application | ApplicationService | `@CommandHandler` 處理 `CreateProductCommand` |
| `ProductQueryModel` | application | QueryModel | `@QueryModel` 只讀，不觸發狀態改變 |
| `CreateProductCommand` | application/command | Command | `@Command` 封裝建立商品意圖 |
| `ProductQueryFacade` | publicapi | Public API | 給其他 Context 查詢商品摘要的公開介面 |
| `ProductSummary` | publicapi | Public DTO | 跨 Context 查詢回傳的穩定 DTO |
| `ProductController` | infrastructure | Controller | REST 端點，委派給 `ProductService` / `ProductQueryModel` |
| `MutablePrice` | — | ⚠️ Violation Demo | 違規 #3：`@ValueObject` 有可變狀態（`@Setter`） |

## 對外暴露

其他 Context 不直接 import `catalog.domain.ProductId`、`Product` 或 `ProductRepository`。
需要讀取商品展示資料時，使用公開介面：
```java
import com.example.demo.catalog.publicapi.ProductQueryFacade;
import com.example.demo.catalog.publicapi.ProductSummary;
```

`Money` 已移至 `shared/`，從 `com.example.demo.shared.Money` import。
