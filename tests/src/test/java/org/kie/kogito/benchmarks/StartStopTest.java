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
package org.kie.kogito.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;
import org.kie.kogito.benchmarks.framework.Commands;
import org.kie.kogito.benchmarks.framework.LogBuilder;
import org.kie.kogito.benchmarks.framework.Logs;
import org.kie.kogito.benchmarks.framework.MvnCmds;
import org.kie.kogito.benchmarks.framework.WebpageTester;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.kogito.benchmarks.framework.Commands.cleanTarget;
import static org.kie.kogito.benchmarks.framework.Commands.getBaseDir;
import static org.kie.kogito.benchmarks.framework.Commands.getBuildCommand;
import static org.kie.kogito.benchmarks.framework.Commands.getOpenedFDs;
import static org.kie.kogito.benchmarks.framework.Commands.getRSSkB;
import static org.kie.kogito.benchmarks.framework.Commands.getRunCommand;
import static org.kie.kogito.benchmarks.framework.Commands.parsePort;
import static org.kie.kogito.benchmarks.framework.Commands.processStopper;
import static org.kie.kogito.benchmarks.framework.Commands.runCommand;
import static org.kie.kogito.benchmarks.framework.Commands.waitForTcpClosed;
import static org.kie.kogito.benchmarks.framework.Logs.SKIP;
import static org.kie.kogito.benchmarks.framework.Logs.appendln;
import static org.kie.kogito.benchmarks.framework.Logs.appendlnSection;
import static org.kie.kogito.benchmarks.framework.Logs.archiveLog;
import static org.kie.kogito.benchmarks.framework.Logs.checkListeningHost;
import static org.kie.kogito.benchmarks.framework.Logs.checkLog;
import static org.kie.kogito.benchmarks.framework.Logs.checkThreshold;
import static org.kie.kogito.benchmarks.framework.Logs.getLogsDir;
import static org.kie.kogito.benchmarks.framework.Logs.parseStartStopTimestamps;
import static org.kie.kogito.benchmarks.framework.Logs.writeReport;

@Tag("startstop")
public class StartStopTest {

    private static final Logger LOGGER = Logger.getLogger(StartStopTest.class.getName());

    public static final String BASE_DIR = getBaseDir();

