import org.gradle.api.tasks.bundling.Zip

// [VXG RealForever]
//
// Repository-owned execution surface for docs/foundation/test-proof/README.md.
// Keep the root build file small; this script owns only test/proof orchestration and evidence
// collection, never feature semantics.

// Code coverage communication is a projection of Kover truth, not another coverage engine.
val vexCoverageAwarenessThresholdPercent = 90.0

data class VexCoverageCounter(
    val missed: Long,
    val covered: Long,
) {
    val total: Long
        get() = missed + covered

    val percent: Double
        get() =
            if (total <= 0L) {
                0.0
            } else {
                covered.toDouble() * 100.0 / total.toDouble()
            }
}

data class VexCoverageClassExposure(
    val className: String,
    val missedLines: Long,
    val coveredLines: Long,
) {
    val totalLines: Long
        get() = missedLines + coveredLines

    val percent: Double
        get() =
            if (totalLines <= 0L) {
                0.0
            } else {
                coveredLines.toDouble() * 100.0 / totalLines.toDouble()
            }
}

fun vexCoverageCounters(
    reportFile: java.io.File,
): Map<String, VexCoverageCounter> {
    val factory =
        javax.xml.parsers.DocumentBuilderFactory
            .newInstance()
            .apply {
                setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true,
                )
                setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false,
                )
                setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false,
                )
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
    val root =
        factory
            .newDocumentBuilder()
            .parse(reportFile)
            .documentElement
    val counters =
        mutableMapOf<String, VexCoverageCounter>()

    for (index in 0 until root.childNodes.length) {
        val element =
            root.childNodes.item(index)
                as? org.w3c.dom.Element
                ?: continue
        if (element.tagName != "counter") {
            continue
        }
        val type =
            element.getAttribute("type")
        if (type.isBlank()) {
            continue
        }
        counters[type] =
            VexCoverageCounter(
                missed =
                    element
                        .getAttribute("missed")
                        .toLong(),
                covered =
                    element
                        .getAttribute("covered")
                        .toLong(),
            )
    }

    return counters
}

fun vexCoverageDirectCounter(
    element: org.w3c.dom.Element,
    type: String,
): VexCoverageCounter? {
    for (index in 0 until element.childNodes.length) {
        val child =
            element.childNodes.item(index)
                as? org.w3c.dom.Element
                ?: continue
        if (
            child.tagName == "counter" &&
            child.getAttribute("type") == type
        ) {
            return VexCoverageCounter(
                missed =
                    child
                        .getAttribute("missed")
                        .toLong(),
                covered =
                    child
                        .getAttribute("covered")
                        .toLong(),
            )
        }
    }
    return null
}

fun vexCoverageClassExposures(
    reportFile: java.io.File,
): List<VexCoverageClassExposure> {
    val factory =
        javax.xml.parsers.DocumentBuilderFactory
            .newInstance()
            .apply {
                setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true,
                )
                setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false,
                )
                setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false,
                )
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
    val root =
        factory
            .newDocumentBuilder()
            .parse(reportFile)
            .documentElement
    val exposures =
        mutableListOf<VexCoverageClassExposure>()
    val packages =
        root.getElementsByTagName(
            "package",
        )

    for (
        packageIndex in
        0 until packages.length
    ) {
        val packageElement =
            packages.item(packageIndex)
                as? org.w3c.dom.Element
                ?: continue

        for (
            childIndex in
            0 until packageElement.childNodes.length
        ) {
            val classElement =
                packageElement.childNodes.item(childIndex)
                    as? org.w3c.dom.Element
                    ?: continue
            if (classElement.tagName != "class") {
                continue
            }

            val line =
                vexCoverageDirectCounter(
                    classElement,
                    "LINE",
                )
                    ?: continue
            if (
                line.total <= 0L ||
                line.percent >=
                vexCoverageAwarenessThresholdPercent
            ) {
                continue
            }

            exposures +=
                VexCoverageClassExposure(
                    className =
                        classElement
                            .getAttribute("name")
                            .replace(
                                '/',
                                '.',
                            ),
                    missedLines =
                        line.missed,
                    coveredLines =
                        line.covered,
                )
        }
    }

    return exposures
        .sortedWith(
            compareByDescending<
                VexCoverageClassExposure
            > {
                it.missedLines
            }
                .thenBy {
                    it.percent
                }
                .thenBy {
                    it.className
                },
        )
}

fun vexCoverageJsonString(
    value: String,
): String =
    value
        .replace(
            "\\",
            "\\\\",
        )
        .replace(
            "\"",
            "\\\"",
        )

