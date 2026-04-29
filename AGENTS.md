# AGENTS.md


## Purpose

This repository contains the custom **Practice** system for the Hera network.

Scope:
- Velocity proxy plugin
- Practice hub plugin
- Practice match plugin
- Practice FFA plugin
- shared domain/data modules

Out of scope:
- other game modes
- public web/API layer

---

## Non negotiables

- I want the project to have as little over-engineering as possible. We want something lightweight, functional, simple, scalable, easy to maintain, auditable, and debuggable — meaning we shouldn't introduce unnecessary complexity that will cause us pain down the line. That said, this doesn't mean we're going to do things poorly — everything must have the best performance and efficiency, each component must fulfill its responsibilities correctly, and we want the fewest bugs and errors possible. No performance issues, no inefficiencies — we want something ultra-performant.'
- Remember that if you need information about anything, we have a docs/ directory where you'll find documentation for everything you might need regarding PaperMC, Velocity, zMenu, scoreboard-library — that's what's available for now. So if you need any information to improve your output and do things correctly by following the docs, that would be great. Whenever you need to do something and you're not sure how to handle it at the level of these dependencies, go to docs/ and you'll find what you're looking for. Also, everything is fragmented — this is very important — DO NOT read an entire file all at once, it's fragmented so you don't waste tokens and context reading things you don't need for what you're currently working on.

---

## Current Architecture

Runtime topology:

```text
Velocity Proxy
  -> Practice Hub (Paper)
  -> Practice Match servers (Paper)
  -> Practice FFA servers (Paper)

Shared infrastructure:
- MySQL = source of truth
- Redis = hot state / cache / queues
```

Design goal:
- simple initial deployment
- horizontal scaling without redesign
- clear separation of responsibilities

---

## Module Layout

Internal modules:
- `prismapractice-api` -> shared contracts/types
- `prismapractice-core` -> domain logic
- `prismapractice-data` -> MySQL/Redis access

Deployable plugins:
- `prismapractice-proxy` -> Velocity
- `prismapractice-hub` -> Paper hub
- `prismapractice-match` -> Paper match servers
- `prismapractice-ffa` -> Paper FFA servers

Dependency direction:

```text
prismapractice-api
prismapractice-core -> prismapractice-api
prismapractice-data -> prismapractice-api + prismapractice-core

prismapractice-proxy -> prismapractice-api + prismapractice-core
prismapractice-hub   -> prismapractice-api + prismapractice-core + prismapractice-data
prismapractice-match -> prismapractice-api + prismapractice-core + prismapractice-data
prismapractice-ffa   -> prismapractice-api + prismapractice-core + prismapractice-data
```

Rules:
- keep `prismapractice-api`, `prismapractice-core`, `prismapractice-data` free of Paper/Velocity APIs when possible
- Paper/Velocity integration belongs in plugin modules only
- root project is an aggregator only; do not recreate `src/` at repository root

---

## Technology Decisions

### Java
- **Java 25**

Why:
- Paper 26.1+ requires Java 25
- Velocity 3.5+ supports Java 21+
- Java 25 is the safe common baseline

### Build System
- **Gradle Kotlin DSL**
- multi-module workspace
- single IntelliJ project

### Packaging
- deployable plugins use **Shadow**
- final jars are written to root `target/` Only plugins that go on the servers, the rest on `target-internal/`


Why:
- plugin jars need shared internal classes bundled
- one output directory is easier for testing/deployment

---

## Approved Dependencies

### Shared infrastructure
- **HikariCP** -> JDBC connection pooling
- **MySQL Connector/J** -> MySQL driver
- **Lettuce** -> Redis client

### Proxy
- **velocity-api** -> Velocity plugin API

### Paper plugins
- **paper-api** -> Paper plugin API
- **PlaceholderAPI** (optional runtime integration)
- **zMenu API** -> inventory/menu UI layer
- **Simple Voice Chat API** -> optional voice integration hooks
- **scoreboard-library** -> sidebar / team / objective infrastructure

### Testing
- **JUnit 5** -> base test framework
- **Mockito** -> mock dependencies for domain/unit test
---

## Why These UI/UX Choices

### zMenu
Use zMenu as a **UI layer only**.

Good use:
- open menus
- define buttons/layout/config
- trigger application services

Bad use:
- queue logic in menu config
- elo logic in menu config
- state transitions in menu config

Rule:
- menu click -> call service from our code

### scoreboard-library
Use it for scoreboard infrastructure only.

Good use:
- sidebar rendering
- teams
- objectives

Bad use:
- domain decisions
- gameplay state ownership

Rule:
- library renders
- our core decides what to show

---

## Runtime Responsibilities

### `prismapractice-proxy`
Owns:
- transfers between servers
- routing / fallback
- proxy-side bootstrap

Does not own:
- elo
- match rules
- persistence

### `prismapractice-hub`
Owns:
- menus
- queue entry/exit UX
- hub scoreboard/chat/tab
- social entry point for Practice

### `prismapractice-match`
Owns:
- duel lifecycle
- arena allocation/usage
- kit application
- result handling

### `prismapractice-ffa`
Owns:
- free-for-all combat loop
- respawn / streak / FFA-specific state

---

## Data Strategy

### MySQL (source of truth)
Store:
- player profile
- elo/mmr per kit
- wins/losses
- match history
- seasons
- competitive punishments

### Redis (hot state)
Store:
- queue state
- current server presence
- current match state
- cache
- cooldowns/locks
- leaderboard projections

Rule:
- no critical persistence in local YAML files
- no heavy MySQL queries in combat-critical code paths

---

## Testing Strategy

### 1. Unit tests
Use for:
- `prismapractice-api`
- `prismapractice-core`
- parts of `prismapractice-data`
- parts of `prismapractice-proxy`

Tools:
- JUnit 5
- Mockito

Why:
- fastest feedback
- best for domain rules

### Build/Test flow
- `test` -> runs automated tests only
- `build` -> compiles, runs tests, packages jars
- 
Correct workflow:
1. write/change code
2. run targeted `test`
3. run `build` before considering work complete
---

## Build Output

Primary commands:

```powershell
./gradlew.bat test
./gradlew.bat build
```

All jars are emitted to:

```text
target/
```

Deployable jars:
- `prismapractice-proxy-<version>.jar`
- `prismapractice-hub-<version>.jar`
- `prismapractice-match-<version>.jar`
- `prismapractice-ffa-<version>.jar`

Shared module jars in `target/` are development artifacts, not plugin deployment units.

---

## Current Code Conventions

- base package: `com.stephanofer.prismapractice.*`
- no camelCase package names like `practiceHub`
- root project must not contain runtime code
- keep logs clear on plugin enable/disable
- prefer explicit small services over giant managers

---

## Anti-Patterns to Avoid

- one giant plugin for everything
- mixing hub, match and FFA logic in one runtime
- putting domain logic into zMenu config
- using scoreboard library as state owner
- using plugin messaging as the main distributed backbone
- forcing voice chat groups too early
- leaking Paper or Velocity APIs into shared domain modules

---

## Current Important Files

- `build.gradle.kts` -> shared Gradle configuration
- `gradle.properties` -> centralized versions and build properties
- `settings.gradle.kts` -> module registration
- `prismapractice-*/build.gradle.kts` -> per-module dependencies and packaging

---

## If You Change the Stack

When changing major architectural decisions, update this file.

Examples:
- switching `plugin.yml` to `paper-plugin.yml`
- replacing MySQL
- changing test strategy

Keep this document concise, current, and operational.
