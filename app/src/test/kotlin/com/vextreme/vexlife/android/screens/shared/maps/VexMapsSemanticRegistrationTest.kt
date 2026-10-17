package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import com.vextreme.vexlife.android.screens.AppScreens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsSemanticRegistrationTest {
    @Test
    fun stableMapControlsAreOwnedByVexMapsRegistry() {
        val elements =
            listOf(
                VexMapsScreen.UsePreciseLocation,
                VexMapsScreen.OpenMapMenu,
                VexMapsScreen.OpenOfflineMaps,
                VexMapsScreen.OpenAppearance,
                VexMapsScreen.AppearanceAutomatic,
                VexMapsScreen.AppearanceLight,
                VexMapsScreen.AppearanceDark,
                VexMapsScreen.ToggleOfflinePreparation,
                VexMapsScreen.PrepareOfflineAroundMe,
                VexMapsScreen.PrepareOfflineTrip,
                VexMapsScreen.BrowseOfflineRegions,
                VexMapsScreen.ToggleAdvancedOfflineSetup,
                VexMapsScreen.ImportPlaceSearch,
                VexMapsScreen.PlaceSearchQuery,
                VexMapsScreen.SearchPlaces,
                VexMapsScreen.ImportOfflineMap,
                VexMapsScreen.ImportRoutingRegion,
                VexMapsScreen.DriveMode,
                VexMapsScreen.WalkMode,
                VexMapsScreen.BicycleMode,
                VexMapsScreen.ChooseOriginOnMap,
                VexMapsScreen.UseDeviceOrigin,
                VexMapsScreen.PlanRoute,
                VexMapsScreen.EditRoute,
                VexMapsScreen.StartNavigation,
                VexMapsScreen.StopNavigation,
                VexMapsScreen.DoneNavigation,
                VexMapsScreen.RecenterNavigation,
            )

        elements.forEach { element ->
            assertSame(
                element,
                AppScreens.registry.element(
                    element.elementId,
                ),
            )
            assertEquals(
                VexMapsScreen.screenId,
                AppScreens.registry
                    .ownerScreenOf(element)
                    ?.screenId,
            )
        }
    }

    @Test
    fun appearanceSemanticIdentityMatchesPreference() {
        assertSame(
            VexMapsScreen.AppearanceAutomatic,
            VexMapsScreen.appearanceElement(
                VexMapsAppearancePreference.AUTOMATIC,
            ),
        )
        assertSame(
            VexMapsScreen.AppearanceLight,
            VexMapsScreen.appearanceElement(
                VexMapsAppearancePreference.LIGHT,
            ),
        )
        assertSame(
            VexMapsScreen.AppearanceDark,
            VexMapsScreen.appearanceElement(
                VexMapsAppearancePreference.DARK,
            ),
        )
    }

    @Test
    fun travelModeSemanticIdentityMatchesMode() {
        assertSame(
            VexMapsScreen.DriveMode,
            VexMapsScreen.travelModeElement(
                VexMapsTravelMode.DRIVE,
            ),
        )
        assertSame(
            VexMapsScreen.WalkMode,
            VexMapsScreen.travelModeElement(
                VexMapsTravelMode.WALK,
            ),
        )
        assertSame(
            VexMapsScreen.BicycleMode,
            VexMapsScreen.travelModeElement(
                VexMapsTravelMode.BICYCLE,
            ),
        )
    }
}

// [VXG RealForever]
