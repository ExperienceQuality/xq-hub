# Spec: xq-terminal

**Status:** Active — **Python-only** buildable plan.

**Related:** [`docs/ideas/xq-terminal.md`](../ideas/xq-terminal.md) · [`docs/specs/xq-qe-box.md`](xq-qe-box.md) · [`docs/specs/xq-terminal-sdk.md`](xq-terminal-sdk.md) · [`docs/specs/xq-terminal-registry.md`](xq-terminal-registry.md) · Hub [`quality/`](../../quality/README.md)

**Pivot:** JVM / fat JAR / ClassLoader / Release URL+sha256 Spec loading is **descoped**. Product path is Python wheels + registry meta-package.

## Problem

XQ needs a TAP-like **admission gate**: given a shippable **asset**, decide whether it may **merge** or **release**, based on small/medium evidence and (for release) large tests in a sandbox against the real artifact. Asset-specific large/release logic must be addable without rebuilding Terminal or putting Spec dependency trees into the Runner.

## Solution

Ship **`xq-terminal`** (Python CLI) in **`xq-qe-box`**. Metaphor: airport Terminal — passport → board merge or release plane.

**Tech stack (locked — Python only):**

| Layer | Choice |
| --- | --- |
| Language | **Python 3.11+** |
| Tooling | **uv** + `pyproject.toml` |
| CLI | Thin edge (**Typer** recommended) → services |
| Passport models | **Pydantic** |
| Spec protocol | Satellite **`xq-terminal-sdk`** (Python package) |
| Spec plugins | Separate **wheels** (`login-spec`, `payment-spec`, …) + entry points |
| Spec intake | Satellite **`xq-terminal-registry`**: authors register in **YAML** → CI **sanitizes** → generates registry `pyproject.toml` deps → publishes meta-package |
| Terminal deps | **`xq-terminal-registry`** only (pinned version) for Specs — not individual Spec wheels |
| Distribution | Installable CLI (`uv tool` / pipx / `pip install`) |

**Not used:** JVM, Gradle, ClassLoader, ServiceLoader, fat JAR Specs, Maven/`--spec-url`+sha256 Spec load path. (`xq-motest` stays Swift.)

```
                    passport.json (CI artifact)
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│  xq-terminal (Python)                                   │
│  board --gate merge|release --asset --sha --reports …   │
│  [release] get_spec(id) from xq-terminal-registry       │
└─────────────┬─────────────────────────────┬─────────────┘
              │                             │
     merge: scan passport            release: passport OK
     failed==0 (quarantine OK)              │
              │                             ▼
              │                    stub sandbox (v1)
              │                    + registry.get_spec(…).run(ctx)
              ▼                             ▼
         JSON: qualified | not qualified + reasons
```

### Product gates

| Gate | Requires | Decision |
| --- | --- | --- |
| **merge** | Passport (small+medium) | `failed == 0` (quarantined allowed) |
| **release** | Passport OK **+** artifact **+** sandbox + Spec via registry | Passport rule, then Spec green |

`passRatio` is **informational** only.

---

## Architecture

### Repos / packages

| Piece | Satellite / home | Role |
| --- | --- | --- |
| Protocol | [`xq-terminal-sdk`](xq-terminal-sdk.md) | `RunnerSpec` / `SpecContext` / `SpecResult` |
| Registry | [`xq-terminal-registry`](xq-terminal-registry.md) | YAML sanitize → generated deps → meta-wheel |
| Specs | N repos (or packages) | Publish wheels; entry point `xq_terminal.specs` |
| Runner | `xq-qe-box` / `cli/xq-terminal` | `board` CLI; depends on registry (+ sdk as needed) |

```
xq-terminal-sdk/                 # Python protocol wheel
xq-terminal-registry/            # specs.yaml → sanitize → pyproject deps → get_spec()
login-spec/, payment-spec/, …    # Spec wheels (dependencies of registry)

xq-qe-box/
├── cli/xq-terminal/             # Python Terminal (Typer → services)
├── packages/sandbox/            # stub provisioner (v1)
├── skills/xq-terminal/
├── cli/xq-motest/               # Swift (unchanged)
└── skills/quality-*/
```

