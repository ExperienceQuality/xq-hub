# Research: Google TAP

**Status:** Research / shaping — primary-source survey of Google’s internal **Test Automation Platform (TAP)**; not a Spec.

**Related Hub context:** [`system-test-automation-platform.md`](system-test-automation-platform.md) (system / E2E platform architecture) · Hub testing vocabulary via ISTQB in that note.

**Acronym collision:** Industry **TAP** often means the [Test Anything Protocol](https://testanything.org/) (a language-agnostic test result stream). **This note is about Google’s Test Automation Platform**, the company-wide continuous build/test system — unrelated to that protocol except by shared letters.

---

## Intent

How does Google engineering implement **TAP** (Test Automation Platform / global continuous testing)? Cover scope, architecture (presubmit vs postsubmit), test selection, flake/failure policy, hermeticity vs live deps, QA vs SWE roles relative to TAP, and what is publicly documented vs internal-only — so XQ can borrow *practices*, not invent a fantasy clone of closed infrastructure.

Primary anchors: *Software Engineering at Google* (free HTML at [abseil.io/resources/swe-book](https://abseil.io/resources/swe-book)), Google Testing Blog posts by Googlers, and Google-authored ICSE/ICST research papers.

---

## 1. What TAP is (and is not)

Google’s **Test Automation Platform (TAP)** is the **global continuous build (CB)** that runs the **majority of automated tests** across the monorepo and is the **gateway for almost all changes** ([SWE Book ch. 23 — Continuous Integration](https://abseil.io/resources/swe-book/html/ch23.html), Adam Bender section “TAP: Google’s Global Continuous Build”).

Published scale snapshots (different years — treat as era-specific, not a single current SLA):

| Claim | Source / era |
| --- | --- |
| &gt;50k unique changes/day and &gt;4 billion individual test cases/day | [SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html) |
| ~13k code projects/day, ~800k builds, ~150M test runs; monorepo on order of ~2B LOC | [Memon et al., ICSE-SEIP 2017 — *Taming Google-Scale Continuous Testing*](https://research.google.com/pubs/archive/45861.pdf) |
| Average ~1 commit/second; milestones historically ~every 45 minutes at peak; affected sets up to millions of tests | Same paper; echoed in [Leong et al., ICSE-SEIP 2019](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google) |

**Scope of tests TAP runs:** TAP schedules **test targets** declared in BUILD files — a target may be a JUnit suite, a single Python test, or a collection of end-to-end scripts ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)). So TAP is **not “unit-only”**: it can include integration/system-ish targets *if* they are declared as buildable test targets and selected into CB.

**Explicit limit:** Many **large** tests **do not belong in TAP** because they are nonhermetic, too flaky, and/or too resource-intensive; Google runs separate post-submit continuous builds (and encourages presubmit where feasible) for those ([SWE Book ch. 14 — Larger Testing](https://abseil.io/resources/swe-book/html/ch14.html)). Release automation is a **separate** system from TAP ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html) footnote on Rapid / SRE book).

**Granularity of “a test”:** Google’s public papers emphasize **targets**, not classical per-method fault matrices — FAILED may mean one case inside a multi-case target ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)).

---

## 2. Architecture: how TAP fits Google CI

### 2.1 Developer path (public sketch)

From Google authors’ ICST 2025 industry paper (author HTML reprint of *Speculative Testing at Google with Transition Prediction*; also indexed at [research.google](https://research.google/pubs/speculative-testing-at-google-with-transition-prediction)):

1. Edit / build / test locally (Blaze/Bazel + Forge compute).
2. Code review via **Critique**; **Presubmit** checks run (intentionally limited for cost/latency).
3. After approval/merge, **TAP Postsubmit** finds breakages that slipped past presubmit.
4. Release automation consumes **green / project-health** signals separately from TAP’s core CB ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

### 2.2 Presubmit vs postsubmit

| Mode | Role | Public practices |
| --- | --- | --- |
| **Presubmit** | Fast gate before submit; typically a **project-local**, fast, reliable subset (often unit/small tests) | Rule of thumb: only fast, reliable tests; accept incomplete coverage and catch remainder postsubmit ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)). Empirically, changes that pass presubmit have **95%+** chance of passing the rest; average wait to submit ~**11 minutes** ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)). Presubmit may use ML-driven selection and “advisors” that ignore/rerun failures ([Henderson et al. / Kondareddy et al. 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html); [Hoang & Berding 2024](https://dl.acm.org/doi/10.1145/3643656.3643896) *Presubmit Rescue* — abstract-level public). |
| **Postsubmit** | Asynchronous **comprehensive** validation of **all potentially affected** targets, including larger/slower ones | Batching / milestones / comprehensive cycles because per-commit full affected sets are unaffordable ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf); [SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)). |

**Why not all tests on every presubmit:** Cost to engineer time, flakiness blocking unrelated authors, and **mid-air collisions** (two independent changes combine to break a test between checkout and submit) ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

**True head vs green head:** CB marks verified commits “green”; engineers often sync to green for stability while submission policies may require true head ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

### 2.3 Execution substrate: Blaze/Bazel + Forge

- **Blaze** is Google’s internal build system; **Bazel** is the open-source reimplementation ([SWE Book ch. 18](https://abseil.io/resources/swe-book/html/ch18.html); [Bazel — artifact-based builds](https://bazel.build/basics/artifact-based-builds)).
- Most remote build/test execution runs on **Forge**, a datacenter distributed build-and-test system that maximizes parallelism and caches actions ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html); [Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)).
- TAP consumes a **near-real-time global dependency graph** from Forge/Blaze to select downstream tests ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

**Public vs closed:** Bazel hermeticity docs are open ([Bazel Hermeticity](https://bazel.build/basics/hermeticity)). **TAP itself, Forge, Critique, and Google’s CB control plane are not open products.**

### 2.4 Historical evolution (public narrative)

TAP launched ~2009 as centralized CB replacing federated team-owned CI; early “run on every change” designs hit capacity walls by ~2012, driving batching, skipping, and repeated rewrites of pre- and post-submit ([Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html), citing Micco and others). Exact internals of each rewrite remain unpublished.

```
Local Blaze/Forge ──► Critique + Presubmit (fast subset / ML selection)
                              │
                              ▼ submit
                     TAP Postsubmit
                     ├── Comprehensive / milestone cycles (all affected since last green cut)
                     └── Speculative cycles (small predicted-fail subset, higher frequency) [2025+]
                              │
                              ▼
                     Culprit finding → Build Cop / autorollback → Release (Rapid, separate)
```

---

## 3. Test selection, affected targets, milestones / cycles

### 3.1 Static selection: reverse dependency / “affected”

At each changelist (CL), TAP computes **AFFECTED** test targets: transitive reverse dependencies of modified files via BUILD rules (and language-specific implicit deps) ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf); [Leong et al. 2019](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google)). SWE Book frames the same idea as downstream dependency analysis via Forge/Blaze ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

### 3.2 Batching: milestones → comprehensive cycles

Because commit rate × test-pool growth made per-CL execution quadratic/unsustainable, TAP **bundles** consecutive commits into **milestones** (also called **comprehensive testing cycles**): run the union of affected targets since the previous cut, at the latest affecting CL for each target ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf); [Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)).

