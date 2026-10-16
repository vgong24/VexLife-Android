package com.vextreme.vexlife.android.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexConversationConvergenceRelayTest {
    private val victorRef =
        VexConversationLabFixture
            .Victor
            .participant
            .participantRef
    private val mayaRef =
        VexConversationLabFixture
            .Maya
            .participant
            .participantRef
    private val vexRef =
        VexConversationLabFixture
            .Vex
            .participant
            .participantRef

    @Test
    fun observedConversationAndAvailabilityProjectWithoutLabOverride() {
        val (repository, lab) =
            seededFixture()
        val relay =
            configuredRelay(
                conversation =
                    repository.state.value,
                lab =
                    lab.state.value,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
                companionAvailability =
                    VexConversationCompanionAvailability.Available,
            )

        val output =
            relay.currentOutput
        val surface =
            requireNotNull(
                output.surface,
            )

        assertSame(
            VexConversationHomeAvailability.Available,
            surface.homeAvailability,
        )
        assertSame(
            VexConversationCompanionAvailability.Available,
            requireNotNull(
                surface.participant(vexRef),
            ).companionAvailability,
        )
        assertSame(
            VexConversationInputProvenance.OBSERVED,
            output.homeAvailabilityProvenance,
        )
        assertSame(
            VexConversationInputProvenance.OBSERVED,
            output
                .companionAvailabilityProvenanceByParticipantRef[
                    vexRef
                ],
        )
        assertFalse(
            output.labOverridesActive,
        )
    }

    @Test
    fun labOverridesChangeEffectiveAvailabilityWithoutChangingRepositoryTruth() {
        val (repository, lab) =
            seededFixture()
        val before =
            repository.state.value

        lab.overrideHomeAvailability(
            VexConversationHomeAvailability.Unavailable,
        )
        lab.overrideCompanionAvailability(
            participantRef = vexRef,
            availability =
                VexConversationCompanionAvailability.Unavailable,
        )

        val relay =
            configuredRelay(
                conversation =
                    repository.state.value,
                lab =
                    lab.state.value,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
                companionAvailability =
                    VexConversationCompanionAvailability.Available,
            )

        val output =
            relay.currentOutput
        val surface =
            requireNotNull(
                output.surface,
            )

        assertEquals(
            before,
            repository.state.value,
        )
        assertSame(
            VexConversationHomeAvailability.Unavailable,
            surface.homeAvailability,
        )
        assertSame(
            VexConversationCompanionAvailability.Unavailable,
            requireNotNull(
                surface.participant(vexRef),
            ).companionAvailability,
        )
        assertSame(
            VexConversationInputProvenance.LAB_OVERRIDE,
            output.homeAvailabilityProvenance,
        )
        assertSame(
            VexConversationInputProvenance.LAB_OVERRIDE,
            output
                .companionAvailabilityProvenanceByParticipantRef[
                    vexRef
                ],
        )
        assertTrue(
            output.labOverridesActive,
        )
    }

    @Test
    fun clearingOverridesRevealsLatestObservedTruthRatherThanRestoringAnOldSnapshot() {
        val (repository, lab) =
            seededFixture()
        val relay =
            configuredRelay(
                conversation =
                    repository.state.value,
                lab =
                    lab.state.value,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
                companionAvailability =
                    VexConversationCompanionAvailability.Available,
            )

        lab.overrideHomeAvailability(
            VexConversationHomeAvailability.Unavailable,
        )
        lab.overrideCompanionAvailability(
            participantRef = vexRef,
            availability =
                VexConversationCompanionAvailability.Unavailable,
        )
        relay.labChanged(
            lab.state.value,
        )

        relay.homeAvailabilityObserved(
            VexConversationHomeAvailability.Unobserved,
        )
        relay.companionAvailabilityObserved(
            mapOf(
                vexRef to
                    VexConversationCompanionAvailability.Unknown,
            ),
        )

        var output =
            relay.currentOutput
        assertSame(
            VexConversationHomeAvailability.Unavailable,
            requireNotNull(
                output.surface,
            ).homeAvailability,
        )
        assertSame(
            VexConversationCompanionAvailability.Unavailable,
            requireNotNull(
                output.surface,
            )
                .participant(vexRef)
                ?.companionAvailability,
        )

        lab.clearAvailabilityOverrides()
        relay.labChanged(
            lab.state.value,
        )

        output =
            relay.currentOutput
        val restoredSurface =
            requireNotNull(
                output.surface,
            )

        assertSame(
            VexConversationHomeAvailability.Unobserved,
            restoredSurface.homeAvailability,
        )
        assertSame(
            VexConversationCompanionAvailability.Unknown,
            requireNotNull(
                restoredSurface.participant(vexRef),
            ).companionAvailability,
        )
        assertSame(
            VexConversationInputProvenance.OBSERVED,
            output.homeAvailabilityProvenance,
        )
        assertSame(
            VexConversationInputProvenance.OBSERVED,
            output
                .companionAvailabilityProvenanceByParticipantRef[
                    vexRef
                ],
        )
        assertFalse(
            output.labOverridesActive,
        )
    }

    @Test
    fun syntheticProviderMessageStillEntersRepositoryBeforeConvergence() {
        val (repository, lab) =
            seededFixture()
        val relay =
            configuredRelay(
                conversation =
                    repository.state.value,
                lab =
                    lab.state.value,
                homeAvailability =
                    VexConversationHomeAvailability.Available,
                companionAvailability =
                    VexConversationCompanionAvailability.Available,
            )

        lab.publishRemoteMessage(
            content = "provider-side test",
            speakerRef = mayaRef,
        )

        assertTrue(
            requireNotNull(
                relay.currentOutput.surface,
            ).thread.messages.isEmpty(),
        )

        relay.conversationChanged(
            repository.state.value,
        )

        val messages =
            requireNotNull(
                relay.currentOutput.surface,
            ).thread.messages

        assertEquals(
            1,
            messages.size,
        )
        assertEquals(
            "provider-side test",
            messages.single().content,
        )
        assertEquals(
            mayaRef,
            messages.single().speakerRef,
        )
    }

    private fun seededFixture():
        Pair<
            InMemoryVexFamilyConversationRepository,
            VexConversationLab
        > {
        val repository =
            InMemoryVexFamilyConversationRepository(
                admittedSpeakerRef =
                    victorRef,
            )
        val lab =
            VexConversationLab(
                repository,
            )
        lab.seedFamilyRoom()

        return repository to lab
    }

    private fun configuredRelay(
        conversation:
            VexConversationState,
        lab:
            VexConversationLabState,
        homeAvailability:
            VexConversationHomeAvailability,
        companionAvailability:
            VexConversationCompanionAvailability,
    ): VexConversationConvergenceRelay =
        VexConversationConvergenceRelay()
            .also { relay ->
                relay.selectThread(
                    VexConversationLabFixture.threadRef,
                )
                relay.authenticatedParticipantObserved(
                    victorRef,
                )
                relay.homeAvailabilityObserved(
                    homeAvailability,
                )
                relay.companionAvailabilityObserved(
                    mapOf(
                        vexRef to companionAvailability,
                    ),
                )
                relay.conversationChanged(
                    conversation,
                )
                relay.labChanged(
                    lab,
                )
            }
}

// [VXG RealForever]
