# Spec: xq-terminal

**Status:** Active — **Python-only**, code-level plan for CLI in `xq-qe-box`.

**Related:** [`xq-terminal-sdk.md`](xq-terminal-sdk.md) · [`xq-terminal-registry.md`](xq-terminal-registry.md) · [`xq-terminal-spec-wheel.md`](xq-terminal-spec-wheel.md) · [`xq-qe-box.md`](xq-qe-box.md)

## Problem

Admission gate: **merge** / **release** from passport + (release) sandbox Spec — without Spec deps on the Runner.

## Solution

Python CLI package **`xq-terminal`** under `xq-qe-box/cli/xq-terminal/`.

| Layer | Choice |
| --- | --- |
| Python | **3.11+** / **uv** |
| CLI | **Typer** → services (no logic in CLI module) |
| Models | **Pydantic v2** |
| Specs | `xq-terminal-registry` only (`get_spec`) |
| Sandbox | Port + stub adapter in `packages/sandbox` or `xq_terminal/adapters` |

### Tree

```
xq-qe-box/cli/xq-terminal/
├── pyproject.toml
├── README.md
├── src/xq_terminal/
│   ├── __init__.py
│   ├── __main__.py              # python -m xq_terminal
│   ├── cli.py                   # Typer app only
│   ├── models/
│   │   ├── passport.py          # Pydantic Passport
│   │   └── board.py             # BoardResult
│   ├── services/
│   │   ├── board.py             # BoardService
│   │   └── passport.py          # load + validate + accounting
│   └── adapters/
│       ├── passport_fs.py       # path/dir → Passport
│       ├── sandbox.py           # SandboxPort + StubSandbox
│       └── registry_specs.py    # thin wrap get_spec
└── tests/
    ├── fixtures/
    │   ├── passport_ok.json
    │   ├── passport_fail.json
    │   ├── passport_quarantine.json
    │   └── passport_bad_accounting.json
    ├── test_merge.py
    └── test_release.py
```

### `pyproject.toml`

```toml
[project]
name = "xq-terminal"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
  "typer>=0.12",
  "pydantic>=2",
  "xq-terminal-registry==0.1.0",   # pin; pulls Specs transitively
]
# FORBIDDEN: login-spec, payment-spec as direct deps

[project.scripts]
xq-terminal = "xq_terminal.cli:app"

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"
```

---

## Models (code-level)

### `models/passport.py`

```python
from __future__ import annotations
from datetime import datetime
from typing import Literal
from pydantic import BaseModel, Field, model_validator

class SuiteResult(BaseModel):
    name: str
    size: Literal["small", "medium", "large"]
    dryRunTotal: int = Field(ge=0)
    passed: int = Field(ge=0)
    failed: int = Field(ge=0)
    quarantined: int = Field(ge=0)
    status: Literal["passed", "failed", "quarantined"]
    owner: str | None = None
    expiresAt: datetime | None = None

    @model_validator(mode="after")
    def suite_accounting(self) -> SuiteResult:
        if self.passed + self.failed + self.quarantined != self.dryRunTotal:
            raise ValueError("suite accounting mismatch")
        if self.status == "quarantined" and (not self.owner or not self.expiresAt):
            raise ValueError("quarantined suite needs owner + expiresAt")
        return self

class Counts(BaseModel):
    dryRunTotal: int = Field(ge=0)
    passed: int = Field(ge=0)
    failed: int = Field(ge=0)
    quarantined: int = Field(ge=0)

class Coverage(BaseModel):
    passRatio: float

class Passport(BaseModel):
    asset: str
    sha: str
    gate: Literal["merge", "release"]
    generatedAt: datetime | None = None
    counts: Counts
    coverage: Coverage | None = None
    suites: list[SuiteResult] = []

    @model_validator(mode="after")
    def root_accounting(self) -> Passport:
        c = self.counts
        if c.passed + c.failed + c.quarantined != c.dryRunTotal:
            raise ValueError("passport accounting mismatch")
        expected = c.passed / c.dryRunTotal if c.dryRunTotal else 0.0
        if self.coverage is None:
            self.coverage = Coverage(passRatio=expected)
        return self

    @property
    def pass_ratio(self) -> float:
        c = self.counts
        return 0.0 if c.dryRunTotal == 0 else c.passed / c.dryRunTotal
```

### `models/board.py`

```python
from pydantic import BaseModel

class SpecOutcome(BaseModel):
    id: str
    success: bool
    message: str

class BoardResult(BaseModel):
    gate: str
    asset: str
    sha: str
    qualified: bool
    passRatio: float
    reasons: list[str]
    spec: SpecOutcome | None = None
```

---

## Services (code-level)

### `services/passport.py`

```python
def load_passport(path: Path) -> Passport:
    """If path is dir, use path / 'passport.json'."""
    ...

def merge_qualified(passport: Passport) -> tuple[bool, list[str]]:
    if passport.counts.failed == 0:
        return True, ["passport failed==0"]
    return False, ["passport not clear: failed > 0"]
```

### `services/board.py`

