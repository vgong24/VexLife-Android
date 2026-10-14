package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.elements.ElementIcon

/**
 * VexStream-specific visual meaning for one projected Playback Control.
 *
 * This is deliberately outside the reusable Behavior kernel: icons/enabled rendering are
 * presentation choices derived from semantic Projection, not additional behavior truth.
 */
internal data class VexStreamPlaybackControlPresentation(
    val icon: ElementIcon,
    val enabled: Boolean,
)

/**
 * Translate the current semantic Control meaning into the visual language shared by VexStream UI
 * surfaces.
 */
internal fun VexBehaviorProjection<
    VexStreamPlaybackState,
    VexStreamPlaybackControl,
    VexStreamPlaybackAct
>.present(
    control: VexStreamPlaybackControl,
): VexStreamPlaybackControlPresentation {
    val projected =
        this[control]

    val icon =
        when (projected.act) {
            VexStreamPlaybackAct.Play ->
                ElementIcon.Play

            VexStreamPlaybackAct.Pause ->
                ElementIcon.Pause

            null ->
                ElementIcon.PlayPause
        }

    return VexStreamPlaybackControlPresentation(
        icon = icon,
        enabled = projected.available,
    )
}

// [VXG RealForever]
