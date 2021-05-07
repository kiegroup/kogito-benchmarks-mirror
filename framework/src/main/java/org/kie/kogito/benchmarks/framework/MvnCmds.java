package org.kie.kogito.benchmarks.framework;

import java.util.stream.Stream;

import static org.kie.kogito.benchmarks.framework.Commands.getLocalMavenRepoDir;
import static org.kie.kogito.benchmarks.framework.Commands.getQuarkusNativeProperties;

public enum MvnCmds {
    QUARKUS_JVM(new String[][] {
            new String[] { "mvn", "clean", "package", "-Dquarkus.package.output-name=quarkus" },
            new String[] { "java", "-jar", "target/quarkus-runner.jar" }
    }),
    SPRING_BOOT_02_JVM(new String[][] {
            new String[] { "mvn", "clean", "package" }, // There is no possibility of changing the final name of the artifact
            new String[] { "java", "-jar", "target/smarthouse-02-dm-springboot-1.0-SNAPSHOT.jar" }
    }),
    SPRING_BOOT_03_JVM(new String[][] {
            new String[] { "mvn", "clean", "package" },
            new String[] { "java", "-jar", "target/smarthouse-03-dm-springboot-1.0-SNAPSHOT.jar" }
    }),
    DEV(new String[][] {
            new String[] { "mvn", "clean", "quarkus:dev", "-Dmaven.repo.local=" + getLocalMavenRepoDir() }
    }),
    NATIVE(new String[][] {
            Stream.concat(Stream.of("mvn", "clean", "compile", "package", "-Pnative"),
                    getQuarkusNativeProperties().stream()).toArray(String[]::new),
            new String[] { Commands.isThisWindows ? "target\\quarkus-runner" : "./target/quarkus-runner" }
    }),
    //    GENERATOR(new String[][] {
    //            new String[] {
    //                    "mvn",
    //                    "io.quarkus:quarkus-maven-plugin:" + getQuarkusVersion() + ":create",
    //                    "-DprojectGroupId=my-groupId",
    //                    "-DprojectArtifactId=" + App.GENERATED_SKELETON.dir,
    //                    "-DprojectVersion=1.0.0-SNAPSHOT",
    //                    "-DpackageName=org.my.group"
    //            }
    //    }),
    MVNW_DEV(new String[][] {
            new String[] { Commands.MVNW, "quarkus:dev" }
    }),
    MVNW_JVM(new String[][] {
            new String[] { Commands.MVNW, "clean", "compile", "quarkus:build", "-Dquarkus.package.output-name=quarkus" },
            new String[] { "java", "-jar", "target/quarkus-app/quarkus-run.jar" }
    }),
    MVNW_NATIVE(new String[][] {
            Stream.concat(Stream.of(Commands.MVNW, "clean", "compile", "package", "-Pnative", "-Dquarkus.package.output-name=quarkus"),
                    getQuarkusNativeProperties().stream()).toArray(String[]::new),
            new String[] { Commands.isThisWindows ? "target\\quarkus-runner" : "./target/quarkus-runner" }
    });

    public final String[][] mvnCmds;

    MvnCmds(String[][] mvnCmds) {
        this.mvnCmds = mvnCmds;
    }

    public boolean isJVM() {
        return this.name().contains("JVM");
    }
}
