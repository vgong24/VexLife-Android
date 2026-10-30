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
 * already entered [VexConversationRepository] are repository truth by the time Hybrid observes
 * them; only explicit Lab overrides are marked [LAB_OVERRIDE].
 */
internal enum class VexConversationHybridInputProvenance {
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
 * Nullable fields are partial-patch semantics. [VexConversationHybridRelay] retains the last
 * observed value for each dependency and reconciles them in one serialized Compound boundary before [process].
 */
internal data class VexConversationHybridInputState(
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
) : VexInputState<VexConversationHybridInputState> {
    override fun latest(
        incoming:
            VexConversationHybridInputState,
    ):
        VexConversationHybridInputState =
        VexConversationHybridInputState(
            conversation =
                (
                    incoming.conversation
                        ?: conversation
                )?.deepCopyForConversationHybrid(),
            selectedThreadRef =
                incoming.selectedThreadRef
                    ?: selectedThreadRef,
            authenticatedParticipant =
                (
                    incoming.authenticatedParticipant
                        ?: authenticatedParticipant
                )?.deepCopy(),
            homeAvailability =
                incoming.homeAvailability
                    ?: homeAvailability,
            companionAvailabilityByParticipantRef =
                (
                    incoming.companionAvailabilityByParticipantRef
                        ?: companionAvailabilityByParticipantRef
                )?.toMap(),
            lab =
                (
                    incoming.lab
                        ?: lab
                )?.deepCopyForConversationHybrid(),
        )

    override fun deepCopy():
        VexConversationHybridInputState =
        copy(
            conversation =
                conversation?.deepCopyForConversationHybrid(),
            authenticatedParticipant =
                authenticatedParticipant?.deepCopy(),
            companionAvailabilityByParticipantRef =
                companionAvailabilityByParticipantRef
                    ?.toMap(),
            lab =
                lab?.deepCopyForConversationHybrid(),
        )
}

/**
 * Current Conversation meaning after independently owned facts have converged.
 *
 * [surface] is the ordinary product projection. The provenance fields exist so Debug/Design-Lab
 * surfaces can explain which effective inputs are observed versus intentionally overridden without
 * adding mock/debug flags to Conversation domain objects.
 */
internal data class VexConversationHybridOutputState(
    val surface:
        VexConversationSurfaceProjection? = null,
    val homeAvailabilityProvenance:
        VexConversationHybridInputProvenance =
        VexConversationHybridInputProvenance.UNOBSERVED,
    val companionAvailabilityProvenanceByParticipantRef:
        Map<
            VexConversationParticipantRef,
            VexConversationHybridInputProvenance
        > = emptyMap(),
    val labOverridesActive: Boolean = false,
) : VexOutputState<VexConversationHybridOutputState> {
    override fun deepCopy():
        VexConversationHybridOutputState =
        copy(
            surface =
                surface?.deepCopyForConversationHybrid(),
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
 * process()  -> how do observed facts + Lab overrides become one effective meaning?
 * outputStates     -> what effective Conversation meaning may downstream consumers depend on?
 *
 * Important ownership rule:
 *
 * repository/provider truth
 *     remains repository/provider truth
 *
 * Lab override
 *     remains an independently owned developer condition
 *
 * process()
 *     is the one place that decides their effective relationship
 *
 * projectVexConversationSurface(...)
 *     maps already-effective facts to UI-facing shape; it does not interpret Lab overrides.
 */
/**
 * Hybrid is a semantic specialization of Compound convergence:
 * canonical/observed facts and explicitly noncanonical Lab perturbations may coexist in one current
 * relationship without changing their original owners or losing provenance.
 *
 * Conversation currently has one earned Hybrid relationship, so the feature name is sufficient.
 * Add a narrower <Question> stem only if Conversation later earns a second independent Hybrid
 * relationship; do not predeclare categories merely for naming symmetry.
 */
internal class VexConversationHybridRelay(
    private val conversationRepository:
        VexConversationRepository?,
    private val conversationLab:
        VexConversationLab?,
) : VexCompoundRelay<
        VexConversationHybridInputState,
        VexConversationHybridOutputState
    >(
        /*
         * Stable semantic identity predates the Kotlin Name Ontology refinement. The code role is
         * now named Hybrid; changing this durable relayRef would be a separate identity/compatibility
         * decision rather than a cosmetic rename.
         */
        relayRef = "relay.conversation.convergence",
        initialInputState =
            VexConversationHybridInputState(),
        initialOutputState =
            VexConversationHybridOutputState(),
    ) {
    /** Pure reducer tests may drive exact facts without starting dependency observers. */
    internal constructor() : this(
        conversationRepository = null,
        conversationLab = null,
    )

    override fun dependencies() =
        vexDependencies<VexConversationHybridInputState> {
            conversationRepository?.let { repository ->
                observe(
                    dependencyRef =
                        CONVERSATION_DEPENDENCY,
                    flow =
                        repository.state,
                ) { conversation ->
                    VexConversationHybridInputState(
                        conversation =
                            conversation
                                .deepCopyForConversationHybrid(),
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
                    VexConversationHybridInputState(
                        lab =
                            labState
                                .deepCopyForConversationHybrid(),
                    )
                }
            }
        }

    fun selectThread(
        threadRef:
            VexConversationThreadRef,
    ) {
        send(
            VexConversationHybridInputState(
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
            VexConversationHybridInputState(
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
            VexConversationHybridInputState(
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
            VexConversationHybridInputState(
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
            VexConversationHybridInputState(
                conversation =
                    conversation
                        .deepCopyForConversationHybrid(),
            ),
        )
    }

    internal fun labChanged(
        lab:
            VexConversationLabState,
    ) {
        send(
            VexConversationHybridInputState(
                lab =
                    lab.deepCopyForConversationHybrid(),
            ),
        )
    }

    override fun process(
        latestStateInput:
            VexConversationHybridInputState,
        currentState:
            VexCompoundState<
                VexConversationHybridInputState,
                VexConversationHybridOutputState
            >,
    ):
        VexConversationHybridOutputState {
        val conversation =
            latestStateInput.conversation
        val selectedThreadRef =
            latestStateInput.selectedThreadRef
        val authenticatedParticipant =
            latestStateInput.authenticatedParticipant
        val observedHomeAvailability =
            latestStateInput.homeAvailability
        val observedCompanionAvailability =
            latestStateInput
                .companionAvailabilityByParticipantRef
        val lab =
            latestStateInput.lab

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
                    VexConversationHybridInputProvenance
                        .LAB_OVERRIDE

                observedHomeAvailability != null &&
                    observedHomeAvailability !=
                    VexConversationHomeAvailability.Unobserved ->
                    VexConversationHybridInputProvenance
                        .OBSERVED

                else ->
                    VexConversationHybridInputProvenance
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
                                VexConversationHybridInputProvenance
                                    .LAB_OVERRIDE

                            participantRef in
                                observedCompanionAvailability
                                    .orEmpty() ->
                                VexConversationHybridInputProvenance
                                    .OBSERVED

                            else ->
                                VexConversationHybridInputProvenance
                                    .UNOBSERVED
                        }
                }

        val labOverridesActive =
            lab?.homeAvailabilityOverride != null ||
                lab
                    ?.companionAvailabilityOverrides
                    ?.isNotEmpty() == true

        return VexConversationHybridOutputState(
            surface =
                surface,
            homeAvailabilityProvenance =
                homeAvailabilityProvenance,
            companionAvailabilityProvenanceByParticipantRef =
                companionProvenance,
            labOverridesActive =
                labOverridesActive,
        )
    }

    private companion object {
        const val CONVERSATION_DEPENDENCY =
            "conversation.state"
        const val LAB_DEPENDENCY =
            "conversation.lab"
    }
}

private fun VexConversationLabState.deepCopyForConversationHybrid():
    VexConversationLabState =
    copy(
        companionAvailabilityOverrides =
            companionAvailabilityOverrides
                .toMap(),
    )

private fun VexConversationState.deepCopyForConversationHybrid():
    VexConversationState =
    copy(
        threads =
            threads.map(
                VexConversationThreadState::deepCopyForConversationHybrid,
            ),
    )

private fun VexConversationThreadState.deepCopyForConversationHybrid():
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
            familyBinding?.let { binding ->
                binding.copy(
                    segmentRefs =
                        binding.segmentRefs
                            .toList(),
                )
            },
    )

private fun VexConversationSurfaceProjection.deepCopyForConversationHybrid():
    VexConversationSurfaceProjection =
    copy(
        thread =
            thread.deepCopyForConversationHybrid(),
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
