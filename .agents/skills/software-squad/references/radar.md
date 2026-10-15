# Active Radar

Use this reference for `$software-squad radar` and for the hourly Codex heartbeat that watches registered GitHub repositories. Radar observes and plans. It never implements, pushes, opens a pull request, merges, or changes target-repository source code.

## Schedule and scope

- Run once per hour as a heartbeat attached to one durable Hub chat on the configured local Codex host. Every scheduled iteration must resume that same chat; never create a standalone chat per scan. Allow an explicit manual run at any time.
- Keep durable truth in `.hub/` records, not only chat history. The heartbeat may use prior chat context for continuity, but every run must reread registry, locks, dashboard, repository snapshots, and active work-item state before acting.
- If the heartbeat is reconfigured or moved, bind it to exactly one replacement Hub chat and archive obsolete radar-run chats. Never leave both cron and heartbeat schedules active for the same Hub.
- On first run, create missing `.hub/registry/`, `.hub/radar/repos/`, and lock directories plus an empty dashboard. A missing or empty registry is a successful zero-repository scan, not an error.
- Acquire `.hub/locks/radar/run/` before scanning. This prevents overlapping scheduled or manual radar runs while still allowing one run to scan distinct repositories in parallel.
- Scan every enabled repository in `.hub/registry/`, even after finding a candidate. One unhealthy repository must not prevent other repositories from being observed.
- Treat the registry as the source of scan scope. Do not discover or enroll repositories implicitly.
- Record the scan start, finish, and outcome per repository. A later successful run must catch up from current remote state after a missed or failed run.
- Keep routine scans silent. Write state only to `.hub/radar/dashboard.md` and per-repository radar snapshots. Notify only on meaningful change, completion requiring attention, failure, or required user action, unless user explicitly requests a status report.

## Mirror refresh and read-only inspection

Each registered repository owns a managed bare mirror at `.hub/mirrors/<repo-id>.git`.

1. Acquire `.hub/locks/repos/<repo-id>/` before changing the bare cache. Different repositories may be scanned concurrently.
2. Validate mirror identity against registered GitHub `owner/name`, clone URL, and default branch.
3. Fetch and prune remote refs in the bare mirror. Never fetch through a user checkout.
4. Resolve and record exact default-branch commit SHA after fetch.
5. Inspect commits, trees, workflow definitions, issues, pull requests, reviews, and checks without creating a worktree.
6. Release repository lock after publishing its snapshot. After every scan finishes, the radar lead alone selects the candidate and atomically publishes the shared dashboard while still holding the run lock.

Radar planning reads files with bare-repository operations at the recorded SHA. It must not create branches, worktrees, commits, labels beyond the Hub lifecycle transition described below, or source edits. Worktrees begin only after user approves a decision-complete plan.

If fetch or GitHub API access fails, preserve last known good snapshot, mark current observation stale with error time and concise reason, continue scanning other repositories, and retry next run. Never present stale data as current.

## Dashboard contract

Maintain one row or section per registered repository with:

- stable repository ID and GitHub `owner/name`;
- enabled state and configured priority;
- default branch, last fetched SHA, last successful fetch time, and observation freshness;
- default-branch health, including required checks and newly failing checks;
- count and oldest age of open `hub:ready` issues;
- active Hub work-item ID, state, base SHA, and lock owner when present;
- linked Hub pull request, mergeability, branch divergence, review decision, requested changes, and required-check summary;
- latest radar outcome, selected candidate reason, blocking reason, and next action;
- stale worktree or stale-lock warning when detected.

Order dashboard entries by actionable severity, then configured repository priority, then `owner/name`. Publish snapshots and dashboard atomically so partial scans cannot corrupt prior state. Do not create routine chat messages, GitHub comments, issues, or pull requests merely because dashboard values changed.

## Candidate selection and auto-planning

After all enabled repositories have been scanned, select at most one new candidate for auto-planning. Use this global priority order:

1. Hub-owned pull request with new requested changes, failed required checks, or broken branch state.
2. Oldest open issue labeled `hub:ready`.
3. Newly failing default branch, compared with the prior successful radar snapshot.

Within the same class, prefer configured repository priority, then oldest detection time, then lexical `owner/name` for a deterministic tie-break. Skip candidates that are already represented by active non-`PR_OPEN` work, have a live execution lock, are disabled, are stale because fetch failed, or cannot be inspected with current credentials. An actionable Hub-owned `PR_OPEN` item is the explicit exception: refresh that same item as repair work instead of creating a duplicate.

For the chosen candidate only:

- acquire an atomic candidate lock, then release it after the plan and dashboard state are durably published; `$software-squad run` acquires a fresh execution lock after approval;
- create or refresh one canonical brief from the bare mirror's exact recorded base SHA;
- execute the same read-only pre-approval planning DAG defined in `orchestration.md`: Product produces the requirement specification while Architect performs architecture discovery and DevOps captures constraints; after all three are accepted, Architect produces the technical solution;
- have the Lead accept and publish `requirements.md` and `technical-solution.md`, then publish a decision-complete `plan.md` approval manifest bound to their exact revisions and the recorded SHAs;
- move the work item to `PLANNING`, then `AWAITING_APPROVAL` when the plan is complete;
- do not enter `AWAITING_APPROVAL` unless both prerequisite artifacts are accepted and the approval manifest passes the readiness checks in `contracts.md`; on capacity interruption, retain `PLANNING` and resume the DAG idempotently;
- record all three artifact paths and revisions, source issue or pull request, base SHA, reason, and approval requirement in the dashboard;
- stop. Do not launch developer agents, create source worktrees, make commits, push, or open a pull request.

