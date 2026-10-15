package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State-only Family Conversation repository for tests/Preview.
 *
 * Every test/preview constructs its own instance. This implements the same Family write contract a
 * future Home-backed VF-02C adapter must implement.
 */
class InMemoryVexFamilyConversationRepository(
    initialState:
        VexConversationState =
        VexConversationState.Empty,
    private val admittedSpeakerRef:
        VexConversationParticipantRef,
) : VexFamilyConversationRepository {
    private data class IdempotentAppend(
        val request:
            VexFamilyConversationSendRequest,
        val messageRef:
            VexConversationMessageRef,
    )

    private val mutableState =
        MutableStateFlow(
            initialState,
        )

    private val appendByIdempotencyKey =
        linkedMapOf<
            VexConversationIdempotencyKey,
            IdempotentAppend
        >()

    override val state:
        StateFlow<VexConversationState> =
        mutableState.asStateFlow()

    /**
     * Test/provider publication seam. Product UI does not own this as a membership mutation API.
     */
    fun publishThread(
        thread:
            VexConversationThreadState,
    ) {
        val current =
            mutableState.value

        mutableState.value =
            current.copy(
                threads =
                    current.threads
                        .filterNot { existing ->
                            existing.threadRef ==
                                thread.threadRef
                        } +
                        thread,
            )
    }

    override suspend fun send(
        request:
            VexFamilyConversationSendRequest,
    ): VexFamilyConversationRepositoryResult {
        val current =
            mutableState.value

        val thread =
            current.thread(
                request.threadRef,
            )
                ?: return VexFamilyConversationRepositoryResult
                    .Rejected(
                        VexFamilyConversationRepositoryRejection
                            .THREAD_UNKNOWN,
                    )

        val family =
            thread.familyBinding
                ?: return VexFamilyConversationRepositoryResult
                    .Rejected(
                        VexFamilyConversationRepositoryRejection
                            .THREAD_NOT_FAMILY_BOUND,
                    )

        if (
            request.spaceRef !=
                family.spaceRef ||
            request.expectedChannelRef !=
                thread.currentChannelRef ||
            request.expectedMembershipGeneration !=
                family.currentMembershipGeneration
        ) {
            return VexFamilyConversationRepositoryResult
                .Rejected(
                    VexFamilyConversationRepositoryRejection
                        .CURRENTNESS_CHANGED,
                )
        }

        if (
            admittedSpeakerRef !in
            thread.currentAudienceRefs
        ) {
            return VexFamilyConversationRepositoryResult
                .Rejected(
                    VexFamilyConversationRepositoryRejection
                        .SPEAKER_NOT_CURRENT,
                )
        }

        val existing =
            appendByIdempotencyKey[
                request.idempotencyKey
            ]

        if (existing != null) {
            return if (
                existing.request ==
                    request
            ) {
                VexFamilyConversationRepositoryResult
                    .Accepted(
                        messageRef =
                            existing.messageRef,
                    )
            } else {
                VexFamilyConversationRepositoryResult
                    .Rejected(
                        VexFamilyConversationRepositoryRejection
                            .IDEMPOTENCY_CONFLICT,
                    )
            }
        }

        val recipients =
            thread.currentAudienceRefs
                .filter { participantRef ->
                    participantRef !=
                        admittedSpeakerRef
                }
                .filter { participantRef ->
                    thread.participant(
                        participantRef,
                    )?.kind ==
                        VexConversationParticipantKind.Human
                }

        if (recipients.isEmpty()) {
            return VexFamilyConversationRepositoryResult
                .Rejected(
                    VexFamilyConversationRepositoryRejection
                        .ADDRESSING_INVALID,
                )
        }

        val nextSequence =
            thread.messages
                .filter { message ->
                    message.channelRef ==
                        thread.currentChannelRef
                }
                .maxOfOrNull { message ->
                    message.sequence
                }
                ?.plus(1L)
                ?: 0L

        val messageRef =
            VexConversationMessageRef(
                "message.in-memory." +
                    thread.threadRef.value +
                    "." +
                    nextSequence
                        .toString()
                        .padStart(
                            length = 3,
                            padChar = '0',
                        ),
            )

        val message =
            VexConversationMessage(
                messageRef =
                    messageRef,
                threadRef =
                    thread.threadRef,
                channelRef =
                    thread.currentChannelRef,
                sequence =
                    nextSequence,
                speakerRef =
                    admittedSpeakerRef,
                recipientRefs =
                    recipients,
                witnessRefs =
                    thread.currentAudienceRefs,
                content =
                    request.content,
                familyBinding =
                    VexFamilyConversationMessageBinding(
                        spaceRef =
                            family.spaceRef,
                        membershipGeneration =
                            family.currentMembershipGeneration,
                    ),
            )

        publishThread(
            thread.copy(
                messages =
                    thread.messages +
                        message,
            ),
        )

        appendByIdempotencyKey[
            request.idempotencyKey
        ] =
            IdempotentAppend(
                request =
                    request,
                messageRef =
                    messageRef,
            )

        return VexFamilyConversationRepositoryResult
            .Accepted(
                messageRef =
                    messageRef,
            )
    }
}

// [VXG RealForever]
