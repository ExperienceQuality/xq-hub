# Spec: xq-motest-cli

**Status:** Cancelled — superseded by an existing project that meets the same goal. No Tickets will be published from this Spec. Historical decisions remain below for context only.

---

## Problem Statement

Coding agents can change app code but cannot reliably drive and verify iOS UI the way a human tester does. Existing tools split the problem: Vibium-style agent UX (map → stable refs → act) exists for browsers, while Mobile Next’s stack shows how to reach devices via a host app and CLI — but agents in this organisation lack a first-class, skill-documented `motest` command surface that owns session lifecycle, caches the app under test, and stays extensible for Android later.

## Solution

~~Ship Satellite `xq-motest-cli`…~~ **Not pursuing.** Adopt the existing project instead of building a parallel Satellite.

## Out of Scope / Cancellation

- Creating `ExperienceQuality/xq-motest-cli`
- Hub catalogue row / `satellite:xq-motest-cli` label for this product
- All proposed tracer-bullet Tickets (repo create → host → session → map/act → skill → acceptance)

## Further Notes

- Parent tracker issue: https://github.com/ExperienceQuality/xq-hub/issues/2 (closed as cancelled)
- If the replacement project should be catalogued as a Satellite or external tool, do that in a new Idea — do not revive this Spec without an explicit reopen decision
