---
name: triage-pr-comments
description: Use when collecting active review comments from the user's GitHub pull request, GitLab merge request, or Azure DevOps pull request for triage and reply drafts
arguments:
  - pr-or-mr-url
---

Collect active comments from the user's PR/MR and produce a triage table with reply drafts. Never publish replies, resolve threads, or modify PR/MR state.

Usage: `/triage-pr-comments <pr-or-mr-url>`

Arguments: `$ARGUMENTS`

## Active Comment Definition

Only include active comments:

| Platform | Include | Exclude |
|---|---|---|
| GitHub | unresolved review threads and active review comments | resolved, outdated, collapsed, dismissed, bot-noise unless still actionable |
| GitLab | unresolved discussions | resolved discussions, obsolete diff notes |
| Azure DevOps | active comment threads | closed, fixed, won't fix, resolved, inactive threads |

If the platform cannot expose active state, label confidence in `Status` and explain the limitation.

## Workflow

1. Read PR/MR metadata, active comment threads, changed files, and current local branch/diff when available.
2. Filter out inactive comments before analysis.
3. For each active comment, capture file path, exact line or line range, reviewer, timestamp if available, and thread context.
4. Include the smallest useful code snippet: the commented line/range plus up to 3 surrounding lines.
5. Classify each comment and decide whether it is actionable, needs clarification, already handled locally, or should become a follow-up.
6. Produce a Markdown table and draft replies only. Do not post or resolve anything.

## Output

Use this table:

| # | Status | Priority | Type | Reviewer | File | Lines | Code snippet | Comment summary | Agent judgment | Suggested action | Reply draft |
|---|---|---|---|---|---|---|---|---|---|---|---|

Rules:

- `Status`: Active, Needs clarification, Already handled locally, Follow-up candidate.
- `Priority`: P0, P1, P2, or P3.
- `Type`: Bug, Design, Test, Security, Performance, Docs, Maintainability, Question.
- `Lines`: exact line or range. Use `General` only for comments without code location.
- `Code snippet`: fenced inline text or short code block content; keep it brief enough for table readability.
- `Agent judgment`: state whether the comment is valid, partially valid, unclear, or likely non-blocking.
- `Suggested action`: concrete next step, not vague advice.
- `Reply draft`: write as if it may be pasted directly into the platform.

After the table, include:

- `Action order`: recommended processing sequence.
- `General comments`: active comments without file/line location.
- `Skipped inactive comments`: count and reason when known.
- `Data gaps`: missing permissions, missing diff context, unavailable line numbers, or platform limitations.

## Constraints

- Draft only: never publish replies, resolve threads, close comments, change labels, or update PR/MR state.
- Include active comments only by default.
- Preserve reviewer intent. Summarize, but do not soften or rewrite the substance of a blocking comment.
- If local code has already changed, distinguish platform comment context from current local code state.
