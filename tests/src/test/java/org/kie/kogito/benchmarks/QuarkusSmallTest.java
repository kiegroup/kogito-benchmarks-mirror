package org.kie.kogito.benchmarks;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.kogito.benchmarks.framework.App;

public class QuarkusSmallTest extends AbstractTemplateTest {

    private static final App APP_TO_TEST = App.SMARTHOUSE_02_QUARKUS_JVM;

    @Test
    public void startStop(TestInfo testInfo) throws IOException, InterruptedException {
        startStop(testInfo, APP_TO_TEST);
    }

    @Test
    public void loadTest(TestInfo testInfo) {
        // TODO
    }
}
