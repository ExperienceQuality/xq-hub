# Research: How agent-device assert and verify work

**Status:** Findings for Wayfinder [How agent-device assert and verify work](https://github.com/ExperienceQuality/xq-hub/issues/4) (map [XQ mobile assert via agent-device](https://github.com/ExperienceQuality/xq-hub/issues/3)).

**Question:** How does `agent-device` produce **info** and support **verify/assert** today — command surface (`get`, `is`, `find`, `wait`, settle diffs, related help topics), exit codes, machine-readable output, and what “compare output to an expectation” means without a separate XQ expect-file engine?

**Primary sources (main @ agent-device 0.20.3):** [Commands](https://oss.callstack.com/agent-device/docs/commands), [Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e), [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots), [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts), [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts), [`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts), [`settle.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/settle.ts), [`cli.ts` failure handler](https://github.com/callstack/agent-device/blob/main/src/cli.ts), [issue #1515](https://github.com/callstack/agent-device/issues/1515). Related Hub: [`agent-device-cli.md`](agent-device-cli.md), [`agent-device-upstream.md`](https://github.com/ExperienceQuality/xq-hub/blob/research/agent-device-upstream/docs/ideas/agent-device-upstream.md). CLI was **not** installed in this research environment — prefer installed `agent-device help …` when a pin is available.

---

## Verdict

**Assert is already a first-class CLI surface**, not a missing product. Agents/CI compare **live `agent-device` info** to an expectation by calling:

| Role | Commands / flags |
| --- | --- |
| Observe (info) | `snapshot -i`, `--settle` diffs, `diff snapshot`, `get text` / `get attrs`, `find …` |
| Assert (pass/fail) | `is <predicate> <selector> [expectedText]` — throws `COMMAND_FAILED` → **CLI exit 1** |
| Wait for condition | `wait text\|selector\|@ref\|stable […]` — presence/time/stability; **not** a hittability assert |
| Post-action evidence (not assert) | `--settle` (diff; never fails the action), `--verify` (AX digest evidence) |
| Repeatable suite | `.ad` `replay` / `test`; Maestro-compat `assertVisible` via `replay`/`test --maestro` |

“Compare info to expectation” **without an XQ expect-file engine** means: run those commands (or a recorded `.ad` / Maestro flow) and treat **exit status + structured failure details** as the verdict. Agents may also match settle-diff / `get` text against a named checklist — that is agent judgment, not a second runner.

`help validate` is **engineering validation** (stale build/daemon risk), not the UI assert vocabulary ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)). Routine QA shapes: `help manual-qa`; full reference: `help workflow`.

---

## How info is produced

### Snapshots and diffs

