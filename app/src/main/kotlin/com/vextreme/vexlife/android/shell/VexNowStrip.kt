package com.vextreme.vexlife.android.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.rememberVexNavigationGlance
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import com.vextreme.vexlife.android.vexstream.rememberVexStreamNowPlaying
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen

/**
 * Live Now strip: reads each feature's own glance and hides a glance on the Screen that owns it.
 *
 * Home is excluded because Home already shows the full Now cards. The host never owns playback or
 * navigation truth; it only decides where already-owned glances are worth repeating.
 */
@Composable
internal fun VexNowStripHost(
    runtime:
        VexRuntime,
) {
    val music =
        rememberVexStreamNowPlaying()
    val navigation =
        rememberVexNavigationGlance()
    val screenId =
        runtime.currentScreen.screenId
    val onHome =
        screenId ==
            HomeScreen.screenId

    VexNowStrip(
        runtime =
            runtime,
        nowPlaying =
            music
                ?.nowPlaying
                ?.takeUnless {
                    onHome ||
                        screenId ==
                        VexStreamMusicScreen.screenId
                },
        navigation =
            navigation
                ?.takeUnless {
                    onHome ||
                        screenId ==
                        VexMapsScreen.screenId
                },
        onTogglePlayback = {
            music
                ?.togglePlayback
                ?.invoke()
        },
    )
}

/**
 * Presentational Now strip. Renders nothing when there is nothing current to show.
 *
 * Tapping a row navigates to the owning Screen; the play/pause control asks the Playback Family
 * through [onTogglePlayback]. Preview and screenshot fixtures call this directly.
 */
@Composable
internal fun VexNowStrip(
    runtime:
        VexRuntime,
    nowPlaying:
        VexStreamNowPlaying?,
    navigation:
        VexNavigationGlance?,
    onTogglePlayback:
        () -> Unit,
) {
    AnimatedVisibility(
        visible =
            nowPlaying != null ||
                navigation != null,
        enter =
            expandVertically() +
                fadeIn(),
        exit =
            shrinkVertically() +
                fadeOut(),
    ) {
        VexContainer(
            element =
                AppShell.Elements.NowStrip,
            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 6.dp,
                ),
            style =
                VexElementStyles.RaisedSurface,
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp,
                    ),
            ) {
                navigation?.let { glance ->
                    VexNowStripRow(
                        runtime =
                            runtime,
                        open =
                            AppShell.Elements.NavigationOpen,
                        feature =
                            VexNowFeature.Navigation,
                        headline =
                            glance.headline(),
                        detail =
                            glance.detail(),
                    )
                }
                nowPlaying?.let { playing ->
                    VexNowStripRow(
                        runtime =
                            runtime,
                        open =
                            AppShell.Elements.NowPlayingOpen,
                        feature =
                            VexNowFeature.Music,
                        headline =
                            playing.displayTitle(),
                        detail =
                            listOf(
                                playing.artist,
                                playing.stateLabel(),
                            )
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                    ) {
                        VexIconButton(
                            element =
                                AppShell.Elements.NowPlayingToggle,
                            handler =
                                ElementActionHandler { element ->
                                    runtime.activate(
                                        element,
                                    )
                                    onTogglePlayback()
                                },
                            enabled =
                                playing.playPauseAvailable &&
                                    !playing.transitioning,
                            iconOverride =
                                playing.toggleIcon,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VexNowStripRow(
    runtime:
        VexRuntime,
    open:
        ButtonElement,
    feature:
        VexNowFeature,
    headline:
        String,
    detail:
        String,
    trailing:
        (@Composable () -> Unit)? =
        null,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .vex(
                        open,
                    )
                    .clickable {
                        runtime.activate(
                            open,
                        )
                    },
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            VexNowGlyph(
                feature =
                    feature,
            )
            Column(
                modifier =
                    Modifier.weight(1f),
            ) {
                VexText(
                    id =
                        feature.name.lowercase() + ".headline",
                    text =
                        headline,
                    style =
                        MaterialTheme.typography.titleSmall,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis,
                )
                if (detail.isNotBlank()) {
                    VexText(
                        id =
                            feature.name.lowercase() + ".detail",
                        text =
                            detail,
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

// [VXG RealForever][SHELL][NOW-STRIP]
