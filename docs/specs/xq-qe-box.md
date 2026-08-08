# Spec: xq-qe-box

**Status:** Active — Satellite bootstrap.

**Related:** [`docs/ideas/xq-qe-box.md`](../ideas/xq-qe-box.md), [`docs/ideas/agent-device-cli.md`](../ideas/agent-device-cli.md), [`docs/specs/xq-motest-deep-modules.md`](xq-motest-deep-modules.md), [`docs/specs/xq-terminal.md`](xq-terminal.md)

## Problem

Agents across XQ Satellites need a single, org-owned place for agent-native mobile QE: a **thin DeviceKit-direct CLI**, skills registry packaging, and optional upstream tools — without forcing the heavier agent-device daemon stack for every loop.

## Solution

Create Satellite **`xq-qe-box`** (`ExperienceQuality/xq-qe-box`) as a **monorepo** that owns the DeviceKit-direct CLI **`xq-motest`** (cloned from `xq-versastacks` `xq-ios-act-cli`), skills, and optional agent-device helpers. Primary path is thin host CLI → DeviceKit — not the agent-device daemon stack.

### Monorepo layout

```
xq-qe-box/
├── CONTEXT.md
├── README.md
├── cli/xq-motest/                   # Swift CLI → DeviceKit (from xq-ios-act)
├── cli/xq-terminal/                 # JVM Terminal / Spec Runner (see xq-terminal Spec)
├── packages/sandbox/                # Terminal sandbox provisioner (stub v1)
├── skills/
│   ├── xq-motest/                   # skill for DeviceKit-direct CLI
│   ├── xq-terminal/                 # skill for board / passport flow
│   ├── xq-mobile-auto-test/         # optional agent-device install + router
│   │   ├── SKILL.md
│   │   └── scripts/install-cli.sh
│   ├── quality-principles/          # Hub quality/ conformance pack
│   ├── quality-asset-strategy/
│   ├── quality-test-plan/
│   ├── quality-reporting/
│   └── quality-controlling/
└── packages/                        # reserved
```

**`xq-terminal-sdk`** lives in Satellite [`xq-terminal-sdk`](xq-terminal-sdk.md) — not under this monorepo. Terminal depends on it as a normal library; Specs `compileOnly`.

### Primary CLI: xq-motest

- Lives in `cli/xq-motest/`; binary `xq-motest`; env `XQ_MOTEST_*`.
- Talks **directly to DeviceKit** JSON-RPC (same shape as MobileCLI → DeviceKit).
- **Does not install** the DeviceKit runner — agent host infra must preinstall `.app` / `.ipa` on test devices (see `xq-motest-deep-modules` Spec + Satellite ADR-0001).
- Skill: `skills/xq-motest/`.
- Origin: see `cli/xq-motest/ORIGIN.md`.
- Architecture deepenings: [`xq-motest-deep-modules`](xq-motest-deep-modules.md).

### Optional: agent-device

`skills/xq-mobile-auto-test/scripts/install-cli.sh` still pins upstream `agent-device` for comparison or workflows that need its settle/`is` surface.

### Quality skills

Five skills under `skills/quality-*` enforce Hub [`quality/`](../../quality/README.md) (sizes, hermeticity, spot coverage). Each skill vendors the needed markdown under `references/` (no Hub download at runtime). Satellites install via `gh skill` and do not keep a quality doc binder.

### Terminal (TAP-like boarding)

[`xq-terminal` Spec](xq-terminal.md): CLI runner/controller that scans a Satellite CI **passport** (small/medium) for merge boarding, and for release boarding runs large tests via an in-box **sandbox** (stub v1) against `ipa`/`apk`/service artifacts. Shared plugin API: [`xq-terminal-sdk`](xq-terminal-sdk.md).

### Skills (`gh skill` / registry)

```bash
gh skill publish --dry-run
gh skill install ExperienceQuality/xq-qe-box xq-motest
gh skill install ExperienceQuality/xq-qe-box xq-mobile-auto-test
gh skill install ExperienceQuality/xq-qe-box quality-principles
gh skill install ExperienceQuality/xq-qe-box quality-asset-strategy
gh skill install ExperienceQuality/xq-qe-box quality-test-plan
gh skill install ExperienceQuality/xq-qe-box quality-reporting
gh skill install ExperienceQuality/xq-qe-box quality-controlling
```

### Hub bookkeeping

- Catalogue row + `satellite:xq-qe-box` label on this Hub.
- Tickets for later work (more skills, future CLI package, CI) target that label.

## Out of scope

- An `xq-qe` wrapper package/binary
- Replacing or forking `agent-device` internals (optional skill only)
- MCP / Node client packaging (CLI agent-native focus)
- Per-app CI matrices (later Tickets)
- Retiring `xq-ios-act` in versastacks (separate Ticket if desired)
- Shipping DeviceKit install/resign scripts inside `xq-motest` (infra owns runner provisioning)

## Acceptance (bootstrap)

- [x] Repo `ExperienceQuality/xq-qe-box` exists (public)
- [x] `cli/xq-motest` present (clone/rename from `xq-ios-act-cli`) + `skills/xq-motest`
- [x] Optional `skills/xq-mobile-auto-test` + agent-device install script; `gh skill publish --dry-run` clean
- [x] Hub catalogue + `satellite:xq-qe-box` label exist

## Tracer-bullet Tickets (suggested)

1. Bootstrap repo + skills (done)
2. Land `xq-motest` in `xq-qe-box` (this change)
3. Clone/improve DeviceKit for XQ search/assert gaps (later Spec)
4. Wire first product Satellite to `xq-motest`
5. Optional: retire or thin `xq-ios-act` in versastacks
