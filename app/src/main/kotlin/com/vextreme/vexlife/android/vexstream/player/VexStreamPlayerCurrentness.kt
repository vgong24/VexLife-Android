package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.toVexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.toVexStreamQueueState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/*
 * Currentness gates for a finite request from a caller that owns no long-lived MediaController
 * (home-screen widget, app shell, later a Vex request).
 *
 * Such a caller borrows a controller for one request. Before activating a Family it must know that
 * the process Player Store holds a current snapshot and that the Family has already reconciled that
 * exact snapshot; otherwise a toggle could be interpreted against stale semantic State.
 *
 * Moved unchanged from the widget (formerly awaitVexStreamWidget*), which proved them first.
 */

internal const val VEXSTREAM_PLAYER_CURRENT_TIMEOUT_MILLIS =
    2_000L

internal suspend fun awaitVexStreamPlayerSnapshot(
    store:
        VexStreamPlayerStore,
    timeoutMillis:
        Long =
        VEXSTREAM_PLAYER_CURRENT_TIMEOUT_MILLIS,
):
    VexStreamPlayerStore.Snapshot.Available? =
    withTimeoutOrNull(
        timeoutMillis,
    ) {
        store.states
            .first { snapshot ->
                snapshot is
                    VexStreamPlayerStore
                        .Snapshot
                        .Available
            } as
            VexStreamPlayerStore
                .Snapshot
                .Available
    }

internal suspend fun awaitVexStreamPlaybackFamilyCurrent(
    family:
        VexStreamPlaybackFamily,
    store:
        VexStreamPlayerStore,
    timeoutMillis:
        Long =
        VEXSTREAM_PLAYER_CURRENT_TIMEOUT_MILLIS,
):
    Boolean =
    withTimeoutOrNull(
        timeoutMillis,
    ) {
        while (
            true
        ) {
            val matched =
                family.states
                    .first { familyState ->
                        val current =
                            store.current as?
                                VexStreamPlayerStore
                                    .Snapshot
                                    .Available
                                ?: return@first false

                        familyState ==
                            current.playback
                                .toVexStreamPlaybackState()
                    }

            val latest =
                store.current as?
                    VexStreamPlayerStore
                        .Snapshot
                        .Available
                    ?: continue

            if (
                matched ==
                latest.playback
                    .toVexStreamPlaybackState()
            ) {
                return@withTimeoutOrNull true
            }
        }

        @Suppress("UNREACHABLE_CODE")
        false
    } ?: false

internal suspend fun awaitVexStreamQueueFamilyCurrent(
    family:
        VexStreamQueueFamily,
    store:
        VexStreamPlayerStore,
    timeoutMillis:
        Long =
        VEXSTREAM_PLAYER_CURRENT_TIMEOUT_MILLIS,
):
    Boolean =
    withTimeoutOrNull(
        timeoutMillis,
    ) {
        while (
            true
        ) {
            val matched =
                family.states
                    .first { familyState ->
                        val current =
                            store.current as?
                                VexStreamPlayerStore
                                    .Snapshot
                                    .Available
                                ?: return@first false

                        familyState ==
                            current.queue
                                .toVexStreamQueueState()
                    }

            val latest =
                store.current as?
                    VexStreamPlayerStore
                        .Snapshot
                        .Available
                    ?: continue

            if (
                matched ==
                latest.queue
                    .toVexStreamQueueState()
            ) {
                return@withTimeoutOrNull true
            }
        }

        @Suppress("UNREACHABLE_CODE")
        false
    } ?: false

// [VXG RealForever][VEXSTREAM][PLAYER][CURRENTNESS]