- Default: `snapshot -i` — token-efficient, interactive, visible-first view with `@eN` refs ([Commands — Snapshot](https://oss.callstack.com/agent-device/docs/commands); [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)).
- Full tree: `--raw` / `--json` when compact view is insufficient.
- After mutations with `--settle`, a **`settled: true` diff** is the preferred next observation; skip re-snapshot when the diff already shows the next target ([`cli-help.ts` Agent Starting Point / manual-qa](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Without settle: `diff snapshot` / `diff snapshot -i` for changed lines only ([Commands](https://oss.callstack.com/agent-device/docs/commands)).

### `--settle` and `--verify` (observation, not assert)

- `--settle` on `press` / `click` / `fill` / `longpress`: wait for UI quiet, append settled diff vs pre-action tree in the **same** response. **Best-effort; never fails the action** ([flag help](https://github.com/callstack/agent-device/blob/main/src/commands/cli-grammar/flag-definitions-action.ts); [`settle.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/settle.ts)).
- `--settle-quiet <ms>` (default 500) and `--timeout` as settle deadline when settle is on ([flag groups](https://github.com/callstack/agent-device/blob/main/src/commands/cli-grammar/flag-groups.ts)).
- `--verify` on press/click/fill: cheap post-action AX digest / node counts / `changedFromBefore` — evidence, **not** an expectation matcher ([flag help](https://github.com/callstack/agent-device/blob/main/src/commands/cli-grammar/flag-definitions-action.ts); composed with settle via [`settleEvidence`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/settle.ts)).
- Human/agent text appends `settled` / `not settled` plus `+`/`-` lines and optional unchanged-interactive tail ([`interaction/output.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/output.ts)).

### Reads

- `get text|attrs <@ref|selector>` — read current values ([Commands](https://oss.callstack.com/agent-device/docs/commands); usage in [`interaction/index.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/index.ts)). Text success prints the text string; attrs as JSON node ([`output.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/output.ts)).
- `find` — semantic locate by query / locator (`text|label|value|role|id`) then action: omit / `click` / `focus` / `exists` / `wait` / `get text` / `get attrs` / `fill` / `type` ([Commands — Find](https://oss.callstack.com/agent-device/docs/commands); [`selectors.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/selectors.ts)). No match → `COMMAND_FAILED` (`find did not match any element`) ([`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts)).
- Truncated preview: expand with `snapshot -s @eN`, not `get text` ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

### Agent verify rule (installed help)

Named expectations must be verified with **`wait text|selector`, `get`, `is`, `find`, or the settled diff** — a bare screenshot/snapshot is not verification ([`cli-help.ts` manual-qa / Validation and evidence](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## Assert command surface: `is`

Documented examples ([Commands — Assertions](https://oss.callstack.com/agent-device/docs/commands)):

```bash
agent-device is visible 'role="button" label="Continue"'
agent-device is exists 'id="primary-cta"'
agent-device is hidden 'text="Loading..."'
agent-device is editable 'id="email"'
agent-device is selected 'label="Wi-Fi"'
agent-device is text 'id="greeting"' "Welcome back"
```

Forms: `is <predicate> <selector>` or `is <selector> <predicate>` ([`normalizeIsPositionals`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).

### Predicates

Source admission list ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)):

`visible` | `hidden` | `exists` | `editable` | `selected` | `focused` | `text`

Docs page examples omit `focused`; help/TV guidance uses `is focused <selector>` ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)). Prefer **installed** help / source over stale docs snippets when they disagree.

### Semantics (docs + source)

- Success returns `{ pass: true, predicate, selector, … }` and prints `Passed: is <predicate>` ([`IsCommandResult`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts); [`isCliOutput`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/output.ts)).
- Failure **throws** `AppError('COMMAND_FAILED', …)` with reasons such as `predicate_failed` / `selector_not_found` (and `expected="…" actual="…"` for `text`) — there is no successful `{ pass: false }` payload ([`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts); [`evaluateIsPredicate`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).
- Docs: **`is` exits non-zero on failure** ([Commands — Assertions](https://oss.callstack.com/agent-device/docs/commands)). CLI maps that throw to **`process.exit(1)`** in [`handleRunCliFailure`](https://github.com/callstack/agent-device/blob/main/src/cli.ts) (not a per-predicate exit-code table). Suite `test` can return richer exit codes via reporters; that path is separate from `is`.
- `is visible` — in current visible snapshot viewport (ancestor geometry can count) ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- `is exists` — selector matches in current snapshot only (no node evidence on success) ([`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts)).
- `is text` — exact string equality (`actualText === expectedText`) ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).
- **`is` does not accept `@eN` refs** — use a selector expression ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- Predicate tokens that double as selector keys: write `visible=true` inside the selector when filtering, not a bare `visible` term ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).

---

## Wait vs assert

```bash
agent-device wait 1500
agent-device wait text "Welcome back"
agent-device wait @e12
agent-device wait 'role="button" label="Continue"' 5000
agent-device wait stable
agent-device wait stable 500 10000
```

([Commands — Wait](https://oss.callstack.com/agent-device/docs/commands); usage `wait <ms>|text <text>|@ref|<selector>|stable [quietMs] [timeoutMs]` in [`capture/wait.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/capture/wait.ts))

- Polls on a fixed interval (help: 300ms) up to timeout (default 10s); timeout raises a command failure, not a soft not-found ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [`selector-wait.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-wait.ts)).
- `wait text` is **text-presence**, not hittability ([Commands — Assertions](https://oss.callstack.com/agent-device/docs/commands)). Prefer `wait text` over `is visible` for async/list presence when no interaction is needed ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- `wait @ref` resolves ref → label/text from a prior snapshot, then polls for that text — **not** stable node identity; duplicates can match the wrong element ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- `wait stable` — fallback when settle was skipped; defaults quiet/timeout 500/10000; do not insert after a successful settle that already shows the change ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

### Known honesty gap (upstream)

[callstack/agent-device#1515](https://github.com/callstack/agent-device/issues/1515) (open, `ready-for-agent`): agents cannot distinguish **element absent** from **capture never returned** on `wait` — risk of a confidently wrong verdict. Product direction in that issue: labeled wait reasons, keep non-zero on absence. Directly relevant to XQ “true assert” honesty.

---

## Exit codes and machine-readable output

| Outcome | Behavior | Source |
| --- | --- | --- |
| Success (default text) | Human/agent line(s) on stdout (e.g. `Passed: is visible`, settle diff lines, `get` text) | [`interaction/output.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/output.ts); [`writeCommandOutput`](https://github.com/callstack/agent-device/blob/main/src/cli/commands/shared.ts) |
| Success (`--json`) | `{ "success": true, "data": … }` | [`shared.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/commands/shared.ts) |
| Failure (parse or command) | Human error on stderr **or** `{ "success": false, "error": <normalized> }`; then **`process.exit(1)`** | [`handleRunCliFailure`](https://github.com/callstack/agent-device/blob/main/src/cli.ts) |
| `test` suites | Streaming pass/fail on stderr; exit from suite/reporters (can raise above 1) | [Commands — Replay](https://oss.callstack.com/agent-device/docs/commands); [Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e) |

Config can default `"json": true` ([Configuration](https://oss.callstack.com/agent-device/docs/configuration)). While exploring, help forbids piping away raw stdout (refs/hints live there) ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)) — CI assert steps can rely on exit codes without scraping.

Related help topics for the loop: `manual-qa`, `workflow`, `validate` (engineering), `dogfood`, plus platform topics (`tv`, `web`, …) ([`HELP_TOPICS`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## Compare without an XQ expect-file engine

Viable patterns already in-tree:

1. **Shell assert (CI/agent step):**  
   `agent-device is text 'id="greeting"' "Welcome back"` → exit 1 fails the step.
2. **Read then compare outside CLI:**  
   `agent-device get text 'id="greeting"' [--json]` → caller compares — only when `is` matchers are insufficient (substring, regex, multi-field).
3. **Agent checklist:** named expectation in the prompt/script → verify via `is` / `wait` / settle diff (help’s manual-qa loop). Settle/`--verify` alone do **not** fail closed on “wrong UI.”
4. **Deterministic replay:** record `.ad` → `agent-device replay` / `test`; or Maestro YAML with `assertVisible` / `assertNotVisible` via `replay`/`test --maestro` ([Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e)).
5. **Batch:** JSON steps with stop-on-first-error ([Commands — Batch](https://oss.callstack.com/agent-device/docs/commands)) — orchestration, not a new matcher language.

None require a Hub-owned expect-file format. Orchestration stays in Satellite CI / agent plans / `.ad` scripts.

---

## Implications for the map

- The missing piece is **not** “invent assert” — it is **using and, where needed, hardening** upstream assert honesty (`wait` reasons per [#1515](https://github.com/callstack/agent-device/issues/1515), docs/CLI predicate consistency, matcher gaps if grilling finds any).
- Do not confuse **`--settle` / `--verify` evidence** with pass/fail assert; XQ CI should pin on `is` / `wait` / replay exit status.
- `xq-qe-box` / `xq-mobile-auto-test` should **route** agents into `is` / `wait` / replay patterns and pin CLI versions — not wrap a second expect engine (aligns with [`agent-device-upstream.md`](https://github.com/ExperienceQuality/xq-hub/blob/research/agent-device-upstream/docs/ideas/agent-device-upstream.md)).
- First upstream gap for grilling: **wait failure labeling / capture-stall honesty (#1515)**, then any predicate/docs mismatch XQ hits in practice — not greenfield assert CLI design.

---

## Gaps / follow-ups

1. Confirm on a pinned install (`≥ 0.20`) that `is focused` and docs examples match help (docs page vs `predicates.ts`).
2. Sample `--json` failure envelope for a failed `is` on the pin XQ ships (`success: false` + normalized error fields).
3. Whether XQ’s first product Satellite prefers `is` steps vs `.ad`/`Maestro` for CI.

---

## Sources

- [Commands](https://oss.callstack.com/agent-device/docs/commands) · [Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e) · [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots) · [Configuration](https://oss.callstack.com/agent-device/docs/configuration)
- [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts) · [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts) · [`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts) · [`settle.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/settle.ts) · [`cli.ts`](https://github.com/callstack/agent-device/blob/main/src/cli.ts) · [`interaction/output.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/output.ts) · [flag `--settle`/`--verify`](https://github.com/callstack/agent-device/blob/main/src/commands/cli-grammar/flag-definitions-action.ts)
- [Issue #1515](https://github.com/callstack/agent-device/issues/1515) — wait absent vs capture stall
- Hub: [`agent-device-cli.md`](agent-device-cli.md) · [`agent-device-upstream.md`](https://github.com/ExperienceQuality/xq-hub/blob/research/agent-device-upstream/docs/ideas/agent-device-upstream.md)
