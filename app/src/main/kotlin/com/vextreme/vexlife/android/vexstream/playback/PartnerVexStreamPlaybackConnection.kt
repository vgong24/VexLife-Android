package com.vextreme.vexlife.android.vexstream.playback

/**
 * Playback semantic owner -> Player Partner Effect realization.
 *
 * Provider observation is independent and returns through the process Player Partner Store.
 */
internal class PartnerVexStreamPlaybackConnection(
    private val admission:
        VexStreamPlaybackAdmission,
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
            admission
                .activate(
                    control =
                        control,
                    onAdmitted =
                        onFamilyCurrent,
                )
                ?: return

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
    private val admission:
        VexStreamPlaybackAdmission,
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
            admission =
                admission,
            effectConnection =
                effectConnection,
            onFamilyCurrent =
                onFamilyCurrent,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PLAYER-PARTNER]
