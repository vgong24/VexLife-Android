package com.vextreme.vexlife.android.testing.journey

/**
 * One catalog assembled from feature/shell-owned shards.
 *
 * The shard boundary answers "where should I look when this walk fails or coverage is missing?"
 * without restricting a journey from crossing Screens when one user intention naturally does so.
 */
object VexJourneyCatalog {
    val shards: List<VexJourneyShard> = listOf(
        AppShellJourneys,
        DebugJourneys,
        GalleryJourneys,
        UserProfileJourneys,
        VexStreamJourneys,
        TalkToVexJourneys,
    )

    val all: List<VexJourney> =
        shards.flatMap { it.journeys }

    val cases: List<VexJourneyCase> =
        shards.flatMap { shard ->
            shard.journeys.flatMap { journey ->
                journey.environments.map { environment ->
                    VexJourneyCase(
                        shardId = shard.shardId,
                        journey = journey,
                        environment = environment,
                    )
                }
            }
        }

    fun requireCase(caseRef: String): VexJourneyCase =
        requireNotNull(cases.firstOrNull { it.caseRef == caseRef }) {
            "unknown journey case: $caseRef"
        }
}
