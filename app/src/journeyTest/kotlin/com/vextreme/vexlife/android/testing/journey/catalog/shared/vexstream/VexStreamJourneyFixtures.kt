package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef

/**
 * Shared semantic refs for the deterministic VexStream Journey fixture.
 *
 * Keeping these in journeyTest prevents the declaration and Android realization from copying
 * runtime item IDs independently.
 */
object VexStreamJourneyFixtures {
    val TrackOne =
        VexStreamTrackRef(
            "journey.track.one",
        )

    val TrackTwo =
        VexStreamTrackRef(
            "journey.track.two",
        )

    val TrackThree =
        VexStreamTrackRef(
            "journey.track.three",
        )

    const val AlbumName: String =
        "Journey Album"
}

// [VXG RealForever]
