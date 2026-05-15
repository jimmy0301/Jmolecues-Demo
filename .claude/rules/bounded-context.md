---
description: Bounded Context 邊界規則，所有 Java 檔案都適用
paths:
  - src/main/java/**/*.java
---

# Bounded Context 邊界規則

## 跨 Context 用 Reference Object

在自己的 `domain/` 定義包裝 UUID 的 `@ValueObject` record，不 import 對方 Context 的任何型別：

```java
// ✅ 正確：ordering/domain/ 定義自己的 Reference，不 import customer 或 catalog 任何型別
@ValueObject public record CustomerReference(UUID id) {}
@ValueObject public record ProductReference(UUID id) {}

public class Order {
    private CustomerReference customer;  // 不持有 Customer，不 import CustomerId
}

// ❌ 違規：直接持有其他 Context 的 Aggregate 物件
public class BadOrder {
    private Customer customer;  // 違反邊界
}
```

**為何不用 `CustomerId` 直接參照：**
- `CustomerId implements Identifier` → ByteBuddy 會對欄位加 `@Id` → `Order` 出現兩個 `@Id`，MongoDB 啟動時 `MappingException`
- `Association<Customer, CustomerId>` → jMolecules ArchUnit DDD rules 掃描泛型參數，誤判為「持有 AggregateRoot」，產生誤報

## Shared Kernel

只放**多個 Context 都需要且語義完全一致**的型別。目前 `shared/` 只有 `Money`。

```java
// ✅ 跨 Context 共用的 Value Object 從 shared kernel import
import com.example.shared.Money;

// ❌ 不應直接依賴其他 Context 的 domain sub-package
import com.example.customer.domain.Customer;
import com.example.catalog.domain.Product;
```

**ID 型別的歸屬：** 各 Context 的 ID 放在自己的 `domain/`，不進 Shared Kernel：
- `CustomerId` → `customer/domain/`（只有 customer context 自用）
- `ProductId` → `catalog/domain/`（只有 catalog context 自用）

**判斷準則：**
- 只有一個 Context 用到 → 放在該 Context 的 `domain` package
- 多個 Context 都用到，語義一致 → 放在 `shared/` Shared Kernel
- 多個 Context 都用到，語義不同 → 各 Context 自訂型別，不共用

**Shared Kernel 的限制：** 修改任何 shared kernel 型別，所有依賴它的 Context 都必須同步調整，因此應盡量小、盡量穩定。

## 禁止事項

1. 跨 Context 持有 Aggregate 物件（用 Reference Object 取代）
2. `@Document` 不搭配 jMolecules annotation 單獨使用
3. 為非 `@AggregateRoot` 的 Entity 建立 `@Repository`
4. ValueObject 有可變狀態（非 final field 或 setter）
5. 跳過 AggregateRoot 直接操作內部 Entity