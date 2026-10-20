package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkJourneyCatalogEntry

/**
 * One catalog assembled from feature/shell-owned shards.
 *
 * The shard boundary answers "where should I look when this walk fails or coverage is missing?"
 * without restricting a journey from crossing Screens when one user intention naturally does so.
 */
object VexJourneyCatalog {
    val shards: List<VexJourneyShard> =
        registeredJourneyShards()

    /**
     * Canonical shard lookup derived from [shards].
     *
     * ScreenCrawler and other consumers query this map; they do not maintain another shard list.
     * New shards become available to selectors as soon as they are added to [shards].
     */
    val shardsById: Map<String, VexJourneyShard> =
        shards.associateBy { it.shardId }

    val all: List<VexJourney> =
        shards.flatMap { it.journeys }

    /**
     * Feature-Walk bindings are projected from the same shard owners as [all].
     *
     * This is completion traceability over Journeys, not a second Journey catalog.
     */
    val featureWalkBindings: List<VexFeatureWalkJourneyCatalogEntry> =
        shards.flatMap { shard ->
            shard.featureWalkCatalogEntries()
        }

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

    /**
     * Ordinary functional journey suite: one baseline Android case per semantic journey.
     *
     * Localization/accessibility permutations are batch concerns and remain available through
     * [cases] / explicit selection without multiplying every normal connected test run.
     */
    val baselineCases: List<VexJourneyCase> =
        shards.flatMap { shard ->
            shard.journeys.map { journey ->
                val baseline = requireNotNull(
                    journey.environments.firstOrNull { environment ->
                        environment.id ==
                            VexJourneyEnvironments.Baseline.id
                    },
                ) {
                    "journey must declare baseline environment: " +
                        "${shard.shardId}/${journey.id}"
                }

                VexJourneyCase(
                    shardId = shard.shardId,
                    journey = journey,
                    environment = baseline,
                )
            }
        }

    fun requireCase(caseRef: String): VexJourneyCase =
        requireNotNull(cases.firstOrNull { it.caseRef == caseRef }) {
            "unknown journey case: $caseRef"
        }
}

/**
 * Projects Feature-Walk bindings only from Journeys that the same shard actually registers.
 *
 * A binding is completion metadata over an executable Journey; it must never create a shadow Journey
 * that exists only in the Feature-Walk projection.
 */
internal fun VexJourneyShard.featureWalkCatalogEntries():
    List<VexFeatureWalkJourneyCatalogEntry> =
    featureWalkJourneys.map { binding ->
        require(
            journeys.any { journey ->
                journey === binding.journey
            },
        ) {
            "Feature-Walk Journey is not registered in shard: " +
                "${shardId}/${binding.journey.id}"
        }

        VexFeatureWalkJourneyCatalogEntry(
            shardId =
                shardId,
            binding =
                binding,
        )
    }

// [VXG RealForever]