Era notes:

- ~2016–2017 public data: milestones often ~**45 minutes** at peak; delays observed up to **~9 hours** under infra stress; sets as large as **~4.2M** tests ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)).
- ~2025 public description: comprehensive cycles when Forge capacity allows; ~**1–2 hours** until a broken test starts failing in postsubmit in the described regime ([Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)).

SKIPPED outcomes include targets that did not match selection criteria (e.g. too large, marked **NOTAP**, etc.) ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)). Exact NOTAP policy UI/API is not public.

### 3.3 Beyond static deps: research → production ML

Empirical findings that motivated smarter scheduling ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)):

- Vast majority of affected targets **never fail**; tiny fraction of executions reveal real break/fix transitions (~**1.23%** after flake filtering in their study window).
- Breakages concentrate at modest **MinDist** in the dependency graph (roughly **5–10** edges for non-flaky edge targets in their analysis) — “far” dependents rarely detect breakages.
- Certain authors/tools, file types, and multi-author hot files correlate with higher breakage risk.

[Leong et al. 2019](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google) evaluate **transition-based** RTS (Pass↔Fail), stress that **84% of transitions are flaky** (citing Micco), and find simple heuristics based on affecting-commit frequency / distinct authors beat random — but a large gap to optimal remains; recent-failure heuristics fail when flakes dominate.

