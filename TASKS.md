# Tasks

## 已完成

- [x] catalog context — `Product`, `ProductId`, `Money`, `ProductRepository`
- [x] customer context — `Customer`, `CustomerId`, `Address`, `CustomerRepository`
- [x] ordering context — `Order`, `OrderId`, `OrderItem`, `OrderItemId`, `Quantity`, `OrderStatus`
- [x] ordering events — `OrderPlaced`, `OrderCancelled`、`@DomainEventHandler` 方法
- [x] ordering services — `PricingService`（DomainService）、`OrderService`（ApplicationService）
- [x] ordering repository — `OrderRepository`
- [x] ordering event listener — `OrderEventListener`（`@ApplicationModuleListener`）
- [x] violation demos — `BadOrder` (#1), `OrderItemRepository` (#2), `MutablePrice` (#3)
- [x] ArchUnit tests — `JMoleculesArchitectureTest`（3 tests + 3 violation demos）
- [x] Spring Modulith tests — `ModularityTest`（verify + documentModules）
- [x] Spotless + Google Java Format 1.28.0 AOSP（`validate` phase）
- [x] ByteBuddy plugin（`transform-extended`，`process-classes` phase）
- [x] `package-info.java`（`@BoundedContext`）— catalog, customer, ordering
- [x] Onion Architecture — `@DomainModelRing` / `@DomainServiceRing` / `@ApplicationServiceRing`
- [x] CLAUDE.md 拆解 — rules / skills / per-context CLAUDE.md
- [x] OpenAPI YAML spec + openapi-generator（catalog, customer, ordering）
- [x] REST API controller（ProductController, CustomerController, OrderController）
- [x] QueryModel（ProductQueryModel, CustomerQueryModel, OrderQueryModel）
- [x] `@InfrastructureRing` — catalog / customer / ordering web package

## 待辦

（全部完成）

## 詳細任務

- [tasks/01-catalog.md](tasks/01-catalog.md) — catalog context 完整說明
- [tasks/02-customer.md](tasks/02-customer.md) — customer context 完整說明
- [tasks/03-ordering.md](tasks/03-ordering.md) — ordering context 完整說明
- [tasks/04-openapi-yaml.md](tasks/04-openapi-yaml.md) — 撰寫 OpenAPI YAML spec
- [tasks/05-openapi-generator.md](tasks/05-openapi-generator.md) — 設定 openapi-generator-maven-plugin
- [tasks/06-product-controller.md](tasks/06-product-controller.md) — 實作 ProductController
- [tasks/07-customer-controller.md](tasks/07-customer-controller.md) — 實作 CustomerController
- [tasks/08-order-controller.md](tasks/08-order-controller.md) — 實作 OrderController
- [tasks/09-infrastructure-ring.md](tasks/09-infrastructure-ring.md) — 加上 @InfrastructureRing
- [tasks/10-archunit-verify.md](tasks/10-archunit-verify.md) — 驗證 ArchUnit 仍通過
- [tasks/11-order-query-model.md](tasks/11-order-query-model.md) — 建立 OrderQueryModel
- [tasks/12-catalog-application.md](tasks/12-catalog-application.md) — catalog application 層 + ProductQueryModel
- [tasks/13-customer-application.md](tasks/13-customer-application.md) — customer application 層 + CustomerQueryModel + CreateCustomerCommand
