package com.vextreme.vexlife.android.testing.crawler

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.vextreme.vexlife.android.testing.journey.VexJourneyExecutionObserver
import com.vextreme.vexlife.android.testing.journey.VexJourneyStepObservation
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Writes crawler evidence through AndroidX PlatformTestStorage.
 *
 * Evidence layout is configuration-first for handoff/ZIP delivery:
 *
 *   vex-screen-crawler/<source>/<locale>/<font>/
 *     screenshots/  <- all PNGs for this language/font batch
 *     cases/        <- per-scenario manifests/start/complete markers
 *
 * Screenshot semantic identity is stable across locale/font configurations; locale/font only appear
 * as the final filename suffix and folder grouping.
 */
class VexScreenCrawlerEvidenceObserver(
    private val case: VexScreenCrawlerCase,
) : VexJourneyExecutionObserver {
    private val capturedScreens = linkedSetOf<String>()
    private var capturedScreenshotCount = 0

    private val storage =
        PlatformTestStorageRegistry.getInstance()

    private val configurationRoot =
        "vex-screen-crawler/" +
            case.sourceRef.safePath() +
            "/" +
            case.locale.id.safePath() +
            "/" +
            case.font.id.safePath()

    private val screenshotsRoot =
        "$configurationRoot/screenshots"

    private val scenarioRoot =
        "$configurationRoot/cases/" +
            case.scenarioRef.safePath()

    private val manifestPath =
        "$scenarioRoot/manifest.jsonl"

    init {
        writeText(
            path = "$scenarioRoot/run-started.txt",
            value = buildString {
                appendLine("scenarioRef=${case.scenarioRef}")
                appendLine("caseRef=${case.caseRef}")
                appendLine("configurationRef=${case.configurationRef}")
                appendLine("sourceRef=${case.sourceRef}")
                appendLine(
                    "targetPackage=${InstrumentationRegistry.getInstrumentation().targetContext.packageName}",
                )
                appendLine("deviceModel=${Build.MODEL}")
                appendLine("sdkInt=${Build.VERSION.SDK_INT}")
                appendLine("storage=PlatformTestStorage")
                appendLine("configurationRoot=$configurationRoot")
            },
        )

        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "START scenario=${case.scenarioRef} " +
                "configuration=${case.configurationRef} " +
                "case=${case.caseRef} " +
                "outputRoot=$configurationRoot",
        )
    }

    override fun afterStep(observation: VexJourneyStepObservation) {
        if (!shouldCaptureScreen(observation.screenId)) return

        when (case.capturePolicy) {
            VexCrawlerCapturePolicy.SCREEN_ONCE -> {
                if (capturedScreens.add(observation.screenId)) {
                    capture(observation)
                }
            }

            VexCrawlerCapturePolicy.EVERY_STEP ->
                capture(observation)

            VexCrawlerCapturePolicy.FINAL_ONLY ->
                Unit
        }
    }

    override fun afterJourney(
        caseLabel: String,
        finalScreenId: String,
    ) {
        if (
            case.capturePolicy == VexCrawlerCapturePolicy.FINAL_ONLY &&
            shouldCaptureScreen(finalScreenId)
        ) {
            capture(
                VexJourneyStepObservation(
                    stepIndex = Int.MAX_VALUE,
                    stepKind = "final",
                    screenId = finalScreenId,
                    semanticRef = null,
                ),
            )
        }

        check(capturedScreenshotCount > 0) {
            "ScreenCrawler executed but captured ZERO screenshots for ${case.caseRef}. " +
                "A crawler run must never pass without evidence."
        }

        writeText(
            path = "$scenarioRoot/run-complete.txt",
            value = buildString {
                appendLine("scenarioRef=${case.scenarioRef}")
                appendLine("caseRef=${case.caseRef}")
                appendLine("configurationRef=${case.configurationRef}")
                appendLine("screenshots=$capturedScreenshotCount")
                appendLine("manifest=manifest.jsonl")
                appendLine("result=COMPLETE")
            },
        )

        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "COMPLETE scenario=${case.scenarioRef} " +
                "configuration=${case.configurationRef} " +
                "screenshots=$capturedScreenshotCount",
        )
        println(
            "Vex ScreenCrawler captured $capturedScreenshotCount screenshot(s).",
        )
        println(
            "Vex ScreenCrawler configuration batch: $configurationRoot",
        )
        println(
            "Collect from repo root with:",
        )
        println(
            "  macOS/Linux: bash tools/pull-vex-screen-crawler.sh",
        )
        println(
            "  Windows: powershell -ExecutionPolicy Bypass -File " +
                ".\\tools\\pull-vex-screen-crawler.ps1",
        )
    }

    private fun shouldCaptureScreen(screenId: String): Boolean =
        case.selectedScreenIds.isEmpty() ||
            screenId in case.selectedScreenIds

    private fun capture(observation: VexJourneyStepObservation) {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        instrumentation.waitForIdleSync()

        val bitmap =
            requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
                "UiAutomation.takeScreenshot() returned null"
            }

        val bytes =
            try {
                ByteArrayOutputStream().use { output ->
                    check(
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            output,
                        ),
                    ) {
                        "failed to encode crawler screenshot for ${case.caseRef}"
                    }
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }

        check(bytes.isNotEmpty()) {
            "crawler screenshot encoded to zero bytes for ${case.caseRef}"
        }

        val identity =
            case.evidenceIdentity(
                screenId = observation.screenId,
                stepIndex = observation.stepIndex,
                stepKind = observation.stepKind,
                semanticRef = observation.semanticRef,
            )

        val stablePrefix =
            identity.evidenceRef.safePath()

        val filename =
            stablePrefix +
                "__" + case.locale.id.safePath() +
                "__" + case.font.id.safePath() +
                ".png"

        val screenshotPath =
            "$screenshotsRoot/$filename"

        storage.openOutputFile(screenshotPath).buffered().use { output ->
            output.write(bytes)
        }

        val screenshotSha256 =
            bytes.sha256()

        storage.openOutputFile(
            manifestPath,
            true,
        ).bufferedWriter().use { writer ->
            writer.appendLine(
                observation.toManifestLine(
                    identity = identity,
                    screenshot = filename,
                    screenshotSha256 = screenshotSha256,
                    screenshotBytes = bytes.size,
                ),
            )
        }

        capturedScreenshotCount += 1

        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "CAPTURE scenario=${identity.scenarioRef} " +
                "evidence=${identity.evidenceRef} " +
                "configuration=${case.configurationRef} " +
                "output=$screenshotPath " +
                "bytes=${bytes.size} " +
                "uri=${storage.getOutputFileUri(screenshotPath)}",
        )
    }

    private fun writeText(
        path: String,
        value: String,
    ) {
        storage.openOutputFile(path).bufferedWriter().use { writer ->
            writer.write(value)
        }
    }

    private fun VexJourneyStepObservation.toManifestLine(
        identity: VexCrawlerEvidenceIdentity,
        screenshot: String,
        screenshotSha256: String,
        screenshotBytes: Int,
    ): String =
        buildString {
            append("{")
            appendJson("scenarioRef", identity.scenarioRef)
            append(",")
            appendJson("evidenceRef", identity.evidenceRef)
            append(",")
            appendJson("configurationRef", case.configurationRef)
            append(",")
            appendJson("caseRef", case.caseRef)
            append(",")
            appendJson("sourceRef", case.sourceRef)
            append(",")
            appendJson("shardId", case.shardId)
            append(",")
            appendJson("journeyId", case.journey.id)
            append(",")
            appendJson("localeId", case.locale.id)
            append(",")
            appendJson("localeTag", case.locale.localeTag)
            append(",")
            appendJson("fontId", case.font.id)
            append(",\"fontScale\":")
            append(case.font.scale)
            append(",")
            appendJson("capturePolicy", case.capturePolicy.name)
            append(",")
            appendJson("screenId", screenId)
            append(",\"stepIndex\":")
            append(stepIndex)
            append(",")
            appendJson("stepKind", stepKind)
            append(",")
            appendJson("semanticRef", semanticRef)
            append(",")
            appendJson("deviceModel", Build.MODEL)
            append(",\"sdkInt\":")
            append(Build.VERSION.SDK_INT)
            append(",")
            appendJson("file", "screenshots/$screenshot")
            append(",\"bytes\":")
            append(screenshotBytes)
            append(",")
            appendJson("sha256", screenshotSha256)
            append("}")
        }
}

private fun StringBuilder.appendJson(
    key: String,
    value: String?,
) {
    append("\"")
    append(key.jsonEscape())
    append("\":")
    if (value == null) {
        append("null")
    } else {
        append("\"")
        append(value.jsonEscape())
        append("\"")
    }
}

private fun String.safePath(): String =
    replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "unknown" }

private fun String.jsonEscape(): String =
    buildString(length) {
        this@jsonEscape.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

private fun ByteArray.sha256(): String {
    val digest =
        MessageDigest.getInstance("SHA-256")
            .digest(this)

    return digest.joinToString("") { byte ->
        "%02x".format(byte)
    }
}

private const val SCREEN_CRAWLER_LOG_TAG =
    "VexScreenCrawler"
