package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal data class VexMapsDeliveryRouteDraftIdentity(
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

internal sealed interface VexMapsDeliveryRouteState :
    VexBehaviorFamily.State<VexMapsDeliveryRouteFamily> {
    data object Empty :
        VexMapsDeliveryRouteState

    data class Draft(
        val source:
            VexMapsDeliveryRouteDraftIdentity,
    ) : VexMapsDeliveryRouteState

    data class Preparing(
        val preparationRef: String,
        val source:
            VexMapsDeliveryRouteDraftIdentity,
    ) : VexMapsDeliveryRouteState {
        init {
            require(preparationRef.isNotBlank())
        }
    }

    data class Ready(
        val planRef: String,
        val source:
            VexMapsDeliveryRouteDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDeliveryRouteState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
        }
    }

    data class Delivering(
        val planRef: String,
        val source:
            VexMapsDeliveryRouteDraftIdentity,
        val orderedStopRefs: List<String>,
        val currentStopRef: String,
    ) : VexMapsDeliveryRouteState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
            require(currentStopRef in orderedStopRefs)
        }
    }

    data class AtDelivery(
        val planRef: String,
        val source:
            VexMapsDeliveryRouteDraftIdentity,
        val orderedStopRefs: List<String>,
        val currentStopRef: String,
    ) : VexMapsDeliveryRouteState {
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

    data class ReturningToStart(
        val planRef: String,
        val source:
            VexMapsDeliveryRouteDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDeliveryRouteState {
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
            VexMapsDeliveryRouteDraftIdentity,
        val orderedStopRefs: List<String>,
    ) : VexMapsDeliveryRouteState {
        init {
            require(planRef.isNotBlank())
            requireExecutionOrder(
                source,
                orderedStopRefs,
            )
        }
    }
}

private fun requireExecutionOrder(
    source: VexMapsDeliveryRouteDraftIdentity,
    orderedStopRefs: List<String>,
) {
    require(orderedStopRefs.isNotEmpty())
    require(orderedStopRefs.distinct().size == orderedStopRefs.size)
    require(orderedStopRefs.toSet() == source.stopRefs.toSet()) {
        "Delivery route execution order must contain the exact admitted Draft stop refs"
    }
}

// [VXG RealForever]
