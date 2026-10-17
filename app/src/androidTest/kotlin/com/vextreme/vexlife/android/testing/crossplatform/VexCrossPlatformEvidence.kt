package com.vextreme.vexlife.android.testing.crossplatform

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import java.io.File
import org.json.JSONObject

/**
 * Small test-only bridge for lived proofs that need more than one platform.
 *
 * Product semantics remain with each platform's real implementation. This helper owns only:
 * - an explicit opt-in instrumentation fixture condition;
 * - append-only Android checkpoint evidence;
 * - one app-private marker path for host/test synchronization.
 *
 * The canonical cross-platform location map is owned by the SDK atlas, not by this helper.
 */
internal class VexCrossPlatformEvidence(
    context: Context,
    private val arguments: Bundle,
    val fixtureRef: String,
) {
    private val root =
        File(
            context.filesDir,
            "cross-platform",
        )
    private val safeFixtureRef =
        fixtureRef.replace(
            Regex("[^A-Za-z0-9._-]"),
            "_",
        )

    val evidenceFile =
        File(
            root,
            "$safeFixtureRef.jsonl",
        )

    val enabled: Boolean
        get() =
            arguments
                .getString(
                    ARG_FIXTURE,
                )
                ?.trim() ==
                fixtureRef

    fun reset() {
        root.mkdirs()
        evidenceFile.delete()
        root
            .listFiles()
            .orEmpty()
            .filter {
                it.name.startsWith(
                    "$safeFixtureRef.host.",
                )
            }
            .forEach(
                File::delete,
            )
    }

    fun argument(
        key: String,
    ): String? =
        arguments
            .getString(
                key,
            )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }

    fun checkpoint(
        phaseRef: String,
        status: String,
        details: Map<String, Any?> = emptyMap(),
    ) {
        require(
            phaseRef.isNotBlank(),
        )
        require(
            status in
                setOf(
                    "PASS",
                    "FAIL",
                    "WAITING",
                    "OBSERVED",
                ),
        )

        root.mkdirs()
        val json =
            JSONObject()
                .put(
                    "schemaVersion",
                    "vexlife.cross-platform-checkpoint/v1",
                )
                .put(
                    "fixtureRef",
                    fixtureRef,
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

        val detailJson =
            JSONObject()
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
            json.toString() +
                "\n",
        )
    }

    fun waitForHostMarker(
        markerRef: String,
        timeoutMs: Long,
    ): String {
        require(
            markerRef.isNotBlank(),
        )
        require(
            timeoutMs > 0L,
        )

        val marker =
            hostMarker(
                markerRef,
            )
        val deadline =
            SystemClock.elapsedRealtime() +
                timeoutMs

        while (
            SystemClock.elapsedRealtime() <
            deadline
        ) {
            if (
                marker.isFile
            ) {
                return marker
                    .readText()
                    .trim()
            }
            SystemClock.sleep(
                200L,
            )
        }

        error(
            "Timed out waiting for cross-platform host marker: $markerRef",
        )
    }

    private fun hostMarker(
        markerRef: String,
    ): File {
        val safeMarkerRef =
            markerRef.replace(
                Regex("[^A-Za-z0-9._-]"),
                "_",
            )
        return File(
            root,
            "$safeFixtureRef.host.$safeMarkerRef",
        )
    }

    companion object {
        const val ARG_FIXTURE =
            "vexCrossPlatformFixture"
    }
}

// [VXG RealForever]
