# Release Packages

Each release package is an immutable Markdown manifest named
`<release-id>.md`. It must link its parent delivery item and contain rollout and
rollback asset hash collections, before/after test evidence, approval links,
production smoke results, and monitoring results. Do not include secrets.
