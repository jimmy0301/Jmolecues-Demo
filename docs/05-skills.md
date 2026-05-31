# Chapter 5：Agent Skills 說明

本章是專案內建技能的入口頁。
每個 Skill 封裝一個常見開發或維護任務，包含固定步驟、程式碼模板與完成後的驗證指令。

---

## 什麼是 Skill？

Skill 是放在 `.agents/skills/<name>/SKILL.md` 的提示模板，在支援技能指令的工具介面輸入 `/<skill-name>` 即可執行。
每個 Skill 包含固定步驟、程式碼模板、以及完成後的驗證指令，避免每次操作重新推導相同規則。

**執行方式：**

```text
/<skill-name> [arguments]
```

---

## Skills 總覽

| Skill | 用途 | 詳細說明 |
|---|---|---|
| `/add-bounded-context` | 新增一個完整的 Bounded Context | [DDD 開發 Skills](05-skills-ddd-development.md#add-bounded-context) |
| `/new-aggregate` | 建立 Aggregate（ID、class、Repository） | [DDD 開發 Skills](05-skills-ddd-development.md#new-aggregate) |
| `/new-entity` | 在 Aggregate 下建立內部 Entity | [DDD 開發 Skills](05-skills-ddd-development.md#new-entity) |
| `/new-domain-event` | 建立 DomainEvent、producer 方法、listener | [DDD 開發 Skills](05-skills-ddd-development.md#new-domain-event) |
| `/new-service` | 建立 Domain Service 或 Application Service | [DDD 開發 Skills](05-skills-ddd-development.md#new-service) |
| `/new-repository` | 為 AggregateRoot 建立 Repository（含框架分離方案） | [DDD 開發 Skills](05-skills-ddd-development.md#new-repository) |
| `/verify-architecture` | 執行完整架構驗證（Spotless + ArchUnit + Modulith） | [架構驗證 Skill](05-skills-architecture-verification.md) |
| `/review-pr-draft` | Review 別人的 PR/MR，只產出 review comment 草稿表格 | [Review Workflow Skills](05-skills-review-workflows.md#review-pr-draft) |
| `/triage-pr-comments` | 整理別人 review 自己 PR/MR 的 active comments 與回覆草稿 | [Review Workflow Skills](05-skills-review-workflows.md#triage-pr-comments) |

---

## 典型開發流程

新增一個完整的 Bounded Context 時，按以下順序執行：

```text
/add-bounded-context <context>
  └─ /new-aggregate <context> <AggregateName>
       ├─ /new-entity <context> <AggregateName> <EntityName>   （有內部 Entity 時）
       ├─ /new-domain-event <EventName> <context>               （有 DomainEvent 時）
       ├─ /new-service domain <context> <DomainService>         （有跨 Aggregate 邏輯時）
       └─ /new-service application <context> <AppService>
/verify-architecture
```

---

## 相關文件

- [DDD 開發 Skills](05-skills-ddd-development.md)
- [架構驗證 Skill](05-skills-architecture-verification.md)
- [Review Workflow Skills](05-skills-review-workflows.md)
