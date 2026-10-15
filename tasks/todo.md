# Checklist Ledger Task List

Source specification: `specs/SPEC-checklist-ledger.md`
Plan: `tasks/plan.md`
Status: `PLANNING — USER APPROVED; READINESS ARTIFACTS AND TEST PLAN REQUIRED BEFORE IMPLEMENTATION`

## Phase 1: Contract foundation

- [ ] T1: Define checklist syntax, template contract, and validation rules
  - Acceptance: Exact four-state syntax, mandatory activity IDs, gate authority,
    evidence, and scope rules are documented.
  - Verify: Temporary fixtures pass/fail as expected; `git diff --check`.
  - Dependencies: None
  - Assignment: Pending approved test plan

## Phase 2: Protected role templates

- [ ] T2: Add planning-role templates for Lead, Product, and Architect
  - Acceptance: Complete protected planning lifecycle with concrete artifacts
    and no pre-approval source-writing authorization.
  - Verify: Template coverage and Markdown checks.
  - Dependencies: T1
  - Assignment: Pending approved test plan

- [ ] T3: Add execution and delivery templates for Developer, Tester/SDET,
  Reviewer, and DevOps
  - Acceptance: Ownership boundaries, validation, evidence, and delivery gates
    are explicit for all four roles.
  - Verify: Template coverage and Markdown checks.
  - Dependencies: T1
  - Assignment: Pending approved test plan

## Checkpoint: Template coverage

- [ ] All seven role templates exist and validate.
- [ ] Mandatory activities are concrete and protected.

## Phase 3: Assignment ledger behavior

- [ ] T4: Implement assignment checklist expansion and validation
  - Acceptance: Required activities are present; malformed, vague, missing-
    evidence, and out-of-scope assignments are rejected.
  - Verify: Unit/contract tests and temporary-repository fixtures.
  - Dependencies: T2, T3
  - Assignment: Pending approved test plan

- [ ] T5: Integrate Lead enforcement, current-state updates, and logical restart
  - Acceptance: Lead gates dispatch/handoff/integration/delivery; material
    changes invalidate approvals; restart preserves abandoned worktrees.
  - Verify: Temporary orchestration scenarios and docs validation.
  - Dependencies: T4
  - Assignment: Pending approved test plan

## Checkpoint: Orchestration behavior

- [ ] Sample assignment shows detailed current-state activity progress.
- [ ] User-gated activity blocks without a recorded decision.
- [ ] Out-of-scope worker result is rejected.
- [ ] Abandoned worker can be logically replaced without deleting evidence.

## Phase 4: Verification and handoff

- [ ] T6: Add temporary-repository tests, validation commands, and documentation
  updates
  - Acceptance: All module success criteria have repeatable evidence and no live
    repository mutation occurs.
  - Verify: Tester matrix, `quick_validate.py`, and `git diff --check`.
  - Dependencies: T5
  - Assignment: Tester/SDET validation with Lead acceptance

## Checkpoint: Complete

- [ ] All `checklist-ledger` success criteria pass.
- [ ] Integrated result reviewed.
- [ ] Separate delivery approval requested before any push or PR.
