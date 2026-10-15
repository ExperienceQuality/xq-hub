# Git Lifecycle

Use this lifecycle for every repository managed by Software Squad. Treat the Hub workspace as the control plane and each target repository as independently owned source code. Never edit a user's existing checkout to perform Hub work.

## Runtime layout

All mutable Hub runtime data lives under the ignored `.hub/` directory in the Hub workspace:

```text
.hub/
  mirrors/<repo-id>.git/
  worktrees/<repo-id>/<work-item-id>/
    integration/
    baseline/
    validation-<role>/
    <assignment-id>/
  registry/<repo-id>.yaml
  work-items/<repo-id>/<work-item-id>/
  locks/radar/run/
  locks/repos/<repo-id>/
  locks/items/<repo-id>/<work-item-id>/
```

`repo-id` must be stable, filesystem-safe, and unique. For GitHub repositories, derive it from the lower-case `owner/name` identity, replacing `/` and every character outside `[a-z0-9-]` with `-`. Collapse repeated hyphens. If that value collides, append a short hash of the canonical clone URL. Do not change an existing ID when a remote URL spelling changes but still resolves to the same GitHub repository.

`work-item-id` and `assignment-id` must be stable identifiers from the Hub record, not free-form issue titles. Branch components must use the same filesystem-safe character set.

## Registering a repository

Registration is explicit. Do not discover and enroll nearby repositories automatically.

1. Resolve the supplied remote to a canonical GitHub `owner/name` and clone URL. Reject ambiguous remotes and repository names that cannot be verified.
2. Verify read access with a non-mutating remote query. Detect the remote default branch from its symbolic `HEAD`; do not assume `main` or `master`.
3. Reject registration if the canonical repository is already registered under another active record. Re-registration of the same repository must be idempotent.
4. Create `.hub/mirrors/<repo-id>.git` as a managed bare cache, add remote `origin`, and map remote heads to `refs/remotes/origin/*` with the quoted fetch refspec `'+refs/heads/*:refs/remotes/origin/*'`. Fetch tags and prune remote-tracking refs. Do not use `git clone --mirror` or `+refs/*:refs/*`; Hub work branches must live safely under separate `refs/heads/*`.
5. Record canonical identity, clone URL, default branch, enabled state, priority, mirror path, fetched base SHA, and fetch timestamp only after the mirror is usable.
6. For GitHub repositories, ensure Hub lifecycle labels exist only when the user has authorized that remote mutation. Label creation is not required to complete local registration.

Never adopt, move, clean, reset, or reconfigure a user's existing clone as the managed mirror. A local source path may help resolve a remote URL, but Hub still creates its own mirror.

## Mirror synchronization

The managed mirror is the canonical local Git source for radar, planning, and worktree creation.

- Fetch and prune before every radar scan that needs current remote state.
- Fetch and prune before creating an approved work item.
- Fetch and prune before integration if an assignment has been long-running or remote movement could affect it.
- Fetch and prune immediately before delivery.
- Resolve the base from the remote-tracking ref after fetch and persist the exact commit SHA used by each phase.
- Never force-update, delete, or push remote refs during a fetch operation.
- A fetch failure leaves the last successful snapshot intact but stale. Record the failure and stop any action that requires current remote truth.

Planning may inspect commits and trees directly in the bare mirror. It must not create a planning worktree or change refs.

## Work item creation

Create worktrees only after the user approves the plan.

1. Acquire an atomic lock for the repository and work item. If a lock already exists, verify its owner is inactive before recovering it; age alone does not prove a lock is stale.
2. Fetch the mirror and resolve the current default-branch SHA.
3. Compare that SHA with the plan's base SHA. Apply the base-movement gate below before creating branches.
4. Create the integration branch `hub/<work-item-id>/integration` at the approved base SHA.
5. Create the integration worktree at `.hub/worktrees/<repo-id>/<work-item-id>/integration`.
6. Create the detached baseline view needed by test design, then dispatch the read-only `test-design` assignment. Do not create Developer worktrees yet.
7. After Lead accepts and publishes `test-plan.md`, create only dependency-free implementation assignments. This includes Developer work and any Tester/SDET `test-automation` work. Branch each one from the current integration SHA as `hub/<work-item-id>/work/<assignment-id>` at `.hub/worktrees/<repo-id>/<work-item-id>/<assignment-id>`.
8. After prerequisite commits are accepted and cherry-picked, create newly unblocked assignment branches from the full current integration SHA, including every accepted assignment already integrated. Record that exact source SHA in the assignment. Never synthesize a prerequisite-only base or dispatch a dependent assignment from the original base.
9. Use detached worktrees for isolated tester, reviewer, or DevOps validation. Validation must never create an implicit delivery branch.

