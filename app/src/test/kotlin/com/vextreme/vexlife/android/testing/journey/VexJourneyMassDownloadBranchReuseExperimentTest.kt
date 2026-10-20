package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubCatalogProjection
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubTrackProjection
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionPlan
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.offline.vexStreamOfflineCollectionPlan
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Second real pressure on the QuantumTest/Vroomba branch-reuse idea.
 *
 * PR #141 asks whether independently-owned branch worlds can reuse one reconstructable reached
 * state. MassDownload supplies a product-owned checkpoint rather than another synthetic route graph:
 *
 * collection = TrackOne local + TrackTwo/TrackThree Hub-only
 * -> current product plan says 1 of 3 on device / 2 exact requestable targets
 *
 * This experiment snapshots only immutable product/test truth. It does not snapshot coroutine Jobs,
 * Compose, Media3, Binder, Context, sockets, or a real Android process.
 *
 * The question is intentionally narrower than framework graduation:
 *
 * can the same exact partial-offline checkpoint be restored into independent product worlds and
 * produce three truthful descendants without the feature moving its semantics into QuantumTest?
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VexJourneyMassDownloadBranchReuseExperimentTest {
    @Test
    fun onePartialCollectionCheckpointProducesIndependentProductFutures() {
        val snapshot =
            partialCollectionSnapshot()
        val request =
            requireNotNull(
                snapshot.plan.requestOrNull(),
            )
        val scheduler =
            TestCoroutineScheduler()
        val dispatcher =
            StandardTestDispatcher(
                scheduler,
            )
        val success =
            snapshot.restore(
                branchRef = BRANCH_SUCCESS,
                routeRef = ROUTE_SUCCESS,
                dispatcher = dispatcher,
            )
        val stale =
            snapshot.restore(
                branchRef = BRANCH_STALE,
                routeRef = ROUTE_STALE,
                dispatcher = dispatcher,
            )
        val hubLoss =
            snapshot.restore(
                branchRef = BRANCH_HUB_LOSS,
                routeRef = ROUTE_HUB_LOSS,
                dispatcher = dispatcher,
            )
        val branches =
            listOf(
                success,
                stale,
                hubLoss,
            )

        try {
            branches.forEach { branch ->
                assertEquals(
                    snapshot.state,
                    branch.repository.state.value,
                )
                assertNotSame(
                    snapshot.state,
                    branch.repository.state.value,
                )
                assertEquals(
                    listOf(
                        TRACK_TWO,
                        TRACK_THREE,
                    ),
                    vexStreamOfflineCollectionPlan(
                        branch.repository.state.value.tracks,
                    ).targets.map {
                        it.trackRef
                    },
                )
            }

            // Same route prefix; branch B changes provider/content truth before consuming the saved
            // request. Exact target binding should skip TrackTwo rather than download changed bytes.
            stale.repository.publishCatalog(
                staleTrackTwoCatalog(),
            )

            // Branch C loses producer reachability before consuming the exact same saved request.
            hubLoss.repository.publishHubUnavailable(
                VexStreamHubState.Unreachable,
            )

            lateinit var jobs:
                Map<String, Job>

            TestScope(
                dispatcher,
            ).runTest {
                jobs =
                    branches.associate { branch ->
                        branch.branchRef to
                            branch.connection.request(
                                request,
                            )
                    }

                assertTrue(
                    jobs.values.all(
                        Job::isActive,
                    ),
                )

                advanceUntilIdle()

                jobs.values.forEach { job ->
                    job.join()
                }
            }

            val evidence =
                branches.map(
                    BranchWorld::evidence,
                )

            assertEquals(
                3,
                evidence.map { item ->
                    Triple(
                        item.snapshotRef,
                        item.branchRef,
                        item.routeRef,
                    )
                }.distinct().size,
            )

            val successPlan =
                success.currentPlan()
            assertTrue(
                successPlan.fullyOnDevice,
            )
            assertEquals(
                3,
                successPlan.onDeviceCount,
            )
            assertNull(
                successPlan.requestOrNull(),
            )
            assertEquals(
                LOCAL_REF_ONE,
                (
                    success.repository.state.value
                        .track(TRACK_ONE)
                        ?.localCopy as
                        VexStreamLocalCopyState.Downloaded
                ).localMediaRef,
            )

            val stalePlan =
                stale.currentPlan()
            assertEquals(
                2,
                stalePlan.onDeviceCount,
            )
            assertEquals(
                listOf(
                    TRACK_TWO,
                ),
                stalePlan.targets.map {
                    it.trackRef
                },
            )
            assertTrue(
                stale.repository.state.value
                    .track(TRACK_THREE)
                    ?.localCopy is
                    VexStreamLocalCopyState.Downloaded,
            )
            assertTrue(
                stale.repository.state.value
                    .track(TRACK_TWO)
                    ?.localCopy ===
                    VexStreamLocalCopyState.NotDownloaded,
            )

            val hubLossPlan =
                hubLoss.currentPlan()
            assertEquals(
                1,
                hubLossPlan.onDeviceCount,
            )
            assertEquals(
                2,
                hubLossPlan.blockedMissingCount,
            )
            assertEquals(
                0,
                hubLossPlan.downloadableMissingCount,
            )
            assertNull(
                hubLossPlan.requestOrNull(),
            )

            // The captured checkpoint stays unchanged after every descendant has finished.
            assertEquals(
                1,
                snapshot.plan.onDeviceCount,
            )
            assertEquals(
                listOf(
                    TRACK_TWO,
                    TRACK_THREE,
                ),
                snapshot.plan.targets.map {
                    it.trackRef
                },
            )
            assertFalse(
                snapshot.plan.fullyOnDevice,
            )
        } finally {
            branches.forEach(
                BranchWorld::close,
            )
        }
    }

    private fun partialCollectionSnapshot(): MassDownloadSnapshot {
        val state =
            VexStreamMusicState(
                hubState =
                    VexStreamHubState.Ready(
                        hubRef = "hub.quantum.mass-download",
                        catalogRevision = "catalog.quantum.mass-download.1",
                    ),
                tracks =
                    listOf(
                        track(
                            ref = TRACK_ONE,
                            digestChar = '1',
                            localCopy =
                                VexStreamLocalCopyState.Downloaded(
                                    mediaSha256 =
                                        digest(
                                            '1',
                                        ),
                                    byteLength = BYTE_LENGTH_ONE,
                                    localMediaRef = LOCAL_REF_ONE,
                                ),
                        ),
                        track(
                            ref = TRACK_TWO,
                            digestChar = '2',
                            byteLength = BYTE_LENGTH_TWO,
                        ),
                        track(
                            ref = TRACK_THREE,
                            digestChar = '3',
                            byteLength = BYTE_LENGTH_THREE,
                        ),
                    ),
            )

        return MassDownloadSnapshot(
            state = state.branchCopy(),
        )
    }

    private fun staleTrackTwoCatalog(): VexStreamHubCatalogProjection =
        VexStreamHubCatalogProjection(
            hubRef = "hub.quantum.mass-download",
            sourceVersion = "quantum-test",
            catalogRevision = "catalog.quantum.mass-download.stale-two",
            tracks =
                listOf(
                    projection(
                        ref = TRACK_ONE,
                        digestChar = '1',
                        byteLength = BYTE_LENGTH_ONE,
                    ),
                    projection(
                        ref = TRACK_TWO,
                        digestChar = '4',
                        byteLength = BYTE_LENGTH_TWO,
                    ),
                    projection(
                        ref = TRACK_THREE,
                        digestChar = '3',
                        byteLength = BYTE_LENGTH_THREE,
                    ),
                ),
        )

    private fun MassDownloadSnapshot.restore(
        branchRef: String,
        routeRef: String,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ): BranchWorld {
        val repository =
            InMemoryVexStreamMusicRepository(
                initialState =
                    state.branchCopy(),
            )
        val scope =
            CoroutineScope(
                SupervisorJob() +
                    dispatcher,
            )

        return BranchWorld(
            snapshotRef = snapshotRef,
            branchRef = branchRef,
            routeRef = routeRef,
            repository = repository,
            scope = scope,
            connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = scope,
                ),
        )
    }

    private data class MassDownloadSnapshot(
        val state: VexStreamMusicState,
    ) {
        val plan: VexStreamOfflineCollectionPlan =
            vexStreamOfflineCollectionPlan(
                state.tracks,
            )

        val snapshotRef: String =
            state.massDownloadSnapshotRef()
    }

    private data class BranchEvidence(
        val snapshotRef: String,
        val branchRef: String,
        val routeRef: String,
        val finalPlan: VexStreamOfflineCollectionPlan,
    )

    private data class BranchWorld(
        val snapshotRef: String,
        val branchRef: String,
        val routeRef: String,
        val repository: InMemoryVexStreamMusicRepository,
        val scope: CoroutineScope,
        val connection: VexStreamOfflineCopyConnection,
    ) {
        fun currentPlan(): VexStreamOfflineCollectionPlan =
            vexStreamOfflineCollectionPlan(
                repository.state.value.tracks,
            )

        fun evidence(): BranchEvidence =
            BranchEvidence(
                snapshotRef = snapshotRef,
                branchRef = branchRef,
                routeRef = routeRef,
                finalPlan = currentPlan(),
            )

        fun close() {
            scope.cancel()
        }
    }

    private fun VexStreamMusicState.branchCopy(): VexStreamMusicState =
        copy(
            tracks =
                tracks.map { item ->
                    item.copy(
                        track =
                            item.track.copy(),
                        localCopy =
                            when (
                                val local =
                                    item.localCopy
                            ) {
                                VexStreamLocalCopyState.NotDownloaded ->
                                    local

                                is VexStreamLocalCopyState.Downloading ->
                                    local.copy()

                                is VexStreamLocalCopyState.Downloaded ->
                                    local.copy()

                                is VexStreamLocalCopyState.Failed ->
                                    local.copy()
                            },
                    )
                },
            playlists =
                playlists.map { playlist ->
                    playlist.copy(
                        trackRefs =
                            playlist.trackRefs.toList(),
                    )
                },
        )

    private fun VexStreamMusicState.massDownloadSnapshotRef(): String {
        val canonical =
            buildString {
                append(
                    "vexstream.mass-download.quantum-snapshot/v1\n",
                )
                tracks
                    .sortedBy {
                        it.track.trackRef.value
                    }
                    .forEach { item ->
                        append(item.track.trackRef.value)
                            .append('|')
                            .append(item.track.mediaSha256.value)
                            .append('|')
                            .append(item.track.mediaByteLength)
                            .append('|')
                            .append(item.hubAvailable)
                            .append('|')
                            .append(item.localCopy.snapshotValue())
                            .append('\n')
                    }
            }

        return "snapshot:vexstream-mass-download:" +
            canonical
                .encodeToByteArray()
                .sha256()
    }

    private fun VexStreamLocalCopyState.snapshotValue(): String =
        when (this) {
            VexStreamLocalCopyState.NotDownloaded ->
                "not-downloaded"

            is VexStreamLocalCopyState.Downloading ->
                "downloading:$bytesDownloaded/$totalBytes"

            is VexStreamLocalCopyState.Downloaded ->
                "downloaded:${mediaSha256.value}:$byteLength:$localMediaRef"

            is VexStreamLocalCopyState.Failed ->
                "failed:$reasonCode:$retryable"
        }

    private fun ByteArray.sha256(): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(this)
            .joinToString(
                separator = "",
            ) { byte ->
                "%02x".format(
                    byte,
                )
            }

    private fun track(
        ref: VexStreamTrackRef,
        digestChar: Char,
        byteLength: Long = BYTE_LENGTH_ONE,
        localCopy: VexStreamLocalCopyState =
            VexStreamLocalCopyState.NotDownloaded,
    ): VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef = ref,
                    mediaSha256 =
                        digest(
                            digestChar,
                        ),
                    mediaByteLength = byteLength,
                    title = ref.value,
                    artist = "Vex",
                    album = "Quantum MassDownload",
                ),
            hubAvailable = true,
            localCopy = localCopy,
        )

    private fun projection(
        ref: VexStreamTrackRef,
        digestChar: Char,
        byteLength: Long,
    ): VexStreamHubTrackProjection =
        VexStreamHubTrackProjection(
            trackRef = ref,
            mediaSha256 =
                digest(
                    digestChar,
                ),
            mediaByteLength = byteLength,
            title = ref.value,
            artist = "Vex",
            album = "Quantum MassDownload",
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
        val TRACK_ONE =
            VexStreamTrackRef(
                "quantum.mass-download.one",
            )
        val TRACK_TWO =
            VexStreamTrackRef(
                "quantum.mass-download.two",
            )
        val TRACK_THREE =
            VexStreamTrackRef(
                "quantum.mass-download.three",
            )

        const val BYTE_LENGTH_ONE = 4_096L
        const val BYTE_LENGTH_TWO = 4_097L
        const val BYTE_LENGTH_THREE = 4_098L
        const val LOCAL_REF_ONE =
            "local.quantum.mass-download.one"

        const val BRANCH_SUCCESS =
            "mass-download-success"
        const val BRANCH_STALE =
            "mass-download-stale-content"
        const val BRANCH_HUB_LOSS =
            "mass-download-hub-loss"

        const val ROUTE_SUCCESS =
            "partial-offline/download-missing/complete"
        const val ROUTE_STALE =
            "partial-offline/content-changed/download-missing"
        const val ROUTE_HUB_LOSS =
            "partial-offline/hub-unavailable/download-missing"
    }
}

// [VXG RealForever]