```python
class BoardService:
    def __init__(self, sandbox: SandboxPort, specs: SpecPort):
        self._sandbox = sandbox
        self._specs = specs

    def board_merge(self, *, asset: str, sha: str, reports: Path) -> BoardResult:
        passport = load_passport(reports)
        self._assert_identity(passport, asset, sha)
        ok, reasons = merge_qualified(passport)
        return BoardResult(
            gate="merge", asset=asset, sha=sha,
            qualified=ok, passRatio=passport.pass_ratio, reasons=reasons,
        )

    def board_release(
        self, *, asset: str, sha: str, reports: Path,
        artifact: str, spec_id: str,
    ) -> BoardResult:
        passport = load_passport(reports)
        self._assert_identity(passport, asset, sha)
        ok, reasons = merge_qualified(passport)
        if not ok:
            return BoardResult(
                gate="release", asset=asset, sha=sha,
                qualified=False, passRatio=passport.pass_ratio,
                reasons=["passport blocked release", *reasons],
            )
        handle = self._sandbox.provision(artifact=artifact, asset=asset)
        try:
            from xq_terminal_sdk import SpecContext
            ctx = SpecContext(
                environment=handle.environment,
                run_id=handle.run_id,
                asset=asset,
                sha=sha,
                gate="release",
                artifact_ref=artifact,
            )
            spec = self._specs.get(spec_id)
            result = spec.run(ctx)
            return BoardResult(
                gate="release", asset=asset, sha=sha,
                qualified=result.success,
                passRatio=passport.pass_ratio,
                reasons=["passport + spec ok"] if result.success else ["spec failed"],
                spec=SpecOutcome(id=spec.name(), success=result.success, message=result.message),
            )
        finally:
            self._sandbox.teardown(handle)
```

### Adapters

```python
# adapters/registry_specs.py
from xq_terminal_registry import get_spec

class RegistrySpecPort:
    def get(self, spec_id: str):
        return get_spec(spec_id)

# adapters/sandbox.py
@dataclass
class SandboxHandle:
    environment: str
    run_id: str

class SandboxPort(Protocol):
    def provision(self, *, artifact: str, asset: str) -> SandboxHandle: ...
    def teardown(self, handle: SandboxHandle) -> None: ...

class StubSandbox:
    def provision(self, *, artifact: str, asset: str) -> SandboxHandle:
        return SandboxHandle(environment="stub", run_id="stub-1")
    def teardown(self, handle: SandboxHandle) -> None:
        return None
```

---

## CLI (code-level)

```python
# cli.py
import json, sys
import typer
from pathlib import Path
from xq_terminal.services.board import BoardService
from xq_terminal.adapters.sandbox import StubSandbox
from xq_terminal.adapters.registry_specs import RegistrySpecPort

app = typer.Typer(add_completion=False)

@app.command("board")
def board(
    asset: str = typer.Option(...),
    gate: str = typer.Option(..., help="merge|release"),
    sha: str = typer.Option(...),
    reports: Path = typer.Option(...),
    artifact: str | None = typer.Option(None),
    spec: str | None = typer.Option(None, help="registry Spec id"),
) -> None:
    svc = BoardService(StubSandbox(), RegistrySpecPort())
    if gate == "merge":
        result = svc.board_merge(asset=asset, sha=sha, reports=reports)
    elif gate == "release":
        if not artifact or not spec:
            raise typer.BadParameter("release requires --artifact and --spec")
        result = svc.board_release(
            asset=asset, sha=sha, reports=reports,
            artifact=artifact, spec_id=spec,
        )
    else:
        raise typer.BadParameter("gate must be merge|release")
    typer.echo(result.model_dump_json(indent=2))
    raise SystemExit(0 if result.qualified else 1)

def main() -> None:
    app()

# for [project.scripts]
# typer apps: app() is invoked as console script target — use callback pattern if needed
```

### CLI contract

```bash
xq-terminal board \
  --asset demo-app \
  --gate merge \
  --sha abc123 \
  --reports dist/quality/passport.json

xq-terminal board \
  --asset demo-app \
  --gate release \
  --sha abc123 \
  --reports dist/quality/passport.json \
  --artifact dist/app.ipa \
  --spec payment-spec
```

Env prefix: `XQ_TERMINAL_*` (reserved; unused in v1 beyond future config).

### Stdout JSON

```json
{
  "gate": "release",
  "asset": "demo-app",
  "sha": "abc123",
  "qualified": true,
  "passRatio": 0.9,
  "reasons": ["passport + spec ok"],
  "spec": { "id": "payment-spec", "success": true, "message": "payment checks ok" }
}
```

Exit: `0` qualified, `1` not qualified, `2` usage/validation error.

---

## Product gates

| Gate | Code path | Decision |
| --- | --- | --- |
| merge | `board_merge` | `counts.failed == 0` |
| release | passport then sandbox + `get_spec(id).run` | both green |

`passRatio` informational only.

---

## Skills

```bash
gh skill install ExperienceQuality/xq-qe-box xq-terminal
```

## Out of scope (v1)

- JVM / `--spec-url` / sha256 ClassLoader  
- Soft-qualify on passRatio  
- Real sandbox (#21)  
- Direct Spec deps on Terminal  

## Acceptance

- [ ] Merge fixtures green (#17)  
- [ ] Release via registry `#19` + example Spec `#22` + registry `#25`  
- [ ] `pyproject` has no Spec wheel deps  
- [ ] Skill (#18)  

## Tracer Tickets

1. [#24](https://github.com/ExperienceQuality/xq-hub/issues/24) — sdk  
2. [#17](https://github.com/ExperienceQuality/xq-hub/issues/17) — merge  
3. [#25](https://github.com/ExperienceQuality/xq-hub/issues/25) — registry  
4. [#22](https://github.com/ExperienceQuality/xq-hub/issues/22) — Spec wheel  
5. [#19](https://github.com/ExperienceQuality/xq-hub/issues/19) — release  
6. [#18](https://github.com/ExperienceQuality/xq-hub/issues/18) — skill  
7. [#20](https://github.com/ExperienceQuality/xq-hub/issues/20) — passport CI  
8. [#21](https://github.com/ExperienceQuality/xq-hub/issues/21) — real sandbox  