Hold `.hub/locks/repos/<repo-id>/` only while fetching, adding or removing worktrees, cherry-picking into integration, or rebasing. Record each mutation batch, then release the repository lock so radar and later phases can proceed. Keep the separate work-item lock for the full active execution. Developers may commit concurrently on their already-owned disjoint branches without holding the repository-wide lock.

For repair of an existing Hub pull request, use a separate path:

1. Require the work-item record to identify the exact pull-request number, remote head branch, target base SHA, and approved source head SHA.
2. Fetch the remote head and prove that its SHA, branch `hub/<work-item-id>/integration`, pull-request number, and repository match the Hub record. If provenance cannot be proven, set `BLOCKED`; never adopt or overwrite the branch.
3. If the fetched source head differs from the SHA bound by the accepted technical solution and approval manifest, stop. Architect must revalidate or revise `technical-solution.md` against the new source head, Lead must accept it, the user must approve a newly bound manifest, and Tester must revise the test plan before any repair writer or validation runs.
4. Reuse the recorded local integration branch when it matches the newly approved source head. Otherwise create it at the fetched source head SHA, not at the target base SHA.
5. Rejoin normal steps 6-7: create the detached baseline view, dispatch read-only `test-design`, and accept/publish its test plan before creating any repair Developer or Tester/SDET worktree.
6. Create repair assignments from that integration SHA under `hub/<work-item-id>/work/<assignment-id>`, make each depend on the accepted test-plan revision, and integrate them normally.
7. Delivery updates the same recorded remote head branch and existing pull request. Never open a second pull request for repair work.

Before reusing any branch or path, prove it belongs to the same Hub work-item record. A name collision with unrecognized state is a blocking condition, not permission to overwrite it.

Each write assignment owns its worktree and the paths declared in its approved scope. Reject commits that modify files outside that scope or include unexplained generated artifacts. Do not let multiple agents write in the same worktree.

## Base-movement gate

When the fetched default branch differs from the plan's base SHA, compare the commit range and changed paths with the approved plan:

- Refresh impact analysis for every base SHA change, even when changed paths appear independent. Architect must revalidate or revise `technical-solution.md` against the new full SHA and Lead must accept its new revision. Revalidate `requirements.md` too when the change affects its evidence, constraints, assumptions, or acceptance criteria.
- Amend the approval manifest to bind the accepted readiness-artifact revisions and new base SHA. Obtain new user approval before work begins.
- Tester must revise `test-plan.md` against the newly accepted requirement and technical-solution revisions; Lead must accept it before any source-writing or validation assignment resumes.
- Highlight overlaps with planned files, interfaces, dependencies, tests, or acceptance criteria; if impact cannot be determined, treat it as overlap.
- Never silently build from an outdated base or merge the new base into assignment branches to bypass this gate.

Persist the old SHA, new SHA, comparison result, and decision in the work-item record.

## Assignment commits and integration

Every accepted developer assignment produces one focused commit unless the approved plan explicitly requires a preserved commit series.

- Require a clean assignment worktree before accepting its result.
- Confirm the commit descends from the assignment's approved base.
- Inspect the full diff and changed-file list against assignment ownership.
- Require assignment-level checks and evidence before acceptance.
- Reject merge commits, unrelated formatting, unrelated lockfile changes, and changes to Hub runtime records from target-repository branches.

The lead integrates accepted assignments by cherry-picking their exact commit SHAs onto `hub/<work-item-id>/integration` in dependency order. Record source branch, source SHA, resulting integration SHA, and check evidence. Resolve conflicts only within approved scope. A conflict that changes interfaces or assumptions requires updated analysis; do not guess through it.

Do not push assignment branches. The integration branch is the only delivery branch unless the approved plan says otherwise.

## Baseline-aware validation

Validation compares the approved base commit and the current integration commit using the same commands and materially equivalent environment.

1. Detect required checks from repository instructions, CI configuration, package/build manifests, and the approved plan.
2. Run the relevant checks against a detached worktree at the approved base and capture command, exit status, and concise failure signature.
3. Run the same checks against the integration commit.
4. Classify each integration failure as new, changed, or unchanged relative to the base evidence.

