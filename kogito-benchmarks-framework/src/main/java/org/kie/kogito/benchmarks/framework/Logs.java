package org.kie.kogito.benchmarks.framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.kogito.benchmarks.framework.Commands.BASE_DIR;
import static org.kie.kogito.benchmarks.framework.Commands.isThisWindows;

public class Logs {
    private static final Logger logger = LoggerFactory.getLogger(Logs.class);

    public static final String jarSuffix = "redhat";
    private static final Pattern jarNamePattern = Pattern.compile("^((?!" + jarSuffix + ").)*jar$");

    private static final Pattern startedPattern = Pattern.compile(".* [Ss]tarted.* in ([0-9\\.]+)(?:s| seconds).*", Pattern.DOTALL);
    private static final Pattern stoppedPattern = Pattern.compile(".* stopped in ([0-9\\.]+)s.*", Pattern.DOTALL);
    /*
     * Due to console colouring, Windows has control characters in the sequence.
     * So "1.778s" in "started in 1.778s." becomes "[38;5;188m1.778".
     * e.g.
     * //started in [38;5;188m1.228[39ms.
     * //stopped in [38;5;188m0.024[39ms[39m[38;5;203m[39m[38;5;227m
     * 
     * Although when run from Jenkins service account; those symbols might not be present
     * depending on whether you checked AllowInteractingWithDesktop.
     * // TODO to make it smoother?
     */
    private static final Pattern startedPatternControlSymbols = Pattern.compile(".* [Ss]tarted.* in .*188m([0-9\\.]+)(?:s| seconds).*", Pattern.DOTALL);
    private static final Pattern stoppedPatternControlSymbols = Pattern.compile(".* stopped in .*188m([0-9\\.]+).*", Pattern.DOTALL);

    private static final Pattern warnErrorDetectionPattern = Pattern.compile("(?i:.*(ERROR|WARN|SLF4J:).*)");
    private static final Pattern listeningOnDetectionPattern = Pattern.compile("(?i:.*Listening on:.*)");
    private static final Pattern devExpectedHostPattern = Pattern.compile("(?i:.*localhost:.*)");
    private static final Pattern defaultExpectedHostPattern = Pattern.compile("(?i:.*0.0.0.0:.*)");

    public static final long SKIP = -1L;