fun vexCoverageAwareness(
    linePercent: Double,
): String =
    if (
        linePercent >=
        vexCoverageAwarenessThresholdPercent
    ) {
        "GREEN"
    } else {
        "YELLOW"
    }

fun vexCoveragePercentText(
    percent: Double,
): String =
    java.lang.String.format(
        java.util.Locale.US,
        "%.2f",
        percent,
    )


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
 *
 * Result inspection is a finalizer of connectedDebugAndroidTest rather than only a downstream
 * dependency. That distinction is intentional: when instrumentation fails, the diagnostic reader
 * still runs and preserves the exact failing testcase identity instead of collapsing the proof to
 * "There were failing tests."
 */
val connectedDeviceResultRoot =
    rootProject.file(
        "app/build/outputs/androidTest-results/connected/debug",
    )
val connectedDeviceReportRoot =
    rootProject.file(
        "app/build/reports/androidTests/connected/debug",
    )
val connectedDeviceSummary =
    layout.buildDirectory.file(
        "vex/test-foundation/device-result-summary.txt",
    )

val verifyVexDeviceResults =
    tasks.register("verifyVexDeviceResults") {
        notCompatibleWithConfigurationCache(
            "Reads Android connected-test XML emitted by the device execution task.",
        )
        group = "verification"
        description =
            "Report exact connected Android test results and fail closed on empty/hidden failures."

        mustRunAfter(":app:connectedDebugAndroidTest")

        doLast {
            val connectedTask =
                project(":app")
                    .tasks
                    .findByName(
                        "connectedDebugAndroidTest",
                    )
            val connectedFailure =
                connectedTask
                    ?.state
                    ?.failure

            val xmlFiles =
                connectedDeviceResultRoot
                    .takeIf { it.isDirectory }
                    ?.walkTopDown()
                    ?.filter { file ->
                        file.isFile &&
                            file.extension.equals(
                                "xml",
                                ignoreCase = true,
                            )
                    }
                    ?.sortedBy { file ->
                        file.relativeTo(
                            connectedDeviceResultRoot,
                        ).path
                    }
                    ?.toList()
                    .orEmpty()

            var executedTests = 0
            var skippedTests = 0
            val failedTests =
                mutableListOf<String>()

            xmlFiles.forEach { file ->
                val document =
                    javax.xml.parsers.DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(file)
                val testCases =
                    document.getElementsByTagName(
                        "testcase",
                    )

                executedTests +=
                    testCases.length

                for (
                    index in
                    0 until testCases.length
                ) {
                    val testCase =
                        testCases.item(index)
                            as? org.w3c.dom.Element
                            ?: continue
                    var failureElement:
                        org.w3c.dom.Element? =
                        null
                    var skipped = false

                    val children =
                        testCase.childNodes
                    for (
                        childIndex in
                        0 until children.length
                    ) {
                        val child =
                            children.item(childIndex)
                                as? org.w3c.dom.Element
                                ?: continue
                        when (
                            child.tagName
                        ) {
                            "failure",
                            "error",
                            ->
                                if (
                                    failureElement == null
                                ) {
                                    failureElement =
                                        child
                                }

                            "skipped" ->
                                skipped = true
                        }
                    }

                    if (skipped) {
                        skippedTests += 1
                    }

                    failureElement?.let { failure ->
                        val className =
                            testCase
                                .getAttribute(
                                    "classname",
                                )
                                .ifBlank {
                                    "UNKNOWN_CLASS"
                                }
                        val testName =
                            testCase
                                .getAttribute(
                                    "name",
                                )
                                .ifBlank {
                                    "UNKNOWN_TEST"
                                }
                        val failureType =
                            failure
                                .getAttribute(
                                    "type",
                                )
                                .ifBlank {
                                    failure.tagName
                                }
                        val failureMessage =
                            failure
                                .getAttribute(
                                    "message",
                                )
                                .ifBlank {
                                    failure.textContent
                                }
                                .trim()
                                .take(4_000)

                        failedTests +=
                            buildString {
                                append(
                                    className,
                                )
                                append("#")
                                append(
                                    testName,
                                )
                                append(" type=")
                                append(
                                    failureType,
                                )
                                if (
                                    failureMessage.isNotBlank()
                                ) {
                                    appendLine()
                                    append(
                                        failureMessage,
                                    )
                                }
                            }
                    }
                }
            }

            val summary =
                buildString {
                    appendLine(
                        "[VXG RealForever]",
                    )
                    appendLine(
                        "proofRef=vexlife.device-result-summary/v1",
                    )
                    appendLine(
                        "connectedTaskFailed=" +
                            (connectedFailure != null),
                    )
                    appendLine(
                        "resultRoot=" +
                            connectedDeviceResultRoot
                                .relativeTo(
                                    rootProject.projectDir,
                                ),
                    )
                    appendLine(
                        "xmlFiles=" +
                            xmlFiles.size,
                    )
                    appendLine(
                        "executedTests=" +
                            executedTests,
                    )
                    appendLine(
                        "skippedTests=" +
                            skippedTests,
                    )
                    appendLine(
                        "failedTests=" +
                            failedTests.size,
                    )
                    failedTests
                        .forEachIndexed {
                                index,
                                failure,
                            ->
                            appendLine(
                                "failure[$index]<<EOF",
                            )
                            appendLine(
                                failure,
                            )
                            appendLine(
                                "EOF",
                            )
                        }
                }

            val summaryFile =
                connectedDeviceSummary
                    .get()
                    .asFile
            summaryFile
                .parentFile
                .mkdirs()
            summaryFile.writeText(
                summary,
            )

            logger.lifecycle(
                summary.trimEnd(),
            )

            failedTests.forEach { failure ->
                logger.error(
                    "Vex connected Android test failure:\n$failure",
                )
            }

            if (
                connectedFailure == null &&
                executedTests <= 0
            ) {
                throw GradleException(
                    "Connected Android proof produced zero instrumentation tests. " +
                        "See " +
                        summaryFile.relativeTo(
                            rootProject.projectDir,
                        ),
                )
            }

            if (
                connectedFailure == null &&
                failedTests.isNotEmpty()
            ) {
                throw GradleException(
                    "Connected Android task returned success but result XML contains " +
                        failedTests.size +
                        " failing test(s). See " +
                        summaryFile.relativeTo(
                            rootProject.projectDir,
                        ),
                )
            }

            if (
                connectedFailure != null &&
                xmlFiles.isEmpty()
            ) {
                logger.error(
                    "Connected Android execution failed before any JUnit XML was retained. " +
                        "Original Gradle failure remains authoritative; " +
                        "resultRoot=" +
                        connectedDeviceResultRoot.relativeTo(
                            rootProject.projectDir,
                        ),
                )
            }
        }
    }

