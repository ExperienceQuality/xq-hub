# XQ Delivery Workflow

## Intake

Keep unclear requests in local `.scratch/intake/` notes; they are ignored by Git
and must not be created as GitHub issues. Remove an intake note after recording
its promoted issue number. The product owner creates a GitHub delivery item only
when it has a target repository, bounded scope, acceptance criteria,
dependencies, and named review owner.

## Delegation

Each delivery item has child work items for independent write scopes. A work item
is eligible to start only when it is labelled `ready-for-agent` and contains:

- `Delegated to: xq-<role>`
- target repository and bounded paths/scope
- acceptance criteria and dependencies
- a solution-architect technical-design handoff or an explicit statement that
  no cross-cutting technical design is required
- named independent human reviewer

The product owner maintains this parent-issue status block:

`ready-for-agent` → `in-delivery` → `awaiting-review` → `ready-for-release` →
`deployed-pending-validation` → `complete`

Use `rollback-pending` or `rolled-back` when applicable. A solution architect
posts a technical-design handoff before cross-cutting implementation when
system boundaries or contracts remain open. Specialists post their final
handoff to their assigned hub issue; the product owner reconciles those
handoffs before advancing the parent state.

## Integration and release gates

The product owner may start independent non-overlapping work in parallel. For a
Java API and iOS client change, prefer a backward-compatible backend release
before the iOS release. Require test-engineer integration validation once all
implementation handoffs are ready.

`ready-for-release` requires each target repository's required CI checks,
targeted test evidence, accepted product criteria, and independent human review.
The parent issue links each item of evidence.

## Release package and deployment

DevOps commits one immutable release package under `docs/releases/` for every
release ID. It records rollout and rollback commit-hash collections for all
application commits/images, database migrations, infrastructure/configuration
revisions, and deployment artifacts. It excludes secrets.

DevOps captures comparable before/after test results against the declared
versions. The test engineer owns production smoke-test evidence; DevOps owns
deployment and observability. A schema-affecting release retains the tested
Docker compatibility matrix. Application rollback works against the migrated
database by default; destructive schema rollback needs an explicit approved
plan.

Non-production promotion may be automated after gates pass. Production rollout,
and any rollback after a failure trigger, require an explicit GitHub approval
comment from the release issue's `Release approver: @<GitHub-user>`. The product
owner tracks monitoring criteria when defined; until then, a deployed release
remains `deployed-pending-validation` rather than `complete`.

Before closing a delivery item, link the release package, approved asset hashes,
CI/test evidence, human review, rollout approval, production smoke result, and
monitoring result.
