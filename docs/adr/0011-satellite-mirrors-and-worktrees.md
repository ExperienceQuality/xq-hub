# Satellites use local mirrors plus worktree checkouts

Agents need durable fetch state and isolated working trees for parallel Ticket waves, without committing Satellite code into the Hub. Under `.satellites/` (gitignored): **mirrors** hold bare clones used only to fetch; **work** holds `git worktree` checkouts, one per Ticket (or short-lived branch). We rejected a single clone per Satellite (parallel agents fight one working tree), cloning fresh per Ticket (slow, wastes fetch cache), and nesting Satellites as Hub submodules (couples histories and PRs).
