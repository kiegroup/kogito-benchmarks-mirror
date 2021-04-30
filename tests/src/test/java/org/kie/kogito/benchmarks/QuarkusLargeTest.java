package org.kie.kogito.benchmarks;

import static org.kie.kogito.benchmarks.framework.Logs.getLogsDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;
import org.kie.kogito.benchmarks.framework.HTTPRequestInfo;
import org.kie.kogito.benchmarks.framework.LogBuilder;
import org.kie.kogito.benchmarks.framework.Logs;

public class QuarkusLargeTest extends AbstractTemplateTest {

    private static final App APP_TO_TEST = App.SAMPLE_KOGITO_APP_QUARKUS_JVM;

    @Test
    public void startStop(TestInfo testInfo) throws IOException, InterruptedException {
        startStop(testInfo, APP_TO_TEST);
    }

    @Test
    public void loadTest(TestInfo testInfo) throws IOException, InterruptedException {
        HTTPRequestInfo requestInfo = HTTPRequestInfo.builder()
                .URI(LOCALHOST + "/LoanApplication")
                .body("{\"amount\":\"2000\"}")
                .method("POST")
                .header("Accept", "*/*")
                .header("Content-Type", "application/json")
                .expectedResponseStatusCode(201)
                .build(); // This may be directly replaced for example by Apache-specific class, but this keeps
                          // it detached from any framework

        loadTest(testInfo, APP_TO_TEST, requestInfo);

//        Path measurementLogSummary = Paths.get(getLogsDir(testInfo.getTestClass().get().getCanonicalName()).toString(), "measurementsSummary.csv");
//
//        for (App app : new App[]{APP_TO_TEST, App.SAMPLE_KOGITO_APP_SPRING_BOOT}) {
//            LogBuilder.Log log = new LogBuilder()
//                    .app(app)
//                    .mode(app.mavenCommands)
//                    .buildTimeMs(100)
//                    .timeToFirstOKRequestMs(200)
//                    .startedInMs(300)
//                    .stoppedInMs(400)
//                    .rssKb(500)
//                    .openedFiles(700)
//                    .build();
//
//            LogBuilder.Log log2 = new LogBuilder()
//                    .app(app)
//                    .mode(app.mavenCommands)
//                    .rssKbFinal(600)
//                    .build();
//
//            Logs.logMeasurementsSummary(log, measurementLogSummary);
//            Logs.logMeasurementsSummary(log2, measurementLogSummary);
//
//
//            Logs.logMeasurementsSummary(log, measurementLogSummary);
//            Logs.logMeasurementsSummary(log2, measurementLogSummary);
//        }


    }
}
