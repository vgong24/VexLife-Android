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
 * override speaker or witness identity through [VexConversationSendRequest].
 */
class InMemoryVexConversationRepository(
    initialState:
        VexConversationState =
        VexConversationState.Empty,
    private val admittedSpeakerRef:
        VexConversationParticipantRef,
) : VexConversationRepository {
    private val mutableState =
        MutableStateFlow(
            initialState,
        )

    override val state:
        StateFlow<VexConversationState> =
        mutableState
            .asStateFlow()

    /**
     * Fixture/provider publication seam.
     *
     * Tests may use this to represent a new current channel generation arriving from the rightful
     * owner. Product UI should not call this as a membership mutation.
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

        if (
            admittedSpeakerRef in
                request.recipientRefs ||
            !request.recipientRefs.all(
                thread.currentAudienceRefs::contains,
            )
        ) {
            return VexConversationRepositoryResult
                .Rejected(
                    VexConversationRepositoryRejection
                        .ADDRESSING_INVALID,
                )
        }

        val nextSequence =
            thread.timeline
                .filterIsInstance<
                    VexConversationTimelineEntry.Message
                >()
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
            VexConversationTimelineEntry.Message(
                entryRef =
                    messageRef.value,
                threadRef =
                    thread.threadRef,
                messageRef =
                    messageRef,
                channelRef =
                    thread.currentChannelRef,
                membershipGeneration =
                    thread.currentMembershipGeneration,
                sequence =
                    nextSequence,
                speakerRef =
                    admittedSpeakerRef,
                recipientRefs =
                    request.recipientRefs,
                witnessRefs =
                    thread.currentAudienceRefs,
                content =
                    request.content,
            )

        publishThread(
            thread.copy(
                timeline =
                    thread.timeline +
                        message,
            ),
        )

        return VexConversationRepositoryResult
            .Accepted(
                messageRef =
                    messageRef,
            )
    }
}

// [VXG RealForever]
