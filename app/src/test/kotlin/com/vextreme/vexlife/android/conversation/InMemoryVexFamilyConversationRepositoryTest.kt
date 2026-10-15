package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryVexFamilyConversationRepositoryTest {
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
    private val channelGenerationZero =
        VexConversationChannelRef(
            "channel.fixture.family-room.g0",
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
    fun firstCanonicalFamilyAppendUsesSequenceZero() =
        runTest {
            val repository =
                repository(
                    emptyGenerationZero(),
                )
            val thread =
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )

            assertTrue(
                repository.send(
                    requestFor(
                        thread =
                            thread,
                        content =
                            "First canonical Family message.",
                        key =
                            "first-message-zero",
                    ),
                ) is
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val appended =
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )
                    .messages
                    .single()

            assertEquals(
                0L,
                appended.sequence,
            )
            assertEquals(
                0L,
                requireNotNull(
                    appended.familyBinding,
                ).membershipGeneration,
            )
        }

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
                    requireNotNull(
                        first.state.value
                            .thread(
                                threadRef,
                            ),
                    ),
                    content =
                        "Only the first repository should change.",
                    key =
                        "instance-isolation",
                )

            assertTrue(
                first.send(request) is
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            assertNotEquals(
                first.state.value,
                second.state.value,
            )
            assertEquals(
                1,
                requireNotNull(
                    second.state.value
                        .thread(
                            threadRef,
                        ),
                )
                    .messages
                    .size,
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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
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
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val appended =
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )
                    .messages
                    .last()

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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
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
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection
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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )
            val family =
                requireNotNull(
                    currentThread.familyBinding,
                )

            assertEquals(
                listOf(
                    channelGenerationTwo,
                    channelGenerationThree,
                ),
                family.segmentRefs,
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
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val appended =
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )
                    .messages
                    .last()
            val appendedFamily =
                requireNotNull(
                    appended.familyBinding,
                )

            assertEquals(
                channelGenerationThree,
                appended.channelRef,
            )
            assertEquals(
                3L,
                appendedFamily.membershipGeneration,
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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
                )
                    .messages
                    .size,
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
                requireNotNull(
                    repository.state.value
                        .thread(
                            threadRef,
                        ),
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
                    VexFamilyConversationRepositoryResult.Accepted,
            )
            assertEquals(
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection
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
        val family =
            requireNotNull(
                current.familyBinding,
            )
        val historicalFamily =
            requireNotNull(
                historical.familyBinding,
            )

        assertEquals(
            channelGenerationTwo,
            historical.channelRef,
        )
        assertEquals(
            2L,
            historicalFamily.membershipGeneration,
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
            family.segmentRefs,
        )
    }

    private fun repository(
        thread:
            VexConversationThreadState,
    ): InMemoryVexFamilyConversationRepository =
        InMemoryVexFamilyConversationRepository(
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
    ): VexFamilyConversationSendRequest {
        val family =
            requireNotNull(
                thread.familyBinding,
            )

        return VexFamilyConversationSendRequest(
            threadRef =
                thread.threadRef,
            spaceRef =
                family.spaceRef,
            expectedChannelRef =
                thread.currentChannelRef,
            expectedMembershipGeneration =
                family.currentMembershipGeneration,
            idempotencyKey =
                VexConversationIdempotencyKey(
                    key,
                ),
            content =
                content,
        )
    }

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

    private fun emptyGenerationZero():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            currentChannelRef =
                channelGenerationZero,
            knownParticipants =
                knownParticipants(),
            currentAudienceRefs =
                listOf(
                    victorRef,
                    mayaRef,
                    vexRef,
                ),
            messages =
                emptyList(),
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        spaceRef,
                    currentMembershipGeneration =
                        0L,
                    segmentRefs =
                        listOf(
                            channelGenerationZero,
                        ),
                ),
        )

    private fun generationTwo():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            currentChannelRef =
                channelGenerationTwo,
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
                                "message.fixture.family-room.000",
                            ),
                        threadRef =
                            threadRef,
                        channelRef =
                            channelGenerationTwo,
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
                            "Historical message authored before Maya later leaves.",
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
                            channelGenerationTwo,
                        ),
                ),
        )

    private fun generationThree():
        VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            currentChannelRef =
                channelGenerationThree,
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
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        spaceRef,
                    currentMembershipGeneration =
                        3L,
                    segmentRefs =
                        listOf(
                            channelGenerationTwo,
                            channelGenerationThree,
                        ),
                ),
        )
}

// [VXG RealForever]
