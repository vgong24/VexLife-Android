package com.vextreme.vexlife.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexConversationSurfaceProjectionTest {
    private val victorRef =
        VexConversationParticipantRef(
            "person.fixture.victor",
        )
    private val mayaRef =
        VexConversationParticipantRef(
            "person.fixture.maya",
        )
    private val vexRef =
        VexConversationParticipantRef(
            "companion.fixture.vex",
        )
    private val spaceRef =
        VexConversationSpaceRef(
            "space.fixture.family-room",
        )
    private val threadRef =
        VexConversationThreadRef(
            "thread.fixture.family-room",
        )
    private val channelRef =
        VexConversationChannelRef(
            "channel.fixture.family-room.g2",
        )

    @Test
    fun unavailableCompanionDoesNotBlockHumanConversationSendEligibility() {
        val thread =
            currentThread()

        val projection =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    victorRef,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
                companionAvailabilityByParticipantRef =
                    mapOf(
                        vexRef to
                            VexConversationCompanionAvailability.Unavailable,
                    ),
            )

        assertSame(
            VexConversationSurfaceSendAvailability.Ready,
            projection.sendAvailability,
        )
        assertEquals(
            VexConversationCompanionAvailability.Unavailable,
            projection
                .participant(vexRef)
                ?.companionAvailability,
        )
        assertNull(
            projection
                .participant(mayaRef)
                ?.companionAvailability,
        )
    }

    @Test
    fun homeUnavailableBlocksSendEligibilityWithoutRewritingConversationTruth() {
        val thread =
            currentThread()

        val projection =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    victorRef,
                homeAvailability =
                    VexConversationHomeAvailability.Unavailable,
            )

        assertSame(
            VexConversationSurfaceSendAvailability.HomeUnavailable,
            projection.sendAvailability,
        )
        assertEquals(
            thread,
            projection.thread,
        )
        assertEquals(
            listOf(
                victorRef,
                mayaRef,
                vexRef,
            ),
            projection
                .participants
                .filter { participant ->
                    participant.isCurrentAudienceMember
                }
                .map { participant ->
                    participant
                        .participant
                        .participantRef
                },
        )
    }

    @Test
    fun removedHistoricalParticipantRemainsRenderableButCannotBeCurrentSpeaker() {
        val thread =
            currentThread().copy(
                currentAudienceRefs =
                    listOf(
                        victorRef,
                        vexRef,
                    ),
            )

        val projection =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    mayaRef,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            )

        assertSame(
            VexConversationSurfaceSendAvailability
                .AuthenticatedParticipantNotCurrent,
            projection.sendAvailability,
        )

        val maya =
            projection.participant(
                mayaRef,
            )
                ?: error(
                    "known historical participant missing",
                )

        assertTrue(
            maya.isAuthenticatedParticipant,
        )
        assertFalse(
            maya.isCurrentAudienceMember,
        )
    }

    @Test
    fun unknownAuthenticatedParticipantFailsClosedWithoutInventingIdentity() {
        val unknown =
            VexConversationParticipantRef(
                "person.fixture.unknown",
            )

        val projection =
            projectVexConversationSurface(
                thread =
                    currentThread(),
                authenticatedParticipantRef =
                    unknown,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            )

        assertSame(
            VexConversationSurfaceSendAvailability
                .AuthenticatedParticipantUnknown,
            projection.sendAvailability,
        )
        assertNull(
            projection.participant(
                unknown,
            ),
        )
    }

    @Test
    fun unobservedHomeAndPrincipalRemainDifferentSurfaceReasons() {
        val thread =
            currentThread()

        val homeUnobserved =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    victorRef,
                homeAvailability =
                    VexConversationHomeAvailability.Unobserved,
            )

        val principalUnobserved =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    null,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            )

        assertSame(
            VexConversationSurfaceSendAvailability.HomeUnobserved,
            homeUnobserved.sendAvailability,
        )
        assertSame(
            VexConversationSurfaceSendAvailability
                .AuthenticatedParticipantUnobserved,
            principalUnobserved.sendAvailability,
        )
    }

    private fun currentThread():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            spaceRef =
                spaceRef,
            currentChannelRef =
                channelRef,
            currentMembershipGeneration =
                2L,
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
                            mayaRef,
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
                    mayaRef,
                    vexRef,
                ),
            timeline =
                listOf(
                    VexConversationTimelineEntry.Message(
                        entryRef =
                            "message.fixture.001",
                        threadRef =
                            threadRef,
                        messageRef =
                            VexConversationMessageRef(
                                "message.fixture.001",
                            ),
                        spaceRef =
                            spaceRef,
                        channelRef =
                            channelRef,
                        membershipGeneration =
                            2L,
                        sequence =
                            1L,
                        speakerRef =
                            mayaRef,
                        recipientRefs =
                            listOf(
                                victorRef,
                            ),
                        witnessRefs =
                            listOf(
                                victorRef,
                                mayaRef,
                                vexRef,
                            ),
                        content =
                            "Historical/current group message.",
                    ),
                ),
        )
}

// [VXG RealForever]
