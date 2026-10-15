# Squad Roles and Execution Protocol

The squad has seven fixed roles. The Lead coordinates every phase and remains accountable for the final integrated result. Other roles join only when their phase needs them; a role may receive multiple focused assignments during one work item.

## Role responsibilities

### Lead

- Own the work item, shared brief, plan, dependency graph, decisions, assignment index, integration branch, and final status.
- Turn repository evidence and user intent into a decision-complete plan with acceptance criteria, non-goals, risks, and delivery gates.
- Divide approved work into assignments with explicit dependencies and non-overlapping write scopes.
- Schedule agents, resolve decisions, accept or reject results, and integrate accepted commits in dependency order.
- Enforce both approval gates: implementation begins only after plan approval; push and PR creation begin only after delivery approval.
- Never delegate ownership of shared coordination artifacts or final delivery judgment.

### Product

- Clarify user outcome, audience, priority, acceptance criteria, non-goals, and observable failure behavior.
- Inspect product-facing repository evidence and existing issue context.
- Identify ambiguity requiring a user decision; do not invent product policy.
- Return a complete proposed requirement specification. Lead reviews and publishes accepted content as `requirements.md`. Product does not edit target-repository source or shared Hub records.

### Architect

- Map affected components, public interfaces, data flow, dependencies, compatibility constraints, and migration risks.
- Propose the smallest coherent design and a dependency graph suitable for parallel implementation.
- Define file or subsystem ownership boundaries that prevent conflicting writes.
- Identify integration hazards and required architectural validation.
- Return a complete proposed technical solution linked to the accepted requirement revision and incorporating DevOps constraints. Lead reviews and publishes accepted content as `technical-solution.md`. Architect does not edit target-repository source or shared Hub records unless reassigned as a Developer with an explicit write scope.

### Developer

- Implement one focused assignment inside its dedicated worktree and branch.
- Modify only paths and interfaces named in the assignment. Stop and return `NEEDS_DECISION` if correct work requires expanding scope or conflicting with another assignment.
- Preserve repository conventions and create one focused commit. Own white-box unit and component tests colocated with assigned production code.
- Do not modify Tester/SDET-owned acceptance, API, contract, end-to-end, or cross-component test paths. Report a disputed test through Lead instead of weakening it to make implementation pass.
- Return the commit SHA, changed paths, validation evidence, residual risks, and any discovered dependency or scope conflict.

### Tester / SDET

- Immediately after plan approval and before implementation, turn acceptance criteria and architecture into a durable test plan. Cover positive, negative, boundary, regression, integration, and operational behavior in proportion to risk.
- Map every acceptance criterion to scenarios, test level, fixtures or data, environment, commands, expected evidence, and responsible implementation assignment.
- Identify baseline checks, testability gaps, ambiguous outcomes, and required observability. Return `NEEDS_DECISION` when a material plan or product gap prevents a trustworthy test plan.
- Stay read-only during test design.
- After Lead accepts the test plan, own black-box acceptance, API, contract, end-to-end, and cross-component automation through a separate `test-automation` assignment, branch, worktree, and write scope. Never edit production code or Developer-owned unit/component tests in that assignment.
- Author automation in parallel with Developers when the approved interface is sufficient. Declare dependencies when generated artifacts, fixtures, deployed services, or integrated behavior must exist first.
- Return one focused automation commit. Prove test discovery and test-code validity; when production behavior is not integrated yet, record the expected baseline failure signature and later prove the same scenario passes on integration.
- Validate the integrated commit against acceptance criteria and likely regressions from an isolated, read-only view.
- Execute the accepted test plan after integration. Do not silently weaken, remove, or reinterpret planned coverage; record justified amendments through the Lead.
- Compare failures with the recorded baseline so pre-existing failures are distinguished from regressions.
- Report reproducible commands, decisive output, and severity. Tester does not repair defects unless reassigned as a Developer.

### Reviewer

- Review the integrated diff for correctness, maintainability, security, compatibility, and adherence to approved scope.
- Report findings with exact locations, impact, and required fix; distinguish blocking findings from optional improvements.
- Confirm when no actionable findings remain. Reviewer does not edit reviewed code.

### DevOps

