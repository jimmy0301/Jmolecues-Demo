---
name: verify-architecture
description: 執行完整的架構驗證（Spotless + ArchUnit + Spring Modulith）並回報結果
---

執行完整的架構驗證並回報結果。

步驟：

1. 格式化所有 Java 檔案
```bash
mvn spotless:apply
```

2. 執行 ArchUnit 架構測試
```bash
mvn test -Dtest=JMoleculesArchitectureTest
```

3. 執行 Spring Modulith 模組驗證與文件產生
```bash
mvn test -Dtest=ModularityTest
```

4. 回報結果，說明：
   - ArchUnit 哪些測試通過、哪些失敗（預期失敗的 violation demo 需標注）
   - Spring Modulith 產生的文件位置（`target/spring-modulith-docs/`）
   - 若有非預期的失敗，分析原因並建議修正

**預期失敗（不需修復）：**
- `JMoleculesArchitectureTest.shouldFollowDddRules` — `BadOrder.customer` 跨 Context 違規
- `JMoleculesArchitectureTest.repositoriesShouldOnlyManageAggregateRoots` — `OrderItemRepository` 管理 Entity
- `JMoleculesArchitectureTest.valueObjectsShouldBeImmutable` — `MutablePrice` 可變狀態
- `ModularityTest.verifiesModularStructure` — 同上 `BadOrder` 違規
