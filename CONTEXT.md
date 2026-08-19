# XQ Delivery Hub

The XQ Delivery Hub coordinates work across XQ-org repositories. It records
delivery intent, stage evidence, and the approval trail; target repositories
remain the source of truth for their code.

## Language

**Outcome issue**:
The parent `xq-hub` GitHub issue that represents one approved outcome across
one or more repositories. Opened by a human at the idea stage; closed only
after smoke tests and monitoring pass.
_Avoid_: Delivery item, epic, task

**Stage issue**:
A child issue of an outcome issue representing one stage of the agentic
delivery workflow (idea, research, poc, design, build, test, or release).
Closed by a required stage artifact committed to the hub; a human approves
the idea stage and the release stage.
_Avoid_: Work item, subtask, ticket

**Stage artifact**:
A file committed to the hub that closes a stage issue: contains outputs,
evidence, dependencies, and any residual risk for that stage. The next stage
issue must reference it before starting.
_Avoid_: Handoff, update, summary

**Release package**:
An immutable file committed under `docs/releases/` by the release stage.
Records rollout and rollback asset hashes, before/after test evidence, and
approval links. Excludes secrets.
_Avoid_: Build, deploy bundle

**Release ID**:
The unique identifier shared by an outcome issue, release package, approvals,
test evidence, rollout, and rollback.
_Avoid_: Version, tag

**Complete**:
The outcome state reached only after production smoke tests and the defined
monitoring result both pass.
_Avoid_: Deployed, merged

**Agentic delivery workflow**:
The hub operating model: named specialist subagents, each linked to a skill,
coordinating via hub GitHub issues as persistent context. Stages are gated:
idea and research are sequential; poc is optional under research; design closes
before build and test may overlap; release requires a human approval. Peers
may discover together; only an open stage issue authorizes writes and stage
artifacts.
_Avoid_: Department of Tech, agentic delivery framework, framework

**Delivery role**:
A named specialist subagent in the agentic delivery workflow with a linked
skill and declared stage eligibility. Platform specialists (Java, iOS, test)
own build and test stages; coordination roles (product owner, solution
architect) own idea, design, and release stages.
_Avoid_: Department role, general technical agent

**Pilot workflow**:
The first narrow, end-to-end run of the agentic delivery workflow used to
validate the operating model before wider use.
_Avoid_: Department rollout
