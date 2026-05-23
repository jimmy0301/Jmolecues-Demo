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
    ├── application/         @ApplicationServiceRing → OrderService, OrderQueryModel, OrderEventListener
    │                                                   commands/
    ├── infrastructure/web/  @InfrastructureRing → OrderController
    ├── BadOrder             ⚠️ violation demo
    ├── OrderItemRepository  ⚠️ violation demo
    ├── BadCommand           ⚠️ violation demo
    ├── BadDomainHandler     ⚠️ violation demo
    └── BadQueryModel        ⚠️ violation demo
```

## 核心規則（詳見 .Codex/rules/）
- 每個 building block 加對應 jMolecules annotation（不可省略）
- 跨 Context 用 Reference Object（`@ValueObject record XxxReference(UUID id)`），不 import 對方型別、不持有 Aggregate 物件
- 跨 Context 讀取 / 更新資料只透過 owner Context 的公開契約（Query Facade、Command Facade、Event、Read Model、Snapshot），不直接使用對方 Repository 或資料庫
- Repository 只為 AggregateRoot 建立
- ValueObject / Command 必須不可變（record 或 final fields）
- @Command 放在 `application.command` 套件；@CommandHandler 只在 `application` 層
- @QueryModel 不可呼叫 @CommandHandler（read side 不觸發 state change）
- 每次新增 class 後執行 `mvn spotless:apply` 再跑 ArchUnit test
- Controller 新增時補對應 `@WebMvcTest` API 測試，放在相同 package（`infrastructure.web`）

## 完成操作後的自動行為

每次新增、修改或刪除 class、package 結構、架構規則後，自動檢查並同步以下文件（有變動才修改）：
- `AGENTS.md` — Bounded Context 結構圖、核心規則
- `docs/` — 相關章節的程式碼範例、表格、結構說明
- `.Codex/rules/` — 對應的 rule 檔範例與規則描述
- `.Codex/skills/` — 對應的 skill 模板

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

@.Codex/rules/ddd-annotations.md
@.Codex/rules/onion-architecture.md
@.Codex/rules/bounded-context.md
@.Codex/rules/cqrs-annotations.md
@.Codex/rules/testing.md

@/Users/keyulun/Documents/claude knowledge base/software-dev-kb/AGENTS.md
