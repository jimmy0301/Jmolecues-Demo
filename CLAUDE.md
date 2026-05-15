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
| 程式碼格式 | Spotless + Google Java Format AOSP | 2.46.1 / 1.28.0 |
| 樣板程式碼 | Lombok | 1.18.46 |

## Bounded Context（Onion Architecture — Classical）
```
com.example.demo
├── shared/                  Shared Kernel → Money
├── catalog/
│   ├── domain/              @DomainModelRing   → Product, ProductId, ProductRepository
│   ├── application/         @ApplicationServiceRing → ProductQueryModel
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

## 核心規則（詳見 .claude/rules/）
- 每個 building block 加對應 jMolecules annotation（不可省略）
- 跨 Context 用 Reference Object（`@ValueObject record XxxReference(UUID id)`），不 import 對方型別、不持有 Aggregate 物件
- Repository 只為 AggregateRoot 建立
- ValueObject / Command 必須不可變（record 或 final fields）
- @Command 放在 `application.command` 套件；@CommandHandler 只在 `application` 層
- @QueryModel 不可呼叫 @CommandHandler（read side 不觸發 state change）
- 每次新增 class 後執行 `mvn spotless:apply` 再跑 ArchUnit test

## 完成操作後的自動行為

每次新增、修改或刪除 class、package 結構、架構規則後，自動檢查並同步以下文件（有變動才修改）：
- `CLAUDE.md` — Bounded Context 結構圖、核心規則
- `docs/` — 相關章節的程式碼範例、表格、結構說明
- `.claude/rules/` — 對應的 rule 檔範例與規則描述
- `.claude/skills/` — 對應的 skill 模板

## 常用指令
```bash
mvn spotless:apply          # 格式化
mvn test -pl .              # 跑所有測試（含 ArchUnit + Modulith）
mvn test -Dtest=JMoleculesArchitectureTest  # 只跑 ArchUnit
```

@.claude/rules/ddd-annotations.md
@.claude/rules/onion-architecture.md
@.claude/rules/bounded-context.md
@.claude/rules/cqrs-annotations.md
@.claude/rules/testing.md
