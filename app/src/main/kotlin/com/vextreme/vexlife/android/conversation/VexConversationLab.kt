package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Developer-only conditions that surround Conversation truth.
 *
 * Home/Companion availability are presentation/provider inputs already modeled outside
 * [VexConversationState]. Auto-mirror and selected remote speaker are laboratory behavior. None of
 * these fields become Conversation messages, Family membership, or production provider truth.
 */
internal data class VexConversationLabState(
    val simulationEnabled: Boolean = false,
    val homeAvailability:
        VexConversationHomeAvailability =
        VexConversationHomeAvailability.Available,
    val companionAvailability:
        VexConversationCompanionAvailability =
        VexConversationCompanionAvailability.Available,
    val autoMirror: Boolean = false,
    val remoteSpeakerRef:
        VexConversationParticipantRef =
        VexConversationLabFixture.Maya
            .participant
            .participantRef,
    val scenarioRef: String = "scenario.conversation.lab.unseeded",
) {
    init {
        require(scenarioRef.isNotBlank()) {
            "Conversation Lab scenarioRef must be non-blank"
        }
    }
}

/**
 * Feature-owned synthetic-provider controller for lived Conversation development.
 *
 * This class does not replace [VexFamilyConversationRepository]. Victor-authored Send still crosses
 * the repository's real [VexFamilyConversationRepository.send] contract. The Lab can only publish
 * synthetic outside-world conditions through the in-memory provider's explicit publication seam.
 *
 * Membership changes advance Family generation/channel truth and never manufacture "joined/left"
 * chat messages. Remote speech is provider-authored truth. Availability remains separate lab input.
 */
