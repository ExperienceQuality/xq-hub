# Spec: xq-terminal Spec wheel (authoring)

**Status:** Active — code-level contract for one Spec package (e.g. `login-spec`, `payment-spec`).

**Related:** [`xq-terminal-sdk.md`](xq-terminal-sdk.md) · [`xq-terminal-registry.md`](xq-terminal-registry.md) · Ticket [#22](https://github.com/ExperienceQuality/xq-hub/issues/22)

## Problem

Spec authors need a copy-pasteable package shape so registry sanitize/entry points work without Terminal changes.

## Solution

Each Spec is a **Python wheel** that:

1. Depends on `xq-terminal-sdk`  
2. Implements `RunnerSpec`  
3. Exposes `load_spec()` via entry point group `xq_terminal.specs`  
4. Is pinned in registry `specs.yaml` after publish  

### Tree (example `payment-spec`)

```
payment-spec/
├── README.md
├── pyproject.toml
├── src/payment_spec/
│   ├── __init__.py       # load_spec + PaymentSpec
│   └── checks.py         # optional helpers
└── tests/
    └── test_payment_spec.py
```

### `pyproject.toml`

```toml
[project]
name = "payment-spec"
version = "2.1.0"
requires-python = ">=3.11"
dependencies = [
  "xq-terminal-sdk>=0.1,<0.2",
  # Spec-owned deps only — will be pulled into registry env
]

[project.entry-points."xq_terminal.specs"]
payment-spec = "payment_spec:load_spec"

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"
```

Entry point **name** must equal registry `id` and CLI `--spec`.

### Code

```python
# src/payment_spec/__init__.py
from __future__ import annotations
from xq_terminal_sdk import SpecContext, SpecResult

class PaymentSpec:
    def name(self) -> str:
        return "payment-spec"

    def run(self, context: SpecContext) -> SpecResult:
        if not context.artifact_ref:
            return SpecResult(False, "missing artifact_ref")
        # large / release checks against sandbox + artifact
        return SpecResult(True, "payment checks ok")

def load_spec() -> PaymentSpec:
    return PaymentSpec()
```

### Registry pin (after publish)

```yaml
# in xq-terminal-registry/specs.yaml
- id: payment-spec
  version: "2.1.0"
  dist: payment-spec
```

### Login Spec (same shape)

```python
# login_spec — name() -> "login-spec"; entry point login-spec = "login_spec:load_spec"
```

## Acceptance (#22)

- [ ] Example wheel builds and installs  
- [ ] `entry_points(group="xq_terminal.specs")["payment-spec"].load()()` returns RunnerSpec  
- [ ] Documented YAML pin for #25  
- [ ] Release board (#19) can invoke via registry  

## Out of scope

- Implementing real payment business logic  
- Fat JAR / Java  
