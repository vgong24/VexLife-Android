package com.vextreme.vexlife.android.conversation

enum class VexConversationHomeAvailability {
    Unobserved,
    Available,
    Unavailable,
}

enum class VexConversationCompanionAvailability {
    Unknown,
    Available,
    Unavailable,
}

/**
 * Derived UI-facing participant projection.
 *
 * Companion availability is presentation input from the Companion/runtime owner; it is not stored in
 * generic Conversation state.
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
 * Pure generic Android presentation projection.
 *
 * It deliberately does not claim that the current thread has a writable provider. Write eligibility
 * is provider/domain-specific and layered separately.
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
 * Family-only Send eligibility for the Android surface.
 *
 * Ready means the Family UI may truthfully offer an attempt. It does not mean VF-02C has admitted the
 * request; provider currentness/idempotency revalidation still occurs in VexFamilyConversationRepository.
 */
sealed interface VexFamilyConversationSurfaceSendAvailability {
    data object Ready :
        VexFamilyConversationSurfaceSendAvailability

    data object NotFamilyConversation :
        VexFamilyConversationSurfaceSendAvailability

    data object HomeUnobserved :
        VexFamilyConversationSurfaceSendAvailability

    data object HomeUnavailable :
        VexFamilyConversationSurfaceSendAvailability

    data object AuthenticatedParticipantUnobserved :
        VexFamilyConversationSurfaceSendAvailability

    data object AuthenticatedParticipantUnknown :
        VexFamilyConversationSurfaceSendAvailability

    data object AuthenticatedParticipantNotCurrent :
        VexFamilyConversationSurfaceSendAvailability
}

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
    )
}

/**
 * Derive only the Family write-affordance portion of the current surface.
 *
 * Generic/direct Conversation deliberately receives NotFamilyConversation here rather than being
 * forced through Family membership semantics.
 */
fun projectVexFamilyConversationSendAvailability(
    surface:
        VexConversationSurfaceProjection,
): VexFamilyConversationSurfaceSendAvailability {
    val thread =
        surface.thread

    if (thread.familyBinding == null) {
        return VexFamilyConversationSurfaceSendAvailability
            .NotFamilyConversation
    }

    if (
        surface.homeAvailability ==
        VexConversationHomeAvailability.Unobserved
    ) {
        return VexFamilyConversationSurfaceSendAvailability
            .HomeUnobserved
    }

    if (
        surface.homeAvailability ==
        VexConversationHomeAvailability.Unavailable
    ) {
        return VexFamilyConversationSurfaceSendAvailability
            .HomeUnavailable
    }

    val participantRef =
        surface.authenticatedParticipantRef
            ?: return VexFamilyConversationSurfaceSendAvailability
                .AuthenticatedParticipantUnobserved

    if (
        thread.participant(
            participantRef,
        ) == null
    ) {
        return VexFamilyConversationSurfaceSendAvailability
            .AuthenticatedParticipantUnknown
    }

    if (
        participantRef !in
        thread.currentAudienceRefs
    ) {
        return VexFamilyConversationSurfaceSendAvailability
            .AuthenticatedParticipantNotCurrent
    }

    return VexFamilyConversationSurfaceSendAvailability.Ready
}

// [VXG RealForever]