    public static void checkLog(String testClass, String testMethod, App app, MvnCmds cmd, File log) throws IOException {
        try (Scanner sc = new Scanner(log, UTF_8)) {
            Set<String> offendingLines = new HashSet<>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                boolean error = warnErrorDetectionPattern.matcher(line).matches();
                if (error) {
                    if (isWhiteListed(app.whitelistLogLines.errs, line)) {
                        logger.info(cmd.name() + " log for " + testMethod + " contains whitelisted error: `" + line + "'");
                    } else if (isWhiteListed(app.whitelistLogLines.platformErrs(), line)) {
                        logger.info(cmd.name() + " log for " + testMethod + " contains platform specific whitelisted error: `" + line + "'");
                    } else {
                        offendingLines.add(line);
                    }
                }
            }
            assertTrue(offendingLines.isEmpty(),
                    cmd.name() + " log should not contain error or warning lines that are not whitelisted. " +
                            "See testsuite" + File.separator + "target" + File.separator + "archived-logs" +
                            File.separator + testClass + File.separator + testMethod + File.separator + log.getName() +
                            " and check these offending lines: \n" + String.join("\n", offendingLines));
        }
    }

    public static void checkListeningHost(String testClass, String testMethod, MvnCmds cmd, File log) throws IOException {
        if (cmd.name().startsWith("SPRING")) {
            return; // Spring Boot doesn't provide the host information by default (only at debug level)
        }
        boolean isOffending = true;
        try (Scanner sc = new Scanner(log, UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (listeningOnDetectionPattern.matcher(line).matches()) {
                    Pattern expectedHostPattern = defaultExpectedHostPattern;
                    if (cmd == MvnCmds.DEV || cmd == MvnCmds.MVNW_DEV) {
                        expectedHostPattern = devExpectedHostPattern;
                    }

                    isOffending = !expectedHostPattern.matcher(line).matches();
                }
            }
        }

        assertFalse(isOffending,
                cmd.name() + " log should contain expected listening host. " +
                        "See testsuite" + File.separator + "target" + File.separator + "archived-logs" +
                        File.separator + testClass + File.separator + testMethod + File.separator + log.getName() +
                        " and check the listening host.");
    }

    private static boolean isWhiteListed(Pattern[] patterns, String line) {
        for (Pattern p : patterns) {
            if (p.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    public static void checkThreshold(App app, MvnCmds cmd, long rssKb, long timeToFirstOKRequest, long timeToReloadedOKRequest) {
        String propPrefix = isThisWindows ? "windows" : "linux";
        if (cmd.isJVM()) {
            propPrefix += ".jvm";
        } else if (cmd == MvnCmds.NATIVE) {
            propPrefix += ".native";
        } else if (cmd == MvnCmds.DEV) {
            propPrefix += ".dev";
        } else {
            throw new IllegalArgumentException("Unexpected mode. Check MvnCmds.java.");
        }
        if (timeToFirstOKRequest != SKIP) {
            long timeToFirstOKRequestThresholdMs = app.thresholdProperties.get(propPrefix + ".time.to.first.ok.request.threshold.ms");
            assertTrue(timeToFirstOKRequest <= timeToFirstOKRequestThresholdMs,
                    "Application " + app + " in " + cmd + " mode took " + timeToFirstOKRequest
                            + " ms to get the first OK request, which is over " +
                            timeToFirstOKRequestThresholdMs + " ms threshold.");
        }
        if (rssKb != SKIP) {
            long rssThresholdKb = app.thresholdProperties.get(propPrefix + ".RSS.threshold.kB");
            assertTrue(rssKb <= rssThresholdKb,
                    "Application " + app + " in " + cmd + " consumed " + rssKb + " kB, which is over " +
                            rssThresholdKb + " kB threshold.");
        }
        if (timeToReloadedOKRequest != SKIP) {
            long timeToReloadedOKRequestThresholdMs = app.thresholdProperties.get(propPrefix + ".time.to.reload.threshold.ms");
            assertTrue(timeToReloadedOKRequest <= timeToReloadedOKRequestThresholdMs,
                    "Application " + app + " in " + cmd + " mode took " + timeToReloadedOKRequest
                            + " ms to get the first OK request after dev mode reload, which is over " +
                            timeToReloadedOKRequestThresholdMs + " ms threshold.");
        }
    }

    public static void archiveLog(String testClass, String testMethod, File log) throws IOException {
        if (log == null || !log.exists()) {
            logger.warn("log must be a valid, existing file. Skipping operation.");
            return;
        }
        if (StringUtils.isBlank(testClass)) {
            throw new IllegalArgumentException("testClass must not be blank");
        }
        if (StringUtils.isBlank(testMethod)) {
            throw new IllegalArgumentException("testMethod must not be blank");
        }
        Path destDir = getLogsDir(testClass, testMethod);
        Files.createDirectories(destDir);
        String filename = log.getName();
        Files.copy(log.toPath(), Paths.get(destDir.toString(), filename), REPLACE_EXISTING);
    }

    public static void writeReport(String testClass, String testMethod, String text) throws IOException {
        Path destDir = getLogsDir(testClass, testMethod);
        Files.createDirectories(destDir);
        Files.write(Paths.get(destDir.toString(), "report.md"), text.getBytes(UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Path agregateReport = Paths.get(getLogsDir().toString(), "aggregated-report.md");
        if (Files.notExists(agregateReport)) {
            Files.write(agregateReport, ("# Aggregated Report\n\n").getBytes(UTF_8), StandardOpenOption.CREATE);
        }
        Files.write(agregateReport, text.getBytes(UTF_8), StandardOpenOption.APPEND);
    }

    /**
     * Markdown needs two newlines to make a new paragraph.
     */
    public static void appendln(StringBuilder s, String text) {
        s.append(text);
        s.append("\n\n");
    }

    public static void appendlnSection(StringBuilder s, String text) {
        s.append(text);
        s.append("\n\n---\n");
    }

    public static Path getLogsDir(String testClass, String testMethod) throws IOException {
        Path destDir = new File(getLogsDir(testClass).toString() + File.separator + testMethod).toPath();
        Files.createDirectories(destDir);
        return destDir;
    }

    public static Path getLogsDir(String testClass) throws IOException {
        Path destDir = new File(getLogsDir().toString() + File.separator + testClass).toPath();
        Files.createDirectories(destDir);
        return destDir;
    }

    public static Path getLogsDir() throws IOException {
        Path destDir = new File(BASE_DIR + File.separator + "tests" + File.separator + "archived-logs").toPath();
        Files.createDirectories(destDir);
        return destDir;
    }

    public static void logMeasurements(LogBuilder.Log log, Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.write(path, (log.headerCSV + "\n").getBytes(UTF_8), StandardOpenOption.CREATE);
        }
        Files.write(path, (log.lineCSV + "\n").getBytes(UTF_8), StandardOpenOption.APPEND);
        logger.info("\n" + log.headerCSV + "\n" + log.lineCSV);
    }

    public static void logMeasurementsSummary(LogBuilder.Log log, Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.write(path, (log.headerCSV + "\n").getBytes(UTF_8), StandardOpenOption.CREATE);
            Files.write(path, (log.lineCSV + "\n").getBytes(UTF_8), StandardOpenOption.APPEND);
            return;
        }

        List<String> lines = Files.lines(path).collect(Collectors.toCollection(ArrayList::new));
        String currentHeader = lines.get(0);
        String headerWithoutAppAndMode = stripAppAndModeColumns(log.headerCSV);
        if (!currentHeader.contains(headerWithoutAppAndMode)) {
            lines.set(0, currentHeader + "," + headerWithoutAppAndMode);
            currentHeader = lines.get(0);
        }

        String lastLine = lines.get(lines.size() - 1);

        long headerLength = currentHeader.chars().filter(value -> value == ',').count();
        long lastLineLength = lastLine.chars().filter(value -> value == ',').count();
        if (lastLineLength < headerLength) {
            String newDataWithoutAppAndMode = stripAppAndModeColumns(log.lineCSV);
            lines.set(lines.size() - 1, lastLine + "," + newDataWithoutAppAndMode);
        } else {
            lines.add(log.lineCSV);
        }

        Files.write(path, (lines.stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator()).getBytes());

        logger.info("\n" + log.headerCSV + "\n" + log.lineCSV);
    }

    private static String stripAppAndModeColumns(String line) {
        int secondOccurrence = line.indexOf(",", line.indexOf(",") + 1);
        return line.substring(secondOccurrence + 1);
    }

    public static float[] parseStartStopTimestamps(File log, App app) throws IOException {
        float[] startedStopped = new float[] { -1f, -1f };
        try (Scanner sc = new Scanner(log, UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();

                Matcher m = startedPatternControlSymbols.matcher(line);
                if (startedStopped[0] == -1f && m.matches()) {
                    startedStopped[0] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = startedPattern.matcher(line);
                if (startedStopped[0] == -1f && m.matches()) {
                    startedStopped[0] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = stoppedPatternControlSymbols.matcher(line);
                if (startedStopped[1] == -1f && m.matches()) {
                    startedStopped[1] = Float.parseFloat(m.group(1));
                    continue;
                }

                m = stoppedPattern.matcher(line);
                if (startedStopped[1] == -1f && m.matches()) {
                    startedStopped[1] = Float.parseFloat(m.group(1));
                }
            }
        }
        if (startedStopped[0] == -1f) {
            logger.error("Parsing start time from log failed. " +
                    "Might not be the right time to call this method. The process might have ben killed before it wrote to log. " +
                    "Find " + log.getName() + " in your target dir.");
        }
        if (startedStopped[1] == -1f && !app.isSpringBoot()) { // Spring Boot doesn't provide stop time
            logger.error("Parsing stop time from log failed. " +
                    "Might not be the right time to call this method. The process might have been killed before it wrote to log. " +
                    "Find " + log.getName() + " in your target dir.");
        }
        return startedStopped;
    }
}
