# Implementation Plan: Checklist Ledger

## Overview

Implement the `checklist-ledger` module from
[`specs/SPEC-checklist-ledger.md`](../specs/SPEC-checklist-ledger.md). The
module adds protected role templates, detailed issue-specific assignment
checklists, simple current-state markers, Lead-owned enforcement, and safe
logical worker restart semantics to the existing `software-squad` skill.

This plan covers the first module only. It does not implement the downstream
`lead-control-channel`, `gate-enforcement`, `agent-supervision`,
`isolated-execution`, `progress-dashboard`, or `delivery-lifecycle` modules.

## Architecture decisions

- Markdown remains the durable source of truth; no database or event log.
- Role templates live under `.agents/skills/software-squad/references/checklists/`.
- Expanded checklists live inside existing assignment records under `.hub/`.
- Visible checklist states remain `TODO`, `DOING`, `BLOCKED`, and `DONE`.
- The Lead owns acceptance and user gates.
- Logical restart creates a fresh assignment from the latest accepted
  integration state; the abandoned worker worktree remains preserved.
- Validation uses temporary repositories or Hub records and must not mutate live
  GitHub repositories.

## Dependency graph

```text
T1 checklist contract
├── T2 planning-role templates
└── T3 execution-role templates
        ↓
T4 assignment expansion and validation
        ↓
T5 orchestration and restart integration
        ↓
T6 temporary-repository verification and documentation
```

T2 and T3 may run in parallel after T1. T4 waits for both template groups. T5
waits for T4. T6 can prepare test fixtures after T1, but final verification
waits for T5.

## Sub-agent dispatch model

The lead remains the coordinator and owns shared records. After this plan is
approved:

1. Dispatch one read-only Tester/SDET assignment to turn the spec and this plan
   into an accepted test plan.
2. After the test plan is accepted, dispatch up to three non-overlapping
   Developers for T1/T2/T3 as capacity and dependencies allow.
3. Dispatch T4 and T5 as dependency-ordered Developer assignments. A Tester
   may own black-box/contract validation but must not edit production skill
   behavior in that assignment.
4. Dispatch T6 as Tester/SDET validation plus Lead review. Do not push or open a
   PR without separate delivery approval.

No source-writing assignment is ready until the accepted `requirements.md`,
`technical-solution.md`, and `test-plan.md` readiness bundle exists for the
work item.

## Task list

### Phase 1: Contract foundation

- [ ] T1: Define checklist syntax, template contract, and validation rules.

### Phase 2: Protected role templates

- [ ] T2: Add planning-role templates for Lead, Product, and Architect.
- [ ] T3: Add execution and delivery templates for Developer, Tester/SDET,
  Reviewer, and DevOps.

### Checkpoint: Template coverage

- [ ] All seven roles have protected templates.
- [ ] Required activities are concrete and issue-specific expansion points are
  documented.
- [ ] Template checks pass in temporary records.

### Phase 3: Assignment ledger behavior

- [ ] T4: Implement assignment checklist expansion and validation.
- [ ] T5: Integrate Lead enforcement, current-state updates, and logical restart
  semantics into the orchestration contracts.

### Checkpoint: Orchestration behavior

- [ ] A sample assignment renders a detailed current-state checklist.
- [ ] A user-gated item cannot be closed without a recorded decision.
- [ ] Out-of-scope worker completion is rejected.
- [ ] An abandoned assignment can be replaced without deleting its worktree or
  evidence.

### Phase 4: Verification and handoff

- [ ] T6: Add temporary-repository tests, validation commands, and documentation
  updates.

### Checkpoint: Complete

- [ ] The checklist-ledger success criteria in the spec are met.
- [ ] `quick_validate.py` passes.
- [ ] `git diff --check` passes.
- [ ] The lead presents the integrated result for review and separate delivery
  approval.

## Task details

### T1: Define checklist syntax, template contract, and validation rules

**Owner:** Architect/Developer

**Description:** Define the minimal Markdown contract for protected templates,
expanded assignment checklists, four visible item states, evidence notes, user
gates, and mandatory activity identifiers. Document how a validator distinguishes
template activities from issue-specific activities.

**Acceptance criteria:**

- [ ] The contract defines exact markers for TODO, DOING, BLOCKED, and DONE.
- [ ] Each mandatory activity has a stable identifier and completion condition.
- [ ] The contract states which activities are Lead-self-completable and which
  require a user decision.
- [ ] The contract states how evidence and assignment scope are represented.

**Verification:** Temporary Markdown fixtures pass and fail the contract checks
  as expected; `git diff --check` passes.

**Dependencies:** None.

**Files likely touched:**

- `.agents/skills/software-squad/references/checklists/README.md`
- `.agents/skills/software-squad/references/contracts.md`

**Estimated scope:** Small.

### T2: Add planning-role templates

**Owner:** Developer

**Description:** Add protected templates for Lead, Product, and Architect with
  concrete planning activities and explicit user-gate markers where applicable.

**Acceptance criteria:**

- [ ] Each role has a complete lifecycle from intake through handoff.
- [ ] Activities name expected artifacts such as requirements, technical
  solution, plan, dependencies, and decisions.
- [ ] Templates do not authorize source-writing work before approval.

**Verification:** Template coverage test identifies all required activities and
  confirms no duplicate role IDs; Markdown lint/check passes.

**Dependencies:** T1.

**Files likely touched:**

- `.agents/skills/software-squad/references/checklists/lead.md`
- `.agents/skills/software-squad/references/checklists/product.md`
- `.agents/skills/software-squad/references/checklists/architect.md`

**Estimated scope:** Medium.

### T3: Add execution and delivery templates

**Owner:** Developer

