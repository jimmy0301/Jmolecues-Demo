---
name: add-bounded-context
description: 在專案新增一個 Bounded Context（package + Onion rings + per-context AGENTS.md）
arguments:
  - context-name
---

在專案新增一個 Bounded Context。

用法：/add-bounded-context <context-name>
範例：/add-bounded-context shipping

引數：$ARGUMENTS

## 建立的目錄結構

```
src/main/java/<base-package>/<context-name>/
├── package-info.java                    @BoundedContext
├── AGENTS.md                            per-context 說明
├── domain/
│   └── package-info.java               @DomainModelRing
└── infrastructure/
    └── web/
        └── package-info.java           @InfrastructureRing（Controller 放這裡時建立）
```

`application/`、`domainservice/`、`infrastructure/web/` 等 ring 在有對應 class 時才建立。

## 步驟

1. **建立 package 目錄**
   ```
   src/main/java/<base-package>/<context-name>/
   src/main/java/<base-package>/<context-name>/domain/
   ```

2. **`package-info.java`** — BoundedContext annotation
   ```java
   @BoundedContext(name = "<context-name>")
   package <base-package>.<context-name>;
   import org.jmolecules.ddd.annotation.BoundedContext;
   ```

3. **`domain/package-info.java`** — DomainModelRing annotation
   ```java
   @DomainModelRing
   package <base-package>.<context-name>.domain;
   import org.jmolecules.architecture.onion.classical.DomainModelRing;
   ```

4. **建立 AggregateRoot（至少一個）**
   執行 `/new-aggregate <context-name> <AggregateName>`

5. **建立 per-context `AGENTS.md`**
   ```markdown
   # <context-name> Context

   [職責說明]

   ## Package 結構（Onion Architecture）

   ```
   <context-name>/
     package-info.java          @BoundedContext
     domain/                    @DomainModelRing
       <AggregateName>Id.java
       <AggregateName>.java
       <AggregateName>Repository.java
     application/               @ApplicationServiceRing
       <AggregateName>Service.java
       <AggregateName>QueryModel.java
       command/
         Create<AggregateName>Command.java
     infrastructure/web/        @InfrastructureRing
       <AggregateName>Controller.java
   ```

   ## Classes

   | Class | Ring | 類型 | 說明 |
   |---|---|---|---|
   | `<AggregateName>` | domain | AggregateRoot | ... |
   | `<AggregateName>Id` | domain | ID / ValueObject | `@ValueObject record` implements `Identifier` |
   | `<AggregateName>Repository` | domain | Repository | `MongoRepository<<AggregateName>, <AggregateName>Id>` |
   | `<AggregateName>Service` | application | ApplicationService | `@CommandHandler` 處理 Command |
   | `<AggregateName>QueryModel` | application | QueryModel | `@QueryModel` 只讀 |
   | `Create<AggregateName>Command` | application/command | Command | `@Command` 封裝建立意圖 |
   | `<AggregateName>Controller` | infrastructure | Controller | REST 端點 |

   ## 對外暴露

   其他 Context 不可 import `<AggregateName>Id`，改用 Reference Object：
   ```java
   @ValueObject public record <AggregateName>Reference(UUID id) {}
   ```
   ```

6. **更新根目錄 `AGENTS.md` 的 Bounded Context 結構圖**

完成後執行：`mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest`