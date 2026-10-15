---
name: software-squad
description: Coordinate a fixed software squad across registered GitHub repositories using active radar, managed bare mirrors, isolated Git worktrees, approval-gated execution, validation, and pull-request delivery. Use only when explicitly invoked for Hub repository registration, radar, status, planning, execution, delivery, or cleanup.
---

# Software Squad

Operate from the Hub workspace. Target repositories live in managed bare mirrors and task worktrees under ignored `.hub/` runtime state. Never treat Hub itself as the target unless it is separately registered.

## Select mode

Infer one mode from the explicit request. If unclear, inspect current Hub state before asking.

- **Register:** add a GitHub repository. Read [references/git-lifecycle.md](references/git-lifecycle.md) and [references/contracts.md](references/contracts.md).
- **Radar:** synchronize and inspect every enabled repository, update dashboard, and optionally prepare one read-only plan. Read [references/radar.md](references/radar.md) and [references/contracts.md](references/contracts.md).
- **Status:** summarize dashboard and work-item records without changing external state. Read [references/radar.md](references/radar.md) and [references/contracts.md](references/contracts.md).
- **Plan:** normalize chat or GitHub intake and produce a decision-complete plan. Read [references/roles.md](references/roles.md), [references/contracts.md](references/contracts.md), and relevant Git synchronization rules.
- **Run:** execute an approved plan through the deterministic manager loop. Read every reference, especially [references/orchestration.md](references/orchestration.md). After plan approval, obtain and accept the Tester's test plan before dispatching any Developer.
- **Deliver:** synchronize, rebase, validate, push, and open a pull request after explicit delivery approval. Read [references/git-lifecycle.md](references/git-lifecycle.md), [references/radar.md](references/radar.md), and [references/contracts.md](references/contracts.md).
- **Cleanup:** remove only eligible worker worktrees and branches. Read [references/git-lifecycle.md](references/git-lifecycle.md).

## Non-negotiable gates

Radar and planning are read-only with respect to target source. Radar may perform the explicitly configured Hub issue-label and Hub-owned status-comment lifecycle.

Obtain direct user approval of the exact plan before creating write-capable worktrees, changing target source, or launching implementation roles. Approval covers only named assignments, scopes, and local validation.

After all local gates pass, obtain separate direct approval before push or pull-request creation. Plan approval never authorizes push, pull request, merge, deploy, publish, target-data changes, or destructive cleanup.

Never merge a pull request automatically. Continue monitoring it through radar.

## Shared invariants

- Fetch the registered remote at every radar pass and synchronization gate.
- Bind plans and branches to full remote base SHAs.
- Give every write-capable assignment its own target-repository branch and worktree.
- Lead owns shared records and integration. Agents own only their assignment file and scoped worktree.
- Run at most three subagents concurrently. Parallelize only dependency-free, non-overlapping scopes.
- Treat the accepted Tester test plan as a hard implementation prerequisite. Every Developer assignment depends on it and names the scenarios it owns.
- Developers own white-box unit and component tests near their code. Tester/SDET owns black-box acceptance, API, contract, end-to-end, and cross-component automation through a separate write-capable assignment and worktree.
- Before any source-writing assignment, require an accepted readiness bundle: requirement specification, technical solution, and test plan. Record exact artifact revisions in every implementation assignment.
- Accept one repair or reassignment attempt. A repeated failure becomes `BLOCKED` with evidence.
- Preserve user changes. Never reset, overwrite, clean, delete, or repurpose an existing checkout.
- Persist normal prose and exact technical evidence. Keep live progress compact.

## Terminal reporting

Return current work-item state, accepted assignments, exact validations, remote/base/branch SHAs, linked issue or pull request, remaining risks, and next authorized action. Never report completion while approved work is active, ready, or awaiting required final validation.
