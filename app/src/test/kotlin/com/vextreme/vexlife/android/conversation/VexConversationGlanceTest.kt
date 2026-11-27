package com.vextreme.vexlife.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexConversationGlanceTest {
    private val victor = VexConversationParticipantRef("Victor")
    private val maya = VexConversationParticipantRef("Maya")
    private val vex = VexConversationParticipantRef("Vex")
    private val threadRef = VexConversationThreadRef("thread.fixture.family")
    private val channelRef = VexConversationChannelRef("channel.fixture.family")

    @Test
    fun noSurfaceIsUnobservedNotEmpty() {
        assertSame(
            VexConversationGlance.Unobserved,
            VexConversationHybridOutputState().toVexConversationGlance(),
        )
    }

    @Test
    fun latestMessageIsChosenBySequenceAndAttributedToItsSpeaker() {
        val glance =
            output(
                authenticated = victor,
                messages =
                    listOf(
                        message(1L, maya, "Yep, leaving now."),
                        message(0L, victor, "Are we still going?"),
                    ),
            ).toVexConversationGlance()

        assertEquals(VexConversationHomeAvailability.Available, glance.homeAvailability)
        assertEquals("Maya", glance.latestSpeakerLabel)
        assertEquals("Yep, leaving now.", glance.latestContent)
        assertFalse(glance.latestFromAuthenticatedParticipant)
        assertEquals(listOf("Victor", "Maya", "Vex"), glance.participantLabels)
    }

    @Test
    fun latestFromTheAuthenticatedParticipantIsMarked() {
        val glance =
            output(
                authenticated = victor,
                messages = listOf(message(0L, victor, "On my way.")),
            ).toVexConversationGlance()

        assertTrue(glance.latestFromAuthenticatedParticipant)
    }

    @Test
    fun emptyRoomHasNoLatestLine() {
        val glance =
            output(
                authenticated = victor,
                messages = emptyList(),
            ).toVexConversationGlance()

        assertNull(glance.latestContent)
        assertNull(glance.latestSpeakerLabel)
    }

    private fun output(
        authenticated: VexConversationParticipantRef?,
        messages: List<VexConversationMessage>,
    ): VexConversationHybridOutputState =
        VexConversationHybridOutputState(
            surface =
                projectVexConversationSurface(
                    thread =
                        VexConversationThreadState(
                            threadRef = threadRef,
                            currentChannelRef = channelRef,
                            knownParticipants =
                                listOf(
                                    VexConversationParticipant(victor, VexConversationParticipantKind.Human),
                                    VexConversationParticipant(maya, VexConversationParticipantKind.Human),
                                    VexConversationParticipant(vex, VexConversationParticipantKind.Companion),
                                ),
                            currentAudienceRefs = listOf(victor, maya, vex),
                            messages = messages.sortedBy { it.sequence },
                        ),
                    authenticatedParticipantRef = authenticated,
                    homeAvailability = VexConversationHomeAvailability.Available,
                ),
        )

    private fun message(
        sequence: Long,
        speaker: VexConversationParticipantRef,
        content: String,
    ): VexConversationMessage =
        VexConversationMessage(
            messageRef = VexConversationMessageRef("message.fixture.$sequence"),
            threadRef = threadRef,
            channelRef = channelRef,
            sequence = sequence,
            speakerRef = speaker,
            recipientRefs = listOf(victor, maya, vex).filterNot { it == speaker },
            witnessRefs = listOf(victor, maya, vex),
            content = content,
        )
}

// [VXG RealForever][CONVERSATION][GLANCE][PROOF]
