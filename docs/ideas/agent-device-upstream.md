# Research: How to extend agent-device upstream

**Status:** Findings for Wayfinder [#5](https://github.com/ExperienceQuality/xq-hub/issues/5) (map [#3](https://github.com/ExperienceQuality/xq-hub/issues/3)).

**Question:** What is the viable path to extend [`callstack/agent-device`](https://github.com/callstack/agent-device) upstream for assert needs — contribution process, extension/plugin points (if any), prior art, and constraints that force a thin wrap instead?

**Primary sources only:** Callstack repo `CONTRIBUTING.md`, `AGENTS.md`, `CONTEXT.md`, ADRs, agent docs, issues/PRs, and first-party docs under https://oss.callstack.com/agent-device/. No invented plugin APIs.

**Related Hub notes:** [`agent-device-cli.md`](agent-device-cli.md) (agent-native CLI usage); XQ standing decision is upstream-first, no fork, no expect-file engine.

---

## Verdict

**Upstream is in-tree contribution, not a third-party plugin.** There is no documented external loader for commands, predicates, or assert engines. Assert improvements land as GitHub issues → PRs that extend the existing agent assert surface (`is`, `wait`, `get`/`find`, structured errors) via the in-repo `CommandDescriptor` / selector / predicate registries. Cloud **providers** and **PlatformPlugin** are real seams, but they own device/platform transport — not custom assertion logic — and still ship inside the Callstack monorepo behind one published `agent-device` npm package.

A **thin XQ wrap** is appropriate only when the need is orchestration or policy outside the CLI (compare multiple command outputs in CI, skill/pin policy), or when maintainers reject a product-shaped change that cannot be expressed as an honest extension of `is`/`wait`/error `details.reason`.

---

## Contribution process

### Setup and validation

[`CONTRIBUTING.md`](https://github.com/callstack/agent-device/blob/main/CONTRIBUTING.md) is the shortest path from checkout to a reviewable change:

- Node.js 22+, pnpm pinned by `package.json` `packageManager` (CI rejects other versions).
- `pnpm install` then `pnpm build`; native surfaces have separate build commands (Apple XCTest per OS, Android helper, macOS helper) — no catch-all `build:all` for day-to-day work.
- Iterate with `pnpm check:quick` / focused Vitest; before push run `pnpm check:affected --run` (GitHub CI remains authoritative for device/toolchain lanes).
- Guidelines: minimal deps, preserve compact agent-friendly JSON, explicit session open/close in tests, integration coverage when adding/changing commands or wire responses.

[`AGENTS.md`](https://github.com/callstack/agent-device/blob/main/AGENTS.md) adds contributor invariants: push only behind `pnpm check:affected --run && git push`; keep scope to one command family unless explicitly cross-cutting; versioned CLI help is the agent-facing source of truth over prose.

### Issues first

Workflow lives on GitHub issues ([`docs/agents/issue-tracker.md`](https://github.com/callstack/agent-device/blob/main/docs/agents/issue-tracker.md)):

- Create/read/comment/label/close via `gh`.
- Labels describe **workflow state**, not ownership ([`docs/agents/triage-labels.md`](https://github.com/callstack/agent-device/blob/main/docs/agents/triage-labels.md)): `needs-triage` → (optional `needs-info`) → `ready-for-agent`, or `wontfix` after an explicit maintainer decision.
- **External PRs are not a triage request surface** (“PRs as a request surface: no”) — propose product work as issues, not drive-by PRs without an agreed issue.

Bug reports should include OS/Node, Xcode/Android SDK when relevant, and exact command + output ([`CONTRIBUTING.md`](https://github.com/callstack/agent-device/blob/main/CONTRIBUTING.md)).

### Pull requests

[`docs/agents/pull-requests.md`](https://github.com/callstack/agent-device/blob/main/docs/agents/pull-requests.md):

- Conventional-commit titles; Summary = user/API behavior (with CLI/Node/MCP examples for public API changes) + `Closes #N`; Validation = scenario/device evidence, not command accounting.
- Device-facing behavior needs live simulator/emulator/device evidence; fixture tests prove contracts only.
- Reviewers check relevant ADRs; an ADR conflict is a finding unless the PR updates/supersedes the ADR.
- Runtime output must stay agent-friendly: compact defaults, bounded JSON, artifact paths for large raw data.

Practical sequence for an assert gap: **file a precise issue** (failure mode, desired agent branch on `code`/`details.reason`, non-goals) → wait for triage/`ready-for-agent` → implement in-tree → PR with live evidence and help/docs updates for CLI-surface changes.

---

## What “extend” means in this codebase (no third-party assert plugin)

### Not found: external command/assert plugins

Searches of the tree for plugin-loader / third-party extension paths returned nothing. Public npm `exports` expose client helpers (`./selectors`, `./batch`, `./android-adb`, `./limrun`, …) but not a register-your-own-command or register-your-own-predicate API ([`package.json` exports](https://github.com/callstack/agent-device/blob/main/package.json)).

The workspace extraction umbrella ([#1490](https://github.com/callstack/agent-device/issues/1490)) states the **published surface stays exactly one npm artifact (`agent-device`)**; new `packages/*` members are `"private": true`. That is modularization for maintainers, not a public plugin marketplace.

### In-tree seams that *look* like plugins (and what they own)

| Seam | What it is | Owns for assert? |
| --- | --- | --- |
| **`CommandDescriptor` registry** ([ADR 0008](https://github.com/callstack/agent-device/blob/main/docs/adr/0008-command-descriptor-registry.md); [`AGENTS.md`](https://github.com/callstack/agent-device/blob/main/AGENTS.md)) | One declaration site per command; catalog, capabilities, daemon route/policy, batch allowlist, MCP/CLI projections derive from it | **Yes** — new assert *commands* or traits on existing ones go here (plus family surface under `src/commands/**`) |
| **`is` predicates** ([`src/selectors/predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts); [`AGENTS.md`](https://github.com/callstack/agent-device/blob/main/AGENTS.md)) | Closed vocabulary evaluated by `evaluateIsPredicate` / admitted by `checkIsPredicate` | **Yes** — new UI predicates belong here (not a parallel matcher engine) |
| **Selector keys** ([`AGENTS.md`](https://github.com/callstack/agent-device/blob/main/AGENTS.md)) | Centralized in `src/selectors/parse.ts` | Indirect — better targeting for asserts |
| **`PlatformPlugin`** ([ADR 0009](https://github.com/callstack/agent-device/blob/main/docs/adr/0009-apple-platform-consolidation.md); [`packages/contracts/src/platform-plugin.ts`](https://github.com/callstack/agent-device/blob/main/packages/contracts/src/platform-plugin.ts); [`src/core/platform-plugin-registry.ts`](https://github.com/callstack/agent-device/blob/main/src/core/platform-plugin-registry.ts)) | One in-repo plugin per platform *family* (Apple, Android, Vega, …); `registerPlatformPlugin` is process-internal | **No** — stops core/daemon from branching on platform (interactor, discovery, appLog/perf/recording/providers facets) |
| **Providers / `ProviderDeviceRuntime`** ([ADR 0001](https://github.com/callstack/agent-device/blob/main/docs/adr/0001-provider-first-integration-scenarios.md); [`CONTEXT.md`](https://github.com/callstack/agent-device/blob/main/CONTEXT.md)) | Request-scoped adapters for device/host tools; cloud packages `@agent-device/provider-webdriver`, `@agent-device/provider-limrun` | **No** — transport/lease/inventory/interact; assert semantics stay on shared commands |
| **Maestro compat engine** ([ADR 0015](https://github.com/callstack/agent-device/blob/main/docs/adr/0015-direct-maestro-engine.md)) | Direct Maestro YAML execution + conformance corpus (`assertVisible`, `assertTrue`, …) | Only for Maestro YAML parity — not the primary agent-native assert path |

[`CONTEXT.md`](https://github.com/callstack/agent-device/blob/main/CONTEXT.md) names the demonstrated axes explicitly: **`CommandDescriptor` (what the system does) × `PlatformPlugin` (how a device family participates)** — not a third axis for user plugins.

---

## Built-in assert surface (what to extend)

First-party docs label this section **Assertions** ([Commands / llms-full](https://oss.callstack.com/agent-device/llms-full.txt); mirrored in Hub [`agent-device-cli.md`](agent-device-cli.md)):

```bash
agent-device is visible 'role="button" label="Continue"'
agent-device is exists 'id="primary-cta"'
agent-device is hidden 'text="Loading..."'
agent-device is editable 'id="email"'
agent-device is selected 'label="Wi-Fi"'
agent-device is text 'id="greeting"' "Welcome back"
```

Documented behavior ([llms-full Assertions](https://oss.callstack.com/agent-device/llms-full.txt)):

- `is` exits **non-zero on failure**.
- Predicates listed in docs: `visible`, `hidden`, `exists`, `editable`, `selected`, `text` (source also admits `focused` in the `IsPredicate` union — [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).
- `wait` polls for presence/text/ref/selector; `wait text` is presence, not hittability.
- Related read/assert helpers: `get`, `find`; settle-first workflow verifies with `wait` / `get` / `is` / `find` rather than screenshots alone ([`cli-help.ts` guidance via README/help](https://github.com/callstack/agent-device/blob/main/README.md)).

Error contract for agents ([ADR 0010](https://github.com/callstack/agent-device/blob/main/docs/adr/0010-error-system.md)): branch on `code`, retry on `retriable`, follow `hint`; machine-dispatchable sub-classification rides in **`details.reason`**. New codes are added deliberately; prefer reason enums over proliferating exit codes.

---

## Prior art (similar changes)

### Assert / wait honesty (closest to XQ needs)

- **[#1515](https://github.com/callstack/agent-device/issues/1515)** (`ready-for-agent`): `wait` conflates “element absent” with “device never gave a picture.” Proposed Tier 1 fixes emit labeled `details.reason` (e.g. `wait_target_absent`), set `retriable` on stalls, and keep **non-zero exit on absence** (`wait` is “used pervasively as an assertion primitive”). Explicitly **not** proposed: differentiated exit codes (reason enum preferred), adaptive timeouts, or making absence exit 0. This is the maintainer-shaped pattern for assert improvements: **honest structured verdicts for agents**, not a new expect engine.
- **[#1381](https://github.com/callstack/agent-device/pull/1381)** / [#1349](https://github.com/callstack/agent-device/issues/1349): landmark identity verification extended to read-only `wait`/`is` steps via `CommandDescriptor` traits — prior art for deepening assert/replay semantics inside existing commands.
- **Maestro `assertTrue`**: [#1295](https://github.com/callstack/agent-device/issues/1295) (enhancement, still `needs-triage` at fetch time) — phase-1 literal/`${VAR}` truthiness under the Maestro compat engine, gated away from full GraalJS expressions. Relevant only if XQ cares about Maestro YAML parity, not agent CLI asserts.

### Platform / provider “extension” prior art (wrong axis for asserts, right for how large landings look)

- **PlatformPlugin facets**: [#974](https://github.com/callstack/agent-device/issues/974) (closed) — daemon-owned facets on `PlatformPlugin` with parity tests before routing; shows the **wrap existing branch + table-equivalence** contribution style.
- **Vega TV**: [#1396](https://github.com/callstack/agent-device/pull/1396) — first-class platform family via in-tree plugin.
- **Cloud providers**: [#948](https://github.com/callstack/agent-device/pull/948) (WebDriver), [#1278](https://github.com/callstack/agent-device/pull/1278) (Limrun), later package facades [#1504](https://github.com/callstack/agent-device/pull/1504) / [#1518](https://github.com/callstack/agent-device/pull/1518) — new device runtimes as in-repo packages, not external plugins.

### Adding commands generally

[ADR 0008](https://github.com/callstack/agent-device/blob/main/docs/adr/0008-command-descriptor-registry.md) consequence: after the descriptor registry, adding a plain command is ~1–2 files of declaration plus per-platform `execute` implementations; every derived table was parity-tested before deleting hand tables. Daemon route/policy remain daemon-owned facets ([ADR 0003](https://github.com/callstack/agent-device/blob/main/docs/adr/0003-daemon-command-registry.md)).

---

## Viable upstream path for XQ assert needs

1. **Prefer extending existing primitives** over proposing an expect-file runner or parallel assert CLI (aligns with Callstack product shape and with Hub map #3 standing decisions).
2. **Classify the gap:**
   - Missing/weak **UI predicate** → extend `IsPredicate` + `evaluateIsPredicate` (+ help).
   - Ambiguous **pass/fail for agents** (absence vs infrastructure) → extend `wait`/`is` error `details.reason` / `retriable` / hints (see #1515 pattern; ADR 0010).
   - New **command** (e.g. batch named checks) → `CommandDescriptor` + family surface + provider-backed integration scenario; expect ADR/help/SkillGym or help-conformance impact.
   - Maestro-only YAML gap → ADR 0015 / conformance corpus track (separate from agent-native).
3. **File an issue** with: agent failure mode, exact commands, desired machine-readable branch keys, non-goals (exit-code sprawl, expect files, forks). Aim for `ready-for-agent` clarity.
4. **Implement in-tree** following CONTRIBUTING + AGENTS gates; include live device evidence for device-facing behavior; update versioned help for CLI-surface changes.
5. **Do not** assume a Plugin API will appear — [#1490](https://github.com/callstack/agent-device/issues/1490) and PlatformPlugin docs describe maintainer modularity, not third-party assert hooks.

---

## When a thin wrap is forced instead of upstream

Upstream contribution is the default. A thin XQ layer (skill, CI script, or pin helper — **not** a second assert engine) is the fallback when:

| Constraint | Why it blocks upstream-only |
| --- | --- |
| **No public assert/command plugin hook** | Cannot ship XQ-specific matchers outside a merged PR ([exports](https://github.com/callstack/agent-device/blob/main/package.json); no plugin-loader paths). |
| **Maintainer `wontfix` / ADR conflict** | Product rejects expect-files, exit-code matrices, or forks of assert philosophy ([#1515](https://github.com/callstack/agent-device/issues/1515) non-goals; ADR review rule in [pull-requests.md](https://github.com/callstack/agent-device/blob/main/docs/agents/pull-requests.md)). |
| **Orchestration across many CLI invocations** | Comparing several `agent-device` JSON outputs, suite gating, or Satellite install policy is consumer-owned; the CLI already treats agents as the loop ([Introduction](https://oss.callstack.com/agent-device/docs/introduction) / README: CLI does not embed test intelligence). |
| **Contribution cost vs urgency** | Live-device evidence, guarantee-matrix / parity gates, and multi-surface updates (CLI/MCP/help) may delay a CI-critical check; a temporary wrap that shells out to stock `is`/`wait` can bridge without forking. |
| **XQ-only vocabulary** | Domain expectations that are not general mobile UI predicates belong in Hub/Satellite skills or Specs, not in Callstack’s closed `IsPredicate` set. |

Hub already forbids a parallel expect-file runner and forbids forking; the wrap, if any, should stay **skill + install pin + optional CI glue** that consumes upstream exit codes / `--json` verdicts.

---

## Sources

- [callstack/agent-device README](https://github.com/callstack/agent-device/blob/main/README.md)
- [CONTRIBUTING.md](https://github.com/callstack/agent-device/blob/main/CONTRIBUTING.md)
- [AGENTS.md](https://github.com/callstack/agent-device/blob/main/AGENTS.md)
- [CONTEXT.md](https://github.com/callstack/agent-device/blob/main/CONTEXT.md)
- [docs/agents/issue-tracker.md](https://github.com/callstack/agent-device/blob/main/docs/agents/issue-tracker.md)
- [docs/agents/triage-labels.md](https://github.com/callstack/agent-device/blob/main/docs/agents/triage-labels.md)
- [docs/agents/pull-requests.md](https://github.com/callstack/agent-device/blob/main/docs/agents/pull-requests.md)
- [ADR index](https://github.com/callstack/agent-device/blob/main/docs/adr/README.md); [0001](https://github.com/callstack/agent-device/blob/main/docs/adr/0001-provider-first-integration-scenarios.md); [0003](https://github.com/callstack/agent-device/blob/main/docs/adr/0003-daemon-command-registry.md); [0008](https://github.com/callstack/agent-device/blob/main/docs/adr/0008-command-descriptor-registry.md); [0009](https://github.com/callstack/agent-device/blob/main/docs/adr/0009-apple-platform-consolidation.md); [0010](https://github.com/callstack/agent-device/blob/main/docs/adr/0010-error-system.md); [0015](https://github.com/callstack/agent-device/blob/main/docs/adr/0015-direct-maestro-engine.md)
- [`platform-plugin.ts`](https://github.com/callstack/agent-device/blob/main/packages/contracts/src/platform-plugin.ts); [`platform-plugin-registry.ts`](https://github.com/callstack/agent-device/blob/main/src/core/platform-plugin-registry.ts); [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)
- [oss.callstack.com agent-device llms-full (Assertions, Wait)](https://oss.callstack.com/agent-device/llms-full.txt)
- Issues/PRs: [#1515](https://github.com/callstack/agent-device/issues/1515), [#1295](https://github.com/callstack/agent-device/issues/1295), [#1490](https://github.com/callstack/agent-device/issues/1490), [#974](https://github.com/callstack/agent-device/issues/974), [#1381](https://github.com/callstack/agent-device/pull/1381), [#1396](https://github.com/callstack/agent-device/pull/1396), [#948](https://github.com/callstack/agent-device/pull/948), [#1278](https://github.com/callstack/agent-device/pull/1278)
