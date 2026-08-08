# Idea: xq-terminal

**Status:** Collapsed to Spec — see [`docs/specs/xq-terminal.md`](../specs/xq-terminal.md) (**Python-only**).

TAP-like **airport Terminal**: passport (small/medium) → qualified / not for **merge** or **release**. Release runs Specs via **`xq-terminal-registry`** (YAML-sanitized meta-package of Spec wheels), not Terminal direct deps.

**Stack:** Python 3.11+ / uv — protocol in [`xq-terminal-sdk`](../specs/xq-terminal-sdk.md), registry in [`xq-terminal-registry`](../specs/xq-terminal-registry.md), CLI in `xq-qe-box`.

**Home:** Runner/skills → [`xq-qe-box`](../specs/xq-qe-box.md); API → sdk; Spec intake → registry.

**Code-level Specs:** [`xq-terminal`](../specs/xq-terminal.md) · [`xq-terminal-sdk`](../specs/xq-terminal-sdk.md) · [`xq-terminal-registry`](../specs/xq-terminal-registry.md) · [`xq-terminal-spec-wheel`](../specs/xq-terminal-spec-wheel.md)
