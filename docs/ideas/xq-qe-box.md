# Idea: xq-qe-box

**Status:** Collapsed to Spec — see [`docs/specs/xq-qe-box.md`](../specs/xq-qe-box.md).

Satellite monorepo for XQ agent-native QE: skills (GitHub / skills.sh registry), install script, and room for future CLI/packages. Adopts upstream [agent-device](https://github.com/callstack/agent-device) rather than reinventing a mobile-control CLI ([`agent-device-cli` research](agent-device-cli.md); cancelled [`xq-motest-cli`](xq-motest-cli.md)).

**Decision (install):** `scripts/install-cli.sh` installs pinned `agent-device` only — no `xq-qe` wrapper package in this repo.