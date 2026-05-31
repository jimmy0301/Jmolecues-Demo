# Chapter 5.2：架構驗證 Skill

本章說明 `/verify-architecture`，用於執行格式化、ArchUnit 與 Spring Modulith 模組驗證。

---

## `/verify-architecture`

**用途：** 一鍵執行完整的架構驗證，回報通過 / 失敗，並說明哪些失敗是預期的 violation demo。

**用法：**

```text
/verify-architecture
```

**執行步驟：**

1. `mvn spotless:apply` — 格式化
2. `mvn test -Dtest=JMoleculesArchitectureTest` — ArchUnit 規則（9 個測試）
3. `mvn test -Dtest=ModularityTest` — Spring Modulith 模組驗證

**預期失敗（violation demo，不需修復）：**

| 測試 | 失敗原因 |
|---|---|
| `shouldFollowDddRules` | `BadOrder`（持有 `Customer` 物件） |
| `repositoriesShouldOnlyManageAggregateRoots` | `OrderItemRepository`（管理 Entity） |
| `valueObjectsShouldBeImmutable` | `MutablePrice`（有 setter） |
| `commandsShouldBeImmutable` | `BadCommand`（non-final 欄位） |
| `queryModelsShouldNotTriggerCommands` | `BadQueryModel`（呼叫 `@CommandHandler`） |
| `commandsShouldResideInApplicationServiceRing` | `BadCommand`（放在未標記 `@ApplicationServiceRing` 的 context root） |
| `commandHandlersShouldBeInApplicationLayer` | `BadDomainHandler`（在 context root） |
| `verifiesModularStructure` | `BadOrder`、`BadCommand`（跨模組邊界） |

**通過規則：**

| 測試 | 說明 |
|---|---|
| `shouldFollowOnionArchitecture` | jMolecules Classical Onion 依賴方向 |
| `dddBuildingBlocksShouldNotDependOnApiDtos` | DDD building block 不依賴 API DTO / generated model |
