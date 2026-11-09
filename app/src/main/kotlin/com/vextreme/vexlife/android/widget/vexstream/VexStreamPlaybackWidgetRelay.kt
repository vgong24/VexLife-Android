package com.vextreme.vexlife.android.widget.vexstream

import com.vextreme.vexlife.android.execution.VexExecutionRoot

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexObserveDependencies
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProjection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackProjectionFor
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueState
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueProjectionFor

/**
 * Bounded Player-origin facts required by the Playback Widget.
 *
 * Full QueueItems remain Player Store-owned; this projection deliberately carries no queue list.
 */
internal data class VexStreamPlaybackWidgetPlayerInput(
    val mediaId:
        String?,
    val title:
        String,
    val artist:
        String,
    val artworkUri:
        String?,
) {
    fun deepCopy():
        VexStreamPlaybackWidgetPlayerInput =
        copy()

    companion object {
        val Unavailable =
            VexStreamPlaybackWidgetPlayerInput(
                mediaId = null,
                title = "",
                artist = "",
                artworkUri = null,
            )
    }
}

/**
 * Partial origin-aware updates. null means this admission does not replace that owner's retained
 * input; it never means the rightful owner itself has no current.
 */
internal data class VexStreamPlaybackWidgetRelayInputState(
    val player:
        VexStreamPlaybackWidgetPlayerInput? =
        null,
    val playback:
        VexStreamPlaybackState? =
        null,
    val queue:
        VexStreamQueueState? =
        null,
) :
    VexInputState<
        VexStreamPlaybackWidgetRelayInputState
    > {
    override fun deepCopy():
        VexStreamPlaybackWidgetRelayInputState =
        copy(
            player =
                player
                    ?.deepCopy(),
            playback =
                playback
                    ?.deepCopyForWidgetRelay(),
            queue =
                queue
                    ?.deepCopyForWidgetRelay(),
        )
}

/**
 * Coherent bounded CURRENT emitted by the Playback Widget's relationship Relay.
 *
 * This is not Player Store CURRENT, Playback Family State or durable Glance SurfaceState. It is the
 * Widget-owned relationship output from those independently rightful owners.
 */
internal data class VexStreamPlaybackWidgetRelayOutputState(
    val mediaId:
        String?,
    val title:
        String,
    val artist:
        String,
    val artworkUri:
        String?,
    val previousEnabled:
        Boolean,
    val playback:
        VexStreamPlaybackProjection,
    val nextEnabled:
        Boolean,
) :
    VexOutputState<
        VexStreamPlaybackWidgetRelayOutputState
    > {
    override fun deepCopy():
        VexStreamPlaybackWidgetRelayOutputState =
        copy(
            playback =
                playback
                    .deepCopyForWidgetRelay(),
        )
}

/**
 * Playback Widget relationship current over bounded Player Store + Playback Family + Queue Family
 * truth.
 *
 * Player Store contributes metadata/artwork, Playback Family contributes Play/Pause meaning, and
 * Queue Family contributes Previous/Next meaning. The Relay observes those rightful currents through
 * the
 * established Compound Relay dependency mechanism. It does not invent transaction labels or mutate
 * either owner. Each input patch is serialized against the Relay's retained relationship current.
 */
