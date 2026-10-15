# Subagent Orchestration Protocol

Use this protocol whenever the Lead delegates Software Squad work to Codex subagents. Durable Hub records are authoritative; native agent handles prove liveness and enable supervision. A queued assignment without a live handle is not running.

## Lead invariants

- The Lead remains active until the work item reaches a terminal or user-gated state. It owns shared records, approvals, scheduling, acceptance, integration, and final reporting.
- Use `spawn_agent` to start a new role assignment, `list_agents` to reconcile native handles and status, `wait_agent({timeout_ms})` to wait for mailbox changes, and `followup_task` to request the single allowed repair from an existing usable agent. Use `send_message` only for non-task coordination; it does not replace a durable assignment or trigger idle work.
- Use `interrupt_agent` only when the user cancels or supersedes active scope, or safety requires stopping an active writer. Record `INTERRUPTED`, preserve its worktree and evidence, and recompute readiness before further dispatch.
- Keep at most three subagents active because the Lead occupies the fourth slot.
- Create or update the assignment file and roster entry before dispatch. Record the returned native agent handle immediately after `spawn_agent` succeeds.
- Dispatch only dependency-free assignments whose scopes do not overlap semantically. No source-writing Developer or Tester/SDET may start until the accepted requirement specification, technical solution, and test plan form a revision-consistent readiness bundle.
- Never infer completion from files, commits, queue history, silence, or elapsed time. Confirm native status and inspect the returned evidence.
- Never let a subagent edit shared Hub coordination records. The Lead alone writes `brief.md`, `plan.md`, `index.md`, `decisions.md`, the roster, integration records, and dashboard state. An agent may append only its assigned result section when explicitly instructed.

## Durable roster

Maintain the active roster in the work item's `index.md`. One row per dispatched assignment must contain:

- assignment ID and role;
- native agent handle returned by `spawn_agent`;
- native status: `RUNNING`, `IDLE`, `COMPLETED`, `FAILED`, `INTERRUPTED`, or `UNKNOWN`;
- durable assignment state: `QUEUED`, `ACTIVE`, `ACCEPTED`, `NEEDS_DECISION`, or `BLOCKED`;
- dependency IDs and source commit SHA;
- read-only view or exact branch and absolute worktree path;
- owned write paths, or `read-only`;
- dispatch time, last observed time, retry count, and evidence location.

Update native status only from agent-tool results. Update durable state only after evaluating assignment evidence. Native `COMPLETED` does not mean `ACCEPTED`.

## Required phase order

### 1. Pre-approval planning

1. Bind all planning work to the same full base SHA.
2. Create three read-only assignments: `requirements` for Product, `architecture-discovery` for Architect, and `devops-constraints` for DevOps.
3. Spawn all three in parallel when three worker slots are available.
4. Reconcile planning handles with `list_agents`, use `wait_agent({timeout_ms})`, and evaluate each result as it arrives. Product must return a complete requirement specification. Lead reviews and publishes accepted Product content as `requirements.md`.
5. After `requirements`, `architecture-discovery`, and `devops-constraints` are accepted, create a distinct `technical-solution` assignment depending on all three. Prefer `followup_task` on the same Architect handle; because this is a new planned assignment ID rather than repair, it does not consume the discovery assignment's retry. Lead reviews and publishes accepted Architect content as `technical-solution.md`. Reject unsupported, inconsistent, or incomplete artifacts.
6. Create one decision-complete approval manifest binding exact requirement and technical-solution revisions, acceptance criteria, non-goals, interfaces, dependency DAG, write scopes, repository checks, risks, and SHAs.
7. End in `AWAITING_APPROVAL`. Do not create write-capable worktrees or dispatch Tester or Developers before direct user approval of this exact manifest and its two artifacts.

### 2. Approved test planning

After plan approval, Tester joins before implementation:

1. Fetch and apply the base-movement gate. Create the integration state only when approval remains valid.
2. Create one read-only Tester assignment bound to the approved requirement revision, technical-solution revision, plan ID, and base SHA. Dispatch Tester alone; do not run source-writing assignments concurrently with this assignment.
3. Require a durable test plan covering:
   - each acceptance criterion and its verification method;
   - unit, component, integration, end-to-end, or manual checks appropriate to the change;
   - regression risks and negative or failure-path cases;
   - baseline commands and expected comparison method;
   - fixtures, test data, environment, service, and tooling prerequisites;
   - assignment-level checks Developers must run;
   - integrated validation commands and pass/fail rules;
   - known limits, flake handling, and evidence format.
4. The Lead reviews the test plan against accepted `requirements.md`, `technical-solution.md`, and DevOps-prescribed checks. Missing coverage, infeasible commands, inconsistent revisions, or ambiguous pass conditions fail acceptance. If trustworthy coverage requires new scope, changed acceptance, or a new assignment absent from the approved plan, invalidate approval and return to planning instead of repairing the test plan around the gap.
5. For in-scope test-plan defects, use one `followup_task` repair on the same Tester handle when possible. If the handle cannot continue, spawn one replacement Tester with the original assignment, returned evidence, and requested corrections; this consumes the assignment's only retry.
6. Require Tester to append the complete proposed test-plan body to its own assignment result. Persist the accepted body as `test-plan.md`, record all three readiness-artifact revisions, and mark the assignment `ACCEPTED`. Only then may any source-writing assignment become dispatchable.
7. If the test plan remains unacceptable after one repair or reassignment, set the Tester assignment and work item to `BLOCKED`. Do not start implementation.

