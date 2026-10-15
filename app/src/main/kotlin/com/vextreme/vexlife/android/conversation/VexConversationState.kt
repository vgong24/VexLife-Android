package com.vextreme.vexlife.android.conversation

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

data class VexConversationParticipant(
    val participantRef:
        VexConversationParticipantRef,
    val displayName: String,
    val kind:
        VexConversationParticipantKind,
) {
    init {
        require(displayName.isNotBlank()) {
            "conversation participant displayName must be non-blank"
        }
    }
}

enum class VexConversationMembershipChange {
    Joined,
    Left,
}

sealed interface VexConversationTimelineEntry {
    val entryRef: String
    val threadRef:
        VexConversationThreadRef

    /**
     * Immutable Conversation message projection.
     *
     * recipientRefs are the addressed targets.
     * witnessRefs are the immutable event audience.
     * Android must not collapse those meanings.
     */
    data class Message(
        override val entryRef: String,
        override val threadRef:
            VexConversationThreadRef,
        val messageRef:
            VexConversationMessageRef,
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
    ) : VexConversationTimelineEntry {
        init {
            require(
                entryRef ==
                    messageRef.value,
            ) {
                "message entryRef must preserve exact messageRef identity"
            }
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
     * Human-readable boundary between immutable channel/membership generations in one stable thread.
     *
     * This does not mutate Family membership. It is retained Conversation/history projection only.
     */
    data class MembershipBoundary(
        override val entryRef: String,
        override val threadRef:
            VexConversationThreadRef,
        val fromChannelRef:
            VexConversationChannelRef,
        val toChannelRef:
            VexConversationChannelRef,
        val membershipGeneration: Long,
        val participantRef:
            VexConversationParticipantRef,
        val change:
            VexConversationMembershipChange,
    ) : VexConversationTimelineEntry {
        init {
            require(entryRef.isNotBlank()) {
                "membership-boundary entryRef must be non-blank"
            }
            require(membershipGeneration > 0L) {
                "membership-boundary generation must be positive"
            }
            require(
                fromChannelRef !=
                    toChannelRef,
            ) {
                "membership boundary must advance channel identity"
            }
        }
    }
}

/**
 * Conversation-owned projection of one stable thread.
 *
 * knownParticipants is the identity directory needed to render retained history. It is NOT the
 * current audience. A participant who later leaves may remain known so historical authored messages
 * and witness sets remain renderable.
 *
 * Home reachability, authenticated-device/session truth, and companion/model availability are
 * deliberately absent because those have different owners.
 */
data class VexConversationThreadState(
    val threadRef:
        VexConversationThreadRef,
    val title: String,
    val currentChannelRef:
        VexConversationChannelRef,
    val currentMembershipGeneration: Long,
    val knownParticipants:
        List<VexConversationParticipant>,
    val currentAudienceRefs:
        List<VexConversationParticipantRef>,
    val timeline:
        List<VexConversationTimelineEntry>,
) {
    private val knownParticipantRefs =
        knownParticipants
            .map { participant ->
                participant.participantRef
            }

    init {
        require(title.isNotBlank()) {
            "conversation title must be non-blank"
        }
        require(currentMembershipGeneration > 0L) {
            "current membership generation must be positive"
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
            timeline
                .map { entry ->
                    entry.entryRef
                }
                .distinct()
                .size ==
                timeline.size,
        ) {
            "conversation timeline entry refs must be unique"
        }

        timeline.forEach { entry ->
            require(
                entry.threadRef ==
                    threadRef,
            ) {
                "timeline entry must belong to its thread"
            }

            when (entry) {
                is VexConversationTimelineEntry.Message -> {
                    require(
                        entry.speakerRef in
                            knownParticipantRefs,
                    ) {
                        "historical speaker must resolve through known participants"
                    }
                    require(
                        entry.witnessRefs.all(
                            knownParticipantRefs::contains,
                        ),
                    ) {
                        "historical witnesses must resolve through known participants"
                    }
                }

                is VexConversationTimelineEntry.MembershipBoundary -> {
                    require(
                        entry.participantRef in
                            knownParticipantRefs,
                    ) {
                        "membership-boundary participant must resolve through known participants"
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
