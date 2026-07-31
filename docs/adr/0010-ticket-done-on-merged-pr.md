# A Ticket is done when its linked PR merges

Fan-out agents need a clear finish line distinct from “ready.” A Ticket closes when a PR is merged on the target Satellite (or on the Hub for `satellite:xq-hub`), that PR links the Hub Ticket, and Ticket acceptance is met. The Hub Issue remains source of truth; the PR is evidence. We rejected closing when a PR merely opens (review can still fail) and requiring a human checkbox on every Ticket (optional at Spec level; too slow as a default agent gate).
