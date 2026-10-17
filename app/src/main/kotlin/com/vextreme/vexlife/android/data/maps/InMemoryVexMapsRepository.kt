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

    override suspend fun saveAppearancePreference(
        preference:
            VexMapsAppearancePreference,
    ) {
        state =
            state.copy(
                appearancePreference =
                    preference,
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
