package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.ui.VexLifeRelays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * First registered production Family Conversation surface.
 *
 * The Screen owns only the human request to refresh the currently singular Family room and the
 * presentation of existing Conversation Hybrid output. It does not own Home pairing, private-route
 * materialization, session credentials, Family authority, Send, Companion availability, or identity
 * display names.
 */
object VexFamilyConversationScreen : AppScreenBlueprint(
    screenKey = "family-conversation",
    titleRes = R.string.family_conversation_title,
) {
    val Refresh = buttonRef(
        id = "body.actions.refresh",
        labelRes = R.string.family_conversation_refresh,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(
            AppActions.RefreshFamilyConversation,
        ),
    )

    override val content =
        layout {
            scrollableColumn(
                "body",
            ) {
                text(
                    id = "intro",
                    textRes =
                        R.string.family_conversation_intro,
                )
                button(
                    Refresh,
                )
            }
        }

    internal override fun appActionRegistrations(
        context:
            ScreenActionContext,
    ): AppActionRegistrations =
        AppActionRegistrations.of(
            appAction(
                action =
                    AppActions.RefreshFamilyConversation,
                ownerRef =
                    screenId,
            ) {
                context.scope.launch {
                    val repository =
                        context.repositories.conversation
                    val relay =
                        context.relays
                            ?.conversation

                    if (
                        repository == null ||
                        relay == null
                    ) {
                        relay?.homeAvailabilityObserved(
                            VexConversationHomeAvailability
                                .Unavailable,
                        )
                        return@launch
                    }

                    try {
                        val thread =
                            repository
                                .refreshOnlyCurrentFamilyRoom()
                        relay.selectThread(
                            thread.threadRef,
                        )
                        relay.homeAvailabilityObserved(
                            VexConversationHomeAvailability
                                .Available,
                        )
                    } catch (_: Exception) {
                        relay.homeAvailabilityObserved(
                            VexConversationHomeAvailability
                                .Unavailable,
                        )
                    }
                }
            },
        )

    @Composable
    internal override fun renderContent(
        runtime:
            VexRuntime,
        repositories:
            AppRepositories,
        relays:
            VexLifeRelays,
        sessionScope:
            CoroutineScope,
        darkTheme:
            Boolean,
    ) {
        VexFamilyConversationScreenContent(
            runtime =
                runtime,
            relays =
                relays,
        )
    }
}

@Composable
internal fun VexFamilyConversationScreenContent(
    runtime:
        VexRuntime,
    relays:
        VexLifeRelays,
) {
    val convergence by
        relays.conversation
            .outputStates
            .collectAsState(
                initial =
                    relays.conversation
                        .currentOutput,
            )

    val surface =
        convergence.surface

    if (surface == null) {
        VexFamilyConversationScreen
            .renderRegisteredContent(
                runtime,
            )
        return
    }

    VexConversationRoom(
        projection =
            surface,
        semanticRootRef =
            VexFamilyConversationScreen
                .screenId,
        roomTitle =
            stringResource(
                R.string.family_conversation_room_title,
            ),
        participantLabel = { participantRef ->
            participantRef.value
        },
        emptyMessagesLabel =
            stringResource(
                R.string.family_conversation_empty,
            ),
        authenticatedSpeakerSuffix =
            stringResource(
                R.string.family_conversation_you,
            ),
    )
}

/**
 * Keeps the empty/unavailable surface on the registered semantic tree without teaching the Screen a
 * second layout language.
 */
@Composable
private fun VexFamilyConversationScreen.renderRegisteredContent(
    runtime:
        VexRuntime,
) {
    com.vextreme.vexlife.android.elements.VexElementTree(
        placement =
            content,
        registry =
            runtime.registry,
        handler =
            runtime,
        selectedChild =
            runtime.presentation::selected,
    )
}

@VexScreenPreviews
@Composable
private fun VexFamilyConversationScreenPreview() {
    VexPreviewHost(
        id = "family-conversation",
        startScreen =
            VexFamilyConversationScreen,
    ) { context ->
        VexFamilyConversationScreenContent(
            runtime =
                context.runtime,
            relays =
                context.relays,
        )
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][REGISTERED-READ-SCREEN]
