package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.conversation.VexConversationMessage
import com.vextreme.vexlife.android.conversation.VexConversationParticipantRef
import com.vextreme.vexlife.android.conversation.VexConversationSurfaceProjection
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexCard
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexText

/**
 * Product-shaped generic Conversation room renderer.
 *
 * This consumes existing [VexConversationSurfaceProjection] truth rather than introducing another
 * room-state owner. Human labels are supplied by the rightful presentation/identity boundary.
 * A developer-environment label is optional presentation context; the renderer does not know how a
 * Conversation Lab works.
 *
 * The parent semantic ref is supplied by the owning Screen/composition. That lets this component be
 * exercised before shared Screen registration is safe without creating a second Screen registry.
 *
 * Durable composer/Send controls are deliberately supplied as [composerContent]. The generic room
 * must not decide that every Conversation has Family Send semantics or create a parallel callback-
 * only action system. A production Screen can therefore provide registered TextField/Button
 * identities and FormSubmission dispatch while Preview remains free to use a local visual composer.
 */
@Composable
internal fun VexConversationRoom(
    projection: VexConversationSurfaceProjection,
    semanticRootRef: String,
    roomTitle: String,
    participantLabel:
        (VexConversationParticipantRef) -> String,
    emptyMessagesLabel: String,
    authenticatedSpeakerSuffix: String,
    developerEnvironmentLabel: String? = null,
    composerContent: @Composable () -> Unit = {},
) {
    require(semanticRootRef.isNotBlank()) {
        "Conversation room semantic root must be non-blank"
    }

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

            composerContent()
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

// [VXG RealForever]