### 3. Implementation

1. Derive ready implementation assignments from the approved dependency DAG. An assignment is ready only when all three readiness artifacts and declared prerequisites are `ACCEPTED`, their revisions agree, prerequisite commits are integrated, its source SHA matches current integration, and no active assignment overlaps its scope.
2. Create dedicated branch and worktree only for ready assignments. Persist exact source SHA and ownership before dispatch.
3. Spawn up to three ready writers. Prioritize one ready Tester/SDET `test-automation` assignment plus up to two Developers. Prefer a new planned assignment on the same Tester handle that authored the test plan; it has its own retry allowance. If test automation is unnecessary or dependency-blocked, dispatch up to three Developers. Leave unused slots idle.
4. Reconcile all implementation handles with `list_agents`, then use `wait_agent({timeout_ms})` for the next mailbox change. Reconcile again after every wake. When one completes, inspect status, diff, changed paths, test-ownership boundaries, worktree cleanliness, commit ancestry, commit SHA, and required evidence before acceptance.
5. Reject out-of-scope writes, missing evidence, dirty worktrees, ambiguous commit ownership, merge commits, or failed required checks. A rejected or failed assignment receives one repair or reassignment only.
6. Cherry-pick accepted commits in DAG order. SDET automation authored in parallel is normally integrated after its dependent production commits, then must pass on integration; preserve explicit DAG order when different. Record resulting integration SHA. Recompute readiness and dispatch newly unblocked assignments from the full current integration SHA.
7. Continue the wait, inspect, integrate, and dispatch loop until every implementation assignment is `ACCEPTED`, `NEEDS_DECISION`, or `BLOCKED`. Never end the Lead turn while a writer remains active or a ready approved assignment remains undispatched.

### 4. Integrated validation

1. Bind Tester, Reviewer, and DevOps validation assignments to the same full integration SHA and dispatch them in parallel using isolated read-only views.
2. Prefer the same native Tester handle that authored the accepted test plan. Use `followup_task` with the validation assignment and current integration SHA when that handle is available.
3. If the original Tester is unavailable, spawn a replacement Tester. Its prompt must require reading the complete accepted test plan, approval-bound acceptance criteria, baseline evidence, and current integration diff before validation.
4. Tester executes the accepted test plan and classifies failures against baseline. Reviewer inspects correctness, maintainability, security, compatibility, and scope. DevOps runs repository-prescribed checks and assesses delivery readiness.
5. The Lead accepts validation only when all three results refer to the same integration SHA and contain required evidence. Any new commit invalidates prior validation results.
6. Route each blocking finding to its owner. A production or white-box unit/component defect creates the smallest scoped Developer repair assignment. A black-box acceptance, API, contract, end-to-end, cross-component, fixture, or harness defect creates a Tester/SDET `test-automation` repair in its own worktree. If expected behavior is disputed, set `NEEDS_DECISION` and return to planning; never weaken a test to make it pass. A correction to an existing assignment consumes that assignment's one remaining retry; a genuinely new defect requires a separately justified assignment and scope. After integration, rerun every validation gate affected by the changed code; rerun all three when impact cannot be isolated.
7. When every required validation assignment is accepted and no new or worsened failure remains, set `PR_READY` and stop at the separate delivery-approval gate.

## Dispatch algorithm

For each phase, the Lead repeats this scheduler loop:

1. Read durable work-item state, DAG, roster, and current native statuses.
2. Call `list_agents` and reconcile every `ACTIVE` roster entry with its native handle. Unknown or missing handles are not active and require resume handling.
3. Compute ready assignments: correct phase, dependencies accepted, source SHA current, scope non-overlapping, required approval valid, and capacity available.
4. Persist each new assignment contract as `QUEUED`, then call `spawn_agent`. Only after spawn succeeds may the Lead record its native handle and transition it to `ACTIVE`. A spawn failure records `dispatchError`, leaves it `QUEUED`, and does not consume its repair allowance.
5. Dispatch ready work up to the three-worker limit and record handles. For `followup_task`, keep the existing handle and transition back to `ACTIVE` only when follow-up dispatch succeeds.
6. Call `wait_agent({timeout_ms})` once with a long bounded wait for mailbox changes, then call `list_agents` and reconcile every running handle. New user input interrupts supervision; use `interrupt_agent` only for cancelled, superseded, or unsafe active work, then preserve evidence and reconcile state.
7. For every returned result, inspect durable and live evidence, then mark `ACCEPTED`, issue the one repair, request a user decision, or mark `BLOCKED`.
8. Integrate accepted write commits, update the DAG and source SHAs, then return to step 1.

