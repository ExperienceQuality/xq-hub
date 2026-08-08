# Spec: xq-terminal-sdk

**Status:** Active — **Python** protocol package for Terminal Specs.

**Related:** [`docs/specs/xq-terminal.md`](xq-terminal.md) · [`docs/specs/xq-terminal-registry.md`](xq-terminal-registry.md)

**Pivot:** Java/Gradle API descoped. Satellite `ExperienceQuality/xq-terminal-sdk` is rewritten as a **Python** library (or replaced by a Python package of the same product name).

## Problem

Terminal Specs and the registry need a shared **Python** contract (`RunnerSpec`, context, result) without embedding passport/CLI logic in Spec wheels.

## Solution

Publish **`xq-terminal-sdk`** as a Python package (uv/`pyproject.toml`):

| Layer | Choice |
| --- | --- |
| Language | **Python 3.11+** |
| API | Protocol / dataclasses (or Pydantic models if shared validation helps) |
| Consumers | Spec wheels (`dependencies`); registry may re-export; Terminal optional |

### API (v1)

```python
from typing import Protocol
from dataclasses import dataclass

@dataclass(frozen=True)
class SpecContext:
    environment: str
    run_id: str
    asset: str
    sha: str
    gate: str
    artifact_ref: str | None

@dataclass(frozen=True)
class SpecResult:
    success: bool
    message: str

class RunnerSpec(Protocol):
    def name(self) -> str: ...
    def run(self, context: SpecContext) -> SpecResult: ...
```

Specs expose an entry point factory, e.g.:

```toml
[project.entry-points."xq_terminal.specs"]
payment-spec = "payment_spec:load_spec"
```

## Layout

```
xq-terminal-sdk/
├── CONTEXT.md
├── README.md
├── pyproject.toml
└── src/xq_terminal_sdk/
    ├── __init__.py
    ├── context.py
    └── protocol.py
```

## Out of scope

- Terminal CLI, registry YAML, sandbox  
- Java sources (archive/remove on rewrite)  

## Acceptance

- [ ] Python package builds (`uv build` / pip install)  
- [ ] Specs can depend on it and implement `RunnerSpec`  
- [ ] Docs: entry-point group `xq_terminal.specs`  
- [ ] Catalogue purpose line = Python protocol (not Java)  
- [x] Java bootstrap (#23) recorded as superseded by Python pivot  

## Tracer Tickets

1. [#24](https://github.com/ExperienceQuality/xq-hub/issues/24) — Rewrite Satellite to Python protocol  
2. Example Spec (#22) + registry (#25) consume this package  
