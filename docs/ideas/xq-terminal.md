# Idea: xq-terminal

**Status:** Collapsed to Spec — see [`docs/specs/xq-terminal.md`](../specs/xq-terminal.md).

TAP-like **airport Terminal** for ExperienceQuality: an asset checks in with a **passport** (small/medium results); the Terminal decides **qualified / not** to board the **merge** or **release** plane. Release boarding uses a **sandbox** and runs asset logic via **remote JVM Spec plugins** (fat JAR + ServiceLoader), not Runner build dependencies.

**Stack:** JVM 17+ / Gradle — shared `runner-sdk` (Java API), Runner CLI may be **Java or Kotlin**, Jackson passport models, isolated ClassLoaders. Ship as **JVM app** (`installDist`), not a native binary. Specs publish as **GitHub Release assets** pinned by **URL + sha256** (not Maven Packages in v1).

Not a clone of Google’s internal TAP binary — borrows the **gateway** role. Aligns with Hub [`quality/`](../../quality/README.md).

**Home:** Satellite [`xq-qe-box`](../specs/xq-qe-box.md).
