import org.gradle.api.tasks.bundling.Zip

// [VXG RealForever]
//
// Repository-owned execution surface for docs/foundation/TEST-PROOF-ARCHITECTURE.md.
// Keep the root build file small; this script owns only test/proof orchestration and evidence
// collection, never feature semantics.

/**
 * Canonical repository-owned JVM/source verification.
 *
 * CI, host runners, and developers should call this task rather than duplicate its task list.
 * Android/device proof remains separate because it owns a different execution substrate.
 */
tasks.register("vexVerifyJvm") {
    group = "verification"
    description =
        "Run source alignment, reusable architecture contracts, and app JVM tests."

    dependsOn(
        ":app:verifySourceAlignment",
        ":architecture:behavior:test",
        ":architecture:execution:test",
        ":architecture:elements:test",
        ":architecture:logging:test",
        ":architecture:relay:test",
        ":app:testDebugUnitTest",
    )
}

/**
 * Canonical local source/build checkpoint that does not require a connected Android target.
 */
tasks.register("vexVerifyLocal") {
    group = "verification"
    description =
        "Run canonical JVM/source verification and assemble the debug APK."

    dependsOn(
        "vexVerifyJvm",
        ":app:assembleDebug",
    )
}

/**
 * Canonical connected-Android Journey/instrumentation proof.
 *
 * Android Gradle Plugin owns target discovery and adb/instrumentation mechanics; Android Studio is
 * only an optional human UI over this same execution path.
 */
tasks.register("vexVerifyDevice") {
    group = "verification"
    description =
        "Run the baseline connected Android instrumentation/Journey suite."

    dependsOn(":app:connectedDebugAndroidTest")
}

/**
 * Local JVM coverage report across the current app + architecture modules.
 *
 * Coverage is diagnostic evidence only at this stage: no percentage gate is admitted.
 */
tasks.register("vexCoverage") {
    group = "verification"
    description =
        "Run JVM tests and generate aggregate Kover HTML/XML coverage reports."

    dependsOn(
        "koverHtmlReport",
        "koverXmlReport",
    )
}

/**
 * Validate committed deterministic Compose screenshot references.
 *
 * Reference generation/update intentionally has no convenience aggregate: accepting new pixels is a
 * judgment operation and uses the explicit app task documented by the Test Foundation.
 */
tasks.register("vexVisualValidate") {
    group = "verification"
    description =
        "Validate deterministic Compose preview screenshot references."

    dependsOn(":app:validateDebugScreenshotTest")
    mustRunAfter(":app:updateDebugScreenshotTest")
}


/**
 * Collects the evidence produced by a Test Foundation host run into one returnable ZIP.
 *
 * This task owns transport convenience only. It does not run or accept proof by itself, and the
 * reference PNGs inside remain candidates until explicitly reviewed.
 *
 * Run it in the same Gradle invocation as the proof tasks with --continue. The mustRunAfter
 * constraints keep this collection step last even when an earlier independent proof task fails.
 */
val vexTestFoundationProofBundle =
    tasks.register<Zip>("vexTestFoundationProofBundle") {
        notCompatibleWithConfigurationCache(
            "Collects host-local evidence by spawning git and reading generated proof artifacts at execution time.",
        )
        group = "verification"
        description =
            "Collect Test Foundation reports and generated visual references into one ZIP."

        archiveFileName.set(
            "VexLife-Test-Foundation-Proof.zip",
        )
        destinationDirectory.set(
            layout.buildDirectory.dir(
                "vex/test-foundation",
            ),
        )

        val metadata =
            layout.buildDirectory.file(
                "vex/test-foundation/proof-metadata.txt",
            )

        doFirst {
            fun commandOutput(
                vararg command: String,
            ): String =
                runCatching {
                    val process =
                        ProcessBuilder(
                            *command,
                        )
                            .directory(rootDir)
                            .redirectErrorStream(true)
                            .start()
                    val output =
                        process.inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }
                            .trim()
                    val exit =
                        process.waitFor()
                    if (exit == 0) {
                        output
                    } else {
                        "UNAVAILABLE(exit=$exit): $output"
                    }
                }.getOrElse { failure ->
                    "UNAVAILABLE: " +
                        failure::class.java.simpleName +
                        ": " +
                        failure.message.orEmpty()
                }

            val target =
                metadata.get().asFile
            target.parentFile.mkdirs()
            target.writeText(
                buildString {
                    appendLine(
                        "[VXG RealForever]",
                    )
                    appendLine(
                        "proofRef=vexlife.test-foundation.host-proof/v1",
                    )
                    appendLine(
                        "gitHead=" +
                            commandOutput(
                                "git",
                                "rev-parse",
                                "HEAD",
                            ),
                    )
                    appendLine(
                        "gitStatus<<EOF",
                    )
                    appendLine(
                        commandOutput(
                            "git",
                            "status",
                            "--porcelain=v1",
                        ),
                    )
                    appendLine("EOF")
                    appendLine(
                        "javaVersion=" +
                            System.getProperty(
                                "java.version",
                            ),
                    )
                    appendLine(
                        "javaVendor=" +
                            System.getProperty(
                                "java.vendor",
                            ),
                    )
                    appendLine(
                        "osName=" +
                            System.getProperty(
                                "os.name",
                            ),
                    )
                    appendLine(
                        "osVersion=" +
                            System.getProperty(
                                "os.version",
                            ),
                    )
                    appendLine(
                        "osArch=" +
                            System.getProperty(
                                "os.arch",
                            ),
                    )
                },
            )
        }

        from(metadata) {
            into("meta")
        }
        from(
            layout.buildDirectory.dir(
                "reports/kover",
            ),
        ) {
            into("coverage/kover")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/reports/vex",
            ),
        ) {
            into("journey/coverage")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/test-results/testDebugUnitTest",
            ),
        ) {
            into("tests/app-jvm")
        }
        from(
            layout.projectDirectory.dir(
                "architecture/behavior/build/test-results",
            ),
        ) {
            into("tests/architecture-behavior")
        }
        from(
            layout.projectDirectory.dir(
                "architecture/elements/build/test-results",
            ),
        ) {
            into("tests/architecture-elements")
        }
        from(
            layout.projectDirectory.dir(
                "architecture/logging/build/test-results",
            ),
        ) {
            into("tests/architecture-logging")
        }
        from(
            layout.projectDirectory.dir(
                "architecture/relay/build/test-results",
            ),
        ) {
            into("tests/architecture-relay")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/reports/tests/updateDebugScreenshotTest",
            ),
        ) {
            into("visual/update-report")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/reports/screenshotTest/preview/debug",
            ),
        ) {
            into("visual/validation-report")
        }
        from(
            layout.projectDirectory.dir(
                "app/src/screenshotTestDebug/reference",
            ),
        ) {
            into("visual/reference")
        }
        from(
            layout.projectDirectory.dir(
                "app/schemas",
            ),
        ) {
            into("room/schemas-candidate")
        }
        from(
            layout.projectDirectory.dir(
                "build/reports/problems",
            ),
        ) {
            into("diagnostics/problems")
        }
        from(
            layout.projectDirectory.dir(
                "build/reports/configuration-cache",
            ),
        ) {
            into("diagnostics/configuration-cache")
        }
    }

vexTestFoundationProofBundle.configure {
    mustRunAfter(
        "vexVerifyLocal",
        "vexCoverage",
        ":app:updateDebugScreenshotTest",
        "vexVisualValidate",
    )
}