    public void testRuntime(TestInfo testInfo, App app, MvnCmds mvnCmds) throws IOException, InterruptedException {
        LOGGER.info("Testing app: " + app.toString() + ", mode: " + mvnCmds.toString());

        Process pA = null;
        File buildLogA = null;
        File runLogA = null;
        StringBuilder whatIDidReport = new StringBuilder();
        File appDir = new File(BASE_DIR + File.separator + app.dir);
        String cn = testInfo.getTestClass().get().getCanonicalName();
        String mn = testInfo.getTestMethod().get().getName();
        try {
            // Cleanup
            cleanTarget(app);
            Files.createDirectories(Paths.get(appDir.getAbsolutePath() + File.separator + "logs"));

            // Build
            buildLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + mvnCmds.name().toLowerCase() + "-build.log");
            ExecutorService buildService = Executors.newFixedThreadPool(1);

            List<String> baseBuildCmd = new ArrayList<>();
            baseBuildCmd.addAll(Arrays.asList(mvnCmds.mvnCmds[0]));
            //baseBuildCmd.add("-Dquarkus.version=" + getQuarkusVersion());
            List<String> cmd = getBuildCommand(baseBuildCmd.toArray(new String[0]));

            buildService.submit(new Commands.ProcessRunner(appDir, buildLogA, cmd, 20)); // TODO exit code handling
            appendln(whatIDidReport, "# " + cn + ", " + mn);
            appendln(whatIDidReport, (new Date()).toString());
            appendln(whatIDidReport, appDir.getAbsolutePath());
            appendlnSection(whatIDidReport, String.join(" ", cmd));
            long buildStarts = System.currentTimeMillis();
            buildService.shutdown();
            buildService.awaitTermination(30, TimeUnit.MINUTES);
            long buildEnds = System.currentTimeMillis();

            assertTrue(buildLogA.exists());
            checkLog(cn, mn, app, mvnCmds, buildLogA);

            List<Long> rssKbList = new ArrayList<>(10);
            List<Long> timeToFirstOKRequestList = new ArrayList<>(10);
            for (int i = 0; i < 1; i++) {
                // Run
                LOGGER.info("Running... round " + i);
                runLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + mvnCmds.name().toLowerCase() + "-run.log");
                cmd = getRunCommand(mvnCmds.mvnCmds[1]);
                appendln(whatIDidReport, appDir.getAbsolutePath());
                appendlnSection(whatIDidReport, String.join(" ", cmd));
                long runStarts = System.currentTimeMillis();
                pA = runCommand(cmd, appDir, runLogA);
                long runEnds = System.currentTimeMillis();
                System.out.println("RunEnds (" + runEnds + ") - RunStarts (" + runStarts + ") : " + (runEnds - runStarts));
                // Test web pages
                long timeToFirstOKRequest = WebpageTester.testWeb(app.urlContent.urlContent[0][0], 10, app.urlContent.urlContent[0][1], true);
                LOGGER.info("Testing web page content...");
                for (String[] urlContent : app.urlContent.urlContent) {
                    WebpageTester.testWeb(urlContent[0], 5, urlContent[1], false);
                }

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

                Path measurementsLog = Paths.get(getLogsDir(cn, mn).toString(), "measurements.csv");
                LogBuilder.Log log = new LogBuilder()
                        .app(app)
                        .mode(mvnCmds)
                        .buildTimeMs(buildEnds - buildStarts)
                        .timeToFirstOKRequestMs(timeToFirstOKRequest)
                        .startedInMs((long) (startedStopped[0] * 1000))
                        .stoppedInMs((long) (startedStopped[1] * 1000))
                        .rssKb(rssKb)
                        .openedFiles(openedFiles)
                        .build();
                Logs.logMeasurements(log, measurementsLog);
                appendln(whatIDidReport, "Measurements:");
                appendln(whatIDidReport, log.headerMarkdown + "\n" + log.lineMarkdown);

                rssKbList.add(rssKb);
                timeToFirstOKRequestList.add(timeToFirstOKRequest);
            }

            long rssKbAvgWithoutMinMax = getAvgWithoutMinMax(rssKbList);
            long timeToFirstOKRequestAvgWithoutMinMax = getAvgWithoutMinMax(timeToFirstOKRequestList);
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

    private long getAvgWithoutMinMax(List<Long> listOfValues) {
        //        listOfValues.remove(Collections.min(listOfValues));
        //        listOfValues.remove(Collections.max(listOfValues));
        return (long) listOfValues.stream().mapToLong(val -> val).average().orElse(Long.MAX_VALUE);
    }

    @Test
    public void kogito(TestInfo testInfo) throws IOException, InterruptedException {
        testRuntime(testInfo, App.SMARTHOUSE_02_QUARKUS_JVM, MvnCmds.QUARKUS_JVM);
    }

    //    @Test
    //    public void jaxRsMinimalJVM(TestInfo testInfo) throws IOException, InterruptedException {
    //        testRuntime(testInfo, App.JAX_RS_MINIMAL, MvnCmds.JVM);
    //    }
    //
    //    @Test
    //    @Tag("native")
    //    public void jaxRsMinimalNative(TestInfo testInfo) throws IOException, InterruptedException {
    //        testRuntime(testInfo, App.JAX_RS_MINIMAL, MvnCmds.NATIVE);
    //    }
    //
    //    @Test
    //    public void fullMicroProfileJVM(TestInfo testInfo) throws IOException, InterruptedException {
    //        testRuntime(testInfo, App.FULL_MICROPROFILE, MvnCmds.JVM);
    //    }
    //
    //    @Test
    //    @Tag("native")
    //    public void fullMicroProfileNative(TestInfo testInfo) throws IOException, InterruptedException {
    //        testRuntime(testInfo, App.FULL_MICROPROFILE, MvnCmds.NATIVE);
    //    }
}
