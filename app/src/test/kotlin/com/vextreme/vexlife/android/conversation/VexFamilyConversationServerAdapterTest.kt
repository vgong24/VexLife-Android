package com.vextreme.vexlife.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFamilyConversationServerAdapterTest {
    @Test
    fun crossGenerationHistoryProjectsCurrentAudienceAndHistoricalParticipants() {
        val projected =
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = history(),
            )

        assertEquals(
            VexConversationThreadRef(
                THREAD,
            ),
            projected.threadRef,
        )
        assertEquals(
            VexConversationChannelRef(
                CHANNEL_TWO,
            ),
            projected.currentChannelRef,
        )
        assertEquals(
            listOf(
                VICTOR,
                BRI,
                COMPANION,
            ).map(
                ::VexConversationParticipantRef,
            ),
            projected.currentAudienceRefs,
        )
        assertEquals(
            listOf(
                VICTOR,
                BRI,
                COMPANION,
                ALEX,
            ).map(
                ::VexConversationParticipantRef,
            ),
            projected.knownParticipants
                .map { participant ->
                    participant.participantRef
                },
        )
        assertEquals(
            VexConversationParticipantKind.Companion,
            requireNotNull(
                projected.participant(
                    VexConversationParticipantRef(
                        COMPANION,
                    ),
                ),
            ).kind,
        )
        assertEquals(
            VexConversationParticipantKind.Human,
            requireNotNull(
                projected.participant(
                    VexConversationParticipantRef(
                        ALEX,
                    ),
                ),
            ).kind,
        )

        val family =
            requireNotNull(
                projected.familyBinding,
            )
        assertEquals(
            VexConversationSpaceRef(
                SPACE,
            ),
            family.spaceRef,
        )
        assertEquals(
            2L,
            family.currentMembershipGeneration,
        )
        assertEquals(
            listOf(
                CHANNEL_ONE,
                CHANNEL_TWO,
            ).map(
                ::VexConversationChannelRef,
            ),
            family.segmentRefs,
        )
        assertFalse(
            family.historyTruncated,
        )

        assertEquals(
            listOf(
                MESSAGE_ONE,
                MESSAGE_TWO,
            ).map(
                ::VexConversationMessageRef,
            ),
            projected.messages
                .map { message ->
                    message.messageRef
                },
        )
        assertEquals(
            listOf(
                VICTOR,
                ALEX,
                BRI,
                COMPANION,
            ).map(
                ::VexConversationParticipantRef,
            ),
            projected.messages
                .first()
                .witnessRefs,
        )
        assertEquals(
            1L,
            requireNotNull(
                projected.messages
                    .first()
                    .familyBinding,
            ).membershipGeneration,
        )
        assertEquals(
            2L,
            requireNotNull(
                projected.messages
                    .last()
                    .familyBinding,
            ).membershipGeneration,
        )
    }

    @Test
    fun producerTruncationTruthIsPreserved() {
        val projected =
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history =
                    history(
                        truncated = true,
                    ),
            )

        assertTrue(
            requireNotNull(
                projected.familyBinding,
            ).historyTruncated,
        )
    }

    @Test
    fun currentCoordinatesMustMatchExactlyOneBootstrapRoom() {
        val current =
            history()

        listOf(
            current.copy(
                spaceRef =
                    "space.vex-family.other",
            ),
            current.copy(
                threadRef =
                    "thread.vex-family.other",
            ),
            current.copy(
                currentChannelRef =
                    "channel.vex-family.other",
                segmentRefs =
                    listOf(
                        CHANNEL_ONE,
                        "channel.vex-family.other",
                    ),
            ),
            current.copy(
                membershipGeneration = 3L,
            ),
        ).forEach { mismatched ->
            assertThrows(IllegalStateException::class.java) {
                projectVexFamilyConversationServerThread(
                    bootstrap = bootstrap(),
                    history = mismatched,
                )
            }
        }
    }

    @Test
    fun bootstrapCurrentPrincipalMustRemainInCurrentHumanAudience() {
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap =
                    bootstrap()
                        .copy(
                            currentPrincipalRef =
                                ALEX,
                        ),
                history = history(),
            )
        }
    }

    @Test
    fun currentRoomMustConfirmCompanionInclusionForAcceptedHistory() {
        val current =
            bootstrap()
        val excluded =
            current.copy(
                rooms =
                    listOf(
                        current.rooms
                            .single()
                            .copy(
                                familyCompanionIncluded =
                                    false,
                            ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = excluded,
                history = history(),
            )
        }
    }

    @Test
    fun historicalFamilyMessagePreservesCompanionSpeakerAndRequiresWitnessTruth() {
        val current =
            history()
        val companionAuthored =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .first()
                            .copy(
                                speakerRef =
                                    COMPANION,
                                recipientRefs =
                                    listOf(
                                        VICTOR,
                                        ALEX,
                                        BRI,
                                    ),
                            ),
                    ),
            )

        val projected =
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = companionAuthored,
            )

        assertEquals(
            VexConversationParticipantKind.Companion,
            requireNotNull(
                projected.participant(
                    VexConversationParticipantRef(
                        COMPANION,
                    ),
                ),
            ).kind,
        )
        assertEquals(
            VexConversationParticipantRef(
                COMPANION,
            ),
            projected.messages
                .single()
                .speakerRef,
        )
        assertEquals(
            listOf(
                VICTOR,
                ALEX,
                BRI,
            ).map(
                ::VexConversationParticipantRef,
            ),
            projected.messages
                .single()
                .recipientRefs,
        )

        val missingCompanionWitness =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .first()
                            .copy(
                                witnessRefs =
                                    listOf(
                                        VICTOR,
                                        ALEX,
                                        BRI,
                                    ),
                            ),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = missingCompanionWitness,
            )
        }
    }

    @Test
    fun companionLineageCannotCollideWithHumanPrincipalIdentity() {
        val current =
            bootstrap()
        val colliding =
            current.copy(
                rooms =
                    listOf(
                        current.rooms
                            .single()
                            .copy(
                                familyCompanionLineageRef =
                                    VICTOR,
                            ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = colliding,
                history = history(),
            )
        }
    }

    @Test
    fun historyMessageMustBelongToADeclaredSegment() {
        val current =
            history()
        val malformed =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .first()
                            .copy(
                                channelRef =
                                    "channel.vex-family.unknown",
                            ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = malformed,
            )
        }
    }

    @Test
    fun historyMessageGenerationMustMatchCurrentVersusHistoricalSegment() {
        val current =
            history()

        val staleCurrentSegment =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .last()
                            .copy(
                                membershipGeneration =
                                    1L,
                            ),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = staleCurrentSegment,
            )
        }

        val currentGenerationOnHistoricalSegment =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .first()
                            .copy(
                                membershipGeneration =
                                    current.membershipGeneration,
                            ),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = currentGenerationOnHistoricalSegment,
            )
        }
    }

    @Test
    fun historyMessagesMustPreserveSegmentAndSequenceOrder() {
        val current =
            history()

        val reversedSegments =
            current.copy(
                messages =
                    current.messages
                        .reversed(),
            )
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = reversedSegments,
            )
        }

        val repeatedSequence =
            current.copy(
                messages =
                    listOf(
                        current.messages
                            .first(),
                        current.messages
                            .first()
                            .copy(
                                messageRef =
                                    "message.vex-family.second",
                                sequence = 0L,
                            ),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            projectVexFamilyConversationServerThread(
                bootstrap = bootstrap(),
                history = repeatedSequence,
            )
        }
    }

    private fun bootstrap():
        VexFamilyConversationServerBootstrap =
        VexFamilyConversationServerBootstrap(
            currentPrincipalRef =
                VICTOR,
            rooms =
                listOf(
                    VexFamilyConversationServerRoom(
                        spaceRef =
                            SPACE,
                        threadRef =
                            THREAD,
                        channelRef =
                            CHANNEL_TWO,
                        membershipGeneration =
                            2L,
                        audiencePrincipalRefs =
                            listOf(
                                VICTOR,
                                BRI,
                            ),
                        familyCompanionLineageRef =
                            COMPANION,
                        familyCompanionIncluded =
                            true,
                    ),
                ),
        )

    private fun history(
        truncated: Boolean = false,
    ): VexFamilyConversationServerThreadHistory =
        VexFamilyConversationServerThreadHistory(
            spaceRef =
                SPACE,
            threadRef =
                THREAD,
            currentChannelRef =
                CHANNEL_TWO,
            membershipGeneration =
                2L,
            segmentRefs =
                listOf(
                    CHANNEL_ONE,
                    CHANNEL_TWO,
                ),
            messages =
                listOf(
                    VexFamilyConversationServerMessage(
                        messageRef =
                            MESSAGE_ONE,
                        spaceRef =
                            SPACE,
                        threadRef =
                            THREAD,
                        channelRef =
                            CHANNEL_ONE,
                        speakerRef =
                            VICTOR,
                        recipientRefs =
                            listOf(
                                ALEX,
                                BRI,
                            ),
                        witnessRefs =
                            listOf(
                                VICTOR,
                                ALEX,
                                BRI,
                                COMPANION,
                            ),
                        membershipGeneration =
                            1L,
                        sequence =
                            0L,
                        content =
                            "Before Alex left.",
                    ),
                    VexFamilyConversationServerMessage(
                        messageRef =
                            MESSAGE_TWO,
                        spaceRef =
                            SPACE,
                        threadRef =
                            THREAD,
                        channelRef =
                            CHANNEL_TWO,
                        speakerRef =
                            BRI,
                        recipientRefs =
                            listOf(
                                VICTOR,
                            ),
                        witnessRefs =
                            listOf(
                                VICTOR,
                                BRI,
                                COMPANION,
                            ),
                        membershipGeneration =
                            2L,
                        sequence =
                            0L,
                        content =
                            "After Alex left.",
                    ),
                ),
            truncated =
                truncated,
        )

    private companion object {
        const val SPACE =
            "space.vex-family.android-adapter"
        const val THREAD =
            "thread.vex-family.android-adapter"
        const val CHANNEL_ONE =
            "channel.vex-family.android-adapter.g1"
        const val CHANNEL_TWO =
            "channel.vex-family.android-adapter.g2"
        const val VICTOR =
            "person.victor-gong"
        const val ALEX =
            "person.alex"
        const val BRI =
            "person.bri"
        const val COMPANION =
            "lineage.vex.family.android-adapter"
        const val MESSAGE_ONE =
            "message.vex-family.android-adapter.1"
        const val MESSAGE_TWO =
            "message.vex-family.android-adapter.2"
    }
}

// [VXG RealForever]
