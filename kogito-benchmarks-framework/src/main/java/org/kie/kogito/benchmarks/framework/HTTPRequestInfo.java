/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.benchmarks.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * A class representing HTTP Request details such as:
 * <ul>
 * <li>URI</li>
 * <li>Request body</li>
 * <li>HTTP method</li>
 * <li>HTTP headers</li>
 * <li>Expected status code of the response</li>
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

        public static final String SMARTHOUSE_BPMN_DMN = "{\n" +
                "  \"sensorsTemperature\": [\n" +
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
                "  \"sensorsHumidity\": [\n" +
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
                "  ],\n" +
                "  \"settingsTemperature\": {\n" +
                "    \"threshold_low\": 21,\n" +
                "    \"threshold_high\": 24\n" +
                "  },\n" +
                "  \"settingsHumidity\": {\n" +
                "    \"threshold_low\": 0,\n" +
                "    \"threshold_high\": 0\n" +
                "  }\n" +
                "}";

        public static final String PMML_CLUSTERING = "{\n" +
                "  \"sepal_length\": 7.9,\n" +
                "  \"sepal_width\": 4.4,\n" +
                "  \"petal_length\": 6.9,\n" +
                "  \"petal_width\": 2.5\n" +
                "}";

        public static final String PMML_FOREST = "{\n" +
                "  \"Age\": 21.0,\n" +
                "  \"MonthlySalary\": 10000.0,\n" +
                "  \"TotalAsset\": 10000.0,\n" +
                "  \"TotalRequired\": 15000.0,\n" +
                "  \"NumberInstallments\": 1.0\n" +
                "}";

        public static final String PMML_MINE = "{\n" +
                "  \"temperature\": 30.0,\n" +
                "  \"humidity\": 20.0\n" +
                "}";

        public static final String PMML_REGRESSION = "{\n" +
                "  \"age\": 0,\n" +
                "  \"salary\": 0,\n" +
                "  \"car_location\": \"carpark\"\n" +
                "}";

        public static final String PMML_SCORECARD = "{\n" +
                "  \"input1\": 0.0,\n" +
                "  \"input2\": 0.0\n" +
                "}";
    }
}
