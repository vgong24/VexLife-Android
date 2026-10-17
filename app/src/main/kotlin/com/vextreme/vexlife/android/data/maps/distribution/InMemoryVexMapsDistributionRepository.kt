package com.vextreme.vexlife.android.data.maps.distribution

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryVexMapsDistributionRepository(
    initialDraft:
        VexMapsDistributionDraft =
        VexMapsDistributionDraft(),
) : VexMapsDistributionRepository {
    private val mutex =
        Mutex()

    private var draft =
        initialDraft.snapshot()

    override suspend fun loadDraft():
        VexMapsDistributionDraft =
        mutex.withLock {
            draft.snapshot()
        }

    override suspend fun addStop(
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
    ): VexMapsDistributionDraft =
        mutex.withLock {
            require(name.isNotBlank())
            require(placeRef == null || placeRef.isNotBlank())

            val nextOrder =
                draft.stops
                    .maxOfOrNull {
                        it.inputOrder
                    }
                    ?.plus(1)
                    ?: 0

            draft =
                draft.copy(
                    revision =
                        draft.revision + 1L,
                    stops =
                        draft.stops +
                            VexMapsDistributionStop(
                                stopRef =
                                    "vexmaps.distribution.stop." +
                                        UUID.randomUUID(),
                                placeRef =
                                    placeRef,
                                name =
                                    name,
                                subtitle =
                                    subtitle,
                                latitude =
                                    latitude,
                                longitude =
                                    longitude,
                                inputOrder =
                                    nextOrder,
                            ),
                )

            draft.snapshot()
        }

    override suspend fun removeStop(
        stopRef: String,
    ): VexMapsDistributionDraft =
        mutex.withLock {
            require(stopRef.isNotBlank())
            val retained =
                draft.stops.filterNot {
                    it.stopRef ==
                        stopRef
                }

            if (
                retained.size !=
                draft.stops.size
            ) {
                draft =
                    draft.copy(
                        revision =
                            draft.revision + 1L,
                        stops =
                            retained,
                    )
            }

            draft.snapshot()
        }

    override suspend fun setReturnToStart(
        enabled: Boolean,
    ): VexMapsDistributionDraft =
        mutex.withLock {
            if (
                draft.returnToStart !=
                enabled
            ) {
                draft =
                    draft.copy(
                        revision =
                            draft.revision + 1L,
                        returnToStart =
                            enabled,
                    )
            }

            draft.snapshot()
        }

    override suspend fun clearDraft():
        VexMapsDistributionDraft =
        mutex.withLock {
            if (draft.stops.isNotEmpty()) {
                draft =
                    draft.copy(
                        revision =
                            draft.revision + 1L,
                        stops =
                            emptyList(),
                    )
            }

            draft.snapshot()
        }

    private fun VexMapsDistributionDraft.snapshot():
        VexMapsDistributionDraft =
        copy(
            stops =
                stops.map {
                    it.copy()
                },
        )
}

// [VXG RealForever]
