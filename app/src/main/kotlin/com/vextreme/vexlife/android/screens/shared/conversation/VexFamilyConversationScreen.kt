package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.VexConversationHybridRelay
import com.vextreme.vexlife.android.conversation.VexFamilyConversationConnectedServiceRepository
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.ui.VexLifeRelays

/**
 * First installed Family Conversation doorway backed by accepted VCS-04 private Home read truth.
 *
 * This Screen does not own pairing, route authority, session credentials, Family membership,
 * Conversation history, Send semantics, Companion runtime, or restart behavior. It asks the current
 * server bootstrap for exactly one Family room, refreshes that canonical read projection, and renders
 * the existing Conversation Hybrid output.
 */
object VexFamilyConversationScreen : AppScreenBlueprint(
    screenKey = "family-conversation",
    titleRes = R.string.conversation_family_title,
) {
    override val content =
        layout {
            column("body") {
                text(
                    id = "state",
                    textRes =
                        R.string.conversation_loading,
                )
            }
        }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relays: VexLifeRelays,
        darkTheme: Boolean,
    ) {
        VexFamilyConversationScreenContent(
            repository =
                repositories.conversation as?
                    VexFamilyConversationConnectedServiceRepository,
            relay =
                relays.conversation,
        )
    }
}

@Composable
internal fun VexFamilyConversationScreenContent(
    repository:
        VexFamilyConversationConnectedServiceRepository?,
    relay:
        VexConversationHybridRelay,
) {
    var refreshFailed by
        remember(
            repository,
        ) {
            mutableStateOf(
                false,
            )
        }

    LaunchedEffect(
        repository,
        relay,
    ) {
        relay.authenticatedParticipantObserved(
            null,
        )

        if (repository == null) {
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Unavailable,
            )
            return@LaunchedEffect
        }

        runCatching {
            repository
                .refreshSingleCurrentRoom()
        }.onSuccess { thread ->
            relay.selectThread(
                thread.threadRef,
            )
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Available,
            )
            refreshFailed =
                false
        }.onFailure {
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Unavailable,
            )
            refreshFailed =
                true
        }
    }

    val hybrid by
        relay.outputStates
            .collectAsState(
                initial =
                    relay.currentOutput,
            )
    val projection =
        hybrid.surface

    if (projection == null) {
        VexComposeScope(
            parentRef =
                VexFamilyConversationScreen.screenId,
        ) {
            VexColumn(
                id = "content",
                modifier =
                    VexModifier
                        .fillMaxSize()
                        .padding(
                            16.dp,
                        ),
            ) {
                VexText(
                    id = "state",
                    text =
                        stringResource(
                            when {
                                repository == null ->
                                    R.string
                                        .conversation_route_unavailable

                                refreshFailed ->
                                    R.string
                                        .conversation_read_unavailable

                                else ->
                                    R.string
                                        .conversation_loading
                            },
                        ),
                    role =
                        TextRole.Supporting,
                )
            }
        }
        return
    }

    VexConversationRoom(
        projection =
            projection,
        semanticRootRef =
            VexFamilyConversationScreen.screenId,
        roomTitle =
            stringResource(
                R.string.conversation_family_title,
            ),
        participantLabel = { participantRef ->
            participantRef.value
        },
        emptyMessagesLabel =
            stringResource(
                R.string.conversation_empty,
            ),
        authenticatedSpeakerSuffix =
            stringResource(
                R.string.conversation_authenticated_suffix,
            ),
        developerEnvironmentLabel =
            if (
                projection.homeAvailability ==
                VexConversationHomeAvailability
                    .Unavailable
            ) {
                stringResource(
                    R.string.conversation_home_unavailable,
                )
            } else {
                null
            },
    )
}

@VexScreenPreviews
@Composable
private fun VexFamilyConversationScreenPreview() {
    VexPreviewHost(
        id = "family-conversation",
        startScreen =
            VexFamilyConversationScreen,
    )
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][INSTALLED-READ]
