package com.vextreme.vexlife.android.vexstream.offline

import android.Manifest
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamOfflineCopyTest {
    @Test
    fun sessionConnectionRoutesDownloadAndRemoveThroughRepositoryTruth() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val trackRef =
                VexStreamTrackRef(
                    "track.offline",
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        state(
                            trackRef,
                        ),
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )

            try {
                connection
                    .request(
                        VexStreamOfflineCopyRequest(
                            trackRef = trackRef,
                            action =
                                VexStreamOfflineCopyAction.Download,
                        ),
                    )
                    .join()

                assertTrue(
                    repository
                        .state
                        .value
                        .track(
                            trackRef,
                        )
                        ?.localCopy is
                        VexStreamLocalCopyState.Downloaded,
                )

                connection
                    .request(
                        VexStreamOfflineCopyRequest(
                            trackRef = trackRef,
                            action =
                                VexStreamOfflineCopyAction.Remove,
                        ),
                    )
                    .join()

                assertTrue(
                    repository
                        .state
                        .value
                        .track(
                            trackRef,
                        )
                        ?.localCopy ===
                        VexStreamLocalCopyState.NotDownloaded,
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun libraryBulkDownloadAndRemoveUsesOnlyThreeSongFixtureAndPreservesCatalog() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        threeSongPartialOfflineState(),
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )
            val originalRefs =
                repository
                    .state
                    .value
                    .tracks
                    .map {
                        it.track.trackRef
                    }

            try {
                val plan =
                    vexStreamOfflineCollectionPlan(
                        repository
                            .state
                            .value
                            .playableTracks(),
                    )

                assertEquals(
                    1,
                    plan.onDeviceCount,
                )
                assertEquals(
                    2,
                    plan.downloadableMissingCount,
                )

                connection
                    .request(
                        requireNotNull(
                            plan.requestOrNull(),
                        ),
                    )
                    .join()

                assertEquals(
                    3,
                    repository
                        .state
                        .value
                        .tracks
                        .count {
                            it.localCopy is
                                VexStreamLocalCopyState.Downloaded
                        },
                )

                val removalRequest =
                    requireNotNull(
                        vexStreamOfflineCollectionRemovalRequest(
                            repository
                                .state
                                .value
                                .playableTracks(),
                        ),
                    )

                assertEquals(
                    3,
                    removalRequest.trackRefs.size,
                )

                connection
                    .request(
                        removalRequest,
                    )
                    .join()

                val finalState =
                    repository
                        .state
                        .value

                assertEquals(
                    originalRefs,
                    finalState.tracks.map {
                        it.track.trackRef
                    },
                )
                assertTrue(
                    finalState.tracks.all {
                        it.localCopy ===
                            VexStreamLocalCopyState.NotDownloaded
                    },
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun libraryReconciliationRefreshesDownloadsOnlyMissingAndStillRunsArtworkWhenAudioIsComplete() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val recordingRepository =
                RecordingVexStreamMusicRepository(
                    InMemoryVexStreamMusicRepository(
                        initialState =
                            threeSongPartialOfflineState(),
                    ),
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository =
                        recordingRepository,
                    scope = ownerScope,
                )

            try {
                connection
                    .requestLibraryReconciliation()
                    .join()

                assertEquals(
                    1,
                    recordingRepository.refreshCalls,
                )
                assertEquals(
                    listOf(
                        VexStreamTrackRef(
                            "track.bulk.two",
                        ),
                        VexStreamTrackRef(
                            "track.bulk.three",
                        ),
                    ),
                    recordingRepository.downloadCalls,
                )
                assertEquals(
                    1,
                    recordingRepository.backfillCalls,
                )
                assertTrue(
                    recordingRepository
                        .state
                        .value
                        .tracks
                        .all {
                            it.localCopy is
                                VexStreamLocalCopyState.Downloaded
                        },
                )

                recordingRepository.resetCalls()

                connection
                    .requestLibraryReconciliation()
                    .join()

                assertEquals(
                    1,
                    recordingRepository.refreshCalls,
                )
                assertTrue(
                    recordingRepository
                        .downloadCalls
                        .isEmpty(),
                )
                assertEquals(
                    1,
                    recordingRepository.backfillCalls,
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun permissionHandoffDoesNotStartDownloadUntilGranted() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val trackRef =
                VexStreamTrackRef(
                    "track.permission-gate",
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        state(
                            trackRef,
                        ),
                )
            var granted =
                false
            var retry:
                (() -> Unit)? =
                null
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                    canDownload = {
                        granted
                    },
                    requestDownloadPermission = {
                        retry = it
                    },
                )

            try {
                connection
                    .request(
                        VexStreamOfflineCopyRequest(
                            trackRef = trackRef,
                            action =
                                VexStreamOfflineCopyAction.Download,
                        ),
                    )
                    .join()

                assertTrue(
                    repository
                        .state
                        .value
                        .track(
                            trackRef,
                        )
                        ?.localCopy ===
                        VexStreamLocalCopyState.NotDownloaded,
                )
                assertNotNull(
                    retry,
                )

                granted =
                    true
                requireNotNull(
                    retry,
                ).invoke()

                assertTrue(
                    repository
                        .state
                        .value
                        .track(
                            trackRef,
                        )
                        ?.localCopy is
                        VexStreamLocalCopyState.Downloaded,
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun deviceLibraryReadPermissionTracksAndroidMediaPermissionSplit() {
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            vexStreamDeviceLibraryReadPermission(28),
        )
        assertEquals(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            vexStreamDeviceLibraryReadPermission(32),
        )
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            vexStreamDeviceLibraryReadPermission(33),
        )
        assertEquals(
            Manifest.permission.READ_MEDIA_AUDIO,
            vexStreamDeviceLibraryReadPermission(36),
        )
    }

    @Test
    fun deviceLibraryReadPermissionHandoffWaitsForConsent() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        state(
                            VexStreamTrackRef(
                                "track.device-library-read",
                            ),
                        ),
                )
            var granted =
                false
            var retry:
                (() -> Unit)? =
                null
            var completionCalled =
                false
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                    canReadDeviceLibrary = {
                        granted
                    },
                    requestDeviceLibraryReadPermission = {
                        retry = it
                    },
                )

            try {
                assertFalse(
                    connection.ensureDeviceLibraryReadAccess {
                        completionCalled =
                            true
                    },
                )
                assertFalse(
                    completionCalled,
                )
                assertNotNull(
                    retry,
                )

                granted =
                    true
                requireNotNull(
                    retry,
                ).invoke()

                assertTrue(
                    completionCalled,
                )
                assertTrue(
                    connection.ensureDeviceLibraryReadAccess {},
                )
            } finally {
                ownerScope.cancel()
            }
        }

    @Test
    fun cancelledOwnerScopeCannotStartAnotherOfflineCopy() =
        runBlocking {
            val ownerScope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Unconfined,
                )
            val trackRef =
                VexStreamTrackRef(
                    "track.cancelled",
                )
            val repository =
                InMemoryVexStreamMusicRepository(
                    initialState =
                        state(
                            trackRef,
                        ),
                )
            val connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = ownerScope,
                )

            ownerScope.cancel()

            val request =
                connection.request(
                    VexStreamOfflineCopyRequest(
                        trackRef = trackRef,
                        action =
                            VexStreamOfflineCopyAction.Download,
                    ),
                )
            request.join()

            assertTrue(
                request.isCancelled,
            )
            assertTrue(
                repository
                    .state
                    .value
                    .track(
                        trackRef,
                    )
                    ?.localCopy ===
                    VexStreamLocalCopyState.NotDownloaded,
            )
        }

    private fun threeSongPartialOfflineState():
        VexStreamMusicState {
        val states =
            listOf(
                "one" to 'a',
                "two" to 'b',
                "three" to 'c',
            ).mapIndexed { index, (ref, digestChar) ->
                val track =
                    VexStreamTrack(
                        trackRef =
                            VexStreamTrackRef(
                                "track.bulk.$ref",
                            ),
                        mediaSha256 =
                            VexStreamMediaSha256(
                                digestChar.toString()
                                    .repeat(
                                        64,
                                    ),
                            ),
                        mediaByteLength =
                            1_000L +
                                index,
                        title = "Bulk $ref",
                        artist = "Vex",
                    )

                VexStreamTrackState(
                    track = track,
                    hubAvailable = true,
                    localCopy =
                        if (index == 0) {
                            VexStreamLocalCopyState.Downloaded(
                                mediaSha256 =
                                    track.mediaSha256,
                                byteLength =
                                    track.mediaByteLength,
                                localMediaRef =
                                    "local.bulk.one",
                            )
                        } else {
                            VexStreamLocalCopyState.NotDownloaded
                        },
                )
            }

        return VexStreamMusicState(
            hubState =
                VexStreamHubState.Ready(
                    hubRef = "hub.bulk.test",
                    catalogRevision =
                        "catalog.bulk.test",
                ),
            tracks = states,
        )
    }

    private fun state(
        trackRef: VexStreamTrackRef,
    ): VexStreamMusicState {
        val track =
            VexStreamTrack(
                trackRef = trackRef,
                mediaSha256 =
                    VexStreamMediaSha256(
                        "a".repeat(64),
                    ),
                mediaByteLength = 1L,
                title = "Offline",
                artist = "Vex",
            )

        return VexStreamMusicState(
            hubState =
                VexStreamHubState.Ready(
                    hubRef = "hub.test",
                    catalogRevision = "catalog.test",
                ),
            tracks =
                listOf(
                    VexStreamTrackState(
                        track = track,
                        hubAvailable = true,
                    ),
                ),
        )
    }
}

private class RecordingVexStreamMusicRepository(
    private val delegate:
        InMemoryVexStreamMusicRepository,
) : VexStreamMusicRepository by delegate {
    var refreshCalls: Int = 0
        private set
    var backfillCalls: Int = 0
        private set
    val downloadCalls =
        mutableListOf<VexStreamTrackRef>()

    override suspend fun refreshCatalog():
        VexStreamRepositoryResult {
        refreshCalls += 1
        return delegate.refreshCatalog()
    }

    override suspend fun backfillOfflineArtwork() {
        backfillCalls += 1
    }

    override suspend fun downloadForOffline(
        trackRef: VexStreamTrackRef,
        expectedMediaSha256:
            VexStreamMediaSha256?,
        expectedMediaByteLength: Long?,
    ): VexStreamRepositoryResult {
        downloadCalls += trackRef
        return delegate.downloadForOffline(
            trackRef = trackRef,
            expectedMediaSha256 =
                expectedMediaSha256,
            expectedMediaByteLength =
                expectedMediaByteLength,
        )
    }

    fun resetCalls() {
        refreshCalls = 0
        backfillCalls = 0
        downloadCalls.clear()
    }
}

// [VXG RealForever]