**Speculative Cycles (production direction, 2025):** Between comprehensive cycles, run a **smaller, ranked subset** more often (~every **20 minutes**, capacity allowing), scored by **Transition Prediction** (tree models over coarse target/commit metadata). Reported: with ~**25%** target budget, ~**85%** recall of breakages; median detection latency improved ~**65%** (example: **107 → 37 minutes**, ~70 minutes absolute) on 3 months of production data ([Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html); [research.google abstract](https://research.google/pubs/speculative-testing-at-google-with-transition-prediction)).

**Presubmit optimization (SWE Book):** Continuous build may silently move some configured-presubmit tests to postsubmit ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html) note 8). Changes that trigger fewer tests get scheduled sooner — cultural pressure toward small CLs ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

---

## 4. Flake handling, failure policy, “quotas”

### 4.1 Flake statistics (public)

John Micco, Google Testing Blog ([Flaky Tests at Google and How We Mitigate Them](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html), May 2016):

- ~**1.5%** of test *runs* flaky (same code, both pass and fail observed).
- ~**16%** of *tests* have some flakiness.
- ~**84%** of observed Pass→Fail transitions involve a flaky test — polluting culprit detection and release gates.
- Insertion rate of flakiness ≈ fix rate → a steady residual flake population.

Same numbers are reused in later Google papers ([Leong et al. 2019](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google)).

### 4.2 Mitigations documented publicly

From Micco’s post and SWE Book:

- Rerun failing tests; option to auto-rerun on failure.
- Mark a test **flaky** so it fails the gate only after **3 consecutive** failures (trades latency for fewer false blocks — Micco notes the downside for long tests).
- **Quarantine** when flake rate is too high: remove from critical path + file a bug (may mask real races).
- Tools that detect **changes in flakiness level** and attribute them to CLs.
- Temporarily remove flaky tests from **presubmit** while investigating ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Company-wide **flake classification** in unified test reporting so authors can discount likely flakes ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Postsubmit: failing targets rerun; a **flakiness oracle** further labels flakes ([Leong et al. 2019](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google)).
- **Presubmit Rescue** (Hoang & Berding, FTW 2024): automatically ignoring flaky executions in presubmit — paper is ACM-published by Google authors; full policy text not freely mirrored in this research pass ([ACM](https://dl.acm.org/doi/10.1145/3643656.3643896)).

Micco comment (first-party): service virtualization/fakes are **owned by test authors / infra teams**, not a single central framework beyond generic mocks (e.g. Mockito) ([blog comments thread under the 2016 post](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)).

### 4.3 Failure / breakage policy

- Cultural norm: **do not pile new work on known failing tests**; Build Cop owns green for the project regardless of who broke it ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Preferred fix: **rollback** (two-click rollbacks claimed); TAP can **auto-rollback** when confidence is high ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Later public detail: autorollback often restricted until **≥10** distinct targets blame the same change, given flake/culprit noise ([Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html); SafeRevert line of work).
- Culprit finding: split failing batches; developer binary-search tools; research/production systems for flake-aware culprit ID ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html); [Ziftci & Reardon 2017](https://research.google/pubs/who-broke-the-build-automatically-identifying-changes-that-induce-test-failures-in-continuous-integration-at-google-scale/); [Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)).
- “Keep green” patterns for suites that can’t all be fixed immediately: tag failures with bugs and suppress until cleaned (Takeout case study) ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

### 4.4 Resource “quotas”

No public user-facing quota API is documented. What *is* public: Forge/TAP are **resource-constrained**; cycle frequency is capacity-gated; TAP biases scheduling toward changes with **fewer** triggered tests; postsubmit waits for Forge headroom ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html); [Speculative Testing 2025](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)). Treat “quota” as **fleet scheduling + selection budgets** (e.g. speculative top-k), not a published product feature.

---

## 5. Environments, hermeticity, stubs/fakes vs live services

### 5.1 Test size taxonomy (feeds what may enter TAP)

