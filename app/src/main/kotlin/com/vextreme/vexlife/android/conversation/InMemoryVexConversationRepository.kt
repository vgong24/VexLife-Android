package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State-only Conversation repository for tests/Preview.
 *
 * This mirrors the VexStream in-memory-repository pattern:
 *
 * - every test/preview constructs its own instance;
 * - it implements the same consumer contract a future real provider must implement;
 * - fixture publication methods represent external provider truth changing;
 * - it performs no Home, network, filesystem, model, federation, or Family-membership effect.
 *
 * [admittedSpeakerRef] stands in for a provider-authenticated current principal. The caller cannot
 * override speaker, recipients, or witness identity through [VexConversationSendRequest].
 */
class InMemoryVexConversationRepository(
    initialState:
        VexConversationState =
        VexConversationState.Empty,
    private val admittedSpeakerRef:
        VexConversationParticipantRef,
) : VexConversationRepository {
    private data class IdempotentAppend(
        val request:
            VexConversationSendRequest,
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
        mutableState
            .asStateFlow()

    /**
     * Fixture/provider publication seam.
     *
     * Tests may use this to represent a new current channel/history projection arriving from the
     * rightful owner. Product UI should not call this as a membership mutation.
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
            VexConversationSendRequest,
    ): VexConversationRepositoryResult {
        val current =
            mutableState.value

        val thread =
            current.thread(
                request.threadRef,
            )
                ?: return VexConversationRepositoryResult
                    .Rejected(
                        VexConversationRepositoryRejection
                            .THREAD_UNKNOWN,
                    )

        if (
            request.spaceRef !=
                thread.spaceRef ||
            request.expectedChannelRef !=
                thread.currentChannelRef ||
            request.expectedMembershipGeneration !=
                thread.currentMembershipGeneration
        ) {
            return VexConversationRepositoryResult
                .Rejected(
                    VexConversationRepositoryRejection
                        .CURRENTNESS_CHANGED,
                )
        }

        if (
            admittedSpeakerRef !in
            thread.currentAudienceRefs
        ) {
            return VexConversationRepositoryResult
                .Rejected(
                    VexConversationRepositoryRejection
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
                VexConversationRepositoryResult
                    .Accepted(
                        messageRef =
                            existing.messageRef,
                    )
            } else {
                VexConversationRepositoryResult
                    .Rejected(
                        VexConversationRepositoryRejection
                            .IDEMPOTENCY_CONFLICT,
                    )
            }
        }

        val recipients =
            currentRecipients(
                thread =
                    thread,
            )

        if (recipients.isEmpty()) {
            return VexConversationRepositoryResult
                .Rejected(
                    VexConversationRepositoryRejection
                        .ADDRESSING_INVALID,
                )
        }

        val nextSequence =
            thread.messages
                .maxOfOrNull { message ->
                    message.sequence
                }
                ?.plus(1L)
                ?: 1L

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
                spaceRef =
                    thread.spaceRef,
                channelRef =
                    thread.currentChannelRef,
                membershipGeneration =
                    thread.currentMembershipGeneration,
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

        return VexConversationRepositoryResult
            .Accepted(
                messageRef =
                    messageRef,
            )
    }

    /**
     * Current Family bridge derives human recipients from trusted channel audience and keeps
     * Companion attention separate from ordinary human delivery.
     *
     * For a non-space direct channel, the fixture addresses every other current participant so a
     * direct human<->Companion pressure case can still be represented without inventing Family rules.
     */
    private fun currentRecipients(
        thread:
            VexConversationThreadState,
    ): List<VexConversationParticipantRef> =
        thread.currentAudienceRefs
            .filter { participantRef ->
                participantRef !=
                    admittedSpeakerRef
            }
            .filter { participantRef ->
                if (
                    thread.spaceRef ==
                    null
                ) {
                    true
                } else {
                    thread.participant(
                        participantRef,
                    )?.kind ==
                        VexConversationParticipantKind.Human
                }
            }
}

// [VXG RealForever]
