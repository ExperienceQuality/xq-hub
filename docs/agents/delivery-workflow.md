# XQ Delivery Workflow

The XQ Delivery Hub runs an agentic delivery workflow through GitHub issues.
Runtime state lives in the issue tracker. Repository docs define the machine.

## Objects

### Outcome issue

An outcome issue is the parent `xq-hub` GitHub issue for one approved outcome
across one or more repositories.

Created by a human at the idea stage with:

- a one-line title
- a one-paragraph description of what success looks like

The outcome issue carries the overall release identity and audit trail. It does
not carry stage checklists or detailed execution fields.

### Stage issue

A stage issue is a child issue of an outcome issue for one stage of the
workflow:

- `stage:idea`
- `stage:research`
- `stage:poc`
- `stage:design`
- `stage:build`
- `stage:test`
- `stage:release`

Every stage issue must contain:

- `Part of #<outcome-issue-number>`
- one line of scope for that stage

Every stage issue must also carry exactly one stage label and one role label:

- `role:product-owner`
- `role:solution-architect`
- `role:java-backend`
- `role:ios`
- `role:test`
- `role:sdet`
- `role:devops`

The assignee is the claiming agent. Assign the issue before any work begins.

### Stage artifact

Closing a stage issue requires a committed stage artifact at:

`docs/stages/<outcome-issue-number>-<stage>.md`

Every stage artifact must contain:

- `## Outputs`
- `## Evidence`
- `## Next-stage input`

The release stage artifact adds:

- `## Release package`

The stage artifact is the durable stage record. The release package under
`docs/releases/<release-id>.md` remains the immutable deployment manifest.

## Stage sequence

The workflow stages are:

`idea → research → poc? → design → build + test → release`

Gate rules:

- `idea` closes before `research` starts
- `research` closes before `design` starts unless a `poc` issue is opened
- `poc` is optional and human-triggered
- `design` closes before `build` starts
- `build` issues may run in parallel by independent repository scope
- `test` starts after build handoffs are ready and may overlap final build
  reconciliation when the test scope is already fixed
- `release` requires explicit human approval before it closes

## Stage ownership

| Delivery role | Agent file | Stages | Notes |
| --- | --- | --- | --- |
| product-owner | `.codex/agents/xq-product-owner.toml` | `idea`, `release` | Coordinates the outcome and records approvals |
| solution-architect | `.codex/agents/xq-solution-architect.toml` | `research`, `design` | Owns discovery and design decisions |
| java-backend | `.codex/agents/xq-java-backend-engineer.toml` | `build` | Java and JVM delivery scopes |
| ios | `.codex/agents/xq-ios-engineer.toml` | `build` | iOS and Swift delivery scopes |
| sdet | `.codex/agents/xq-sdet.toml` | `build` | Shared test-platform and reliability build scopes |
| test-engineer | `.codex/agents/xq-test-engineer.toml` | `test` | Targeted regression and smoke evidence |
| devops | `.codex/agents/xq-devops-engineer.toml` | `release` | Executes release, writes release package, owns deployment evidence |

DevOps may also be assigned earlier stage issues when CI/CD pipeline work or
infrastructure setup is part of the delivery. That assignment supplements the
primary stage owner rather than replacing it.

Stage eligibility must stay aligned in two places:

- the `stages` field in each `.codex/agents/*.toml`
- the role table in this document

## Human approvals

Human approval is required at two gates:

- `idea`: a human creates the outcome issue and approves the idea stage before
  research starts
- `release`: a named human approver must approve rollout before the release
  stage closes

Subagents may collaborate for discovery, but only an open stage issue
authorizes writes and stage artifacts.

## poc

`poc` is optional. When research cannot settle a design question on paper, the
research agent flags the need in the research stage issue. A human then creates
the `stage:poc` issue and blocks design on it.

Any delivery role may own the `poc` issue when explicitly assigned by a human.

## Build, test, and release evidence

Build and test work must still route through registered `delivery-ready`
repositories in `repository-context.yaml`.

The test engineer owns targeted regression and production smoke evidence. DevOps
owns the release package, deployment evidence, rollback readiness, and
observability.

For a schema-affecting release, keep the tested Docker compatibility matrix.
Application rollback works against the migrated database by default; destructive
schema rollback requires an explicit approved plan.

Production rollout, and any rollback after a failure trigger, require an
explicit GitHub approval comment from the named release approver. An outcome
reaches `complete` only after production smoke tests and the defined monitoring
result both pass.
