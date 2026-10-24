package com.vextreme.vexlife.android.vexstream.offline

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionTestScenarios.Scenario
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamOfflineCollectionTest {
    @Test
    fun planPartitionsCurrentCollectionWithoutTargetingExistingOrInFlightCopies() {
        val downloaded =
            track(
                ref = "downloaded",
                digestChar = 'a',
                localCopy =
                    VexStreamLocalCopyState.Downloaded(
                        mediaSha256 = digest('a'),
                        byteLength = BYTE_LENGTH,
                        localMediaRef = "local.downloaded",
                    ),
            )
        val downloading =
            track(
                ref = "downloading",
                digestChar = 'b',
                localCopy =
                    VexStreamLocalCopyState.Downloading(
                        bytesDownloaded = 100,
                        totalBytes = BYTE_LENGTH,
                    ),
            )
        val missing =
            track(
                ref = "missing",
                digestChar = 'c',
            )
        val retryable =
            track(
                ref = "retryable",
                digestChar = 'd',
                localCopy =
                    VexStreamLocalCopyState.Failed(
                        reasonCode = "NETWORK",
                        retryable = true,
                    ),
            )
        val blockedOffline =
            track(
                ref = "blocked-offline",
                digestChar = 'e',
                hubAvailable = false,
            )
        val blockedFailure =
            track(
                ref = "blocked-failure",
                digestChar = 'f',
                localCopy =
                    VexStreamLocalCopyState.Failed(
                        reasonCode = "INVALID",
                        retryable = false,
                    ),
            )

        val plan =
            vexStreamOfflineCollectionPlan(
                listOf(
                    downloaded,
                    downloading,
                    missing,
                    retryable,
                    blockedOffline,
                    blockedFailure,
                ),
            )

        assertEquals(6, plan.totalCount)
        assertEquals(1, plan.onDeviceCount)
        assertEquals(1, plan.downloadingCount)
        assertEquals(2, plan.blockedMissingCount)
        assertEquals(2, plan.downloadableMissingCount)
        assertEquals(5, plan.missingCount)
        assertFalse(plan.fullyOnDevice)
        assertEquals(
            listOf(
                "missing",
                "retryable",
            ),
            plan.targets.map {
                it.trackRef.value
            },
        )
    }

    @Test
    fun removalRequestTargetsOnlyVerifiedDownloadedSongs() {
        val downloaded =
            track(
                ref = "downloaded",
                digestChar = 'a',
                localCopy =
                    VexStreamLocalCopyState.Downloaded(
                        mediaSha256 = digest('a'),
                        byteLength = BYTE_LENGTH,
                        localMediaRef = "local.downloaded",
                    ),
            )
        val missing =
            track(
                ref = "missing",
                digestChar = 'b',
            )
        val downloading =
            track(
                ref = "downloading",
                digestChar = 'c',
                localCopy =
                    VexStreamLocalCopyState.Downloading(
                        bytesDownloaded = 1,
                        totalBytes = BYTE_LENGTH,
                    ),
            )

        val request =
            vexStreamOfflineCollectionRemovalRequest(
                listOf(
                    downloaded,
                    missing,
                    downloading,
                ),
            )

        assertEquals(
            listOf(
                VexStreamTrackRef(
                    "downloaded",
                ),
            ),
            requireNotNull(
                request,
            ).trackRefs,
        )
    }

    @Test
    fun collectionRequestDownloadsOnlyCurrentMissingTargetsAndConvergesToFullyOnDevice() {
        val outcome =
            runCanonicalScenario(
                Scenario.CompleteMissing,
            )

        assertTrue(
            outcome.finalPlan.fullyOnDevice,
        )
        assertEquals(
            3,
            outcome.finalPlan.onDeviceCount,
        )
        assertEquals(
            0,
            outcome.finalPlan.downloadableMissingCount,
        )
        assertEquals(
            null,
            outcome.finalPlan.requestOrNull(),
        )
        assertEquals(
            VexStreamOfflineCollectionTestScenarios.LOCAL_REF_ONE,
            (
                outcome.finalState
                    .track(
                        VexStreamOfflineCollectionTestScenarios.TRACK_ONE,
                    )
                    ?.localCopy as
                    VexStreamLocalCopyState.Downloaded
            ).localMediaRef,
        )
    }

    @Test
    fun savedCollectionRequestSkipsMissingTargetsAfterHubBecomesUnavailable() {
        val outcome =
            runCanonicalScenario(
                Scenario.HubLoss,
            )

        assertEquals(
            1,
            outcome.finalPlan.onDeviceCount,
        )
        assertEquals(
            2,
            outcome.finalPlan.blockedMissingCount,
        )
        assertEquals(
            0,
            outcome.finalPlan.downloadableMissingCount,
        )
        assertEquals(
            null,
            outcome.finalPlan.requestOrNull(),
        )
        assertTrue(
            outcome.finalState
                .track(
                    VexStreamOfflineCollectionTestScenarios.TRACK_ONE,
                )
                ?.localCopy is
                VexStreamLocalCopyState.Downloaded,
        )
    }

    @Test
    fun staleExactTargetIsSkippedInsteadOfDownloadingChangedMedia() {
        val outcome =
            runCanonicalScenario(
                Scenario.StaleContent,
            )

        assertEquals(
            2,
            outcome.finalPlan.onDeviceCount,
        )
        assertEquals(
            listOf(
                VexStreamOfflineCollectionTestScenarios.TRACK_TWO,
            ),
            outcome.finalPlan.targets.map {
                it.trackRef
            },
        )
        assertTrue(
            outcome.finalState
                .track(
                    VexStreamOfflineCollectionTestScenarios.TRACK_TWO,
                )
                ?.localCopy ===
                VexStreamLocalCopyState.NotDownloaded,
        )
        assertTrue(
            outcome.finalState
                .track(
                    VexStreamOfflineCollectionTestScenarios.TRACK_THREE,
                )
                ?.localCopy is
                VexStreamLocalCopyState.Downloaded,
        )
    }

    private fun runCanonicalScenario(
        scenario: Scenario,
    ): VexStreamOfflineCollectionTestScenarios.Outcome =
        runBlocking {
            val checkpoint =
                VexStreamOfflineCollectionTestScenarios
                    .partialOfflineCheckpoint()

            VexStreamOfflineCollectionTestScenarios.execute(
                scenario = scenario,
                checkpoint = checkpoint,
                world =
                    VexStreamOfflineCollectionTestScenarios.restore(
                        checkpoint = checkpoint,
                        scope = this,
                    ),
            )
        }

    private fun track(
        ref: String,
        digestChar: Char,
        hubAvailable: Boolean = true,
        localCopy: VexStreamLocalCopyState =
            VexStreamLocalCopyState.NotDownloaded,
    ): VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            ref,
                        ),
                    mediaSha256 =
                        digest(
                            digestChar,
                        ),
                    mediaByteLength = BYTE_LENGTH,
                    title = ref,
                    artist = "Vex",
                    album = "Mass Download",
                ),
            hubAvailable = hubAvailable,
            localCopy = localCopy,
        )

    private fun digest(
        char: Char,
    ): VexStreamMediaSha256 =
        VexStreamMediaSha256(
            char.toString()
                .repeat(
                    64,
                ),
        )

    private companion object {
        const val BYTE_LENGTH = 4_096L
    }
}

// [VXG RealForever]
