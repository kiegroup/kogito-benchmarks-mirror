package org.kie.kogito.benchmarks.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import static org.kie.kogito.benchmarks.framework.Commands.APPS_DIR;

public enum App {
    SMARTHOUSE_02_QUARKUS_JVM("smarthouse-02-quarkus", MvnCmds.QUARKUS_JVM, URLContent.SMARTHOUSE_02, WhitelistLogLines.EVERYTHING),
    SMARTHOUSE_03_QUARKUS_JVM("smarthouse-03-quarkus", MvnCmds.QUARKUS_JVM, URLContent.SMARTHOUSE_03, WhitelistLogLines.EVERYTHING),

    SMARTHOUSE_02_SPRING_BOOT("smarthouse-02-springboot", MvnCmds.SPRING_BOOT_JVM, URLContent.SMARTHOUSE_02, WhitelistLogLines.EVERYTHING),
    SMARTHOUSE_03_SPRING_BOOT("smarthouse-03-springboot", MvnCmds.SPRING_BOOT_JVM, URLContent.SMARTHOUSE_03, WhitelistLogLines.EVERYTHING);

    public final String dir;
    public final MvnCmds mavenCommands;
    public final URLContent urlContent;
    public final WhitelistLogLines whitelistLogLines;
    public final Map<String, Long> thresholdProperties = new HashMap<>();

    App(String dir, MvnCmds mavenCommands, URLContent urlContent, WhitelistLogLines whitelistLogLines) {
        this.dir = dir;
        this.mavenCommands = mavenCommands;
        this.urlContent = urlContent;
        this.whitelistLogLines = whitelistLogLines;

        String propertiesFilePath = "/" + dir + "/threshold.properties";
        URL propertiesFile = Optional.ofNullable(App.class.getResource(propertiesFilePath))
                .orElseThrow(() -> new RuntimeException("Couldn't find " + propertiesFilePath));
        String appDirNormalized = dir.toUpperCase().replace('-', '_') + "_";
        try (InputStream input = propertiesFile.openStream()) {
            Properties props = new Properties();
            props.load(input);
            for (String pn : props.stringPropertyNames()) {
                String normPn = pn.toUpperCase().replace('.', '_');
                String env = System.getenv().get(appDirNormalized + normPn);
                if (StringUtils.isNotBlank(env)) {
                    props.replace(pn, env);
                }
                String sys = System.getProperty(appDirNormalized + normPn);
                if (StringUtils.isNotBlank(sys)) {
                    props.replace(pn, sys);
                }
                thresholdProperties.put(pn, Long.parseLong(props.getProperty(pn)));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Check threshold.properties and Sys and Env variables (upper case, underscores instead of dots). " +
                    "All values are expected to be of type long.");
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read " + propertiesFilePath);
        }
    }

    public File getAppDir() {
        return new File(APPS_DIR, dir);
    }

    public boolean isQuarkus() {
        return this.name().contains("QUARKUS");
    }

    public boolean isSpringBoot() {
        return this.name().contains("SPRING");
    }
}
