package com.vextreme.vexlife.android.testing.journey

interface VexJourneyShard {
    val shardId: String
    val journeys: List<VexJourney>
}