**Description:** Add protected templates for Developer, Tester/SDET, Reviewer,
  and DevOps, separating white-box ownership, black-box ownership, validation,
  and delivery checks.

**Acceptance criteria:**

- [ ] Developer activities include scoped implementation, white-box tests,
  required checks, focused commit, and evidence handoff.
- [ ] Tester/SDET activities distinguish read-only test planning from owned
  black-box automation and integrated validation.
- [ ] Reviewer and DevOps activities remain read-only or delivery-gated as
  required by the existing skill.
- [ ] Templates forbid changes outside assigned scope.

**Verification:** Template coverage test identifies all required activities and
  ownership classes; Markdown lint/check passes.

**Dependencies:** T1.

**Files likely touched:**

- `.agents/skills/software-squad/references/checklists/developer.md`
- `.agents/skills/software-squad/references/checklists/tester.md`
- `.agents/skills/software-squad/references/checklists/reviewer.md`
- `.agents/skills/software-squad/references/checklists/devops.md`

**Estimated scope:** Medium.

### T4: Implement assignment checklist expansion and validation

**Owner:** Developer

**Description:** Add the smallest helper or documented deterministic procedure
  that combines a protected role template with issue-specific activities and
  validates an assignment record before dispatch or acceptance.

**Acceptance criteria:**

- [ ] Expansion includes every mandatory role activity.
- [ ] Expansion preserves issue-specific activity detail, dependencies, scope,
  and evidence requirements.
- [ ] Validation rejects missing mandatory activities, vague empty assignments,
  invalid markers, missing evidence for required completed items, and
  out-of-scope claimed changes.
- [ ] Validation does not modify target repositories or remote state.

**Verification:** Unit/contract tests cover valid, missing, malformed, and
out-of-scope fixtures; temporary-repository test passes.

**Dependencies:** T2, T3.

**Files likely touched:**

- `.agents/skills/software-squad/scripts/validate_checklists.py` (or the
  approved equivalent)
- `.agents/skills/software-squad/references/checklists/README.md`
- `tests/checklist_ledger/test_expansion.py` (or the approved test location)

**Estimated scope:** Medium.

### T5: Integrate Lead enforcement and logical restart semantics

**Owner:** Developer

**Description:** Update the orchestration and role contracts so the Lead
  maintains current checklists, enforces user gates, rejects worker scope drift,
  and can logically abandon and restart a worker from the latest accepted
  integration state while preserving the old worktree and evidence.

**Acceptance criteria:**

- [ ] Dispatch prompts require the expanded checklist and identify the Lead as
  acceptance authority.
- [ ] Orchestration rules require checklist completion before dispatch,
  handoff, integration, or delivery transitions.
- [ ] User follow-ups update the latest plan/checklist and invalidate affected
  approvals when material.
- [ ] Restart semantics preserve the abandoned assignment record, branch,
  worktree, and evidence while creating a clean replacement assignment.

**Verification:** Temporary orchestration scenarios cover an approval block,
scope rejection, user follow-up, stall/restart, and preserved worktree; docs
validation and `git diff --check` pass.

**Dependencies:** T4.

**Files likely touched:**

- `.agents/skills/software-squad/SKILL.md`
- `.agents/skills/software-squad/references/orchestration.md`
- `.agents/skills/software-squad/references/contracts.md`
- `.agents/skills/software-squad/references/roles.md`

**Estimated scope:** Medium.

### T6: Add verification and handoff documentation

**Owner:** Tester/SDET, with Lead review

**Description:** Exercise the complete checklist-ledger contract in temporary
  repositories, record exact validation commands, and update the module spec or
  skill documentation with resolved open questions.

**Acceptance criteria:**

- [ ] Every success criterion in the module spec has a repeatable check.
- [ ] Risky orchestration tests use temporary repositories only.
- [ ] Validation proves no live GitHub or target source mutation.
- [ ] `quick_validate.py` and `git diff --check` pass.

**Verification:** Tester returns command output, fixture locations, and a
  pass/fail matrix tied to T1-T5; Lead reviews and accepts the result.

**Dependencies:** T5.

**Files likely touched:**

- `tests/checklist_ledger/test_orchestration.py`
- `tests/checklist_ledger/fixtures/`
- `specs/SPEC-checklist-ledger.md`
- `.agents/skills/software-squad/references/checklists/README.md`

**Estimated scope:** Medium.

## Parallel work

Safe after T1: T2 and T3, because they own disjoint template files. Do not
parallelize T4 with T2/T3, or T5 with T4. T6 may prepare fixtures early but its
acceptance depends on the integrated T5 behavior.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Checklist detail becomes vague role prose | High | Require concrete paths, commands, artifacts, and evidence in T2/T3 acceptance. |
| New validation helper duplicates existing contracts | Medium | T1 maps current assignment contracts before T4 adds helpers. |
| Lead enforcement remains prompt-only | High | T4/T5 reject missing checklist/evidence and test out-of-scope results. |
| Restart deletes or obscures useful work | High | Preserve old assignment, branch, worktree, and evidence; only logically abandon it. |
| Existing orchestration gates regress | High | T6 reruns existing quick validation and temporary forward tests. |
| Scope expands into downstream modules | Medium | Keep T5 changes limited to checklist data and existing orchestration contracts. |

## Open questions carried into planning

- Exact checklist parser/validator location and runtime language.
- Whether checkbox markers or an assignment header is the machine-readable state.
- Inline evidence versus a keyed evidence section.
- The stall timeout policy, owned by `agent-supervision` rather than this module.

## Approval gate

This plan is ready for review. Approval authorizes planning of the assignments,
not worktree creation, source-writing agents, branch pushes, or pull-request
creation. After approval, the Lead must dispatch and accept the read-only Tester
test plan before dispatching T2-T5 Developers.
