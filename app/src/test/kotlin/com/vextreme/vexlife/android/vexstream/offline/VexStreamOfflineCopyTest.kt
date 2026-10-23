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

// [VXG RealForever]
