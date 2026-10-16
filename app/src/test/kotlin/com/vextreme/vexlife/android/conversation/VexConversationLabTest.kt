package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexConversationLabTest {
    private val victorRef =
        VexConversationLabFixture.Victor
            .participant
            .participantRef
    private val mayaRef =
        VexConversationLabFixture.Maya
            .participant
            .participantRef
    private val alexRef =
        VexConversationLabFixture.Alex
            .participant
            .participantRef
    private val vexRef =
        VexConversationLabFixture.Vex
            .participant
            .participantRef

    @Test
    fun seedFamilyRoomIsDeterministicAndKeepsAlexKnownButNotCurrent() {
        val (repository, lab) =
            fixture()

        val thread =
            lab.seedFamilyRoom()

        assertEquals(
            VexConversationLabFixture.threadRef,
            thread.threadRef,
        )
        assertEquals(
            1L,
            requireNotNull(
                thread.familyBinding,
            ).currentMembershipGeneration,
        )
        assertEquals(
            listOf(
                victorRef,
                mayaRef,
                vexRef,
            ),
            thread.currentAudienceRefs,
        )
        assertNotNull(
            thread.participant(
                alexRef,
            ),
        )
        assertTrue(
            thread.messages.isEmpty(),
        )
        assertEquals(
            thread,
            repository
                .state
                .value
                .thread(
                    VexConversationLabFixture.threadRef,
                ),
        )
        assertTrue(
            lab.state.value.simulationEnabled,
        )
    }

    @Test
    fun addAndRemoveParticipantsAdvanceGenerationWithoutManufacturingMessages() {
        val (_, lab) =
            fixture()
        lab.seedFamilyRoom()

        val withAlex =
            lab.addParticipant(
                alexRef,
            )

        assertEquals(
            2L,
            requireNotNull(
                withAlex.familyBinding,
            ).currentMembershipGeneration,
        )
        assertEquals(
            VexConversationLabFixture.channelRef(2L),
            withAlex.currentChannelRef,
        )
        assertEquals(
            listOf(
                victorRef,
                mayaRef,
                vexRef,
                alexRef,
            ),
            withAlex.currentAudienceRefs,
        )
        assertTrue(
            withAlex.messages.isEmpty(),
        )

        val withoutMaya =
            lab.removeParticipant(
                mayaRef,
            )

        assertEquals(
            3L,
            requireNotNull(
                withoutMaya.familyBinding,
            ).currentMembershipGeneration,
        )
        assertFalse(
            mayaRef in
                withoutMaya.currentAudienceRefs,
        )
        assertNotNull(
            withoutMaya.participant(
                mayaRef,
            ),
        )
        assertTrue(
            withoutMaya.messages.isEmpty(),
        )
        assertEquals(
            listOf(
                VexConversationLabFixture.channelRef(1L),
                VexConversationLabFixture.channelRef(2L),
                VexConversationLabFixture.channelRef(3L),
            ),
            requireNotNull(
                withoutMaya.familyBinding,
            ).segmentRefs,
        )
    }

    @Test
    fun remoteSpeechPublishesProviderTruthThroughConversationRepository() {
        val (repository, lab) =
            fixture()
        lab.seedFamilyRoom()

        val message =
            lab.publishRemoteMessage(
                content = "test message",
                speakerRef = mayaRef,
            )

        val current =
            requireNotNull(
                repository
                    .state
                    .value
                    .thread(
                        VexConversationLabFixture.threadRef,
                    ),
            )

        assertEquals(
            mayaRef,
            message.speakerRef,
        )
        assertEquals(
            listOf(
                victorRef,
            ),
            message.recipientRefs,
        )
        assertEquals(
            listOf(
                victorRef,
                mayaRef,
                vexRef,
            ),
            message.witnessRefs,
        )
        assertEquals(
            listOf(
                message,
            ),
            current.messages,
        )
        assertEquals(
            "scenario.conversation.lab.c04.remote-speech",
            lab.state.value.scenarioRef,
        )
    }

    @Test
    fun remoteInputUsesKnownHumanLabelWithoutMintingIdentity() {
        val (repository, lab) =
            fixture()
        lab.seedFamilyRoom()

        val message =
            lab.publishRemoteInput(
                "[Maya]{test message}",
            )

        assertEquals(
            mayaRef,
            message.speakerRef,
        )
        assertEquals(
            "test message",
            message.content,
        )
        assertEquals(
            listOf(
                message,
            ),
            requireNotNull(
                repository
                    .state
                    .value
                    .thread(
                        VexConversationLabFixture.threadRef,
                    ),
            ).messages,
        )
    }

    @Test
    fun remoteInputRejectsUnknownSpeakerWithoutChangingConversationTruth() {
        val (repository, lab) =
            fixture()
        lab.seedFamilyRoom()

        val before =
            repository.state.value
        val failure =
            runCatching {
                lab.publishRemoteInput(
                    "[Unknown]{do not mint me}",
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException,
        )
        assertEquals(
            before,
            repository.state.value,
        )
    }

    @Test
    fun victorSendStaysOnRepositoryContractAndMirrorArrivesOnlyAfterAcceptance() =
        runBlocking {
            val (repository, lab) =
                fixture()
            val seeded =
                lab.seedFamilyRoom()
            lab.setAutoMirror(true)

            val family =
                requireNotNull(
                    seeded.familyBinding,
                )
            val content =
                "Are you coming?"
            val result =
                repository.send(
                    VexFamilyConversationSendRequest(
                        threadRef =
                            seeded.threadRef,
                        spaceRef =
                            family.spaceRef,
                        expectedChannelRef =
                            seeded.currentChannelRef,
                        expectedMembershipGeneration =
                            family.currentMembershipGeneration,
                        idempotencyKey =
                            VexConversationIdempotencyKey(
                                "debug-victor-send-001",
                            ),
                        content =
                            content,
                    ),
                )

            assertTrue(
                result is
                    VexFamilyConversationRepositoryResult.Accepted,
            )

            val mirror =
                lab.mirrorAcceptedVictorSend(
                    content,
                )

            val current =
                requireNotNull(
                    repository
                        .state
                        .value
                        .thread(
                            seeded.threadRef,
                        ),
                )

            assertEquals(
                2,
                current.messages.size,
            )
            assertEquals(
                victorRef,
                current.messages[0].speakerRef,
            )
            assertEquals(
                0L,
                current.messages[0].sequence,
            )
            assertEquals(
                mayaRef,
                current.messages[1].speakerRef,
            )
            assertEquals(
                1L,
                current.messages[1].sequence,
            )
            assertEquals(
                "You said: \"Are you coming?\"",
                mirror?.content,
            )
            assertEquals(
                "scenario.conversation.lab.c06.mirror-response",
                lab.state.value.scenarioRef,
            )
        }

    @Test
    fun generationAdvanceMakesPreviouslyFormedSendStaleWithoutGhostMessage() =
        runBlocking {
            val (repository, lab) =
                fixture()
            val seeded =
                lab.seedFamilyRoom()
            val family =
                requireNotNull(
                    seeded.familyBinding,
                )

            val staleRequest =
                VexFamilyConversationSendRequest(
                    threadRef =
                        seeded.threadRef,
                    spaceRef =
                        family.spaceRef,
                    expectedChannelRef =
                        seeded.currentChannelRef,
                    expectedMembershipGeneration =
                        family.currentMembershipGeneration,
                    idempotencyKey =
                        VexConversationIdempotencyKey(
                            "debug-stale-send-001",
                        ),
                    content =
                        "This should be stale.",
                )

            val advanced =
                lab.advanceMembershipGeneration()
            val result =
                repository.send(
                    staleRequest,
                )

            assertEquals(
                2L,
                requireNotNull(
                    advanced.familyBinding,
                ).currentMembershipGeneration,
            )
            assertEquals(
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection
                        .CURRENTNESS_CHANGED,
                ),
                result,
            )
            assertTrue(
                requireNotNull(
                    repository
                        .state
                        .value
                        .thread(
                            seeded.threadRef,
                        ),
                ).messages.isEmpty(),
            )
        }

    @Test
    fun availabilityControlsDoNotRewriteConversationTruth() {
        val (repository, lab) =
            fixture()
        lab.seedFamilyRoom()

        val before =
            repository.state.value

        lab.setHomeAvailability(
            VexConversationHomeAvailability.Unavailable,
        )
        lab.setCompanionAvailability(
            VexConversationCompanionAvailability.Unavailable,
        )

        assertEquals(
            before,
            repository.state.value,
        )
        assertSame(
            VexConversationHomeAvailability.Unavailable,
            lab.state.value.homeAvailability,
        )
        assertSame(
            VexConversationCompanionAvailability.Unavailable,
            lab.state.value.companionAvailability,
        )
    }

    @Test
    fun resetReseedsTheSameKnownWorldAndClearsMessages() {
        val (_, lab) =
            fixture()
        lab.seedFamilyRoom()
        lab.publishRemoteMessage(
            content = "temporary",
        )
        lab.addParticipant(
            alexRef,
        )

        val reset =
            lab.resetFamilyRoom()

        assertEquals(
            1L,
            requireNotNull(
                reset.familyBinding,
            ).currentMembershipGeneration,
        )
        assertEquals(
            listOf(
                victorRef,
                mayaRef,
                vexRef,
            ),
            reset.currentAudienceRefs,
        )
        assertTrue(
            reset.messages.isEmpty(),
        )
        assertEquals(
            "scenario.conversation.lab.c16.reset-reseed",
            lab.state.value.scenarioRef,
        )
    }

    private fun fixture():
        Pair<
            InMemoryVexFamilyConversationRepository,
            VexConversationLab,
        > {
        val repository =
            InMemoryVexFamilyConversationRepository(
                admittedSpeakerRef =
                    victorRef,
            )

        return repository to
            VexConversationLab(
                repository,
            )
    }
}

// [VXG RealForever]