project(":app")
    .tasks
    .matching { task ->
        task.name ==
            "connectedDebugAndroidTest"
    }
    .configureEach {
        doFirst {
            rootProject.delete(
                connectedDeviceResultRoot,
            )
            rootProject.delete(
                connectedDeviceReportRoot,
            )
            rootProject.delete(
                connectedDeviceSummary
                    .get()
                    .asFile,
            )
        }
        finalizedBy(
            verifyVexDeviceResults,
        )
    }

/**
 * Canonical connected-Android Journey/instrumentation proof.
 *
 * This lifecycle task depends only on connectedDebugAndroidTest.
 *
 * verifyVexDeviceResults is attached to the connected task exclusively through finalizedBy above.
 * Do not also declare the inspector as an ordinary dependency here: after a connected-test failure,
 * Gradle skips ordinary dependents but still executes finalizers. Keeping one relationship preserves
 * failure diagnostics on the exact aggregate command consumers actually run.
 */
tasks.register("vexVerifyDevice") {
    group = "verification"
    description =
        "Run a non-empty connected Android suite and preserve exact failure diagnostics."

    dependsOn(
        ":app:connectedDebugAndroidTest",
    )
}

/**
 * Deterministic contract proof for the small coverage communication layer.
 *
 * This proves the parser/awareness boundary itself without pretending Gradle orchestration belongs
 * inside the product's code-coverage percentage.
 */
