package com.vextreme.vexlife.android.vexstream.playback

import kotlinx.coroutines.Job

/**
 * Concrete Playback realization boundary.
 *
 * The connection does not own semantic State. It receives the shared Playback Family and realizes
 * accepted Effects against one concrete Media3 controller.
 */
internal fun interface VexStreamPlaybackEffectConnection {
    suspend fun realize(
        effect:
            VexStreamPlaybackEffect,
    )
}

/** Semantic Control -> accepted Playback Effect boundary. */
internal interface VexStreamPlaybackConnection {
    fun activate(
        control:
            VexStreamPlaybackControl,
    ):
        Job?
}

/**
 * Immutable outward snapshot of one Playback Family State and its control availability.
 *
 * This is a projection only. The live [VexStreamPlaybackFamily] remains the one semantic truth owner.
 */
internal data class VexStreamPlaybackProjection(
    val state:
        VexStreamPlaybackState,
    val playPauseAvailable:
        Boolean,
)

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
