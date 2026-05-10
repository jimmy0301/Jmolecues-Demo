# ADR 001 — 使用 jMolecules 表達 DDD 架構意圖

| 欄位 | 內容 |
|---|---|
| **狀態** | Accepted |
| **決策日期** | 2026-05-10 |
| **決策者** | 開發團隊 |

---

## 背景

在 DDD 專案中，AggregateRoot、Entity、ValueObject、Repository 等概念存在於開發者的腦海與文件中，但程式碼本身無法表達這些意圖。常見問題：

- 新開發者無法從程式碼中辨識「這個 class 是 AggregateRoot 還是普通 POJO？」
- Repository 有時被誤用來操作非 AggregateRoot 的 Entity，違反 DDD 規則但沒有機制阻擋
- 跨 Bounded Context 的物件持有（應只允許 ID 參照）在 Code Review 時容易被遺漏
- 架構規則只存在文件中，無法自動驗證

---

## 決策

採用 **jMolecules**（`org.jmolecules` BOM 2025.0.2）作為 DDD 架構意圖的表達工具，搭配 **ArchUnit** 自動驗證架構規則。

同時採用 **jMolecules Onion Architecture**（Classical）定義 package 分層，並以 **Spring Modulith** 管理 Bounded Context 的模組邊界。

---

## 考量的替代方案

| 方案 | 優點 | 缺點 |
|---|---|---|
| 純 Javadoc 註解（`/** @AggregateRoot */`） | 零依賴 | 無法自動驗證，只是文件 |
| 自訂 annotation（團隊自建） | 完全控制 | 需維護 ArchUnit 規則，重複造輪 |
| jMolecules（本決策） | 標準化、有 ArchUnit 整合、Spring/ByteBuddy 整合 | 增加依賴，需學習 |
| 不做標記（依靠 naming convention） | 最簡單 | 無強制力，規則容易被忽略 |

---

## 決策理由

### 1. 程式碼即文件
jMolecules annotation 讓 DDD 概念直接體現在 class 上，任何人看到 `@AggregateRoot` 就知道這個 class 的角色：

```java
@AggregateRoot
@Document(collection = "orders")
public class Order implements AggregateRoot<Order, OrderId> { ... }
```

### 2. 自動架構驗證
搭配 `jmolecules-archunit`，可在 CI 階段自動驗證：
- `@Repository` 只能管理 `@AggregateRoot`
- 跨 Context 不允許直接持有 Aggregate 物件（只允許 ID 參照）
- `@ValueObject` 必須不可變

違規立即在 `mvn test` 阻擋，不依賴人工 Code Review。

### 3. Spring / MongoDB 整合
`jmolecules-bytebuddy-nodep` 在編譯期透過 ByteBuddy 自動為 Aggregate 產生：
- MongoDB 的 `equals()` / `hashCode()`（基於 ID）
- Spring Data 所需的 `MutablePersistable` 實作

減少樣板程式碼，讓開發者專注領域邏輯。

### 4. Spring Modulith 整合
jMolecules typed interface（`Entity<A,ID>`、`AggregateRoot<A,ID>`）讓 Spring Modulith 能正確辨識 Aggregate 與 Entity 的歸屬關係，產生準確的模組文件。

---

## 後果

### 正面影響
- 新成員可以快速從程式碼理解 DDD 角色，降低 onboarding 成本
- 架構規則有自動防護網，不依賴人工
- Spring / MongoDB 整合樣板程式碼大幅減少

### 負面影響 / 風險
- 引入額外依賴（jMolecules BOM），需跟隨版本更新
- 開發者需學習 jMolecules annotation 語意與使用規則
- ByteBuddy 在 compile time 執行，增加少量建置時間
- GJF 版本需注意 JDK 相容性（JDK 17 使用 1.28.0，不可升至 1.30.0+）

---

## 相關決策

- [ADR 002（待建）] 使用 Spring Modulith 管理模組邊界
- [ADR 003（待建）] Onion Architecture 分層策略
