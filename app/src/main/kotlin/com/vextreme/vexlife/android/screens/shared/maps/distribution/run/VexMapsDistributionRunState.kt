package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal data class VexMapsDistributionRunDraftIdentity(
    val revision: Long,
    val stopRefs: List<String>,
    val returnToStart: Boolean,
) {
    init {
        require(revision >= 0L)
        require(stopRefs.isNotEmpty())
        require(stopRefs.all { it.isNotBlank() })
        require(stopRefs.distinct().size == stopRefs.size)
    }

    val stopCount: Int
        get() = stopRefs.size
}

internal sealed interface VexMapsDistributionRunState :
    VexBehaviorFamily.State<VexMapsDistributionRunFamily> {
    data object Empty :
        VexMapsDistributionRunState

    data class Draft(
        val source:
            VexMapsDistributionRunDraftIdentity,
    ) : VexMapsDistributionRunState

    data class Preparing(
        val preparationRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
    ) : VexMapsDistributionRunState {
        init {
            require(preparationRef.isNotBlank())
        }
    }

    data class Ready(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
        }
    }

    data class Running(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val orderedStopRefs: List<String>,
        val currentStopRef: String,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
            require(currentStopRef in orderedStopRefs)
        }
    }

    data class AtStop(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val orderedStopRefs: List<String>,
        val currentStopRef: String,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
            require(currentStopRef in orderedStopRefs)
        }

        val isLastStop: Boolean
            get() =
                currentStopRef ==
                    orderedStopRefs.last()

        val nextStopRef: String?
            get() {
                val index =
                    orderedStopRefs.indexOf(
                        currentStopRef,
                    )
                return orderedStopRefs
                    .getOrNull(
                        index + 1,
                    )
            }
    }

    data class Returning(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
        }
    }

    data class Completed(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
        }
    }
}

// [VXG RealForever]


private fun requireExecutionOrder(
    source: VexMapsDistributionRunDraftIdentity,
    orderedStopRefs: List<String>,
) {
    require(orderedStopRefs.isNotEmpty())
    require(orderedStopRefs.distinct().size == orderedStopRefs.size)
    require(orderedStopRefs.toSet() == source.stopRefs.toSet()) {
        "Distribution execution order must contain the exact admitted Draft stop refs"
    }
}
