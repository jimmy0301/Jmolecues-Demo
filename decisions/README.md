# Architecture Decisions

這個目錄保存專案中的架構決策記錄。
當團隊做出會影響多人、長期維護、技術選型或架構邊界的決定時，應在這裡補一筆 decision record。

---

## 何時需要新增 decision

- 選擇或更換主要技術，例如資料庫、框架、通訊方式
- 決定 DDD / CQRS / Onion Architecture 的例外策略
- 決定跨 Context 整合方式或公開合約形狀
- 決定併發控制策略，例如 optimistic locking、lease lock、retry / conflict response
- 決定違反一般規則但有業務理由的例外做法

## 建議格式

```md
# YYYY-MM-DD：決策標題

## Status

Accepted / Superseded / Deprecated

## Context

為什麼需要做這個決策？有哪些限制？

## Decision

決定採用什麼做法？

## Consequences

這個決策帶來哪些好處、成本與後續約束？
```

## 與 `docs/adr/` 的關係

`docs/adr/` 已有早期架構決策文件。
新的決策可以放在 `decisions/`。若未來要統一目錄，再一次搬移與修正連結。
