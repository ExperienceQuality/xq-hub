# Research: How agent-device assert and verify work

**Status:** Findings for Wayfinder [How agent-device assert and verify work](https://github.com/ExperienceQuality/xq-hub/issues/4) (map [XQ mobile assert via agent-device](https://github.com/ExperienceQuality/xq-hub/issues/3)).

**Question:** How does `agent-device` produce **info** and support **verify/assert** today — command surface, exit codes, machine-readable output, and what “compare output to an expectation” means without a separate XQ expect-file engine?

**Primary sources:** [Commands](https://oss.callstack.com/agent-device/docs/commands), [Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e), [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts), [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts), [`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts), [issue #1515](https://github.com/callstack/agent-device/issues/1515). Related Hub notes: [`agent-device-cli.md`](agent-device-cli.md), [`agent-device-upstream.md`](https://github.com/ExperienceQuality/xq-hub/blob/research/agent-device-upstream/docs/ideas/agent-device-upstream.md).

---

## Verdict

**Assert is already a first-class CLI surface**, not a missing product. Agents/CI compare **live `agent-device` info** to an expectation by calling:

| Role | Commands |
| --- | --- |
| Observe (info) | `snapshot -i`, settle diffs, `get text` / `get attrs`, `find …` |
| Assert (pass/fail) | `is <predicate> <selector> [expectedText]` — **exits non-zero on failure** |
| Wait for condition | `wait text|selector|@ref [timeoutMs]` — presence/time, **not** a hittability assert |
| Repeatable suite | `.ad` `replay` / Maestro-compat `assertVisible` via `replay`/`test --maestro` |

“Compare info to expectation” **without an XQ expect-file engine** means: run those commands (or a recorded `.ad` / Maestro flow) and treat **exit status + structured failure details** as the verdict. Agents may also eye-ball settle-diff / `get` text against a named checklist expectation — that is agent judgment, not a second runner.

`help validate` is **engineering validation** (stale build/daemon risk), not the UI assert vocabulary ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## How info is produced

### Snapshots and diffs

- Default: `snapshot -i` — token-efficient, interactive, visible-first view with `@eN` refs ([Commands — Snapshot](https://oss.callstack.com/agent-device/docs/commands); [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)).
- Full tree: `--raw` / `--json` when compact view is insufficient.
- After mutations with `--settle`, a **`settled: true` diff** is the preferred next observation; skip re-snapshot when the diff already shows the next target ([`cli-help.ts` Agent Starting Point / manual-qa](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Without settle: `diff snapshot` / `diff snapshot -i` for changed lines only.

### Reads

- `get text <ref|selector>` / `get attrs …` — read current values ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- `find …` — locate by text/selector; can fail with `COMMAND_FAILED` when no match ([`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts)).
- Truncated preview: expand with `snapshot -s @eN`, not `get text` ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

### Agent verify rule (installed help)

Named expectations must be verified with **`wait text|selector`, `get`, `is`, `find`, or the settled diff** — a bare screenshot/snapshot is not verification ([`cli-help.ts` manual-qa](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

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

### Predicates

Source admission list ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)):

`visible` | `hidden` | `exists` | `editable` | `selected` | `focused` | `text`

Docs page examples omit `focused`; help/TV guidance uses `is focused <selector>` ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)). Prefer **installed** `agent-device help` / source over stale docs snippets when they disagree.

### Semantics (docs + source)

- **`is` exits non-zero on failure** ([Commands — Assertions](https://oss.callstack.com/agent-device/docs/commands)).
- Failures surface as `COMMAND_FAILED` with details such as `expected="…" actual="…"` for `text` ([`evaluateIsPredicate`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts); throw sites in [`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts)).
- CLI maps non-zero command results to `process.exit(exitCode)` ([`src/cli/commands/generic.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/commands/generic.ts)).
- `is visible` — in current visible snapshot viewport (ancestor geometry can count) ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- `is exists` — selector matches in current snapshot only.
- `is text` — exact text compare to expected string (`actualText === expectedText`) ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).
- **`is` does not accept `@eN` refs** — use a selector expression ([Commands](https://oss.callstack.com/agent-device/docs/commands)).
- Selector keys (closed set) include boolean keys that collide with predicate names — use `visible=true` inside selectors when filtering ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).

---

## Wait vs assert

```bash
agent-device wait 1500
agent-device wait text "Welcome back"
agent-device wait @e12
agent-device wait 'role="button" label="Continue"' 5000
```

([Commands — Wait](https://oss.callstack.com/agent-device/docs/commands))

- Polls until selector/text resolves or timeout.
- `wait text` is **text-presence**, not hittability ([Commands — Assertions](https://oss.callstack.com/agent-device/docs/commands)).
- `wait @ref` resolves ref → label/text from a prior snapshot, then polls for that text — **not** stable node identity; duplicates can match the wrong element ([Commands](https://oss.callstack.com/agent-device/docs/commands)).

### Known honesty gap (upstream)

[callstack/agent-device#1515](https://github.com/callstack/agent-device/issues/1515) (open, `ready-for-agent`): agents cannot distinguish **element absent** from **capture never returned** on `wait` — risk of a confidently wrong verdict. Product direction in that issue: labeled wait reasons, keep non-zero on absence. Directly relevant to XQ “true assert” honesty.

---

## Machine-readable output

- Many commands support `--json` (e.g. `snapshot --json`, `capabilities --json`, `doctor --json`) ([Commands](https://oss.callstack.com/agent-device/docs/commands); [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start)).
- Default text is intentional for agent context; escalate to `--json`/`--raw` when needed ([`agent-device-cli.md`](agent-device-cli.md)).
- `is` failure details embed expected/actual in error text/details for `text` predicates ([`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts)).
- While exploring, help forbids piping away raw stdout (refs/hints live there) ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)) — CI assert steps can still use exit codes without scraping.

---

## Compare without an XQ expect-file engine

Viable patterns already in-tree:

1. **Shell assert (CI/agent step):**  
   `agent-device is text 'id="greeting"' "Welcome back"` → non-zero fails the step.
2. **Read then compare outside CLI:**  
   `agent-device get text 'id="greeting"' --json` (or text) → caller compares — only needed when `is` matchers are insufficient.
3. **Agent checklist:** named expectation in the prompt/script → verify via `is` / `wait` / settle diff (help’s manual-qa loop).
4. **Deterministic replay:** record `.ad` → `agent-device replay …`; or Maestro YAML with `assertVisible` / `assertNotVisible` via `replay`/`test --maestro` ([Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e)).

None of these require a Hub-owned expect-file format. Orchestration (which commands to run in which order) can stay in Satellite CI / agent plans / `.ad` scripts.

---

## Implications for the map

- The missing piece is **not** “invent assert” — it is **using and, where needed, hardening** upstream assert honesty (`wait` reasons per [#1515](https://github.com/callstack/agent-device/issues/1515), docs/CLI consistency, matcher gaps if grilling finds any).
- `xq-qe-box` / `xq-mobile-auto-test` should **route** agents into `is` / `wait` / replay patterns and pin CLI versions — not wrap a second expect engine (aligns with [`agent-device-upstream.md`](https://github.com/ExperienceQuality/xq-hub/blob/research/agent-device-upstream/docs/ideas/agent-device-upstream.md)).
- First upstream gap grilling should start from **wait failure labeling / capture-stall honesty** and any predicate/docs mismatch XQ hits in practice — not from greenfield assert CLI design.

---

## Gaps / follow-ups

1. Confirm on a pinned install (`≥ 0.20`) that `is focused` and docs examples match help (docs page vs `predicates.ts`).
2. Sample `--json` shape for a failed `is` on the pin XQ ships.
3. Whether XQ’s first product Satellite prefers `is` steps vs `.ad`/`Maestro` for CI.

---

## Sources

- [Commands](https://oss.callstack.com/agent-device/docs/commands) · [Replay & E2E](https://oss.callstack.com/agent-device/docs/replay-e2e) · [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)
- [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts) · [`predicates.ts`](https://github.com/callstack/agent-device/blob/main/src/selectors/predicates.ts) · [`selector-read.ts`](https://github.com/callstack/agent-device/blob/main/src/commands/interaction/runtime/selector-read.ts) · [`generic.ts` CLI exit](https://github.com/callstack/agent-device/blob/main/src/cli/commands/generic.ts)
- [Issue #1515](https://github.com/callstack/agent-device/issues/1515) — wait absent vs capture stall
- Hub: [`agent-device-cli.md`](agent-device-cli.md) · upstream research branch `research/agent-device-upstream`
