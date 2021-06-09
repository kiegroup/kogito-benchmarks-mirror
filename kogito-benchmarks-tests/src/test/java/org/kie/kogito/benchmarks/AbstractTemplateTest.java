package org.kie.kogito.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;
import org.kie.kogito.benchmarks.framework.BuildResult;
import org.kie.kogito.benchmarks.framework.HTTPRequestInfo;
import org.kie.kogito.benchmarks.framework.LogBuilder;
import org.kie.kogito.benchmarks.framework.Logs;
import org.kie.kogito.benchmarks.framework.MvnCmds;
import org.kie.kogito.benchmarks.framework.RunInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.kogito.benchmarks.framework.Commands.buildApp;
import static org.kie.kogito.benchmarks.framework.Commands.cleanTarget;
import static org.kie.kogito.benchmarks.framework.Commands.getOpenedFDs;
import static org.kie.kogito.benchmarks.framework.Commands.getRSSkB;
import static org.kie.kogito.benchmarks.framework.Commands.parsePort;
import static org.kie.kogito.benchmarks.framework.Commands.processStopper;
import static org.kie.kogito.benchmarks.framework.Commands.setCPUAffinity;
import static org.kie.kogito.benchmarks.framework.Commands.startApp;
import static org.kie.kogito.benchmarks.framework.Commands.waitForTcpClosed;
import static org.kie.kogito.benchmarks.framework.Logs.SKIP;
import static org.kie.kogito.benchmarks.framework.Logs.appendln;
import static org.kie.kogito.benchmarks.framework.Logs.archiveLog;
import static org.kie.kogito.benchmarks.framework.Logs.checkListeningHost;
import static org.kie.kogito.benchmarks.framework.Logs.checkLog;
import static org.kie.kogito.benchmarks.framework.Logs.checkThreshold;
import static org.kie.kogito.benchmarks.framework.Logs.getLogsDir;
import static org.kie.kogito.benchmarks.framework.Logs.parseStartStopTimestamps;
import static org.kie.kogito.benchmarks.framework.Logs.writeReport;

