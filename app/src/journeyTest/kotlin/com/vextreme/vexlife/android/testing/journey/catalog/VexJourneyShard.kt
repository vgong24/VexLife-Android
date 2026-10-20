package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkJourneyBinding

interface VexJourneyShard {
    val shardId: String
    val journeys: List<VexJourney>

    /**
     * Feature-Walk Cases currently realized by Journeys in this shard.
     *
     * Most Journeys predate/need no whole-feature completion contract, so the default stays empty.
     */
    val featureWalkJourneys: List<VexFeatureWalkJourneyBinding>
        get() =
            emptyList()
}
