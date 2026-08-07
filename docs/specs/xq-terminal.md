# Spec: xq-terminal

**Status:** Active — buildable plan for Satellite `xq-qe-box`.

**Related:** [`docs/ideas/xq-terminal.md`](../ideas/xq-terminal.md) · [`docs/specs/xq-qe-box.md`](xq-qe-box.md) · Hub [`quality/`](../../quality/README.md) · quality skills in `xq-qe-box`

## Problem

XQ needs a TAP-like **admission gate**: given a shippable **asset**, decide whether it may **merge** or **release**, based on small/medium evidence and (for release) large tests in a sandbox against the real artifact. Today policy lives in `quality/` and skills, but there is no single **runner/controller** that agents and CI call for a clear qualified / not outcome.

Asset-specific large/release logic must be **addable without rebuilding the Terminal**: Specs are remote runtime plugins, not Runner build dependencies.

## Solution

Ship **`xq-terminal`** inside **`xq-qe-box`** as a **JVM Runner + controller CLI**. Metaphor: airport Terminal — check-in → validate passport → board merge plane or release plane.

**Tech stack (locked):**

| Layer | Choice |
| --- | --- |
| Language | **Java** (17+) |
| Build | **Gradle** + Shadow (or equivalent) for Spec fat JARs |
| CLI | Thin CLI edge (**Picocli** recommended) → services |
| JSON / passport | **Jackson** (Pydantic-role for the passport contract) |
| Plugin model | Shared **`runner-sdk`** + remote Spec fat JARs + **ServiceLoader** + isolated **URLClassLoader** |

**Not used:** Python / uv / Fire / httpimport for the Terminal core. (`xq-motest` stays Swift.)

```
                    passport.json (CI artifact)
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│  xq-terminal (JVM Runner)                               │
│  board --gate merge|release --asset --sha --reports …   │
│  [release] resolve Spec JAR → ClassLoader → ServiceLoader│
└─────────────┬─────────────────────────────┬─────────────┘
              │                             │
     merge: scan passport            release: passport OK
     failed==0 (quarantine OK)              │
              │                             ▼
              │                    stub sandbox (v1)
              │                    + load remote Spec fat JAR
              │                             │
              │                             ▼
              │                    RunnerSpec.run(context)
              ▼                             ▼
         JSON: qualified | not qualified + reasons
```

### Product gates (unchanged)

| Gate | Name | Requires | Decision |
| --- | --- | --- | --- |
| **merge** | Merge plane | Passport for small+medium | `failed == 0` (quarantined allowed) |
| **release** | Release plane | Passport OK **+** shippable artifact **+** sandbox + Spec run | Passport rule, then Spec/sandbox green |

`passRatio` (`passed / dryRunTotal`) is **informational** — not a soft pass for real failures.

---

## Architecture & design patterns

### Package layout (`cli` / `services` / `models` / `adapters`)

```
xq-qe-box/
├── packages/runner-sdk/          # shared API (RunnerSpec, SpecContext, SpecResult, …)
├── cli/xq-terminal/              # Runner CLI + board controller
│   └── src/main/java/.../
│       ├── cli/                  # Picocli commands only
│       ├── services/             # BoardService, SpecLoadService, …
│       ├── models/               # Passport, BoardResult (Jackson)
│       └── adapters/             # PassportFileAdapter, StubSandbox, Maven/URL resolver, ClassLoader
├── packages/sandbox/             # stub provisioner API (v1); real backends later
├── skills/xq-terminal/
├── cli/xq-motest/                # existing Swift
└── skills/quality-*/
```

**Patterns:**

| Concern | Pattern |
| --- | --- |
| CLI | Thin **command** edge → `BoardService` |
| Passport / board JSON | **Models** validated at boundary (Jackson) |
| Merge vs release | **Strategy** / gate branch on `BoardService` |
| Sandbox | **Port + adapter** (`SandboxPort`, `StubSandboxAdapter`) |
| Asset large tests | **Remote Spec plugin** (not Runner compile dependency) |
| Isolation | **One ClassLoader per Spec**; parent for `java.*` + `runner-sdk` API |

### Remote JVM Spec Runner (core principle)

> A **Spec** is a remote runtime plugin artifact, not a Runner build dependency.

Two roles:

| Role | Knows | Does not |
| --- | --- | --- |
| **Runner** (`xq-terminal`) | `runner-sdk`, artifact resolve, ClassLoader, ServiceLoader, board/passport | Individual Spec Maven coords as `implementation` deps |
| **Spec** (e.g. payment-spec) | Implements `RunnerSpec`, owns OkHttp/Jackson/…, builds fat JAR, publishes | Coupling into Runner `build.gradle` |

```
Spec project                         Runner project
------------                         --------------
runner-sdk (compileOnly)             runner-sdk
okhttp, jackson, …                  plugin loader
     | build fat JAR                 artifact resolver
     v
payment-spec-all.jar
     | publish
     v
Remote registry / URL
     | runtime resolution
     v
Runner → ClassLoader → ServiceLoader → RunnerSpec.run()
```

**Do not** add Specs to the Runner:

```kotlin
// WRONG
implementation("com.myorg.specs:payment-spec:1.4.0")
```

Pass at runtime:

```text
xq-terminal board … --spec com.myorg.specs:payment-spec:1.4.0
# or
xq-terminal board … --spec-url https://…/payment-spec-all.jar
```

### Shared contract (`runner-sdk`)

