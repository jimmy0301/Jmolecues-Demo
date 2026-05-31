# Changelog

All notable changes to this project are documented here.

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.
There are no release tags yet, so entries are grouped by commit date.

## 2026-05-31

### Added

- Added PR review workflow skills and related onboarding guidance.
- Added DTO boundary guidance to keep API request / response DTOs and OpenAPI generated models out of domain and application layers.
- Added expanded DDD / CQRS rules for aggregate invariants, transaction boundaries, event boundaries, command validation, listener retry, idempotency, concurrency, and ubiquitous language.
- Added idempotent projection and optimistic locking implementation examples in `ordering`.
- Added pessimistic locking guidance and examples, including MongoDB lease-lock semantics.
- Added annotation-first architecture rules for jMolecules and Spring Modulith metadata.
- Added Process Manager / Saga guidance and implementation example.
- Added WireMock testing guidance for external HTTP adapter contracts.
- Added feature toggle, migration, backward compatibility, and expand-and-contract migration guidance.
- Added Flamingock data migration guidance, including MongoDB index migration standards, rollback expectations, and testing requirements.

### Changed

- Reorganized the onboarding guide into smaller, focused chapters.
- Clarified and aligned the technical stack guidance across README, `AGENTS.md`, and `docs/02-tech-stack.md`.
- Synchronized `.codex/rules/`, `docs/`, `patterns/`, and checklist guidance with the latest DDD / CQRS / migration rules.

## 2026-05-23

### Added

- Added cross-Bounded Context data access patterns.

### Changed

- Reorganized onboarding documentation and adjusted guide content for newer team members.

## 2026-05-20

### Changed

- Updated domain event documentation to reflect the `AbstractAggregateRoot` event publishing pattern.

## 2026-05-19

### Added

- Added Testcontainers MongoDB integration tests for `catalog`, `customer`, and `ordering` repositories.
- Added Testcontainers guidance to the technical stack documentation.
- Added Conventional Commits enforcement through a `commit-msg` hook.

### Changed

- Migrated `Order` domain event publishing to the `AbstractAggregateRoot` pattern.

## 2026-05-17

### Added

- Added `@WebMvcTest` controller API tests.
- Updated documentation to explain controller slice testing expectations.

## 2026-05-16

### Added

- Added `POST /products` with `CreateProductCommand`.
- Added unit tests for domain, application, and shared layers.
- Added unit testing guidance and updated the technical stack documentation.
- Added skills documentation for team onboarding and agent-assisted development.
- Added expected failure tables for architecture and modularity tests.

### Changed

- Converted architecture diagrams to Mermaid.
- Updated technical stack documentation for OpenAPI Generator.
- Updated CQRS and Onion Architecture implementation docs to match actual package structure.
- Synchronized project-local agent guidance with actual bounded context structure.
- Reworked README into a team onboarding guide with a clearer learning path.
- Updated bounded-context and DDD annotation rules to reflect the Reference Object pattern.

### Fixed

- Fixed documentation consistency issues across learning path, diagrams, package examples, and expected failure mappings.
- Fixed skill template issues.

### Maintenance

- Added automatic documentation synchronization rules for architecture, package, and rule changes.

## 2026-05-15

### Added

- Added OpenAPI YAML specs and OpenAPI Generator Maven Plugin.
- Added generated API flow with QueryModels and customer application layer.
- Added REST controllers for product, customer, and order APIs.
- Added `@InfrastructureRing` to controller packages.
- Added Bounded Context documentation for cross-context reference patterns.

### Changed

- Generalized skills and fixed domain event handler guidance.
- Moved identifiers to their owning Bounded Contexts and introduced `CustomerReference` / `ProductReference`.
- Merged bounded context documentation into the DDD chapter.
- Clarified Shared Kernel guidance for `CustomerId` and `ProductId`.

### Maintenance

- Cleaned up skill usage documentation and task tracking.

## 2026-05-14

### Changed

- Added DDD onboarding documentation and Shared Kernel guidance.
- Generalized architecture rules for reuse.

## 2026-05-11

### Added

- Added CQRS violation demos and ArchUnit rules.
- Added full CQRS and Onion Architecture rule coverage.
- Added test summary documentation.

### Fixed

- Replaced deprecated jMolecules CQRS annotation package usage.

## 2026-05-10

### Added

- Added the initial jMolecules DDD demo with Onion Architecture.
- Added initial PRD, domain model, and ADR 001.
- Added jMolecules CQRS architecture.
- Added initial technical stack documentation.

### Changed

- Replaced early ASCII diagrams with Mermaid diagrams.
- Fixed context map and state diagram rendering issues.
