# XQ Agent Context Hub

Shared understanding and work packaging for agents across the ExperienceQuality organisation. Application code lives elsewhere.

## Language

**Hub**:
The GitHub repository whose product is shared understanding and work packaging for agents — glossary, ADRs, specs, tickets, and handoffs — not shipped application software.
_Avoid_: Monorepo, orchestration runtime, control plane, platform

**Satellite**:
A GitHub repository in the organisation that owns and ships application code for a product or system. Agents implement there; they plan and package work in the Hub. Each Satellite owns its product domain language.
_Avoid_: Downstream repo, child repo, microservice (when you mean any non-Hub repo)

**Satellite catalogue**:
A Hub-owned index of Satellites — name, link, and one-line purpose — without copying a Satellite’s product glossary.
_Avoid_: Registry, inventory, monorepo map

## Work packaging

**Idea**:
Sharpened intent that is not yet a buildable plan — the output of grilling, research, or exploration.
_Avoid_: Initiative, epic, proposal (when you mean unshaped intent)

**Spec**:
A buildable plan collapsed from an Idea, still Hub-owned before it is sliced into Tickets.
_Avoid_: Design doc, PRD, roadmap item

**Ticket**:
A tracer-bullet slice of a Spec, aimed at one Satellite (or at Hub docs), with explicit blockers. Tracked as a GitHub Issue on the Hub; Satellite work links back to that issue.
_Avoid_: Task, story, work item, issue (when speaking in Hub language — prefer Ticket)
