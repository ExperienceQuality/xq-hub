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
| Language | **JVM 17+** — `runner-sdk` as **Java** interfaces; Runner CLI **Java or Kotlin** |
| Build | **Gradle** + Shadow (or equivalent) for Spec fat JARs |
| CLI | Thin CLI edge (**Picocli** recommended) → services |
| Distribution | **JVM app** (`installDist` / `run`) — **not** native binary |
| JSON / passport | **Jackson** |
| Plugin model | Shared **`runner-sdk`** + remote Spec fat JARs + **ServiceLoader** + isolated **ClassLoader** |
| Spec publish | **GitHub Release asset** (fat JAR URL) + **sha256** pin — not Maven/GitHub Packages for v1 |

**Not used:** Python / uv / Fire / httpimport for the Terminal core; Maven coordinate resolve / GitHub Packages Maven registry as the Spec load path. (`xq-motest` stays Swift.)

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
│       └── adapters/             # PassportFileAdapter, StubSandbox, Url+Sha256 resolver, ClassLoader
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
| **Runner** (`xq-terminal`) | `runner-sdk`, URL+sha256 fetch/cache, ClassLoader, ServiceLoader, board/passport | Specs as `implementation` deps; Maven registry resolve |
| **Spec** (e.g. payment-spec) | Implements `RunnerSpec`, owns OkHttp/Jackson/…, builds fat JAR, publishes as **Release asset** | Coupling into Runner `build.gradle` |

```
Spec project                         Runner project
------------                         --------------
runner-sdk (compileOnly)             runner-sdk
okhttp, jackson, …                  plugin loader
     | build fat JAR                 URL + sha256 resolver
     v
payment-spec-all.jar
     | attach to GitHub Release
     v
https://github.com/…/releases/download/v1.2.3/payment-spec-all.jar
     | GET → sha256 verify → cache
     v
Runner → ClassLoader → ServiceLoader → RunnerSpec.run()
```

**Do not** add Specs to the Runner:

```kotlin
// WRONG
implementation("com.myorg.specs:payment-spec:1.4.0")
```

**Locked Spec pin (v1):** URL + sha256 only. No `--spec` Maven coordinates.

```text
xq-terminal board … \
  --spec-url https://github.com/org/repo/releases/download/v1.2.3/payment-spec-all.jar \
  --spec-sha256 <hex>
```

Optional Spec Index entry (config or future index file):

```json
{
  "url": "https://github.com/org/repo/releases/download/v1.2.3/payment-spec-all.jar",
  "sha256": "<hex>"
}
```

`file://` URLs allowed for local/dev; sha256 still required unless a documented test-only escape hatch.

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

Production load path: **`--spec-url` + `--spec-sha256`** → download (or cache hit) → **verify SHA-256** (fail closed on mismatch) → `URLClassLoader` from local file → `ServiceLoader.load(RunnerSpec.class, loader)`.

Cache key: sha256 (content-addressed under e.g. `~/.cache/xq-terminal/specs/<sha256>/…`). Never load a JAR whose digest does not match the pin.

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

Distribution: **JVM app** via Gradle Application plugin (`installDist` / `run`) — script launcher `xq-terminal` on PATH after install. Requires a JDK/JRE at runtime.

**Descoped:** native OS binary (GraalVM Native Image, Kotlin/Native, jlink/jpackage as a product requirement). Dynamic Spec `ClassLoader` loading is incompatible with a closed-world native image; do not pursue a single static binary in v1.

Env: `XQ_TERMINAL_*`.

```bash
xq-terminal board \
  --asset <id> \
  --gate merge|release \
  --sha <git-sha> \
  --reports <dir-or-passport.json> \
  [--artifact <path-or-ref>] \
  [--spec-url <jar-url> --spec-sha256 <hex>]
```

- **merge:** passport required; Spec flags unused.
- **release:** `--artifact` required; `--spec-url` **and** `--spec-sha256` required for Spec load (v1).

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
implement RunnerSpec → add deps → fat JAR → GitHub Release asset + publish sha256 → pin in CI/index
```

No Runner rebuild / no Spec `implementation` in Runner Gradle / no ServiceLoader hardcoding of Spec classes / no Maven Packages resolve in the Runner.

## Out of scope (v1)

- Cloning Google TAP / Forge
- Python Terminal core
- Soft-qualify on `passRatio` while `failed > 0`
- Full multi-tenant sandbox
- New Satellite solely for Terminal
- Replacing `xq-motest`
- In-process loading of Specs without **URL + sha256** pin/cache/verify
- Maven / GitHub Packages as Spec resolve path (v1)
- **Native binary** shipping (GraalVM Native Image, Kotlin/Native, or jpackage as a v1 deliverable) — JVM distribution only

## Acceptance (Spec-level)

- [ ] `runner-sdk` published/consumed; example Spec fat JAR loads via ServiceLoader from Runner
- [ ] `xq-terminal board --gate merge` fixture passport (pass / fail / quarantine / bad accounting)
- [ ] Accounting enforced
- [ ] `board --gate release` with stub sandbox + `--spec-url` + `--spec-sha256` returns qualified/not; mismatch fails closed
- [ ] Specs are **not** Runner `implementation` dependencies
- [ ] Skill `xq-terminal` installable via `gh skill`
- [ ] Hub Idea collapsed; linked from `xq-qe-box` Spec
- [x] Tracer Tickets #17–#22 (JVM stack; Spec pin = Release URL + sha256)

## Tracer-bullet Tickets

1. [#17](https://github.com/ExperienceQuality/xq-hub/issues/17) — `runner-sdk` + passport models + `board --gate merge`
2. [#18](https://github.com/ExperienceQuality/xq-hub/issues/18) — Skill `xq-terminal` + README
3. [#19](https://github.com/ExperienceQuality/xq-hub/issues/19) — Stub sandbox + Spec ClassLoader load (`--spec-url` + `--spec-sha256`) + `board --gate release`
4. [#20](https://github.com/ExperienceQuality/xq-hub/issues/20) — Satellite CI emits `passport.json`
5. [#21](https://github.com/ExperienceQuality/xq-hub/issues/21) — Real sandbox backend (later)
6. [#22](https://github.com/ExperienceQuality/xq-hub/issues/22) — Example Spec fat JAR (ServiceLoader + GitHub Release + sha256)
