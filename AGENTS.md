# jMolecules DDD Demo

電商 demo，展示 jMolecules annotation 用法。每個 DDD building block 都用對應 annotation 標記。

## 技術棧

| 類別 | 技術 | 版本 |
|---|---|---|
| 語言 / 平台 | Java | 17 |
| 框架 | Spring Boot | 3.5.14 |
| Web | Spring Boot Starter Web | — |
| API 規格 | OpenAPI Generator Maven Plugin | 7.12.0 |
| 資料庫 | Spring Data MongoDB | — |
| DDD Annotations | jMolecules DDD | BOM 2025.0.2 |
| DDD Events | jMolecules Events | BOM 2025.0.2 |
| 架構風格 | jMolecules Onion Architecture（Classical） | BOM 2025.0.2 |
| 架構風格 | jMolecules CQRS Architecture | BOM 2025.0.2 |
| Spring 整合 | jMolecules Spring Integration | BOM 2025.0.2 |
| Bytecode 轉換 | jMolecules ByteBuddy（nodep） + ByteBuddy Maven Plugin | BOM / 1.14.12 |
| 模組管理 | Spring Modulith Starter Core | 1.3.5 |
| 模組事件 | Spring Modulith Events API | 1.3.5 |
| 架構測試 | jMolecules ArchUnit + Spring Modulith Test | BOM / 1.3.5 |
| 單元測試 | JUnit Jupiter + AssertJ + Mockito | 5.12.2 / 3.27.7 / 5.17.0 |
| 程式碼格式 | Spotless + Google Java Format AOSP | 2.46.1 / 1.28.0 |
| 樣板程式碼 | Lombok | 1.18.46 |

## Bounded Context（Onion Architecture — Classical）
```
com.example.demo
├── shared/                  Shared Kernel → Money
├── catalog/
│   ├── domain/              @DomainModelRing   → Product, ProductId, ProductRepository
│   ├── application/         @ApplicationServiceRing → ProductService, ProductQueryModel
│   │                                                   command/ → CreateProductCommand
│   ├── infrastructure/web/  @InfrastructureRing → ProductController
│   └── MutablePrice         ⚠️ violation demo
├── customer/
│   ├── domain/              @DomainModelRing   → Customer, CustomerId, Address, CustomerRepository
│   ├── application/         @ApplicationServiceRing → CustomerService, CustomerQueryModel, CreateCustomerCommand
│   └── infrastructure/web/  @InfrastructureRing → CustomerController
└── ordering/
    ├── domain/              @DomainModelRing   → Order, OrderId, OrderItem, OrderRepository
    │                                             CustomerReference, ProductReference, events…
    ├── domainservice/       @DomainServiceRing → PricingService
    ├── application/         @ApplicationServiceRing → OrderService, OrderQueryModel, OrderSummaryProjection
    │                                                   OrderEventListener, commands/
    ├── infrastructure/web/  @InfrastructureRing → OrderController
    ├── BadOrder             ⚠️ violation demo
    ├── OrderItemRepository  ⚠️ violation demo
    ├── BadCommand           ⚠️ violation demo
    ├── BadDomainHandler     ⚠️ violation demo
    └── BadQueryModel        ⚠️ violation demo
```

