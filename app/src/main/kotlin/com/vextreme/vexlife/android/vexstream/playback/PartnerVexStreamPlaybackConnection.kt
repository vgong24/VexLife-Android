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
) :
    VexStreamPlaybackConnection {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ) {
        val effect =
            family
                .activate(
                    control,
                )
                ?.effect
                ?: return

        effectConnection.realize(
            effect,
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PLAYER-PARTNER]
