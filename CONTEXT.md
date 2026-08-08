# XQ Agent Context Hub

Shared understanding and work packaging for agents across the ExperienceQuality organisation. Application code lives elsewhere.

## Language

**Hub**:
The GitHub repository whose product is shared understanding and work packaging for agents — glossary, ADRs, specs, tickets, and handoffs — not shipped application software.
_Avoid_: Monorepo, orchestration runtime, control plane, platform

**Satellite**:
A GitHub repository in the organisation that owns and ships application code for a product or system. Agents implement there; they plan and package work in the Hub. Each Satellite owns its product domain language.
_Avoid_: Downstream repo, child repo, microservice (when you mean any non-Hub repo)

**Satellite catalogue**:
A Hub-owned index of Satellites — name, link, one-line purpose, and matching `satellite:<name>` label — without copying a Satellite’s product glossary. Lives as `docs/satellites.md`; Ticket labels mirror this file.
_Avoid_: Registry, inventory, monorepo map

## Work packaging

**Idea**:
Sharpened intent that is not yet a buildable plan — the output of grilling, research, or exploration.
_Avoid_: Initiative, epic, proposal (when you mean unshaped intent)

**Spec**:
A buildable plan collapsed from an Idea, still Hub-owned before it is sliced into Tickets.
_Avoid_: Design doc, PRD, roadmap item

**Ticket**:
A tracer-bullet slice of a Spec, aimed at one Satellite (or at Hub docs), with explicit blockers to other Hub Tickets. Tracked as a GitHub Issue on the Hub; a Satellite label routes it; Satellite work links back to that issue. Ready means open, blockers closed, and a Satellite label is set. Done means a merged PR that links the Ticket and meets its acceptance.
_Avoid_: Task, story, work item, issue (when speaking in Hub language — prefer Ticket)

**Prototype**:
Throwaway code that answers one design question. Local-only under `.prototype/`, never committed; the lasting output is what is written back into an Idea or Spec.
_Avoid_: Spike, sandbox, POC (when you mean a Hub-managed throwaway answer)

**Handoff**:
A short Hub-owned brief that packages intent, constraints, and links for the next session or agent. The next agent receives an explicit pointer at launch (path or Ticket); it does not scan for open handoffs. Lasting truth stays in Idea, Spec, Ticket, and CONTEXT — not in the Handoff.
_Avoid_: Session log, ticket dump, inbox, queue (ready work is a Ticket)

## Quality (org testing method)

Normative docs live under Hub `quality/`. Vocabulary (asset, component, capability, attribute, spot, test sizes) is defined in [`quality/glossary.md`](quality/glossary.md). Agents conform via `quality-*` skills installed from Satellite `xq-qe-box` (`gh skill install`), not via Satellite-local quality binders.

**Terminal** (`xq-terminal`):
TAP-like admission **Python** CLI in `xq-qe-box`: asset checks in with a **passport** (small/medium results JSON); output is **qualified / not** to board the **merge** or **release** plane. Release Specs come from Satellite **`xq-terminal-registry`** (YAML sanitize → Spec wheels as registry pip deps); Terminal does not depend on Specs directly. Protocol: **`xq-terminal-sdk`**. Spec: [`docs/specs/xq-terminal.md`](docs/specs/xq-terminal.md).
_Avoid_: Google TAP product clone; JVM Terminal/Specs; declaring Specs as Terminal dependencies; hand-edited registry `pyproject.toml`
