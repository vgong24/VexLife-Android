package com.vextreme.vexlife.android.screens.shared.vex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.toVexConversationGlance
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.rememberVexNavigationGlance
import com.vextreme.vexlife.android.shell.VexNowFeature
import com.vextreme.vexlife.android.shell.VexNowGlyph
import com.vextreme.vexlife.android.shell.VexPresenceOrb
import com.vextreme.vexlife.android.shell.detail
import com.vextreme.vexlife.android.shell.displayTitle
import com.vextreme.vexlife.android.shell.headline
import com.vextreme.vexlife.android.shell.latestLine
import com.vextreme.vexlife.android.shell.stateLabel
import com.vextreme.vexlife.android.ui.VexLifeRelayGraph
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import com.vextreme.vexlife.android.vexstream.rememberVexStreamNowPlaying

/**
 * Talk to Vex, before Vex is connected: an honest context lens.
 *
 * There is no model, session or voice effect here yet (direct Companion Conversation is held until a
 * real provider contract exists). Instead of a developer placeholder, the Screen shows a person the
 * exact bounded glances Vex would receive, each labelled with the owner it is read from. Seeing them
 * grants nothing: any later Vex request converges on the same owners and controls a person uses.
 */
object TalkToVexScreen : AppScreenBlueprint(
    screenKey = "talk-to-vex",
    titleRes = R.string.talk_to_vex_title,
) {
    val StatusSection = columnRef(
        "body.status",
    )

    val LensSection = columnRef(
        "body.lens",
    )

    override val content = layout {
        scrollableColumn("body") {
            column(
                element = StatusSection,
                style = VexElementStyles.RaisedSurface,
            ) {}
            column(
                element = LensSection,
                style = VexElementStyles.RaisedSurface,
            ) {}
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relayGraph: VexLifeRelayGraph,
    ) {
        val music =
            rememberVexStreamNowPlaying()
        val navigation =
            rememberVexNavigationGlance()
        val conversation by
            relayGraph
                .conversation
                .outputStates
                .collectAsState(
                    initial =
                        relayGraph.conversation.currentOutput,
                )

        TalkToVexScreenContent(
            nowPlaying = music?.nowPlaying,
            navigation = navigation,
            family = conversation.toVexConversationGlance(),
        )
    }
}

@Composable
internal fun TalkToVexScreenContent(
    nowPlaying: VexStreamNowPlaying? = null,
    navigation: VexNavigationGlance? = null,
    family: VexConversationGlance = VexConversationGlance.Unobserved,
) {
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
            element = TalkToVexScreen.StatusSection,
            style = VexElementStyles.RaisedSurface,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexPresenceOrb()
                VexText(
                    id = "title",
                    text = stringResource(R.string.talk_to_vex_status_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            VexText(
                id = "body",
                text = stringResource(R.string.talk_to_vex_status_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The future conversation entry, shown inert so its place is visible but nothing is sent.
            VexText(
                id = "composer",
                text = stringResource(R.string.talk_to_vex_composer_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        VexContainer(
            element = TalkToVexScreen.LensSection,
            style = VexElementStyles.RaisedSurface,
        ) {
            VexText(
                id = "title",
                text = stringResource(R.string.talk_to_vex_lens_title),
                style = MaterialTheme.typography.titleMedium,
            )
            VexText(
                id = "body",
                text = stringResource(R.string.talk_to_vex_lens_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TalkToVexLensRow(
                id = "music",
                feature = VexNowFeature.Music,
                label = stringResource(R.string.talk_to_vex_lens_music),
                owner = stringResource(R.string.talk_to_vex_lens_owner_music),
                value =
                    nowPlaying
                        ?.let { playing ->
                            listOf(
                                playing.displayTitle(),
                                playing.artist,
                                playing.stateLabel(),
                            )
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                        }
                        ?: stringResource(R.string.talk_to_vex_lens_nothing_playing),
            )
            TalkToVexLensRow(
                id = "navigation",
                feature = VexNowFeature.Navigation,
                label = stringResource(R.string.talk_to_vex_lens_navigation),
                owner = stringResource(R.string.talk_to_vex_lens_owner_navigation),
                value =
                    navigation
                        ?.let { "${it.headline()} · ${it.detail()}" }
                        ?: stringResource(R.string.talk_to_vex_lens_no_journey),
            )
            TalkToVexLensRow(
                id = "family",
                feature = VexNowFeature.Family,
                label = stringResource(R.string.talk_to_vex_lens_family),
                owner = stringResource(R.string.talk_to_vex_lens_owner_family),
                value =
                    if (family.homeAvailability == VexConversationHomeAvailability.Available) {
                        family.latestLine()
                            ?: stringResource(R.string.home_family_empty_body)
                    } else {
                        stringResource(R.string.talk_to_vex_lens_family_unavailable)
                    },
            )
        }
    }
}

@Composable
private fun TalkToVexLensRow(
    id: String,
    feature: VexNowFeature,
    label: String,
    owner: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VexNowGlyph(
            feature = feature,
            size = 36.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            VexText(
                id = "$id.label",
                text = "$label · $owner",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VexText(
                id = "$id.value",
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@VexScreenPreviews
@Composable
private fun TalkToVexScreenPreview() {
    VexPreviewHost(
        id = "talk-to-vex",
        startScreen = TalkToVexScreen,
    ) {
        TalkToVexScreenContent()
    }
}

// [VXG RealForever]