### Core principle

> Specs are **not** Terminal dependencies. They are **registry** dependencies (after YAML sanitize). Terminal imports the registry only.

```text
WRONG:  xq-terminal pyproject depends on payment-spec
RIGHT:  specs.yaml → sanitize → xq-terminal-registry depends on payment-spec==…
        xq-terminal depends on xq-terminal-registry==…
```

### Release Spec selection

```bash
xq-terminal board \
  --asset <id> \
  --gate release \
  --sha <git-sha> \
  --reports <passport.json> \
  --artifact <path-or-ref> \
  --spec <spec-id>              # e.g. payment-spec
  # optional: --registry-version pin already in Terminal env/install
```

Terminal: `from xq_terminal_registry import get_spec` → `get_spec(spec_id).run(ctx)`.

### Passport contract

Unchanged schema vs prior Spec — Satellite CI artifact; Terminal only scans.

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
  "coverage": { "passRatio": 0.9167 },
  "suites": []
}
```

Rules: `passed + failed + quarantined == dryRunTotal`; qualify iff `failed == 0`; quarantined needs owner + expiry when used.

### CLI contract

Env: `XQ_TERMINAL_*`.

```bash
xq-terminal board \
  --asset <id> \
  --gate merge|release \
  --sha <git-sha> \
  --reports <dir-or-passport.json> \
  [--artifact <path-or-ref>] \
  [--spec <spec-id>]
```

- **merge:** passport only.  
- **release:** `--artifact` + `--spec` required (v1).  

Stdout: agent-native JSON (`qualified`, `passRatio`, `reasons`, Spec message).

### Sandbox (v1 stub)

Stub provisioner in-box; then `get_spec(…).run(ctx)`. Real backends later. DeviceKit / `xq-motest` unchanged (ADR-0001).

### Skills

```bash
gh skill install ExperienceQuality/xq-qe-box xq-terminal
```

### Spec author flow

```text
implement Spec (sdk protocol) → publish wheel + entry point
  → PR pin into registry specs.yaml
  → sanitize CI → registry release
  → Terminal (already on registry) can --spec <id>
```

## Out of scope (v1)

- JVM / Kotlin Terminal or Specs  
- Per-board `--spec-url` / sha256 ClassLoader path  
- Soft-qualify on `passRatio` while `failed > 0`  
- Full multi-tenant sandbox  
- Replacing `xq-motest`  
- Authors editing registry `pyproject.toml` by hand  

## Acceptance

- [ ] Python `xq-terminal-sdk` protocol published  
- [ ] Registry: YAML → sanitize → generated deps → `get_spec`  
- [ ] `board --gate merge` fixtures (pass / fail / quarantine / bad accounting)  
- [ ] `board --gate release` with stub sandbox + `--spec` via registry  
- [ ] Terminal does **not** depend on individual Spec wheels  
- [ ] Skill installable via `gh skill`  
- [x] Pivot from JVM documented; Python registry Idea collapsed into Specs  

## Tracer-bullet Tickets

1. [#24](https://github.com/ExperienceQuality/xq-hub/issues/24) — Python `xq-terminal-sdk` rewrite (supersedes Java #23)  
2. [#17](https://github.com/ExperienceQuality/xq-hub/issues/17) — passport + `board --gate merge`  
3. [#25](https://github.com/ExperienceQuality/xq-hub/issues/25) — Registry YAML sanitize + meta-package  
4. [#22](https://github.com/ExperienceQuality/xq-hub/issues/22) — example Spec wheel (`login-spec` or `payment-spec`)  
5. [#19](https://github.com/ExperienceQuality/xq-hub/issues/19) — stub sandbox + release via `get_spec`  
6. [#18](https://github.com/ExperienceQuality/xq-hub/issues/18) — skill + README  
7. [#20](https://github.com/ExperienceQuality/xq-hub/issues/20) — CI emits passport  
8. [#21](https://github.com/ExperienceQuality/xq-hub/issues/21) — real sandbox (later)  
