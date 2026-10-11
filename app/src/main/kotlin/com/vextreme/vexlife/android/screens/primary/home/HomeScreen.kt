package com.vextreme.vexlife.android.screens.primary.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen

object HomeScreen : AppScreenBlueprint(
    screenKey = "home",
    titleRes = R.string.home_title,
) {
    val VexStreamSection = columnRef(
        "body.vexstream",
    )

    val SystemsSection = columnRef(
        "body.systems",
    )

    val ConnectVexStreamMusic = buttonRef(
        id = "body.vexstream.connect",
        labelRes = R.string.home_vexstream_connect,
        style = VexElementStyles.PrimaryAction,
        intent = navigateTo(VexStreamMusicScreen),
    )

    /*
     * Preserve the already-observed semantic ID even though the product has graduated from the
     * provisional "Vex systems" visual grouping into its own Home section.
     */
    val VexStreamMusic = buttonRef(
        id = "body.systems.vexstream-music",
        labelRes = R.string.home_vexstream_open,
        style = VexElementStyles.PrimaryAction,
        intent = navigateTo(VexStreamMusicScreen),
    )

    val VexMaps = buttonRef(
        id = "body.systems.vex-maps",
        labelRes = R.string.home_vexmaps,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(VexMapsScreen),
    )

    val TalkToVex = buttonRef(
        id = "body.systems.talk-to-vex",
        labelRes = R.string.home_talk_to_vex,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(TalkToVexScreen),
    )

    override val content = layout {
        scrollableColumn("body") {
            column(
                element = VexStreamSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(ConnectVexStreamMusic)
                button(VexStreamMusic)
            }

            column(
                element = SystemsSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(VexMaps)
                button(TalkToVex)
            }
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        HomeScreenContent(
            runtime = runtime,
            repositories = repositories,
        )
    }
}

@Composable
internal fun HomeScreenContent(
    runtime: VexRuntime,
    repositories: AppRepositories,
) {
    val vexStreamState by repositories.vexStream.state.collectAsState()
    val paired =
        vexStreamState.connectionState as? VexStreamConnectionState.Paired

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState(),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VexContainer(
            element = HomeScreen.VexStreamSection,
            style = VexElementStyles.RaisedSurface,
        ) {
            Text(
                text = stringResource(
                    R.string.home_vexstream_music,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (paired == null) {
                Text(
                    text = stringResource(
                        R.string.home_vexstream_disconnected_body,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element = HomeScreen.ConnectVexStreamMusic,
                    handler = runtime,
                )
            } else {
                Text(
                    text =
                        if (vexStreamState.hubState is VexStreamHubState.Ready) {
                            stringResource(
                                R.string.home_vexstream_connected,
                                paired.displayName,
                            )
                        } else {
                            stringResource(
                                R.string.home_vexstream_offline,
                            )
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.home_vexstream_connected_body,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element = HomeScreen.VexStreamMusic,
                    handler = runtime,
                )
            }
        }

        VexContainer(
            element = HomeScreen.SystemsSection,
            style = VexElementStyles.RaisedSurface,
        ) {
            Text(
                text = stringResource(
                    R.string.home_systems_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            VexButton(
                element = HomeScreen.VexMaps,
                handler = runtime,
            )
            VexButton(
                element = HomeScreen.TalkToVex,
                handler = runtime,
            )
        }
    }
}

@VexScreenPreviews
@Composable
private fun HomeScreenPreview() {
    VexPreviewHost(
        id = "home",
        startScreen = HomeScreen,
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            repositories = context.repositories,
        )
    }
}

// [VXG RealForever]
