# Tickets name their Satellite via Hub Issue labels

Agents grabbing ready work need a filterable routing key for “which repo does this Ticket hit?” Each Ticket Issue on the Hub carries one `satellite:<name>` label (including `satellite:xq-hub` for Hub-docs work). We rejected target-only-in-body (not queryable), one project-board column per Satellite (weaker for `gh`-driven agents), and auto-splitting every Spec slice into multiple Issues (over-creates when the slice is already one Satellite).
