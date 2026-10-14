package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.elements.ElementIcon

internal data class VexStreamPlaybackControlPresentation(
    val icon: ElementIcon,
    val enabled: Boolean,
)

internal fun VexBehaviorProjection<
    VexStreamPlaybackFamily,
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
