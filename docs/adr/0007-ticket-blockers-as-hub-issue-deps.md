# Ticket blockers are Hub Issue dependencies

Agents need a single ready/blocked graph for fan-out. Blockers are other Hub Tickets, expressed as native GitHub issue dependencies when available; otherwise `Blocked by: #N` in the body plus `status:blocked` / `status:ready` labels kept in sync. We rejected body links alone (drift, hard to query), project-board status alone (not a real dependency graph), and blocking on Satellite PR/issue state (couples Hub readiness to trackers we already rejected as primary).
