package org.kie.kogito.benchmarks.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.benchmarks.framework.Logs.appendln;
import static org.kie.kogito.benchmarks.framework.Logs.appendlnSection;

public class Commands {
    private static final Logger logger = LoggerFactory.getLogger(Commands.class);

    public static final String BASE_DIR = getBaseDir();
    public static final String APPS_DIR = getAppsDir();
    public static final String MVNW = Commands.isThisWindows ? "mvnw.cmd" : "./mvnw";
    public static final boolean isThisWindows = System.getProperty("os.name").matches(".*[Ww]indows.*");
    private static final Pattern numPattern = Pattern.compile("[ \t]*[0-9]+[ \t]*");
    private static final Pattern quarkusVersionPattern = Pattern.compile("[ \t]*<quarkus.version>([^<]*)</quarkus.version>.*");
    private static final Pattern trailingSlash = Pattern.compile("/+$");

    public static String getArtifactGeneBaseDir() {
        for (String p : new String[] { "ARTIFACT_GENERATOR_WORKSPACE", "artifact.generator.workspace" }) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }
        return System.getProperty("java.io.tmpdir");
    }

    public static String getLocalMavenRepoDir() {
        for (String p : new String[] { "TESTS_MAVEN_REPO_LOCAL", "tests.maven.repo.local" }) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }

        String mainBuildRepo = System.getProperty("maven.repo.local");
        if (StringUtils.isNotBlank(mainBuildRepo)) {
            return mainBuildRepo;
        }

        return System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
    }

    /**
     * Get system properties starting with `quarkus.native` prefix, for example quarkus.native.builder-image
     * 
     * @return List of `-Dquarkus.native.xyz=foo` strings
     */
    public static List<String> getQuarkusNativeProperties() {
        List<String> quarkusNativeProperties = System.getProperties().entrySet().stream()
                .filter(e -> e.getKey().toString().contains("quarkus.native"))
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());
        return quarkusNativeProperties;
    }

    public static String getQuarkusPlatformVersion() {
        for (String p : new String[] { "QUARKUS_PLATFORM_VERSION", "quarkus.platform.version" }) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }
        logger.warn("Failed to detect quarkus.platform.version/QUARKUS_PLATFORM_VERSION, defaulting to getQuarkusVersion().");
        return getQuarkusVersion();
    }

    public static String getQuarkusVersion() {
        for (String p : new String[] { "QUARKUS_VERSION", "quarkus.version" }) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                return env;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                return sys;
            }
        }
        String failure = "Failed to determine quarkus.version. Check pom.xm, check env and sys vars QUARKUS_VERSION";
        try (Scanner sc = new Scanner(new File(getBaseDir() + File.separator + "pom.xml"), StandardCharsets.UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                Matcher m = quarkusVersionPattern.matcher(line);
                if (m.matches()) {
                    return m.group(1);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(failure);
        }
        throw new IllegalArgumentException(failure);
    }

    public static String getBaseDir() {
        String baseDirValue = getSystemPropertyOrEnvVarValue("basedir");
        return new File(baseDirValue).getParent();
    }

    public static String getAppsDir() {
        return getSystemPropertyOrEnvVarValue("appsDir");
    }

    private static String getSystemPropertyOrEnvVarValue(String name) {
        String systemPropertyValue = System.getProperty(name);
        if (StringUtils.isNotBlank(systemPropertyValue)) {
            return systemPropertyValue;
        }

        String envPropertyValue = System.getenv(name);

        if (StringUtils.isBlank(envPropertyValue)) {
            throw new IllegalArgumentException("Unable to determine the value of the property " + name);
        }
        return envPropertyValue;
    }

    public static void cleanTarget(App app) {
        String target = APPS_DIR + File.separator + app.dir + File.separator + "target";
        String logs = APPS_DIR + File.separator + app.dir + File.separator + "logs";
        cleanDirOrFile(target, logs);
    }

    public static BuildResult buildApp(App app, String methodName, String className, StringBuilder whatIDidReport) throws InterruptedException {
        File appDir = app.getAppDir();
        File buildLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + app.mavenCommands.name().toLowerCase() + "-build.log");
        ExecutorService buildService = Executors.newFixedThreadPool(1);

        List<String> baseBuildCmd = new ArrayList<>(Arrays.asList(app.mavenCommands.mvnCmds[0]));
        List<String> cmd = getBuildCommand(baseBuildCmd.toArray(new String[0]));

        buildService.submit(new Commands.ProcessRunner(appDir, buildLogA, cmd, 20)); // TODO exit code handling
        appendln(whatIDidReport, "# " + className + ", " + methodName);
        appendln(whatIDidReport, (new Date()).toString());
        appendln(whatIDidReport, appDir.getAbsolutePath());
        appendlnSection(whatIDidReport, String.join(" ", cmd));
        long buildStarts = System.currentTimeMillis();
        buildService.shutdown();
        buildService.awaitTermination(30, TimeUnit.MINUTES);
        long buildEnds = System.currentTimeMillis();
        long buildTimeMs = buildEnds - buildStarts;

        return new BuildResult(buildTimeMs, buildLogA, 0);
    }

    public static RunInfo startApp(App app, StringBuilder whatIDidReport) throws IOException, InterruptedException {
        File appDir = app.getAppDir();
        File runLogA = new File(appDir.getAbsolutePath() + File.separator + "logs" + File.separator + app.mavenCommands.name().toLowerCase() + "-run.log");
        List<String> cmd = getRunCommand(app.mavenCommands.mvnCmds[1]);
        appendln(whatIDidReport, appDir.getAbsolutePath());
        appendlnSection(whatIDidReport, String.join(" ", cmd));
        Process pA = runCommand(cmd, appDir, runLogA);
        // Test web pages
        long timeToFirstOKRequest = WebpageTester.testWeb(app.urlContent.urlContent[0][0], 10, app.urlContent.urlContent[0][1], true);
        logger.info("Testing web page content...");
        for (String[] urlContent : app.urlContent.urlContent) {
            WebpageTester.testWeb(urlContent[0], 5, urlContent[1], false);
        }

        return new RunInfo(pA, runLogA, timeToFirstOKRequest);
    }

    public static void setCPUAffinity(RunInfo runInfo, int numOfCores) throws IOException, InterruptedException {
        logger.info("Setting CPU affinity of app to " + numOfCores + " core(s)");
        String listOfCores = "0-" + (numOfCores - 1);
        String processId = String.valueOf(runInfo.getProcess().pid());
        ProcessBuilder pBuilder = new ProcessBuilder("taskset", "-cpa", listOfCores, processId);
        pBuilder.redirectErrorStream(true);
        Process p = pBuilder.start();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String newCPUAffinity = bufferedReader.lines()
                    .filter(s -> s.contains("new affinity list"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("New CPU Affinity not set!"));
            String expectedAffinityList = numOfCores == 1 ? "0" : numOfCores == 2 ? "0,1" : listOfCores;
            Assertions.assertThat(newCPUAffinity).contains(expectedAffinityList);
            p.waitFor();
        }
        Assertions.assertThat(p.exitValue()).isEqualTo(0);
    }

    public static void cleanDirOrFile(String... path) {
        for (String s : path) {
            try {
                Files.walk(Paths.get(s))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                logger.warn("Unable to delete directories or files", e);
            }
        }
    }

    public static List<String> getRunCommand(String[] baseCommand) {
        List<String> runCmd = new ArrayList<>();
        if (isThisWindows) {
            runCmd.add("cmd");
            runCmd.add("/C");
        }
        runCmd.addAll(Arrays.asList(baseCommand));

        return Collections.unmodifiableList(runCmd);
    }

    public static List<String> getBuildCommand(String[] baseCommand) {
        List<String> buildCmd = new ArrayList<>();
        if (isThisWindows) {
            buildCmd.add("cmd");
            buildCmd.add("/C");
        }
        buildCmd.addAll(Arrays.asList(baseCommand));
        buildCmd.add("-Dmaven.repo.local=" + getLocalMavenRepoDir());
        // use the same settings.xml as the main build
        buildCmd.add("-s");
        buildCmd.add(System.getProperty("maven.settings"));

        return Collections.unmodifiableList(buildCmd);
    }

    public static boolean waitForTcpClosed(String host, int port, long loopTimeoutS) throws InterruptedException, UnknownHostException {
        InetAddress address = InetAddress.getByName(host);
        long now = System.currentTimeMillis();
        long startTime = now;
        InetSocketAddress socketAddr = new InetSocketAddress(address, port);
        while (now - startTime < 1000 * loopTimeoutS) {
            try (Socket socket = new Socket()) {
                // If it let's you write something there, it is still ready.
                socket.connect(socketAddr, 1000);
                socket.setSendBufferSize(1);
                socket.getOutputStream().write(1);
                socket.shutdownInput();
                socket.shutdownOutput();
                logger.info("Socket still available: " + host + ":" + port);
            } catch (IOException e) {
                // Exception thrown - socket is likely closed.
                return true;
            }
            Thread.sleep(1000);
            now = System.currentTimeMillis();
        }
        return false;
    }

    // TODO this may be useful in the future
    public static void adjustPrettyPrintForJsonLogging(String appDir) throws IOException {
        Path appProps = Paths.get(appDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "application.properties");
        Path appYaml = Paths.get(appDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "application.yml");

        adjustFileContent(appProps, "quarkus.log.console.json.pretty-print=true", "quarkus.log.console.json.pretty-print=false");
        adjustFileContent(appYaml, "pretty-print: true", "pretty-print: fase");
    }

    private static void adjustFileContent(Path path, String regex, String replacement) throws IOException {
        if (Files.exists(path)) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll(regex, replacement);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static int parsePort(String url) {
        return Integer.parseInt(url.split(":")[2].split("/")[0]);
    }

    public static Process runCommand(List<String> command, File directory, File logFile) {
        ProcessBuilder pa = new ProcessBuilder(command);
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.directory(directory);
        pa.redirectErrorStream(true);
        pa.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        Process pA = null;
        try {
            pA = pa.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pA;
    }

    public static void pidKiller(long pid, boolean force) {
        try {
            if (isThisWindows) {
                if (!force) {
                    Process p = Runtime.getRuntime().exec(new String[] {
                            BASE_DIR + File.separator + "framework" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator +
                                    "CtrlC.exe ",
                            Long.toString(pid) });
                    p.waitFor(1, TimeUnit.MINUTES);
                }
                Runtime.getRuntime().exec(new String[] { "cmd", "/C", "taskkill", "/PID", Long.toString(pid), "/F", "/T" });
            } else {
                Runtime.getRuntime().exec(new String[] { "kill", force ? "-9" : "-15", Long.toString(pid) });
            }
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static long getRSSkB(long pid) throws IOException, InterruptedException {
        ProcessBuilder pa;
        if (isThisWindows) {
            // Note that PeakWorkingSetSize might be better, but we would need to change it on Linux too...
            // https://docs.microsoft.com/en-us/windows/win32/cimwin32prov/win32-process
            pa = new ProcessBuilder("wmic", "process", "where", "processid=" + pid, "get", "WorkingSetSize");
        } else {
            pa = new ProcessBuilder("ps", "-p", Long.toString(pid), "-o", "rss=");
        }
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.redirectErrorStream(true);
        Process p = pa.start();
        try (BufferedReader processOutputReader =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = processOutputReader.readLine()) != null) {
                if (numPattern.matcher(l).matches()) {
                    if (isThisWindows) {
                        // Qualifiers: DisplayName ("Working Set Size"), Units ("bytes")
                        return Long.parseLong(l.trim()) / 1024L;
                    } else {
                        return Long.parseLong(l.trim());
                    }
                }
            }
            p.waitFor();
        }
        return -1L;
    }

    public static long getOpenedFDs(long pid) throws IOException, InterruptedException {
        ProcessBuilder pa;
        long count = 0;
        if (isThisWindows) {
            pa = new ProcessBuilder("wmic", "process", "where", "processid=" + pid, "get", "HandleCount");
        } else {
            pa = new ProcessBuilder("lsof", "-F0n", "-p", Long.toString(pid));
        }
        Map<String, String> envA = pa.environment();
        envA.put("PATH", System.getenv("PATH"));
        pa.redirectErrorStream(true);
        Process p = pa.start();
        try (BufferedReader processOutputReader =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            if (isThisWindows) {
                String l;
                // TODO: We just get a magical number with all FDs... Is it O.K.?
                while ((l = processOutputReader.readLine()) != null) {
                    if (numPattern.matcher(l).matches()) {
                        return Long.parseLong(l.trim());
                    }
                }
            } else {
                // TODO: For the time being we count apples and oranges; we might want to distinguish .so and .jar ?
                while (processOutputReader.readLine() != null) {
                    count++;
                }
            }
            p.waitFor();
        }
        return count;
    }

    /*
     * TODO: CPU cycles used
     * 
     * Pros: good data
     * Cons: dependency on perf tool; will not translate to Windows data
     * 
     * karm@local:~/workspaceRH/fooBar$ perf stat java -jar target/fooBar-1.0.0-SNAPSHOT-runner.jar
     * 2020-02-25 16:07:00,870 INFO [io.quarkus] (main) fooBar 1.0.0-SNAPSHOT (running on Quarkus 999-SNAPSHOT) started in 0.776s.
     * 2020-02-25 16:07:00,873 INFO [io.quarkus] (main) Profile prod activated.
     * 2020-02-25 16:07:00,873 INFO [io.quarkus] (main) Installed features: [amazon-lambda, cdi, resteasy]
     * 2020-02-25 16:07:03,360 INFO [io.quarkus] (main) fooBar stopped in 0.018s
     * 
     * Performance counter stats for 'java -jar target/fooBar-1.0.0-SNAPSHOT-runner.jar':
     * 
     * 1688.910052 task-clock:u (msec) # 0.486 CPUs utilized
     * 0 context-switches:u # 0.000 K/sec
     * 0 cpu-migrations:u # 0.000 K/sec
     * 12,865 page-faults:u # 0.008 M/sec
     * 4,274,799,448 cycles:u # 2.531 GHz
     * 4,325,761,598 instructions:u # 1.01 insn per cycle
     * 919,713,769 branches:u # 544.561 M/sec
     * 29,310,015 branch-misses:u # 3.19% of all branches
     * 
     * 3.473028811 seconds time elapsed
     */

    // TODO Ask Karm about this
    public static void processStopper(Process p, boolean force) throws InterruptedException, IOException {
        p.children().forEach(child -> {
            if (child.supportsNormalTermination()) {
                child.destroy();
            }
            pidKiller(child.pid(), force);
        });
        if (p.supportsNormalTermination()) {
            p.destroy();
            p.waitFor(3, TimeUnit.MINUTES);
        }
        pidKiller(p.pid(), force);
    }

    public static class ProcessRunner implements Runnable {
        final File directory;
        final File log;
        final List<String> command;
        final long timeoutMinutes;

        public ProcessRunner(File directory, File log, List<String> command, long timeoutMinutes) {
            this.directory = directory;
            this.log = log;
            this.command = command;
            this.timeoutMinutes = timeoutMinutes;
        }

        @Override
        public void run() {
            ProcessBuilder pb = new ProcessBuilder(command);
            Map<String, String> env = pb.environment();
            env.put("PATH", System.getenv("PATH"));
            pb.directory(directory);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(log));
            Process p = null;
            try {
                logger.info("Running command: " + String.join(" ", command));
                p = pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Objects.requireNonNull(p).waitFor(timeoutMinutes, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
