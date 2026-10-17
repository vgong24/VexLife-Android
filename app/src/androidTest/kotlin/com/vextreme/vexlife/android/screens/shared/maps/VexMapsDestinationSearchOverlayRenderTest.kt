package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.ui.VexLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class VexMapsDestinationSearchOverlayRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun browseFrontDoorShowsWhereToAndSearchAction() {
        val runtime =
            runtime()

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    VexMapsDestinationSearchOverlay(
                        runtime = runtime,
                        query = "",
                        state =
                            VexMapsDestinationSearchState
                                .Idle,
                        onQueryChange = {},
                        onSearch = {},
                        onPlaceSelected = {},
                    )
                }
            }
        }

        compose
            .onNodeWithText(
                "Where to?",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithContentDescription(
                "Search",
            )
            .assertIsDisplayed()
    }

    @Test
    fun resultSelectionReturnsTheChosenDestination() {
        val runtime =
            runtime()
        val selected =
            mutableStateOf<
                VexMapsPlaceSearchResult?
            >(null)
        val place =
            VexMapsPlaceSearchResult(
                placeRef =
                    "android-geocoder.test",
                name =
                    "Palmdale",
                subtitle =
                    "Palmdale, CA, USA",
                coordinate =
                    VexMapCoordinate(
                        latitude =
                            34.5794,
                        longitude =
                            -118.1165,
                    ),
            )

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    VexMapsDestinationSearchOverlay(
                        runtime = runtime,
                        query = "93550",
                        state =
                            VexMapsDestinationSearchState
                                .Results(
                                    listOf(place),
                                ),
                        onQueryChange = {},
                        onSearch = {},
                        onPlaceSelected = {
                            selected.value = it
                        },
                    )
                }
            }
        }

        compose
            .onNodeWithText(
                "Palmdale",
            )
            .assertIsDisplayed()
            .performClick()

        assertSame(
            place,
            selected.value,
        )
        assertEquals(
            "Palmdale, CA, USA",
            place.subtitle,
        )
    }

    private fun runtime() =
        VexRuntime(
            registry = AppScreens.registry,
            logger = VexLogger(),
            initialScreen =
                AppScreens.primary.first(),
            primaryScreens =
                AppScreens.primary,
            currentAppActions = {
                AppActionRegistrations.Empty
            },
        ).apply {
            navigation.push(
                VexMapsScreen,
            )
        }
}

// [VXG RealForever]
