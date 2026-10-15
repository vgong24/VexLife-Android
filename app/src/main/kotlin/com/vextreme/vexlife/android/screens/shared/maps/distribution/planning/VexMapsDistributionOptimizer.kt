package com.vextreme.vexlife.android.screens.shared.maps.distribution.planning

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate

/**
 * Travel-time optimizer for one immutable Distribution preparation snapshot.
 *
 * The optimizer intentionally does not mutate Draft truth. It uses provider-derived pairwise travel
 * times, caches them for this optimization attempt, builds a nearest-neighbor seed, then repeatedly
 * applies whole-route 2-opt improvements. This is a bounded heuristic rather than a false claim of
 * global TSP optimality.
 */
internal class VexMapsDistributionOptimizer(
    private val costProvider:
        VexMapsDistributionLegCostProvider,
) {
    suspend fun optimize(
        draft:
            VexMapsDistributionDraft,
        preparation:
            VexMapsDistributionPreparationSnapshot,
    ): VexMapsDistributionPlan {
        require(
            preparation.draftRevision ==
                draft.revision,
        ) {
            "Distribution preparation revision does not match Draft"
        }
        require(
            preparation.stopRefs ==
                draft.stops
                    .sortedBy {
                        it.inputOrder
                    }
                    .map {
                        it.stopRef
                    },
        ) {
            "Distribution preparation stop identity does not match Draft"
        }
        require(
            preparation.returnToStart ==
                draft.returnToStart,
        ) {
            "Distribution preparation return policy does not match Draft"
        }

        val cache =
            mutableMapOf<
                LegKey,
                VexMapsDistributionLegCost
            >()

        suspend fun cost(
            from: VexMapCoordinate,
            to: VexMapCoordinate,
        ): VexMapsDistributionLegCost =
            cache.getOrPutSuspend(
                LegKey(
                    from = from,
                    to = to,
                ),
            ) {
                costProvider.cost(
                    from = from,
                    to = to,
                    travelMode =
                        preparation
                            .travelMode,
                )
            }

        suspend fun routeDuration(
            order:
                List<VexMapsDistributionStop>,
        ): Long {
            var total = 0L
            var previous =
                preparation.origin

            order.forEach { stop ->
                val coordinate =
                    stop.coordinate()
                total +=
                    cost(
                        previous,
                        coordinate,
                    ).durationSeconds
                previous =
                    coordinate
            }

            if (
                preparation
                    .returnToStart
            ) {
                total +=
                    cost(
                        previous,
                        preparation.origin,
                    ).durationSeconds
            }

            return total
        }

        val remaining =
            draft.stops
                .sortedBy {
                    it.inputOrder
                }
                .toMutableList()
        val seed =
            mutableListOf<
                VexMapsDistributionStop
            >()
        var current =
            preparation.origin

        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var bestCost =
                Long.MAX_VALUE

            remaining
                .forEachIndexed {
                    index,
                    stop,
                    ->
                    val candidate =
                        cost(
                            current,
                            stop.coordinate(),
                        ).durationSeconds

                    if (candidate < bestCost) {
                        bestCost =
                            candidate
                        bestIndex =
                            index
                    }
                }

            val next =
                remaining.removeAt(
                    bestIndex,
                )
            seed +=
                next
            current =
                next.coordinate()
        }

        var best =
            seed.toList()
        var bestDuration =
            routeDuration(
                best,
            )
        var improved =
            true

        while (improved) {
            improved =
                false

            improvement@
            for (
                start in
                0 until
                    best.lastIndex
            ) {
                for (
                    end in
                    start + 1..
                        best.lastIndex
                ) {
                    val candidate =
                        buildList {
                            addAll(
                                best.take(
                                    start,
                                ),
                            )
                            addAll(
                                best
                                    .subList(
                                        start,
                                        end + 1,
                                    )
                                    .asReversed(),
                            )
                            addAll(
                                best.drop(
                                    end + 1,
                                ),
                            )
                        }

                    val duration =
                        routeDuration(
                            candidate,
                        )

                    if (
                        duration <
                        bestDuration
                    ) {
                        best =
                            candidate
                        bestDuration =
                            duration
                        improved =
                            true
                        break@improvement
                    }
                }
            }
        }

        return VexMapsDistributionPlan(
            planRef =
                "vexmaps.distribution.plan." +
                    preparation
                        .preparationRef,
            preparation =
                preparation,
            orderedStops =
                best,
            estimatedDurationSeconds =
                bestDuration,
        )
    }
}

private data class LegKey(
    val from: VexMapCoordinate,
    val to: VexMapCoordinate,
)

private fun VexMapsDistributionStop.coordinate():
    VexMapCoordinate =
    VexMapCoordinate(
        latitude = latitude,
        longitude = longitude,
    )

private suspend fun <
    Key,
    Value,
> MutableMap<Key, Value>.getOrPutSuspend(
    key: Key,
    value:
        suspend () -> Value,
): Value {
    this[key]
        ?.let {
            return it
        }

    return value()
        .also {
            this[key] =
                it
        }
}

// [VXG RealForever]
