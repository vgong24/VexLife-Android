package com.vextreme.vexlife.android.conversation

/**
 * Home reachability observed for the Android Conversation surface.
 *
 * This is an input projection from the rightful Home/transport owner. It is not Conversation state
 * and does not grant authentication or authorization by itself.
 */
enum class VexConversationHomeAvailability {
    Unobserved,
    Available,
    Unavailable,
}

/**
 * Companion/runtime availability observed for one participant projected as a Companion.
 *
 * This remains independent of ordinary human Conversation availability.
 */
enum class VexConversationCompanionAvailability {
    Unknown,
    Available,
    Unavailable,
}

/**
 * Why the current Android surface may or may not offer Send as an eligible human action.
 *
 * This is presentation eligibility only. A Ready surface does not promise provider admission:
 * VexConversationRepository still revalidates thread/channel/membership currentness and addressing
 * at Send time.
 */
sealed interface VexConversationSurfaceSendAvailability {
    data object Ready :
        VexConversationSurfaceSendAvailability

    data object HomeUnobserved :
        VexConversationSurfaceSendAvailability

    data object HomeUnavailable :
        VexConversationSurfaceSendAvailability

    data object AuthenticatedParticipantUnobserved :
        VexConversationSurfaceSendAvailability

    data object AuthenticatedParticipantUnknown :
        VexConversationSurfaceSendAvailability

    data object AuthenticatedParticipantNotCurrent :
        VexConversationSurfaceSendAvailability
}

/**
 * Derived UI-facing participant projection.
 *
 * [companionAvailability] is populated only for Companion participants and stays null for humans.
 */
data class VexConversationSurfaceParticipant(
    val participant:
        VexConversationParticipant,
    val isCurrentAudienceMember:
        Boolean,
    val isAuthenticatedParticipant:
        Boolean,
    val companionAvailability:
        VexConversationCompanionAvailability?,
)

/**
 * Pure Android presentation projection over several already-owned truths.
 *
 * This type deliberately does NOT use the word Experience:
 *
 * - draft PR #81 uses Experience for a broad human/product orientation root;
 * - merged PR #82 uses <Name>Experience for a Behavior-local UI membrane.
 *
 * Conversation should not silently choose between those repository-wide meanings.
 *
 * This projection owns no state or effects. It merely combines:
 *
 * - Conversation-owned [thread];
 * - authenticated participant identity projected by the Home/session owner;
 * - Home reachability projected by the Home/transport owner;
 * - Companion availability projected by the Companion/runtime owner.
 *
 * The source owners remain visible in the constructor rather than being hidden inside
 * [VexConversationThreadState].
 */
data class VexConversationSurfaceProjection(
    val thread:
        VexConversationThreadState,
    val authenticatedParticipantRef:
        VexConversationParticipantRef?,
    val homeAvailability:
        VexConversationHomeAvailability,
    val participants:
        List<VexConversationSurfaceParticipant>,
    val sendAvailability:
        VexConversationSurfaceSendAvailability,
) {
    init {
        require(
            participants
                .map { participant ->
                    participant
                        .participant
                        .participantRef
                }
                .distinct()
                .size ==
                participants.size,
        ) {
            "surface participant refs must be unique"
        }

        require(
            participants
                .map { participant ->
                    participant
                        .participant
                        .participantRef
                } ==
                thread
                    .knownParticipants
                    .map { participant ->
                        participant.participantRef
                    },
        ) {
            "surface participants must preserve Conversation known-participant order"
        }
    }

    fun participant(
        participantRef:
            VexConversationParticipantRef,
    ): VexConversationSurfaceParticipant? =
        participants
            .firstOrNull { participant ->
                participant
                    .participant
                    .participantRef ==
                    participantRef
            }
}

/**
 * Compose several current owner projections without creating another state owner.
 *
 * Companion availability is keyed by exact participant identity. Missing entries project Unknown.
 * Human participants never receive a Companion availability value.
 */
fun projectVexConversationSurface(
    thread:
        VexConversationThreadState,
    authenticatedParticipantRef:
        VexConversationParticipantRef?,
    homeAvailability:
        VexConversationHomeAvailability,
    companionAvailabilityByParticipantRef:
        Map<
            VexConversationParticipantRef,
            VexConversationCompanionAvailability
        > =
        emptyMap(),
): VexConversationSurfaceProjection {
    val knownParticipant =
        authenticatedParticipantRef
            ?.let(
                thread::participant,
            )

    val sendAvailability =
        when {
            homeAvailability ==
                VexConversationHomeAvailability.Unobserved ->
                VexConversationSurfaceSendAvailability
                    .HomeUnobserved

            homeAvailability ==
                VexConversationHomeAvailability.Unavailable ->
                VexConversationSurfaceSendAvailability
                    .HomeUnavailable

            authenticatedParticipantRef == null ->
                VexConversationSurfaceSendAvailability
                    .AuthenticatedParticipantUnobserved

            knownParticipant == null ->
                VexConversationSurfaceSendAvailability
                    .AuthenticatedParticipantUnknown

            authenticatedParticipantRef !in
                thread.currentAudienceRefs ->
                VexConversationSurfaceSendAvailability
                    .AuthenticatedParticipantNotCurrent

            else ->
                VexConversationSurfaceSendAvailability
                    .Ready
        }

    val participants =
        thread.knownParticipants
            .map { participant ->
                VexConversationSurfaceParticipant(
                    participant =
                        participant,
                    isCurrentAudienceMember =
                        participant.participantRef in
                            thread.currentAudienceRefs,
                    isAuthenticatedParticipant =
                        participant.participantRef ==
                            authenticatedParticipantRef,
                    companionAvailability =
                        when (
                            participant.kind
                        ) {
                            VexConversationParticipantKind.Human ->
                                null

                            VexConversationParticipantKind.Companion ->
                                companionAvailabilityByParticipantRef[
                                    participant.participantRef
                                ] ?: VexConversationCompanionAvailability.Unknown
                        },
                )
            }

    return VexConversationSurfaceProjection(
        thread = thread,
        authenticatedParticipantRef =
            authenticatedParticipantRef,
        homeAvailability =
            homeAvailability,
        participants =
            participants,
        sendAvailability =
            sendAvailability,
    )
}

// [VXG RealForever]
