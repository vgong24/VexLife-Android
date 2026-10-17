package com.vextreme.vexlife.android.testing.featurewalk

import com.vextreme.vexlife.android.testing.journey.VexJourney
import com.vextreme.vexlife.android.testing.journey.VexJourneyEnvironment
import com.vextreme.vexlife.android.testing.journey.VexJourneyEnvironments
import com.vextreme.vexlife.android.testing.journey.VexJourneyScope
import com.vextreme.vexlife.android.testing.journey.vexJourney

data class VexFeatureWalkJourneyBinding(
    val featureWalkCase: VexFeatureWalkCase,
    val journey: VexJourney,
)

data class VexFeatureWalkJourneyCatalogEntry(
    val shardId: String,
    val binding: VexFeatureWalkJourneyBinding,
) {
    init {
        require(shardId.isNotBlank()) {
            "Feature-Walk Journey shard ID must not be blank"
        }
    }

    val journeyRef: String
        get() =
            "$shardId/${binding.journey.id}"
}

/**
 * Forms one Journey from one currently executable Feature-Walk Case.
 *
 * The Case owns the Journey's local ID/title so the Journey cannot silently retranscribe a second
 * human-path identity. Environment expansion remains owned by [VexJourneyEnvironment].
 */
fun vexFeatureWalkJourney(
    featureWalkCase: VexFeatureWalkCase,
    environments: List<VexJourneyEnvironment> =
        listOf(
            VexJourneyEnvironments.Baseline,
        ),
    walk: VexJourneyScope.() -> Unit,
): VexFeatureWalkJourneyBinding {
    require(featureWalkCase.journeyExpectedNow) {
        "Feature-Walk Case is held/future and cannot bind a Journey yet: " +
            featureWalkCase.ref
    }

    return VexFeatureWalkJourneyBinding(
        featureWalkCase =
            featureWalkCase,
        journey =
            vexJourney(
                id =
                    featureWalkCase.key,
                title =
                    featureWalkCase.title,
                environments =
                    environments,
                walk =
                    walk,
            ),
    )
}

// [VXG RealForever]
