package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.StateFlow

@JvmInline
value class VexConversationIdempotencyKey(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "Conversation idempotency key must be non-blank"
        }
        require(value.length <= 512) {
            "Conversation idempotency key must be at most 512 characters"
        }
    }

    override fun toString(): String = value
}

/**
 * Android consumer boundary for portable Conversation state and admitted message requests.
 *
 * This repository does NOT become the portable Conversation semantic owner. A future Home-backed
 * implementation must consume the accepted Home Bridge / Family / service.conversation contracts and
 * project only the result Android needs.
 *
 * The request deliberately cannot author:
 *
 * - speakerRef / authenticated principal;
 * - recipientRefs for the current Family path;
 * - witnessRefs / current audience authority;
 * - Home/session capability evidence;
 * - model/companion availability.
 *
 * The provider derives those truths from the current channel/authority contract.
 */
interface VexConversationRepository {
    val state:
        StateFlow<VexConversationState>

    suspend fun send(
        request:
            VexConversationSendRequest,
    ): VexConversationRepositoryResult
}

/**
 * Client request bound to the Conversation truth the human saw while composing.
 *
 * The shape intentionally aligns with the current VexLife VF-02C Family Conversation bridge:
 *
 * spaceRef
 * channelRef
 * content
 * expectedMembershipGeneration
 * idempotencyKey
 *
 * [threadRef] remains an Android-local routing/current-thread coordinate; a Home-backed adapter does
 * not need to send it to an HTTP owner that already resolves thread identity from the trusted channel.
 *
 * The caller does not supply speaker, recipients, or witnesses. That prevents Android from becoming
 * an authority source merely because it can display participant identities.
 */
data class VexConversationSendRequest(
    val threadRef:
        VexConversationThreadRef,
    val spaceRef:
        VexConversationSpaceRef?,
    val expectedChannelRef:
        VexConversationChannelRef,
    val expectedMembershipGeneration:
        Long,
    val idempotencyKey:
        VexConversationIdempotencyKey,
    val content: String,
) {
    init {
        require(expectedMembershipGeneration > 0L) {
            "expected membership generation must be positive"
        }
        require(content.isNotBlank()) {
            "Conversation Send content must be non-blank"
        }
    }
}

enum class VexConversationRepositoryRejection {
    THREAD_UNKNOWN,
    CURRENTNESS_CHANGED,
    SPEAKER_NOT_CURRENT,
    ADDRESSING_INVALID,
    IDEMPOTENCY_CONFLICT,
}

/**
 * Small consumer result until a real Home-backed provider teaches richer delivery/receipt semantics.
 *
 * Accepted means the provider admitted/appended one Conversation message. It does not mean remote
 * mirrors delivered it, a human acknowledged it, or a companion/model attended to it.
 */
sealed interface VexConversationRepositoryResult {
    data class Accepted(
        val messageRef:
            VexConversationMessageRef,
    ) : VexConversationRepositoryResult

    data class Rejected(
        val reason:
            VexConversationRepositoryRejection,
    ) : VexConversationRepositoryResult
}

// [VXG RealForever]
