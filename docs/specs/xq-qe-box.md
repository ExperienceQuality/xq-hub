# Spec: xq-qe-box

**Status:** Active — Satellite bootstrap.

**Related:** [`docs/ideas/xq-qe-box.md`](../ideas/xq-qe-box.md), [`docs/ideas/agent-device-cli.md`](../ideas/agent-device-cli.md)

## Problem

Agents across XQ Satellites need a single, org-owned place to get:

1. An agent-native device CLI (upstream `agent-device`)
2. Skills published to the skills registry (`npx skills add …`)
3. A reliable bash install path for humans and harnesses

Without a Satellite, install steps and skills drift per app repo.

## Solution

Create Satellite **`xq-qe-box`** (`ExperienceQuality/xq-qe-box`) as a **monorepo** that owns skills, install scripts, and future CLI/packages. Do not ship a second mobile-control runtime or an `xq-qe` wrapper — install and route to `agent-device`.

### Monorepo layout

```
xq-qe-box/
├── CONTEXT.md
├── README.md
├── skills/
│   └── xq-mobile-auto-test/
│       ├── SKILL.md                 # Agent Skills format
│       └── scripts/install-cli.sh   # pinned agent-device install (part of skill)
├── packages/                 # reserved
└── cli/                      # reserved
```

### Install contract

`skills/xq-mobile-auto-test/scripts/install-cli.sh` installs pinned `agent-device` globally (exact version; no silent `@latest` for agents) and prints next steps (skill install + `agent-device help workflow`).

### Skills (`gh skill` / registry)

Layout must satisfy `gh skill publish` discovery (`skills/*/SKILL.md`; `name` matches directory; required frontmatter). Optional `scripts/` beside `SKILL.md` is valid per the Agent Skills spec. Validate with `gh skill publish --dry-run` before a real publish.

```bash
gh skill preview ExperienceQuality/xq-qe-box xq-mobile-auto-test
gh skill install ExperienceQuality/xq-qe-box xq-mobile-auto-test
# or: npx skills add ExperienceQuality/xq-qe-box --skill xq-mobile-auto-test
```

Day-one skill: **`xq-mobile-auto-test`** — thin router to bundled install script + version-matched `agent-device help`.

### Hub bookkeeping

- Catalogue row + `satellite:xq-qe-box` label on this Hub.
- Tickets for later work (more skills, future CLI package, CI) target that label.

## Out of scope

- An `xq-qe` wrapper package/binary
- Replacing or forking `agent-device` internals
- MCP / Node client packaging (CLI agent-native focus)
- Per-app CI matrices (later Tickets)

## Acceptance (bootstrap)

- [x] Repo `ExperienceQuality/xq-qe-box` exists (public)
- [x] Layout with `skills/xq-mobile-auto-test/SKILL.md` + `skills/xq-mobile-auto-test/scripts/install-cli.sh`; `gh skill publish --dry-run` clean
- [ ] `agent-device --version` works after install on a target machine
- [x] Skill listable via `npx skills add ExperienceQuality/xq-qe-box --list`
- [x] Hub catalogue + `satellite:xq-qe-box` label exist

## Tracer-bullet Tickets (suggested)

1. Bootstrap repo + install script + `xq-mobile-auto-test` skill (this Spec)
2. Pin strategy / release notes for `agent-device` upgrades
3. Wire first product Satellite with skill + install docs
4. Optional: first-party CLI under `cli/` or `packages/` when Spec’d