## 核心規則（詳見 .codex/rules/）
- 每個 building block 加對應 jMolecules annotation（不可省略）
- 跨 Context 用 Reference Object（`@ValueObject record XxxReference(UUID id)`），不 import 對方型別、不持有 Aggregate 物件
- 跨 Context 讀取 / 更新資料只透過 owner Context 的公開契約（Query Facade、Command Facade、Event、Read Model、Snapshot），不直接使用對方 Repository 或資料庫
- Repository 只為 AggregateRoot 建立
- Aggregate Root 必須保護 invariant；外部只能透過有業務語意的方法改變狀態，不暴露可修改集合或 setter 繞過規則
- 一個 CommandHandler 原則上只修改一個 Aggregate；跨 Aggregate / 跨 Context 後續動作用 Domain Event、Process Manager 或 Saga
- 跨 Context event 視為 Integration Event / public contract，欄位只用穩定型別，consumer 必須具備冪等性
- 可被並發修改的 Aggregate 必須定義 optimistic locking / version conflict 策略；重送語意與並發策略需一致；悲觀鎖只能作為例外策略並需記錄鎖範圍、timeout、deadlock 處理與測試；MongoDB 若需悲觀鎖語意，用 lease lock 範例，不宣稱有 SQL row lock
- Process Manager / Saga 只用於有狀態、多步驟、跨 Aggregate / Context / 外部系統流程，且需具備 correlation id、重試與補償策略
- 命名必須使用所在 Bounded Context 的 ubiquitous language；Command 祈使句、Event 過去式、Query / Projection 讀取語意，避免模糊 `Manager` / `Processor` / `Data`
- ValueObject / Command 必須不可變（record 或 final fields）
- API request / response DTO、OpenAPI generated model、Spring MVC interface 只屬於 `infrastructure.web`；進入 application 前必須轉成 Command、Query 參數或 application result，禁止在 domain / domainservice / application 層使用
- @Command 必須位於標記 `@ApplicationServiceRing` 的 package；@CommandHandler 只在 `@ApplicationServiceRing` package
- @Command 是 use case input，不是 API DTO；@CommandHandler 不應為了 Controller 方便而直接暴露 Aggregate 當 response
- @QueryModel 不可呼叫 @CommandHandler、不可 save/delete/publish event（read side 不觸發 state change）；projection 可 eventual consistent，但必須可重建且可冪等處理事件
- 驗證需分層：transport validation 在 Controller/API DTO，use case validation 在 Application，business invariant 在 Domain
- 每次新增 class 後執行 `mvn spotless:apply` 再跑 ArchUnit test
- Controller 新增時補對應 `@WebMvcTest` API 測試，放在相同 package（`infrastructure.web`）
- 新增架構決策時補 `decisions/`；新增跨專案可重用技術模式時補 `patterns/`

## 完成操作後的自動行為

每次新增、修改或刪除 class、package 結構、架構規則後，自動檢查並同步以下文件（有變動才修改）：
- `AGENTS.md` — Bounded Context 結構圖、核心規則
- `docs/` — 相關章節的程式碼範例、表格、結構說明
- `.codex/rules/` — 對應的 rule 檔範例與規則描述
- `.agents/skills/` — 對應的 skill 模板

## Conventional Commits

本專案使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>[optional scope]: <description>
```

| type | 用途 |
|---|---|
| `feat` | 新功能 |
| `fix` | 修 bug |
| `docs` | 文件 |
| `refactor` | 重構（不影響行為） |
| `test` | 新增或修改測試 |
| `chore` | 維護、建置、工具設定 |
| `style` | 格式調整 |
| `build` | 建置系統或依賴異動 |
| `ci` | CI 設定 |
| `perf` | 效能優化 |

scope 建議用 Bounded Context 名稱：`catalog`、`customer`、`ordering`、`shared`

```
feat(ordering): add PlaceOrderCommand handler
fix(catalog): correct price calculation for discounts
docs: update README with setup guide
test(customer): add WebMvcTest for CustomerController
```

**commit-msg hook** 存放於 `.githooks/commit-msg`，每次 `mvn` 執行時自動安裝到 `.git/hooks/`。
手動安裝（clone 後首次）：`mvn validate -q`

## 常用指令
```bash
mvn spotless:apply          # 格式化
mvn test -pl .              # 跑所有測試（含 ArchUnit + Modulith）
mvn test -Dtest=JMoleculesArchitectureTest  # 只跑 ArchUnit
```

@.codex/rules/ddd-annotations.md
@.codex/rules/onion-architecture.md
@.codex/rules/bounded-context.md
@.codex/rules/cqrs-annotations.md
@.codex/rules/testing.md
@.codex/rules/naming.md

@/Users/keyulun/Documents/claude knowledge base/software-dev-kb/AGENTS.md
