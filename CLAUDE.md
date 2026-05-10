# jMolecules DDD Demo

電商 demo，展示 jMolecules annotation 用法。每個 DDD building block 都用對應 annotation 標記。

## 技術棧
- Java 17 · Spring Boot 3.5.14 · Spring Data MongoDB · Spring Modulith 1.3.5
- jMolecules BOM 2025.0.2 · jMolecules ArchUnit · jMolecules Onion Architecture
- Spotless GJF 1.28.0 AOSP
- Lombok

## Bounded Context（Onion Architecture — Classical）
```
com.example.demo
├── catalog/
│   ├── domain/         @DomainModelRing   → Product, ProductId, Money, ProductRepository
│   └── MutablePrice    ⚠️ violation demo
├── customer/
│   └── domain/         @DomainModelRing   → Customer, CustomerId, Address, CustomerRepository
└── ordering/
    ├── domain/         @DomainModelRing   → Order, OrderId, OrderItem, OrderRepository, events…
    ├── domainservice/  @DomainServiceRing → PricingService
    ├── application/    @ApplicationServiceRing → OrderService, OrderEventListener
    ├── BadOrder        ⚠️ violation demo
    └── OrderItemRepository ⚠️ violation demo
```

## 核心規則（詳見 .claude/rules/）
- 每個 building block 加對應 jMolecules annotation（不可省略）
- 跨 Context 只允許 ID 參照，不可持有 Aggregate 物件
- Repository 只為 AggregateRoot 建立
- ValueObject 必須不可變（record 或 final fields）
- 每次新增 class 後執行 `mvn spotless:apply` 再跑 ArchUnit test

## 常用指令
```bash
mvn spotless:apply          # 格式化
mvn test -pl .              # 跑所有測試（含 ArchUnit + Modulith）
mvn test -Dtest=JMoleculesArchitectureTest  # 只跑 ArchUnit
```

@.claude/rules/ddd-annotations.md
@.claude/rules/bounded-context.md
@.claude/rules/testing.md
