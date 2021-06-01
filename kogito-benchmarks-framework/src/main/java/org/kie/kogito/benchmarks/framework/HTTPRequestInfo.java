package org.kie.kogito.benchmarks.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * A class representing
 */
public class HTTPRequestInfo {

    private String uri;
    private String body;
    private String method;
    private Map<String, String> headers = new HashMap<>();
    private int expectedResponseStatusCode;

    private void setURI(String uri) {
        this.uri = uri;
    }

    private void setBody(String body) {
        this.body = body;
    }

    private void setMethod(String method) {
        this.method = method;
    }

    private void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    private void setExpectedResponseStatusCode(int expectedResponseStatusCode) {
        this.expectedResponseStatusCode = expectedResponseStatusCode;
    }

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
            instance.setURI(uri);
            return this;
        }

        public Builder body(String body) {
            instance.setBody(body);
            return this;
        }

        public Builder method(String method) {
            instance.setMethod(method);
            return this;
        }

        public Builder header(String name, String value) {
            instance.addHeader(name, value);
            return this;
        }

        public Builder expectedResponseStatusCode(int statusCode) {
            instance.setExpectedResponseStatusCode(statusCode);
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
