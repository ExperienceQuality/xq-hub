# Spec: xq-motest deep modules

**Status:** Active — buildable plan for Satellite `xq-qe-box`.

**Related:** [`docs/specs/xq-qe-box.md`](xq-qe-box.md), architecture review (2026-08-01), Satellite ADR `docs/adr/0001-cli-assumes-devicekit-preinstalled.md` in `xq-qe-box`.

## Problem

`xq-motest` still behaves like a shell-orchestration façade over DeviceKit: bash scripts, `#filePath` package-root walks, a fat executable (`MotestCommand`), library `exit`/`Never`, a cosmetic transport injection, and a flat all-`public` Motest surface. That fights Swift package norms and blocks deep, testable Session modules agents can call with locality.

## Hard constraint (problem 6 — decided)

**`xq-motest` ships no install/resign/lifecycle shell scripts.** The agent host environment must already have the DeviceKit runner (`.app` / `.ipa`) available and installed on the test devices. The CLI assumes that infra is correct and only talks to DeviceKit (health + JSON-RPC). Missing runner → clear MotestError, not an install path.

Recorded as Satellite ADR-0001. Do not revive `scripts/devicekit/*`, `ModulePaths` / `ShellScripts` discovery, or `devicekit install` / resign as CLI responsibilities.

## Solution

Deepen five modules in `cli/xq-motest` (Swift SPM). Order is intentional: Runtime first, then CommandRunner, then errors / KitClient / access control.

### 1. DeviceKitRuntime (Strong)

**Files today:** `RuntimeEnsure.swift`, `DeviceKitStart.swift`, `HealthWait.swift`, `ShellScripts.swift` / `ModulePaths`, `ResignIPA.swift`, `scripts/devicekit/*`, related `devicekit` CLI verbs.

**Change:** One deep **DeviceKitRuntime** module owns “is DeviceKit ready for this Session?” — health wait, and only the minimum start of an **already-installed** runner if the product still needs a start signal. Delete script discovery and install/resign surface. Fail fast when the runner is not installed or not reachable; hints point at infra, not CLI install.

**Done when:**

- [ ] No `scripts/` under `cli/xq-motest` except optional test runners (`run-all.sh` / `run-swift.sh` may remain as host test helpers, not DeviceKit install)
- [ ] No `#filePath` walk to locate bash DeviceKit helpers
- [ ] `devicekit install` / resign paths removed from Motest public surface and skill docs
- [ ] RuntimeEnsure / start / health share one module interface; MotestCommand does not own boot details

### 2. CommandRunner as Session surface (Strong)

**Files today:** `MotestCommand.swift` (~438 lines), `CommandRunner.swift`, `Session` / map / kit call sites.

**Change:** Every Session mutation and query (including screenshot, foreground, app install-of-**AUT**, tap parsing) goes through **CommandRunner**. Executable is ArgumentParser + Envelope mapping only.

**Done when:**

- [ ] MotestCommand has no KitTransport / screenshot / tap business logic
- [ ] CommandRunner is the single in-process interface skills and tests call for Session verbs
- [ ] New verb = extend CommandRunner, not the exe

### 3. Errors throw; libraries don’t exit (Worth exploring)

**Files today:** `Envelope.swift`, `MotestError.swift`, `ProcessRunner.swift`, library call sites using `Never` / `exit` / `try!`.

**Change:** Motest library throws `MotestError` (or returns `Result`). Envelope emit + process exit live only in the `xq-motest` executable target.

**Done when:**

- [ ] Motest target has no `exit` / `Never` error helpers used as control flow
- [ ] XCTest can exercise failure paths without process death
- [ ] Exe maps MotestError → Envelope + ExitCode

### 4. Real KitClient adapter (Worth exploring)

**Files today:** `Transport.swift` / KitTransport, `HealthWait.swift`, `Session`, RuntimeEnsure injection.

**Change:** One **KitClient** interface for health + JSON-RPC. URLSession is one adapter; tests use a fake. Prefer structured concurrency over `DispatchSemaphore` sync-over-async.

**Done when:**

- [ ] Health and RPC share the same KitClient
- [ ] RuntimeEnsure / DeviceKitRuntime do not open a second URLSession stack
- [ ] At least one fake KitClient used in Motest tests

### 5. Package shape and access control (Worth exploring)

**Files today:** flat `Sources/Motest/*`, nearly everything `public`.

**Change:** Default `internal`. Public Motest surface is Session, CommandRunner, MotestError (and thin Config as needed). Optional folder nesting or target split only if it clarifies the interface.

**Done when:**

- [ ] Helpers (process, paths, resign leftovers if any) are not part of the public interface
- [ ] README / skill document the public surface, not every type
- [ ] `swift build` + existing test script still green

## Out of scope

- Embedding DeviceKit runner binaries in the CLI package
- agent-device daemon path (`xq-mobile-auto-test`)
- Cloning/forking DeviceKit-ios feature work (separate Spec)
- Android / non-Apple DeviceKit
- Designing concrete Swift protocols in this Spec (interfaces come after grilling a slice)

## Tracer-bullet Tickets

Label: `satellite:xq-qe-box` + `ready-for-agent`. Native `blocked_by` edges: `#9←#8`, `#10←#9`, `#11←#10`, `#12←#9`.

1. [#8](https://github.com/ExperienceQuality/xq-hub/issues/8) — DeviceKitRuntime + delete install scripts (§1 + ADR-0001) — **unblocked**
2. [#9](https://github.com/ExperienceQuality/xq-hub/issues/9) — CommandRunner owns Session verbs (§2)
3. [#10](https://github.com/ExperienceQuality/xq-hub/issues/10) — MotestError throws; Envelope at exe edge (§3)
4. [#11](https://github.com/ExperienceQuality/xq-hub/issues/11) — KitClient adapter + fake in tests (§4)
5. [#12](https://github.com/ExperienceQuality/xq-hub/issues/12) — internal-by-default Motest public surface (§5)

## Acceptance (Spec-level)

- [ ] ADR-0001 accepted in `xq-qe-box`; skill/README state “runner preinstalled by infra”
- [ ] Five Ticket slices above closed with merged PRs linking Hub Tickets
- [ ] `bash cli/xq-motest/scripts/run-all.sh` (or successor) green without DeviceKit install scripts
