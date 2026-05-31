# Chapter 2.1：Flamingock Data Migration

Flamingock 是本專案 data migration 的標準工具，用來把資料庫欄位、index、reference data、feature flag 或外部系統設定等 application-level change 寫成可版本控管、可稽核、可回復的 Change。

官方文件：

- [Flamingock Introduction](https://docs.flamingock.io/get-started/Introduction)
- [Spring Boot integration](https://docs.flamingock.io/frameworks/springboot-integration/introduction)
- [MongoDB Spring Data Target System](https://docs.flamingock.io/target-systems/mongodb-springdata-target-system)
- [Change Anatomy and Structure](https://docs.flamingock.io/changes/anatomy-and-structure)
- [Testing Flamingock](https://docs.flamingock.io/testing/introduction)

---

## 使用時機

使用 Flamingock：

- 新增或調整 MongoDB collection / index
- 補齊既有 document 的新欄位或預設值
- 搬移欄位、拆分欄位、轉換舊資料格式
- 建立 reference data
- 在 rollout 過程中設定 feature flag 或外部系統設定

不要使用 Flamingock：

- 一般 request-time 業務邏輯
- 每次 API 呼叫都要執行的狀態轉換
- 需要長時間人工判讀的 ETL 任務
- 取代 Aggregate invariant 或 application use case

---

## 專案規範

- Change 一旦部署，不可修改既有檔案；修正請新增下一個 Change。
- Change id 必須全專案唯一，命名描述業務目的，不使用流水號作為唯一語意。
- 檔名使用 `_0001__MeaningfulName.java` 或 `_0001__MeaningfulName.yaml`，用 order 控制執行順序。
- 每個 Change 必須有 `@Rollback` 或 template rollback；無法完整 rollback 時，PR 要寫明補償方式。
- Migration 要可重跑；filter 必須只挑尚未遷移或需要修正的資料。
- Backfill 不直接繞過 Aggregate invariant；若必須直接更新 collection，需限制在資料形狀調整、index、舊資料補值等 technical migration。
- 大量資料 migration 要分批、可觀測，並記錄處理筆數與失敗筆數。
- Breaking contract change 必須搭配 expand-and-contract、feature toggle 或 deprecation 流程。

---

## Spring Boot 設定範例

以下是新人理解用範例，實際 dependency 版本以導入時的 Flamingock BOM / plugin 設定為準。

```xml
<dependency>
    <groupId>io.flamingock</groupId>
    <artifactId>flamingock-springboot-integration</artifactId>
    <version>${flamingock.version}</version>
</dependency>
```

使用 Spring Boot 自動整合時，在 configuration class 或 main class 啟用 Flamingock：

```java
@EnableFlamingock(
    stages = {
        @Stage(location = "com.example.demo.migration")
    }
)
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

本專案使用 MongoDB，因此 target system 優先選 Spring Data MongoDB target system。若 migration 只需要 MongoDB driver 操作，也可使用 MongoDB sync target system；不要在 domain layer 建立 migration class。

---

## Code-based Change 範例

適合需要條件判斷、分批、複雜轉換或呼叫 Spring bean 的 migration。

```java
@TargetSystem("mongodb")
@Change(id = "backfill-order-status", author = "ordering-team")
public class _0001__BackfillOrderStatus {

    @Apply
    public void apply(MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("status").exists(false));
        Update update = new Update().set("status", "CREATED");

        mongoTemplate.updateMulti(query, update, "orders");
    }

    @Rollback
    public void rollback(MongoTemplate mongoTemplate) {
        Query query = Query.query(Criteria.where("status").is("CREATED"));
        Update update = new Update().unset("status");

        mongoTemplate.updateMulti(query, update, "orders");
    }
}
```

注意：

- `apply()` filter 要能避免重複更新已完成資料。
- `rollback()` 不應刪掉使用者在 migration 後合法寫入的新資料；必要時用 migration marker 欄位或更精準條件。
- 若欄位已經被新版本程式使用，rollback 通常應配合 feature toggle 關閉新路徑，而不是盲目刪資料。

---

## Template-based Change 範例

適合建立 collection、index 或簡單 update。若 migration 邏輯有業務條件，改用 code-based Change。

```yaml
id: create-order-created-at-index
author: ordering-team
template: mongodb-sync-template
targetSystem:
  id: "mongodb"
steps:
  - apply:
      type: createIndex
      collection: orders
      parameters:
        keys:
          createdAt: 1
        options:
          name: "idx_orders_created_at"
    rollback:
      type: dropIndex
      collection: orders
      parameters:
        indexName: "idx_orders_created_at"
```

---

## 測試要求

- Unit test：測 `@Apply` / `@Rollback` 的查詢條件與資料轉換邏輯。
- Integration test：用 Testcontainers MongoDB 驗證 migration 實際執行結果。
- Spring Boot integration test：確認 Flamingock 不會在測試 context 自動污染資料，必要時使用 deferred execution。
- Migration review 要包含 rollback / retry / already-applied / partial failure 情境。

---

## Review Checklist

- Change 是否 immutable，且沒有修改已部署過的 Change？
- Change id 是否穩定、唯一、有業務語意？
- Migration 是否可重跑？
- Rollback 是否真的安全，還是需要補償流程？
- 是否需要 feature toggle 隔離 deploy 與 enable？
- 是否需要 expand-and-contract 避免新舊版本服務不相容？
- 大量資料是否有 batching、timeout、observability 與失敗處理？
