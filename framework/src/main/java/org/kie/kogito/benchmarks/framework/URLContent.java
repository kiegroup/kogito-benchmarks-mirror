package org.kie.kogito.benchmarks.framework;

public enum URLContent {
    SAMPLE_KOGITO_APP(new String[][] { new String[] { "http://localhost:8080/LoanApplication", "[]" },
            new String[] { "http://localhost:8080/greeting", "1" } }),
    JAX_RS_MINIMAL(new String[][] {
            new String[] { "http://localhost:8080", "Hello from a simple JAX-RS app." },
            new String[] { "http://localhost:8080/data/hello", "Hello World" }
    }),
    FULL_MICROPROFILE(new String[][] {
            new String[] { "http://localhost:8080", "Hello from a full MicroProfile suite" },
            new String[] { "http://localhost:8080/data/hello", "Hello World" },
            new String[] { "http://localhost:8080/data/config/injected", "Config value as Injected by CDI Injected value" },
            new String[] { "http://localhost:8080/data/config/lookup", "Config value from ConfigProvider lookup value" },
            new String[] { "http://localhost:8080/data/resilience", "Fallback answer due to timeout" },
            new String[] { "http://localhost:8080/health", "\"UP\"" },
            new String[] { "http://localhost:8080/data/metric/timed", "Request is used in statistics, check with the Metrics call." },
            new String[] { "http://localhost:8080/metrics", "ontroller_timed_request_seconds_count" },
            new String[] { "http://localhost:8080/data/secured/test", "Jessie specific value" },
            new String[] { "http://localhost:8080/openapi", "/resilience" },
            new String[] { "http://localhost:8080/data/client/test/parameterValue=xxx", "Processed parameter value 'parameterValue=xxx'" }
    }),
    GENERATED_SKELETON(new String[][] {
            new String[] { "http://localhost:8080", "Congratulations" },
            new String[] { "http://localhost:8080/hello-spring", "Bye Spring" }
    });

    public final String[][] urlContent;

    URLContent(String[][] urlContent) {
        this.urlContent = urlContent;
    }
}
