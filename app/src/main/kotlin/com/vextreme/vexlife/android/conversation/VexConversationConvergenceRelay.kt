package com.vextreme.vexlife.android.conversation

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexDeepCopyable
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.vexDependencies

/**
 * Distinguishes an effective Conversation input from the place its current value came from.
 *
 * This is developer/proof provenance, not product-domain truth. Synthetic provider events that have
 * already entered [VexConversationRepository] are repository truth by the time Convergence observes
 * them; only explicit Lab overrides are marked [LAB_OVERRIDE].
 */
internal enum class VexConversationInputProvenance {
    UNOBSERVED,
    OBSERVED,
    LAB_OVERRIDE,
}

/**
 * Null is meaningful here: it means the authenticated participant owner was observed and currently
 * has no participant ref. The nullable wrapper itself is reserved for "this partial Relay patch did
 * not supply a new authenticated-participant observation."
 */
internal data class VexConversationAuthenticatedParticipantObservation(
    val participantRef:
        VexConversationParticipantRef?,
) : VexDeepCopyable<VexConversationAuthenticatedParticipantObservation> {
    override fun deepCopy():
        VexConversationAuthenticatedParticipantObservation =
        copy()
}

/**
 * Current facts that may change independently around a selected Conversation.
 *
 * Nullable fields are partial-patch semantics. [VexConversationConvergenceRelay] retains the last
 * observed value for each dependency and reconciles them in one serialized [processStates] call.
 */
internal data class VexConversationConvergenceInputState(
    val conversation:
        VexConversationState? = null,
    val selectedThreadRef:
        VexConversationThreadRef? = null,
    val authenticatedParticipant:
        VexConversationAuthenticatedParticipantObservation? = null,
    val homeAvailability:
        VexConversationHomeAvailability? = null,
    val companionAvailabilityByParticipantRef:
        Map<
            VexConversationParticipantRef,
            VexConversationCompanionAvailability
        >? = null,
    val lab:
        VexConversationLabState? = null,
) : VexInputState<VexConversationConvergenceInputState> {
    override fun deepCopy():
        VexConversationConvergenceInputState =
        copy(
            conversation =
                conversation?.deepCopyForConversationRelay(),
            authenticatedParticipant =
                authenticatedParticipant?.deepCopy(),
            companionAvailabilityByParticipantRef =
                companionAvailabilityByParticipantRef
                    ?.toMap(),
            lab =
                lab?.copy(
                    companionAvailabilityOverrides =
                        lab.companionAvailabilityOverrides
                            .toMap(),
                ),
        )
}

/**
 * Current Conversation meaning after independently owned facts have converged.
 *
 * [surface] is the ordinary product projection. The provenance fields exist so Debug/Design-Lab
 * surfaces can explain which effective inputs are observed versus intentionally overridden without
 * adding mock/debug flags to Conversation domain objects.
 */
internal data class VexConversationConvergenceOutputState(
    val surface:
        VexConversationSurfaceProjection? = null,
    val homeAvailabilityProvenance:
        VexConversationInputProvenance =
        VexConversationInputProvenance.UNOBSERVED,
    val companionAvailabilityProvenanceByParticipantRef:
        Map<
            VexConversationParticipantRef,
            VexConversationInputProvenance
        > = emptyMap(),
    val labOverridesActive: Boolean = false,
) : VexOutputState<VexConversationConvergenceOutputState> {
    override fun deepCopy():
        VexConversationConvergenceOutputState =
        copy(
            surface =
                surface?.deepCopyForConversationRelay(),
            companionAvailabilityProvenanceByParticipantRef =
                companionAvailabilityProvenanceByParticipantRef
                    .toMap(),
        )
}

/**
 * One feature-owned relationship for facts that continuously change around Conversation.
 *
 * Read this class in the same four places as other Compound Relays:
 *
 * dependencies()   -> which long-lived feature truths are observed continuously?
 * public methods   -> which explicit runtime/surface facts may enter?
 * processStates()  -> how do observed facts + Lab overrides become one effective meaning?
 * outputStates     -> what may the Conversation surface depend on?
 *
 * Important ownership rule:
 *
 * repository/provider truth
 *     remains repository/provider truth
 *
 * Lab override
 *     remains an independently owned developer condition
 *
 * processStates()
 *     is the one place that decides their effective relationship
 *
 * projectVexConversationSurface(...)
 *     maps already-effective facts to UI-facing shape; it does not interpret Lab overrides.
 */
