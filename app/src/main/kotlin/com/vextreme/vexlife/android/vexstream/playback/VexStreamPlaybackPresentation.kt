package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.elements.ElementIcon

internal data class VexStreamPlaybackControlPresentation(
    val icon: ElementIcon,
    val enabled: Boolean,
)

internal fun VexStreamPlaybackProjection.present(
    control: VexStreamPlaybackControl,
): VexStreamPlaybackControlPresentation {
    val projected =
        this[control]

    val icon =
        projected.meaning.fold(
            onAvailable = { act ->
                when (act) {
                    VexStreamPlaybackAct.Play ->
                        ElementIcon.Play

                    VexStreamPlaybackAct.Pause ->
                        ElementIcon.Pause
                }
            },
            onUnavailable = {
                ElementIcon.PlayPause
            },
        )

    return VexStreamPlaybackControlPresentation(
        icon = icon,
        enabled =
            projected.isAvailable,
    )
}

// [VXG RealForever]