public abstract class AbstractTemplateTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTemplateTest.class);

    public static final int START_STOP_ITERATIONS = 3;
    public static final String LOCALHOST = "http://localhost:8080";

    public void startStop(TestInfo testInfo, App app) throws IOException, InterruptedException {
        logger.info("Running startStop test. Testing app: " + app.toString() + ", mode: " + app.mavenCommands.toString());

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        StringBuilder whatIDidReport = new StringBuilder();
        File appDir = app.getAppDir();
        MvnCmds mvnCmds = app.mavenCommands;
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            cleanTarget(app);
            Files.createDirectories(Path.of(appDir.getAbsolutePath(), "logs"));

            // Build first time to download dependencies
            BuildResult buildResult = buildApp(app, mn, cn, whatIDidReport);
            buildLogA = buildResult.getBuildLog();

            assertTrue(buildLogA.exists());
            checkLog(cn, mn, app, mvnCmds, buildLogA);

            // Prepare for measurements
            List<Long> buildTimeValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> rssKbValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> timeToFirstOKRequestValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> startedInMsValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> stoppedInMsValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> openedFilesValues = new ArrayList<>(START_STOP_ITERATIONS);

            for (int i = 0; i < START_STOP_ITERATIONS; i++) {
                logger.info("Running... round " + i);
                // Build
                buildResult = buildApp(app, mn, cn, whatIDidReport);
                buildLogA = buildResult.getBuildLog();

                assertTrue(buildLogA.exists());
                checkLog(cn, mn, app, mvnCmds, buildLogA);

                // Run
                RunInfo runInfo = startApp(app, whatIDidReport);
                pA = runInfo.getProcess();
                runLogA = runInfo.getRunLog();

                logger.info("Terminate and scan logs...");
                pA.getInputStream().available(); // TODO Ask Karm

                long rssKb = getRSSkB(pA.pid());
                long openedFiles = getOpenedFDs(pA.pid());

                processStopper(pA, false);

                logger.info("Gonna wait for ports closed...");
                // Release ports
                assertTrue(waitForTcpClosed("localhost", parsePort(app.urlContent.urlContent[0][0]), 60),
                        "Main port is still open");
                checkLog(cn, mn, app, mvnCmds, runLogA);
                checkListeningHost(cn, mn, mvnCmds, runLogA);

                float[] startedStopped = parseStartStopTimestamps(runLogA, app);
                long startedInMs = (long) (startedStopped[0] * 1000);
                long stoppedInMs = (long) (startedStopped[1] * 1000);

                Path measurementsLog = getLogsDir(cn, mn).resolve("measurements.csv");
                LogBuilder.Log log = new LogBuilder()
                        .app(app)
                        .mode(mvnCmds)
                        .buildTimeMs(buildResult.getBuildTimeMs())
                        .timeToFirstOKRequestMs(runInfo.getTimeToFirstOKRequest())
                        .startedInMs(startedInMs)
                        .stoppedInMs(stoppedInMs)
                        .rssKb(rssKb)
                        .openedFiles(openedFiles)
                        .build();
                Logs.logMeasurements(log, measurementsLog);
                appendln(whatIDidReport, "Measurements:");
                appendln(whatIDidReport, log.headerMarkdown + "\n" + log.lineMarkdown);

                buildTimeValues.add(buildResult.getBuildTimeMs());
                rssKbValues.add(rssKb);
                openedFilesValues.add(openedFiles);
                timeToFirstOKRequestValues.add(runInfo.getTimeToFirstOKRequest());
                startedInMsValues.add(startedInMs);
                stoppedInMsValues.add(stoppedInMs);
            }

            long buildTimeAvgWithoutMinMax = getAvgWithoutMinMax(buildTimeValues);
            long rssKbAvgWithoutMinMax = getAvgWithoutMinMax(rssKbValues);
            long openedFilesAvgWithoutMinMax = getAvgWithoutMinMax(openedFilesValues);
            long timeToFirstOKRequestAvgWithoutMinMax = getAvgWithoutMinMax(timeToFirstOKRequestValues);
            long startedInMsAvgWithoutMinMax = getAvgWithoutMinMax(startedInMsValues);
            long stoppedInMsAvgWithoutMinMax = getAvgWithoutMinMax(stoppedInMsValues);

            Path measurementsSummary = getLogsDir(cn).resolve("measurementsSummary.csv");

            LogBuilder.Log log = new LogBuilder()
                    .app(app)
                    .mode(mvnCmds)
                    .buildTimeMs(buildTimeAvgWithoutMinMax)
                    .timeToFirstOKRequestMs(timeToFirstOKRequestAvgWithoutMinMax)
                    .startedInMs(startedInMsAvgWithoutMinMax)
                    .stoppedInMs(stoppedInMsAvgWithoutMinMax)
                    .rssKb(rssKbAvgWithoutMinMax)
                    .openedFiles(openedFilesAvgWithoutMinMax)
                    .build();
            Logs.logMeasurementsSummary(log, measurementsSummary);

            logger.info("AVG timeToFirstOKRequest without min and max values: " + timeToFirstOKRequestAvgWithoutMinMax);
            logger.info("AVG rssKb without min and max values: " + rssKbAvgWithoutMinMax);
            checkThreshold(app, mvnCmds, rssKbAvgWithoutMinMax, timeToFirstOKRequestAvgWithoutMinMax, SKIP);
        } finally {
            // Make sure processes are down even if there was an exception / failure
            if (pA != null) {
                processStopper(pA, true);
            }
            // Archive logs no matter what
            archiveLog(cn, mn, buildLogA);
            archiveLog(cn, mn, runLogA);
            writeReport(cn, mn, whatIDidReport.toString());
        }
    }

    private long getAvgWithoutMinMax(List<Long> listOfValues) { // TODO Median?
        listOfValues.remove(Collections.min(listOfValues));
        listOfValues.remove(Collections.max(listOfValues));
        return (long) listOfValues.stream().mapToLong(val -> val).average().orElse(Long.MAX_VALUE);
    }

    public void loadTest(TestInfo testInfo, App app, HTTPRequestInfo requestInfo) throws IOException, InterruptedException {
        logger.info("Running loadTest test. Testing app: " + app.toString() + ", mode: " + app.mavenCommands.toString());

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        StringBuilder whatIDidReport = new StringBuilder();
        File appDir = app.getAppDir();
        MvnCmds mvnCmds = app.mavenCommands;
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            cleanTarget(app);
            Files.createDirectories(Path.of(appDir.getAbsolutePath(), "logs"));

            // Build
            BuildResult buildResult = buildApp(app, mn, cn, whatIDidReport);
            buildLogA = buildResult.getBuildLog();

            assertTrue(buildLogA.exists());
            checkLog(cn, mn, app, mvnCmds, buildLogA);

            // Start the App
            RunInfo runInfo = startApp(app, whatIDidReport);
            setCPUAffinity(runInfo, 4);
            pA = runInfo.getProcess();
            runLogA = runInfo.getRunLog();

            long rssKb = getRSSkB(pA.pid());

            List<Long> values = new ArrayList<>(20000);

            // Intentionally commented, still considering implementing a pluggable REST client interface
            // Plain OLD Java "HTTP Client"
            //            long startTime = System.currentTimeMillis();
            //            for (int i = 0; i < 20000; i++) {
            //                long requestStartTime = System.nanoTime();
            //                HttpURLConnection c = (HttpURLConnection) new URL(requestInfo.getURI()).openConnection();
            //                requestInfo.getHeaders().forEach(c::setRequestProperty);
            //                c.setRequestMethod(requestInfo.getMethod());
            //                c.setDoOutput(true);
            //                c.setConnectTimeout(500);
            //
            //                try (OutputStream os = c.getOutputStream()) {
            //                    os.write(requestInfo.getBody().getBytes());
            //                }
            //
            //                try (Scanner scanner = new Scanner(c.getInputStream(), StandardCharsets.UTF_8.toString())) {
            //                    Assertions.assertThat(c.getResponseCode()).isEqualTo(requestInfo.getExpectedResponseStatusCode());
            ////                    System.out.println("Response code: " + c.getResponseCode());
            //                    scanner.useDelimiter("\\A");
            //                    String webPage = scanner.hasNext() ? scanner.next() : "";
            ////                    System.out.println("Page is: " + webPage);
            //                } catch (Exception e) {
            //                    LOGGER.info("Error when executing request on " + requestInfo.getURI(), e);
            //                }
            //
            //                long requestEndTime = System.nanoTime();
            //                long duration = requestEndTime - requestStartTime;
            //                values.add(duration);
            ////                System.out.println(duration);
            //            }
            //            long endTime = System.currentTimeMillis();

            // Java 11 HTTP Client

            //            HttpClient httpClient = HttpClient.newHttpClient();
            //            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(requestInfo.getURI()))
            //                    .POST(HttpRequest.BodyPublishers.ofString(requestInfo.getBody()));
            //            requestInfo.getHeaders().forEach(requestBuilder::header);
            //            HttpRequest request = requestBuilder.build();
            //
            //
            //
            //
            //            long startTime = System.currentTimeMillis();
            //            for (int i = 0; i < 20000; i++) {
            //                long requestStartTime = System.nanoTime();
            //                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            //                Assertions.assertThat(response.statusCode()).isEqualTo(requestInfo.getExpectedResponseStatusCode());
            ////                System.out.println("Response code: " + response.statusCode());
            ////                System.out.println("Page is: " + response.body());
            //                long requestEndTime = System.nanoTime();
            //                long duration = requestEndTime - requestStartTime;
            //                values.add(duration);
            ////                System.out.println(duration);
            //            }
            //            long endTime = System.currentTimeMillis();

            // Apache HTTP Client 4
            long totalDuration;
            long firstResponseTime;
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost postRequest = new HttpPost(requestInfo.getURI());
                postRequest.setEntity(new StringEntity(requestInfo.getBody()));
                requestInfo.getHeaders().forEach(postRequest::setHeader);

                // Warm up run
                runRequests(client, postRequest, 1000, requestInfo.getExpectedResponseStatusCode(), values);

                firstResponseTime = values.get(0);
                values.clear();

                // Measurements run
                long startTime = System.currentTimeMillis();
                runRequests(client, postRequest, 20000, requestInfo.getExpectedResponseStatusCode(), values);
                long endTime = System.currentTimeMillis();
                totalDuration = endTime - startTime;
            }

            // Intentionally left here until a proper reporting to a file is present
            System.out.println("First response time: " + firstResponseTime);
            System.out.println("First response times: " + values.stream().limit(100).collect(Collectors.toList()));
            System.out.println("Last response times: " + values.stream().skip(values.size() - 100).collect(Collectors.toList()));
            System.out.println("Average response time: " + values.stream().mapToLong(Long::longValue).average());
            System.out.println("Total duration: " + totalDuration);

            long rssKbFinal = getRSSkB(pA.pid());
            long openedFiles = getOpenedFDs(pA.pid()); // TODO also do before the "test" itself? Maybe not needed as before is covered in a startStop test

            // Stop the App
            processStopper(pA, false);

            logger.info("Gonna wait for ports closed...");
            // Release ports
            assertTrue(waitForTcpClosed("localhost", parsePort(app.urlContent.urlContent[0][0]), 60),
                    "Main port is still open");
            checkLog(cn, mn, app, mvnCmds, runLogA);
            checkListeningHost(cn, mn, mvnCmds, runLogA);

            float[] startedStopped = parseStartStopTimestamps(runLogA, app);// Don't need this in the load test?
            long startedInMs = (long) (startedStopped[0] * 1000);
            long stoppedInMs = (long) (startedStopped[1] * 1000);

            Path measurementsLog = getLogsDir(cn, mn).resolve("measurements.csv");
            Path measurementsSummaryLog = getLogsDir(cn).resolve("measurementsSummary.csv");
            LogBuilder.Log log = new LogBuilder()
                    .app(app)
                    .mode(mvnCmds)
                    .buildTimeMs(buildResult.getBuildTimeMs())
                    .timeToFirstOKRequestMs(runInfo.getTimeToFirstOKRequest())
                    .startedInMs(startedInMs)
                    .stoppedInMs(stoppedInMs)
                    .rssKb(rssKb)
                    .rssKbFinal(rssKbFinal)
                    .openedFiles(openedFiles)
                    .build();

            Logs.logMeasurements(log, measurementsLog);

            LogBuilder.Log summaryLog = new LogBuilder()
                    .app(app)
                    .mode(mvnCmds)
                    .rssKbFinal(rssKbFinal)
                    .build();
            Logs.logMeasurementsSummary(summaryLog, measurementsSummaryLog);
            appendln(whatIDidReport, "Measurements:");
            appendln(whatIDidReport, log.headerMarkdown + "\n" + log.lineMarkdown);

            // TODO check other load test-related metrics here, e.g. rssKbFinal
            checkThreshold(app, mvnCmds, rssKb, runInfo.getTimeToFirstOKRequest(), SKIP);
        } finally {
            // Make sure processes are down even if there was an exception / failure
            if (pA != null) {
                processStopper(pA, true);
            }
            // Archive logs no matter what
            archiveLog(cn, mn, buildLogA);
            archiveLog(cn, mn, runLogA);
            writeReport(cn, mn, whatIDidReport.toString());
        }
    }

    private void runRequests(CloseableHttpClient client, HttpUriRequest request, int count, int expectedResponseStatusCode, List<Long> values) throws IOException {
        for (int i = 0; i < count; i++) {
            long requestStartTime = System.nanoTime();
            try (CloseableHttpResponse response = client.execute(request)) {
                Assertions.assertThat(response.getStatusLine().getStatusCode()).isEqualTo(expectedResponseStatusCode);
                EntityUtils.consume(response.getEntity());
            }
            long requestEndTime = System.nanoTime();
            long duration = requestEndTime - requestStartTime;
            values.add(duration);
        }
    }

}