If planning fails, mark the work item `BLOCKED` with recoverable evidence rather than selecting a second candidate during the same scan. Manual radar reruns obey the same one-candidate limit unless user explicitly requests planning for a named item.

## Locks and idempotency

- Use one radar run lock, per-repository locks, and per-work-item coordination locks containing owner identity, acquisition time, heartbeat time, repository ID, and work-item ID when applicable.
- A live work-item lock makes all other coordinators observe-only. They may refresh dashboard facts but must not mutate that work item.
- Repeated scans over unchanged remote state must produce the same candidate decision and must not duplicate work items, plans, labels, or status comments.
- Key issue-derived work by repository ID plus issue number. Identify a Hub-owned pull request by a recorded work-item PR number and head branch `hub/<work-item-id>/integration`, then refresh that same work item for repairs. If its local audit record is unavailable or disagrees with the head branch, mark the candidate `BLOCKED`; do not synthesize a replacement identity or adopt the branch.
- Recover a stale lock only after confirming its owner is inactive. Record recovery in the work-item ledger. When ownership cannot be confirmed, leave the lock and mark `NEEDS_DECISION` or `BLOCKED`.
- Write new state to a temporary sibling and rename atomically. Retain last known good state when publication fails.

## GitHub lifecycle

Use these mutually exclusive labels for Hub-controlled issue state:

- `hub:ready`: eligible for radar selection;
- `hub:planning`: plan generation in progress;
- `hub:awaiting-approval`: plan complete; user approval required;
- `hub:in-progress`: approved implementation running;
- `hub:pr-open`: linked pull request open;
- `hub:blocked`: user decision or external recovery required;
- `hub:done`: linked work merged and complete.

During registration, ask once for repository-specific lifecycle authorization and persist it in the registry record. When authorized, ensure the configured seven lifecycle labels exist. Without authorization, radar keeps lifecycle state local and performs no GitHub writes.

Every label transition removes only values in that repository's configured `lifecycleLabels` set, then applies exactly the mapped new state label in one idempotent operation. Never wildcard-delete labels by prefix. Radar may advance a selected issue from ready to planning, then awaiting approval. Maintain at most one Hub-owned status comment per issue, store its comment ID, and update it in place for meaningful lifecycle changes; never append hourly comments. Label or comment failure must be recorded without losing local plan state.

## Work-item transitions

For an authorized linked issue, apply local state, label, status comment, and lock changes as one recoverable transition:

| Event | Local state | Lifecycle label | Lock and cleanup |
| --- | --- | --- | --- |
| Candidate detected | `DETECTED` | `hub:ready` | no item lock |
| Planning starts | `PLANNING` | `hub:planning` | temporary candidate lock |
| Plan ready | `AWAITING_APPROVAL` | `hub:awaiting-approval` | publish plan, release candidate lock |
| Plan approved | `EXECUTING` | `hub:in-progress` | acquire execution item lock |
| Final checks start | `VALIDATING` | `hub:in-progress` | retain item lock |
| Local gates pass | `PR_READY` | `hub:in-progress` | retain item lock until delivery or pause |
| PR created | `PR_OPEN` | `hub:pr-open` | release execution lock after state is durable |
| Existing-PR repair pushed after delivery approval | `PR_OPEN` | `hub:pr-open` | update existing PR record; release execution lock after state is durable |
| Material choice needed | `NEEDS_DECISION` | `hub:blocked` | stop writers, preserve state, release item lock |
| Terminal failure | `BLOCKED` | `hub:blocked` | stop writers, preserve state, release item lock |
| PR merged | `DONE` | `hub:done`, then close issue | clean eligible worker state; retain integration and audit state |
| PR closed unmerged | `BLOCKED` | `hub:blocked` | retain worktrees and evidence |

When planning repairs for an already open Hub PR, keep the issue label `hub:pr-open` while local state is `PLANNING` or `AWAITING_APPROVAL`; record repair state in the Hub-owned comment and dashboard. Without a linked issue or lifecycle authorization, perform only local state and lock changes.

Radar observes active pull requests and records:

- open, closed, or merged state;
- head SHA and divergence from current default branch;
- mergeability and conflict state;
- required check names and pending, passing, or failing result;
- review decision, approvals, requested changes, and latest actionable review time.

New requested changes or failed required checks on a Hub pull request make it highest-priority planning input, but repair work still requires a revised plan and user approval before implementation. Store both target base SHA and PR head SHA. Keep any linked issue at `hub:pr-open` while the local repair plan waits for approval; show repair approval state in the dashboard and work-item record. Radar never dismisses reviews, reruns checks, pushes repairs, merges, or closes a pull request.

When lifecycle authorization exists and a linked pull request is confirmed merged, set the issue to `hub:done` and close it. When a linked pull request closes unmerged, set the issue to `hub:blocked` and keep it open. Set `hub:pr-open` only after a pull request actually exists. These reconciliation actions must remain idempotent.

## Approval boundary

`AWAITING_APPROVAL` is a hard gate. Radar may gather evidence and improve a plan while preserving its recorded base SHA, but it must not begin implementation. After explicit user approval, `$software-squad run` owns worktree creation and squad execution under the separate execution protocol. Delivery still requires its own explicit approval before any push or pull-request creation.
