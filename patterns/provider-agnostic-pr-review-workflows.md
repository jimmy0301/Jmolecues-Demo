# Provider-Agnostic PR Review Workflows

當 review 工作可能發生在 GitHub、GitLab、Azure DevOps 等不同平台時，skill 應該把「工作流」和「平台資料取得方式」分離。

## Pattern

- Skill 名稱描述使用者任務，不綁平台，例如 `review-pr-draft`、`triage-pr-comments`。
- Skill 內定義穩定的輸出格式、風險分類、禁止動作。
- 平台差異只影響資料取得：
  - GitHub: PR、review thread、check run。
  - GitLab: MR、discussion、pipeline。
  - Azure DevOps: PR、comment thread、policy/check。
- 預設只產出草稿，不發布 comment、不 resolve thread、不改 PR/MR 狀態。

## Why

這讓同一個 agent workflow 可以跨平台使用，也避免每換平台就重寫 review 邏輯。
