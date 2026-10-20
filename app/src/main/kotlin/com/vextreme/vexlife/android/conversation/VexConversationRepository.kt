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
 * Generic Android read boundary for portable Conversation state.
 *
 * This does not imply that every Conversation provider shares one write/currentness contract.
 */
interface VexConversationRepository {
    val state:
        StateFlow<VexConversationState>
}

/**
 * Family-specific writable Conversation boundary aligned to VexLife VF-02C.
 *
 * Family membership generation and provider-derived Family recipient semantics live here rather than
 * being imposed on all generic Conversation shapes.
 */
interface VexFamilyConversationRepository :
    VexConversationRepository {
    suspend fun send(
        request:
            VexFamilyConversationSendRequest,
    ): VexFamilyConversationRepositoryResult
}

/**
 * Family caller intent aligned to VF-02C.
 *
 * The caller supplies routing/content/currentness/idempotency only. It does not author trusted
 * speaker, Family recipients, witness audience, membership/role, Home/session authority or model
 * truth.
 */
data class VexFamilyConversationSendRequest(
    val threadRef:
        VexConversationThreadRef,
    val spaceRef:
        VexConversationSpaceRef,
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
            "expected canonical Family membership generation must be positive"
        }
        require(content.isNotBlank()) {
            "Family Conversation Send content must be non-blank"
        }
    }
}

enum class VexFamilyConversationRepositoryRejection {
    THREAD_UNKNOWN,
    THREAD_NOT_FAMILY_BOUND,
    CURRENTNESS_CHANGED,
    SPEAKER_NOT_CURRENT,
    ADDRESSING_INVALID,
    IDEMPOTENCY_CONFLICT,
}

sealed interface VexFamilyConversationRepositoryResult {
    data class Accepted(
        val messageRef:
            VexConversationMessageRef,
    ) : VexFamilyConversationRepositoryResult

    data class Rejected(
        val reason:
            VexFamilyConversationRepositoryRejection,
    ) : VexFamilyConversationRepositoryResult
}

// [VXG RealForever]
