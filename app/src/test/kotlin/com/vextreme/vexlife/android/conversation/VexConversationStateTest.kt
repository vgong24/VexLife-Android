package com.vextreme.vexlife.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexConversationStateTest {
    private val victorRef =
        VexConversationParticipantRef(
            "person.fixture.victor",
        )
    private val vexRef =
        VexConversationParticipantRef(
            "companion.fixture.vex",
        )
    private val threadRef =
        VexConversationThreadRef(
            "thread.fixture.direct-vex",
        )
    private val channelRef =
        VexConversationChannelRef(
            "channel.fixture.direct-vex",
        )

    @Test
    fun genericDirectConversationAcceptsFirstSequenceZeroWithoutFamilyGeneration() {
        val first =
            VexConversationMessage(
                messageRef =
                    VexConversationMessageRef(
                        "message.fixture.direct-vex.000",
                    ),
                threadRef =
                    threadRef,
                channelRef =
                    channelRef,
                sequence =
                    0L,
                speakerRef =
                    victorRef,
                recipientRefs =
                    listOf(
                        vexRef,
                    ),
                witnessRefs =
                    listOf(
                        victorRef,
                        vexRef,
                    ),
                content =
                    "Hello, Vex.",
            )

        val thread =
            VexConversationThreadState(
                threadRef =
                    threadRef,
                currentChannelRef =
                    channelRef,
                knownParticipants =
                    listOf(
                        VexConversationParticipant(
                            participantRef =
                                victorRef,
                            kind =
                                VexConversationParticipantKind.Human,
                        ),
                        VexConversationParticipant(
                            participantRef =
                                vexRef,
                            kind =
                                VexConversationParticipantKind.Companion,
                        ),
                    ),
                currentAudienceRefs =
                    listOf(
                        victorRef,
                        vexRef,
                    ),
                messages =
                    listOf(
                        first,
                    ),
            )

        assertEquals(
            0L,
            thread.messages
                .single()
                .sequence,
        )
        assertNull(
            thread.familyBinding,
        )
        assertNull(
            thread.messages
                .single()
                .familyBinding,
        )
    }
}

// [VXG RealForever]
