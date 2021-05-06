package org.kie.kogito.benchmarks;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;
import org.kie.kogito.benchmarks.framework.HTTPRequestInfo;

public class SmartHouse03SpringBootTest extends AbstractTemplateTest {

    private static final App APP_TO_TEST = App.SMARTHOUSE_03_SPRING_BOOT;

    @Test
    public void startStop(TestInfo testInfo) throws IOException, InterruptedException {
        startStop(testInfo, APP_TO_TEST);
    }

    @Test
    public void loadTest(TestInfo testInfo) throws IOException, InterruptedException {
        HTTPRequestInfo requestInfo = HTTPRequestInfo.builder()
                .URI(LOCALHOST + "/heating")
                .body(HTTPRequestInfo.Body.HEATING_02)
                .method("POST")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .expectedResponseStatusCode(200)
                .build(); // This may be directly replaced for example by Apache-specific class, but this keeps
                          // it detached from any framework

        loadTest(testInfo, APP_TO_TEST, requestInfo);
    }
}
