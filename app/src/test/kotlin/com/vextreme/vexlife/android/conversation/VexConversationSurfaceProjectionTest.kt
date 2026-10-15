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
    fun unavailableCompanionDoesNotBlockFamilyHumanSendEligibility() {
        val projection =
            projectVexConversationSurface(
                thread =
                    familyThread(),
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
            VexFamilyConversationSurfaceSendAvailability.Ready,
            projectVexFamilyConversationSendAvailability(
                projection,
            ),
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
    fun genericDirectConversationDoesNotSilentlyGainFamilySendSemantics() {
        val direct =
            VexConversationThreadState(
                threadRef =
                    VexConversationThreadRef(
                        "thread.fixture.direct",
                    ),
                currentChannelRef =
                    VexConversationChannelRef(
                        "channel.fixture.direct",
                    ),
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
                    emptyList(),
            )

        val projection =
            projectVexConversationSurface(
                thread =
                    direct,
                authenticatedParticipantRef =
                    victorRef,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            )

        assertSame(
            VexFamilyConversationSurfaceSendAvailability.NotFamilyConversation,
            projectVexFamilyConversationSendAvailability(
                projection,
            ),
        )
    }

    @Test
    fun homeUnavailableBlocksFamilySendEligibilityWithoutRewritingConversationTruth() {
        val thread =
            familyThread()

        val projection =
            projectVexConversationSurface(
                thread = thread,
                authenticatedParticipantRef =
                    victorRef,
                homeAvailability =
                    VexConversationHomeAvailability.Unavailable,
            )

        assertSame(
            VexFamilyConversationSurfaceSendAvailability.HomeUnavailable,
            projectVexFamilyConversationSendAvailability(
                projection,
            ),
        )
        assertEquals(
            thread,
            projection.thread,
        )
    }

    @Test
    fun removedHistoricalParticipantRemainsRenderableButCannotBeCurrentFamilySpeaker() {
        val thread =
            familyThread().copy(
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
            VexFamilyConversationSurfaceSendAvailability
                .AuthenticatedParticipantNotCurrent,
            projectVexFamilyConversationSendAvailability(
                projection,
            ),
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
                    familyThread(),
                authenticatedParticipantRef =
                    unknown,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            )

        assertSame(
            VexFamilyConversationSurfaceSendAvailability
                .AuthenticatedParticipantUnknown,
            projectVexFamilyConversationSendAvailability(
                projection,
            ),
        )
        assertNull(
            projection.participant(
                unknown,
            ),
        )
    }

    @Test
    fun unobservedHomeAndPrincipalRemainDifferentFamilySendReasons() {
        val thread =
            familyThread()

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
            VexFamilyConversationSurfaceSendAvailability.HomeUnobserved,
            projectVexFamilyConversationSendAvailability(
                homeUnobserved,
            ),
        )
        assertSame(
            VexFamilyConversationSurfaceSendAvailability
                .AuthenticatedParticipantUnobserved,
            projectVexFamilyConversationSendAvailability(
                principalUnobserved,
            ),
        )
    }

    private fun familyThread():
        VexConversationThreadState =
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
            messages =
                listOf(
                    VexConversationMessage(
                        messageRef =
                            VexConversationMessageRef(
                                "message.fixture.000",
                            ),
                        threadRef =
                            threadRef,
                        channelRef =
                            channelRef,
                        sequence =
                            0L,
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
                        familyBinding =
                            VexFamilyConversationMessageBinding(
                                spaceRef =
                                    spaceRef,
                                membershipGeneration =
                                    2L,
                            ),
                    ),
                ),
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        spaceRef,
                    currentMembershipGeneration =
                        2L,
                    segmentRefs =
                        listOf(
                            channelRef,
                        ),
                ),
        )
}

// [VXG RealForever]
