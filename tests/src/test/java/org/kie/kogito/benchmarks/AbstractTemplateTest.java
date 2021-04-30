package org.kie.kogito.benchmarks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;
import org.kie.kogito.benchmarks.framework.BuildResult;
import org.kie.kogito.benchmarks.framework.HTTPRequestInfo;
import org.kie.kogito.benchmarks.framework.LogBuilder;
import org.kie.kogito.benchmarks.framework.Logs;
import org.kie.kogito.benchmarks.framework.MvnCmds;
import org.kie.kogito.benchmarks.framework.RunInfo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.kogito.benchmarks.framework.Commands.BASE_DIR;
import static org.kie.kogito.benchmarks.framework.Commands.buildApp;
import static org.kie.kogito.benchmarks.framework.Commands.cleanTarget;
import static org.kie.kogito.benchmarks.framework.Commands.getOpenedFDs;
import static org.kie.kogito.benchmarks.framework.Commands.getRSSkB;
import static org.kie.kogito.benchmarks.framework.Commands.parsePort;
import static org.kie.kogito.benchmarks.framework.Commands.processStopper;
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

    private static final Logger LOGGER = Logger.getLogger(StartStopTest.class.getName());

    public static final int START_STOP_ITERATIONS = 3;
    public static final String LOCALHOST = "http://localhost:8080";

    public void startStop(TestInfo testInfo, App app) throws IOException, InterruptedException {
        LOGGER.info("Testing app startStop: " + app.toString() + ", mode: " + app.mavenCommands.toString());

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        StringBuilder whatIDidReport = new StringBuilder();
        File appDir = app.getAppDir(BASE_DIR);
        MvnCmds mvnCmds = app.mavenCommands;
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            cleanTarget(app);
            Files.createDirectories(Paths.get(appDir.getAbsolutePath() + File.separator + "logs"));

            // Build
            BuildResult buildResult = buildApp(app, mn, cn, whatIDidReport);
            buildLogA = buildResult.getBuildLog();

            assertTrue(buildLogA.exists());
            checkLog(cn, mn, app, mvnCmds, buildLogA);

            // Prepare for run
            List<Long> rssKbValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> timeToFirstOKRequestValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> startedInMsValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> stoppedInMsValues = new ArrayList<>(START_STOP_ITERATIONS);
            List<Long> openedFilesValues = new ArrayList<>(START_STOP_ITERATIONS);

            for (int i = 0; i < START_STOP_ITERATIONS; i++) {
                // Run
                LOGGER.info("Running... round " + i);
                RunInfo runInfo = startApp(app, whatIDidReport);
                pA = runInfo.getProcess();
                runLogA = runInfo.getRunLog();

                LOGGER.info("Terminate and scan logs...");
                pA.getInputStream().available(); // TODO Ask Karm

                long rssKb = getRSSkB(pA.pid());
                long openedFiles = getOpenedFDs(pA.pid());

                processStopper(pA, false);

                LOGGER.info("Gonna wait for ports closed...");
                // Release ports
                assertTrue(waitForTcpClosed("localhost", parsePort(app.urlContent.urlContent[0][0]), 60),
                        "Main port is still open");
                checkLog(cn, mn, app, mvnCmds, runLogA);
                checkListeningHost(cn, mn, mvnCmds, runLogA);

                float[] startedStopped = parseStartStopTimestamps(runLogA);
                long startedInMs = (long) (startedStopped[0] * 1000);
                long stoppedInMs = (long) (startedStopped[1] * 1000);

                Path measurementsLog = Paths.get(getLogsDir(cn, mn).toString(), "measurements.csv");
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

                rssKbValues.add(rssKb);
                openedFilesValues.add(openedFiles);
                timeToFirstOKRequestValues.add(runInfo.getTimeToFirstOKRequest());
                startedInMsValues.add(startedInMs);
                stoppedInMsValues.add(stoppedInMs);
            }

            long rssKbAvgWithoutMinMax = getAvgWithoutMinMax(rssKbValues);
            long openedFilesAvgWithoutMinMax = getAvgWithoutMinMax(openedFilesValues);
            long timeToFirstOKRequestAvgWithoutMinMax = getAvgWithoutMinMax(timeToFirstOKRequestValues);
            long startedInMsAvgWithoutMinMax = getAvgWithoutMinMax(startedInMsValues);
            long stoppedInMsAvgWithoutMinMax = getAvgWithoutMinMax(stoppedInMsValues);

            Path measurementsSummary = Paths.get(getLogsDir(cn).toString(), "measurementsSummary.csv");

            LogBuilder.Log log = new LogBuilder()
                    .app(app)
                    .mode(mvnCmds)
                    .buildTimeMs(buildResult.getBuildTimeMs())
                    .timeToFirstOKRequestMs(timeToFirstOKRequestAvgWithoutMinMax)
                    .startedInMs(startedInMsAvgWithoutMinMax)
                    .stoppedInMs(stoppedInMsAvgWithoutMinMax)
                    .rssKb(rssKbAvgWithoutMinMax)
                    .openedFiles(openedFilesAvgWithoutMinMax)
                    .build();
            Logs.logMeasurementsSummary(log, measurementsSummary);

            LOGGER.info("AVG timeToFirstOKRequest without min and max values: " + timeToFirstOKRequestAvgWithoutMinMax);
            LOGGER.info("AVG rssKb without min and max values: " + rssKbAvgWithoutMinMax);
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
            //cleanTarget(app);
        }
    }

    private long getAvgWithoutMinMax(List<Long> listOfValues) { // TODO Median?
        listOfValues.remove(Collections.min(listOfValues));
        listOfValues.remove(Collections.max(listOfValues));
        return (long) listOfValues.stream().mapToLong(val -> val).average().orElse(Long.MAX_VALUE);
    }

    public void loadTest(TestInfo testInfo, App app, HTTPRequestInfo requestInfo) throws IOException, InterruptedException {
        LOGGER.info("Testing app startStop: " + app.toString() + ", mode: " + app.mavenCommands.toString());

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        StringBuilder whatIDidReport = new StringBuilder();
        File appDir = app.getAppDir(BASE_DIR);
        MvnCmds mvnCmds = app.mavenCommands;
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            cleanTarget(app);
            Files.createDirectories(Paths.get(appDir.getAbsolutePath() + File.separator + "logs"));

            // Build
            BuildResult buildResult = buildApp(app, mn, cn, whatIDidReport);
            buildLogA = buildResult.getBuildLog();

            assertTrue(buildLogA.exists());
            checkLog(cn, mn, app, mvnCmds, buildLogA);

            // Start the App
            RunInfo runInfo = startApp(app, whatIDidReport);
            pA = runInfo.getProcess();
            runLogA = runInfo.getRunLog();

            long rssKb = getRSSkB(pA.pid());

            List<Long> values = new ArrayList<>(20000);


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
            long startTime;
            try (CloseableHttpClient client = HttpClients.createDefault()){
                HttpPost postRequest = new HttpPost(requestInfo.getURI());
                postRequest.setEntity(new StringEntity(requestInfo.getBody()));
                requestInfo.getHeaders().forEach(postRequest::setHeader);

                startTime = System.currentTimeMillis();
                for (int i = 0; i < 20000; i++) {
                    long requestStartTime = System.nanoTime();
                    try (CloseableHttpResponse response = client.execute(postRequest)) {
                        Assertions.assertThat(response.getStatusLine().getStatusCode()).isEqualTo(requestInfo.getExpectedResponseStatusCode());
//                        System.out.println("Response code: " + response.getStatusLine().getStatusCode());
//                        System.out.println("Page is: " + EntityUtils.toString(response.getEntity()));
                        EntityUtils.consume(response.getEntity());
                    }
                    long requestEndTime = System.nanoTime();
                    long duration = requestEndTime - requestStartTime;
                    values.add(duration);
                }
            }
            long endTime = System.currentTimeMillis();

            System.out.println("First response time: " + values.get(0));
            System.out.println("Second response time: " + values.get(1));
            System.out.println("Third response time: " + values.get(2));
            System.out.println("Average response time: " + values.stream().mapToLong(Long::longValue).skip(1).average());
            System.out.println("Total duration: " + (endTime - startTime));

            long rssKbFinal = getRSSkB(pA.pid());
            long openedFiles = getOpenedFDs(pA.pid()); // TODO also do before the "test" itself?

            // Stop the App
            processStopper(pA, false);

            LOGGER.info("Gonna wait for ports closed...");
            // Release ports
            assertTrue(waitForTcpClosed("localhost", parsePort(app.urlContent.urlContent[0][0]), 60),
                       "Main port is still open");
            checkLog(cn, mn, app, mvnCmds, runLogA);
            checkListeningHost(cn, mn, mvnCmds, runLogA);

            float[] startedStopped = parseStartStopTimestamps(runLogA);// Don't need this in the load test?
            long startedInMs = (long) (startedStopped[0] * 1000);
            long stoppedInMs = (long) (startedStopped[1] * 1000);

            Path measurementsLog = Paths.get(getLogsDir(cn, mn).toString(), "measurements.csv");
            Path measurementsSummaryLog = Paths.get(getLogsDir(cn).toString(), "measurementsSummary.csv");
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

            //checkThreshold(app, mvnCmds, rssKbAvgWithoutMinMax, timeToFirstOKRequestAvgWithoutMinMax, SKIP);
        } finally {
            // Make sure processes are down even if there was an exception / failure
            if (pA != null) {
                processStopper(pA, true);
            }
            // Archive logs no matter what
            archiveLog(cn, mn, buildLogA);
            archiveLog(cn, mn, runLogA);
            writeReport(cn, mn, whatIDidReport.toString());
            //cleanTarget(app);

        }
    }

}
