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
    private val alexRef =
        VexConversationParticipantRef(
            "person.fixture.alex",
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
                    content =
                        "Only the first repository should change.",
                    key =
                        "instance-isolation",
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
                    ?.messages
                    ?.size,
            )
        }

    @Test
    fun familySendDerivesHumanRecipientsAndFullWitnessAudienceAtProvider() =
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
                        content =
                            "Family delivery is provider-derived.",
                        key =
                            "provider-derived-family-recipients",
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
                    ?.messages
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
                    mayaRef,
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
                    content =
                        "Dinner is still at seven.",
                    key =
                        "draft-before-membership-change",
                )

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

            assertEquals(
                listOf(
                    channelGenerationTwo,
                    channelGenerationThree,
                ),
                currentThread.segmentRefs,
            )

            assertTrue(
                repository.send(
                    requestFor(
                        thread =
                            currentThread,
                        content =
                            staleRequest.content,
                        key =
                            "draft-after-membership-change",
                    ),
                ) is
                    VexConversationRepositoryResult.Accepted,
            )

            val appended =
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?.messages
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
                    alexRef,
                ),
                appended.recipientRefs,
            )
            assertEquals(
                listOf(
                    victorRef,
                    alexRef,
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
    fun exactIdempotentRetryReturnsSameMessageWithoutDuplicateAppend() =
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
            val request =
                requestFor(
                    thread =
                        thread,
                    content =
                        "One canonical message.",
                    key =
                        "retry-one",
                )

            val first =
                repository.send(
                    request,
                )
            val second =
                repository.send(
                    request,
                )

            assertEquals(
                first,
                second,
            )
            assertEquals(
                2,
                repository.state.value
                    .thread(
                        threadRef,
                    )
                    ?.messages
                    ?.size,
            )
        }

    @Test
    fun reusedIdempotencyKeyWithDifferentMeaningFailsClosed() =
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

            val first =
                requestFor(
                    thread =
                        thread,
                    content =
                        "First meaning.",
                    key =
                        "conflict-one",
                )
            val conflicting =
                first.copy(
                    content =
                        "Different meaning.",
                )

            assertTrue(
                repository.send(first) is
                    VexConversationRepositoryResult.Accepted,
            )
            assertEquals(
                VexConversationRepositoryResult.Rejected(
                    VexConversationRepositoryRejection
                        .IDEMPOTENCY_CONFLICT,
                ),
                repository.send(
                    conflicting,
                ),
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
            current.messages
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
        assertEquals(
            listOf(
                channelGenerationTwo,
                channelGenerationThree,
            ),
            current.segmentRefs,
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
        content: String,
        key: String,
    ): VexConversationSendRequest =
        VexConversationSendRequest(
            threadRef =
                thread.threadRef,
            spaceRef =
                thread.spaceRef,
            expectedChannelRef =
                thread.currentChannelRef,
            expectedMembershipGeneration =
                thread
                    .currentMembershipGeneration,
            idempotencyKey =
                VexConversationIdempotencyKey(
                    key,
                ),
            content =
                content,
        )

    private fun knownParticipants():
        List<VexConversationParticipant> =
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
                    alexRef,
                kind =
                    VexConversationParticipantKind.Human,
            ),
            VexConversationParticipant(
                participantRef =
                    vexRef,
                kind =
                    VexConversationParticipantKind.Companion,
            ),
        )

    private fun generationTwo():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            spaceRef =
                spaceRef,
            currentChannelRef =
                channelGenerationTwo,
            currentMembershipGeneration =
                2L,
            segmentRefs =
                listOf(
                    channelGenerationTwo,
                ),
            knownParticipants =
                knownParticipants(),
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
                                "message.fixture.family-room.001",
                            ),
                        threadRef =
                            threadRef,
                        spaceRef =
                            spaceRef,
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
            spaceRef =
                spaceRef,
            currentChannelRef =
                channelGenerationThree,
            currentMembershipGeneration =
                3L,
            segmentRefs =
                listOf(
                    channelGenerationTwo,
                    channelGenerationThree,
                ),
            knownParticipants =
                knownParticipants(),
            currentAudienceRefs =
                listOf(
                    victorRef,
                    alexRef,
                    vexRef,
                ),
            messages =
                generationTwo()
                    .messages,
        )
}

// [VXG RealForever]
