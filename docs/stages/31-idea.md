# Outcome #31: Unified XCUITest Support — Idea

## Outputs

- Approved outcome: both XQ iOS applications consume one shared, versioned
  XCUITest support framework while retaining app-specific screen objects and
  journeys in their owning repositories.
- Delivery targets: `ExperienceQuality/xq-financial-app` and
  `ExperienceQuality/xq-fitness-app`, both registered as `delivery-ready`.
- Maintainer, independent reviewer, and release approver: `@chauhaidang`.
- Research boundary: compare at least two viable sharing mechanisms before
  selecting an architecture; do not migrate either application during research.
- Primary quality signals: deterministic test isolation, consistent failure
  diagnosis, and lower duplicated harness maintenance.

## Evidence

- [Outcome #31](https://github.com/ExperienceQuality/xq-hub/issues/31)
- [Idea stage #32](https://github.com/ExperienceQuality/xq-hub/issues/32)
- [Human approval](https://github.com/ExperienceQuality/xq-hub/issues/32#issuecomment-5381543106)
- Both applications currently contain parallel `ApplicationDescriptor`,
  `LaunchConfiguration`, `BaseUITestCase`, element-helper, screen-object, and
  simulator-runner implementations.
- Repository ownership, validation commands, environments, and dependencies are
  recorded in `repository-context.yaml` and
  `docs/agents/repository-catalog.md`.

## Next-stage input

- Open a `stage:research` issue owned by `role:sdet` under outcome #31.
- Capture each application's current test-harness seam at an exact commit.
- Compare a shared Swift package, a shared source target, and any other viable
  native Xcode mechanism using primary-source evidence.
- Recommend one option with migration boundaries, versioning strategy,
  validation commands, unresolved risks, and a decision-critical proof-of-concept
  proposal.
- Keep all research read-only with respect to the two application repositories.