```java
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
    String artifactRef   // optional; set on release
) {}

public record SpecResult(
    boolean success,
    String message
) {}
```

Spec registers via ServiceLoader:

```text
META-INF/services/<RunnerSpec FQCN>
→ com.example.payment.PaymentSpec
```

Fat JAR includes Spec + its runtime deps; **exclude** `runner-sdk` types from the uber JAR (Runner provides them on the parent ClassLoader).

### ClassLoader isolation

```
Runner ClassLoader
└── runner-sdk
      ├── Spec ClassLoader A (PaymentSpec, Jackson 2, OkHttp 4)
      └── Spec ClassLoader B (LoginSpec, Jackson 3, OkHttp 5)
```

Delegate to parent: `java.*`, runner API packages. Prefer **child-first** for plugin classes/deps (custom ClassLoader; default `URLClassLoader` is parent-first and insufficient for conflicting deps).

Production load path: resolve → **download/cache + integrity check** → `URLClassLoader` from local file → `ServiceLoader.load(RunnerSpec.class, loader)`.

### Passport contract

Produced by **Satellite CI** after small/medium (planned inventory = dry-run), published as a **build artifact** (e.g. `dist/quality/passport.json`). Terminal **only scans**; it does not invent the passport.

Minimal schema (v1):

```json
{
  "asset": "ios-app",
  "sha": "<git-sha>",
  "gate": "merge",
  "generatedAt": "<iso8601>",
  "counts": {
    "dryRunTotal": 120,
    "passed": 110,
    "failed": 5,
    "quarantined": 5
  },
  "coverage": {
    "passRatio": 0.9167
  },
  "suites": [
    {
      "name": "UnitTests",
      "size": "small",
      "dryRunTotal": 80,
      "passed": 78,
      "failed": 1,
      "quarantined": 1,
      "status": "passed|failed|quarantined",
      "owner": "optional-if-quarantined",
      "expiresAt": "optional-iso8601"
    }
  ]
}
```

Rules:

- `dryRunTotal` = tests **discovered/planned** before execute.
- Accounting: `passed + failed + quarantined == dryRunTotal`.
- `passRatio = passed / dryRunTotal`.
- Quarantined needs owner + expiry when status is `quarantined`.
- Any non-quarantined `failed` → **not qualified**.

### CLI contract

Binary / distribution: `xq-terminal` (Gradle application or jlink/jpackage later). Env: `XQ_TERMINAL_*`.

```bash
xq-terminal board \
  --asset <id> \
  --gate merge|release \
  --sha <git-sha> \
  --reports <dir-or-passport.json> \
  [--artifact <path-or-ref>] \
  [--spec <maven-coord> | --spec-url <jar-url>]
```

- **merge:** passport required; `--spec` optional (usually unused).
- **release:** `--artifact` required; `--spec` / `--spec-url` required for large Spec invocation (v1).

Stdout: agent-native JSON (`qualified`, `passRatio`, `reasons`, Spec message when run).

### Sandbox platform (v1 stub)

In-box stub provisioner: accept artifact + asset → fake handle/endpoints; then invoke loaded `RunnerSpec`. Real DO/K8s/device-lab backends later behind the same port. DeviceKit install remains infra / `xq-motest` (ADR-0001).

### Skills

```bash
gh skill install ExperienceQuality/xq-qe-box xq-terminal
```

Skill: run `xq-terminal board`, interpret JSON; do not reimplement passport rules in prose.

### Developer experience (Specs)

```
implement RunnerSpec → add deps → fat JAR → publish
```

No Runner rebuild / no Spec `implementation` in Runner Gradle / no ServiceLoader hardcoding of Spec classes.

## Out of scope (v1)

- Cloning Google TAP / Forge
- Python Terminal core
- Soft-qualify on `passRatio` while `failed > 0`
- Full multi-tenant sandbox
- New Satellite solely for Terminal
- Replacing `xq-motest`
- In-process loading of untrusted Specs without pin/cache/integrity

## Acceptance (Spec-level)

- [ ] `runner-sdk` published/consumed; example Spec fat JAR loads via ServiceLoader from Runner
- [ ] `xq-terminal board --gate merge` fixture passport (pass / fail / quarantine / bad accounting)
- [ ] Accounting enforced
- [ ] `board --gate release` with stub sandbox + `--spec-url` (or coord) returns qualified/not
- [ ] Specs are **not** Runner `implementation` dependencies
- [ ] Skill `xq-terminal` installable via `gh skill`
- [ ] Hub Idea collapsed; linked from `xq-qe-box` Spec
- [x] Tracer Tickets #17–#21 (amend bodies for JVM stack)

## Tracer-bullet Tickets

1. [#17](https://github.com/ExperienceQuality/xq-hub/issues/17) — `runner-sdk` + passport models + `board --gate merge`
2. [#18](https://github.com/ExperienceQuality/xq-hub/issues/18) — Skill `xq-terminal` + README
3. [#19](https://github.com/ExperienceQuality/xq-hub/issues/19) — Stub sandbox + Spec ClassLoader load + `board --gate release`
4. [#20](https://github.com/ExperienceQuality/xq-hub/issues/20) — Satellite CI emits `passport.json`
5. [#21](https://github.com/ExperienceQuality/xq-hub/issues/21) — Real sandbox backend (later)
6. [#22](https://github.com/ExperienceQuality/xq-hub/issues/22) — Example Spec fat JAR (ServiceLoader + publish)
