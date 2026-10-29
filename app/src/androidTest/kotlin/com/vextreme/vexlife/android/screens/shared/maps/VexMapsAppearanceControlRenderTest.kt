package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.ui.VexLifeTheme
import com.vextreme.vexlife.android.ui.VexThemePresentation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VexMapsAppearanceControlRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactControlExposesHumanAppearanceChoices() {
        var selected =
            VexMapsAppearancePreference
                .AUTOMATIC
        val runtime =
            VexRuntime(
                registry = AppScreens.registry,
                logger = VexLogger(),
                initialScreen = AppScreens.primary.first(),
                primaryScreens = AppScreens.primary,
                currentAppActions = {
                    AppActionRegistrations.Empty
                },
            ).apply {
                navigation.push(
                    VexMapsScreen,
                )
            }

        compose.setContent {
            VexLifeTheme(
                presentation =
                    VexThemePresentation.Dark,
            ) {
                Surface {
                    VexMapsAppearanceControl(
                        runtime = runtime,
                        preference =
                            VexMapsAppearancePreference
                                .AUTOMATIC,
                        onPreferenceSelected = {
                            selected = it
                        },
                    )
                }
            }
        }

        compose
            .onNodeWithContentDescription(
                "Map appearance",
            )
            .assertIsDisplayed()
            .performClick()

        compose
            .onNodeWithText(
                "Automatic",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "Light",
            )
            .assertIsDisplayed()
            .performClick()

        assertEquals(
            VexMapsAppearancePreference.LIGHT,
            selected,
        )
    }
}

// [VXG RealForever]
