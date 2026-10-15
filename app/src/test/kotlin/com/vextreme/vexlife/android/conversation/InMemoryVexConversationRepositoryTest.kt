package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryVexConversationRepositoryTest {
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
    private val threadRef =
        VexConversationThreadRef(
            "thread.fixture.family-room",
        )
    private val channelGenerationTwo =
        VexConversationChannelRef(
            "channel.fixture.family-room.g2",
        )
    private val channelGenerationThree =
        VexConversationChannelRef(
            "channel.fixture.family-room.g3",
        )

    @Test
    fun repositoryInstancesDoNotShareFixtureState() =
        runTest {
            val first =
                repository(
                    generationTwo(),
                )
            val second =
                repository(
                    generationTwo(),
                )

            val request =
                requestFor(
                    first.state.value
                        .thread(
                            threadRef,
                        )
                        ?: error(
                            "fixture thread missing",
                        ),
                    recipients =
                        listOf(
                            mayaRef,
                            vexRef,
                        ),
                    content =
                        "Only the first repository should change.",
                )

            assertTrue(
                first.send(request) is
                    VexConversationRepositoryResult.Accepted,
            )

            assertNotEquals(
                first.state.value,
                second.state.value,
            )
            assertEquals(
                1,
                second.state.value
                    .thread(
                        threadRef,
                    )
                    ?.timeline
                    ?.filterIsInstance<
                        VexConversationTimelineEntry.Message
                    >()
                    ?.size,
            )
        }

    @Test
    fun sendBindsProviderSpeakerAndFullWitnessAudienceRatherThanCallerAuthorship() =
        runTest {
            val repository =
                repository(
                    generationTwo(),
                )
            val thread =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?: error(
                        "fixture thread missing",
                    )

            val result =
                repository.send(
                    requestFor(
                        thread =
                            thread,
                        recipients =
                            listOf(
                                vexRef,
                            ),
                        content =
                            "Address Vex without turning the addressee into the audience.",
                    ),
                )

            assertTrue(
                result is
                    VexConversationRepositoryResult.Accepted,
            )

            val appended =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?.timeline
                    ?.filterIsInstance<
                        VexConversationTimelineEntry.Message
                    >()
                    ?.last()
                    ?: error(
                        "appended message missing",
                    )

            assertEquals(
                victorRef,
                appended.speakerRef,
            )
            assertEquals(
                listOf(
                    vexRef,
                ),
                appended.recipientRefs,
            )
            assertEquals(
                listOf(
                    victorRef,
                    mayaRef,
                    vexRef,
                ),
                appended.witnessRefs,
            )
        }

    @Test
    fun membershipChangeBetweenDraftAndSendRejectsStaleBindingWithoutPosting() =
        runTest {
            val repository =
                repository(
                    generationTwo(),
                )
            val draftThread =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?: error(
                        "fixture thread missing",
                    )
            val staleRequest =
                requestFor(
                    thread =
                        draftThread,
                    recipients =
                        listOf(
                            mayaRef,
                            vexRef,
                        ),
                    content =
                        "Dinner is still at seven.",
                )

            /*
             * Test-side provider pressure: current Family/Conversation truth advances before the
             * already-authored request is activated. Product code does not own this mutation API.
             */
            repository.publishThread(
                generationThree(),
            )

            val beforeSend =
                repository.state.value

            assertEquals(
                VexConversationRepositoryResult.Rejected(
                    VexConversationRepositoryRejection
                        .CURRENTNESS_CHANGED,
                ),
                repository.send(
                    staleRequest,
                ),
            )

            assertEquals(
                beforeSend,
                repository.state.value,
            )

            val currentThread =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?: error(
                        "current fixture thread missing",
                    )

            assertTrue(
                repository.send(
                    requestFor(
                        thread =
                            currentThread,
                        recipients =
                            listOf(
                                vexRef,
                            ),
                        content =
                            staleRequest.content,
                    ),
                ) is
                    VexConversationRepositoryResult.Accepted,
            )

            val appended =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?.timeline
                    ?.filterIsInstance<
                        VexConversationTimelineEntry.Message
                    >()
                    ?.last()
                    ?: error(
                        "rebound message missing",
                    )

            assertEquals(
                channelGenerationThree,
                appended.channelRef,
            )
            assertEquals(
                3L,
                appended.membershipGeneration,
            )
            assertEquals(
                listOf(
                    victorRef,
                    vexRef,
                ),
                appended.witnessRefs,
            )
            assertFalse(
                mayaRef in
                    appended.witnessRefs,
            )
        }

    @Test
    fun removedParticipantRemainsKnownForHistoricalAuthorshipAndWitnesses() {
        val current =
            generationThree()

        assertFalse(
            mayaRef in
                current.currentAudienceRefs,
        )
        assertTrue(
            current.participant(
                mayaRef,
            ) != null,
        )

        val historical =
            current.timeline
                .filterIsInstance<
                    VexConversationTimelineEntry.Message
                >()
                .single()

        assertEquals(
            channelGenerationTwo,
            historical.channelRef,
        )
        assertEquals(
            2L,
            historical.membershipGeneration,
        )
        assertEquals(
            mayaRef,
            historical.speakerRef,
        )
        assertTrue(
            mayaRef in
                historical.witnessRefs,
        )
    }

    private fun repository(
        thread:
            VexConversationThreadState,
    ): InMemoryVexConversationRepository =
        InMemoryVexConversationRepository(
            initialState =
                VexConversationState(
                    threads =
                        listOf(
                            thread,
                        ),
                ),
            admittedSpeakerRef =
                victorRef,
        )

    private fun requestFor(
        thread:
            VexConversationThreadState,
        recipients:
            List<VexConversationParticipantRef>,
        content: String,
    ): VexConversationSendRequest =
        VexConversationSendRequest(
            threadRef =
                thread.threadRef,
            expectedChannelRef =
                thread.currentChannelRef,
            expectedMembershipGeneration =
                thread
                    .currentMembershipGeneration,
            recipientRefs =
                recipients,
            content =
                content,
        )

    private fun knownParticipants():
        List<VexConversationParticipant> =
        listOf(
            VexConversationParticipant(
                participantRef =
                    victorRef,
                displayName =
                    "Victor",
                kind =
                    VexConversationParticipantKind.Human,
            ),
            VexConversationParticipant(
                participantRef =
                    mayaRef,
                displayName =
                    "Maya",
                kind =
                    VexConversationParticipantKind.Human,
            ),
            VexConversationParticipant(
                participantRef =
                    vexRef,
                displayName =
                    "Vex",
                kind =
                    VexConversationParticipantKind.Companion,
            ),
        )

    private fun generationTwo():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            title =
                "Family Room",
            currentChannelRef =
                channelGenerationTwo,
            currentMembershipGeneration =
                2L,
            knownParticipants =
                knownParticipants(),
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
                            "message.fixture.family-room.001",
                        threadRef =
                            threadRef,
                        messageRef =
                            VexConversationMessageRef(
                                "message.fixture.family-room.001",
                            ),
                        channelRef =
                            channelGenerationTwo,
                        membershipGeneration =
                            2L,
                        sequence =
                            1L,
                        speakerRef =
                            mayaRef,
                        recipientRefs =
                            listOf(
                                victorRef,
                                vexRef,
                            ),
                        witnessRefs =
                            listOf(
                                victorRef,
                                mayaRef,
                                vexRef,
                            ),
                        content =
                            "Historical message authored before Maya later leaves.",
                    ),
                ),
        )

    private fun generationThree():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            title =
                "Family Room",
            currentChannelRef =
                channelGenerationThree,
            currentMembershipGeneration =
                3L,
            knownParticipants =
                knownParticipants(),
            currentAudienceRefs =
                listOf(
                    victorRef,
                    vexRef,
                ),
            timeline =
                generationTwo()
                    .timeline +
                    VexConversationTimelineEntry.MembershipBoundary(
                        entryRef =
                            "boundary.fixture.family-room.g3",
                        threadRef =
                            threadRef,
                        fromChannelRef =
                            channelGenerationTwo,
                        toChannelRef =
                            channelGenerationThree,
                        membershipGeneration =
                            3L,
                        participantRef =
                            mayaRef,
                        change =
                            VexConversationMembershipChange.Left,
                    ),
        )
}

// [VXG RealForever]
