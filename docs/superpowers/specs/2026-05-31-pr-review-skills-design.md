# PR Review Skills Design

## Goal

建立兩個跨平台 PR/MR workflow skills：

- `review-pr-draft`: 使用者 review 別人的 PR/MR，只產出 review comment 草稿。
- `triage-pr-comments`: 別人 review 使用者的 PR/MR 後，只抓 active comments，整理成表格並產出 reply drafts。

## Platforms

Skills 不綁定平台。GitHub、GitLab、Azure DevOps 的差異只影響資料取得與 comment/thread 狀態判斷。

## Safety

兩個 skills 都只能產生草稿，不可發布 comment、提交 review、resolve thread、改 label 或更新 PR/MR 狀態。

## Output

`review-pr-draft` 以 findings table 呈現優先級、類型、檔案、行數、程式碼片段、風險與草稿 comment。

`triage-pr-comments` 以 active comment table 呈現狀態、優先級、reviewer、檔案、行數、程式碼片段、comment 摘要、agent 判斷、建議處理方式與回覆草稿。

## Documentation

同步更新 `docs/05-skills.md`，並把跨平台 workflow pattern 記錄到 `patterns/provider-agnostic-pr-review-workflows.md`。
