# Quality standards (Hub)

Normative method for ExperienceQuality testing — **test sizes, hermeticity, and coverage** — derived from Google SWE practices (not a clone of internal TAP).

| Doc | Role |
| --- | --- |
| [`glossary.md`](glossary.md) | Asset, component, capability, attribute, spot, sizes |
| [`principles.md`](principles.md) | Binding rules (sizes, hermeticity, coverage matrix, stages) |
| [`templates/`](templates/) | Ephemeral plan shapes for strategy / plan / reporting / controlling |

**No durable per-asset matrices** in Hub or Satellites. Conformance evidence lives in PR/Ticket text + sized/staged tests.

## Skills (agent-facing copy)

Conforming skills live in [`xq-qe-box`](https://github.com/ExperienceQuality/xq-qe-box) and **vendor these docs under each skill’s `references/`** (no network fetch to Hub at runtime). When you change Hub `quality/`, re-port into the skill `references/` folders in the same change set (or immediately after).

```bash
gh skill install ExperienceQuality/xq-qe-box quality-principles
gh skill install ExperienceQuality/xq-qe-box quality-asset-strategy
gh skill install ExperienceQuality/xq-qe-box quality-test-plan
gh skill install ExperienceQuality/xq-qe-box quality-reporting
gh skill install ExperienceQuality/xq-qe-box quality-controlling
```

## Related research

- [`docs/ideas/google-tap.md`](../docs/ideas/google-tap.md)
- [`docs/ideas/system-test-automation-platform.md`](../docs/ideas/system-test-automation-platform.md)
