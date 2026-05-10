---
description: ArchUnit 和 Spring Modulith 測試規則
paths:
  - src/test/**/*.java
  - src/main/java/**/*.java
---

# 測試規則

## ArchUnit — JMoleculesArchitectureTest

位置：`src/test/java/com/example/demo/arch/JMoleculesArchitectureTest.java`

三個測試，每次新增 class 後都要跑：

| Test | 驗證內容 |
|---|---|
| `shouldFollowDddRules` | jMolecules 內建規則，含跨 Context 物件參照 |
| `repositoriesShouldOnlyManageAggregateRoots` | `@Repository` 只能管理 `@AggregateRoot` |
| `valueObjectsShouldBeImmutable` | `@ValueObject` 不可有非 final 欄位或 setter |

**預期失敗（violation demo）：** 以下三個失敗是故意的，不要修復：
- `BadOrder.customer` — 違規 #1（跨 Context 物件參照）
- `OrderItemRepository` — 違規 #2（為 Entity 建立 Repository）
- `MutablePrice` — 違規 #3（ValueObject 有可變狀態）

## Spring Modulith — ModularityTest

- `verifiesModularStructure` — 預期失敗（因為 `BadOrder` violation demo）
- `documentModules` — 產生文件到 `target/spring-modulith-docs/`

## ArchUnit 重要細節

- `JavaParameterizedType` → 用 `.toErasure()` 取得 `JavaClass`，不是 `.getRawType()`
- Lombok `@Setter` 的 `RetentionPolicy.SOURCE`，在 bytecode 中不可見
  → 改用非 final field 檢查即可涵蓋所有 Lombok setter 情況
- record 類別跳過 immutability 檢查（record 天生不可變）

## 執行指令

```bash
mvn test -Dtest=JMoleculesArchitectureTest   # 只跑架構測試
mvn test -Dtest=ModularityTest               # 只跑模組測試
mvn test                                     # 全部跑（含預期失敗）
```

## Spotless（格式化）

- Spotless `check` 在 `validate` phase 執行（早於 `compile`），格式錯誤會立即終止建置
- ByteBuddy 在 `process-classes` 執行，格式通過才會到達此階段
- 提交前務必先跑 `mvn spotless:apply`
- 若 spotless 報錯，先 `mvn clean` 再重跑
