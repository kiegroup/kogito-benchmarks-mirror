package org.kie.kogito.benchmarks.framework;

import java.io.File;

public class BuildResult {

    private final long buildTimeMs;
    private final File buildLog;
    private final int exitCode;

    public BuildResult(long buildTimeMs, File buildLog, int exitCode) {
        this.buildTimeMs = buildTimeMs;
        this.buildLog = buildLog;
        this.exitCode = exitCode;
    }

    public long getBuildTimeMs() {
        return buildTimeMs;
    }

    public File getBuildLog() {
        return buildLog;
    }

    public int getExitCode() {
        return exitCode;
    }
}
