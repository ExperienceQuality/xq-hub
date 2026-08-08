# Spec: xq-runner-sdk

**Status:** Active — buildable plan for Satellite `xq-runner-sdk`.

**Related:** [`docs/specs/xq-terminal.md`](xq-terminal.md) · [`docs/specs/xq-qe-box.md`](xq-qe-box.md)

## Problem

`xq-terminal` (Runner) and remote Spec fat JARs must share a stable **Java** contract (`RunnerSpec`, context, result) without putting that API inside the QE monorepo or making Specs a Runner build dependency. The contract needs its own publishable artifact and version line.

## Solution

Ship Satellite **`xq-runner-sdk`** (`ExperienceQuality/xq-runner-sdk`) as a **Java 17+ library** — interfaces and records only (no CLI, no ClassLoader, no passport logic).

| Layer | Choice |
| --- | --- |
| Language | **Java 17+** (interfaces/records; no Kotlin in the SDK) |
| Build | **Gradle** (Java library) |
| Coordinates | `com.experiencequality.runner:runner-sdk:<version>` |
| Publish (v1) | **GitHub Packages** (Maven) — library consumers use normal Gradle deps |
| Consumers | Runner (`implementation`); Specs (`compileOnly`, exclude from fat JAR) |

**Not this Satellite:** Terminal CLI, sandbox, passport models, Spec plugins, Python/Node loaders.

### API (v1)

```java
package com.experiencequality.runner.sdk;

public interface RunnerSpec {
    String name();
    SpecResult run(SpecContext context);
}

public record SpecContext(
    String environment,
    String runId,
    String asset,
    String sha,
    String gate,
    String artifactRef
) {}

public record SpecResult(
    boolean success,
    String message
) {}
```

ServiceLoader registration lives in **Spec** projects (`META-INF/services/...`), not in this SDK.

### Layout

```
xq-runner-sdk/
├── CONTEXT.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
└── src/main/java/com/experiencequality/runner/sdk/
    ├── RunnerSpec.java
    ├── SpecContext.java
    └── SpecResult.java
```

## Out of scope

- Picocli / board CLI
- URL+sha256 Spec loader
- Passport JSON schema
- Example payment Spec (Ticket on `xq-qe-box` / example path)

## Acceptance

- [ ] Repo `ExperienceQuality/xq-runner-sdk` exists; catalogue + `satellite:xq-runner-sdk` label
- [ ] Java API matches above; `./gradlew build` green
- [ ] Documented consume path: Runner `implementation`, Spec `compileOnly`
- [ ] First version publishable (GitHub Packages or documented `mavenLocal` for bootstrap)
- [ ] `xq-terminal` Spec points here (not `xq-qe-box/packages/runner-sdk`)

## Tracer-bullet Tickets

1. [#23](https://github.com/ExperienceQuality/xq-hub/issues/23) — Bootstrap Satellite + Java API + publish
2. Consumers: Terminal (#17+) and example Spec (#22) depend on published SDK (blocked by #23)
