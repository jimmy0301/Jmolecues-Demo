---
name: new-aggregate
description: 在指定的 Bounded Context 建立一組完整的 Aggregate 相關 class
arguments:
  - context
  - AggregateName
---

在指定的 Bounded Context 建立一組完整的 Aggregate 相關 class。

用法：/new-aggregate <context> <AggregateName> [ValueObject1] [ValueObject2] ...
範例：/new-aggregate catalog Product Money Price

引數：$ARGUMENTS

## 目錄位置（Onion Architecture）

所有 domain class 放在 `<context>/domain/` ring（`@DomainModelRing`）：

```
src/main/java/com/example/demo/<context>/
├── package-info.java              @BoundedContext（已存在則跳過）
└── domain/
    ├── package-info.java          @DomainModelRing（已存在則跳過）
    ├── <AggregateName>Id.java
    ├── <AggregateName>.java
    ├── <ValueObject>.java         （依需要）
    └── <AggregateName>Repository.java
```

## 步驟

1. **`domain/<AggregateName>Id.java`** — ID class
   ```java
   @ValueObject
   public record <AggregateName>Id(@Identity UUID id) implements Identifier {
       public static <AggregateName>Id create() { return new <AggregateName>Id(UUID.randomUUID()); }
   }
   ```

2. **`domain/<AggregateName>.java`** — AggregateRoot
   ```java
   @AggregateRoot
   @Document(collection = "<context_plural>")
   public class <AggregateName> {   // 若有內部 Entity 需要 typed interface，才加 implements AggregateRoot<T,ID>
       @Id @Identity private <AggregateName>Id id;
       protected <AggregateName>() {}
       public <AggregateName>(...) { this.id = <AggregateName>Id.create(); ... }
   }
   ```

3. **各 ValueObject** — 每個用 `@ValueObject` record 實作，放在 `domain/`

4. **`domain/<AggregateName>Repository.java`** — Repository
   ```java
   @Repository  // org.jmolecules.ddd.annotation.Repository
   public interface <AggregateName>Repository extends MongoRepository<<AggregateName>, <AggregateName>Id> {}
   ```

5. **`domain/package-info.java`**（若不存在）
   ```java
   @DomainModelRing
   package com.example.demo.<context>.domain;
   import org.jmolecules.architecture.onion.classical.DomainModelRing;
   ```

6. **`package-info.java`**（若不存在）
   ```java
   @BoundedContext(name = "<context>")
   package com.example.demo.<context>;
   import org.jmolecules.ddd.annotation.BoundedContext;
   ```

7. **更新 `src/main/java/com/example/demo/<context>/CLAUDE.md`** 加入新 class 說明

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```
