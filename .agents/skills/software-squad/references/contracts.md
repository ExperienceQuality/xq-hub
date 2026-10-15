# Durable contracts

Use these records under the Hub workspace's ignored `.hub/` directory. Write persisted records in normal technical prose. Use UTC ISO-8601 timestamps and full commit SHAs.

## Layout

```text
.hub/
|-- registry/<repo-id>.yaml
|-- mirrors/<repo-id>.git/
|-- radar/dashboard.md
|-- radar/repos/<repo-id>.yaml
|-- locks/radar/run/
|-- locks/repos/<repo-id>/
|-- locks/items/<repo-id>/<work-item-id>/
|-- work-items/<repo-id>/<work-item-id>/
|   |-- brief.md
|   |-- requirements.md
|   |-- technical-solution.md
|   |-- plan.md
|   |-- test-plan.md
|   |-- index.md
|   |-- decisions.md
|   `-- assignments/<assignment-id>.md
`-- worktrees/<repo-id>/<work-item-id>/
    |-- integration/
    |-- baseline/
    |-- validation-<role>/
    `-- <assignment-id>/
```

Do not put credentials, tokens, or copied GitHub secrets in any record.

## Repository record

Each registry file contains:

- `schemaVersion: 1`
- stable `repoId`
- GitHub `owner` and `name`
- canonical clone `url`
- `remote: origin`
- remote `baseBranch`
- local `mirrorPath`
- `enabled` boolean
- integer `priority`, lower values first
- `registeredAt`, `lastFetchedAt`, and `lastFetchedSha`
- complete seven-state `lifecycleLabels` mapping, defaulting to the labels in `radar.md`
- `githubLifecycleAuthorized` boolean and authorization time; when true, it covers only ensuring Hub lifecycle labels, replacing those labels, maintaining one Hub-owned status comment, and closing a linked issue after its PR is confirmed merged

Registration validates that `repoId` contains only lowercase letters, digits, and hyphens; the remote resolves; the detected base branch exists; and no other record uses the same `repoId` or `owner/name`.

## Work-item record

`brief.md` is canonical intake. It contains repository identity, stable work-item ID, source (`chat`, `issue`, `pull-request`, or `radar`), linked issue and pull-request numbers when present, Hub-owned remote branch, Hub-owned status-comment ID, goal, acceptance criteria, non-goals, target base SHA, and source head SHA for pull-request repair work.

`requirements.md` is the accepted Product specification. It contains a stable requirement ID and revision, audience and user outcome, functional and non-functional requirements, acceptance criteria, observable failure behavior, constraints, assumptions, and non-goals.

`technical-solution.md` is the accepted Architect solution. It contains a stable solution ID and revision, source requirement revision and SHAs, current-state evidence, design and interfaces, schemas and data flow when relevant, compatibility and migration behavior, dependency DAG, write scopes, operational design, and implementation risks. DevOps planning evidence is incorporated into its build and delivery constraints.

`plan.md` is the approval manifest. It contains a plan ID, exact requirement and technical-solution revisions, target base SHA, source head SHA when planning pull-request repair, assignments, final checks, risks, and approval state. Approval binds the plan ID, both artifact revisions, target base SHA, and any source head SHA. A change to any bound value invalidates approval.

`test-plan.md` is created only after plan approval. Tester appends the complete proposed test-plan body to the result section of its own `test-design` assignment file; it does not write the shared artifact directly. Lead reviews that body and copies the accepted version into `test-plan.md`. It contains a stable test-plan ID and revision, exact requirement and technical-solution revisions, source plan ID and SHAs, an acceptance-to-scenario matrix, test levels, positive/negative/boundary/regression scenarios, fixtures and data, environment needs, exact commands where known, baseline comparison, observability needs, scenario ownership, pass criteria, and delivery blockers. Scenario ownership distinguishes Developer white-box unit/component tests from Tester/SDET black-box acceptance, API, contract, end-to-end, and cross-component automation. Every implementation assignment depends on accepted `test-design` and references all three readiness-artifact revisions. Any amended requirement, technical solution, plan ID, target base SHA, or repair source head SHA invalidates downstream approvals and the accepted test plan; after new plan approval, Tester must revise it and Lead must publish a newly accepted revision before any further implementation or integrated-validation assignment.

The pre-implementation readiness predicate is true only when `requirements.md`, `technical-solution.md`, and `test-plan.md` are all `ACCEPTED`, their revision links agree, the plan approval remains valid, and their bound SHAs match current work-item state. No source-writing assignment may become ready otherwise.

`index.md` is lead-owned and contains exactly one work-item state:

`DETECTED`, `PLANNING`, `AWAITING_APPROVAL`, `EXECUTING`, `VALIDATING`, `PR_READY`, `PR_OPEN`, `NEEDS_DECISION`, `BLOCKED`, or `DONE`.

It also lists assignment state, current native agent handle, branch, worktree, commit, retry count, and evidence location. Queue history never proves that an agent is alive; confirm native status.

`decisions.md` is append-only. Record user approvals and material choices with time, affected plan or delivery action, and exact authorized scope.

## Assignment file

Lead creates one file per assignment before dispatch. Agent appends its result; lead never rewrites returned evidence.

Required contract fields:

- assignment ID and role
- responsibility and acceptance evidence
- dependency assignment IDs
- owned relative write paths, or explicit read-only scope
- source base or prerequisite commit
- branch and absolute worktree path
- required validation
- expected terminal status
- requirement, technical-solution, and test-plan IDs and revisions plus owned scenario IDs for every implementation assignment
- test ownership class: `developer-white-box`, `sdet-black-box`, or `not-applicable`

Required result fields:

- agent result status: `PASS`, `FAIL`, `NEEDS_DECISION`, or `BLOCKED`
- concise summary
- changed paths and commit SHA, when write-capable
- exact validation commands and outcomes
- remaining risks
- precise next action or unblocker

`PASS` is evidence, not acceptance. After review, the Lead maps a valid `PASS` to assignment state `ACCEPTED`. `FAIL` triggers the one allowed repair or maps to `BLOCKED`; the other result statuses map directly to their same-named assignment states.

Reject results with changed paths outside owned scope, missing validation evidence, ambiguous commit ownership, or a claimed success while required checks fail.

## Locks

Acquire locks through atomic directory creation. One radar run owns `.hub/locks/radar/run/`; fetches, remote-tracking-ref updates, worktree add/remove operations, integration cherry-picks, and rebases own `.hub/locks/repos/<repo-id>/` only for their mutation batch; an active work item owns `.hub/locks/items/<repo-id>/<work-item-id>/`. Independent Developers committing on already-owned disjoint branches rely on worktree ownership and Git's branch-ref locks, not the repository-wide lock. Record owner thread or automation identity, acquisition time, and purpose inside each lock.

Do not remove a lock only because it is old. Confirm its owner is inactive and inspect worktree/ledger state first. Radar skips locked work items and records them as deferred.
