package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.conversation.InMemoryVexFamilyConversationRepository
import com.vextreme.vexlife.android.conversation.VexConversationCompanionAvailability
import com.vextreme.vexlife.android.conversation.VexConversationConvergenceRelay
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.VexConversationLab
import com.vextreme.vexlife.android.conversation.VexConversationLabFixture
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSurfaceSendAvailability
import com.vextreme.vexlife.android.conversation.projectVexFamilyConversationSendAvailability
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.ui.VexLifeTheme
import kotlinx.coroutines.awaitCancellation

/**
 * Lived-design preview for the feature-owned Conversation Lab.
 *
 * The raw Material composer here is intentionally Preview-only. It pressures the visual room before
 * shared app registration is safe, but it is not the production Send contract. A registered Screen
 * must later supply durable TextField/Button identity and FormSubmission/AppAction dispatch.
 */
@Preview(
    name = "Conversation Lab · Family room",
    showBackground = true,
)
@Composable
private fun VexConversationRoomPreview() {
    val repository =
        remember {
            InMemoryVexFamilyConversationRepository(
                admittedSpeakerRef =
                    VexConversationLabFixture
                        .Victor
                        .participant
                        .participantRef,
            )
        }
    val lab =
        remember(repository) {
            VexConversationLab(
                repository,
            ).also { conversationLab ->
                conversationLab.seedFamilyRoom()
                conversationLab.publishRemoteMessage(
                    content = "Yep, leaving now.",
                    speakerRef =
                        VexConversationLabFixture
                            .Maya
                            .participant
                            .participantRef,
                )
            }
        }
    val convergenceRelay =
        remember(
            repository,
            lab,
        ) {
            VexConversationConvergenceRelay(
                conversationRepository =
                    repository,
                conversationLab =
                    lab,
            ).also { relay ->
                relay.selectThread(
                    VexConversationLabFixture.threadRef,
                )
                relay.authenticatedParticipantObserved(
                    VexConversationLabFixture
                        .Victor
                        .participant
                        .participantRef,
                )
                relay.homeAvailabilityObserved(
                    VexConversationHomeAvailability.Available,
                )
                relay.companionAvailabilityObserved(
                    mapOf(
                        VexConversationLabFixture
                            .Vex
                            .participant
                            .participantRef to
                            VexConversationCompanionAvailability.Available,
                    ),
                )
            }
        }

    LaunchedEffect(
        convergenceRelay,
    ) {
        convergenceRelay.activate(this)
        try {
            awaitCancellation()
        } finally {
            convergenceRelay.deactivate()
        }
    }

    val convergence by
        convergenceRelay
            .outputStates
            .collectAsState(
                initial =
                    convergenceRelay.currentOutput,
            )
    val projection =
        convergence.surface
            ?: return
    val labels =
        remember {
            VexConversationLabFixture
                .all
                .associate { identity ->
                    identity
                        .participant
                        .participantRef to
                        identity.displayName
                }
        }
    var draft by
        rememberSaveable {
            mutableStateOf(
                "Are you coming?",
            )
        }

    VexLifeTheme(
        darkTheme = true,
    ) {
        VexConversationRoom(
            projection = projection,
            semanticRootRef =
                "preview.conversation.family-room",
            roomTitle = "Family",
            participantLabel = { participantRef ->
                requireNotNull(
                    labels[participantRef],
                ) {
                    "missing preview label for $participantRef"
                }
            },
            emptyMessagesLabel = "No messages yet.",
            authenticatedSpeakerSuffix = "you",
            developerEnvironmentLabel =
                "Conversation Lab active",
            composerContent = {
                val sendAvailability =
                    projectVexFamilyConversationSendAvailability(
                        projection,
                    )
                val canSend =
                    sendAvailability is
                        VexFamilyConversationSurfaceSendAvailability.Ready &&
                        draft.isNotBlank()

                PreviewConversationComposer(
                    semanticRootRef =
                        "preview.conversation.family-room",
                    label = "Type a message",
                    sendLabel = "Send",
                    draft = draft,
                    enabled = canSend,
                    onDraftChange = {
                        draft = it
                    },
                    onSend = {},
                )
            },
        )
    }
}

@Composable
private fun PreviewConversationComposer(
    semanticRootRef: String,
    label: String,
    sendLabel: String,
    draft: String,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val composerRef =
        "$semanticRootRef.composer"
    val sendRef =
        "$semanticRootRef.composer.send"

    VexRow(
        id = "composer-row",
        modifier =
            VexModifier
                .fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp,
            ),
    ) {
        val composerModifier =
            VexModifier
                .weight(1f)
                .vex(
                    composerRef,
                )

        VexComposeScope(
            parentRef = composerRef,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier =
                    composerModifier,
                label = {
                    VexText(
                        id = "label",
                        text = label,
                        role = TextRole.Label,
                    )
                },
                singleLine = false,
                maxLines = 4,
            )
        }

        VexComposeScope(
            parentRef = sendRef,
        ) {
            Button(
                onClick = onSend,
                modifier =
                    VexModifier
                        .vex(
                            sendRef,
                        ),
                enabled = enabled,
            ) {
                VexText(
                    id = "label",
                    text = sendLabel,
                    role = TextRole.Label,
                )
            }
        }
    }
}

// [VXG RealForever]