internal class VexConversationConvergenceRelay(
    private val conversationRepository:
        VexConversationRepository?,
    private val conversationLab:
        VexConversationLab?,
) : VexCompoundRelay<
        VexConversationConvergenceInputState,
        VexConversationConvergenceOutputState
    >(
        relayRef = "relay.conversation.convergence",
        initialInputState =
            VexConversationConvergenceInputState(),
        initialOutputState =
            VexConversationConvergenceOutputState(),
    ) {
    /** Pure reducer tests may drive exact facts without starting dependency observers. */
    internal constructor() : this(
        conversationRepository = null,
        conversationLab = null,
    )

    override fun dependencies() =
        vexDependencies<VexConversationConvergenceInputState> {
            conversationRepository?.let { repository ->
                observe(
                    dependencyRef =
                        CONVERSATION_DEPENDENCY,
                    flow =
                        repository.state,
                ) { conversation ->
                    VexConversationConvergenceInputState(
                        conversation =
                            conversation
                                .deepCopyForConversationRelay(),
                    )
                }
            }

            conversationLab?.let { lab ->
                observe(
                    dependencyRef =
                        LAB_DEPENDENCY,
                    flow =
                        lab.state,
                ) { labState ->
                    VexConversationConvergenceInputState(
                        lab =
                            labState.copy(
                                companionAvailabilityOverrides =
                                    labState
                                        .companionAvailabilityOverrides
                                        .toMap(),
                            ),
                    )
                }
            }
        }

    fun selectThread(
        threadRef:
            VexConversationThreadRef,
    ) {
        send(
            VexConversationConvergenceInputState(
                selectedThreadRef =
                    threadRef,
            ),
        )
    }

    /**
     * Admits the current authenticated-participant observation.
     *
     * Null is a real observation: the identity/session owner currently exposes no authenticated
     * Conversation participant.
     */
    fun authenticatedParticipantObserved(
        participantRef:
            VexConversationParticipantRef?,
    ) {
        send(
            VexConversationConvergenceInputState(
                authenticatedParticipant =
                    VexConversationAuthenticatedParticipantObservation(
                        participantRef,
                    ),
            ),
        )
    }

    fun homeAvailabilityObserved(
        availability:
            VexConversationHomeAvailability,
    ) {
        send(
            VexConversationConvergenceInputState(
                homeAvailability =
                    availability,
            ),
        )
    }

    fun companionAvailabilityObserved(
        availabilityByParticipantRef:
            Map<
                VexConversationParticipantRef,
                VexConversationCompanionAvailability
            >,
    ) {
        send(
            VexConversationConvergenceInputState(
                companionAvailabilityByParticipantRef =
                    availabilityByParticipantRef
                        .toMap(),
            ),
        )
    }

    /*
     * Direct reducer seams remain internal for focused processStates tests. Production repository
     * and Lab observation enter through dependencies() instead of UI forwarding.
     */
    internal fun conversationChanged(
        conversation:
            VexConversationState,
    ) {
        send(
            VexConversationConvergenceInputState(
                conversation =
                    conversation
                        .deepCopyForConversationRelay(),
            ),
        )
    }

    internal fun labChanged(
        lab:
            VexConversationLabState,
    ) {
        send(
            VexConversationConvergenceInputState(
                lab =
                    lab.copy(
                        companionAvailabilityOverrides =
                            lab.companionAvailabilityOverrides
                                .toMap(),
                    ),
            ),
        )
    }

    override fun processStates(
        inputState:
            VexConversationConvergenceInputState,
        currentState:
            VexCompoundState<
                VexConversationConvergenceInputState,
                VexConversationConvergenceOutputState
            >,
    ): VexCompoundState<
        VexConversationConvergenceInputState,
        VexConversationConvergenceOutputState
    > {
        val previous =
            currentState.inputState

        val conversation =
            inputState.conversation
                ?: previous.conversation
        val selectedThreadRef =
            inputState.selectedThreadRef
                ?: previous.selectedThreadRef
        val authenticatedParticipant =
            inputState.authenticatedParticipant
                ?: previous.authenticatedParticipant
        val observedHomeAvailability =
            inputState.homeAvailability
                ?: previous.homeAvailability
        val observedCompanionAvailability =
            inputState
                .companionAvailabilityByParticipantRef
                ?: previous
                    .companionAvailabilityByParticipantRef
        val lab =
            inputState.lab
                ?: previous.lab

        val homeAvailability =
            lab
                ?.homeAvailabilityOverride
                ?: observedHomeAvailability
                ?: VexConversationHomeAvailability.Unobserved

        val companionAvailability =
            observedCompanionAvailability
                .orEmpty()
                .toMutableMap()
                .apply {
                    putAll(
                        lab
                            ?.companionAvailabilityOverrides
                            .orEmpty(),
                    )
                }
                .toMap()

        val thread =
            selectedThreadRef
                ?.let { ref ->
                    conversation
                        ?.thread(ref)
                }

        val surface =
            thread?.let { currentThread ->
                projectVexConversationSurface(
                    thread =
                        currentThread,
                    authenticatedParticipantRef =
                        authenticatedParticipant
                            ?.participantRef,
                    homeAvailability =
                        homeAvailability,
                    companionAvailabilityByParticipantRef =
                        companionAvailability,
                )
            }

        val homeAvailabilityProvenance =
            when {
                lab?.homeAvailabilityOverride != null ->
                    VexConversationInputProvenance
                        .LAB_OVERRIDE

                observedHomeAvailability != null ->
                    VexConversationInputProvenance
                        .OBSERVED

                else ->
                    VexConversationInputProvenance
                        .UNOBSERVED
            }

        val companionProvenance =
            thread
                ?.knownParticipants
                .orEmpty()
                .filter { participant ->
                    participant.kind ==
                        VexConversationParticipantKind.Companion
                }
                .associate { participant ->
                    val participantRef =
                        participant.participantRef

                    participantRef to
                        when {
                            participantRef in
                                lab
                                    ?.companionAvailabilityOverrides
                                    .orEmpty() ->
                                VexConversationInputProvenance
                                    .LAB_OVERRIDE

                            participantRef in
                                observedCompanionAvailability
                                    .orEmpty() ->
                                VexConversationInputProvenance
                                    .OBSERVED

                            else ->
                                VexConversationInputProvenance
                                    .UNOBSERVED
                        }
                }

        val coherentInput =
            VexConversationConvergenceInputState(
                conversation =
                    conversation
                        ?.deepCopyForConversationRelay(),
                selectedThreadRef =
                    selectedThreadRef,
                authenticatedParticipant =
                    authenticatedParticipant
                        ?.deepCopy(),
                homeAvailability =
                    observedHomeAvailability,
                companionAvailabilityByParticipantRef =
                    observedCompanionAvailability
                        ?.toMap(),
                lab =
                    lab?.copy(
                        companionAvailabilityOverrides =
                            lab.companionAvailabilityOverrides
                                .toMap(),
                    ),
            )

        val labOverridesActive =
            lab?.homeAvailabilityOverride != null ||
                lab
                    ?.companionAvailabilityOverrides
                    ?.isNotEmpty() == true

        return currentState.copy(
            inputState =
                coherentInput,
            outputState =
                VexConversationConvergenceOutputState(
                    surface =
                        surface,
                    homeAvailabilityProvenance =
                        homeAvailabilityProvenance,
                    companionAvailabilityProvenanceByParticipantRef =
                        companionProvenance,
                    labOverridesActive =
                        labOverridesActive,
                ),
        )
    }

    private companion object {
        const val CONVERSATION_DEPENDENCY =
            "conversation.state"
        const val LAB_DEPENDENCY =
            "conversation.lab"
    }
}

private fun VexConversationState.deepCopyForConversationRelay():
    VexConversationState =
    copy(
        threads =
            threads.map(
                VexConversationThreadState::deepCopyForConversationRelay,
            ),
    )

private fun VexConversationThreadState.deepCopyForConversationRelay():
    VexConversationThreadState =
    copy(
        knownParticipants =
            knownParticipants.map { participant ->
                participant.copy()
            },
        currentAudienceRefs =
            currentAudienceRefs.toList(),
        messages =
            messages.map { message ->
                message.copy(
                    recipientRefs =
                        message.recipientRefs
                            .toList(),
                    witnessRefs =
                        message.witnessRefs
                            .toList(),
                    familyBinding =
                        message.familyBinding
                            ?.copy(),
                )
            },
        familyBinding =
            familyBinding?.copy(
                segmentRefs =
                    familyBinding.segmentRefs
                        .toList(),
            ),
    )

private fun VexConversationSurfaceProjection.deepCopyForConversationRelay():
    VexConversationSurfaceProjection =
    copy(
        thread =
            thread.deepCopyForConversationRelay(),
        participants =
            participants.map { participant ->
                participant.copy(
                    participant =
                        participant.participant
                            .copy(),
                )
            },
    )

// [VXG RealForever]