val vexCoverageSummaryContract =
    tasks.register("vexCoverageSummaryContract") {
        group = "verification"
        description =
            "Prove coverage-summary parsing and 90% awareness classification."

        doLast {
            fun fixture(
                name: String,
                missedLines: Long,
                coveredLines: Long,
            ): java.io.File {
                val file =
                    temporaryDir.resolve(
                        "$name.xml",
                    )
                file.writeText(
                    """
                    <report name="fixture">
                      <package name="example">
                        <class name="example/LargeGap">
                          <counter type="LINE" missed="20" covered="10"/>
                        </class>
                        <class name="example/SmallGap">
                          <counter type="LINE" missed="3" covered="17"/>
                        </class>
                        <class name="example/Green">
                          <counter type="LINE" missed="1" covered="19"/>
                        </class>
                      </package>
                      <counter type="INSTRUCTION" missed="0" covered="1"/>
                      <counter type="BRANCH" missed="1" covered="9"/>
                      <counter type="LINE" missed="$missedLines" covered="$coveredLines"/>
                    </report>
                    """.trimIndent(),
                )
                return file
            }

            val green =
                vexCoverageCounters(
                    fixture(
                        name = "green",
                        missedLines = 10,
                        coveredLines = 90,
                    ),
                ).getValue("LINE")
            check(
                kotlin.math.abs(
                    green.percent - 90.0,
                ) < 0.0001,
            )
            check(
                vexCoverageAwareness(
                    green.percent,
                ) == "GREEN",
            )

            val yellow =
                vexCoverageCounters(
                    fixture(
                        name = "yellow",
                        missedLines = 11,
                        coveredLines = 89,
                    ),
                ).getValue("LINE")
            check(
                kotlin.math.abs(
                    yellow.percent - 89.0,
                ) < 0.0001,
            )
            check(
                vexCoverageAwareness(
                    yellow.percent,
                ) == "YELLOW",
            )

            val exposures =
                vexCoverageClassExposures(
                    fixture(
                        name = "exposure",
                        missedLines = 10,
                        coveredLines = 90,
                    ),
                )
            check(
                exposures.map {
                    it.className
                } ==
                    listOf(
                        "example.LargeGap",
                        "example.SmallGap",
                    ),
            )
            check(
                exposures.first().missedLines == 20L,
            )
        }
    }

val vexCoverageStatusMarkdown =
    layout.buildDirectory.file(
        "vex/test-foundation/coverage-status.md",
    )
val vexCoverageSummaryJson =
    layout.buildDirectory.file(
        "vex/test-foundation/coverage-summary.json",
    )

/**
 * One head-bound coverage projection for humans and dashboards.
 *
 * Kover remains the coverage producer. This task reads its JaCoCo-compatible XML and emits a tiny
 * latest-state projection. The 90% line target is awareness-only:
 *
 *   GREEN  >= 90%
 *   YELLOW <  90%
 *
 * Yellow never turns a successful test/coverage run into failure.
 */
