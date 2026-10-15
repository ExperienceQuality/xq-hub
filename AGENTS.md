# Hub v2 workspace

Hub v2 is a repository-scoped Codex control plane for coordinating work in registered GitHub repositories. The `software-squad` skill owns repository registration, active radar, planning, parallel execution, validation, and pull-request delivery.

## Runtime boundaries

- Treat Hub as control-plane state, never as a target repository checkout.
- Keep managed bare mirrors, worktrees, locks, dashboards, and work-item records under `.hub/`; this directory is local runtime state and must stay ignored by Git.
- Never edit target source outside a worktree created from its registered bare mirror.
- Radar may fetch, inspect, update Hub-owned GitHub issue labels/comments, and prepare read-only plans. It may not start implementation.
- Run scheduled radar as one hourly heartbeat attached to its owning Hub chat. Reuse that chat across runs; do not create one standalone chat per scan.
- Require direct user approval for a plan before creating write-capable worktrees or launching implementation agents.
- After plan approval, require a read-only Tester assignment and accepted durable test plan before dispatching any Developer.
- Require three accepted, mutually consistent readiness artifacts before any source-writing agent starts: `requirements.md`, `technical-solution.md`, and `test-plan.md`.
- Developers own white-box tests in their source scopes. Tester/SDET owns black-box and cross-component automation in a separate branch, worktree, and assignment.
- Require a second direct user approval before pushing a branch or opening a pull request.
- Never merge a pull request, deploy, publish, delete target data, or discard user changes automatically.
- Use at most three subagents concurrently because the lead occupies the fourth active slot.
- Preserve worker worktrees and branches until radar confirms the pull request merged. Then remove workers only; retain integration state and audit records.

## Verification

- Validate the skill with the bundled `quick_validate.py` tool.
- Run `git diff --check` when this workspace becomes a Git repository.
- Forward-test risky orchestration behavior in temporary repositories. Do not mutate live GitHub repositories for tests.
