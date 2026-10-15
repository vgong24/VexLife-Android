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
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
        }
    }

    data class Running(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val currentStopRef: String,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            require(currentStopRef in source.stopRefs)
        }
    }

    data class AtStop(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
        val currentStopRef: String,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
            require(currentStopRef in source.stopRefs)
        }

        val isLastStop: Boolean
            get() =
                currentStopRef ==
                    source.stopRefs.last()

        val nextStopRef: String?
            get() {
                val index =
                    source.stopRefs.indexOf(
                        currentStopRef,
                    )
                return source.stopRefs
                    .getOrNull(
                        index + 1,
                    )
            }
    }

    data class Returning(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
        }
    }

    data class Completed(
        val planRef: String,
        val source:
            VexMapsDistributionRunDraftIdentity,
    ) : VexMapsDistributionRunState {
        init {
            require(planRef.isNotBlank())
        }
    }
}

// [VXG RealForever]
