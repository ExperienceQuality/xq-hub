# Idea: xq-terminal

**Status:** Collapsed to Spec — see [`docs/specs/xq-terminal.md`](../specs/xq-terminal.md).

TAP-like **airport Terminal** for ExperienceQuality: an asset checks in with a **passport** (small/medium results); the Terminal decides **qualified / not** to board the **merge** or **release** plane. Release boarding uses a **sandbox** and runs asset logic via **remote JVM Spec plugins** (fat JAR + ServiceLoader), not Runner build dependencies.

**Stack:** JVM 17+ / Gradle — shared API in Satellite [`xq-terminal-sdk`](../specs/xq-terminal-sdk.md) (Java), Runner CLI in `xq-qe-box` (Java or Kotlin), Jackson passport models, isolated ClassLoaders. Ship Runner as **JVM app** (`installDist`). Specs publish as **GitHub Release assets** pinned by **URL + sha256**; SDK publishes via **GitHub Packages**.

Not a clone of Google’s internal TAP binary — borrows the **gateway** role. Aligns with Hub [`quality/`](../../quality/README.md).

**Home:** Runner/skills → [`xq-qe-box`](../specs/xq-qe-box.md); API → [`xq-terminal-sdk`](../specs/xq-terminal-sdk.md).

**Alternate (Python) Idea:** Specs as wheels; registry meta-package lists them as `pyproject` deps after YAML sanitize — [`xq-terminal-python-registry`](xq-terminal-python-registry.md).
