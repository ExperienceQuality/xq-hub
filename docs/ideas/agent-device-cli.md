# Research: agent-device CLI (agent-native)

**Status:** Draft — scoped to the **agent-native CLI** (how coding agents install, invoke, and loop through shell commands). Not MCP, not the Node client API, not contributor architecture.

**Related Hub context:** [`docs/ideas/xq-motest-cli.md`](xq-motest-cli.md) was cancelled in favour of adopting this project. Frame adoption as “wire agents to the CLI,” not reinventing a mobile-test CLI.

Primary sources: [callstack/agent-device](https://github.com/callstack/agent-device) README / skills / installed help (`src/cli/parser/cli-help.ts`), plus first-party docs under https://oss.callstack.com/agent-device/docs/. Prefer **installed** `agent-device help …` over any Hub copy — help is version-matched to the binary ([AGENTS.md](https://github.com/callstack/agent-device/blob/main/AGENTS.md); [Introduction](https://oss.callstack.com/agent-device/docs/introduction)).

---

## What agents get

`agent-device` is a shell CLI that lets a coding agent **inspect, control, and verify** a real app on device/simulator targets, returning compact UI state and optional evidence files ([README](https://github.com/callstack/agent-device/blob/main/README.md); [Introduction](https://oss.callstack.com/agent-device/docs/introduction)).

The agent (or harness) chooses each next command; the CLI executes and prints results. It does not embed test intelligence ([Introduction](https://oss.callstack.com/agent-device/docs/introduction)).

A long-lived **daemon** is started as needed behind the CLI and owns the session; agents normally only talk to the `agent-device` binary (state under `~/.agent-device` for packaged installs) ([Installation](https://oss.callstack.com/agent-device/docs/installation); [Sessions](https://oss.callstack.com/agent-device/docs/sessions)). Treat it as an invisible runtime dependency unless diagnosing failures (`Session state: …`, `runner.log`, request logs).

---

## Install and invoke

**Requirements:** Node.js ≥ 22.12 (web automation needs Node 24+) ([README](https://github.com/callstack/agent-device/blob/main/README.md); [package.json `engines`](https://github.com/callstack/agent-device/blob/main/package.json)).

**Preferred for agents — global install** (stable `PATH` command + matching help) ([Installation](https://oss.callstack.com/agent-device/docs/installation); [Expo agent-device quick start](https://docs.expo.dev/agents/agent-device/)):

```bash
npm install -g agent-device@latest   # or yarn/pnpm/bun global
agent-device doctor
agent-device --version
agent-device help workflow
```

Humans should run `doctor` before handing the CLI to an agent ([Installation](https://oss.callstack.com/agent-device/docs/installation)).

**Invoke rules for agents** ([skills/agent-device/SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md); [Installation](https://oss.callstack.com/agent-device/docs/installation); [Agent Setup recommended rule](https://oss.callstack.com/agent-device/docs/agent-setup)):

- Resolve `agent-device` the way the user’s login shell would (agent `PATH` often differs); use an absolute binary path if needed.
- Do **not** autonomously run `npx -y agent-device@latest` or silent global upgrades; require a trusted install / exact version approval.
- Skill currently requires **CLI ≥ 0.20.0** for current help topics ([SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md)).
- Optional project defaults: `./agent-device.json` or `~/.agent-device/config.json` (e.g. default `platform`, `device`, `json`) overridden by flags ([Configuration](https://oss.callstack.com/agent-device/docs/configuration)).

Local targets need Xcode/`simctl`/`devicectl` (Apple) and/or Android SDK/`adb` ([Installation](https://oss.callstack.com/agent-device/docs/installation)). No library is added to the app under test — the CLI drives the installed app ([Expo](https://docs.expo.dev/agents/agent-device/)).

---

## Skills → versioned CLI help

The official skill is a **thin router**, not a command manual ([SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md); [Agent Setup](https://oss.callstack.com/agent-device/docs/agent-setup); [AGENTS.md](https://github.com/callstack/agent-device/blob/main/AGENTS.md) forbids putting behavior detail in skills).

```bash
npx skills add callstack/agent-device
```

Before planning, agents are told to read the **smallest matching** installed topic ([SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md)):

| Topic | When |
| --- | --- |
| `help manual-qa` | Scripted / checklist QA |
| `help validate` | Code/runtime validation, stale build/daemon risk |
| `help dogfood` | Exploratory QA + evidence report |
| `help workflow` | Full reference / mixed tasks |

Additional topics when relevant (same sources + [`cli-help.ts` HELP_TOPICS](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)): `debugging`, `react-native`, `react-devtools`, `cdp`, `tv`, `physical-device`, `ios-system-ui`, `remote`, `macos`, `web`, `maestro`.

Without a skill, the same contract applies: run `--version`, then `help workflow` (or a task topic) before inventing commands ([Agent Setup](https://oss.callstack.com/agent-device/docs/agent-setup)).

Top-level help also ships an **Agent Starting Point**: settle-first loop, selector grammar, and “command lines only when asked for a plan” ([`cli-help.ts` AGENT_START_LINES](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## Agent-native interaction loop

### Settle-first default (current help)

Installed guidance’s default app loop ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); echoed in [README](https://github.com/callstack/agent-device/blob/main/README.md) / [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start)):

1. `agent-device open <app> --platform ios|android …`
2. `agent-device snapshot -i` → mint current `@eN` refs
3. Mutate with `press` / `click` / `fill` / `longpress` **and `--settle`**
4. Treat a `settled: true` **diff** as the next observation (fresh refs on changed lines) — do not blindly re-`snapshot` when the diff already shows the next target
5. Verify named expectations with `wait text|selector`, `get`, `is`, `find`, or that settled diff — not a bare screenshot alone
6. `agent-device close`

`--settle` only on those mutating commands — never on `open`, `snapshot`, or `close` ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

If settle is skipped or unsupported: verify with `diff snapshot` / `diff snapshot -i` (changed lines only), not a full tree dump ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)).

Network/debounced UI after settle: `wait text "…"` or `wait <selector>`, not snapshot polling ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

### Refs, selectors, and staleness

- Refs look like `@e12`. Prefer refs from the **latest** snapshot or settle diff ([README](https://github.com/callstack/agent-device/blob/main/README.md); [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Optional pin: `@e12~s4` where `4` is `refsGeneration` from the minting response; iOS rejects stale refs on mutations ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Durable selectors (examples): `label="Search"`, `id="submit"`, `role=button label="Follow"`. Selector keys are a closed set (`id`, `role`, `text`, `label`, … — not `placeholder`/`index`/`key`) ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- After mutation, refs are stale: use a known selector, or refresh with `snapshot -i` (optionally `-s` scoped) ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Coordinates are fallback-only after refs/selectors fail; use `snapshot -i --json` for rects if needed ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

### Snapshot habits

- Default: `snapshot -i` (interactive, token-efficient, visible-first) ([Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)).
- Off-screen content appears as scroll **hints**, not refs — `scroll`, then re-snapshot ([Snapshots](https://oss.callstack.com/agent-device/docs/snapshots); [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Full provider tree only when needed: `--raw` or `--json` ([Snapshots](https://oss.callstack.com/agent-device/docs/snapshots); [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start)).

### Sessions (CLI-visible)

- Implicit `default` session is scoped to the current git worktree / CWD so parallel agents don’t collide ([Sessions](https://oss.callstack.com/agent-device/docs/sessions)).
- Named `--session <name>` when sharing/reusing intentionally; put it on every command in that flow ([Sessions](https://oss.callstack.com/agent-device/docs/sessions); [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).
- Serialize mutating commands in one session; parallelize only reads or separate sessions/devices ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [Sessions](https://oss.callstack.com/agent-device/docs/sessions)).
- CI cleanup: `close --shutdown` to stop sim/emulator ([Sessions](https://oss.callstack.com/agent-device/docs/sessions)).

### Agent output hygiene (from help)

While exploring: do **not** pipe CLI output through `jq`/`grep`/`head`/`tail` or hide stderr — raw stdout carries refs, warnings, hints, and diagnostics for the next step ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)). Follow structured **hints** on failures before inventing recovery ([`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## Command families agents call from the shell

Mental model from [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start), [Commands](https://oss.callstack.com/agent-device/docs/commands), and help starting point — not a full dump. Discover support with `agent-device capabilities --platform …` ([Quick Start](https://oss.callstack.com/agent-device/docs/quick-start)).

| Family | Typical CLI commands |
| --- | --- |
| Bootstrap / device | `doctor`, `boot`, `shutdown`, `devices`, `apps`, `capabilities` |
| Session | `open`, `close`, `session` |
| Map UI | `snapshot`, `diff snapshot` |
| Act | `press`, `click`, `fill`, `type`, `scroll`, `swipe`, `gesture`, `longpress`, `back`, `home`, `wait`, `alert`, `keyboard` |
| Read / assert | `get`, `is`, `find` |
| Evidence (on demand) | `screenshot`, `record`, `logs`, `network`, `trace`, `perf`, `artifacts` |
| App bits | `install`, `reinstall`, `install-from-source`, `settings`, `push`, … |
| RN / Expo hazards | `react-native` (e.g. `dismiss-overlay`), `react-devtools`, `metro`, `cdp` — after `help react-native` / related topics |
| Repeat | `batch`, `replay`, `test` (incl. Maestro where supported) |

TV / desktop / web: same binary, different help (`help tv`, `help macos`, `help web`) and often focus/remote or helper-specific controls ([README](https://github.com/callstack/agent-device/blob/main/README.md); HELP_TOPICS in [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts)).

---

## Output formats agents rely on

| Mode | Behavior | Source |
| --- | --- | --- |
| **Default human/agent text** | Token-efficient snapshots with `@refs`; settle diffs (`+`/`-`/unchanged); compact command results; recovery **hints** | [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots); [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [README](https://github.com/callstack/agent-device/blob/main/README.md) |
| **`--json`** | Structured parse for scripts/automation; e.g. `snapshot --json`, `get text @e1 --json`, `capabilities --json`, `perf … --json`, `artifacts --json` | [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start); [Commands](https://oss.callstack.com/agent-device/docs/commands) |
| **Large evidence** | Paths + compact metadata on stdout; heavy files stay on disk (screenshots, `.trace`, heap dumps, recordings) | [Commands / perf](https://oss.callstack.com/agent-device/docs/commands); [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts) |
| **Session paths** | Human: `Session state: <path>`; JSON: `sessionStateDir`, often `runnerLogPath` / `requestLogPath` | [Sessions](https://oss.callstack.com/agent-device/docs/sessions) |
| **Config default** | `"json": true` in config files possible | [Configuration](https://oss.callstack.com/agent-device/docs/configuration) |

Default text is intentional for agent context budget; escalate to `--json`/`--raw` only when the compact view lacks what you need ([Snapshots](https://oss.callstack.com/agent-device/docs/snapshots)).

---

## Platform notes that change the CLI workflow

Only differences agents must account for when choosing commands/help:

| Target | Agent-facing difference | Source |
| --- | --- | --- |
| **iOS / Android mobile** | Shared snapshot/ref loop; `--platform ios\|android`; boot if no device | [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start); [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots) |
| **Physical device** | Pairing/signing; read `help physical-device` before inventing flags | [Installation](https://oss.callstack.com/agent-device/docs/installation); HELP_TOPICS |
| **Android text** | Emulators auto test-IME for non-ASCII; real devices may need `open … --test-ime` | [Known Limitations](https://oss.callstack.com/agent-device/docs/known-limitations) |
| **React Native / Expo** | Overlays: `react-native dismiss-overlay`; don’t tap warning text by hand; read `help react-native` | [`cli-help.ts`](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts); [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start) |
| **TV** | Focus/remote-first; `help tv` — not the same tap-heavy loop | HELP_TOPICS; [README](https://github.com/callstack/agent-device/blob/main/README.md) |
| **Vega OS** | VVD-only initially; discovery/lifecycle/remote — capture/selectors may be unsupported; follow `help tv` / target help for control-only loop | [README](https://github.com/callstack/agent-device/blob/main/README.md); [SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md) |
| **When capture unsupported** | Skill: use control-only loop + device display as visual truth | [SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md) |

iOS `settings` helpers are largely simulator-oriented ([Quick Start](https://oss.callstack.com/agent-device/docs/quick-start)).

---

## Adoption sketch (CLI-only)

For an XQ satellite: pin a trusted `agent-device` on PATH → add skill or project rule pointing at `help workflow` / task topics → human runs `doctor` once → agents run settle-first shell loops against sim/emulator → capture evidence paths only when needed. Do not invent a parallel CLI.

---

## Footer: license / version

MIT ([LICENSE](https://github.com/callstack/agent-device/blob/main/LICENSE)). npm package `agent-device` **0.20.3** at research time ([package.json](https://github.com/callstack/agent-device/blob/main/package.json)). Pre-1.0; help and flags move — always read the **installed** CLI help.

---

## Gaps / follow-ups

1. Which satellite gets the CLI first, and which default `--platform` / device name?
2. Pin global toolchain vs `agent-device.json` / package-manager global per machine?
3. Require skill install in agent harnesses, or only a Cursor/project rule quoting `help workflow`?
4. CI: local `boot` + `.ad` `replay`/`test`, or leave CI for a later Spec?
5. Confirm agent sandbox `PATH` can see the global binary (absolute path in rules if not).

---

## Sources

- [README](https://github.com/callstack/agent-device/blob/main/README.md) · [AGENTS.md](https://github.com/callstack/agent-device/blob/main/AGENTS.md) · [skills/agent-device/SKILL.md](https://github.com/callstack/agent-device/blob/main/skills/agent-device/SKILL.md)
- [cli-help.ts](https://github.com/callstack/agent-device/blob/main/src/cli/parser/cli-help.ts) (Agent Starting Point, HELP_TOPICS, settle-first loop)
- Docs: [Introduction](https://oss.callstack.com/agent-device/docs/introduction) · [Installation](https://oss.callstack.com/agent-device/docs/installation) · [Agent Setup](https://oss.callstack.com/agent-device/docs/agent-setup) · [Quick Start](https://oss.callstack.com/agent-device/docs/quick-start) · [Sessions](https://oss.callstack.com/agent-device/docs/sessions) · [Snapshots](https://oss.callstack.com/agent-device/docs/snapshots) · [Commands](https://oss.callstack.com/agent-device/docs/commands) · [Configuration](https://oss.callstack.com/agent-device/docs/configuration) · [Known Limitations](https://oss.callstack.com/agent-device/docs/known-limitations)
- Hub: [xq-motest-cli.md](xq-motest-cli.md)