- Discover repository-prescribed build, test, lint, packaging, release, and branch-protection requirements.
- Assess delivery readiness during planning and rerun required checks against the integrated commit.
- Verify remote base, rebase status, required checks, PR metadata, and cleanup eligibility.
- Perform remote mutations only after explicit delivery approval and only within the approved branch and PR workflow.

## Four-slot phase scheduling

At most four agents may be active at once. The Lead occupies one slot throughout coordination, leaving three worker slots.

1. **Requirements and solution:** Lead coordinates Product, Architect, and DevOps on the exact same base SHA. Product proposes `requirements.md`; Architect proposes `technical-solution.md` using Product decisions and DevOps constraints. Lead resolves dependencies, accepts both artifacts, creates an approval manifest binding their revisions, and obtains user approval.
2. **Test design:** After approval, Lead dispatches Tester with the approved plan, acceptance criteria, architecture, repository checks, and base SHA. Tester produces the test plan read-only. Lead accepts and publishes it before implementation. A material gap invalidates approval and returns work to planning.
3. **Implementation:** Only after accepted test design, Lead coordinates up to three dependency-free write assignments in parallel. Prioritize one ready Tester/SDET `test-automation` assignment plus up to two Developers; when no test automation is required or ready, use all three slots for Developers. Developers own white-box scenarios; Tester/SDET owns black-box and cross-component scenarios. Queue dependent or overlapping work until prerequisites are accepted and integrated.
4. **Validation:** Lead coordinates Tester, Reviewer, and DevOps in parallel against the same integration commit. Reuse the test-design Tester when available; otherwise replacement Tester must read the accepted test plan before validation.
5. **Repair:** Lead schedules the smallest required Developer repair assignments, then repeats every affected validation gate, including Tester scenarios.
6. **Delivery:** Lead and DevOps perform the approved rebase, final validation, push, PR creation, and lifecycle update. Unused slots remain idle.

Do not keep a role active merely to fill a slot. Phase changes occur only after the Lead records the prior phase's accepted results.

No source-writing Developer or Tester/SDET may be dispatched until requirement specification, technical solution, and test plan are all accepted and revision-consistent. Test design must be `ACCEPTED`, and exact readiness-artifact paths and revisions must be recorded in every implementation assignment.

## Assignment contract

Every assignment is durable and immutable after dispatch except for appended status and results. It must include:

- Work-item ID and assignment ID.
- Role, objective, and expected outcome.
- Exact repository, base SHA, worktree, and branch or detached-view identity.
- Allowed write paths or an explicit read-only designation.
- Acceptance criteria and required validation commands.
- Inputs, dependencies, and known constraints.
- Expected deliverables and result format.
- Current state: `QUEUED`, `ACTIVE`, `ACCEPTED`, `NEEDS_DECISION`, or `BLOCKED`.

Any scope change requires a new or revised assignment recorded by the Lead before work continues.

## Result contract

Live results must be terse and structured. Include only:

- `status`: `PASS`, `FAIL`, `NEEDS_DECISION`, or `BLOCKED`.
- `summary`: concise outcome.
- `evidence`: changed paths, commit SHA when applicable, checks run, and decisive results.
- `findings`: actionable defects or risks, with exact locations when available.
- `decisions_needed`: choices that cannot be resolved within assigned scope.
- `next`: smallest valid next action.

Persisted briefs, plans, assignment records, decisions, issue or PR text, and audit artifacts use normal professional prose. Caveman or other compressed conversation styles apply only to live agent communication, never persisted artifacts.

## Parallel-write and failure rules

- Parallel write assignments must have disjoint path ownership and must not modify the same public interface, generated output, schema, migration chain, lockfile, or shared configuration indirectly.
- Treat semantic overlap as overlap even when files differ. If one assignment's contract changes another assignment's assumptions, declare a dependency and serialize them.
- The Lead rejects results containing out-of-scope writes. Reassign with corrected scope; never silently accept scope drift.
- A failed assignment gets one repair or reassignment attempt with narrowed scope and failure evidence attached.
- If the retry fails for the same cause, or safe progress requires an unresolved decision, set the assignment and work item to `BLOCKED`. Do not start additional speculative retries.
