package com.vextreme.vexlife.android.data.maps

/**
 * Preview/test implementation of [VexMapsRepository].
 */
class InMemoryVexMapsRepository(
    initialState: VexMapsStoredState =
        VexMapsStoredState(),
) : VexMapsRepository {
    private var state =
        initialState.copy(
            lastLocation =
                initialState
                    .lastLocation
                    ?.copy(),
        )

    override suspend fun load():
        VexMapsStoredState =
        snapshot()

    override suspend fun saveLastLocation(
        location: VexMapsStoredLocation,
    ) {
        state =
            state.copy(
                lastLocation =
                    location.copy(),
            )
    }

    override suspend fun saveOnlineStyle(
        styleRef: String,
    ) {
        require(styleRef.isNotBlank()) {
            "styleRef must not be blank"
        }
        state =
            state.copy(
                onlineStyleRef =
                    styleRef,
            )
    }

    fun snapshot(): VexMapsStoredState =
        state.copy(
            lastLocation =
                state
                    .lastLocation
                    ?.copy(),
        )
}

// [VXG RealForever]
