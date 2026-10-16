package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
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
import com.vextreme.vexlife.android.conversation.VexConversationLab
import com.vextreme.vexlife.android.conversation.VexConversationLabFixture
import com.vextreme.vexlife.android.conversation.VexConversationMessage
import com.vextreme.vexlife.android.conversation.VexConversationParticipantRef
import com.vextreme.vexlife.android.conversation.VexConversationSurfaceProjection
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSurfaceSendAvailability
import com.vextreme.vexlife.android.conversation.projectVexConversationSurface
import com.vextreme.vexlife.android.conversation.projectVexFamilyConversationSendAvailability
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexCard
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Product-shaped Conversation room renderer.
 *
 * This consumes existing [VexConversationSurfaceProjection] truth rather than introducing another
 * room-state owner. Human labels are supplied by the rightful presentation/identity boundary.
 * A developer-environment label is optional presentation context; the renderer does not know how a
 * Conversation Lab works.
 *
 * The parent semantic ref is supplied by the owning Screen/composition. That lets this component be
 * exercised before shared Screen registration is safe without creating a second Screen registry.
 */
@Composable
internal fun VexConversationRoom(
    projection: VexConversationSurfaceProjection,
    semanticRootRef: String,
    roomTitle: String,
    participantLabel:
        (VexConversationParticipantRef) -> String,
    composerLabel: String,
    sendLabel: String,
    emptyMessagesLabel: String,
    authenticatedSpeakerSuffix: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    developerEnvironmentLabel: String? = null,
) {
    require(semanticRootRef.isNotBlank()) {
        "Conversation room semantic root must be non-blank"
    }

    val sendAvailability =
        projectVexFamilyConversationSendAvailability(
            projection,
        )
    val canSend =
        sendAvailability is
            VexFamilyConversationSurfaceSendAvailability.Ready &&
            draft.isNotBlank()

    VexComposeScope(
        parentRef = semanticRootRef,
    ) {
        VexColumn(
            id = "content",
            modifier =
                VexModifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp),
        ) {
            VexText(
                id = "title",
                text = roomTitle,
                role = TextRole.Title,
            )

            VexText(
                id = "participants",
                text =
                    projection
                        .thread
                        .currentAudienceRefs
                        .joinToString(
                            separator = " · ",
                            transform = participantLabel,
                        ),
                role = TextRole.Supporting,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            developerEnvironmentLabel
                ?.takeIf(String::isNotBlank)
                ?.let { environmentLabel ->
                    VexCard(
                        id = "developer-environment",
                        modifier =
                            VexModifier
                                .fillMaxWidth(),
                    ) {
                        VexText(
                            id = "label",
                            text = environmentLabel,
                            role = TextRole.Supporting,
                            modifier =
                                VexModifier
                                    .padding(12.dp),
                        )
                    }
                }

            VexStableLazyColumn(
                listRef =
                    "$semanticRootRef.messages",
                items =
                    projection.thread.messages,
                itemRef = { message ->
                    message.messageRef.value
                },
                contentType = {
                    "conversation-message"
                },
                modifier =
                    VexModifier
                        .weight(1f),
                contentPadding =
                    PaddingValues(
                        vertical = 4.dp,
                    ),
                emptyContent = {
                    VexText(
                        id = "messages-empty",
                        text = emptyMessagesLabel,
                        role = TextRole.Supporting,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        modifier =
                            VexModifier
                                .padding(16.dp),
                    )
                },
            ) { message ->
                ConversationMessageCard(
                    semanticRootRef =
                        semanticRootRef,
                    message = message,
                    speakerLabel =
                        participantLabel(
                            message.speakerRef,
                        ),
                    authenticatedSpeakerSuffix =
                        authenticatedSpeakerSuffix,
                    isAuthenticatedSpeaker =
                        message.speakerRef ==
                            projection
                                .authenticatedParticipantRef,
                )
            }

            ConversationComposer(
                semanticRootRef =
                    semanticRootRef,
                label = composerLabel,
                sendLabel = sendLabel,
                draft = draft,
                enabled = canSend,
                onDraftChange = onDraftChange,
                onSend = {
                    onSend(
                        draft.trim(),
                    )
                },
            )
        }
    }
}

@Composable
private fun ConversationMessageCard(
    semanticRootRef: String,
    message: VexConversationMessage,
    speakerLabel: String,
    authenticatedSpeakerSuffix: String,
    isAuthenticatedSpeaker: Boolean,
) {
    val messageRoot =
        "$semanticRootRef.messages." +
            message.messageRef.value

    VexComposeScope(
        parentRef = messageRoot,
    ) {
        VexCard(
            id = "card",
            modifier =
                VexModifier
                    .fillMaxWidth(),
        ) {
            VexColumn(
                id = "body",
                modifier =
                    VexModifier
                        .padding(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp,
                    ),
            ) {
                VexText(
                    id = "speaker",
                    text =
                        if (isAuthenticatedSpeaker) {
                            "$speakerLabel · $authenticatedSpeakerSuffix"
                        } else {
                            speakerLabel
                        },
                    role = TextRole.Label,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                )
                VexText(
                    id = "content",
                    text = message.content,
                )
            }
        }
    }
}

@Composable
private fun ConversationComposer(
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
    val conversationState by
        repository.state.collectAsState()
    val labState by
        lab.state.collectAsState()
    val thread =
        conversationState.thread(
            VexConversationLabFixture.threadRef,
        )
            ?: return
    val projection =
        projectVexConversationSurface(
            thread = thread,
            authenticatedParticipantRef =
                VexConversationLabFixture
                    .Victor
                    .participant
                    .participantRef,
            homeAvailability =
                labState.homeAvailability,
            companionAvailabilityByParticipantRef =
                mapOf(
                    VexConversationLabFixture
                        .Vex
                        .participant
                        .participantRef to
                        VexConversationCompanionAvailability.Available,
                ),
        )
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
            composerLabel = "Type a message",
            sendLabel = "Send",
            emptyMessagesLabel = "No messages yet.",
            authenticatedSpeakerSuffix = "you",
            draft = draft,
            onDraftChange = {
                draft = it
            },
            onSend = {},
            developerEnvironmentLabel =
                "Conversation Lab · " +
                    labState.scenarioRef,
        )
    }
}

// [VXG RealForever]
