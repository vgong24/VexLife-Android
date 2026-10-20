package com.vextreme.vexlife.android.conversation

/**
 * Human-selected Family Conversation read target.
 *
 * The caller chooses semantic Family/Conversation identity only. Current channel and membership
 * generation come from the server bootstrap so Android cannot silently replay stale routing facts.
 */
internal data class VexFamilyConversationReadRequest(
    val spaceRef:
        VexConversationSpaceRef,
    val threadRef:
        VexConversationThreadRef,
)

/**
 * Exact semantic intent accepted by the current Family thread-history producer.
 *
 * Transport/session material deliberately does not belong here. A later private-gateway adapter may
 * carry this intent, but Conversation never receives bearer tokens, headers, URLs, or Home leases.
 */
internal data class VexFamilyConversationThreadHistoryIntent(
    val spaceRef: String,
    val channelRef: String,
    val expectedMembershipGeneration:
        Long,
) {
    init {
        require(spaceRef.isNotBlank()) {
            "Family Conversation history spaceRef must be non-blank"
        }
        require(channelRef.isNotBlank()) {
            "Family Conversation history channelRef must be non-blank"
        }
        require(expectedMembershipGeneration > 0L) {
            "Family Conversation history membership generation must be positive"
        }
    }
}

/**
 * Feature-specific read port for VCS-04 Connected Services.
 *
 * This is not a generic HTTP/API client. Implementations own only carriage of the already-defined
 * Family Conversation read operations. Home/session authentication and private-gateway mechanics
 * remain outside this feature contract.
 */
internal interface VexFamilyConversationConnectedServicePort {
    suspend fun bootstrap():
        VexFamilyConversationServerBootstrap

    suspend fun readThreadHistory(
        intent:
            VexFamilyConversationThreadHistoryIntent,
    ): VexFamilyConversationServerThreadHistory
}

/**
 * Reads one exact current Family Conversation through an injected Connected Services port.
 *
 * The client owns only the feature-local sequence:
 *
 * requested Family room
 * -> current bootstrap coordinates
 * -> bounded thread-history intent
 * -> accepted server projection
 * -> existing Android Conversation state
 *
 * It owns no retained repository state, app graph, Home/session authority, retry policy, Send
 * semantics, URL/header vocabulary, or network effect.
 */
internal class VexFamilyConversationConnectedServiceClient(
    private val port:
        VexFamilyConversationConnectedServicePort,
) {
    suspend fun readThread(
        request:
            VexFamilyConversationReadRequest,
    ): VexConversationThreadState {
        val bootstrap =
            port.bootstrap()

        val matchingRooms =
            bootstrap.rooms
                .filter { room ->
                    room.spaceRef ==
                        request.spaceRef.value &&
                        room.threadRef ==
                        request.threadRef.value
                }

        check(
            matchingRooms.size == 1,
        ) {
            "Family Conversation read target must resolve exactly one current bootstrap room"
        }

        val room =
            matchingRooms.single()

        val history =
            port.readThreadHistory(
                VexFamilyConversationThreadHistoryIntent(
                    spaceRef =
                        room.spaceRef,
                    channelRef =
                        room.channelRef,
                    expectedMembershipGeneration =
                        room.membershipGeneration,
                ),
            )

        check(
            history.spaceRef ==
                room.spaceRef &&
                history.threadRef ==
                room.threadRef &&
                history.currentChannelRef ==
                room.channelRef &&
                history.membershipGeneration ==
                room.membershipGeneration,
        ) {
            "Family Conversation history must match the requested current bootstrap room"
        }

        return projectVexFamilyConversationServerThread(
            bootstrap =
                bootstrap,
            history =
                history,
        )
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][READ-CLIENT]