val vexCoverageSummary =
    tasks.register("vexCoverageSummary") {
        group = "verification"
        description =
            "Publish one head-bound coverage summary with 90% awareness coloring."

        notCompatibleWithConfigurationCache(
            "Reads generated Kover XML and binds the summary to the current Git head.",
        )

        dependsOn(
            "koverXmlReport",
            vexCoverageSummaryContract,
        )

        doLast {
            val reportFile =
                layout.buildDirectory
                    .file(
                        "reports/kover/report.xml",
                    )
                    .get()
                    .asFile
            if (!reportFile.isFile) {
                throw GradleException(
                    "Kover XML report was not found at " +
                        reportFile.relativeTo(
                            rootProject.projectDir,
                        ),
                )
            }

            val counters =
                vexCoverageCounters(
                    reportFile,
                )
            val line =
                counters["LINE"]
                    ?: throw GradleException(
                        "Kover XML report has no root LINE counter.",
                    )
            if (line.total <= 0L) {
                throw GradleException(
                    "Kover XML report has zero executable lines.",
                )
            }
            val branch =
                counters["BRANCH"]
            val awareness =
                vexCoverageAwareness(
                    line.percent,
                )
            val emoji =
                if (awareness == "GREEN") {
                    "🟢"
                } else {
                    "🟡"
                }
            val lineText =
                vexCoveragePercentText(
                    line.percent,
                )
            val branchText =
                branch
                    ?.takeIf {
                        it.total > 0L
                    }
                    ?.let {
                        vexCoveragePercentText(
                            it.percent,
                        )
                    }
            val underCoveredClasses =
                vexCoverageClassExposures(
                    reportFile,
                )
            val topUnderCoveredClasses =
                underCoveredClasses.take(
                    10,
                )
            val gitHead =
                runCatching {
                    val process =
                        ProcessBuilder(
                            "git",
                            "rev-parse",
                            "HEAD",
                        )
                            .directory(
                                rootProject.projectDir,
                            )
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
                    if (
                        exit != 0 ||
                        !output.matches(
                            Regex(
                                "[0-9a-fA-F]{40,64}",
                            ),
                        )
                    ) {
                        throw GradleException(
                            "Coverage summary could not bind the current Git head.",
                        )
                    }
                    output
                }.getOrElse { failure ->
                    throw GradleException(
                        "Coverage summary requires an exact Git head.",
                        failure,
                    )
                }

            val markdown =
                buildString {
                    appendLine(
                        "<!-- VEX-COVERAGE-STATUS:START -->",
                    )
                    appendLine(
                        "### Code coverage",
                    )
                    append(
                        "$emoji **$lineText% lines**",
                    )
                    if (branchText != null) {
                        append(
                            " · $branchText% branches",
                        )
                    }
                    appendLine(
                        " · awareness target ≥ 90%",
                    )
                    appendLine(
                        "Head: `$gitHead`",
                    )
                    if (
                        topUnderCoveredClasses
                            .isNotEmpty()
                    ) {
                        appendLine()
                        appendLine(
                            "#### Largest under-covered classes",
                        )
                        appendLine(
                            "| Class | Lines | Missed |",
                        )
                        appendLine(
                            "| --- | ---: | ---: |",
                        )
                        topUnderCoveredClasses
                            .forEach { exposure ->
                                appendLine(
                                    "| `" +
                                        exposure.className +
                                        "` | 🟡 " +
                                        vexCoveragePercentText(
                                            exposure.percent,
                                        ) +
                                        "% | " +
                                        exposure.missedLines +
                                        " |",
                                )
                            }
                    }
                    appendLine(
                        "<!-- VEX-COVERAGE-STATUS:END -->",
                    )
                }

            val markdownFile =
                vexCoverageStatusMarkdown
                    .get()
                    .asFile
            markdownFile
                .parentFile
                .mkdirs()
            markdownFile.writeText(
                markdown,
            )

            val jsonFile =
                vexCoverageSummaryJson
                    .get()
                    .asFile
            jsonFile
                .parentFile
                .mkdirs()
            jsonFile.writeText(
                buildString {
                    appendLine("{")
                    appendLine(
                        "  \"schemaVersion\": \"vexlife.code-coverage-summary/v1\",",
                    )
                    appendLine(
                        "  \"gitHead\": \"$gitHead\",",
                    )
                    appendLine(
                        "  \"awarenessThresholdPercent\": 90.0,",
                    )
                    appendLine(
                        "  \"awareness\": \"$awareness\",",
                    )
                    appendLine(
                        "  \"lineCoveragePercent\": $lineText,",
                    )
                    appendLine(
                        "  \"lineCovered\": ${line.covered},",
                    )
                    appendLine(
                        "  \"lineMissed\": ${line.missed},",
                    )
                    if (branchText == null) {
                        appendLine(
                            "  \"branchCoveragePercent\": null,",
                        )
                    } else {
                        appendLine(
                            "  \"branchCoveragePercent\": $branchText,",
                        )
                    }
                    appendLine(
                        "  \"underCoveredClassCount\": " +
                            underCoveredClasses.size +
                            ",",
                    )
                    appendLine(
                        "  \"topUnderCoveredClasses\": [",
                    )
                    topUnderCoveredClasses
                        .forEachIndexed {
                                index,
                                exposure,
                            ->
                            val comma =
                                if (
                                    index ==
                                    topUnderCoveredClasses
                                        .lastIndex
                                ) {
                                    ""
                                } else {
                                    ","
                                }
                            appendLine(
                                "    {" +
                                    "\"className\": \"" +
                                    vexCoverageJsonString(
                                        exposure.className,
                                    ) +
                                    "\", " +
                                    "\"lineCoveragePercent\": " +
                                    vexCoveragePercentText(
                                        exposure.percent,
                                    ) +
                                    ", " +
                                    "\"missedLines\": " +
                                    exposure.missedLines +
                                    ", " +
                                    "\"coveredLines\": " +
                                    exposure.coveredLines +
                                    "}$comma",
                            )
                        }
                    appendLine(
                        "  ]",
                    )
                    appendLine("}")
                },
            )

            logger.lifecycle(
                markdown.trimEnd(),
            )
        }
    }

/**
 * Local JVM coverage report across the current app + architecture modules.
 *
 * Coverage remains diagnostic evidence. The summary's 90% target is a human awareness threshold,
 * not a build gate.
 */
tasks.register("vexCoverage") {
    group = "verification"
    description =
        "Run JVM tests and generate aggregate Kover reports plus one current coverage summary."

    dependsOn(
        "koverHtmlReport",
        vexCoverageSummary,
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
            vexCoverageStatusMarkdown,
        ) {
            into("coverage")
        }
        from(
            vexCoverageSummaryJson,
        ) {
            into("coverage")
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
            connectedDeviceSummary,
        ) {
            into("tests/android-connected")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/outputs/androidTest-results/connected/debug",
            ),
        ) {
            into("tests/android-connected/xml")
        }
        from(
            layout.projectDirectory.dir(
                "app/build/reports/androidTests/connected/debug",
            ),
        ) {
            into("tests/android-connected/report")
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
                "architecture/execution/build/test-results",
            ),
        ) {
            into("tests/architecture-execution")
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

