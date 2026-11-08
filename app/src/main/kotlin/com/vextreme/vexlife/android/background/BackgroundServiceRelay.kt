package com.vextreme.vexlife.android.background

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies
import kotlinx.coroutines.flow.Flow

/**
 * Current background-capable VexLife relationships represented by this process convergence.
 *
 * Add a relationship only when a real background-capable owner exists. Future Health, Call, and
 * Companion relationships are pressure for this Relay, not source claims made ahead of those owners.
 */
internal enum class BackgroundServiceRelationship {
    Playback,
    Navigation,
}

internal data class BackgroundServiceInputState(
    val playbackActive:
        Boolean? =
        null,
    val navigationActive:
        Boolean? =
        null,
) :
    VexInputState<
        BackgroundServiceInputState
    > {
    override fun deepCopy():
        BackgroundServiceInputState =
        copy()
}

internal data class BackgroundServiceOutputState(
    val activeRelationships:
        Set<
            BackgroundServiceRelationship
        > =
        emptySet(),
) :
    VexOutputState<
        BackgroundServiceOutputState
    > {
    val backgroundContinuityRequired:
        Boolean
        get() =
            activeRelationships
                .isNotEmpty()

    val concurrentBackgroundRelationships:
        Boolean
        get() =
            activeRelationships
                .size >
                1

    override fun deepCopy():
        BackgroundServiceOutputState =
        copy(
            activeRelationships =
                activeRelationships
                    .toSet(),
        )
}

/**
 * Process-lived convergence for background-capable VexLife relationships.
 *
 * Human question:
 *
 *     "Which background-capable VexLife relationships are current together while UI may be absent?"
 *
 * This Relay does not own Playback or Navigation truth, start/stop Android Services, or replace
 * Family/Store/Partner/provider ownership. It continuously observes the already-owned truths and
 * produces one relationship State that downstream process behavior may consume.
 *
 * As Health, Call, Companion, or other background-capable Families become real, they may join this
 * convergence directly or through already-converged lower Relay outputs. Do not reopen lower Relay
 * children at this level merely to reconstruct relationship meaning they already own.
 */
internal class BackgroundServiceRelay(
    private val playbackStates:
        Flow<Boolean>,
    private val navigationStates:
        Flow<Boolean>,
) :
    VexCompoundRelay<
        BackgroundServiceInputState,
        BackgroundServiceOutputState
    >(
        relayRef =
            "relay.vexlife.background-service",
        initialInputState =
            BackgroundServiceInputState(
                playbackActive =
                    false,
                navigationActive =
                    false,
            ),
        initialOutputState =
            BackgroundServiceOutputState(),
    ) {
    override fun dependencies() =
        vexDependencies<
            BackgroundServiceInputState
        > {
            observe(
                dependencyRef =
                    PLAYBACK_DEPENDENCY,
                flow =
                    playbackStates,
            ) { active ->
                BackgroundServiceInputState(
                    playbackActive =
                        active,
                )
            }
            observe(
                dependencyRef =
                    NAVIGATION_DEPENDENCY,
                flow =
                    navigationStates,
            ) { active ->
                BackgroundServiceInputState(
                    navigationActive =
                        active,
                )
            }
        }

    override fun processStates(
        inputState:
            BackgroundServiceInputState,
        currentState:
            VexCompoundState<
                BackgroundServiceInputState,
                BackgroundServiceOutputState
            >,
    ):
        VexCompoundState<
            BackgroundServiceInputState,
            BackgroundServiceOutputState
        > {
        val previous =
            currentState
                .inputState
        val playbackActive =
            inputState
                .playbackActive
                ?: previous
                    .playbackActive
                ?: false
        val navigationActive =
            inputState
                .navigationActive
                ?: previous
                    .navigationActive
                ?: false

        val currentInput =
            BackgroundServiceInputState(
                playbackActive =
                    playbackActive,
                navigationActive =
                    navigationActive,
            )

        val active =
            buildSet {
                if (
                    playbackActive
                ) {
                    add(
                        BackgroundServiceRelationship
                            .Playback,
                    )
                }
                if (
                    navigationActive
                ) {
                    add(
                        BackgroundServiceRelationship
                            .Navigation,
                    )
                }
            }

        return currentState.copy(
            inputState =
                currentInput,
            outputState =
                BackgroundServiceOutputState(
                    activeRelationships =
                        active,
                ),
        )
    }

    private companion object {
        const val PLAYBACK_DEPENDENCY =
            "playback"
        const val NAVIGATION_DEPENDENCY =
            "navigation"
    }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE-RELAY]
