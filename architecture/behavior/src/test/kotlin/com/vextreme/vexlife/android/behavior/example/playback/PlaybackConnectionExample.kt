package com.vextreme.vexlife.android.behavior.example.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PlaybackConnectionExample(
    initialState: PlaybackState,
) :
    VexPortFlow<
        PlaybackState,
        PlaybackControl,
        PlaybackAct
    > {
    private val behavior =
        playbackBehaviorBlueprint()

    private val controls =
        behavior.playbackControls()

    private var observedState =
        initialState

    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                observedState,
            ),
        )

    override val projections:
        StateFlow<
            VexBehaviorProjection<
                PlaybackState,
                PlaybackControl,
                PlaybackAct
            >
        > =
        mutableProjections.asStateFlow()

    val realizedEffects =
        mutableListOf<PlaybackEffect>()

    override fun activate(
        control: PlaybackControl,
    ) {
        val act =
            controls.resolve(
                current = observedState,
                control = control,
            )?.act
                ?: return

        val relationship =
            behavior.resolve(
                current = observedState,
                act = act,
            )
                ?: return

        realizedEffects +=
            relationship.effect

        observe(
            realize(
                relationship.effect,
            ),
        )
    }

    fun observe(
        state: PlaybackState,
    ) {
        observedState =
            state
        mutableProjections.value =
            controls.projection(
                observedState,
            )
    }

    private fun realize(
        effect: PlaybackEffect,
    ): PlaybackState =
        when (effect) {
            PlaybackEffect.PausePlayback ->
                PlaybackState.Paused

            is PlaybackEffect.SetSpeed ->
                PlaybackState.Playing(
                    speed = effect.speed,
                )
        }
}

// [VXG RealForever]