[SWE Book ch. 11 — Testing Overview](https://abseil.io/resources/swe-book/html/ch11.html):

- **Small:** single process; no sleep/I/O/network; doubles for heavy deps.
- **Medium:** multi-process OK; network only to `localhost`.
- **Large:** multi-machine / remote OK; often isolated from day-to-day workflow.

Pyramid guideline ~**80% / 15% / 5%** narrow / medium / broad scope. Hermeticity is a goal at all sizes; harder as size grows.

### 5.2 Hermetic servers & backends

- [Google Testing Blog — Hermetic Servers](https://testing.googleblog.com/2012/10/hermetic-servers.html) (Narla & Salas): “server in a box” — entire server starts on one machine without network; DI for peers; bundled static assets; faked datastores; used for startup/API/UI e2e on continuous builds when changelists affect the SUT.
- SWE Book: hermetic backends for larger-scoped **presubmit**; full sandboxed stacks (extreme: ~400 servers for DisplayAds) vs more common **record/replay**; many teams mix hermetic and live backends ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Presubmit apps should generally **not** talk to real production backends (security/quota) ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

### 5.3 What stays outside TAP

Nonhermetic / ultra-flaky / ultra-expensive large tests → **alternate** CB/presubmit paths ([SWE Book ch. 14](https://abseil.io/resources/swe-book/html/ch14.html)). Staging/prod probers and manual QA on RCs appear in the CD path, not as TAP’s core job ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).

---

## 6. QA vs SWE relative to TAP

Primary sources describe a **developer-owned automated testing culture**, not a classic QA gate in front of TAP:

- “Unlike the QA processes of yore… engineers who build systems today play an active and integral role in writing and running automated tests for their own code” ([SWE Book ch. 11](https://abseil.io/resources/swe-book/html/ch11.html)).
- TAP breakages are handled by **SWEs** acting as **Build Cop** (and automated culprit/rollback), not a separate QA org running TAP ([SWE Book ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)).
- Manual QA still appears **later** (e.g. RC environments) ([ch. 23](https://abseil.io/resources/swe-book/html/ch23.html)); older Google books (*How Google Tests Software*) discuss SET/TE roles historically, but TAP’s public CI story is EngProd/SWE + Build Cop.

**Honest gap:** No first-party public doc says “QA does not use TAP” as a hard rule; the documented operating model is **SWE-written tests + centralized CB**.

---

## 7. What outsiders can adopt vs what is internal-only

| Transferable (public practices / OSS-adjacent) | Internal-only / not productized |
| --- | --- |
| Presubmit = fast/reliable; postsubmit = comprehensive | TAP binary / control plane / Google fleet |
| Affected-target selection via build graph (Bazel `bazel query` / similar) | Forge, company-wide realtime dep graph service |
| Milestone/batch testing when commit rate outruns capacity | Google milestone scheduler + NOTAP policies |
| Transition-focused selection; de-flake before measuring RTS | Production Transition Prediction features/training |
| Flake quarantine, reruns, flake marks, Build Cop + rollback-first | Auto-rollback thresholds, flakiness oracle implementation |
| Hermetic servers / Testcontainers-like isolation; record-replay | Google sandbox server catalogs |
| Small/medium/large **enforceable** size constraints | Google custom security managers / size enforcement |
| Separate lanes for nonhermetic system/E2E | Exact TAP vs non-TAP routing UIs |
| Bazel hermetic builds ([docs](https://bazel.build/basics/hermeticity)) | Blaze-only integrations |

**Not adoptable by cloning:** monorepo-wide single CB at Google scale; Critique; Rapid; Google’s speculative-cycle ML stack as a service.

---

## 8. Implications for XQ (thin)

Against Hub research in [`system-test-automation-platform.md`](system-test-automation-platform.md):

1. **Do not equate “TAP” with a system-test platform.** Google itself keeps many nonhermetic system/E2E suites **out of TAP** and on alternate CBs ([ch. 14](https://abseil.io/resources/swe-book/html/ch14.html)). XQ’s system/E2E control plane maps more to those **side lanes + orchestration**, while a TAP-like core maps to **fast hermetic/component gates** on every change.
2. **Steal the split, not the monorepo.** PR/presubmit: contracts + Testcontainers + small hermetic suites; merge/nightly: thinner production-like multi-surface E2E — same “shift left what is fast/deterministic” logic as [ch. 23](https://abseil.io/resources/swe-book/html/ch23.html) and the Hub note’s layered architecture.
3. **Affected selection beats “run everything.”** Even without Blaze, path/package graphs or explicit suite tags + skip budgets matter once suites grow; Google’s papers show most affected tests never fail ([Memon et al. 2017](https://research.google.com/pubs/archive/45861.pdf)).
4. **Flake policy is part of the platform.** Quarantine, rerun rules, and “green via tagged known failures” are first-class; otherwise system E2E will recreate Micco’s 84%-transitions-are-flakes problem ([Micco 2016](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)).
5. **Own scenarios/adapters; don’t invent Forge.** Hub already prefers composing Playwright/Appium/Maestro/agent-device + orchestration ([system-test note](system-test-automation-platform.md)) — consistent with Google open-sourcing **Bazel**, not TAP.

---

## Open questions / public-info gaps

- Current (2026) TAP capacity SLOs, exact speculative-cycle parameters, and presubmit ML feature sets beyond papers.
- Full text / operational semantics of **Presubmit Rescue** and **NOTAP** marking.
- How mobile/UI lab farms (if any) attach to TAP vs separate CBs — not covered in the cited TAP papers.
- Precise EngProd org chart vs Build Cop rotations today.
- Whether “four billion test cases” vs “150M test runs” counting methodology is stable across eras.

---

## Sources

### Books / first-party HTML

- [Software Engineering at Google — ch. 11 Testing Overview](https://abseil.io/resources/swe-book/html/ch11.html)
- [Software Engineering at Google — ch. 14 Larger Testing](https://abseil.io/resources/swe-book/html/ch14.html)
- [Software Engineering at Google — ch. 18 Build Systems](https://abseil.io/resources/swe-book/html/ch18.html)
- [Software Engineering at Google — ch. 23 Continuous Integration (TAP)](https://abseil.io/resources/swe-book/html/ch23.html)
- [SWE Book landing](https://abseil.io/resources/swe-book)

### Google Testing Blog

- [Flaky Tests at Google and How We Mitigate Them (John Micco, 2016)](https://testing.googleblog.com/2016/05/flaky-tests-at-google-and-how-we.html)
- [Hermetic Servers (Narla & Salas, 2012)](https://testing.googleblog.com/2012/10/hermetic-servers.html)

### Research papers / Google Research

- [Memon et al. — Taming Google-Scale Continuous Testing (ICSE-SEIP 2017) PDF](https://research.google.com/pubs/archive/45861.pdf) · [pub page](https://research.google/pubs/taming-google-scale-continuous-testing/)
- [Leong et al. — Assessing Transition-based Test Selection Algorithms at Google (ICSE-SEIP 2019)](https://research.google/pubs/assessing-transition-based-test-selection-algorithms-at-google) · [open PDF mirror (uni.lu)](https://orbilu.uni.lu/bitstream/10993/39793/1/conference_041818%20%286%29.pdf)
- [Kondareddy / Azad / Singh / Henderson — Speculative Testing at Google with Transition Prediction (ICST 2025)](https://research.google/pubs/speculative-testing-at-google-with-transition-prediction) · [author HTML](https://hackthology.com/speculative-testing-at-google-with-transition-prediction.html)
- [Ziftci & Reardon — Who Broke the Build? (ICSE-SEIP 2017)](https://research.google/pubs/who-broke-the-build-automatically-identifying-changes-that-induce-test-failures-in-continuous-integration-at-google-scale/)
- [Hoang & Berding — Presubmit Rescue (FTW 2024)](https://dl.acm.org/doi/10.1145/3643656.3643896) (ACM; full PDF not retrieved in this pass)

### Bazel / protocol disambiguation

- [Bazel is the open-sourced version of Blaze](https://bazel.build/basics/artifact-based-builds)
- [Bazel Hermeticity](https://bazel.build/basics/hermeticity)
- [Test Anything Protocol (unrelated acronym)](https://testanything.org/)

### Hub

- [`docs/ideas/system-test-automation-platform.md`](system-test-automation-platform.md)
