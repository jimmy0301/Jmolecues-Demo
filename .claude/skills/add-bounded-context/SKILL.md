---
name: add-bounded-context
description: 在專案新增一個 Bounded Context（package + Onion rings + per-context CLAUDE.md）
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
├── package-info.java          @BoundedContext
├── CLAUDE.md                  per-context 說明
└── domain/
    └── package-info.java      @DomainModelRing
```

`application/` 和 `domainservice/` 等 ring 在有對應 class 時才建立。

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

5. **建立 per-context `CLAUDE.md`**
   ```markdown
   # <context-name> Context

   [職責說明]

   ## Package 結構（Onion Architecture）
   ...（依實際 ring 填寫）

   ## Classes
   | Class | Ring | 類型 | 說明 |

   ## 對外暴露
   其他 Context 只能使用 `<AggregateName>Id`（或透過 shared kernel），不可直接依賴此 Context 的 domain sub-package。
   ```

6. **更新根目錄 `CLAUDE.md` 的 Bounded Context 結構圖**

完成後執行：`mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest`