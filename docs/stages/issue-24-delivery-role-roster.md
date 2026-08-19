# Stage artifact: Define delivery role roster and stage eligibility
<!-- wayfinder ticket: #24 | map: #21 -->

## Outputs

### Roster

All seven existing roles carry over. No drops or merges.

| Delivery role | TOML | Stage eligibility | Notes |
|---|---|---|---|
| product-owner | `xq-product-owner` | idea, release | Human approves both; role coordinates and records |
| solution-architect | `xq-solution-architect` | research, design | Posts design handoff before build may start |
| java-backend | `xq-java-backend-engineer` | build | Target: Java/JVM repositories |
| ios | `xq-ios-engineer` | build | Target: iOS/Swift repositories |
| sdet | `xq-sdet` | build | Platform test architecture; cross-suite reliability |
| test-engineer | `xq-test-engineer` | test | Targeted regression and smoke evidence |
| devops | `xq-devops-engineer` | release, + CI/CD and infra setup at any stage when assigned | Owns release execution, release package, CI/CD pipelines, and infrastructure setup |

### Stage eligibility rules

- **idea**: product-owner creates the outcome issue and the idea stage issue; human approves before research may start.
- **research**: solution-architect owns; may flag poc need to human.
- **poc**: optional; human-triggered; any role assigned by the human.
- **design**: solution-architect owns; closes before build/test may overlap.
- **build**: java-backend, ios, or sdet — one per independent repository scope; may run in parallel once design closes.
- **test**: test-engineer owns; runs after build handoffs are ready.
- **release**: product-owner coordinates; devops executes rollout and writes the release package; human approves before release stage closes.

devops may also be assigned a stage issue at any earlier stage when CI/CD pipeline work or infrastructure setup is required — this is independent of the primary stage owner.

### Stage eligibility declaration

Declared in two places (kept in sync):
1. `stages` field added to each `.codex/agents/*.toml`
2. Role-to-stage table in `docs/agents/delivery-workflow.md` (written in ticket #25)

## Evidence

Decided in wayfinder ticket #24 via grilling.

## Next-stage input

Ticket #25 (rewrite workflow doc) is now unblocked. It must include:
- The role-to-stage table above
- Gate rules: idea/research sequential; poc optional/human-triggered; design closes before build/test overlap; release requires human approval
- devops cross-stage CI/CD assignment rule
- `stages` field format for TOML files
