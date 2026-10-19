package com.vextreme.vexlife.android.testing.crossplatform

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Test-only evidence and host-synchronization bridge for lived cross-platform proofs.
 *
 * Product semantics remain with each participating product. This helper owns only:
 * - explicit fixture/run/phase selection;
 * - append-only Android checkpoint evidence;
 * - checkpoint screenshots through AndroidX PlatformTestStorage;
 * - one app-private host marker namespace scoped to the exact run.
 *
 * The canonical cross-platform relationship map lives in Vextreme-SDK, not here.
 */
internal class VexCrossPlatformEvidence(
    context: Context,
    private val arguments: Bundle,
    val fixtureRef: String,
) {
    val runRef: String =
        requireNotNull(
            arguments
                .getString(ARG_RUN_REF)
                ?.trim()
                ?.takeIf(String::isNotBlank),
        ) {
            "$ARG_RUN_REF is required for cross-platform execution"
        }

    private val root =
        File(
            context.filesDir,
            "cross-platform",
        )

    private val safeFixtureRef = fixtureRef.safePath()
    private val safeRunRef = runRef.safePath()

    private val runRoot =
        File(
            root,
            "$safeFixtureRef/$safeRunRef",
        )

    val evidenceFile =
        File(
            runRoot,
            "android-checkpoints.jsonl",
        )

    private val storage =
        PlatformTestStorageRegistry.getInstance()

    private var lastCheckpointRef: String? =
        null

    val enabled: Boolean
        get() =
            arguments
                .getString(ARG_FIXTURE)
                ?.trim() == fixtureRef

    fun phaseSelected(
        expectedPhase: String,
    ): Boolean =
        enabled &&
            arguments
                .getString(ARG_PHASE)
                ?.trim()
                ?.uppercase() == expectedPhase.uppercase()

    fun argument(
        key: String,
    ): String? =
        arguments
            .getString(key)
            ?.trim()
            ?.takeIf(String::isNotBlank)

    /**
     * Phase A is the only phase allowed to clear this exact run's prior evidence.
     *
     * The root is already scoped by fixtureRef + runRef, so unrelated test runs and ordinary
     * product state are never deleted here.
     */
    fun resetRunEvidence() {
        runRoot.deleteRecursively()
        check(runRoot.mkdirs()) {
            "unable to create cross-platform evidence root: ${runRoot.absolutePath}"
        }
    }

    fun checkpoint(
        phaseRef: String,
        status: String,
        details: Map<String, Any?> = emptyMap(),
    ) {
        require(phaseRef.isNotBlank())
        require(
            status in
                setOf(
                    "PASS",
                    "FAIL",
                    "WAITING",
                    "OBSERVED",
                ),
        )

        runRoot.mkdirs()

        val json =
            JSONObject()
                .put(
                    "schemaVersion",
                    "vexlife.cross-platform-checkpoint/v2",
                )
                .put(
                    "fixtureRef",
                    fixtureRef,
                )
                .put(
                    "runRef",
                    runRef,
                )
                .put(
                    "phaseRef",
                    phaseRef,
                )
                .put(
                    "status",
                    status,
                )
                .put(
                    "observedAtEpochMs",
                    System.currentTimeMillis(),
                )

        val detailJson = JSONObject()
        details
            .toSortedMap()
            .forEach { (key, value) ->
                detailJson.put(
                    key,
                    value,
                )
            }
        json.put(
            "details",
            detailJson,
        )

        evidenceFile.appendText(
            json.toString() + "\n",
        )

        Log.i(
            LOG_TAG,
            "CHECKPOINT run=$runRef phase=$phaseRef status=$status details=$detailJson",
        )
    }

    /**
     * Capture one lived visual checkpoint without introducing another app-walk route.
     *
     * ScreenCrawler remains the reusable screenshot/evidence precedent. This helper deliberately
     * reuses the same AndroidX PlatformTestStorage transfer boundary for the cross-platform run.
     */
    fun screenshot(
        phaseRef: String,
    ): ScreenshotEvidence {
        require(phaseRef.isNotBlank())

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        instrumentation.waitForIdleSync()

        val bitmap =
            requireNotNull(
                instrumentation
                    .uiAutomation
                    .takeScreenshot(),
            ) {
                "UiAutomation.takeScreenshot() returned null for $phaseRef"
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
                        "failed to encode cross-platform screenshot for $phaseRef"
                    }
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }

        check(bytes.isNotEmpty()) {
            "cross-platform screenshot encoded to zero bytes for $phaseRef"
        }

        val outputPath =
            "vex-cross-platform/" +
                safeFixtureRef +
                "/" +
                safeRunRef +
                "/screenshots/" +
                phaseRef.safePath() +
                ".png"

        storage
            .openOutputFile(outputPath)
            .buffered()
            .use { output ->
                output.write(bytes)
            }

        val privateScreenshot =
            File(
                runRoot,
                "screenshots/" +
                    phaseRef.safePath() +
                    ".png",
            )
        privateScreenshot
            .parentFile
            ?.mkdirs()
        privateScreenshot
            .writeBytes(
                bytes,
            )

        val evidence =
            ScreenshotEvidence(
                outputPath = outputPath,
                privatePath =
                    privateScreenshot
                        .relativeTo(
                            root.parentFile
                                ?: error("cross-platform evidence root has no parent"),
                        )
                        .path,
                byteLength = bytes.size,
                sha256 = bytes.sha256(),
            )

        checkpoint(
            phaseRef = "$phaseRef.screenshot",
            status = "PASS",
            details =
                mapOf(
                    "outputPath" to evidence.outputPath,
                    "privatePath" to evidence.privatePath,
                    "byteLength" to evidence.byteLength,
                    "sha256" to evidence.sha256,
                    "uri" to
                        storage
                            .getOutputFileUri(outputPath)
                            .toString(),
                ),
        )

        return evidence
    }

    fun failure(
        failure: Throwable,
    ) {
        checkpoint(
            phaseRef = "android.failure",
            status = "FAIL",
            details =
                mapOf(
                    "lastCheckpointRef" to
                        lastCheckpointRef,
                    "type" to
                        failure
                            .javaClass
                            .name,
                    "message" to
                        failure
                            .message,
                ),
        )
    }

    fun waitForHostMarker(
        markerRef: String,
        timeoutMs: Long,
    ): String {
        require(markerRef.isNotBlank())
        require(timeoutMs > 0L)

        val marker = hostMarker(markerRef)
        val deadline =
            SystemClock.elapsedRealtime() +
                timeoutMs

        while (
            SystemClock.elapsedRealtime() <
            deadline
        ) {
            if (marker.isFile) {
                return marker
                    .readText()
                    .trim()
            }

            SystemClock.sleep(
                200L,
            )
        }

        error(
            "Timed out waiting for cross-platform host marker " +
                "$markerRef for run $runRef",
        )
    }

    /**
     * Relative path deliberately exposed for host orchestration documentation/evidence.
     *
     * The host still needs the qualified Android package/run-as boundary to write it.
     */
    fun hostMarkerRelativePath(
        markerRef: String,
    ): String =
        hostMarker(markerRef)
            .relativeTo(
                root.parentFile
                    ?: error("cross-platform evidence root has no parent"),
            )
            .path

    private fun hostMarker(
        markerRef: String,
    ): File =
        File(
            runRoot,
            "host.${markerRef.safePath()}",
        )

    data class ScreenshotEvidence(
        val outputPath: String,
        val privatePath: String,
        val byteLength: Int,
        val sha256: String,
    )

    companion object {
        const val ARG_FIXTURE =
            "vexCrossPlatformFixture"

        const val ARG_RUN_REF =
            "vexCrossPlatformRunRef"

        const val ARG_PHASE =
            "vexCrossPlatformPhase"
    }
}

private fun String.safePath(): String =
    replace(
        Regex("[^A-Za-z0-9._-]+"),
        "_",
    )
        .trim('_')
        .ifBlank { "unknown" }

private fun ByteArray.sha256(): String {
    val digest =
        MessageDigest
            .getInstance("SHA-256")
            .digest(this)

    return digest.joinToString("") { byte ->
        "%02x".format(byte)
    }
}

private const val LOG_TAG =
    "VexCrossPlatform"

// [VXG RealForever]
