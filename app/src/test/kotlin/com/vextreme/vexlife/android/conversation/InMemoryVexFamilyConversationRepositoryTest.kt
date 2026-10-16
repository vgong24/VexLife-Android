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
    private val channelGenerationOne =
        VexConversationChannelRef(
            "channel.fixture.family-room.g1",
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
    fun firstCanonicalFamilyAppendUsesGenerationOneAndSequenceZero() =
        runTest {
            val repository =
                repository(
                    emptyGenerationOne(),
                )

            val result =
                repository.send(
                    requestFor(
                        thread =
                            currentThread(
                                repository,
                            ),
                        content =
                            "First canonical Family message.",
                        key =
                            "first-message",
                    ),
                )

            assertTrue(
                result is
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val appended =
                currentThread(
                    repository,
                ).messages.single()
            val family =
                requireNotNull(
                    appended.familyBinding,
                )

            assertEquals(
                0L,
                appended.sequence,
            )
            assertEquals(
                1L,
                family.membershipGeneration,
            )
        }

    @Test
    fun sameIdempotencyKeyInSuccessorChannelFormsDistinctSequenceZeroMessage() =
        runTest {
            val repository =
                repository(
                    emptyGenerationTwo(),
                )
            val key =
                "same-key-across-generations"

            val firstResult =
                repository.send(
                    requestFor(
                        thread =
                            currentThread(
                                repository,
                            ),
                        content =
                            "Message in generation two.",
                        key =
                            key,
                    ),
                )

            val firstRef =
                (
                    firstResult as
                        VexFamilyConversationRepositoryResult.Accepted
                    ).messageRef
            val afterGenerationTwo =
                currentThread(
                    repository,
                )
            val firstMessage =
                afterGenerationTwo
                    .messages
                    .single()

            assertEquals(
                0L,
                firstMessage.sequence,
            )
            assertEquals(
                channelGenerationTwo,
                firstMessage.channelRef,
            )

            repository.publishThread(
                generationThreeFrom(
                    afterGenerationTwo,
                ),
            )

            val secondResult =
                repository.send(
                    requestFor(
                        thread =
                            currentThread(
                                repository,
                            ),
                        content =
                            "Message in generation three.",
                        key =
                            key,
                    ),
                )

            val secondRef =
                (
                    secondResult as
                        VexFamilyConversationRepositoryResult.Accepted
                    ).messageRef
            val messages =
                currentThread(
                    repository,
                ).messages

            assertEquals(
                2,
                messages.size,
            )
            assertEquals(
                0L,
                messages[0].sequence,
            )
            assertEquals(
                0L,
                messages[1].sequence,
            )
            assertEquals(
                channelGenerationTwo,
                messages[0].channelRef,
            )
            assertEquals(
                channelGenerationThree,
                messages[1].channelRef,
            )
            assertNotEquals(
                firstRef,
                secondRef,
            )
        }

    @Test
    fun sameChannelRetryIdentitySurvivesRepositoryReconstructionAndChangedMeaningConflicts() =
        runTest {
            val original =
                repository(
                    generationTwo(),
                )
            val request =
                requestFor(
                    thread =
                        currentThread(
                            original,
                        ),
                    content =
                        "One canonical meaning.",
                    key =
                        "same-channel-key",
                )

            val first =
                original.send(
                    request,
                )
            val persistedState =
                original.state.value

            /*
             * VF-02C resolves idempotency from deterministic canonical message identity in the
             * Conversation Store. Reconstruct the fake from current state to prove retry does not
             * depend on process-local idempotency bookkeeping.
             */
            val reconstructed =
                InMemoryVexFamilyConversationRepository(
                    initialState =
                        persistedState,
                    admittedSpeakerRef =
                        victorRef,
                )

            val retry =
                reconstructed.send(
                    request,
                )

            assertEquals(
                first,
                retry,
            )
            assertEquals(
                2,
                currentThread(
                    reconstructed,
                ).messages.size,
            )

            assertEquals(
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection
                        .IDEMPOTENCY_CONFLICT,
                ),
                reconstructed.send(
                    request.copy(
                        content =
                            "Different meaning.",
                    ),
                ),
            )
        }

    @Test
    fun staleGenerationIsRejectedBeforeRetryIdentityCanAdmitIt() =
        runTest {
            val repository =
                repository(
                    generationTwo(),
                )
            val staleRequest =
                requestFor(
                    thread =
                        currentThread(
                            repository,
                        ),
                    content =
                        "Bound to generation two.",
                    key =
                        "stale-before-send",
                )

            repository.publishThread(
                generationThreeFrom(
                    currentThread(
                        repository,
                    ),
                ),
            )

            val before =
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
                before,
                repository.state.value,
            )
        }

    @Test
    fun familyProviderDerivesHumanRecipientsAndFullWitnessAudience() =
        runTest {
            val repository =
                repository(
                    generationTwo(),
                )

            assertTrue(
                repository.send(
                    requestFor(
                        thread =
                            currentThread(
                                repository,
                            ),
                        content =
                            "Provider-derived addressing.",
                        key =
                            "provider-addressing",
                    ),
                ) is
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val appended =
                currentThread(
                    repository,
                ).messages.last()

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

            first.send(
                requestFor(
                    thread =
                        currentThread(
                            first,
                        ),
                    content =
                        "Only first changes.",
                    key =
                        "instance-isolation",
                ),
            )

            assertNotEquals(
                first.state.value,
                second.state.value,
            )
            assertEquals(
                1,
                currentThread(
                    second,
                ).messages.size,
            )
        }

    @Test
    fun removedParticipantRemainsKnownForHistoricalAuthorshipAndWitnesses() {
        val current =
            generationThreeFrom(
                generationTwo(),
            )
        val family =
            requireNotNull(
                current.familyBinding,
            )
        val historical =
            current.messages.single()

        assertFalse(
            mayaRef in
                current.currentAudienceRefs,
        )
        assertTrue(
            current.participant(
                mayaRef,
            ) != null,
        )
        assertEquals(
            channelGenerationTwo,
            historical.channelRef,
        )
        assertEquals(
            2L,
            requireNotNull(
                historical.familyBinding,
            ).membershipGeneration,
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

    private fun currentThread(
        repository:
            InMemoryVexFamilyConversationRepository,
    ): VexConversationThreadState =
        requireNotNull(
            repository.state.value
                .thread(
                    threadRef,
                ),
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

    private fun emptyGenerationOne():
        VexConversationThreadState =
        familyThread(
            currentChannelRef =
                channelGenerationOne,
            generation =
                1L,
            audience =
                listOf(
                    victorRef,
                    mayaRef,
                    vexRef,
                ),
            segmentRefs =
                listOf(
                    channelGenerationOne,
                ),
            messages =
                emptyList(),
        )

    private fun emptyGenerationTwo():
        VexConversationThreadState =
        familyThread(
            currentChannelRef =
                channelGenerationTwo,
            generation =
                2L,
            audience =
                listOf(
                    victorRef,
                    mayaRef,
                    vexRef,
                ),
            segmentRefs =
                listOf(
                    channelGenerationTwo,
                ),
            messages =
                emptyList(),
        )

    private fun generationTwo():
        VexConversationThreadState =
        emptyGenerationTwo()
            .copy(
                messages =
                    listOf(
                        VexConversationMessage(
                            messageRef =
                                VexConversationMessageRef(
                                    "message.fixture.family-room.g2.000",
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
                                "Historical message before successor generation.",
                            familyBinding =
                                VexFamilyConversationMessageBinding(
                                    spaceRef =
                                        spaceRef,
                                    membershipGeneration =
                                        2L,
                                ),
                        ),
                    ),
            )

    private fun generationThreeFrom(
        prior:
            VexConversationThreadState,
    ): VexConversationThreadState {
        val priorFamily =
            requireNotNull(
                prior.familyBinding,
            )

        return familyThread(
            currentChannelRef =
                channelGenerationThree,
            generation =
                3L,
            audience =
                listOf(
                    victorRef,
                    alexRef,
                    vexRef,
                ),
            segmentRefs =
                (
                    priorFamily.segmentRefs +
                        channelGenerationThree
                    ).distinct(),
            messages =
                prior.messages,
        )
    }

    private fun familyThread(
        currentChannelRef:
            VexConversationChannelRef,
        generation: Long,
        audience:
            List<VexConversationParticipantRef>,
        segmentRefs:
            List<VexConversationChannelRef>,
        messages:
            List<VexConversationMessage>,
    ): VexConversationThreadState =
        VexConversationThreadState(
            threadRef =
                threadRef,
            currentChannelRef =
                currentChannelRef,
            knownParticipants =
                knownParticipants(),
            currentAudienceRefs =
                audience,
            messages =
                messages,
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        spaceRef,
                    currentMembershipGeneration =
                        generation,
                    segmentRefs =
                        segmentRefs,
                ),
        )
}

// [VXG RealForever]
