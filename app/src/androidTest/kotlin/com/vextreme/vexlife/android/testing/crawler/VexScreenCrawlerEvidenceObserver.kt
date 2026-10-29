package com.vextreme.vexlife.android.testing.crawler

import android.graphics.Bitmap
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.vextreme.vexlife.android.testing.journey.VexJourneyExecutionObserver
import com.vextreme.vexlife.android.testing.journey.VexJourneyStepObservation
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Writes crawler screenshots through AndroidX PlatformTestStorage.
 *
 * Delivery layout is configuration-first and intentionally screenshot-only:
 *
 *   vex-screen-crawler/<source>/<locale>/<font>/screenshots/
 *
 * Filename identity remains stable across locale/font runs; the varying configuration is appended at
 * the end so equivalent semantic frames can be matched before opening the image.
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

    init {
        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "START scenario=${case.scenarioRef} " +
                "configuration=${case.configurationRef} " +
                "case=${case.caseRef} " +
                "outputRoot=$screenshotsRoot",
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

        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "COMPLETE scenario=${case.scenarioRef} " +
                "configuration=${case.configurationRef} " +
                "screenshots=$capturedScreenshotCount " +
                "outputRoot=$screenshotsRoot",
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
                observation,
            )

        val filename =
            identity.evidenceRef.safePath() +
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

        capturedScreenshotCount += 1

        Log.i(
            SCREEN_CRAWLER_LOG_TAG,
            "CAPTURE scenario=${identity.scenarioRef} " +
                "evidence=${identity.evidenceRef} " +
                "configuration=${case.configurationRef} " +
                "output=$screenshotPath " +
                "bytes=${bytes.size} " +
                "sha256=$screenshotSha256 " +
                "uri=${storage.getOutputFileUri(screenshotPath)}",
        )
    }
}

private fun String.safePath(): String =
    replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "unknown" }

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
