---
description: ArchUnit 和 Spring Modulith 測試規則
paths:
  - src/test/**/*.java
  - src/main/java/**/*.java
---

# 測試規則

## ArchUnit — JMoleculesArchitectureTest

位置：`src/test/java/com/example/demo/arch/JMoleculesArchitectureTest.java`

每次新增 class 後都要跑。

### 測試總覽（8 tests，7 預期失敗）

| Test | 類型 | 預期結果 | 觸發 violation demo |
|---|---|---|---|
| `shouldFollowDddRules` | DDD | ❌ 預期失敗 | `BadOrder` #1 |
| `shouldFollowOnionArchitecture` | Onion | ✅ 通過 | — |
| `repositoriesShouldOnlyManageAggregateRoots` | DDD | ❌ 預期失敗 | `OrderItemRepository` #2 |
| `valueObjectsShouldBeImmutable` | DDD | ❌ 預期失敗 | `MutablePrice` #3 |
| `commandsShouldBeImmutable` | CQRS | ❌ 預期失敗 | `BadCommand` #4 |
| `queryModelsShouldNotTriggerCommands` | CQRS | ❌ 預期失敗 | `BadQueryModel` #5 |
| `commandsShouldResideInCommandPackage` | CQRS | ❌ 預期失敗 | `BadCommand` #6 |
| `commandHandlersShouldBeInApplicationLayer` | CQRS | ❌ 預期失敗 | `BadDomainHandler` #7 |

### 規則說明

#### DDD / Onion

| Test | 驗證內容 |
|---|---|
| `shouldFollowDddRules` | jMolecules 內建規則，含跨 Context 物件參照 |
| `shouldFollowOnionArchitecture` | Onion Classical 環狀依賴方向（內圈不依賴外圈） |
| `repositoriesShouldOnlyManageAggregateRoots` | `@Repository` 只能管理 `@AggregateRoot` |
| `valueObjectsShouldBeImmutable` | `@ValueObject` 不可有非 final 欄位或 setter |

#### CQRS

| Test | 驗證內容 |
|---|---|
| `commandsShouldBeImmutable` | `@Command` 不可有非 final 欄位或 setter |
| `queryModelsShouldNotTriggerCommands` | `@QueryModel` 不可呼叫 `@CommandHandler` 方法 |
| `commandsShouldResideInCommandPackage` | `@Command` 必須在 `*.application.command` 套件 |
| `commandHandlersShouldBeInApplicationLayer` | `@CommandHandler` 方法只能在 `*.application.*` 套件 |

### Violation Demo 索引（不要修復）

| Demo class | 違規 # | 說明 |
|---|---|---|
| `BadOrder` | #1 | 跨 Context 持有 `Customer` 物件（應用 ID 參照） |
| `OrderItemRepository` | #2 | `@Repository` 管理非 `@AggregateRoot` 的 Entity |
| `MutablePrice` | #3 | `@ValueObject` 有非 final 欄位與 setter |
| `BadCommand` | #4 | `@Command` 有 non-final field（可變） |
| `BadQueryModel` | #5 | `@QueryModel` 呼叫 `@CommandHandler`（read side 觸發 state change） |
| `BadCommand` | #6 | `@Command` 放在 context root，不在 `application.command` 套件 |
| `BadDomainHandler` | #7 | `@CommandHandler` 在 domain 層（非 `application` 層） |

## Spring Modulith — ModularityTest

- `verifiesModularStructure` — 預期失敗（因為 `BadOrder` violation demo）
- `documentModules` — 產生文件到 `target/spring-modulith-docs/`

## ArchUnit 重要細節

- `JavaParameterizedType` → 用 `.toErasure()` 取得 `JavaClass`，不是 `.getRawType()`
- Lombok `@Setter` 的 `RetentionPolicy.SOURCE`，在 bytecode 中不可見
  → 改用非 final field 檢查即可涵蓋所有 Lombok setter 情況
- record 類別跳過 immutability 檢查（record 天生不可變）
- `MethodCallTarget.resolveMember()` → 回傳 `Optional<JavaMethod>`，用於追蹤 bytecode method call 的 annotation
- `JMoleculesArchitectureRules.ensureOnionClassical()` → 內建 Onion Classical 規則，直接 `.check(classes)` 即可，本專案可通過
- `JMoleculesCqrsRules` 不存在（v0.33.0），CQRS 規則需手寫 ArchCondition

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
