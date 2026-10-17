package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * Compact map-view preference control.
 *
 * Appearance is intentionally not a Context Dock page or primary FAB. Provider style refs remain
 * below [VexMapsAppearancePreference]; this surface exposes only Automatic / Light / Dark intent.
 */
@Composable
internal fun VexMapsAppearanceControl(
    runtime: VexRuntime,
    preference: VexMapsAppearancePreference,
    onPreferenceSelected:
        (VexMapsAppearancePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by
        rememberSaveable {
            mutableStateOf(false)
        }

    Box(
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color =
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(
                        alpha = 0.82f,
                    ),
            contentColor =
                MaterialTheme
                    .colorScheme
                    .onSurface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            IconButton(
                modifier =
                    Modifier.vexElement(
                        VexMapsScreen
                            .OpenAppearance,
                    ),
                onClick = {
                    runtime.activate(
                        VexMapsScreen
                            .OpenAppearance,
                    )
                    expanded = true
                },
            ) {
                Icon(
                    imageVector =
                        preference.icon(),
                    contentDescription =
                        stringResource(
                            R.string
                                .vexmaps_appearance,
                        ),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
        ) {
            VexMapsAppearancePreference
                .entries
                .forEach { option ->
                    val element =
                        VexMapsScreen
                            .appearanceElement(
                                option,
                            )
                    DropdownMenuItem(
                        modifier =
                            Modifier.vexElement(
                                element,
                            ),
                        text = {
                            Text(
                                stringResource(
                                    option.labelRes(),
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    option.icon(),
                                contentDescription =
                                    null,
                            )
                        },
                        trailingIcon =
                            if (
                                option ==
                                preference
                            ) {
                                {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.Check,
                                        contentDescription =
                                            null,
                                    )
                                }
                            } else {
                                null
                            },
                        onClick = {
                            runtime.activate(
                                element,
                            )
                            expanded = false
                            onPreferenceSelected(
                                option,
                            )
                        },
                    )
                }
        }
    }
}

private fun VexMapsAppearancePreference.icon():
    ImageVector =
    when (this) {
        VexMapsAppearancePreference.AUTOMATIC ->
            Icons.Filled.BrightnessAuto

        VexMapsAppearancePreference.LIGHT ->
            Icons.Filled.LightMode

        VexMapsAppearancePreference.DARK ->
            Icons.Filled.DarkMode
    }

private fun VexMapsAppearancePreference.labelRes():
    Int =
    when (this) {
        VexMapsAppearancePreference.AUTOMATIC ->
            R.string
                .vexmaps_appearance_automatic

        VexMapsAppearancePreference.LIGHT ->
            R.string
                .vexmaps_appearance_light

        VexMapsAppearancePreference.DARK ->
            R.string
                .vexmaps_appearance_dark
    }

@Suppress("UNUSED_PARAMETER")
private fun semanticButton(
    element: ButtonElement,
) {
    // Type anchor only: appearance actions remain registered ButtonElements even though this custom
    // native Maps control renders them as icon/menu items rather than generic VexButton cells.
}

// [VXG RealForever]
