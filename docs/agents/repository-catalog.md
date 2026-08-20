# XQ Repository Catalog

[`repository-context.yaml`](../../repository-context.yaml) is the canonical,
machine-readable registry of GitHub repositories known to this hub. Read it
before routing XQ-org work. This catalog is the delivery-ready view: a repository
may appear in the YAML as `reference-only` for discovery, but must not be routed
until its current owner, validation command, environment, and dependencies are
known.

| Repository | Purpose | Owner | Required validation | Deployment environment | Dependencies |
| --- | --- | --- | --- | --- | --- |
| `ExperienceQuality/xq-hub` | Cross-repository delivery coordination | XQ delivery lead | Markdown/link validation | None | Target XQ repositories |
| `ExperienceQuality/jvm-test-kit` | Single-package JVM service-test kit with JUnit Jupiter support and GitHub Packages publication | `@chauhaidang` | `./gradlew clean check` | GitHub Actions release workflow to GitHub Packages Maven registry | JDK 21, Gradle Wrapper 9.6, JUnit Jupiter, GitHub Actions, GitHub Packages Maven registry, Docker-compatible runner and approved PostgreSQL image for the PostgreSQL fixture, `io.swagger.parser.v3:swagger-parser` for OpenAPI 3.0 loading |

For every target repository, record its canonical repository name, purpose,
maintainer, required CI/test command, deployment environment, and material
dependencies before creating an agent-ready work item.
