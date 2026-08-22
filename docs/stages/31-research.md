# Outcome #31: Unified XCUITest Support — Research

## Outputs

### Current-state boundary

The two applications independently implement the same reusable seam in
`AppUITests/UITestSupport.swift`:

- `ApplicationDescriptor` and `LaunchConfiguration`
- `BaseUITestCase` application lifecycle and failure attachments
- `XCUIElement` existence, hittability, tapping, and text-entry helpers
- `ScreenObject`

The shared component must not own application bundle identifiers, reset policy,
clean-state assertions, accessibility identifiers, screen objects, journeys,
signing settings, or personal-device discovery. Those remain app-owned.

Baseline commits:

- `ExperienceQuality/xq-financial-app@df6cb8fd3eabf996ec2f4998af31f7e690bf465b`
- `ExperienceQuality/xq-fitness-app@60b8de45b617e3a2aa130d417bb1b9e677444b07`

### Options

| Criterion | Standalone source Swift package | Shared Xcode framework target | Prebuilt XCFramework |
| --- | --- | --- | --- |
| Repository and language fit | Native Swift source product linked only to each UI-test target | Native Swift, but requires referenced projects, a workspace, submodule, or vendoring contract | Native binary distribution with separate iOS and Simulator variants |
| Maintenance and versioning | One semantic release line; consumer requirements and resolved revisions | Git/submodule revision plus project and checkout coordination | Binary archive, signing, publication, and compatibility pipeline |
| Deterministic local/CI use | Commit `Package.resolved`; resolve deliberately; retain each app's scheme and XCResult path | Deterministic only with pinned checkout layout and recursive CI setup | Deterministic only with a reproducible binary build and artifact-integrity process |
| Debugging and diagnosis | Source-visible and breakpointable; local package override supported | Source-visible but spans multiple projects/workspaces | Weakest source-level diagnosis |
| Security and licensing | Reviewable source and SwiftPM revision fingerprints; new public package needs an explicit license | Reviewable source with repository controls; same license decision | Requires binary provenance and signature controls |
| Migration and operating cost | Low to medium | Medium to high | High |

### Recommendation

Create a standalone source Swift package, provisionally
`ExperienceQuality/xq-xcuitest-support`, containing one library product with no
external dependencies, plugins, binary targets, or resources. Link it only to
the existing UI-testing bundle in each application. Start with
`swift-tools-version: 5.10`, iOS 17, and a pre-1.0 release line; make only the
consumer extension contract public or open.

This recommendation is conditional on a throwaway proof of concept. SwiftPM and
Xcode support linking a package library product to a selected target, but the
research did not execute an Xcode UI-test bundle that imports an XCTest-using
regular package library and accesses `XCUIApplication` across that module
boundary.

### Decision-critical proof of concept

On throwaway branches or temporary checkouts:

1. Create a local package with the reusable seam and the required public/open
   access levels.
2. Link it only to finance's UI-test target.
3. Add it to fitness `project.yml`, link it only to the UI-test target, record
   the XcodeGen version, and regenerate the project.
4. Import the package from each app-specific test case without moving screens,
   journeys, launch/reset policy, or accessibility contracts.
5. Run one existing reset/relaunch journey per app and one intentional failure
   probe that must retain its screenshot and accessibility hierarchy in the
   XCResult bundle.
6. Switch the local reference to a temporary semantic tag and rerun with a
   committed resolved revision and automatic package resolution disabled.

The proof demonstrates compile/link/runtime compatibility, cross-module
subclassing, source-level diagnostics, failure attachments, and deterministic
resolution. It does not establish full-suite reliability, physical-device
signing, future Xcode compatibility, release automation, or long-term semantic
version discipline.

## Evidence

- [Outcome #31](https://github.com/ExperienceQuality/xq-hub/issues/31)
- [Research stage #34](https://github.com/ExperienceQuality/xq-hub/issues/34)
- [Approved delegation scopes](https://github.com/ExperienceQuality/xq-hub/issues/34#issuecomment-5381556010)
- [Apple: Swift packages](https://developer.apple.com/documentation/xcode/swift-packages)
- [Apple: link a target to a package product](https://help.apple.com/xcode/mac/current/en.lproj/devb83d64851.html)
- [Apple: add tests to an Xcode project](https://developer.apple.com/documentation/xcode/adding-tests-to-your-xcode-project)
- [Apple: Swift packages in CI](https://developer.apple.com/documentation/xcode/building-swift-packages-or-apps-that-use-them-in-continuous-integration-workflows)
- [SwiftPM: package description](https://docs.swift.org/swiftpm/documentation/packagedescription/)
- [SwiftPM: package security](https://docs.swift.org/swiftpm/documentation/packagemanagerdocs/packagesecurity/)
- [XcodeGen project specification](https://github.com/yonaskolb/XcodeGen/blob/master/Docs/ProjectSpec.md)
- Finance static checks: runner scripts and scheme XML parsed, and Xcode listed
  the expected targets and schemes. The catalogued test command was not run.
- Fitness evidence was static inspection only. The catalogued build and test
  command was not run.
- CoreSimulatorService was unavailable in the sandbox, so no simulator pass,
  XCResult inspection, runtime, or flake-rate evidence exists yet.

Material risks and open facts:

- The package repository does not yet exist in `repository-context.yaml`; it
  must be created and registered before build routing.
- Public-package visibility and license are undecided. Neither application
  currently reports a detected license.
- Finance records older Swift settings and must prove compatibility with the
  proposed Swift 5.10 package.
- Fitness does not pin its XcodeGen executable version; project regeneration is
  not yet reproducible.
- Extraction changes internal declarations into a public/open API. The design
  stage must minimize and document that extension surface.
- Both suites currently run serially. Reset isolation and failure attachments
  require behavior-level validation before any parallelization discussion.
- CI configuration was not found in the inspected application trees; no CI
  behavior is claimed.

## Next-stage input

Human decisions required before continuing:

1. Approve or reject the standalone Swift package recommendation.
2. If approved, authorize a `stage:poc` issue before design.
3. Choose the package repository's canonical name, visibility, and license.

The POC must stay outside production code, use the exact baseline commits above,
record Xcode/Swift/XcodeGen and simulator versions, run the representative
commands, preserve both XCResult bundles, and report pass/fail evidence without
starting the full migration.
