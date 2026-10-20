package com.vextreme.vexlife.android.vexstream.offline

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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
    fun collectionRequestDownloadsOnlyCurrentMissingTargetsAndConvergesToFullyOnDevice() =
        runBlocking {
            val existingCopy =
                VexStreamLocalCopyState.Downloaded(
                    mediaSha256 = digest('a'),
                    byteLength = BYTE_LENGTH,
                    localMediaRef = "local.existing",
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        readyState(
                            track(
                                ref = "already-local",
                                digestChar = 'a',
                                localCopy = existingCopy,
                            ),
                            track(
                                ref = "missing-one",
                                digestChar = 'b',
                            ),
                            track(
                                ref = "missing-two",
                                digestChar = 'c',
                            ),
                        ),
                )
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )

            try {
                val request =
                    requireNotNull(
                        vexStreamOfflineCollectionPlan(
                            repository.state.value.tracks,
                        ).requestOrNull(),
                    )

                connection
                    .request(
                        request,
                    )
                    .join()

                val current =
                    repository.state.value
                assertSame(
                    existingCopy,
                    current
                        .track(
                            VexStreamTrackRef(
                                "already-local",
                            ),
                        )
                        ?.localCopy,
                )
                assertTrue(
                    current
                        .track(
                            VexStreamTrackRef(
                                "missing-one",
                            ),
                        )
                        ?.localCopy is
                        VexStreamLocalCopyState.Downloaded,
                )
                assertTrue(
                    current
                        .track(
                            VexStreamTrackRef(
                                "missing-two",
                            ),
                        )
                        ?.localCopy is
                        VexStreamLocalCopyState.Downloaded,
                )

                val completedPlan =
                    vexStreamOfflineCollectionPlan(
                        current.tracks,
                    )
                assertTrue(completedPlan.fullyOnDevice)
                assertEquals(3, completedPlan.onDeviceCount)
                assertEquals(0, completedPlan.downloadableMissingCount)
                assertEquals(null, completedPlan.requestOrNull())
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun savedCollectionRequestSkipsMissingTargetsAfterHubBecomesUnavailable() =
        runBlocking {
            val existingCopy =
                VexStreamLocalCopyState.Downloaded(
                    mediaSha256 = digest('a'),
                    byteLength = BYTE_LENGTH,
                    localMediaRef = "local.existing",
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        readyState(
                            track(
                                ref = "already-local",
                                digestChar = 'a',
                                localCopy = existingCopy,
                            ),
                            track(
                                ref = "missing-one",
                                digestChar = 'b',
                            ),
                            track(
                                ref = "missing-two",
                                digestChar = 'c',
                            ),
                        ),
                )
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )

            try {
                val request =
                    requireNotNull(
                        vexStreamOfflineCollectionPlan(
                            repository.state.value.tracks,
                        ).requestOrNull(),
                    )

                repository.publishHubUnavailable(
                    VexStreamHubState.Unreachable,
                )

                connection
                    .request(
                        request,
                    )
                    .join()

                val current =
                    repository.state.value
                val currentPlan =
                    vexStreamOfflineCollectionPlan(
                        current.tracks,
                    )

                assertSame(
                    existingCopy,
                    current
                        .track(
                            VexStreamTrackRef(
                                "already-local",
                            ),
                        )
                        ?.localCopy,
                )
                assertEquals(
                    1,
                    currentPlan.onDeviceCount,
                )
                assertEquals(
                    2,
                    currentPlan.blockedMissingCount,
                )
                assertEquals(
                    0,
                    currentPlan.downloadableMissingCount,
                )
                assertEquals(
                    null,
                    currentPlan.requestOrNull(),
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun staleExactTargetIsSkippedInsteadOfDownloadingChangedMedia() =
        runBlocking {
            val ref =
                VexStreamTrackRef(
                    "changing-track",
                )
            val staleTarget =
                VexStreamOfflineCopyTarget(
                    trackRef = ref,
                    mediaSha256 = digest('a'),
                    mediaByteLength = BYTE_LENGTH,
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        readyState(
                            track(
                                ref = ref.value,
                                digestChar = 'b',
                            ),
                        ),
                )
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )

            try {
                connection
                    .request(
                        VexStreamOfflineCollectionRequest(
                            targets =
                                listOf(
                                    staleTarget,
                                ),
                        ),
                    )
                    .join()

                assertTrue(
                    repository
                        .state
                        .value
                        .track(ref)
                        ?.localCopy ===
                        VexStreamLocalCopyState.NotDownloaded,
                )
            } finally {
                ownerScope.cancel()
            }
        }

    private fun readyState(
        vararg tracks: VexStreamTrackState,
    ): VexStreamMusicState =
        VexStreamMusicState(
            hubState =
                VexStreamHubState.Ready(
                    hubRef = "hub.mass-download.test",
                    catalogRevision = "catalog.mass-download.test",
                ),
            tracks = tracks.toList(),
        )

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
