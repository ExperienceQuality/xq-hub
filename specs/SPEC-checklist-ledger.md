# Spec: Checklist Ledger

Status: `APPROVED`

Module ID: `checklist-ledger`

Initiative: Checklist-Governed Software Squad

## Objective

Give every Lead and worker assignment a clear, inspectable checklist of real
activities. The checklist is the current operating contract for the assignment:
it shows what remains, who owns it, what depends on it, and whether the Lead
can allow the assignment to advance.

The ledger must stay deliberately simple. It stores the latest plan and latest
checklist state, not a general event-sourcing system. A worker result may be
logically abandoned and restarted from the latest accepted integration state.
The abandoned worktree and branch remain preserved until the existing Hub
cleanup rules make them eligible for removal.

### Users and outcomes

- The Lead reads and enforces every checklist.
- Workers read and update their own progress and evidence.
- The user inspects progress through the Lead and approves only material gates.
- A person reviewing `.hub/` can understand the current state without replaying
  chat history.

## Functional requirements

### CL-1: Protected role templates

Provide one reusable template for each active role: Lead, Product, Architect,
Developer, Tester/SDET, Reviewer, and DevOps. Templates live with the
`software-squad` skill under `references/checklists/`.

Each template defines mandatory lifecycle activities. The Lead may add, reorder,
or clarify issue-specific activities, but may not silently remove mandatory
activities or weaken their completion conditions.

### CL-2: Issue-specific expansion

Before dispatch, the Lead expands the selected role template into a concrete
assignment checklist. Activities must name the actual issue or work item,
assignment scope, relevant files or artifacts, dependencies, commands where
known, and the evidence that makes the activity complete.

An activity must describe observable work, for example:

```markdown
- [ ] Inspect `src/persistence/**` and record the affected interfaces.
- [ ] Implement only the assigned persistence paths.
- [ ] Run `npm test -- persistence` and record the exit status.
- [ ] Confirm no Tester-owned files changed.
```

### CL-3: Small current-state vocabulary

Checklist items use only these visible states:

- `[ ]` TODO
- `[-]` DOING
- `[!]` BLOCKED, with a short reason
- `[x]` DONE

The ledger does not require separate `SUBMITTED`, `ACCEPTED`, `REJECTED`, or
`STALLED` item states. Assignment acceptance and agent health remain Lead and
supervision concerns. A worker may report evidence; only the Lead may treat a
gated activity as complete.

### CL-4: Durable assignment record

Copy the expanded checklist into the assignment record under:

`.hub/work-items/<repo-id>/<work-item-id>/assignments/<assignment-id>.md`

The assignment record also identifies role, objective, dependencies, source
SHA, branch or detached view, absolute worktree path, owned write paths, retry
count, and current assignment state. The checklist is the authoritative latest
view for activities; the surrounding Hub contract remains authoritative for
provenance and lifecycle state.

### CL-5: Evidence and completion

Each activity may include a short evidence note or link. Evidence can be a
command and result, changed path, commit SHA, artifact revision, review note,
or decision reference. Evidence is concise and human-readable; it is not a
second state machine.

The Lead may mark an activity `[x]` when its completion condition and evidence
are satisfied. Automated checks may supply evidence, but they do not replace
Lead judgment for scope, quality, or acceptance.

### CL-6: Lead and user gates

Routine Lead activities may be completed by the Lead with evidence. Activities
declared as user gates cannot be marked complete by the Lead; they require a
durable user decision recorded in the existing decisions record. Examples are
implementation approval, material scope change, cancellation, push approval,
and pull-request approval.

### CL-7: Latest-plan updates

The Lead may update the current plan and checklist in response to a user
follow-up, finding, dependency change, or recovery decision. The latest version
must clearly show the resulting activities and current status. A material
change to acceptance criteria, write scope, dependency order, readiness
artifacts, or delivery behavior invalidates affected approval gates and must
return to the existing approval workflow.

### CL-8: Restart and abandonment

When a worker is stalled, out of scope, or produces an unusable result, the
Lead may mark the assignment result abandoned, reset the affected current
activities to TODO in a replacement assignment, and dispatch a fresh worker
from the latest accepted integration state. The original result, branch, and
worktree are retained as inactive evidence until existing cleanup rules permit
removal. No user changes may be discarded automatically.

### CL-9: Template protection and scope checks

Validation must detect a missing mandatory template activity, an assignment
checklist with no concrete issue-specific detail, a completed item without
evidence where evidence is required, and a worker result that claims completion
outside its owned scope. Such a result is not accepted by the Lead.

## Non-functional requirements

- **Readable:** A person can understand an assignment's current work by
  reading one Markdown file.
- **Deterministic:** The same role template and assignment inputs produce the
  same mandatory activity set.
- **Recoverable:** Restarting a worker never requires deleting or rewriting its
  prior worktree.
- **Compatible:** Existing `.hub/` contracts, approval states, three-worker
  concurrency limit, and cleanup rules remain valid.
- **Local:** No database, remote service, or web UI is required for this module.

## Tech stack and storage

- Markdown templates and assignment records.
- Existing `software-squad` skill references and Hub `.hub/` records.
- Small validation helpers may use Python or shell, but the source of truth is
  the Markdown record rather than generated JSON.

## Commands

The implementation must document and support exact validation commands. The
initial contract is:

```sh
python3 /Users/davidchau/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/software-squad
git diff --check
```

Module-specific validation must also provide a command that checks role-template
coverage, checklist syntax, mandatory activity presence, and representative
assignment expansion against temporary Hub records. The exact helper path and
command are an open question for the plan phase.

## Project structure

```text
.agents/skills/software-squad/
├── SKILL.md
├── references/
│   └── checklists/
│       ├── lead.md
│       ├── product.md
│       ├── architect.md
│       ├── developer.md
│       ├── tester.md
│       ├── reviewer.md
│       └── devops.md
└── ...existing references...

.hub/work-items/<repo-id>/<work-item-id>/assignments/
└── <assignment-id>.md

specs/
├── capability-map.md
└── SPEC-checklist-ledger.md
```

## Code and record style

Use plain, direct activity language. Every line begins with one verb and names
the concrete object or evidence involved. Prefer paths, artifact IDs, commands,
and states over vague phrases such as “handle the implementation.”

Good:

```markdown
- [ ] Run `npm test -- persistence` and record the exit status in this assignment.
```

Avoid:

```markdown
- [ ] Make sure persistence works.
```

Templates use stable role headings and keep mandatory activities in lifecycle
order. Assignment-specific additions follow the template activities under an
`## Issue-specific activities` heading.

## Testing strategy

Validation is read-only and must use temporary repositories or temporary Hub
records for risky orchestration behavior. It must cover:

1. Every active role has a template.
2. Mandatory template activities cannot be omitted by expansion.
3. Issue-specific activities contain concrete scope or evidence expectations.
4. The four visible item states render and round-trip correctly.
5. User-gated items cannot be completed without a recorded user decision.
6. A worker result outside its write scope is rejected.
7. A logically abandoned assignment can be replaced while its original
   worktree and evidence remain intact.
8. A material plan change invalidates the affected approval gate.
9. Existing `quick_validate.py` and `git diff --check` pass.

No live GitHub repository, remote branch, or target source may be mutated by
these tests.

## Boundaries

### Always

- Keep role templates protected and version-controlled.
- Keep assignment checklists detailed enough to identify the actual activity.
- Record evidence for completed gated activities.
- Let the Lead own acceptance and user-gate decisions.
- Preserve abandoned worker worktrees and branches until Hub cleanup permits
  removal.

### Ask first

- Change a mandatory role activity.
- Add a new role or checklist state.
- Change what counts as a user gate.
- Change assignment ownership, readiness requirements, or delivery approval.

### Never

- Require a database or event-sourcing layer for this module.
- Let a worker mark its own gated result accepted.
- Hide issue-specific work behind generic template language.
- Delete abandoned worker evidence or user changes automatically.
- Treat an unchecked or blocked required activity as complete.

## Success criteria

The module is complete when:

1. Seven protected role templates exist and validate.
2. A Lead can expand each template into a detailed assignment checklist.
3. A reviewer can understand current progress from the assignment Markdown file
   alone.
4. Checklist state remains limited to TODO, DOING, BLOCKED, and DONE.
5. User-gated activities cannot be closed without a recorded decision.
6. Lead follow-ups can update the latest plan and checklist without losing the
   abandoned worker's physical worktree or evidence.
7. Temporary-repository tests prove scope rejection and safe logical restart.
8. Existing Hub approval, worktree, concurrency, and cleanup invariants remain
   intact.

## Open questions

1. Should the visible state use Markdown markers exactly as `[ ]`, `[-]`, `[!]`,
   and `[x]`, or should the existing assignment `State:` field be the only
   machine-readable state while checkboxes remain presentation?
2. What initial timeout or heartbeat policy should `agent-supervision` use to
   label an assignment stalled? This module only records the Lead's resulting
   current checklist state; supervision owns detection.
3. Should evidence be inline after each item, or collected in a short evidence
   section keyed by checklist line?

## Approval request

Please review the objective, simple checklist model, protected templates,
Lead-owned enforcement, restart behavior, success criteria, and open questions.
Approve this specification or request changes before we move to the planning
phase for `checklist-ledger`.