Do not busy-poll. Do not wait on completed handles. Do not dispatch speculative work to occupy capacity.

## Role prompt template

Every `spawn_agent` or `followup_task` prompt must be self-contained and include these fields:

```text
ROLE: <Product|Architect|Developer|Tester|Reviewer|DevOps>
WORK_ITEM: <repo-id>/<work-item-id>
ASSIGNMENT: <assignment-id>
OBJECTIVE: <one bounded outcome>
PHASE: <planning|test-planning|test-automation|implementation|validation|repair|delivery>
REPOSITORY: <owner/name and mirror identity>
PLAN_ID: <approved plan ID, or pending during pre-approval planning>
BASE_SHA: <full SHA>
INTEGRATION_SHA: <full SHA or not-applicable>
VIEW: <read-only commit/tree or absolute worktree and branch>
WRITE_SCOPE: <exact relative paths, or read-only>
DEPENDENCIES: <assignment IDs and accepted commit SHAs>
INPUTS: <brief, plan, accepted test plan, baseline, prior findings, repository instructions>
ACCEPTANCE: <observable criteria for this assignment>
REQUIRED_CHECKS: <exact commands or discovery responsibility>
DELIVERABLES: <result fields and any durable artifact required>
CONSTRAINTS: <no shared-record edits, no scope expansion, no remote mutation, role-specific limits>
RESULT_FORMAT: status, summary, evidence, findings, decisions_needed, next
```

Developer prompts must state that one focused commit is required, white-box unit/component tests belong to the Developer, SDET-owned paths are forbidden, and out-of-scope changes stop work with `NEEDS_DECISION`. Tester test-planning prompts must state that no source or shared-record edits are allowed, name the destination the Lead will publish, and require complete test-plan content in the assignment result. Tester/SDET automation prompts require one focused commit, exact black-box test paths and scenario IDs, no production edits, baseline failure evidence when applicable, and integrated pass evidence. Validation prompts must name the exact integration SHA. Repair prompts must include rejected evidence, requested correction, and remaining retry count.

## Retry and reassignment

- A `followup_task` that dispatches a distinct preplanned assignment ID—such as `technical-solution`, `test-automation`, or integrated validation—is normal phase progression and does not consume another assignment's retry.
- Each assignment has one total repair or reassignment attempt, recorded as `retryCount: 1`. Follow-up and replacement are alternative uses of the same retry, not separate allowances.
- Prefer `followup_task` when the original handle is usable and retained context is valuable. Spawn a replacement only when the original agent failed, was interrupted irrecoverably, or cannot accept follow-up.
- A replacement prompt includes the original contract, all returned evidence, precise rejection reason, current SHA, and remaining work. It must not broaden scope.
- Same-cause failure after the retry sets assignment and work item `BLOCKED`. A newly discovered user decision sets `NEEDS_DECISION`. Do not silently create a third attempt or let another role repair code without a new recorded assignment.

## Resume after interruption

On Lead restart or interrupted supervision:

1. Read `brief.md`, accepted `requirements.md`, accepted `technical-solution.md`, approved `plan.md`, accepted `test-plan.md`, `index.md`, decisions, assignment files, locks, and Git state before dispatching anything. Verify that all three readiness artifacts remain accepted, their revision links agree, the manifest binds those exact revisions, and every bound base/source SHA still matches current state.
2. Call `list_agents` and match every recorded native handle. Reattach supervision to handles still running; never duplicate their assignments.
3. For completed handles, retrieve and evaluate results before changing durable state.
4. For missing, unknown, or irrecoverably interrupted handles, inspect their worktree read-only. Preserve all files and commits. Do not assume partial work is valid or clean it automatically.
5. If an assignment has trustworthy terminal evidence, evaluate it normally. Otherwise use its one reassignment attempt with the preserved worktree evidence and a fresh owned worktree or view. If retry was already consumed, mark `BLOCKED`.
6. Recompute DAG readiness from accepted durable assignments and current integration SHA. Never use roster history alone to infer dependency completion.
7. If approval binding, base SHA, integration SHA, lock ownership, branch provenance, or worktree state cannot be proven, set `NEEDS_DECISION` or `BLOCKED` before further dispatch.

## Terminal conditions

The Lead may stop active orchestration only when one of these conditions is durably recorded:

- `AWAITING_APPROVAL`: decision-complete plan exists and implementation approval is missing.
- `NEEDS_DECISION`: safe progress requires a specific user choice that agents cannot resolve.
- `BLOCKED`: required work failed after its one retry, provenance is unsafe, or an external prerequisite prevents progress.
- `PR_READY`: implementation and integrated validation are accepted; delivery approval is missing.
- `PR_OPEN`: approved remote delivery completed and radar owns monitoring.
- `DONE`: radar confirmed merge or recorded another authorized terminal resolution.

Before stopping, confirm no native agent remains running, no accepted-plan assignment is ready but undispatched, no completed result remains unevaluated, and the next authorized action is explicit. Never report success while any required assignment is `QUEUED`, `ACTIVE`, `NEEDS_DECISION`, or `BLOCKED`.
