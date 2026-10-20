package com.vextreme.vexlife.android.vexstream.offline

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
import kotlinx.coroutines.CoroutineScope

/**
 * Feature-owned test scenarios for the three independently runnable MassDownload behaviors that
 * currently pressure QuantumTest.
 *
 * This is not a QuantumTest API. Ordinary focused tests own these scenarios first. An optional
 * accelerator may reuse the same checkpoint/arrangement later, but deleting that accelerator must
 * not remove these scenario meanings or their canonical test entry points.
 */
internal object VexStreamOfflineCollectionTestScenarios {
    enum class Scenario(
        val scenarioRef: String,
    ) {
        CompleteMissing(
            scenarioRef = "mass-download.complete-missing",
        ),
        StaleContent(
            scenarioRef = "mass-download.stale-content",
        ),
        HubLoss(
            scenarioRef = "mass-download.hub-loss",
        ),
    }

    data class Checkpoint(
        val state: VexStreamMusicState,
        val request: VexStreamOfflineCollectionRequest,
    )

    data class Outcome(
        val finalState: VexStreamMusicState,
        val finalPlan: VexStreamOfflineCollectionPlan,
    )

    class World internal constructor(
        val repository: InMemoryVexStreamMusicRepository,
        val connection: VexStreamOfflineCopyConnection,
    )

    fun partialOfflineCheckpoint(): Checkpoint {
        val state =
            VexStreamMusicState(
                hubState =
                    VexStreamHubState.Ready(
                        hubRef = "hub.mass-download.test",
                        catalogRevision = "catalog.mass-download.test",
                    ),
                tracks =
                    listOf(
                        track(
                            ref = TRACK_ONE,
                            digestChar = '1',
                            byteLength = BYTE_LENGTH_ONE,
                            localCopy =
                                VexStreamLocalCopyState.Downloaded(
                                    mediaSha256 = digest('1'),
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
        val copied =
            state.branchCopy()

        return Checkpoint(
            state = copied,
            request =
                requireNotNull(
                    vexStreamOfflineCollectionPlan(
                        copied.tracks,
                    ).requestOrNull(),
                ),
        )
    }

    fun restore(
        checkpoint: Checkpoint,
        scope: CoroutineScope,
    ): World {
        val repository =
            InMemoryVexStreamMusicRepository(
                initialState =
                    checkpoint.state.branchCopy(),
            )

        return World(
            repository = repository,
            connection =
                VexStreamOfflineCopyConnection(
                    repository = repository,
                    scope = scope,
                ),
        )
    }

    suspend fun execute(
        scenario: Scenario,
        checkpoint: Checkpoint,
        world: World,
    ): Outcome {
        when (scenario) {
            Scenario.CompleteMissing ->
                Unit

            Scenario.StaleContent ->
                world.repository.publishCatalog(
                    staleTrackTwoCatalog(),
                )

            Scenario.HubLoss ->
                world.repository.publishHubUnavailable(
                    VexStreamHubState.Unreachable,
                )
        }

        world.connection
            .request(
                checkpoint.request,
            )
            .join()

        val finalState =
            world.repository.state.value

        return Outcome(
            finalState = finalState,
            finalPlan =
                vexStreamOfflineCollectionPlan(
                    finalState.tracks,
                ),
        )
    }

    fun VexStreamMusicState.branchCopy(): VexStreamMusicState =
        copy(
            tracks =
                tracks.map { item ->
                    item.copy(
                        track = item.track.copy(),
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

    private fun staleTrackTwoCatalog(): VexStreamHubCatalogProjection =
        VexStreamHubCatalogProjection(
            hubRef = "hub.mass-download.test",
            sourceVersion = "mass-download-test",
            catalogRevision = "catalog.mass-download.test.stale-two",
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

    private fun track(
        ref: VexStreamTrackRef,
        digestChar: Char,
        byteLength: Long,
        localCopy: VexStreamLocalCopyState =
            VexStreamLocalCopyState.NotDownloaded,
    ): VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef = ref,
                    mediaSha256 = digest(digestChar),
                    mediaByteLength = byteLength,
                    title = ref.value,
                    artist = "Vex",
                    album = "Mass Download",
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
            mediaSha256 = digest(digestChar),
            mediaByteLength = byteLength,
            title = ref.value,
            artist = "Vex",
            album = "Mass Download",
        )

    private fun digest(
        char: Char,
    ): VexStreamMediaSha256 =
        VexStreamMediaSha256(
            char.toString()
                .repeat(64),
        )

    val TRACK_ONE =
        VexStreamTrackRef(
            "mass-download.one",
        )
    val TRACK_TWO =
        VexStreamTrackRef(
            "mass-download.two",
        )
    val TRACK_THREE =
        VexStreamTrackRef(
            "mass-download.three",
        )

    const val BYTE_LENGTH_ONE = 4_096L
    const val BYTE_LENGTH_TWO = 4_097L
    const val BYTE_LENGTH_THREE = 4_098L
    const val LOCAL_REF_ONE =
        "local.mass-download.one"
}

// [VXG RealForever]
