---
name: review-pr-draft
description: Use when reviewing someone else's GitHub pull request, GitLab merge request, or Azure DevOps pull request and only draft review feedback is needed
arguments:
  - pr-or-mr-url
---

Review someone else's PR/MR and produce draft feedback only. Never publish, submit, approve, request changes, or resolve comments on the platform.

Usage: `/review-pr-draft <pr-or-mr-url>`

Arguments: `$ARGUMENTS`

## Platform Detection

Infer the platform from the URL or available MCP/app tools:

| Platform | Target | Terms |
|---|---|---|
| GitHub | Pull Request | PR, review thread, check run |
| GitLab | Merge Request | MR, discussion, pipeline |
| Azure DevOps | Pull Request | PR, thread, policy/check |

If the platform tool is unavailable, ask the user for exported diff/comment data instead of guessing.

## Workflow

1. Read PR/MR title, description, author, base branch, target branch, changed files, diff, and CI/check status.
2. Understand intent before reviewing details. If intent is unclear, mark findings that depend on the missing context.
3. Review for correctness, architecture boundaries, tests, migrations/config, backward compatibility, security, and operational risk.
4. Prefer high-signal comments. Do not nitpick formatting unless it affects behavior or maintainability.
5. Produce a Markdown table of draft comments. Do not post it.

## Output

Use this table:

| # | Priority | Type | File | Lines | Code snippet | Finding | Risk | Draft review comment | Blocking |
|---|---|---|---|---|---|---|---|---|---|

Rules:

- `Priority`: P0, P1, P2, or P3.
- `Type`: Bug, Design, Test, Security, Performance, Docs, Maintainability, Question.
- `Lines`: exact line or range when available.
- `Code snippet`: minimum relevant code, usually the changed line plus up to 3 context lines.
- `Draft review comment`: write as if it may be pasted directly into the platform.
- `Blocking`: Yes only when the PR/MR should not merge before addressing the issue.

After the table, include:

- `Summary`: 2-4 bullets with the overall review stance.
- `Open questions`: only questions that affect review confidence.
- `Skipped`: mention unavailable data, missing tools, or files not reviewed.

## Constraints

- Draft only: never call tools that publish comments, submit reviews, approve, request changes, or update PR/MR state.
- If no actionable issues are found, say so clearly and include remaining risk or test gaps.
- Keep comments specific to the diff unless surrounding code is required to validate behavior.
