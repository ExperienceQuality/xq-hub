# Karate Test Framework v2

**Status:** research recommendation, not an adoption decision  
**Audience:** XQ-org test-platform / Java backend teams  
**Research date:** 2026-08-20  
**Source policy:** official docs, GitHub repo, release notes, license, first-party pages only.

## Recommendation in brief

Do **not** add Karate 2.x to `ExperienceQuality/jvm-test-kit` or any XQ product codebase on the strength of this note. Keep the current XQ service-test stack: **JUnit Jupiter**, the in-house **jvm-test-kit** HTTP / OpenAPI / Postgres fixtures, **JDK 21**, **Gradle Wrapper 9.6**, **Testcontainers-backed PostgreSQL**, and **`io.swagger.parser.v3:swagger-parser`** for an OpenAPI 3.0 subset. Treat **REST Assured** as an optional Java HTTP DSL only where a live-server suite already benefits from it, consistent with the earlier backend-stack note.

Karate 2.x is a capable, MIT-licensed, JVM-native HTTP/Gherkin framework with a new 2.x line that already matches XQ’s Java 21 baseline. It is still a **second authoring model** (plain-text `.feature` files plus an embedded JavaScript engine), not a drop-in library for a single-package JUnit Jupiter kit. OpenAPI-to-test conversion, IDE debugging, and several protocol add-ons sit on the commercial side of Karate Labs’ product split.

This is a research recommendation, not an adoption decision. No prototype was created and no Karate dependency was added to a product repository.

## Stated need and constraints

The question is whether Karate Labs **Karate v2** (Karate 2.x) should be considered as an XQ-org test tool. Constraints taken from the hub registry, not inferred from Karate:

| Constraint | Source |
| --- | --- |
| Target kit is a **single-package** JVM service-test library with **JUnit Jupiter** support | [`repository-context.yaml`](../../repository-context.yaml), [repository catalog](../agents/repository-catalog.md) |
| Validation command `./gradlew clean check`; **Gradle Wrapper 9.6** | same |
| Runtime/build: **JDK 21** | same |
| Postgres via Docker-compatible runner and an approved PostgreSQL image | same |
| OpenAPI 3.0 loading via `io.swagger.parser.v3:swagger-parser` (2.1.x pin at implement time) | same |
| This research must not adopt, install, or prototype Karate in a product codebase | task instruction; XQ tool-evaluation workflow is research-only until a human selects an option |

Quality signal for this evaluation: **compatibility and operating cost** versus the existing kit, then CI/Docker determinism. Popularity is not used as evidence.

## What Karate 2.x is

