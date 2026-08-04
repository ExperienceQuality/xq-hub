# Idea: xq-terminal

**Status:** Collapsed to Spec — see [`docs/specs/xq-terminal.md`](../specs/xq-terminal.md).

TAP-like **airport Terminal** for ExperienceQuality: an asset checks in with a **passport** (small/medium results); the Terminal decides **qualified / not** to board the **merge** or **release** plane. Release boarding also spins a **sandbox** and runs large tests against the shippable artifact (`ipa` / `apk` / service).

Not a clone of Google’s internal TAP binary — borrows the **gateway** role (almost every change is admitted or rejected by continuous testing policy). Aligns with Hub [`quality/`](../../quality/README.md) (sizes, hermeticity, stages).

**Home:** Satellite [`xq-qe-box`](../specs/xq-qe-box.md) — CLI runner/controller + v1 stub sandbox platform colocated (not a new Satellite for v1).
