---
name: new-entity
description: 在指定的 Aggregate 底下建立一個內部 Entity
arguments:
  - context
  - AggregateName
  - EntityName
---

在指定的 Aggregate 底下建立一個內部 Entity。

用法：/new-entity <context> <AggregateName> <EntityName>
範例：/new-entity ordering Order OrderItem

引數：$ARGUMENTS

## 目錄位置（Onion Architecture）

Entity 與 owning AggregateRoot 放在同一個 `<context>/domain/` ring：

```
src/main/java/<base-package>/<context>/domain/
├── <AggregateName>.java           （已存在）
├── <EntityName>Id.java            ← 新增
└── <EntityName>.java              ← 新增
```

## 步驟

1. **`domain/<EntityName>Id.java`** — Entity 的 ID class
   ```java
   @ValueObject
   public record <EntityName>Id(@Identity UUID id) implements Identifier {
       public static <EntityName>Id create() { return new <EntityName>Id(UUID.randomUUID()); }
   }
   ```

2. **`domain/<EntityName>.java`** — Entity class
   ```java
   @org.jmolecules.ddd.annotation.Entity
   public class <EntityName> implements Entity<<AggregateName>, <EntityName>Id> {

       @Identity private <EntityName>Id id = <EntityName>Id.create();  // 不加 @Id

       protected <EntityName>() {}  // MongoDB 反序列化用

       public <EntityName>(...) { ... }
   }
   ```
   - 同時用 `@org.jmolecules.ddd.annotation.Entity`（ArchUnit）和 `implements Entity<A,ID>`（Spring Modulith）
   - id 欄位加 `@Identity`，**不加** `@Id`（只有 MongoDB document root 的 AggregateRoot 才加）
   - 加 protected 無參數建構子

3. **確認 owning AggregateRoot 已實作 typed interface**
   ```java
   public class <AggregateName> implements org.jmolecules.ddd.types.AggregateRoot<<AggregateName>, <AggregateName>Id> { ... }
   ```
   若沒有，補上去，否則編譯失敗（`Entity<A,ID>` 的 type bound 要求 `A extends AggregateRoot<A,?>`）

4. **在 AggregateRoot 加管理 Entity 的方法**
   ```java
   public void add<EntityName>(<EntityName> entity) {
       // 狀態檢查...
       entities.add(entity);
   }
   ```
   禁止外部直接操作 Entity，一律透過 AggregateRoot 的方法。

5. **更新 `<context>/AGENTS.md`** 加入新 Entity 的說明

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```

> ⚠️ 禁止為這個 Entity 建立 `@Repository`，Repository 只能管理 `@AggregateRoot`。
