# Implement agents load a fixed minimum context pack

Agents implementing a Ticket must not invent what to read. The minimum pack is: Hub Ticket → linked Spec (Idea only if Spec points to open questions) → Hub `CONTEXT.md` → target Satellite `CONTEXT.md` → `docs/satellites.md` only if routing is unclear. Linked ADRs/Specs only when referenced. We rejected “read the whole Hub” each time — it burns context and mixes unrelated product language.
