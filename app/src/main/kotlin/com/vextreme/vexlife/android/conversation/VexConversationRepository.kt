package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.StateFlow

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
 * - witnessRefs / current audience authority;
 * - Home/session capability evidence;
 * - model/companion availability.
 *
 * Those remain provider/server-side current truths.
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
 * expectedChannelRef + expectedMembershipGeneration make draft->send currentness explicit. A future
 * real provider revalidates those values against its authoritative current state rather than silently
 * upgrading the request.
 */
data class VexConversationSendRequest(
    val threadRef:
        VexConversationThreadRef,
    val expectedChannelRef:
        VexConversationChannelRef,
    val expectedMembershipGeneration:
        Long,
    val recipientRefs:
        List<VexConversationParticipantRef>,
    val content: String,
) {
    init {
        require(expectedMembershipGeneration > 0L) {
            "expected membership generation must be positive"
        }
        require(recipientRefs.isNotEmpty()) {
            "Conversation Send requires at least one addressed recipient"
        }
        require(
            recipientRefs
                .distinct()
                .size ==
                recipientRefs.size,
        ) {
            "Conversation Send recipient refs must be unique"
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
