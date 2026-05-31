# Chapter 5.3：Review Workflow Skills

本章說明 PR / MR review 相關技能。這些技能只產出草稿或整理表格，不直接發布 review、不改 PR/MR 狀態。

---

## `/review-pr-draft`

**用途：** Review 別人的 GitHub PR、GitLab MR 或 Azure DevOps PR，只產出草稿，不發布 comment、不提交 review、不 approve/request changes。

**用法：**

```text
/review-pr-draft <pr-or-mr-url>
```

**輸出表格：**

| # | Priority | Type | File | Lines | Code snippet | Finding | Risk | Draft review comment | Blocking |
|---|---|---|---|---|---|---|---|---|---|

**平台支援：**

| 平台 | Review 目標 | 相關資料 |
|---|---|---|
| GitHub | Pull Request | PR metadata、diff、review threads、check runs |
| GitLab | Merge Request | MR metadata、diff、discussions、pipelines |
| Azure DevOps | Pull Request | PR metadata、diff、comment threads、policies/checks |

**規則：**

- 只產出草稿，不寫回平台
- 優先檢查 correctness、架構邊界、測試、相容性、安全與營運風險
- 找不到 actionable issue 時，明確說明沒有發現阻塞問題，並列出剩餘風險或測試缺口

---

## `/triage-pr-comments`

**用途：** 整理別人 review 自己 PR/MR 後留下的 active comments，只抓尚未 resolved / closed / inactive 的 comments，產出處理表格與回覆草稿。

**用法：**

```text
/triage-pr-comments <pr-or-mr-url>
```

**輸出表格：**

| # | Status | Priority | Type | Reviewer | File | Lines | Code snippet | Comment summary | Agent judgment | Suggested action | Reply draft |
|---|---|---|---|---|---|---|---|---|---|---|---|

**Active comments 定義：**

| 平台 | 納入 | 排除 |
|---|---|---|
| GitHub | unresolved review threads、active review comments | resolved、outdated、dismissed、非 actionable bot comments |
| GitLab | unresolved discussions | resolved discussions、obsolete diff notes |
| Azure DevOps | active comment threads | closed、fixed、won't fix、resolved、inactive threads |

**規則：**

- 每筆 active comment 必須帶檔案、行數或行數範圍
- 每筆 active comment 必須附最小必要程式碼片段，預設 comment 行上下各 3 行
- 沒有行號的 active comment 放在 `General comments`
- 已 resolved / outdated / closed 的 comments 預設略過，最後只回報略過數量
- 只產出 reply draft，不發布、不 resolve thread、不改 PR/MR 狀態
