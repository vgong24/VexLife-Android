package com.vextreme.vexlife.android.conversation

/**
 * Minimal current-room projection from the accepted VexLife Family bootstrap boundary.
 *
 * These are producer facts, not another Android state owner. Transport/authentication stays outside
 * this adapter.
 */
internal data class VexFamilyConversationServerBootstrap(
    val currentPrincipalRef: String,
    val rooms:
        List<VexFamilyConversationServerRoom>,
)

internal data class VexFamilyConversationServerRoom(
    val spaceRef: String,
    val threadRef: String,
    val channelRef: String,
    val membershipGeneration: Long,
    val audiencePrincipalRefs:
        List<String>,
    val familyCompanionLineageRef:
        String,
    val familyCompanionIncluded:
        Boolean,
)

/**
 * Minimal cross-generation history projection from accepted VexLife READ_THREAD_HISTORY.
 *
 * Wire-only fields that Conversation does not own (for example content hashes or server timestamps)
 * stay outside the domain state rather than being promoted into new Android meaning.
 */
internal data class VexFamilyConversationServerThreadHistory(
    val spaceRef: String,
    val threadRef: String,
    val currentChannelRef: String,
    val membershipGeneration: Long,
    val segmentRefs:
        List<String>,
    val messages:
        List<VexFamilyConversationServerMessage>,
    val truncated: Boolean,
)

internal data class VexFamilyConversationServerMessage(
    val messageRef: String,
    val spaceRef: String,
    val threadRef: String,
    val channelRef: String,
    val speakerRef: String,
    val recipientRefs:
        List<String>,
    val witnessRefs:
        List<String>,
    val membershipGeneration: Long,
    val sequence: Long,
    val content: String,
)

/**
 * Projects one accepted Family server room/history pair into the existing Android Conversation
 * domain.
 *
 * VexLife remains the Family membership/history owner. This function only verifies that two server
 * projections describe the same current room, then maps their already-admitted truth into Android's
 * generic Conversation + explicit Family bindings.
 *
 * Contradictory projections fail closed. This function does not perform HTTP, authentication,
 * pairing, session resolution, retries, persistence, or app-graph registration.
 */
