# Spec: xq-terminal

**Status:** Active — buildable plan for Satellite `xq-qe-box`.

**Related:** [`docs/ideas/xq-terminal.md`](../ideas/xq-terminal.md) · [`docs/specs/xq-qe-box.md`](xq-qe-box.md) · Hub [`quality/`](../../quality/README.md) · quality skills in `xq-qe-box`

## Problem

XQ needs a TAP-like **admission gate**: given a shippable **asset**, decide whether it may **merge** or **release**, based on small/medium evidence and (for release) large tests in a sandbox against the real artifact. Today policy lives in `quality/` and skills, but there is no single **runner/controller** that agents and CI call for a clear qualified / not outcome.

## Solution

Ship **`xq-terminal`** inside **`xq-qe-box`**: a CLI that is both **runner** and **controller**. Metaphor: airport Terminal — check-in → validate passport → board merge plane or release plane.

```
                    passport.json (CI artifact)
                              │
                              ▼
┌─────────────────────────────────────────────────────────┐
│  xq-terminal (xq-qe-box)                                │
│  board --gate merge|release --asset --sha --reports …   │
└─────────────┬─────────────────────────────┬─────────────┘
              │                             │
     merge: scan passport            release: passport OK
     failed==0 (quarantine OK)              │
              │                             ▼
              │                    stub sandbox (v1)
              │                    install ipa|apk|service
              │                             │
              │                             ▼
              │                    large tests → qualify
              ▼                             ▼
         JSON: qualified | not qualified + reasons
```

### Gates

| Gate | Name | Requires | Decision |
| --- | --- | --- | --- |
| **merge** | Merge plane | Passport for small+medium | `failed == 0` (quarantined allowed) |
| **release** | Release plane | Passport OK **+** shippable artifact **+** sandbox large tests | Passport rule, then sandbox run green |

`passRatio` (`passed / dryRunTotal`) is **informational** — not a soft pass for real failures.

### Passport contract

Produced by **Satellite CI** after small/medium (planned inventory = dry-run), published as a **build artifact** (e.g. `dist/quality/passport.json`). Not committed as living truth. Terminal **only scans**; it does not invent the passport.

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

- `dryRunTotal` = tests **discovered/planned** before execute (inventory).
- Accounting: `passed + failed + quarantined == dryRunTotal` (suite- and asset-level).
- `passRatio = passed / dryRunTotal`.
- Quarantined suites/cases need owner + expiry when status is `quarantined` (align `quality-controlling`).
- Any `failed` (non-quarantined) → **not qualified**.

### CLI contract

Binary: `xq-terminal` (name fixed for v1). Env prefix: `XQ_TERMINAL_*` as needed.

```bash
xq-terminal board \
  --asset <id> \
  --gate merge|release \
  --sha <git-sha> \
  --reports <dir-or-passport.json> \
  [--artifact <path-or-ref>]
```

- **merge:** `--artifact` optional; passport required.
- **release:** `--artifact` required (`ipa` / `apk` / service image ref).

Stdout: agent-native JSON, e.g.:

```json
{
  "ok": true,
  "qualified": true,
  "gate": "merge",
  "asset": "ios-app",
  "sha": "...",
  "passRatio": 0.9167,
  "reasons": []
}
```

Non-zero exit when not qualified or usage/runtime error (stable exit codes TBD in implementation Ticket).

### Sandbox platform (v1 stub)

Lives **in `xq-qe-box`** beside the CLI (shared org platform boundary; not per-Satellite compose as the product).

v1:

- **Stub/fake provisioner** — accepts artifact + asset id, returns a sandbox handle / local fake endpoints.
- Runs **large** suite entrypoint declared for the asset (convention TBD in first Ticket: config file or flags).
- Later Tickets: real DO/K8s/device-lab backends behind the same provision API.

Terminal **orchestrates** sandbox; does not embed DeviceKit install (still ADR-0001 / `xq-motest` for device control when large tests need it).

### Layout in xq-qe-box

```
xq-qe-box/
├── cli/xq-terminal/          # NEW — board controller + passport scan
├── packages/sandbox/         # NEW — stub provisioner (v1); real backends later
├── skills/xq-terminal/       # NEW — agent skill for board flow
├── cli/xq-motest/            # existing
└── skills/quality-*/         # existing — policy language Terminal enforces
```

### Skills

```bash
gh skill install ExperienceQuality/xq-qe-box xq-terminal
```

Skill: load Hub quality principles (vendored or via `quality-principles`), run `xq-terminal board`, interpret JSON. Do not reimplement passport rules in prose only.

## Out of scope (v1)

- Cloning Google TAP / Forge / monorepo CB
- Durable passport committed in Satellite git as source of truth
- Soft-qualify on `passRatio` while `failed > 0`
- Full production sandbox (real multi-tenant K8s) — stub only
- New Satellite solely for Terminal (keep in `xq-qe-box` until it outgrows)
- Replacing `xq-motest` or DeviceKit ownership

## Acceptance (Spec-level)

- [ ] `cli/xq-terminal` boards **merge** from a fixture passport (fail/quarantine/pass cases)
- [ ] Accounting enforced (`dryRunTotal` vs passed/failed/quarantined)
- [ ] **release** path calls stub sandbox with `--artifact` and returns qualified/not
- [ ] Skill `xq-terminal` installable via `gh skill`
- [ ] Hub Idea collapsed; this Spec linked from `xq-qe-box` Spec
- [ ] Tracer-bullet Tickets filed on Hub with `satellite:xq-qe-box`

## Tracer-bullet Tickets (suggested)

1. Passport schema + `board --gate merge` (fixture-driven)
2. Skill `xq-terminal` + README
3. Stub sandbox package + `board --gate release`
4. Wire one Satellite CI to emit `passport.json` artifact
5. Real sandbox backend (later Spec slice)
