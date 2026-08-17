# XQ Delivery Hub

The XQ Delivery Hub coordinates work across XQ-org repositories. It records the
delivery intent, ownership, release evidence, and approval trail; target
repositories remain the source of truth for their code.

## Language

**Delivery item**:
The parent `xq-hub` GitHub issue that represents one approved outcome across one
or more repositories.
_Avoid_: Epic, task

**Work item**:
A child issue with one independent write scope, target repository, reviewer, and
named delegated subagent.
_Avoid_: Subtask, ticket

**Handoff**:
The durable completion record posted to a work item's hub issue, containing
changed commit hashes, validation evidence, dependencies, and residual risk.
_Avoid_: Update, summary

**Release package**:
An immutable DevOps-owned manifest that connects a release ID to the included
asset hashes, rollout evidence, and rollback hashes.
_Avoid_: Build, deploy bundle

**Release ID**:
The unique identifier shared by a delivery item, release package, approvals,
test evidence, rollout, and rollback.
_Avoid_: Version, tag

**Complete**:
The delivery state reached only after production smoke tests and the defined
monitoring result both pass.
_Avoid_: Deployed, merged
