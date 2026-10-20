package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.R

/**
 * Small Android presentation projections shared by Maps planner/navigation surfaces.
 *
 * Keep provider/domain meaning out of here: these are resource/formatting mappings only.
 */
internal data class VexMapsDurationEstimate(
    val hours: Int,
    val minutes: Int,
)

internal fun projectVexMapsDurationEstimate(
    durationSeconds: Int,
): VexMapsDurationEstimate {
    require(durationSeconds > 0)

    val totalMinutes =
        (
            durationSeconds +
                SECONDS_PER_MINUTE -
                1
        ) /
            SECONDS_PER_MINUTE

    return VexMapsDurationEstimate(
        hours =
            totalMinutes /
                MINUTES_PER_HOUR,
        minutes =
            totalMinutes %
                MINUTES_PER_HOUR,
    )
}

internal val VexMapsTravelMode.labelRes: Int
    get() =
        when (this) {
            VexMapsTravelMode.DRIVE ->
                R.string.vexmaps_route_mode_drive

            VexMapsTravelMode.WALK ->
                R.string.vexmaps_route_mode_walk

            VexMapsTravelMode.BICYCLE ->
                R.string.vexmaps_route_mode_bicycle
        }

private const val SECONDS_PER_MINUTE =
    60

private const val MINUTES_PER_HOUR =
    60

// [VXG RealForever]
