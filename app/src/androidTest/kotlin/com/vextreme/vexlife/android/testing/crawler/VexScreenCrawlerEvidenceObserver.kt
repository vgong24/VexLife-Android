package com.vextreme.vexlife.android.testing.crawler

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.testing.journey.VexJourneyExecutionObserver
import com.vextreme.vexlife.android.testing.journey.VexJourneyStepObservation
import java.io.File
import java.security.MessageDigest

/**
 * Writes crawler screenshots and a JSONL manifest without retaining Activity/Compose/Bitmap objects.
 *
 * The target app context is acquired only to resolve the output directory. Each screenshot Bitmap is
 * written and recycled immediately; retained state is strings/files/IDs only.
 */
class VexScreenCrawlerEvidenceObserver(
    private val case: VexScreenCrawlerCase,
) : VexJourneyExecutionObserver {
    private val capturedScreens = linkedSetOf<String>()
    private var capturedScreenshotCount = 0
    private val crawlerRootDirectory: File by lazy {
        val targetContext =
            InstrumentationRegistry.getInstrumentation().targetContext
        requireNotNull(
            targetContext.getExternalFilesDir("vex-screen-crawler"),
        ) {
            "crawler external files directory is unavailable"
        }
    }

    private val rootDirectory: File by lazy {
        File(
            crawlerRootDirectory,
            "${case.sourceRef.safePath()}/${case.caseRef.safePath()}",
        ).also { directory ->
            if (directory.exists()) {
                check(directory.deleteRecursively()) {
                    "failed to clear stale crawler case artifacts: " +
                        directory.absolutePath
                }
            }
            check(directory.mkdirs() || directory.isDirectory) {
                "failed to create crawler case artifact directory: " +
                    directory.absolutePath
            }
        }
    }

    private val manifest: File
        get() = File(rootDirectory, "manifest.jsonl")

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
        check(manifest.isFile && manifest.length() > 0L) {
            "ScreenCrawler screenshots exist but manifest is missing/empty for ${case.caseRef}"
        }

        println(
            "Vex ScreenCrawler captured $capturedScreenshotCount screenshot(s).",
        )
        println(
            "Vex ScreenCrawler case artifacts: ${rootDirectory.absolutePath}",
        )
        println(
            "Vex ScreenCrawler pull from repo root (macOS/Linux): " +
                "bash tools/pull-vex-screen-crawler.sh",
        )
        println(
            "Vex ScreenCrawler pull from repo root (Windows PowerShell): " +
                "powershell -ExecutionPolicy Bypass -File " +
                ".\\\\tools\\\\pull-vex-screen-crawler.ps1",
        )
    }

    private fun shouldCaptureScreen(screenId: String): Boolean =
        case.selectedScreenIds.isEmpty() ||
            screenId in case.selectedScreenIds

    private fun capture(observation: VexJourneyStepObservation) {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        // Compose may already be idle while the device has a pending draw. Wait at the Android
        // instrumentation boundary immediately before capturing the physical evidence frame.
        instrumentation.waitForIdleSync()

        val bitmap =
            requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
                "UiAutomation.takeScreenshot() returned null"
            }

        val stepLabel =
            if (observation.stepIndex == Int.MAX_VALUE) {
                "final"
            } else {
                observation.stepIndex
                    .toString()
                    .padStart(3, '0')
            }

        val filename =
            listOf(
                stepLabel,
                observation.screenId.safePath(),
                observation.stepKind.safePath(),
            ).joinToString("__") + ".png"

        val screenshot = File(rootDirectory, filename)

        try {
            screenshot.outputStream().buffered().use { output ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        output,
                    ),
                ) {
                    "failed to encode crawler screenshot: ${screenshot.absolutePath}"
                }
            }
        } finally {
            bitmap.recycle()
        }

        manifest.appendText(
            observation.toManifestLine(
                screenshot = screenshot,
                screenshotSha256 = screenshot.sha256(),
            ) + "\n",
        )

        check(screenshot.isFile && screenshot.length() > 0L) {
            "crawler screenshot was not written: ${screenshot.absolutePath}"
        }
        capturedScreenshotCount += 1
    }

    private fun VexJourneyStepObservation.toManifestLine(
        screenshot: File,
        screenshotSha256: String,
    ): String =
        buildString {
            append("{")
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
            appendJson("file", screenshot.name)
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


private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte ->
        "%02x".format(byte)
    }
}
