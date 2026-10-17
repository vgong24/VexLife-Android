package com.vextreme.vexlife.android.testing.featurewalk

/**
 * Typed human navigation into the current Feature-Walk contract.
 *
 * This is test/proof metadata. It does not become a production feature registry, runtime router, or
 * acceptance authority.
 */
object FeatureWalks {
    object conversation {
        object simulatedFamily {
            val roundTrip: VexFeatureWalkCase
                get() =
                    ConversationFeatureWalks.roundTrip
        }
    }

    object vexMaps {
        object destinationNavigation {
            val destinationShell: VexFeatureWalkCase
                get() =
                    VexMapsFeatureWalks.destinationShell

            val findRouteNavigate: VexFeatureWalkCase
                get() =
                    VexMapsFeatureWalks.findRouteNavigate
        }
    }

    val features: List<VexFeatureWalkFeature> =
        listOf(
            ConversationFeatureWalks.feature,
            VexMapsFeatureWalks.feature,
        )

    val walks: List<VexFeatureWalk> =
        features.flatMap { feature ->
            feature.walks
        }

    val cases: List<VexFeatureWalkCase> =
        walks.flatMap { walk ->
            walk.cases
        }

    val checkpoints: List<VexFeatureWalkCheckpoint> =
        cases.flatMap { case ->
            case.checkpoints
        }

    fun requireCase(
        caseRef: String,
    ): VexFeatureWalkCase =
        requireNotNull(
            cases.firstOrNull { case ->
                case.ref == caseRef
            },
        ) {
            "unknown Feature-Walk Case: $caseRef"
        }

    fun requireCheckpoint(
        checkpointRef: String,
    ): VexFeatureWalkCheckpoint =
        requireNotNull(
            checkpoints.firstOrNull { checkpoint ->
                checkpoint.ref == checkpointRef
            },
        ) {
            "unknown Feature-Walk Checkpoint: $checkpointRef"
        }
}

// [VXG RealForever]
