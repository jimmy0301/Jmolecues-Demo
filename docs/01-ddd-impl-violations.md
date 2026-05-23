# DDD Violation Demo

本文件列出專案中刻意保留的 DDD 違規範例。這些 class 用來展示 ArchUnit 與 Spring Modulith 如何抓出錯誤模式，**不要修復它們**。

| Class | 違規 | 正確做法 | 觸發測試 |
|---|---|---|---|
| [`BadOrder`](../src/main/java/com/example/demo/ordering/BadOrder.java) | 直接持有 `Customer` 物件 | 改用 `CustomerReference(UUID)` | ArchUnit `shouldFollowDddRules`、Spring Modulith `verifiesModularStructure` |
| [`OrderItemRepository`](../src/main/java/com/example/demo/ordering/OrderItemRepository.java) | `@Repository` 管理 Entity（非 AggregateRoot） | Repository 只為 AggregateRoot 建立 | ArchUnit `repositoriesShouldOnlyManageAggregateRoots` |
| [`MutablePrice`](../src/main/java/com/example/demo/catalog/MutablePrice.java) | `@ValueObject` 有 setter（可變） | 改用 `record` | ArchUnit `valueObjectsShouldBeImmutable` |

`BadOrder` 同時觸發兩套驗證：ArchUnit 從 DDD 規則角度抓到，Spring Modulith 從模組邊界角度抓到；展示同一個違規如何被不同工具從不同角度偵測。
