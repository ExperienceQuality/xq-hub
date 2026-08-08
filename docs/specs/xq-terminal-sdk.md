# Spec: xq-terminal-sdk

**Status:** Active — **Python** protocol package (code-level).

**Related:** [`xq-terminal.md`](xq-terminal.md) · [`xq-terminal-registry.md`](xq-terminal-registry.md) · [`xq-terminal-spec-wheel.md`](xq-terminal-spec-wheel.md)

**Ticket:** [#24](https://github.com/ExperienceQuality/xq-hub/issues/24)

## Problem

Specs and the registry need one shared Python contract without CLI/passport logic in the SDK.

## Solution

Satellite `ExperienceQuality/xq-terminal-sdk` — installable package **`xq-terminal-sdk`** (import name `xq_terminal_sdk`).

| Layer | Choice |
| --- | --- |
| Python | **3.11+** |
| Layout | `src/` |
| Build | **uv** / hatchling or setuptools |
| Public API | dataclasses + `Protocol` (no Pydantic in sdk v1 — keep Spec deps light) |

### Tree

```
xq-terminal-sdk/
├── CONTEXT.md
├── README.md
├── pyproject.toml
├── src/xq_terminal_sdk/
│   ├── __init__.py          # re-exports public API
│   ├── context.py           # SpecContext, SpecResult
│   ├── protocol.py          # RunnerSpec
│   └── entrypoints.py       # ENTRY_POINT_GROUP constant
└── tests/
    └── test_protocol_shape.py
```

### `pyproject.toml` (normative shape)

```toml
[project]
name = "xq-terminal-sdk"
version = "0.1.0"
description = "RunnerSpec protocol for xq-terminal Specs"
requires-python = ">=3.11"
dependencies = []

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.hatch.build.targets.wheel]
packages = ["src/xq_terminal_sdk"]
```

### Code — `context.py`

```python
from __future__ import annotations
from dataclasses import dataclass

@dataclass(frozen=True, slots=True)
class SpecContext:
    environment: str
    run_id: str
    asset: str
    sha: str
    gate: str                 # "merge" | "release" (release for Spec runs in v1)
    artifact_ref: str | None

@dataclass(frozen=True, slots=True)
class SpecResult:
    success: bool
    message: str
```

### Code — `protocol.py`

```python
from __future__ import annotations
from typing import Protocol, runtime_checkable
from xq_terminal_sdk.context import SpecContext, SpecResult

@runtime_checkable
class RunnerSpec(Protocol):
    def name(self) -> str: ...
    def run(self, context: SpecContext) -> SpecResult: ...
```

### Code — `entrypoints.py`

```python
ENTRY_POINT_GROUP = "xq_terminal.specs"
# Spec wheels MUST register under this group; name = Spec id (e.g. "payment-spec")
```

### Code — `__init__.py`

```python
from xq_terminal_sdk.context import SpecContext, SpecResult
from xq_terminal_sdk.protocol import RunnerSpec
from xq_terminal_sdk.entrypoints import ENTRY_POINT_GROUP

__all__ = ["SpecContext", "SpecResult", "RunnerSpec", "ENTRY_POINT_GROUP"]
```

### Spec wheel contract (consumers)

```toml
# in payment-spec/pyproject.toml
dependencies = ["xq-terminal-sdk>=0.1,<0.2"]

[project.entry-points."xq_terminal.specs"]
payment-spec = "payment_spec:load_spec"
```

```python
# payment_spec/__init__.py
from xq_terminal_sdk import RunnerSpec, SpecContext, SpecResult

class PaymentSpec:
    def name(self) -> str:
        return "payment-spec"

    def run(self, context: SpecContext) -> SpecResult:
        ...

def load_spec() -> RunnerSpec:
    return PaymentSpec()
```

`load_spec` must be a zero-arg callable returning an object satisfying `RunnerSpec`.

## Out of scope

- YAML registry, Terminal CLI, sandbox, Pydantic passport models  
- Java  

## Acceptance

- [ ] Package installs; `from xq_terminal_sdk import RunnerSpec, SpecContext, SpecResult, ENTRY_POINT_GROUP`  
- [ ] `runtime_checkable` works for isinstance in tests  
- [ ] README documents entry-point group + `load_spec`  
- [ ] Java sources removed or archived under `archive/java/`  
