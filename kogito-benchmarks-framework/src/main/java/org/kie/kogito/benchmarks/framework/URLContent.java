package org.kie.kogito.benchmarks.framework;

public enum URLContent {
    SMARTHOUSE_02(new String[][] { new String[] { "http://localhost:8080/heating", "name=\"heating\"" } }),
    SMARTHOUSE_03(new String[][] { new String[] { "http://localhost:8080/heating", "name=\"heating\"" } });

    public final String[][] urlContent;

    URLContent(String[][] urlContent) {
        this.urlContent = urlContent;
    }
}