internal fun projectVexFamilyConversationServerThread(
    bootstrap:
        VexFamilyConversationServerBootstrap,
    history:
        VexFamilyConversationServerThreadHistory,
): VexConversationThreadState {
    require(
        bootstrap.currentPrincipalRef.isNotBlank(),
    ) {
        "Family bootstrap current principal must be non-blank"
    }
    require(
        history.segmentRefs.isNotEmpty(),
    ) {
        "Family thread history must retain at least one segment"
    }
    require(
        history.segmentRefs.distinct().size ==
            history.segmentRefs.size,
    ) {
        "Family thread history segment refs must be unique"
    }
    require(
        history.segmentRefs.last() ==
            history.currentChannelRef,
    ) {
        "Family thread history current channel must be the newest segment"
    }
    require(
        history.membershipGeneration > 0L,
    ) {
        "Family thread history membership generation must be positive"
    }

    val room =
        bootstrap.rooms
            .singleOrNull { candidate ->
                candidate.spaceRef ==
                    history.spaceRef &&
                    candidate.threadRef ==
                    history.threadRef &&
                    candidate.channelRef ==
                    history.currentChannelRef &&
                    candidate.membershipGeneration ==
                    history.membershipGeneration
            }
            ?: error(
                "Family bootstrap and thread history must resolve exactly one matching current room",
            )

    require(
        room.audiencePrincipalRefs.isNotEmpty(),
    ) {
        "Family current human audience must not be empty"
    }
    require(
        room.audiencePrincipalRefs
            .all { principalRef ->
                principalRef.isNotBlank()
            },
    ) {
        "Family current human principal refs must be non-blank"
    }
    require(
        room.audiencePrincipalRefs.distinct().size ==
            room.audiencePrincipalRefs.size,
    ) {
        "Family current human principal refs must be unique"
    }
    require(
        bootstrap.currentPrincipalRef in
            room.audiencePrincipalRefs,
    ) {
        "Family bootstrap current principal must belong to the current room audience"
    }
    require(
        room.familyCompanionIncluded,
    ) {
        "Family thread history requires current room Companion inclusion"
    }
    require(
        room.familyCompanionLineageRef.isNotBlank(),
    ) {
        "Family current room must expose its accepted Companion lineage for participant identity"
    }
    require(
        room.familyCompanionLineageRef !in
            room.audiencePrincipalRefs,
    ) {
        "Family Companion lineage must not collide with a human principal ref"
    }

    val segmentIndex =
        history.segmentRefs
            .withIndex()
            .associate { indexed ->
                indexed.value to
                    indexed.index
            }
    val generationBySegment =
        mutableMapOf<String, Long>()
    var previousSegmentIndex = -1
    var previousSegmentGeneration:
        Long? = null
    val lastSequenceBySegment =
        mutableMapOf<String, Long>()

    history.messages.forEach { message ->
        require(
            message.messageRef.isNotBlank(),
        ) {
            "Family history messageRef must be non-blank"
        }
        require(
            room.familyCompanionLineageRef in
                message.witnessRefs,
        ) {
            "Family history witness snapshot must retain the accepted Companion lineage"
        }
        require(
            message.spaceRef ==
                history.spaceRef,
        ) {
            "Family history message spaceRef must match thread history"
        }
        require(
            message.threadRef ==
                history.threadRef,
        ) {
            "Family history message threadRef must match thread history"
        }
        val currentSegmentIndex =
            requireNotNull(
                segmentIndex[
                    message.channelRef
                ],
            ) {
                "Family history message channel must belong to a retained segment"
            }
        require(
            currentSegmentIndex >=
                previousSegmentIndex,
        ) {
            "Family history messages must preserve canonical segment order"
        }

        val priorGeneration =
            generationBySegment[
                message.channelRef
            ]
        if (priorGeneration == null) {
            generationBySegment[
                message.channelRef
            ] = message.membershipGeneration
        }
        require(
            priorGeneration == null ||
                priorGeneration ==
                message.membershipGeneration,
        ) {
            "Family history segment cannot carry more than one membership generation"
        }
        require(
            message.membershipGeneration > 0L &&
                message.membershipGeneration <=
                history.membershipGeneration,
        ) {
            "Family history message generation must be positive and no newer than current"
        }

        if (
            currentSegmentIndex >
            previousSegmentIndex
        ) {
            previousSegmentGeneration
                ?.let { prior ->
                    require(
                        message.membershipGeneration >
                            prior,
                    ) {
                        "Family history segment generations must advance in canonical order"
                    }
                }
            previousSegmentGeneration =
                message.membershipGeneration
            previousSegmentIndex =
                currentSegmentIndex
        }

        val previousSequence =
            lastSequenceBySegment[
                message.channelRef
            ]
        if (previousSequence != null) {
            require(
                message.sequence >
                    previousSequence,
            ) {
                "Family history message sequence must advance within one channel segment"
            }
        }
        lastSequenceBySegment[
            message.channelRef
        ] = message.sequence
    }

    val currentHumanRefs =
        room.audiencePrincipalRefs
            .map(
                ::VexConversationParticipantRef,
            )
    val companionRef =
        VexConversationParticipantRef(
            room.familyCompanionLineageRef,
        )
    val currentAudienceRefs =
        currentHumanRefs +
            companionRef

    val participantRefs =
        linkedSetOf<String>()
            .apply {
                addAll(
                    room.audiencePrincipalRefs,
                )
                add(
                    room.familyCompanionLineageRef,
                )
                history.messages.forEach { message ->
                    add(message.speakerRef)
                    addAll(
                        message.recipientRefs,
                    )
                    addAll(
                        message.witnessRefs,
                    )
                }
            }

    require(
        participantRefs.all { participantRef ->
            participantRef.isNotBlank()
        },
    ) {
        "Family history participant refs must be non-blank"
    }

    val messages =
        history.messages
            .map { message ->
                VexConversationMessage(
                    messageRef =
                        VexConversationMessageRef(
                            message.messageRef,
                        ),
                    threadRef =
                        VexConversationThreadRef(
                            message.threadRef,
                        ),
                    channelRef =
                        VexConversationChannelRef(
                            message.channelRef,
                        ),
                    sequence =
                        message.sequence,
                    speakerRef =
                        VexConversationParticipantRef(
                            message.speakerRef,
                        ),
                    recipientRefs =
                        message.recipientRefs
                            .map(
                                ::VexConversationParticipantRef,
                            ),
                    witnessRefs =
                        message.witnessRefs
                            .map(
                                ::VexConversationParticipantRef,
                            ),
                    content =
                        message.content,
                    familyBinding =
                        VexFamilyConversationMessageBinding(
                            spaceRef =
                                VexConversationSpaceRef(
                                    message.spaceRef,
                                ),
                            membershipGeneration =
                                message.membershipGeneration,
                        ),
                )
            }

    val knownParticipants =
        participantRefs
            .map { participantRef ->
                VexConversationParticipant(
                    participantRef =
                        VexConversationParticipantRef(
                            participantRef,
                        ),
                    kind =
                        if (
                            participantRef ==
                            room.familyCompanionLineageRef
                        ) {
                            VexConversationParticipantKind.Companion
                        } else {
                            VexConversationParticipantKind.Human
                        },
                )
            }

    return VexConversationThreadState(
        threadRef =
            VexConversationThreadRef(
                history.threadRef,
            ),
        currentChannelRef =
            VexConversationChannelRef(
                history.currentChannelRef,
            ),
        knownParticipants =
            knownParticipants,
        currentAudienceRefs =
            currentAudienceRefs,
        messages =
            messages,
        familyBinding =
            VexFamilyConversationThreadBinding(
                spaceRef =
                    VexConversationSpaceRef(
                        history.spaceRef,
                    ),
                currentMembershipGeneration =
                    history.membershipGeneration,
                segmentRefs =
                    history.segmentRefs
                        .map(
                            ::VexConversationChannelRef,
                        ),
                historyTruncated =
                    history.truncated,
            ),
    )
}

// [VXG RealForever]
