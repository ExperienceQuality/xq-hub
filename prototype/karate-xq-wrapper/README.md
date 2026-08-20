# Karate XQ Wrapper POC

**Status:** throwaway prototype for [#26](https://github.com/ExperienceQuality/xq-hub/issues/26)  
**Not an adoption into `jvm-test-kit`.**

## Result

`./gradlew :consumer-demo:test` — **BUILD SUCCESSFUL**

### Proven

1. Natural Gradle path works: `karate-junit6` + `@Karate.Test` + `useJUnitPlatform()`
2. **`karate-base.js` shipped in the support jar was discovered** and applied (`xqFramework`, `xqEcho`, `baseUrl`)
3. Consumer owns thin `karate-config.js` and merges via `karate.get('config')` + `karate.merge(...)`
4. Feature uses XQ capabilities **without** `Java.type`

### Gradle packaging note (important)

Karate docs show `karate-config.js` under `src/test/java`. In **Gradle**, non-`.java` files there are **not** copied to the test classpath by default. This POC places consumer config in:

`consumer-demo/src/test/resources/karate-config.js`

### Residual

- External HTTP was removed after httpbin returned 503; POC is offline-only
- Jar discovery of `karate-base.js` worked here, but still treat multi-jar classpath ordering as something to pin in a real design
- Not integrated with jvm-test-kit Postgres/OpenAPI

## Layout

```text
xq-karate-support/   Java helper + karate-base.js (in jar root)
consumer-demo/       thin karate-config.js + feature + JUnit runner
```

## Run

```bash
cd prototype/karate-xq-wrapper
./gradlew :consumer-demo:test
```
