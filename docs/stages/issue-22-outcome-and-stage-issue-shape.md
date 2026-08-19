# Stage artifact: Define outcome issue and stage issue shape
<!-- wayfinder ticket: #22 | map: #21 -->

## Outputs

### Outcome issue

Opened by a human at the idea stage. Required fields:

- **Title**: one-line description of the outcome
- **Body**: one paragraph describing what success looks like

No task list, no stage checklist, no repos, no criteria in the body.
The `stage:idea` child issue carries criteria for that stage.

Labels: none required at creation (stage issues carry the stage/role labels).

### Stage issue

One child issue per stage per outcome. Required fields in the body:

- `Part of #<outcome>` — parent reference
- One-line scope for this stage

That is all. The agent reads the outcome issue for context.

Labels: two labels required — one stage label and one role label.

**Stage labels:**
`stage:idea` | `stage:research` | `stage:poc` | `stage:design` | `stage:build` | `stage:test` | `stage:release`

**Role labels (short form):**
`role:product-owner` | `role:solution-architect` | `role:java-backend` | `role:ios` | `role:test` | `role:sdet` | `role:devops`

Assignee = the claiming agent (set before any work begins).

### poc stage

Optional. Triggered by a human. When research cannot settle a design
question on paper, the research agent flags the need in its stage issue
comment; a human creates the `stage:poc` issue and blocks `stage:design`
on it.

## Evidence

Decided in wayfinder ticket #22 via grilling. No code changes required;
GitHub label creation is a follow-on install step (tracked in #25 via the
workflow doc rewrite).

## Dependencies

- Labels `stage:*` and `role:*` must be created in the hub repo before the
  workflow can be operated. Tracked in #25.
- Stage artifact schema (ticket #23) determines what agents write when
  closing a stage issue.

## Residual risk

- Role label list assumes the current seven-agent roster is final. If the
  roster changes, labels need updating.
- "One-line scope" is intentionally loose; a later grilling ticket may
  tighten it if agents over-interpret scope boundaries.
