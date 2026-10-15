package com.vextreme.vexlife.android.conversation

@JvmInline
value class VexConversationSpaceRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexConversationSpaceRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class VexConversationThreadRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexConversationThreadRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class VexConversationChannelRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexConversationChannelRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class VexConversationMessageRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexConversationMessageRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class VexConversationParticipantRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexConversationParticipantRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

enum class VexConversationParticipantKind {
    Human,
    Companion,
}

/**
 * Stable identity needed by Conversation history/current-audience projection.
 *
 * Human-facing display name, avatar, relationship label and Family role are deliberately absent:
 * those belong to their own identity/relationship/Family/presentation owners.
 */
data class VexConversationParticipant(
    val participantRef:
        VexConversationParticipantRef,
    val kind:
        VexConversationParticipantKind,
)

/**
 * One immutable Conversation message projection.
 *
 * recipientRefs are the addressed delivery targets admitted by the provider.
 * witnessRefs are the immutable event audience.
 * Android must not collapse those meanings or reconstruct old witnesses from current membership.
 */
data class VexConversationMessage(
    val messageRef:
        VexConversationMessageRef,
    val threadRef:
        VexConversationThreadRef,
    val spaceRef:
        VexConversationSpaceRef?,
    val channelRef:
        VexConversationChannelRef,
    val membershipGeneration: Long,
    val sequence: Long,
    val speakerRef:
        VexConversationParticipantRef,
    val recipientRefs:
        List<VexConversationParticipantRef>,
    val witnessRefs:
        List<VexConversationParticipantRef>,
    val content: String,
) {
    init {
        require(membershipGeneration > 0L) {
            "message membershipGeneration must be positive"
        }
        require(sequence > 0L) {
            "message sequence must be positive"
        }
        require(content.isNotBlank()) {
            "message content must be non-blank"
        }
        require(
            recipientRefs
                .distinct()
                .size ==
                recipientRefs.size,
        ) {
            "message recipient refs must be unique"
        }
        require(
            witnessRefs
                .distinct()
                .size ==
                witnessRefs.size,
        ) {
            "message witness refs must be unique"
        }
        require(
            speakerRef in
                witnessRefs,
        ) {
            "message speaker must belong to immutable event audience"
        }
        require(
            recipientRefs.all(
                witnessRefs::contains,
            ),
        ) {
            "addressed recipients must belong to immutable event audience"
        }
    }
}

/**
 * Conversation-owned projection of one stable thread across immutable channel segments.
 *
 * Current VexLife Family source already has a canonical cross-generation reader:
 *
 * readFamilyThreadHistory(...)
 * -> threadRef
 * -> currentChannelRef
 * -> membershipGeneration
 * -> segmentRefs[]
 * -> messages[]
 *
 * [segmentRefs] therefore carries real source meaning. It is ordered from older to newer channel
 * generations and ends at [currentChannelRef].
 *
 * Membership transition events are NOT stored here because the canonical Conversation history reader
 * currently returns channel segments plus messages, not synthetic "joined/left" message events. A
 * future Android presentation may derive a visible segment boundary from exact channel/generation
 * changes or consume an explicit lifecycle projection if one is admitted.
 *
 * [spaceRef] is present for a Family/space-scoped thread and remains null for Conversation shapes
 * that do not belong to such a space.
 *
 * knownParticipants is the identity directory needed to render retained history. It is NOT the
 * current audience. A participant who later leaves may remain known so historical authored messages
 * and witness sets remain attributable.
 *
 * Human-facing thread title/label is deliberately absent. Home reachability,
 * authenticated-device/session truth, Family role, participant display metadata and companion/model
 * availability are also absent because those have different owners.
 */
data class VexConversationThreadState(
    val threadRef:
        VexConversationThreadRef,
    val spaceRef:
        VexConversationSpaceRef?,
    val currentChannelRef:
        VexConversationChannelRef,
    val currentMembershipGeneration: Long,
    val segmentRefs:
        List<VexConversationChannelRef>,
    val knownParticipants:
        List<VexConversationParticipant>,
    val currentAudienceRefs:
        List<VexConversationParticipantRef>,
    val messages:
        List<VexConversationMessage>,
) {
    private val knownParticipantRefs =
        knownParticipants
            .map { participant ->
                participant.participantRef
            }

    init {
        require(currentMembershipGeneration > 0L) {
            "current membership generation must be positive"
        }
        require(segmentRefs.isNotEmpty()) {
            "conversation thread must retain at least one channel segment"
        }
        require(
            segmentRefs
                .distinct()
                .size ==
                segmentRefs.size,
        ) {
            "conversation thread segment refs must be unique"
        }
        require(
            segmentRefs.last() ==
                currentChannelRef,
        ) {
            "current channel must be the newest retained thread segment"
        }
        require(
            knownParticipantRefs
                .distinct()
                .size ==
                knownParticipantRefs.size,
        ) {
            "known participant refs must be unique"
        }
        require(
            currentAudienceRefs
                .distinct()
                .size ==
                currentAudienceRefs.size,
        ) {
            "current audience refs must be unique"
        }
        require(
            currentAudienceRefs.all(
                knownParticipantRefs::contains,
            ),
        ) {
            "current audience must resolve through known participants"
        }
        require(
            messages
                .map { message ->
                    message.messageRef
                }
                .distinct()
                .size ==
                messages.size,
        ) {
            "conversation message refs must be unique"
        }

        messages.forEach { message ->
            require(
                message.threadRef ==
                    threadRef,
            ) {
                "message must belong to its thread"
            }
            require(
                message.spaceRef ==
                    spaceRef,
            ) {
                "message spaceRef must match its thread scope"
            }
            require(
                message.channelRef in
                    segmentRefs,
            ) {
                "message channel must belong to one retained thread segment"
            }
            require(
                message.membershipGeneration <=
                    currentMembershipGeneration,
            ) {
                "message generation cannot be newer than current thread generation"
            }
            require(
                message.speakerRef in
                    knownParticipantRefs,
            ) {
                "historical speaker must resolve through known participants"
            }
            require(
                message.witnessRefs.all(
                    knownParticipantRefs::contains,
                ),
            ) {
                "historical witnesses must resolve through known participants"
            }
        }
    }

    fun participant(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationParticipant? =
        knownParticipants
            .firstOrNull { participant ->
                participant.participantRef ==
                    participantRef
            }
}

/**
 * Android consumer state for currently known Conversation threads.
 *
 * Thread selection/presentation is intentionally not owned here.
 */
data class VexConversationState(
    val threads:
        List<VexConversationThreadState> =
        emptyList(),
) {
    init {
        require(
            threads
                .map { thread ->
                    thread.threadRef
                }
                .distinct()
                .size ==
                threads.size,
        ) {
            "conversation thread refs must be unique"
        }
    }

    fun thread(
        threadRef:
            VexConversationThreadRef,
    ): VexConversationThreadState? =
        threads
            .firstOrNull { thread ->
                thread.threadRef ==
                    threadRef
            }

    companion object {
        val Empty =
            VexConversationState()
    }
}

// [VXG RealForever]