Karate Labs describes Karate as an **open-source** tool that combines API test automation, mocks, performance testing, and UI automation in one framework. Tests are written in **plain text using Gherkin**. Unlike Cucumber, Karate does not require separate Java step-definition glue.
[Karate FAQ — What is Karate?](https://docs.karatelabs.io/faq/)
[Feature files](https://docs.karatelabs.io/core-syntax/feature-files)

The **open-source engine** is the GitHub repository [karatelabs/karate](https://github.com/karatelabs/karate). The README states that Karate is “the open-source tool that combines API testing, mocks, performance testing, and UI automation into a single, unified framework,” and points usage docs at [docs.karatelabs.io](https://docs.karatelabs.io/).
[karatelabs/karate README](https://github.com/karatelabs/karate/blob/main/README.md)

Karate Labs Inc. is the commercial vendor. First-party pages distinguish an **open-source framework** from paid products: Karate Xplorer (desktop API client), Karate Agent (AI-native / LLM browser testing), IDE plugins, and premium protocol extensions. The company site describes an “open-source engine” plus enterprise platform features (audit logs, offline licensing, dedicated support).
[What’s New in v2 — Commercial & Enterprise](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Karate Labs home](https://karatelabs.io/)
[FAQ — telemetry and products](https://docs.karatelabs.io/faq/)

### JVM vs standalone

Karate v2 is a **JVM framework**. Official install paths are:

1. VS Code extension (installs CLI and Java).
2. **Karate CLI** (`karate.sh` / [karatelabs/karate-cli](https://github.com/karatelabs/karate-cli)) — can install a JRE and the Karate JAR; Java 21+ is required.
3. **Maven** test dependency (`io.karatelabs:karate-core` or `karate-junit6`).
4. **Gradle** test dependency plus `useJUnitPlatform()`.
5. **Standalone fat JAR** (`java -jar karate-<version>.jar`), Java 21+ only.

[Install & Get Started](https://docs.karatelabs.io/getting-started/install-dependencies)
[Karate CLI README](https://github.com/karatelabs/karate-cli/blob/main/README.md)
[Standalone execution](https://docs.karatelabs.io/getting-started/standalone-execution/)

The Java API can also embed HTTP/JSON/`Match` calls without `.feature` files, using `io.karatelabs:karate-core`.
[Java API — Java DSL](https://docs.karatelabs.io/advanced/java-api)

### BDD/Gherkin vs Java API

Primary authoring is **Gherkin `.feature` files** with built-in HTTP keywords (`url`, `path`, `request`, `method`, `status`, `match`). Teams may use Given/When/Then or a technical `*` style.
[Making requests](https://docs.karatelabs.io/http-requests/making-requests)
[Feature files](https://docs.karatelabs.io/core-syntax/feature-files)

Java is the **runner and interop** surface: `io.karatelabs.junit6.Karate` / `@Karate.Test`, `io.karatelabs.core.Runner`, `Java.type()` for calling Java from features.
[JUnit](https://docs.karatelabs.io/running-tests/junit)
[Command line — JUnit 6 runner](https://docs.karatelabs.io/running-tests/command-line/)
[Java API](https://docs.karatelabs.io/advanced/java-api)

**Inference (not a vendor claim):** for XQ, Karate would be a Gherkin-first suite sitting beside, not inside, the existing JUnit Jupiter Java API of jvm-test-kit.

## Current 2.x line, dates, Java baseline, license

### Releases

GitHub Releases for [karatelabs/karate](https://github.com/karatelabs/karate/releases) (UTC, non-prerelease unless noted):

| Tag | Published |
| --- | --- |
| `v2.0.0.RC2` (prerelease) | 2026-03-24 |
| **`v2.0.0`** | **2026-03-26** |
| `v2.0.1` … `v2.0.10` | 2026-04-07 through 2026-06-04 |
| `v2.1.0` | 2026-06-17 |
| `v2.1.1` | 2026-07-15 |
| **`v2.1.2`** (latest GitHub release at research date) | **2026-08-14** |

[v2.0.0 release notes](https://github.com/karatelabs/karate/releases/tag/v2.0.0)
[GitHub Releases list](https://github.com/karatelabs/karate/releases)

`v2.0.0` is described as the first major v2 release: a ground-up rewrite on Java 21+ virtual threads with a custom JavaScript engine.
[v2.0.0 release notes](https://github.com/karatelabs/karate/releases/tag/v2.0.0)

Maven Central currently presents **`io.karatelabs:karate-core:2.1.2`** and **`io.karatelabs:karate-junit6:2.1.2`** as the coordinates on the artifact pages (age listed as six days before this research date, consistent with the 2026-08-14 GitHub tag).
[Maven Central karate-core](https://central.sonatype.com/artifact/io.karatelabs/karate-core)
[Maven Central karate-junit6](https://central.sonatype.com/artifact/io.karatelabs/karate-junit6)

The repository `pom.xml` on `main` at research time is **`io.karatelabs:karate-parent:2.1.3.RC1`** (release candidate of the next patch/minor, not treated here as the latest *released* line).
[karate pom.xml on main](https://github.com/karatelabs/karate/blob/main/pom.xml)

Official docs are not fully version-aligned: What’s New in v2 shows `karate-junit6` **2.1.2**; the Maven install path shows `karate-core` **2.1.2**; the Gradle install snippet still shows **2.0.0**; the migration guide still shows **2.0.0** plus `junit-jupiter` **5.10.1**.
[What’s New in v2](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Install](https://docs.karatelabs.io/getting-started/install-dependencies)
[Migration from v1](https://docs.karatelabs.io/getting-started/migration-from-v1/)

### Java baseline

Karate v2 **requires Java 21+** for virtual threads. Docs state there is no GraalVM dependency and that it works on any Java 21+ JVM.
[What’s New in v2](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Install](https://docs.karatelabs.io/getting-started/install-dependencies)
[Migration from v1](https://docs.karatelabs.io/getting-started/migration-from-v1/)

The parent POM sets `<java.release.version>21</java.release.version>` and `<java.compiler.version>24</java.compiler.version>` (compiler used to *build* Karate, not a user requirement stated in the install guide).
[karate pom.xml on main](https://github.com/karatelabs/karate/blob/main/pom.xml)

The FAQ still lists older lines (Karate 1.4.x+: Java 17+; earlier 1.x: Java 8+). That table is about **1.x**, not a waiver of the v2 Java 21 requirement.
[FAQ — Java version](https://docs.karatelabs.io/faq/)

The Gradle install snippet includes `jvmArgs '--enable-preview'`. No matching requirement appears in the Maven path, the Java 21+ statements, or the parent POM’s `java.release.version`. Treat **`--enable-preview` as an unverified docs leftover**, not a confirmed user requirement.
[Install — Path 4 Gradle](https://docs.karatelabs.io/getting-started/install-dependencies)

### License

The GitHub `LICENSE` file is the **MIT License**, Copyright (c) 2025 Karate Labs.
[LICENSE](https://github.com/karatelabs/karate/blob/main/LICENSE)

Maven Central metadata for `karate-core` and `karate-junit6` also lists **MIT License**. The parent POM declares the same.
[Maven Central karate-core](https://central.sonatype.com/artifact/io.karatelabs/karate-core)
[pom.xml](https://github.com/karatelabs/karate/blob/main/pom.xml)

Karate Labs’ **IDE plugins and protocol add-ons are not the MIT core**. IntelliJ plugin use is under the Karate Labs EULA; Kafka add-on runtime requires a Karate Labs license file.
[IntelliJ plugin](https://docs.karatelabs.io/ide-support/intellij)
[karate-kafka README](https://github.com/karatelabs/karate-addons/blob/main/karate-kafka/README.md)

Security support: **2.x supported, 1.x not supported**.
[SECURITY.md](https://github.com/karatelabs/karate/blob/main/SECURITY.md)

## Maven / Gradle coordinates and how tests are run

### Coordinates

| Artifact | Group | Typical use |
| --- | --- | --- |
| `karate-core` | `io.karatelabs` | Core engine, HTTP client/server, CLI fatjar, Java DSL |
| `karate-junit6` | `io.karatelabs` | JUnit 6 / `@TestFactory` integration; depends on `karate-core`; `junit-jupiter-api` is **provided** |
| `karate-js` | `io.karatelabs` | Embedded JS engine (pulled by `karate-core`) |
| `karate-gatling` | `io.karatelabs` | Optional Gatling performance module |
| `karate-image` | `io.karatelabs` | Image-comparison extension |

[Maven Central karate-core POM excerpt](https://central.sonatype.com/artifact/io.karatelabs/karate-core)
[Maven Central karate-junit6 POM excerpt](https://central.sonatype.com/artifact/io.karatelabs/karate-junit6)
[What’s New — JUnit 6](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Performance testing](https://docs.karatelabs.io/extensions/performance-testing/)

**There is no first-party Karate Gradle plugin documented for v2.** Gradle integration is: add the Maven coordinate as `testImplementation`, enable `useJUnitPlatform()`, forward `karate.*` system properties, run `./gradlew test`.
[Install — Gradle](https://docs.karatelabs.io/getting-started/install-dependencies)
[Command line — Gradle](https://docs.karatelabs.io/running-tests/command-line/)

`karate-junit6` does **not** bundle JUnit. Callers must add `junit-jupiter` (or equivalent) themselves. The migration guide’s example still pins `junit-jupiter` 5.10.1, while the Karate parent POM on `main` uses `<junit.version>6.1.3</junit.version>`. Current JUnit documentation describes **JUnit 6.1.3 = Platform + Jupiter + Vintage**, with Vintage deprecated, and Java 17+ at runtime.
[Migration from v1](https://docs.karatelabs.io/getting-started/migration-from-v1/)
[pom.xml](https://github.com/karatelabs/karate/blob/main/pom.xml)
[JUnit overview](https://docs.junit.org/current/user-guide/)

### Runners

| Entry point | First-party description |
| --- | --- |
| JUnit `@Karate.Test` / `Karate.run(...)` returning `Iterable<DynamicNode>` | Dynamic tests via `@TestFactory`; IDE tree; `mvn test` / `./gradlew test` |
| `Runner.path(...).parallel(n)` → `SuiteResult` | Programmatic / CI parallel runner |
| `karate run` CLI | No JUnit class required; flags for tags, threads, env, report formats |
| Standalone JAR | Same CLI grammar via `java -jar` |
| System properties | `karate.options`, `karate.env`, `KARATE_OPTIONS`, `KARATE_ENV` override builder defaults |

[JUnit](https://docs.karatelabs.io/running-tests/junit)
[Command line](https://docs.karatelabs.io/running-tests/command-line/)
[Java API — Runner](https://docs.karatelabs.io/advanced/java-api)

CI reports: HTML under `target/karate-reports/`, optional JUnit XML and Cucumber JSON.
[JUnit — CI reports](https://docs.karatelabs.io/running-tests/junit)

## Capabilities relevant to XQ

### HTTP API testing — verified

Native REST (and GraphQL, SOAP) via Gherkin keywords; Apache HttpClient 5.6 in v2; declarative `configure auth` for Basic, Bearer, OAuth2, NTLM.
[Making requests](https://docs.karatelabs.io/http-requests/making-requests)
[GraphQL](https://docs.karatelabs.io/http-requests/graphql)
[What’s New — HTTP client and auth](https://docs.karatelabs.io/getting-started/whats-new-v2/)

Payload assertions use `match` and Karate’s own schema markers (`#string`, `#number`, `#[]`, optional `##`, reusable schema objects). The schema page states this is “simpler … than JSON-schema, with zero dependencies,” not OpenAPI document validation.
[Schema validation](https://docs.karatelabs.io/assertions/schema-validation)

Mocks: rewritten Netty HTTP mock server; docs describe mocks for API testing and **consumer-driven contracts** without an external broker.
[What’s New — mock server](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Test doubles](https://docs.karatelabs.io/extensions/test-doubles)

### OpenAPI / contract checking — mixed; do not over-claim

**Verified in OSS Karate:**

- Response/request **payload schema** via Karate `match` markers and JSON files (`read('…schema.json')`), not via swagger-parser.
  [Schema validation](https://docs.karatelabs.io/assertions/schema-validation)
- Mocks that can be compared to a real provider (vendor wording: contract testing without a broker).
  [Karate Labs home — API mocks & contract testing](https://karatelabs.io/)
- Dry-run coverage docs mention an `@cov=openapi:` tag in the context of the **Karate Agent coverage extension**, not as a swagger-parser based checker in `karate-core`.
  [Command line — dry run coverage](https://docs.karatelabs.io/running-tests/command-line/)

**Verified as commercial IDE, not core OSS:**

- IntelliJ **ENTERPRISE** license: import OpenAPI/Swagger of any version, convert specs to tests and mocks, payload subset chooser, spec impact analysis.
  [IntelliJ — OpenAPI / Swagger (ENTERPRISE)](https://docs.karatelabs.io/ide-support/intellij)
- Xplorer **Premium**: OpenAPI “Try It”.
  [Karate Xplorer](https://karatelabs.io/xplorer)

**Not evidenced:** that Karate 2.x OSS loads OpenAPI 3.0 with `swagger-parser`, enforces XQ’s OpenAPI 3.0 subset, or replaces jvm-test-kit’s document-loading path.

### Wait / polling — verified in v2 FAQ; detailed page missing at research time

The v2 docs index lists a **Polling** topic under HTTP requests and Advanced, but those slugs returned HTTP 404 when fetched on 2026-08-20 (`/http-requests/polling`, `/advanced/polling`, `/advanced/db`).

The **v2 FAQ** still documents `retry until` for intermittent HTTP, e.g. `* retry until responseStatus == 200` plus `configure readTimeout`.
[FAQ — intermittent failures](https://docs.karatelabs.io/faq/)

The v1 README (preserved on the `v1.5.2` tag) specifies `retry until` as HTTP retry-before-`method`, default `{ count: 3, interval: 3000 }`. That is **1.x documentation**; v2 feature-file compatibility claims “most feature files work unchanged,” with listed exceptions that do **not** include removal of `retry until`.
[v1.5.2 README — retry until](https://github.com/karatelabs/karate/blob/v1.5.2/README.md)
[Migration — feature file compatibility](https://docs.karatelabs.io/getting-started/migration-from-v1/)

UI auto-wait is a separate, documented v2 browser feature and is not an HTTP poller.
[What’s New — auto-wait](https://docs.karatelabs.io/getting-started/whats-new-v2/)

### Database / Postgres — Java interop, not a first-class Postgres fixture

v2 FAQ: databases are accessed **through Java interop** or by calling HTTP endpoints that query the database. The example is an HTTP `/db` query API, not JDBC in the DSL.
[FAQ — databases](https://docs.karatelabs.io/faq/)

The first-party example [karatelabs/karate-examples database](https://github.com/karatelabs/karate-examples/blob/main/database/README.md) is a Spring Boot project: Karate via `@SpringBootTest`, JDBC via a one-time `DbUtils` Java class and Spring JDBC, `Java.type` from `karate-config.js`. It is a pattern, not a bundled Postgres module, and it does not specify PostgreSQL 16 or Testcontainers.

v2 parallel control includes `@lock=database` as an *example lock name* for serializing contended scenarios. That is a concurrency primitive, not a database driver.
[What’s New — @lock](https://docs.karatelabs.io/getting-started/whats-new-v2/)

**Not evidenced:** a Karate-owned Testcontainers PostgreSQL 16 fixture comparable to XQ’s kit constraint.

### CI determinism and Docker

Documented CI patterns (GitHub Actions reference on karate-todo): Java 21 Temurin, `./mvnw verify`, upload `target/karate-reports`, tag-filter `@external` / `@todo` out of CI, JUnit XML, secret-scan of HTML reports, `--no-color` auto in non-TTY.
[CI/CD](https://docs.karatelabs.io/running-tests/ci-cd)
[Command line — no-color](https://docs.karatelabs.io/running-tests/command-line/)

The same CI page’s hybrid API+UI job uses **Testcontainers + headless Chrome**, `ContainerDriverProvider`, and host vs `host.docker.internal` URL splitting. That is **browser** containerization, not Postgres.
[CI/CD](https://docs.karatelabs.io/running-tests/ci-cd)

Parallelism uses Java 21 virtual threads; v2 replaces `@parallel=false` (silently ignored) with `@lock`. Unmigrated `@parallel=false` therefore **runs in parallel**.
[Migration — parallel](https://docs.karatelabs.io/getting-started/migration-from-v1/)

`karate.callSingle()` is documented to run expensive setup once across parallel threads (auth, seed).
[Java API — callSingle](https://docs.karatelabs.io/advanced/java-api)

Feature-file guidance: scenarios must be independent; Background resets per scenario; parallel order is not guaranteed.
[Feature files](https://docs.karatelabs.io/core-syntax/feature-files)

v2.1.2 release notes include an OOM fix under parallel execution (embedded-expression deep-copy).
[v2.1.2 release notes](https://github.com/karatelabs/karate/releases)

**Inference:** Karate can run deterministically in CI if tests avoid wall-clock sleeps, pin env via `karate.env`, isolate data, and lock contended resources. The framework does not itself provide XQ’s Postgres image matrix. HTTP `retry until` can hide slowness or flakes if used as a correctness substitute (same class of risk as any poller).

## How v2 differs from 1.x (official notes)

Karate v2 is a **complete ground-up rewrite**. Official highlights:

| Area | v1 | v2 (official) |
| --- | --- | --- |
| JS engine | GraalJS (implied by “replaces GraalJS”) | Hand-rolled `karate-js`, ES6+, thread-safe, no GraalVM |
| Java | 1.4.x+: 17+ (FAQ) | **21+**, virtual threads |
| JUnit artifact | `karate-junit5` (bundled JUnit) | `karate-junit6` (JUnit **provided**) |
| Packages | `com.intuit.karate` | `io.karatelabs.*`; shims keep old packages |
| Parallel opt-out | `@parallel=false` | `@lock` / `@lock=*`; old tag **silently ignored** |
| HTTP client | (upgraded in v2 notes) | Apache HttpClient 5.6 |
| Auth | often `karate-config.js` headers | `configure auth` |
| Mocks | previous implementation | Rewrite: new JS engine + Netty |
| UI driver | WebDriver primary | CDP primary + W3C WebDriver; pooling; auto-wait |
| Gatling | Scala-era integration | Java-only DSL, package `io.karatelabs.gatling` |
| Logging | several `configure` keys + Java `HttpLogModifier` | single `configure logging` |
| Hooks | multiple interfaces | `RunListener` + `configure onStepFailure` |

[What’s New in v2](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[Migration from v1](https://docs.karatelabs.io/getting-started/migration-from-v1/)
[v2.0.0 release](https://github.com/karatelabs/karate/releases/tag/v2.0.0)

Shims: `com.intuit.karate.Runner`, `Results`, `MockServer`, `com.intuit.karate.junit5.Karate` delegate to v2 and are deprecated.
[Migration — shims](https://docs.karatelabs.io/getting-started/migration-from-v1/)

**Stale first-party conflict:** [README_V2.md](https://github.com/karatelabs/karate/blob/main/README_V2.md) still says v1 supports browser automation “and this will be eventually added to v2.” That is contradicted by the v2.0.0 release notes and current UI docs. Prefer the release notes and [What’s New in v2](https://docs.karatelabs.io/getting-started/whats-new-v2/).

v2.1.0 breaking change: mock servers no longer evaluate untrusted request data as code by default (`javaBridgeEnabled` / `requestExpressionsEnabled` opt-in) to close an RCE risk.
[v2.1.0 release notes](https://github.com/karatelabs/karate/releases)

## Maintenance, stability, commercial vs OSS

**Maintenance (verified):**

- Active 2.x line: 2.0.0 (2026-03-26) through 2.1.2 (2026-08-14), many patch releases.
- `SECURITY.md`: only 2.x supported.
- GitHub Actions badge on the main README.
- Parent POM still moving (`2.1.3.RC1` on `main`).

**Stability (verified facts, then inference):**

- Fact: v2 is explicitly a rewrite; migration lists silent `@parallel=false` ignore, Gatling session prefix break, logging key no-ops, and UI driver changes.
- Fact: 2.1.x notes still fix v2 regressions (e.g. `method` keyword variables, OOM under parallel).
- Inference: the line is **young and still hardening**. That is not a defect claim; it is a cost/risk input for adoption.

**OSS vs commercial (first-party):**

| Layer | License / access (first-party wording) |
| --- | --- |
| `karatelabs/karate` core, CLI, HTML reports | MIT / open-source |
| Karate CLI installer | MIT ([karate-cli README](https://github.com/karatelabs/karate-cli/blob/main/README.md)) |
| IDE syntax highlighting vs debug | Debug called a **paid** plugin feature in Why Karate; IntelliJ PLUS vs PRO vs ENTERPRISE tables |
| OpenAPI import / generate tests | IntelliJ **ENTERPRISE** |
| Karate Xplorer | Free core; Premium paid (OpenAPI Try It, runner) |
| Karate Agent | Enterprise / self-hosted BYO-LLM product |
| WebSocket testing | Listed as **commercial module** on What’s New in v2 |
| gRPC, Kafka, WebSocket | FAQ lists “premium protocol extensions”; kafka add-on requires runtime license |

[Why Karate — IDE](https://docs.karatelabs.io/getting-started/why-karate)
[What’s New — commercial](https://docs.karatelabs.io/getting-started/whats-new-v2/)
[FAQ — telemetry product list](https://docs.karatelabs.io/faq/)
[karate-kafka license](https://github.com/karatelabs/karate-addons/blob/main/karate-kafka/README.md)

Zero telemetry is claimed for OSS and premium products.
[FAQ — telemetry](https://docs.karatelabs.io/faq/)

## Fit versus XQ constraints

| XQ constraint | Karate 2.x fit | Evidence |
| --- | --- | --- |
| Single-package jvm-test-kit | Poor as a *replacement*. Karate is a multi-module Gherkin + JS engine + optional Gatling/image modules. Embedding it would change the kit’s authoring model and dependency surface (`karate-js`, HttpClient 5, Netty, json-smart, json-path, etc.). | [Maven Central karate-core POM](https://central.sonatype.com/artifact/io.karatelabs/karate-core); [repository-context.yaml](../../repository-context.yaml) |
| JUnit Jupiter | Partial. Runs on the JUnit Platform via `karate-junit6` and `junit-jupiter-api` (provided). Docs name **JUnit 6**; example pins still show Jupiter 5.10.1. XQ does not currently declare JUnit 6. | [Migration](https://docs.karatelabs.io/getting-started/migration-from-v1/); [JUnit user guide](https://docs.junit.org/current/user-guide/) |
| JDK 21 | Good. v2 requires 21+. | [Install](https://docs.karatelabs.io/getting-started/install-dependencies) |
| Gradle Wrapper 9.6 | Usable without a Karate plugin: `test { useJUnitPlatform() }`. Not validated against Gradle 9.6 in this research. | [Command line — Gradle](https://docs.karatelabs.io/running-tests/command-line/) |
| Testcontainers Postgres (approved image / PG 16 in XQ planning) | Not provided by Karate. DB access is Java interop / HTTP. Karate’s own Testcontainers usage in published CI is Chrome for UI. | [FAQ — databases](https://docs.karatelabs.io/faq/); [CI/CD](https://docs.karatelabs.io/running-tests/ci-cd); [Testcontainers Postgres module](https://java.testcontainers.org/modules/databases/postgres/) |
| swagger-parser OpenAPI 3.0 subset | Not the Karate OSS path. Spec-to-test is a paid IDE feature; OSS uses Karate `match` schemas. | [IntelliJ OpenAPI](https://docs.karatelabs.io/ide-support/intellij); [Schema validation](https://docs.karatelabs.io/assertions/schema-validation) |
| No production adoption in this research | Observed. No dependency or prototype added. | this note |

Karate’s own FAQ says Java-heavy teams comfortable with code should consider **REST Assured**.
[FAQ — comparison](https://docs.karatelabs.io/faq/)

REST Assured is a Java DSL for REST testing (Java 17+ as of 6.0.0 per its README news). The earlier XQ backend-stack note already classified it as **optional, not baseline**.
[rest-assured README](https://github.com/rest-assured/rest-assured/blob/master/README.md)
[Modern Java backend testing stack](./modern-java-backend-testing-stack.md)

## Options

### Option A — Stay on the current XQ stack (recommended)

**JUnit Jupiter + ExperienceQuality/jvm-test-kit** (HTTP client helpers, OpenAPI 3.0 loading via swagger-parser, Postgres Testcontainers fixture) **+ REST Assured only if a live HTTP DSL is justified**.

Pros:

- Matches the registered kit purpose, validation command, JDK, Gradle Wrapper, and OpenAPI loader.
- Keeps one Java/JUnit programming model; no Gherkin/JS engine in the shared kit.
- Postgres and OpenAPI remain explicit, version-pinned kit concerns.
- Aligns with the layered JUnit/AssertJ/Testcontainers recommendation already recorded for Java backends.
- MIT/Apache-style Java libraries already in the XQ plan; no Karate Labs EULA for core tests.

Cons:

- No Karate-style one-line JSON `match` DSL or built-in HTML step report.
- Non-programmers cannot author tests as `.feature` files without a second tool.
- HTTP polling/retry and mocks must stay in kit/Java (or a separately chosen mock library).

### Option B — Introduce Karate 2.x as an additional HTTP/BDD layer

Add `io.karatelabs:karate-junit6` (released 2.1.x) to a **separate** test source set or a throwaway prototype *after* a human adoption decision — not into the production kit during research.

Pros:

- Java 21 and Gradle-via-JUnit-Platform are officially supported shapes.
- Strong HTTP Gherkin, `match` schemas, mocks, parallel virtual threads, HTML reports, JUnit XML.
- MIT core; CLI/standalone available for non-Gradle experiments.
- Active 2.x releases and 1.x explicitly unsupported (so “stay on 1.x” is not a support path).

Cons:

- Second language (Gherkin + karate-js) in a single-package Jupiter kit.
- OpenAPI 3.0 subset and Postgres 16 fixtures are **not** Karate-owned; XQ would still maintain jvm-test-kit (or duplicate them via Java interop).
- JUnit 6 vs current Jupiter pin is unresolved; docs versions disagree.
- Rewrite-era regressions and silent `@parallel=false` behavior.
- IDE debug and OpenAPI generation are paid; Kafka/gRPC/WebSocket add-ons are licensed.
- Fat dependency tree (HttpClient 5, Netty, custom JS) versus a thin kit.

### Option C — Not recommended as a third baseline: Karate 1.x

1.x is **unsupported** per `SECURITY.md`. Java 17+ (1.4.x) also mismatches the v2-only support policy. Do not start new XQ work on 1.x.

## Comparison

| Criterion | Option A: jvm-test-kit + JUnit Jupiter (+ optional REST Assured) | Option B: Karate 2.x | Option C: Karate 1.x |
| --- | --- | --- | --- |
| Repository/language compatibility | Java/JUnit Jupiter, Gradle 9.6, single kit package | JVM 21+, Gherkin + JS + Java runners; no Gradle plugin | Unsupported line; older Java story |
| Maintenance and release stability | Owned by XQ; versions pinned in the kit | Busy 2.x rewrite line (Mar–Aug 2026); 1.x unsupported | Explicitly unsupported |
| CI and Docker determinism | Kit owns Postgres Testcontainers + approved image | CI docs + Chrome Testcontainers; DB not bundled; `retry until` / parallel locks are operator-owned | n/a for new work |
| Ease of local setup and debugging | `./gradlew clean check`; Java IDE | Maven/Gradle or CLI; rich HTML reports; **debug in IDE is paid** | n/a |
| Test authoring and failure diagnosis | Java assertions; kit HTTP/OpenAPI helpers | Gherkin `match` diffs, HTML reports; Java interop for DB | n/a |
| Ecosystem, licensing, and security fit | swagger-parser + JUnit; no Karate EULA | MIT core; commercial IDE/OpenAPI/protocols; mock RCE defaults tightened in 2.1.0 | Unsupported |
| Migration and operating cost | Continue current design | New DSL, JUnit 6 alignment, dual harness, possible paid plugins | Dead end |

## Pros and cons of Karate 2.x for XQ (standalone)

| Pros | Cons |
| --- | --- |
| MIT OSS core on Maven Central | Does not replace jvm-test-kit’s OpenAPI 3.0 or Postgres fixtures |
| Java 21 matches XQ | Gherkin/JS is a second stack in a Java kit |
| JUnit Platform / Gradle test task documented | `karate-junit6` + JUnit 6 vs undocumented Gradle 9.6 matrix |
| HTTP, GraphQL, SOAP, mocks, Gatling optional | OpenAPI-to-test is ENTERPRISE IDE, not swagger-parser |
| Virtual threads and `@lock` | `@parallel=false` silently ignored — footgun if any 1.x habits leak in |
| HTML + JUnit XML reports | HTML reports can leak secrets; vendor tells you to scan them |
| Active 2.x patches | Rewrite still accumulating regression fixes |
| FAQ itself points Java-centric teams to REST Assured | Paid surface for debug, OpenAPI import, Kafka/gRPC/WebSocket |

## Recommendation

**Recommend Option A:** keep JUnit Jupiter and `ExperienceQuality/jvm-test-kit` as the XQ service-test platform. Do not adopt Karate 2.x into the kit or backend services in this research phase.

Why this serves the stated need: XQ’s registered constraints are a **Java, single-package, Gradle-checked kit** with **owned OpenAPI 3.0 loading and Postgres containers**. Karate 2.x is a **unified Gherkin product** whose best-documented strengths (readable HTTP scenarios, mocks, HTML reports, optional UI/perf) do not close those kit gaps and would add a second runtime (karate-js), a second syntax, and a commercial edge for OpenAPI authoring. JDK 21 compatibility is necessary but not sufficient.

Karate 2.x remains a **reasonable future candidate** only if XQ later decides it wants Gherkin-authored black-box API suites *outside* the kit (for example a dedicated acceptance repo), accepts JUnit 6 alignment, and still keeps Postgres/OpenAPI in Java fixtures. That would be a new human decision, then an isolated prototype — not this note.

### Uncertainty still required before any adoption

1. Exact JUnit version jvm-test-kit uses versus `karate-junit6`’s provided `junit-jupiter-api` and Karate’s `6.1.3` parent property.
2. Whether Gradle Wrapper **9.6** runs `karate-junit6` 2.1.x without `--enable-preview` (docs disagree with the Java 21 statements).
3. Whether v2 `retry until` semantics match the 1.x README (v2 polling doc pages 404’d).
4. Whether any OSS Karate API consumes OpenAPI 3.0 documents in a way that preserves XQ’s swagger-parser subset rules (not evidenced).
5. Classpath clash risk (HttpClient 5, Netty, Jackson/json-smart) inside the single-package kit — karate-examples even documents a shaded fatjar workaround for Spring clashes.
6. Whether XQ would pay for IDE debug / OpenAPI generation, or stay on OSS-only Gherkin.
7. Parallel + Testcontainers Postgres isolation: Karate `@lock` vs kit-owned schemas; no first-party matrix.
8. `README_V2.md` vs current UI docs: trust the release notes, but treat other repo docs as possibly stale.
9. No runtime experiment was performed (by design). A throwaway prototype is allowed only after a human selects an option.

## Verified facts vs inferences

**Verified:** product split (OSS GitHub + Karate Labs commercial tools); MIT core; Java 21+ for v2; coordinates `io.karatelabs:karate-core` / `karate-junit6`; latest GitHub/Maven 2.1.2 on 2026-08-14; JUnit 6 module with provided Jupiter API; Gradle via JUnit Platform not a Karate plugin; Gherkin HTTP + Java Runner; schema `match` not swagger-parser; DB via Java interop; 1.x unsupported; OpenAPI import is IntelliJ ENTERPRISE; Kafka add-on is licensed.

**Inferences:** Karate is a poor *replacement* for jvm-test-kit; 2.x is still hardening; `--enable-preview` in the Gradle snippet is likely stale; HTTP retry can reduce CI determinism if misused; dual-harness cost would dominate any DSL benefit for XQ’s current kit shape.

## Out of scope (explicit)

- No Karate dependency, submodule, or prototype was added to xq-hub, jvm-test-kit, or any other product repo.
- No Maven/Gradle install of Karate was performed for evaluation.
- Marketing claims (Fortune 500 counts, “60% less test code,” analyst mentions) are not used as evidence.
)
