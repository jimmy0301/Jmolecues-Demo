# 2026-05-31：Annotation-First Architecture Tests

## Status

Accepted

## Context

本專案使用 DDD、CQRS 與 Onion Architecture 規範 package 邊界。
package 名稱是重要慣例，但不應成為架構測試唯一依據；未來若調整資料夾命名，架構規則仍應能靠明確 metadata 判斷角色與邊界。

## Decision

架構測試優先使用 annotation 與 package metadata 判斷角色：

- DDD building block 以 `@AggregateRoot`、`@Entity`、`@ValueObject`、`@DomainEvent` 判斷
- Application ring 以 package-info 的 `@ApplicationServiceRing` 判斷
- Command handler 所在層以 declaring package 是否標記 `@ApplicationServiceRing` 判斷
- Command 所在層以 class package 是否標記 `@ApplicationServiceRing` 判斷
- API DTO / generated model 這類沒有穩定 annotation 的外部契約，用集中設定的 package pattern 判斷

package name 仍可作為 convention，但測試不把 `application`、`domain`、`infrastructure` 等資料夾名稱當成唯一真相。

## Consequences

正面影響：

- package rename 時，只要 package-info annotation 正確，架構測試不需大幅改寫
- 新增 package 時，必須明確標記 ring metadata，邊界更清楚
- DTO / generated package pattern 集中設定，未來調整成本較低

成本與限制：

- 每個 ring package 需要維護 `package-info.java`
- 沒有 annotation 的外部契約仍需少量 package pattern
- 若新增新的 generated DTO 位置，需同步更新 architecture test 的 pattern 設定
