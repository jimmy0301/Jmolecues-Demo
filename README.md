# jMolecules DDD Demo

用 Java + Spring Boot 展示 **Domain-Driven Design（DDD）** 核心戰術模式的電商範例專案。  
每個 DDD building block 都以 [jMolecules](https://github.com/xmolecules/jmolecules) annotation 明確標記，並配有 ArchUnit 架構測試自動驗證。

---

## 章節

1. [Domain-Driven Design 核心概念](docs/01-ddd.md)  
   — Building blocks 定義、Bounded Context、Shared Kernel  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [DDD 實作說明](docs/01-ddd-impl.md) — 本專案對照表、學習路徑、violation demo

2. [Onion Architecture — 洋蔥分層](docs/04-onion.md)  
   — 四個 Ring 職責、依賴方向規則  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [Onion Architecture 實作說明](docs/04-onion-impl.md) — Ring annotation、package-info.java、ArchUnit 驗證

3. [CQRS — 命令與查詢分離](docs/03-cqrs.md)  
   — 核心概念、Command vs Query、執行流程圖  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [CQRS 實作說明](docs/03-cqrs-impl.md) — jMolecules annotations、程式碼範例、violation demo

4. [技術棧說明](docs/02-tech-stack.md)  
   — 採用的框架與工具、各自的職責與選擇理由

---

## 快速開始

```bash
# 格式化
mvn spotless:apply

# 全部測試（含 ArchUnit + Spring Modulith）
mvn test

# 只跑架構測試（ArchUnit）
mvn test -Dtest=JMoleculesArchitectureTest

# 只跑模組結構測試（Spring Modulith）
mvn test -Dtest=ModularityTest
```

**預期結果**：

| 測試 | 結果 | 說明 |
|---|---|---|
| `JMoleculesArchitectureTest`（8 tests） | 7 失敗 / 1 通過 | 7 個因 violation demo 刻意失敗；`shouldFollowOnionArchitecture` 通過 |
| `ModularityTest#verifiesModularStructure` | 失敗 | `BadOrder` 持有跨 Context 的 `Customer` 物件（violation demo #1） |
| `ModularityTest#documentModules` | 通過 | 產生模組文件到 `target/spring-modulith-docs/` |

---

## 延伸閱讀
- [`docs/adr/001-use-jmolecules.md`](docs/adr/001-use-jmolecules.md) — 為何選擇 jMolecules 的架構決策記錄