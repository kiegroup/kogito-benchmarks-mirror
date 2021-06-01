package org.kie.kogito.benchmarks.framework;

import java.io.File;

/**
 * A class which holds information about a started application.
 */
public class RunInfo {

    private final Process process;
    private final File runLog;
    private final long timeToFirstOKRequest;

    public RunInfo(Process process, File runLog, long timeToFirstOKRequest) {
        this.process = process;
        this.runLog = runLog;
        this.timeToFirstOKRequest = timeToFirstOKRequest;
    }

    public Process getProcess() {
        return process;
    }

    public File getRunLog() {
        return runLog;
    }

    public long getTimeToFirstOKRequest() {
        return timeToFirstOKRequest;
    }
}