internal class VexStreamPlaybackWidgetRelay(
    executionRoot:
        VexExecutionRoot,
    playerStore:
        VexStreamPlayerStore,
    playbackFamily:
        VexStreamPlaybackFamily,
    queueFamily:
        VexStreamQueueFamily,
) :
    VexCompoundRelay<
        VexStreamPlaybackWidgetRelayInputState,
        VexStreamPlaybackWidgetRelayOutputState
    >(
        relayRef =
            "relay.vexstream.playback-widget",
        initialInputState =
            VexStreamPlaybackWidgetRelayInputState(
                player =
                    playerStore
                        .current
                        .toVexStreamPlaybackWidgetPlayerInput(),
                playback =
                    playbackFamily
                        .currentState
                        .deepCopyForWidgetRelay(),
                queue =
                    queueFamily
                        .currentState
                        .deepCopyForWidgetRelay(),
            ),
        initialOutputState =
            vexStreamPlaybackWidgetRelayOutput(
                player =
                    playerStore
                        .current
                        .toVexStreamPlaybackWidgetPlayerInput(),
                playbackState =
                    playbackFamily.currentState,
                queueState =
                    queueFamily.currentState,
            ),
        executionRoot =
            executionRoot,
        observedDependencies =
            vexObserveDependencies<
                VexStreamPlaybackWidgetRelayInputState
            > {
                observe(
                    dependencyRef =
                        "player.current",
                    flow =
                        playerStore.states,
                ) { current ->
                    VexStreamPlaybackWidgetRelayInputState(
                        player =
                            current
                                .toVexStreamPlaybackWidgetPlayerInput(),
                    )
                }

                observe(
                    dependencyRef =
                        "playback.family.current",
                    flow =
                        playbackFamily.states,
                ) { state ->
                    VexStreamPlaybackWidgetRelayInputState(
                        playback =
                            state
                                .deepCopyForWidgetRelay(),
                    )
                }

                observe(
                    dependencyRef =
                        "queue.family.current",
                    flow =
                        queueFamily.states,
                ) { state ->
                    VexStreamPlaybackWidgetRelayInputState(
                        queue =
                            state
                                .deepCopyForWidgetRelay(),
                    )
                }
            },
    ) {
    init {
        startObservedDependencies()
    }

    override fun processStates(
        inputState:
            VexStreamPlaybackWidgetRelayInputState,
        currentState:
            VexCompoundState<
                VexStreamPlaybackWidgetRelayInputState,
                VexStreamPlaybackWidgetRelayOutputState
            >,
    ):
        VexCompoundState<
            VexStreamPlaybackWidgetRelayInputState,
            VexStreamPlaybackWidgetRelayOutputState
        > {
        val previous =
            currentState.inputState
        val player =
            inputState.player
                ?: previous.player
                ?: error(
                    "Playback Widget Relay requires retained Player input",
                )
        val playback =
            inputState.playback
                ?: previous.playback
                ?: error(
                    "Playback Widget Relay requires retained Playback Family input",
                )
        val queue =
            inputState.queue
                ?: previous.queue
                ?: error(
                    "Playback Widget Relay requires retained Queue Family input",
                )

        return currentState.copy(
            inputState =
                VexStreamPlaybackWidgetRelayInputState(
                    player =
                        player.deepCopy(),
                    playback =
                        playback.deepCopyForWidgetRelay(),
                    queue =
                        queue.deepCopyForWidgetRelay(),
                ),
            outputState =
                vexStreamPlaybackWidgetRelayOutput(
                    player =
                        player,
                    playbackState =
                        playback,
                    queueState =
                        queue,
                ),
        )
    }
}

internal fun vexStreamPlaybackWidgetRelayOutput(
    player:
        VexStreamPlaybackWidgetPlayerInput,
    playbackState:
        VexStreamPlaybackState,
    queueState:
        VexStreamQueueState,
):
    VexStreamPlaybackWidgetRelayOutputState {
    return VexStreamPlaybackWidgetRelayOutputState(
        mediaId =
            player.mediaId,
        title =
            player.title,
        artist =
            player.artist,
        artworkUri =
            player.artworkUri,
        previousEnabled =
            vexStreamQueueProjectionFor(
                queueState,
            )[
                VexStreamQueueControl.Previous
            ],
        playback =
            vexStreamPlaybackProjectionFor(
                playbackState,
            ),
        nextEnabled =
            vexStreamQueueProjectionFor(
                queueState,
            )[
                VexStreamQueueControl.Next
            ],
    )
}

private fun VexStreamPlayerStore.Snapshot.toVexStreamPlaybackWidgetPlayerInput():
    VexStreamPlaybackWidgetPlayerInput =
    when (
        this
    ) {
        VexStreamPlayerStore.Snapshot.Unavailable ->
            VexStreamPlaybackWidgetPlayerInput
                .Unavailable

        is VexStreamPlayerStore.Snapshot.Available ->
            VexStreamPlaybackWidgetPlayerInput(
                mediaId =
                    currentMedia.mediaId,
                title =
                    currentMedia.title,
                artist =
                    currentMedia.artist,
                artworkUri =
                    currentMedia.artworkUri,
            )
    }

private fun VexStreamPlaybackProjection.deepCopyForWidgetRelay():
    VexStreamPlaybackProjection =
    copy(
        state =
            state
                .deepCopyForWidgetRelay(),
    )

private fun VexStreamPlaybackState.deepCopyForWidgetRelay():
    VexStreamPlaybackState =
    when (
        this
    ) {
        VexStreamPlaybackState.NoMedia ->
            VexStreamPlaybackState.NoMedia

        is VexStreamPlaybackState.Media ->
            copy()

        is VexStreamPlaybackState.Ended ->
            copy()
    }

private fun VexStreamQueueState.deepCopyForWidgetRelay():
    VexStreamQueueState =
    when (
        this
    ) {
        VexStreamQueueState.Empty ->
            VexStreamQueueState.Empty

        is VexStreamQueueState.NonEmpty ->
            copy()
    }

// [VXG RealForever][VEXSTREAM][WIDGET][RELAY]
