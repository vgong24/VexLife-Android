package com.vextreme.vexlife.android.testing.crawler

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.testing.journey.VexJourneyExecutionObserver
import com.vextreme.vexlife.android.testing.journey.VexJourneyStepObservation
import java.io.File

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
    private val rootDirectory: File by lazy {
        val targetContext =
            InstrumentationRegistry.getInstrumentation().targetContext
        val external =
            requireNotNull(
                targetContext.getExternalFilesDir("vex-screen-crawler"),
            ) {
                "crawler external files directory is unavailable"
            }

        File(
            external,
            "${case.sourceRef.safePath()}/${case.caseRef.safePath()}",
        ).also(File::mkdirs)
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

            VexCrawlerCapturePolicy.EVERY_SEMANTIC_STEP ->
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

        println(
            "Vex ScreenCrawler artifacts: ${rootDirectory.absolutePath}",
        )
    }

    private fun shouldCaptureScreen(screenId: String): Boolean =
        case.selectedScreenIds.isEmpty() ||
            screenId in case.selectedScreenIds

    private fun capture(observation: VexJourneyStepObservation) {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()
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
            ) + "\n",
        )
    }

    private fun VexJourneyStepObservation.toManifestLine(
        screenshot: File,
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
