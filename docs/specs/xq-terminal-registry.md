# Spec: xq-terminal-registry

**Status:** Active — buildable plan for Satellite `xq-terminal-registry`.

**Related:** [`docs/specs/xq-terminal.md`](xq-terminal.md) · [`docs/specs/xq-terminal-sdk.md`](xq-terminal-sdk.md) · collapsed from [`docs/ideas/xq-terminal-python-registry.md`](../ideas/xq-terminal-python-registry.md)

## Problem

Spec authors must register Spec releases without editing Terminal, and without pushing unsanitized dependency trees into the Runner. Terminal must not list every Spec wheel as its own dependency.

## Solution

Satellite **`xq-terminal-registry`**: authors register Spec pins in **`specs.yaml`**. CI **sanitizes**, then **generates** `pyproject.toml` so Specs become **`[project] dependencies`** of the meta-package. Publish **`xq-terminal-registry`**. Runtime: `get_spec(id)` via entry points.

### Why YAML exists

Authors must **not** hand-edit registry `pyproject.toml`. YAML is the intake gate so CI can **sanitize before** dependencies are written:

- Allowlist of Spec distribution names / sources  
- Exact `==` version pins  
- Dep policy (reject disallowed / unsafe Spec metadata)  
- Stable id ↔ dist ↔ entry point mapping  
- Reviewable PRs on YAML; `pyproject.toml` is generated  

### Flow

```text
specs.yaml  →  sanitize  →  generated pyproject.toml dependencies
                                    ↓
                         pip install xq-terminal-registry
                                    ↓
                         installs login-spec, payment-spec, …
                                    ↓
                         get_spec("payment-spec").run(ctx)
```

### Example

```yaml
# specs.yaml
specs:
  - id: login-spec
    version: "1.0.0"
    dist: login-spec
  - id: payment-spec
    version: "2.1.0"
    dist: payment-spec
```

```toml
# pyproject.toml (GENERATED)
[project]
name = "xq-terminal-registry"
version = "0.1.0"
dependencies = [
  "login-spec==1.0.0",
  "payment-spec==2.1.0",
]
```

```python
from xq_terminal_registry import get_spec, list_specs
```

Terminal pins **`xq-terminal-registry==x.y.z`** (not floating latest).

## Out of scope

- Vendoring Spec source into the registry tree (product path = pip deps)  
- Terminal depending on Spec wheels directly  
- JVM  

## Acceptance

- [ ] Repo + catalogue label `satellite:xq-terminal-registry`  
- [ ] Sanitize job blocks bad YAML / policy violations  
- [ ] Generated `pyproject.toml` + publishable wheel  
- [ ] `get_spec` / `list_specs` work after install  
- [ ] Documented author flow (PR YAML pin)  

## Tracer Tickets

1. [#25](https://github.com/ExperienceQuality/xq-hub/issues/25) — Bootstrap registry Satellite + sanitize + generate  
2. Wire example Spec (#22) through YAML → Terminal release (#19)  
