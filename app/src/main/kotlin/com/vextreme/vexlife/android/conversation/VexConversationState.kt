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
 * Stable identity needed by generic Conversation history/current-audience projection.
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
 * Family-bound metadata on one otherwise-generic Conversation message.
 *
 * Portable service.conversation and Conversation Store do not require Family membership generation
 * for every message. The Family adapter adds this exact meaning.
 */
data class VexFamilyConversationMessageBinding(
    val spaceRef:
        VexConversationSpaceRef,
    val membershipGeneration: Long,
) {
    init {
        require(membershipGeneration >= 0L) {
            "Family message membershipGeneration must be non-negative"
        }
    }
}

/**
 * One immutable generic Conversation message projection.
 *
 * sequence is zero-based because Conversation Store assigns sequence 0 to the first canonical
 * message in a channel.
 *
 * [familyBinding] remains null for non-Family Conversation.
 */
data class VexConversationMessage(
    val messageRef:
        VexConversationMessageRef,
    val threadRef:
        VexConversationThreadRef,
    val channelRef:
        VexConversationChannelRef,
    val sequence: Long,
    val speakerRef:
        VexConversationParticipantRef,
    val recipientRefs:
        List<VexConversationParticipantRef>,
    val witnessRefs:
        List<VexConversationParticipantRef>,
    val content: String,
    val familyBinding:
        VexFamilyConversationMessageBinding? =
        null,
) {
    init {
        require(sequence >= 0L) {
            "message sequence must be non-negative"
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
 * Family-bound currentness/history metadata on an otherwise-generic Conversation thread.
 *
 * Current VexLife family-membership-runtime owns this segment/generation meaning through
 * readFamilyThreadHistory(...).
 */
data class VexFamilyConversationThreadBinding(
    val spaceRef:
        VexConversationSpaceRef,
    val currentMembershipGeneration:
        Long,
    val segmentRefs:
        List<VexConversationChannelRef>,
) {
    init {
        require(currentMembershipGeneration >= 0L) {
            "Family current membership generation must be non-negative"
        }
        require(segmentRefs.isNotEmpty()) {
            "Family Conversation thread must retain at least one channel segment"
        }
        require(
            segmentRefs
                .distinct()
                .size ==
                segmentRefs.size,
        ) {
            "Family Conversation segment refs must be unique"
        }
    }
}

/**
 * Generic Conversation-owned projection of one current thread.
 *
 * [familyBinding] is present only for a Family-bound Conversation. Generic/direct Conversation does
 * not invent Family membership generation or Family channel-segment semantics.
 *
 * knownParticipants is the identity directory needed to attribute retained messages. It is NOT the
 * same thing as current audience.
 */
data class VexConversationThreadState(
    val threadRef:
        VexConversationThreadRef,
    val currentChannelRef:
        VexConversationChannelRef,
    val knownParticipants:
        List<VexConversationParticipant>,
    val currentAudienceRefs:
        List<VexConversationParticipantRef>,
    val messages:
        List<VexConversationMessage>,
    val familyBinding:
        VexFamilyConversationThreadBinding? =
        null,
) {
    private val knownParticipantRefs =
        knownParticipants
            .map { participant ->
                participant.participantRef
            }

    init {
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

        familyBinding?.let { binding ->
            require(
                binding.segmentRefs.last() ==
                    currentChannelRef,
            ) {
                "Family current channel must be the newest retained thread segment"
            }
        }

        messages.forEach { message ->
            require(
                message.threadRef ==
                    threadRef,
            ) {
                "message must belong to its thread"
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

            when (val messageFamily = message.familyBinding) {
                null -> {
                    require(familyBinding == null) {
                        "Family-bound thread messages require Family message binding"
                    }
                    require(
                        message.channelRef ==
                            currentChannelRef,
                    ) {
                        "generic non-Family thread currently projects only its current channel"
                    }
                }

                else -> {
                    val threadFamily =
                        requireNotNull(
                            familyBinding,
                        ) {
                            "Family message cannot appear in a non-Family thread"
                        }
                    require(
                        messageFamily.spaceRef ==
                            threadFamily.spaceRef,
                    ) {
                        "Family message spaceRef must match thread Family binding"
                    }
                    require(
                        messageFamily.membershipGeneration <=
                            threadFamily.currentMembershipGeneration,
                    ) {
                        "Family message generation cannot be newer than current Family generation"
                    }
                    require(
                        message.channelRef in
                            threadFamily.segmentRefs,
                    ) {
                        "Family message channel must belong to one retained Family segment"
                    }
                }
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
