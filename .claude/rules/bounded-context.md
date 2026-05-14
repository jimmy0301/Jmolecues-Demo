---
description: Bounded Context 邊界規則，所有 Java 檔案都適用
paths:
  - src/main/java/**/*.java
---

# Bounded Context 邊界規則

## 跨 Context 只能用 ID 參照

```java
// ✅ 正確：Order 只持有 CustomerId
public class Order {
    private CustomerId customerId;
}

// ❌ 違規：直接持有其他 Context 的 Aggregate 物件
public class BadOrder {
    private Customer customer;  // 違反邊界
}
```

## Shared Kernel

跨多個 Context 使用的 Value Object 與 ID class 放在獨立的 shared kernel package（例如 `shared/`），不屬於任一 Context。

```java
// ✅ 跨 Context 的型別從 shared kernel import
import com.example.shared.Money;
import com.example.shared.CustomerId;

// ❌ 不應直接依賴其他 Context 的 domain sub-package
import com.example.customer.domain.CustomerId;
```

**判斷準則：**
- 只有一個 Context 用到 → 放在該 Context 的 `domain` package
- 兩個以上 Context 都用到 → 放在 shared kernel package

**Shared Kernel 的限制：** 修改任何 shared kernel 型別，所有依賴它的 Context 都必須同步調整，因此應盡量小、盡量穩定。

## 禁止事項

1. 跨 Context 持有 Aggregate 物件（只能持有其 ID）
2. `@Document` 不搭配 jMolecules annotation 單獨使用
3. 為非 `@AggregateRoot` 的 Entity 建立 `@Repository`
4. ValueObject 有可變狀態（非 final field 或 setter）
5. 跳過 AggregateRoot 直接操作內部 Entity