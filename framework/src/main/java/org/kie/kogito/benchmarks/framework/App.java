package org.kie.kogito.benchmarks.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import static org.kie.kogito.benchmarks.framework.Commands.APPS_DIR;

public enum App {
    SAMPLE_KOGITO_APP_QUARKUS_JVM("sample-kogito-app", MvnCmds.QUARKUS_JVM, URLContent.SAMPLE_KOGITO_APP, WhitelistLogLines.SAMPLE_KOGITO_APP),
    SAMPLE_KOGITO_APP_SPRING_BOOT("sample-kogito-app", MvnCmds.SPRING_BOOT_JVM, URLContent.SAMPLE_KOGITO_APP, WhitelistLogLines.SAMPLE_KOGITO_APP);
    //    JAX_RS_MINIMAL("app-jax-rs-minimal", URLContent.JAX_RS_MINIMAL, WhitelistLogLines.JAX_RS_MINIMAL),
    //    FULL_MICROPROFILE("app-full-microprofile", URLContent.FULL_MICROPROFILE, WhitelistLogLines.FULL_MICROPROFILE),
    //    GENERATED_SKELETON("app-generated-skeleton", URLContent.GENERATED_SKELETON, WhitelistLogLines.GENERATED_SKELETON);

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
        File tpFile = new File(APPS_DIR + File.separator + dir + File.separator + "threshold.properties");
        String appDirNormalized = dir.toUpperCase().replace('-', '_') + "_";
        try (InputStream input = new FileInputStream(tpFile)) {
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
            throw new RuntimeException("Couldn't find " + tpFile.getAbsolutePath());
        }
    }

    public File getAppDir() {
        return new File(APPS_DIR, dir);
    }
}
