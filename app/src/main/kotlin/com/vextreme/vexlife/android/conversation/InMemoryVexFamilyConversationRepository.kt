package com.vextreme.vexlife.android.conversation

import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State-only Family Conversation repository for tests/Preview.
 *
 * Every test/preview constructs its own instance. This implements the same Family write contract a
 * future Home-backed VF-02C adapter must implement.
 *
 * Message identity deliberately mirrors VF-02C's semantic SCOPE, not its exact JavaScript hash
 * bytes:
 *
 * spaceRef + channelRef + authenticated principalRef + idempotencyKey
 *
 * Idempotent retry is resolved from the deterministic message identity already present in current
 * Conversation state, not from process-local retry bookkeeping.
 */
class InMemoryVexFamilyConversationRepository(
    initialState:
        VexConversationState =
        VexConversationState.Empty,
    private val admittedSpeakerRef:
        VexConversationParticipantRef,
) : VexFamilyConversationRepository {
    private data class MessageIdentityScope(
        val spaceRef:
            VexConversationSpaceRef,
        val channelRef:
            VexConversationChannelRef,
        val principalRef:
            VexConversationParticipantRef,
        val idempotencyKey:
            VexConversationIdempotencyKey,
    )

    private val mutableState =
        MutableStateFlow(
            initialState,
        )

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

        /*
         * VF-02C establishes current Family/channel truth before retry lookup. Preserve that order:
         * a request bound to an older channel/generation remains stale even when its key was once
         * valid there.
         */
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

        val identityScope =
            MessageIdentityScope(
                spaceRef =
                    family.spaceRef,
                channelRef =
                    thread.currentChannelRef,
                principalRef =
                    admittedSpeakerRef,
                idempotencyKey =
                    request.idempotencyKey,
            )

        val messageRef =
            deterministicMessageRef(
                identityScope,
            )

        val existing =
            thread.messages
                .firstOrNull { message ->
                    message.messageRef ==
                        messageRef
                }

        if (existing != null) {
            val existingFamily =
                existing.familyBinding

            val sameRetryMeaning =
                existing.threadRef ==
                    request.threadRef &&
                    existing.channelRef ==
                    request.expectedChannelRef &&
                    existing.speakerRef ==
                    admittedSpeakerRef &&
                    existingFamily?.spaceRef ==
                    request.spaceRef &&
                    existingFamily.membershipGeneration ==
                    request.expectedMembershipGeneration &&
                    existing.content ==
                    request.content &&
                    existing.recipientRefs ==
                    recipients

            return if (sameRetryMeaning) {
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

        return VexFamilyConversationRepositoryResult
            .Accepted(
                messageRef =
                    messageRef,
            )
    }

    private fun deterministicMessageRef(
        scope:
            MessageIdentityScope,
    ): VexConversationMessageRef {
        val canonical =
            listOf(
                scope.spaceRef.value,
                scope.channelRef.value,
                scope.principalRef.value,
                scope.idempotencyKey.value,
            ).joinToString(
                separator = "\u001f",
            )

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    canonical
                        .toByteArray(
                            Charsets.UTF_8,
                        ),
                )
                .joinToString(
                    separator = "",
                ) { byte ->
                    "%02x".format(
                        byte.toInt() and
                            0xff,
                    )
                }

        return VexConversationMessageRef(
            "message.in-memory.vex-family." +
                digest.take(32),
        )
    }
}

// [VXG RealForever]
