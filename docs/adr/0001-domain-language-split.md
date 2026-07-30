# Domain language is split: Hub vs Satellite

Agents need shared vocabulary in more than one repo. We decided Hub `CONTEXT.md` holds only org-wide / cross-repo language; each Satellite owns its product glossary; the Hub keeps a Satellite catalogue (name, link, purpose) rather than copying product terms. The alternative — a Hub-only glossary for every product — is simpler early but becomes noisy and stale as Satellites grow.
