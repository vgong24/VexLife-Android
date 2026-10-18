package com.vextreme.vexlife.android.conversation

/**
 * Stable synthetic identities used only by the Conversation Lab.
 *
 * Human-facing labels stay here rather than moving into [VexConversationParticipant]. The generic
 * Conversation model keeps stable identity/kind only; a real product identity owner can replace
 * these lab labels later without changing Conversation truth.
 */
internal data class VexConversationLabIdentity(
    val participant: VexConversationParticipant,
    val displayName: String,
)

internal object VexConversationLabFixture {
    val Victor =
        VexConversationLabIdentity(
            participant =
                VexConversationParticipant(
                    participantRef =
                        VexConversationParticipantRef(
                            "person.debug.victor",
                        ),
                    kind =
                        VexConversationParticipantKind.Human,
                ),
            displayName = "Victor",
        )

    val Maya =
        VexConversationLabIdentity(
            participant =
                VexConversationParticipant(
                    participantRef =
                        VexConversationParticipantRef(
                            "person.debug.maya",
                        ),
                    kind =
                        VexConversationParticipantKind.Human,
                ),
            displayName = "Maya",
        )

    val Alex =
        VexConversationLabIdentity(
            participant =
                VexConversationParticipant(
                    participantRef =
                        VexConversationParticipantRef(
                            "person.debug.alex",
                        ),
                    kind =
                        VexConversationParticipantKind.Human,
                ),
            displayName = "Alex",
        )

    val Vex =
        VexConversationLabIdentity(
            participant =
                VexConversationParticipant(
                    participantRef =
                        VexConversationParticipantRef(
                            "companion.debug.vex",
                        ),
                    kind =
                        VexConversationParticipantKind.Companion,
                ),
            displayName = "Vex",
        )

    val all: List<VexConversationLabIdentity> =
        listOf(
            Victor,
            Maya,
            Alex,
            Vex,
        )

    val spaceRef =
        VexConversationSpaceRef(
            "space.debug.family",
        )

    val threadRef =
        VexConversationThreadRef(
            "thread.debug.family",
        )

    fun channelRef(
        membershipGeneration: Long,
    ): VexConversationChannelRef {
        require(membershipGeneration > 0L)

        return VexConversationChannelRef(
            "channel.debug.family.g$membershipGeneration",
        )
    }

    fun identity(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationLabIdentity? =
        all.firstOrNull { identity ->
            identity.participant.participantRef ==
                participantRef
        }

    fun requireIdentity(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationLabIdentity =
        requireNotNull(
            identity(participantRef),
        ) {
            "unknown Conversation Lab participant: $participantRef"
        }
}

// [VXG RealForever]
