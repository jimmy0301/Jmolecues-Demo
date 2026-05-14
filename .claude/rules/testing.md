---
description: ArchUnit 和 Spring Modulith 測試規則
paths:
  - src/test/**/*.java
  - src/main/java/**/*.java
---

# 測試規則

每次新增 class 後都要跑架構測試。

## ArchUnit — 架構測試

### 應涵蓋的驗證規則

#### DDD / Onion

| 驗證內容 | 說明 |
|---|---|
| `shouldFollowDddRules` | jMolecules 內建規則（`JMoleculesDddRules.all()`），含跨 Context 物件參照、`@Identity` 必要性 |
| `shouldFollowOnionArchitecture` | Onion Classical 環狀依賴方向（`JMoleculesArchitectureRules.ensureOnionClassical()`） |
| `repositoriesShouldOnlyManageAggregateRoots` | `@Repository` 只能管理 `@AggregateRoot` |
| `valueObjectsShouldBeImmutable` | `@ValueObject` 不可有非 final 欄位或 setter |

#### CQRS

| 驗證內容 | 說明 |
|---|---|
| `commandsShouldBeImmutable` | `@Command` 不可有非 final 欄位或 setter |
| `queryModelsShouldNotTriggerCommands` | `@QueryModel` 不可呼叫 `@CommandHandler` 方法 |
| `commandsShouldResideInCommandPackage` | `@Command` 必須在 `*.application.command` 套件 |
| `commandHandlersShouldBeInApplicationLayer` | `@CommandHandler` 方法只能在 `*.application.*` 套件 |

### ArchUnit 實作細節

- `JavaParameterizedType` → 用 `.toErasure()` 取得 `JavaClass`，不是 `.getRawType()`
- Lombok `@Setter` 的 `RetentionPolicy.SOURCE`，在 bytecode 中不可見  
  → 改用**非 final field 檢查**即可涵蓋所有 Lombok setter 情況
- record 類別跳過 immutability 檢查（record 天生不可變）
- `MethodCallTarget.resolveMember()` → 回傳 `Optional<JavaMethod>`，用於追蹤 bytecode method call 的 annotation
- `JMoleculesArchitectureRules.ensureOnionClassical()` → 直接 `.check(classes)` 即可
- `JMoleculesCqrsRules` 不存在（v0.33.0），CQRS 規則需手寫 `ArchCondition`

## Spring Modulith — ModularityTest

- `verifiesModularStructure` — 驗證模組邊界；若有 violation demo 刻意違反邊界，預期失敗
- `documentModules` — 產生模組文件到 `target/spring-modulith-docs/`

## 執行指令

```bash
mvn test -Dtest=JMoleculesArchitectureTest   # 只跑架構測試
mvn test -Dtest=ModularityTest               # 只跑模組測試
mvn test                                     # 全部跑
```

## Spotless（格式化）

- Spotless `check` 在 `validate` phase 執行（早於 `compile`），格式錯誤會立即終止建置
- ByteBuddy 在 `process-classes` 執行，格式通過才會到達此階段
- 提交前務必先跑 `mvn spotless:apply`
- 若 spotless 報錯，先 `mvn clean` 再重跑