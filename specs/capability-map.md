# Capability Map: Checklist-Governed Software Squad

Status: `APPROVED`

Approved on: 2026-10-15

## Initiative outcome

Hub coordinates a software-delivery team through one user-facing Lead. The
Lead turns GitHub work into approval-bound, evidence-backed checklists, fans
out bounded assignments to isolated subagents, supervises progress and stalls,
and integrates only accepted results. Every Lead and worker action remains
visible through durable Hub records.

## Modules

| Module ID | Responsibility | Depends on |
| --- | --- | --- |
| `checklist-ledger` | Define checklist templates, instantiated items, ownership, dependencies, evidence, status transitions, and revision history for Lead and worker assignments. | — |
| `lead-control-channel` | Make the Lead the sole user-facing channel and convert follow-ups, approvals, cancellations, and decisions into durable change requests. | `checklist-ledger` |
| `gate-enforcement` | Evaluate checklist predicates before dispatch, source writes, handoffs, integration, lifecycle transitions, delivery, and completion. | `checklist-ledger`, `lead-control-channel` |
| `agent-supervision` | Bind assignments to native agent handles, track liveness and progress, detect stalls, and support inspection, prompting, retry, interruption, and reassignment without losing evidence. | `checklist-ledger`, `gate-enforcement` |
| `isolated-execution` | Govern assignment branches and worktrees, path ownership, dependency-safe fan-out, commit acceptance, and ordered integration. | `gate-enforcement`, `agent-supervision` |
| `progress-dashboard` | Present Lead and worker checklists, activity, blockers, dependencies, evidence, stalled state, approvals, and next actions in one inspectable view. | `checklist-ledger`, `agent-supervision`, `isolated-execution` |
| `delivery-lifecycle` | Apply checklist gates to planning, readiness artifacts, integrated validation, delivery approval, pull-request monitoring, and safe post-merge cleanup. | `isolated-execution`, `progress-dashboard` |

## Dependency order

```text
checklist-ledger
    ↓
lead-control-channel
    ↓
gate-enforcement
    ↓
agent-supervision
    ↓
isolated-execution
    ↓
progress-dashboard
    ↓
delivery-lifecycle
```

Module IDs are stable. Each module receives its own specification and proceeds
through specification, planning, task decomposition, and implementation in
dependency order.

## First-release proof

The first release proves one complete path:

1. The Lead creates its checklist for a GitHub issue.
2. Planning produces role and issue-specific worker checklists.
3. The user approves the exact plan through the Lead.
4. Tester planning satisfies the readiness gate.
5. The Lead fans out at most three non-overlapping assignments.
6. Workers report progress and evidence against checklist items.
7. A simulated stalled worker is detected and safely reassigned.
8. The Lead integrates accepted work.
9. The dashboard reaches `PR_READY`.
10. Hub stops for separate delivery approval.

## Initiative boundaries

### Always

- Keep `.hub/` as ignored local runtime state and Hub as the control plane.
- Route user communication through the Lead.
- Preserve existing implementation and delivery approval gates.
- Preserve worker worktrees, branches, evidence, and history during recovery.
- Limit concurrency to the Lead plus at most three workers.

### Ask first

- Change an approved plan, readiness artifact, checklist contract, or scope.
- Push a branch or create a pull request.
- Expand beyond the approved first-release boundary.

### Never

- Permit direct user-to-worker control messages.
- Let a worker accept its own gated evidence or integrate its own work.
- Merge, deploy, publish, discard user changes, or destructively recover
  automatically.
- Treat prompt-level workflow enforcement as operating-system isolation.

## First-release exclusions

- Web UI, external database, or remote Hub server.
- Arbitrary team topologies or worker-to-worker delegation.
- More than three concurrent workers.
- Automatic merge or deployment.
- Destructive automated recovery.
- Per-worker operating-system sandboxing.