internal class VexConversationLab(
    private val repository:
        InMemoryVexFamilyConversationRepository,
) {
    private val mutableState =
        MutableStateFlow(
            VexConversationLabState(),
        )

    val state:
        StateFlow<VexConversationLabState> =
        mutableState.asStateFlow()

    fun seedFamilyRoom():
        VexConversationThreadState {
        val thread =
            familyThread(
                generation = 1L,
                currentAudienceRefs =
                    listOf(
                        VexConversationLabFixture.Victor
                            .participant
                            .participantRef,
                        VexConversationLabFixture.Maya
                            .participant
                            .participantRef,
                        VexConversationLabFixture.Vex
                            .participant
                            .participantRef,
                    ),
                segmentRefs =
                    listOf(
                        VexConversationLabFixture
                            .channelRef(1L),
                    ),
                messages = emptyList(),
            )

        repository.publishThread(thread)
        mutableState.value =
            VexConversationLabState(
                simulationEnabled = true,
                scenarioRef =
                    "scenario.conversation.lab.c02.family-room",
            )

        return thread
    }

    fun resetFamilyRoom():
        VexConversationThreadState =
        seedFamilyRoom()
            .also {
                mutableState.value =
                    mutableState.value.copy(
                        scenarioRef =
                            "scenario.conversation.lab.c16.reset-reseed",
                    )
            }

    fun setSimulationEnabled(
        enabled: Boolean,
    ) {
        updateLabState {
            copy(
                simulationEnabled = enabled,
            )
        }
    }

    fun setHomeAvailability(
        availability:
            VexConversationHomeAvailability,
    ) {
        updateLabState {
            copy(
                homeAvailability = availability,
            )
        }
    }

    fun setCompanionAvailability(
        availability:
            VexConversationCompanionAvailability,
    ) {
        updateLabState {
            copy(
                companionAvailability = availability,
            )
        }
    }

    fun setAutoMirror(
        enabled: Boolean,
    ) {
        updateLabState {
            copy(
                autoMirror = enabled,
            )
        }
    }

    /**
     * Parses the developer-only remote-speech shorthand already defined by Conversation design.
     *
     * Ordinary composer text must not be routed here. This method is only the Lab-side meaning of
     * an explicitly selected debug injection such as `[Maya]{test message}`.
     */
    fun publishRemoteInput(
        input: String,
    ): VexConversationMessage {
        val value =
            input.trim()
        val closeBracket =
            value.indexOf(']')

        require(
            value.startsWith("[") &&
                closeBracket > 1 &&
                closeBracket + 1 < value.length &&
                value[closeBracket + 1] == '{' &&
                value.endsWith("}"),
        ) {
            "Conversation Lab remote input must use [Speaker]{message} syntax"
        }

        val speakerLabel =
            value.substring(
                startIndex = 1,
                endIndex = closeBracket,
            ).trim()
        val content =
            value.substring(
                startIndex = closeBracket + 2,
                endIndex = value.lastIndex,
            )

        val identity =
            requireNotNull(
                VexConversationLabFixture
                    .all
                    .singleOrNull { candidate ->
                        candidate.displayName ==
                            speakerLabel
                    },
            ) {
                "unknown Conversation Lab speaker label: $speakerLabel"
            }

        return publishRemoteMessage(
            content = content,
            speakerRef =
                identity
                    .participant
                    .participantRef,
        )
    }

    fun selectRemoteSpeaker(
        participantRef:
            VexConversationParticipantRef,
    ) {
        val identity =
            VexConversationLabFixture
                .requireIdentity(
                    participantRef,
                )

        require(
            identity.participant.participantRef !=
                VexConversationLabFixture.Victor
                    .participant
                    .participantRef,
        ) {
            "Conversation Lab remote speaker cannot impersonate Victor"
        }

        updateLabState {
            copy(
                remoteSpeakerRef =
                    identity
                        .participant
                        .participantRef,
            )
        }
    }

    fun addParticipant(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationThreadState {
        val identity =
            VexConversationLabFixture
                .requireIdentity(
                    participantRef,
                )
        val current =
            requireFamilyRoom()

        if (
            identity.participant.participantRef in
            current.currentAudienceRefs
        ) {
            return current
        }

        val nextAudience =
            current.knownParticipants
                .map { participant ->
                    participant.participantRef
                }
                .filter { knownRef ->
                    knownRef in
                        (
                            current.currentAudienceRefs +
                                identity
                                    .participant
                                    .participantRef
                            )
                }

        return publishSuccessorGeneration(
            current = current,
            currentAudienceRefs = nextAudience,
            scenarioRef =
                "scenario.conversation.lab.c03.add-participant",
        )
    }

    fun removeParticipant(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationThreadState {
        VexConversationLabFixture
            .requireIdentity(
                participantRef,
            )

        require(
            participantRef !=
                VexConversationLabFixture.Victor
                    .participant
                    .participantRef,
        ) {
            "Conversation Lab cannot remove the authenticated Victor fixture"
        }

        val current =
            requireFamilyRoom()

        if (
            participantRef !in
            current.currentAudienceRefs
        ) {
            return current
        }

        val nextAudience =
            current.currentAudienceRefs
                .filterNot { currentRef ->
                    currentRef == participantRef
                }

        return publishSuccessorGeneration(
            current = current,
            currentAudienceRefs = nextAudience,
            scenarioRef =
                "scenario.conversation.lab.c08.remove-participant",
        )
    }

    /**
     * Advances currentness while preserving the same audience.
     *
     * A caller may intentionally retain a request formed before this transition to pressure
     * CURRENTNESS_CHANGED without the Lab fabricating the stale request itself.
     */
    fun advanceMembershipGeneration():
        VexConversationThreadState {
        val current =
            requireFamilyRoom()

        return publishSuccessorGeneration(
            current = current,
            currentAudienceRefs =
                current.currentAudienceRefs,
            scenarioRef =
                "scenario.conversation.lab.c09.generation-boundary",
        )
    }

    /**
     * Publishes one external/provider-authored message.
     *
     * This is the semantic meaning behind debug-only input such as
     * `[Maya]{test message}`; the parser/UI belongs above this controller.
     */
    fun publishRemoteMessage(
        content: String,
        speakerRef:
            VexConversationParticipantRef =
            state.value.remoteSpeakerRef,
    ): VexConversationMessage {
        check(
            state.value.simulationEnabled,
        ) {
            "Conversation Lab simulation is not enabled"
        }

        val trimmedContent =
            content.trim()

        require(trimmedContent.isNotEmpty()) {
            "Conversation Lab remote message must be non-blank"
        }

        val speaker =
            VexConversationLabFixture
                .requireIdentity(
                    speakerRef,
                )

        require(
            speaker.participant.participantRef !=
                VexConversationLabFixture.Victor
                    .participant
                    .participantRef,
        ) {
            "Conversation Lab remote speech cannot impersonate Victor"
        }

        val current =
            requireFamilyRoom()

        require(
            speaker.participant.participantRef in
                current.currentAudienceRefs,
        ) {
            "Conversation Lab remote speaker must belong to current audience"
        }

        val family =
            requireNotNull(
                current.familyBinding,
            )

        val nextSequence =
            current.messages
                .filter { message ->
                    message.channelRef ==
                        current.currentChannelRef
                }
                .maxOfOrNull { message ->
                    message.sequence
                }
                ?.plus(1L)
                ?: 0L

        val recipients =
            current.currentAudienceRefs
                .filterNot { participantRef ->
                    participantRef ==
                        speaker.participant.participantRef
                }
                .filter { participantRef ->
                    current
                        .participant(
                            participantRef,
                        )
                        ?.kind ==
                        VexConversationParticipantKind.Human
                }

        require(recipients.isNotEmpty()) {
            "Conversation Lab remote message requires at least one current human recipient"
        }

        val message =
            VexConversationMessage(
                messageRef =
                    VexConversationMessageRef(
                        buildString {
                            append("message.debug.")
                            append(
                                current
                                    .currentChannelRef
                                    .value,
                            )
                            append(".")
                            append(
                                nextSequence
                                    .toString()
                                    .padStart(
                                        length = 3,
                                        padChar = '0',
                                    ),
                            )
                            append(".")
                            append(
                                speaker
                                    .participant
                                    .participantRef
                                    .value,
                            )
                        },
                    ),
                threadRef =
                    current.threadRef,
                channelRef =
                    current.currentChannelRef,
                sequence =
                    nextSequence,
                speakerRef =
                    speaker
                        .participant
                        .participantRef,
                recipientRefs =
                    recipients,
                witnessRefs =
                    current.currentAudienceRefs,
                content =
                    trimmedContent,
                familyBinding =
                    VexFamilyConversationMessageBinding(
                        spaceRef =
                            family.spaceRef,
                        membershipGeneration =
                            family
                                .currentMembershipGeneration,
                    ),
            )

        repository.publishThread(
            current.copy(
                messages =
                    current.messages +
                        message,
            ),
        )

        updateLabState {
            copy(
                scenarioRef =
                    "scenario.conversation.lab.c04.remote-speech",
            )
        }

        return message
    }

    /**
     * Optional model-free external response after an already-accepted Victor Send.
     *
     * The caller decides when Send was accepted. This method never performs or retries Victor's
     * product Send on the caller's behalf.
     */
    fun mirrorAcceptedVictorSend(
        content: String,
    ): VexConversationMessage? {
        if (!state.value.autoMirror) {
            return null
        }

        val message =
            publishRemoteMessage(
                content =
                    "You said: \"${content.trim()}\"",
            )

        updateLabState {
            copy(
                scenarioRef =
                    "scenario.conversation.lab.c06.mirror-response",
            )
        }

        return message
    }

    private fun requireFamilyRoom():
        VexConversationThreadState =
        requireNotNull(
            repository
                .state
                .value
                .thread(
                    VexConversationLabFixture.threadRef,
                ),
        ) {
            "Conversation Lab Family room has not been seeded"
        }

    private fun publishSuccessorGeneration(
        current:
            VexConversationThreadState,
        currentAudienceRefs:
            List<VexConversationParticipantRef>,
        scenarioRef: String,
    ): VexConversationThreadState {
        val family =
            requireNotNull(
                current.familyBinding,
            )
        val nextGeneration =
            family.currentMembershipGeneration +
                1L
        val nextChannel =
            VexConversationLabFixture
                .channelRef(
                    nextGeneration,
                )

        val next =
            current.copy(
                currentChannelRef =
                    nextChannel,
                currentAudienceRefs =
                    currentAudienceRefs,
                familyBinding =
                    family.copy(
                        currentMembershipGeneration =
                            nextGeneration,
                        segmentRefs =
                            family.segmentRefs +
                                nextChannel,
                    ),
            )

        repository.publishThread(next)

        updateLabState {
            copy(
                scenarioRef = scenarioRef,
            )
        }

        return next
    }

    private fun familyThread(
        generation: Long,
        currentAudienceRefs:
            List<VexConversationParticipantRef>,
        segmentRefs:
            List<VexConversationChannelRef>,
        messages:
            List<VexConversationMessage>,
    ): VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                VexConversationLabFixture.threadRef,
            currentChannelRef =
                VexConversationLabFixture
                    .channelRef(
                        generation,
                    ),
            knownParticipants =
                VexConversationLabFixture
                    .all
                    .map { identity ->
                        identity.participant
                    },
            currentAudienceRefs =
                currentAudienceRefs,
            messages =
                messages,
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        VexConversationLabFixture.spaceRef,
                    currentMembershipGeneration =
                        generation,
                    segmentRefs =
                        segmentRefs,
                ),
        )

    private inline fun updateLabState(
        transform:
            VexConversationLabState.() ->
                VexConversationLabState,
    ) {
        mutableState.value =
            mutableState.value
                .transform()
    }
}

// [VXG RealForever]
