package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

internal data class VexStreamPlaybackExperienceRelayInputState(
    val player:
        VexStreamPlaybackReadState? =
        null,
    val playback:
        VexStreamPlaybackState? =
        null,
) :
    VexInputState<
        VexStreamPlaybackExperienceRelayInputState
    > {
    override fun deepCopy():
        VexStreamPlaybackExperienceRelayInputState =
        copy(
            player =
                player
                    ?.copy(),
            playback =
                playback
                    ?.deepCopyForExperienceRelay(),
        )
}

internal data class VexStreamPlaybackExperienceRelayOutputState(
    val readState:
        VexStreamPlaybackReadState,
    val playbackProjection:
        VexStreamPlaybackProjection,
) :
    VexOutputState<
        VexStreamPlaybackExperienceRelayOutputState
    > {
    override fun deepCopy():
        VexStreamPlaybackExperienceRelayOutputState =
        copy(
            readState =
                readState.copy(),
            playbackProjection =
                playbackProjection
                    .deepCopyForExperienceRelay(),
        )
}

/**
 * Process-current relationship consumed by Compose Playback presentation.
 *
 * Provider-origin admission occurs only after Player Store admission + Playback Family
 * reconciliation. Control-origin admission occurs only after Family.activate(...) admits its exact
 * semantic State. Live progress is intentionally separate because clock time advances without a
 * Partner occurrence.
 */
internal class VexStreamPlaybackExperienceRelay(
    playerStore:
        VexStreamPlayerStore,
    playbackFamily:
        VexStreamPlaybackFamily,
) :
    VexCompoundRelay<
        VexStreamPlaybackExperienceRelayInputState,
        VexStreamPlaybackExperienceRelayOutputState
    >(
        relayRef =
            "relay.vexstream.playback-experience",
        initialInputState =
            VexStreamPlaybackExperienceRelayInputState(
                player =
                    playerStore
                        .current
                        .toVexStreamPlaybackExperienceReadState(),
                playback =
                    playbackFamily
                        .currentState
                        .deepCopyForExperienceRelay(),
            ),
        initialOutputState =
            vexStreamPlaybackExperienceRelayOutput(
                readState =
                    playerStore
                        .current
                        .toVexStreamPlaybackExperienceReadState(),
                familyState =
                    playbackFamily.currentState,
            ),
    ) {
    internal fun admitProviderRoundTrip(
        playerCurrent:
            VexStreamPlayerPartner.Current,
        familyState:
            VexStreamPlaybackState,
    ) {
        send(
            VexStreamPlaybackExperienceRelayInputState(
                player =
                    playerCurrent
                        .toVexStreamPlaybackExperienceReadState(),
                playback =
                    familyState
                        .deepCopyForExperienceRelay(),
            ),
        )
    }

    internal fun admitFamilyCurrent(
        familyState:
            VexStreamPlaybackState,
    ) {
        send(
            VexStreamPlaybackExperienceRelayInputState(
                playback =
                    familyState
                        .deepCopyForExperienceRelay(),
            ),
        )
    }

    override fun processStates(
        inputState:
            VexStreamPlaybackExperienceRelayInputState,
        currentState:
            VexCompoundState<
                VexStreamPlaybackExperienceRelayInputState,
                VexStreamPlaybackExperienceRelayOutputState
            >,
    ):
        VexCompoundState<
            VexStreamPlaybackExperienceRelayInputState,
            VexStreamPlaybackExperienceRelayOutputState
        > {
        val previous =
            currentState.inputState
        val player =
            inputState.player
                ?: previous.player
                ?: error(
                    "Playback Experience Relay requires retained Player input",
                )
        val playback =
            inputState.playback
                ?: previous.playback
                ?: error(
                    "Playback Experience Relay requires retained Family input",
                )

        return currentState.copy(
            inputState =
                VexStreamPlaybackExperienceRelayInputState(
                    player =
                        player.copy(),
                    playback =
                        playback
                            .deepCopyForExperienceRelay(),
                ),
            outputState =
                vexStreamPlaybackExperienceRelayOutput(
                    readState =
                        player,
                    familyState =
                        playback,
                ),
        )
    }
}

internal fun vexStreamPlaybackExperienceRelayOutput(
    readState:
        VexStreamPlaybackReadState,
    familyState:
        VexStreamPlaybackState,
):
    VexStreamPlaybackExperienceRelayOutputState =
    VexStreamPlaybackExperienceRelayOutputState(
        readState =
            readState.copy(),
        playbackProjection =
            vexStreamPlaybackProjectionFor(
                familyState,
            ),
    )

private fun VexStreamPlayerPartner.Current.toVexStreamPlaybackExperienceReadState():
    VexStreamPlaybackReadState =
    when (
        this
    ) {
        VexStreamPlayerPartner.Current.Unavailable ->
            VexStreamPlaybackReadState
                .Empty

        is VexStreamPlayerPartner.Current.Available ->
            VexStreamPlaybackReadState(
                currentMediaId =
                    currentMedia.mediaId,
                currentTitle =
                    currentMedia.title,
                currentArtist =
                    currentMedia.artist,
                currentArtworkUri =
                    currentMedia.artworkUri,
                queueSize =
                    queueItems.mediaIds
                        .size,
                currentIndex =
                    queueNavigation
                        .currentIndex,
            )
    }

private fun VexStreamPlaybackProjection.deepCopyForExperienceRelay():
    VexStreamPlaybackProjection =
    copy(
        state =
            state
                .deepCopyForExperienceRelay(),
    )

private fun VexStreamPlaybackState.deepCopyForExperienceRelay():
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

// [VXG RealForever][VEXSTREAM][PLAYBACK][EXPERIENCE-RELAY]
