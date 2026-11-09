package com.vextreme.vexlife.android.vexstream.playback

/**
 * Playback semantic owner -> Player Partner Effect realization.
 *
 * Provider observation is independent and returns through the process Player Partner Store.
 */
internal class PartnerVexStreamPlaybackConnection(
    private val family:
        VexStreamPlaybackFamily,
    private val effectConnection:
        VexStreamPlaybackEffectConnection,
    private val onFamilyCurrent:
        (VexStreamPlaybackState) -> Unit =
        {},
) :
    VexStreamPlaybackConnection {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ) {
        val act =
            family
                .activate(
                    control,
                )
                ?: return

        act.admittedState
            ?.let(
                onFamilyCurrent,
            )

        val effect =
            act.effect
                ?: return

        effectConnection.realize(
            effect,
        )
    }
}

/**
 * Feature-local construction boundary that keeps every Playback Control path on the same Family
 * admission -> Widget relationship -> Effect realization sequence.
 */
internal class VexStreamPlaybackConnectionFactory(
    private val family:
        VexStreamPlaybackFamily,
    private val onFamilyCurrent:
        (VexStreamPlaybackState) -> Unit =
        {},
) {
    fun create(
        effectConnection:
            VexStreamPlaybackEffectConnection,
    ):
        VexStreamPlaybackConnection =
        PartnerVexStreamPlaybackConnection(
            family =
                family,
            effectConnection =
                effectConnection,
            onFamilyCurrent =
                onFamilyCurrent,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PLAYER-PARTNER]
