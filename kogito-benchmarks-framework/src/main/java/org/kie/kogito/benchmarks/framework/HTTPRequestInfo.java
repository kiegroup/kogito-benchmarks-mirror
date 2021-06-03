package org.kie.kogito.benchmarks.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * A class representing HTTP Request details such as:
 * <ul>
 *     <li> URI</li>
 *     <li>Request body</li>
 *     <li>HTTP method</li>
 *     <li>HTTP headers</li>
 *     <li>Expected status code of the response</li>
 * </ul>
 */
public class HTTPRequestInfo {

    private String uri;
    private String body;
    private String method;
    private Map<String, String> headers = new HashMap<>();
    private int expectedResponseStatusCode;

    public String getURI() {
        return uri;
    }

    public String getBody() {
        return body;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getExpectedResponseStatusCode() {
        return expectedResponseStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final HTTPRequestInfo instance = new HTTPRequestInfo();

        public Builder URI(String uri) {
            instance.uri = uri;
            return this;
        }

        public Builder body(String body) {
            instance.body = body;
            return this;
        }

        public Builder method(String method) {
            instance.method = method;
            return this;
        }

        public Builder header(String name, String value) {
            instance.headers.put(name, value);
            return this;
        }

        public Builder expectedResponseStatusCode(int statusCode) {
            instance.expectedResponseStatusCode = statusCode;
            return this;
        }

        public HTTPRequestInfo build() {
            return instance;
        }

    }

    public static class Body {
        public static final String HEATING_02 = "{\n" +
                "  \"Sensors Temperature\": [\n" +
                "    {\n" +
                "      \"placement\": \"OUTSIDE\",\n" +
                "      \"current\": 25,\n" +
                "      \"previous\": [\n" +
                "        24,\n" +
                "        23,\n" +
                "        19,\n" +
                "        16,\n" +
                "        15,\n" +
                "        11\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"placement\": \"INSIDE\",\n" +
                "      \"current\": 24.9,\n" +
                "      \"previous\": [\n" +
                "        25,\n" +
                "        28,\n" +
                "        28,\n" +
                "        28,\n" +
                "        28,\n" +
                "        28\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"Settings Temperature\": {\n" +
                "    \"threshold_low\": 21,\n" +
                "    \"threshold_high\": 24\n" +
                "  },\n" +
                "  \"Settings Humidity\": {\n" +
                "    \"threshold_low\": 0,\n" +
                "    \"threshold_high\": 0\n" +
                "  },\n" +
                "  \"Sensors Humidity\": [\n" +
                "    {\n" +
                "      \"placement\": \"OUTSIDE\",\n" +
                "      \"current\": 0,\n" +
                "      \"previous\": [\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"placement\": \"INSIDE\",\n" +
                "      \"current\": 0,\n" +
                "      \"previous\": [\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0,\n" +
                "        0\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

}