A failure may be accepted as pre-existing only when the same check fails at the base with the same material cause and the change does not worsen its scope or severity. Missing dependencies, unavailable services, timeouts, and flaky results are not automatically equivalent. Rerun or investigate enough to make the comparison reliable.

Every new or worsened failure blocks delivery. Documented unchanged baseline failures may remain only when they do not prevent evaluation of the requested change. If the base itself cannot be validated, report the limitation and require an explicit user decision before treating validation as sufficient.

## Delivery gate

Passing local validation makes a work item `PR_READY`; it does not authorize any remote write.

Before push or PR creation:

1. Present integrated commit, diff summary, check evidence, baseline exceptions, and proposed remote branch and PR metadata.
2. Fetch and prune again. Resolve the latest default-branch SHA before requesting delivery approval.
3. If the target base SHA changed, invalidate the current technical-solution acceptance, plan approval, and accepted test plan. Return to impact analysis; require Architect to revalidate or revise the technical solution, Lead to accept it, and the user to approve a new manifest bound to the current readiness revisions and base SHA. Tester must then revise the test plan and Lead must accept/publish its new revision before any conflict-repair writer or final integrated validation. Rebase `hub/<work-item-id>/integration` and rerun the full validation suite against that accepted revision. If conflicts arise, make at most one ownership-routed scoped repair attempt; mark the item blocked when safe repair is unclear.
4. Present the final rebased commit and refreshed evidence, then obtain explicit user approval to deliver. Earlier implementation approval does not count as delivery approval.
5. Immediately before push, fetch once more. If the target base changed again, invalidate delivery approval and repeat step 3; never push a commit approved against an older target base.
6. For a new work item, push only `hub/<work-item-id>/integration`, then open one PR with the approved base branch. For existing-PR repair, push to the proven recorded head branch and update the existing PR; never open another PR. Record remote branch, PR URL, and final commit SHA.

Never push, create a PR, update an issue, create labels, merge, close, or delete remote state without authorization covering that action. Never use `--force`; if a previously authorized delivery requires rewriting a Hub-owned remote branch, stop and request specific approval for `--force-with-lease` after explaining why.

The Hub never merges its own PR automatically.

## Remote tracking and merge confirmation

Radar may read PR, review, check, issue, and branch state without mutating it. Treat a PR as merged only when GitHub reports a merge commit or merged state and the result is reachable from the registered default branch after fetch.

A green check suite or closed PR is not merge confirmation. A closed, unmerged PR keeps the work item and linked issue blocked. If the default branch changes independently, retain the audit record and determine whether the work was superseded rather than assuming success.

## Cleanup

Cleanup is allowed only after confirmed merge or explicit user cancellation. Before removing anything, resolve exact paths and branch ownership from Hub records and inspect every worktree.

- Never delete or reset a dirty worktree.
- Never delete untracked or ignored files. They may be user data or unrecoverable agent output.
- If a worktree contains changes or unknown files, preserve it and report the exact path and status.
- Remove clean worker and detached validation worktrees after confirmed merge.
- Delete only local worker branches that are fully integrated and recorded as Hub-owned.
- Do not delete remote branches unless the user explicitly requests it.
- Retain the integration worktree, integration branch, work-item ledger, final evidence, and PR linkage for recovery and audit unless a separate retention policy explicitly authorizes archival.
- Prune worktree metadata only after verifying the corresponding directory is gone or intentionally preserved.
- Release the work-item lock only after state and cleanup results are durably recorded.

Never run destructive cleanup against `$HOME`, `~`, the Hub root, a repository root, unresolved variables, globs, or paths inferred only from branch names. Use exact absolute paths beneath the recorded `.hub/worktrees/<repo-id>/<work-item-id>/` directory.

## Dirty or unexpected state

User data wins over automation. Stop and report instead of repairing automatically when any managed path contains:

- staged or unstaged changes not recorded by the owning assignment;
- untracked or ignored files whose provenance is unknown;
- a checked-out branch or `HEAD` different from the Hub record;
- commits not recorded by the Hub;
- a remote URL or default branch changed outside registration;
- an active process or agent that may still own the worktree or lock.

Safe inspection commands are allowed. Do not use `git reset --hard`, `git clean`, `git checkout --`, broad file deletion, branch overwrite, or worktree removal to recover automatically. Preserve evidence, mark the affected assignment or work item `NEEDS_DECISION` or `BLOCKED`, and ask the user for direction.
