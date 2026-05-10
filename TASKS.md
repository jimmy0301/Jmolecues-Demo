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

## 待辦

- [ ] REST API controller（catalog, ordering, customer）
- [ ] Swagger / OpenAPI 文件
- [ ] `@InfrastructureRing` — 待 controller / infrastructure 實作後補上

## 詳細任務

- [tasks/01-catalog.md](tasks/01-catalog.md) — catalog context 完整說明
- [tasks/02-customer.md](tasks/02-customer.md) — customer context 完整說明
- [tasks/03-ordering.md](tasks/03-ordering.md) — ordering context 完整說明
