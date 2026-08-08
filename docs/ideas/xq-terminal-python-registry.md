# Idea: xq-terminal Python registry (Spec megapackage)

**Status:** Active Idea — Python path for Terminal Specs via a **registry meta-package**. Distinct from the locked JVM path in [`xq-terminal`](../specs/xq-terminal.md) / [`xq-terminal-sdk`](../specs/xq-terminal-sdk.md); collapse to Spec when chosen as product direction.

**Related:** throwaway POC under Hub `.prototype/xq-terminal-registry-demo/` (vendoring sketch; product plan below is **dependency**-based).

## Problem

Spec authors need to publish `login-spec`, `payment-spec`, … without teaching `xq-terminal` about each wheel URL, sha256, or dependency tree. Dependency resolution must not live in the Terminal runner. Authors should register Spec releases in one place; Terminal should only consume a single registry package.

## Solution (planned)

Three logical pieces (not necessarily three long-lived product repos):

| Piece | Role |
| --- | --- |
| **Spec wheels** | e.g. `login-spec`, `payment-spec` — each implements the Spec protocol + entry point |
| **Registry repo** | Authors register releases in **YAML**; CI **sanitizes**, then **generates** `pyproject.toml` and publishes **`xq-terminal-registry`** |
| **Terminal** | Depends only on `xq-terminal-registry` (pinned version); `get_spec(id).run(ctx)` |

```text
Author publishes login-spec==1.0.0, payment-spec==2.1.0
        │
        ▼
Registry repo: specs.yaml  (human registration surface)
        │
        ▼
CI sanitize (allowlist, pins, dep policy)  ← reason YAML exists
        │
        ▼
Generated pyproject.toml
  dependencies = ["login-spec==1.0.0", "payment-spec==2.1.0", …]
        │
        ▼
Build/publish xq-terminal-registry==…
        │
        ▼
Terminal: pip install xq-terminal-registry==…
         get_spec("payment-spec")  # entry point / import into Spec wheels
```

### Why YAML (not hand-edited registry `pyproject.toml`)

Authors must **not** write the registry’s `pyproject.toml` directly. YAML is the controlled intake; CI **sanitizes Spec declarations before** they become pip dependencies:

- **Allowlist** — only known Spec package names / sources  
- **Exact pins** — `==` versions only (no open ranges unless explicitly policy-approved)  
- **Dep policy** — reject or rewrite Spec metadata that would pull disallowed transitive deps into the registry env  
- **Identity mapping** — stable Spec id (`login-spec`) ↔ distribution name / entry point  
- **Reviewable diffs** — humans review YAML PRs; generated `pyproject.toml` is an artifact  

So: **YAML = sanitize gate**; **`pyproject.toml` = generated install plan**.

### How Specs are “loaded”

1. **Install time:** `pip install xq-terminal-registry` installs Spec wheels listed as **`[project] dependencies`**.  
2. **Run time:** registry `get_spec(id)` uses **entry points** (group e.g. `xq_terminal.specs`) or explicit imports into those installed packages.  

No vendoring of `.py` into the registry tree for the product plan (vendoring was only a POC shortcut).

### Example registration

```yaml
# registry/specs.yaml
specs:
  - id: login-spec
    version: "1.0.0"
    dist: login-spec          # PyPI / Packages distribution name
  - id: payment-spec
    version: "2.1.0"
    dist: payment-spec
```

Generated (after sanitize):

```toml
# registry/pyproject.toml  (GENERATED — do not hand-edit)
[project]
name = "xq-terminal-registry"
version = "0.1.0"   # registry release line (bumps when membership/pins change)
dependencies = [
  "login-spec==1.0.0",
  "payment-spec==2.1.0",
]
```

Spec wheel advertises:

```toml
# login-spec/pyproject.toml
[project.entry-points."xq_terminal.specs"]
login-spec = "login_spec:load_spec"
```

Terminal:

```python
from xq_terminal_registry import get_spec

result = get_spec("payment-spec").run(context)
```

### Version pumping

- Author ships a new Spec wheel version.  
- Author (or bot) updates **one YAML pin** (or CI proposes a PR).  
- Sanitize → regenerate `pyproject.toml` → publish new **registry** version.  
- Terminal pins **`xq-terminal-registry==x.y.z`** for reproducible boarding (not floating `latest`).  

Registry row for Spec *identity* stays; pins move. Terminal does not list Specs in its own `pyproject.toml`.

### Repos (guidance)

| Repo | Owns |
| --- | --- |
| Spec repos (N) | Individual Spec wheels |
| Registry (1) | `specs.yaml`, sanitize CI, generated megameta package |
| Terminal (1) | Runner CLI; depends on registry only |

SDK protocol may live as a small shared wheel depended on by Specs (and optionally registry); Terminal need not depend on every Spec.

## Out of scope (this Idea)

- Replacing the locked **JVM** Terminal + Release JAR + sha256 path until an explicit pivot  
- Real sandbox backends  
- Letting Spec authors push arbitrary deps into registry without sanitize  

## Open questions

- Where Spec wheels are hosted (Private PyPI / GitHub Packages / Release assets + index)?  
- How strict is transitive dep sanitization (sbom, deny-list, hash-pinning)?  
- Relationship to Hub Ticket stream if this becomes the chosen product path vs JVM Spec  

## Capture note

Locked product Specs today remain JVM-oriented. This Idea records the **Python registry-as-dependencies** plan and the **YAML sanitize-before-pyproject** rationale for when/if that path is selected.
