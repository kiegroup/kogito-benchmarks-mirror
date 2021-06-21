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
package org.kie.kogito.benchmarks.framework;

import java.util.stream.Stream;

import static org.kie.kogito.benchmarks.framework.Commands.getLocalMavenRepoDir;
import static org.kie.kogito.benchmarks.framework.Commands.getQuarkusNativeProperties;

public enum MvnCmds {
    QUARKUS_JVM(new String[][] {
            new String[] { "mvn", "clean", "package", "-Dquarkus.package.output-name=quarkus" },
            new String[] { "java", "-jar", "target/quarkus-runner.jar" }
    }),
    SPRING_BOOT_JVM(new String[][] {
            new String[] { "mvn", "clean", "package" }, // The JAR name is unified by setting finalName in the kie-assets-library repo
            new String[] { "java", "-jar", "target/smarthouse.jar" }
    }),

    // These are not used now but may be useful in the future
    DEV(new String[][] {
            new String[] { "mvn", "clean", "quarkus:dev", "-Dmaven.repo.local=" + getLocalMavenRepoDir() }
    }),
    NATIVE(new String[][] {
            Stream.concat(Stream.of("mvn", "clean", "compile", "package", "-Pnative"),
                    getQuarkusNativeProperties().stream()).toArray(String[]::new),
            new String[] { Commands.isThisWindows ? "target\\quarkus-runner" : "./target/quarkus-runner" }
    }),
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
