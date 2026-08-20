package com.xq.karate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Throwaway POC helper. Demonstrates Java capabilities consumers reach via Karate config,
 * without calling Java.type in feature files.
 */
public final class XqKarateSupport {
    private XqKarateSupport() {
    }

    /**
     * Bootstrap used from karate-base.js / karate-config.js.
     * Returns plain JSON-friendly values plus a helper map of static-like functions
     * represented as nested maps for Karate JS consumption patterns.
     */
    public static Map<String, Object> bootstrap(String baseUrl) {
        String resolved = (baseUrl == null || baseUrl.isBlank())
                ? "https://httpbin.org"
                : baseUrl;
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("baseUrl", resolved);
        config.put("xqRequestId", "xq-" + UUID.randomUUID());
        config.put("xqEcho", new XqEcho());
        return config;
    }

    /**
     * Simple capability: echo/normalize a payload map.
     * Features call: xqEcho.ping('hello')
     */
    public static final class XqEcho {
        public String ping(String message) {
            return "xq:" + Objects.requireNonNull(message, "message");
        }

        public Map<String, Object> wrap(String name) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", Objects.requireNonNull(name, "name"));
            body.put("source", "xq-karate-support");
            return body;
        }
    }
}
