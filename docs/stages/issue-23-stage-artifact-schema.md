# Stage artifact: Define stage artifact schema
<!-- wayfinder ticket: #23 | map: #21 -->

## Outputs

### Schema

Every stage artifact is a Markdown file with three required sections:

```markdown
## Outputs

<what this stage produced>

## Evidence

<validation results, links, or observations that prove the outputs are correct>

## Next-stage input

<what the next stage issue needs to reference or know before starting>
```

The release stage artifact adds one additional required section:

```markdown
## Release package

<link to the immutable release package file under docs/releases/>
```

### Location and naming

`docs/stages/<outcome-issue-number>-<stage>.md`

Examples:
- `docs/stages/42-idea.md`
- `docs/stages/42-design.md`
- `docs/stages/42-release.md`

### Relationship to release package

The stage artifact closes the stage issue and is the decision/coordination
record. The release package (`docs/releases/<release-id>.md`) is the
immutable deployment manifest (asset hashes, rollback collections). Both
exist for the release stage; the stage artifact links to the release package.

## Evidence

Decided in wayfinder ticket #23 via grilling.

## Next-stage input

Ticket #24 (delivery role roster) and #22 (issue shape) are now both
resolved. Ticket #25 (rewrite workflow doc) can start once #24 closes.
