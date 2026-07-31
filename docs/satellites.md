# Satellite catalogue

Hub index of Satellites. Ticket labels mirror this table. Do not copy product glossaries here — each Satellite owns its `CONTEXT.md`.

| Name | Repo | Purpose | Ticket label |
| --- | --- | --- | --- |
| xq-hub | https://github.com/ExperienceQuality/xq-hub | Agent Context Hub (docs, Specs, Tickets) | `satellite:xq-hub` |
| xq-fitness-app | https://github.com/ExperienceQuality/xq-fitness-app | Fitness product application | `satellite:xq-fitness-app` |
| xq-financial-app | https://github.com/ExperienceQuality/xq-financial-app | Financial product application | `satellite:xq-financial-app` |
| xq-versastacks | https://github.com/ExperienceQuality/xq-versastacks | Versastacks system | `satellite:xq-versastacks` |
| xq-context-hub | https://github.com/ExperienceQuality/xq-context-hub | Legacy / prior context hub (clarify or retire) | `satellite:xq-context-hub` |

Add a row when a Satellite joins the org. Create the matching GitHub label on this Hub repo before filing Tickets against it. Tighten purpose lines when each Satellite’s own `CONTEXT.md` exists.

## Local layout (on the machine running agents)

Satellite application code is **not** committed into the Hub. Local materialization lives under `.satellites/` (gitignored):

```
.satellites/
├── mirrors/
│   └── <name>.git/          # bare clone — fetch / pull only
└── work/
    └── <name>/
        └── <ticket>-<slug>/ # git worktree checkout for one Ticket
```

| Place | Role |
| --- | --- |
| `.satellites/mirrors/<name>.git` | **Pull place** — bare repo; `git fetch` only. Shared cache for all agents on this machine. |
| `.satellites/work/<name>/<ticket>-<slug>` | **Checkout place** — one worktree per Ticket so parallel agents never share a working tree. |

Hub-docs Tickets (`satellite:xq-hub`) use **this** clone — no mirror/worktree under `.satellites/`.

### Bootstrap a Satellite mirror

```bash
NAME=xq-financial-app   # catalogue Name
URL=git@github.com:ExperienceQuality/${NAME}.git

mkdir -p .satellites/mirrors .satellites/work/"$NAME"
git clone --bare "$URL" ".satellites/mirrors/${NAME}.git"
```

### Open a worktree for a Ticket (one agent)

```bash
NAME=xq-financial-app
TICKET=12
SLUG=short-description
BRANCH="ticket/${TICKET}-${SLUG}"
WT=".satellites/work/${NAME}/${TICKET}-${SLUG}"

git -C ".satellites/mirrors/${NAME}.git" fetch --prune origin
git -C ".satellites/mirrors/${NAME}.git" worktree add -b "$BRANCH" "$WT" origin/main
# If main is not the default branch, use origin/HEAD or the catalogue default.
```

Work in `"$WT"`. Open the PR from that worktree; body must link `ExperienceQuality/xq-hub#${TICKET}`.

### Remove a worktree when the Ticket is done

```bash
NAME=xq-financial-app
TICKET=12
SLUG=short-description
WT=".satellites/work/${NAME}/${TICKET}-${SLUG}"

git -C ".satellites/mirrors/${NAME}.git" worktree remove "$WT"
# optional: delete local branch on the bare repo after PR merge
```

### Refresh mirrors (before a wave)

```bash
for m in .satellites/mirrors/*.git; do
  git -C "$m" fetch --prune origin
done
```

## Parallel wave

A wave is N **ready** Hub Tickets (blockers closed, `satellite:*` set), each launched as its own agent with an **explicit pointer** — not a scan.

1. List ready Tickets on the Hub (`gh issue list` + dependency / `Blocked by` check).
2. Refresh mirrors once.
3. For each Ticket: create its worktree (above), then start one agent/session with:
   - Hub Ticket URL
   - absolute path to the worktree (or Hub root for `satellite:xq-hub`)
   - instruction to load the implement pack (ADR 0009) and open a PR linking the Ticket
4. Next wave = Tickets unblocked after the first wave’s merges.

Parent prompt template per agent:

```text
Implement Hub Ticket https://github.com/ExperienceQuality/xq-hub/issues/<N>.
Working tree: <absolute-path-to-worktree>.
Load: Ticket → linked Spec → Hub CONTEXT.md → Satellite CONTEXT.md.
Open a PR on this Satellite that links ExperienceQuality/xq-hub#<N>.
```
