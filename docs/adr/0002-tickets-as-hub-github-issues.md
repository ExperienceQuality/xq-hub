# Tickets are GitHub Issues on the Hub

Cross-repo agent work needs one place to see readiness and blockers. We track each Ticket as a GitHub Issue on the Hub (`xq-hub`). Implementation happens on the target Satellite; PRs (and any Satellite-local discussion) link back to the Hub issue. We rejected Satellite-only issues (fragments the Spec’s ready/blocked view), Hub-only `.scratch` markdown (weak org visibility for `gh`-driven agents), and dual issues on Hub + Satellite (sync tax).
