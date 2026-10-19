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

        lastCheckpointRef =
            phaseRef
    }

    /**
     * Capture one lived visual checkpoint without introducing another app-walk route.
     *
     * ScreenCrawler remains the reusable screenshot/evidence precedent. This helper deliberately
     * reuses the same AndroidX PlatformTestStorage transfer boundary for the cross-platform run.
     */
    fun screenshot(
        phaseRef: String,
        details: Map<String, Any?> = emptyMap(),
    ): ScreenshotEvidence {
        require(phaseRef.isNotBlank())

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val capture =
            runCatching {
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

                bytes
            }

        if (capture.isFailure) {
            val failure =
                requireNotNull(
                    capture.exceptionOrNull(),
                )
            val evidence =
                ScreenshotEvidence(
                    state = "UNAVAILABLE",
                    outputPath = null,
                    privatePath = null,
                    byteLength = null,
                    sha256 = null,
                    transferState = "NOT_REACHED",
                    transferFailureType = null,
                    transferFailureMessage = null,
                    captureFailureType =
                        failure
                            .javaClass
                            .name,
                    captureFailureMessage =
                        failure
                            .message,
                )

            checkpoint(
                phaseRef = "$phaseRef.screenshot",
                status = "OBSERVED",
                details =
                    details +
                        evidence.details(),
            )

            return evidence
        }

        val bytes =
            requireNotNull(
                capture.getOrNull(),
            )
        val sha256 =
            bytes.sha256()

        val privateScreenshot =
            File(
                runRoot,
                "screenshots/" +
                    phaseRef.safePath() +
                    ".png",
            )
        val privateWrite =
            runCatching {
                privateScreenshot
                    .parentFile
                    ?.mkdirs()
                privateScreenshot
                    .writeBytes(
                        bytes,
                    )
            }
        val privatePath =
            if (
                privateWrite.isSuccess
            ) {
                privateScreenshot
                    .relativeTo(
                        root.parentFile
                            ?: error("cross-platform evidence root has no parent"),
                    )
                    .path
            } else {
                null
            }

        val outputPath =
            "vex-cross-platform/" +
                safeFixtureRef +
                "/" +
                safeRunRef +
                "/screenshots/" +
                phaseRef.safePath() +
                ".png"

        val transfer =
            runCatching {
                storage
                    .openOutputFile(outputPath)
                    .buffered()
                    .use { output ->
                        output.write(bytes)
                    }

                storage
                    .getOutputFileUri(
                        outputPath,
                    )
                    .toString()
            }
        val transferUri =
            transfer.getOrNull()
        val transferFailure =
            transfer.exceptionOrNull()

        val state =
            when {
                privateWrite.isSuccess &&
                    transfer.isSuccess ->
                    "CAPTURED_PRIVATE_AND_TRANSFERRED"

                privateWrite.isSuccess ->
                    "CAPTURED_PRIVATE_ONLY"

                transfer.isSuccess ->
                    "CAPTURED_TRANSFER_ONLY"

                else ->
                    "UNAVAILABLE"
            }

        val evidence =
            ScreenshotEvidence(
                state = state,
                outputPath =
                    outputPath.takeIf {
                        transfer.isSuccess
                    },
                privatePath = privatePath,
                byteLength = bytes.size,
                sha256 = sha256,
                transferState =
                    if (
                        transfer.isSuccess
                    ) {
                        "PASS"
                    } else {
                        "UNAVAILABLE"
                    },
                transferFailureType =
                    transferFailure
                        ?.javaClass
                        ?.name,
                transferFailureMessage =
                    transferFailure
                        ?.message,
                captureFailureType =
                    privateWrite
                        .exceptionOrNull()
                        ?.javaClass
                        ?.name,
                captureFailureMessage =
                    privateWrite
                        .exceptionOrNull()
                        ?.message,
                transferUri = transferUri,
            )

        checkpoint(
            phaseRef = "$phaseRef.screenshot",
            status =
                if (
                    state ==
                    "UNAVAILABLE"
                ) {
                    "OBSERVED"
                } else {
                    "PASS"
                },
            details =
                details +
                    evidence.details(),
        )

        return evidence
    }

    fun beginWalkAction(
        walkRef: String,
        actionRef: String,
        expectedResultRef: String,
        details: Map<String, Any?> = emptyMap(),
    ): WalkAction {
        val action =
            WalkAction(
                walkRef = walkRef,
                actionRef = actionRef,
                expectedResultRef = expectedResultRef,
            )
        val links =
            action.links() +
                details

        checkpoint(
            phaseRef = "$actionRef.action",
            status = "OBSERVED",
            details = links,
        )
        screenshot(
            phaseRef = "$actionRef.before",
            details =
                links +
                    mapOf(
                        "visualStage" to "BEFORE_ACTION",
                    ),
        )

        return action
    }

    fun completeWalkAction(
        action: WalkAction,
        observedResultRef: String,
        details: Map<String, Any?> = emptyMap(),
    ) {
        val links =
            action.links() +
                mapOf(
                    "observedResultRef" to observedResultRef,
                ) +
                details

        checkpoint(
            phaseRef = "${action.actionRef}.result",
            status = "PASS",
            details = links,
        )
        screenshot(
            phaseRef = "${action.actionRef}.after",
            details =
                links +
                    mapOf(
                        "visualStage" to "AFTER_RESULT",
                    ),
        )
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

    data class WalkAction(
        val walkRef: String,
        val actionRef: String,
        val expectedResultRef: String,
    ) {
        fun links(): Map<String, Any?> =
            mapOf(
                "walkRef" to walkRef,
                "actionRef" to actionRef,
                "expectedResultRef" to expectedResultRef,
            )
    }

    data class ScreenshotEvidence(
        val state: String,
        val outputPath: String?,
        val privatePath: String?,
        val byteLength: Int?,
        val sha256: String?,
        val transferState: String,
        val transferFailureType: String?,
        val transferFailureMessage: String?,
        val captureFailureType: String?,
        val captureFailureMessage: String?,
        val transferUri: String? = null,
    ) {
        fun details(): Map<String, Any?> =
            mapOf(
                "screenshotState" to state,
                "outputPath" to outputPath,
                "privatePath" to privatePath,
                "byteLength" to byteLength,
                "sha256" to sha256,
                "transferState" to transferState,
                "transferUri" to transferUri,
                "transferFailureType" to transferFailureType,
                "transferFailureMessage" to transferFailureMessage,
                "captureFailureType" to captureFailureType,
                "captureFailureMessage" to captureFailureMessage,
            )
    }

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
