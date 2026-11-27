package com.vextreme.vexlife.android.conversation

/**
 * What a compact surface outside the Conversation room may show about the current Family room.
 *
 * Human question: "Is the Family room reachable, and what was said last?" — asked from Home or
 * (later) Vex context. It reads the session Hybrid output; Conversation truth, currentness and Send
 * stay with their existing owners. Unread / attention is deliberately absent: no read-position owner
 * exists yet, so a glance must not imply one.
 *
 * Speaker labels use the participant ref, exactly like the installed room renderer today.
 */
internal data class VexConversationGlance(
    val homeAvailability:
        VexConversationHomeAvailability,
    val latestSpeakerLabel:
        String?,
    val latestFromAuthenticatedParticipant:
        Boolean,
    val latestContent:
        String?,
    val participantLabels:
        List<String>,
) {
    companion object {
        val Unobserved =
            VexConversationGlance(
                homeAvailability =
                    VexConversationHomeAvailability.Unobserved,
                latestSpeakerLabel =
                    null,
                latestFromAuthenticatedParticipant =
                    false,
                latestContent =
                    null,
                participantLabels =
                    emptyList(),
            )
    }
}

/** Pure projection of the current Hybrid output. */
internal fun VexConversationHybridOutputState.toVexConversationGlance(): VexConversationGlance {
    val surface =
        surface
            ?: return VexConversationGlance.Unobserved
    val latest =
        surface.thread.messages
            .maxByOrNull {
                it.sequence
            }

    return VexConversationGlance(
        homeAvailability =
            surface.homeAvailability,
        latestSpeakerLabel =
            latest
                ?.speakerRef
                ?.value,
        latestFromAuthenticatedParticipant =
            latest != null &&
                latest.speakerRef ==
                surface.authenticatedParticipantRef,
        latestContent =
            latest
                ?.content,
        participantLabels =
            surface.participants.map {
                it.participant.participantRef.value
            },
    )
}

// [VXG RealForever][CONVERSATION][GLANCE]
