package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.events.EventsPresentationRefs
import com.vextreme.vexlife.android.shell.AppShell

object EventsJourneys : VexJourneyShard {
    override val shardId = "primary.events"

    private const val SourceRef =
        "source.journey.external-information"
    private const val MessageRef =
        "event.journey.external.message.001"
    private const val CallRef =
        "event.journey.external.call.002"
    private const val MapRef =
        "event.journey.external.map.003"

    private val fixtureEvents =
        listOf(
            VexExternalEvent(
                eventRef = MessageRef,
                sourceRef = SourceRef,
                kind = VexExternalEventKind.MESSAGE,
                title = "Journey message",
                detail = "External message-like information arrived while another Screen was current.",
                occurredAtEpochMillis = 1_000L,
            ),
            VexExternalEvent(
                eventRef = CallRef,
                sourceRef = SourceRef,
                kind = VexExternalEventKind.CALL,
                title = "Journey call",
                detail = "External call-like information arrived outside Events.",
                occurredAtEpochMillis = 2_000L,
            ),
            VexExternalEvent(
                eventRef = MapRef,
                sourceRef = SourceRef,
                kind = VexExternalEventKind.MAP,
                title = "Journey map update",
                detail = "External map-like information arrived outside Events.",
                occurredAtEpochMillis = 3_000L,
            ),
        )

    private fun externalSnapshotSignal(
        revision: Int,
    ): VexJourneySignal =
        journeySignal(
            signalRef =
                "signal.events.external.snapshot-" +
                    revision.toString().padStart(3, '0'),
        ) { repositories ->
            repositories.events.accept(
                VexExternalEventSourceSnapshot(
                    sourceRef = SourceRef,
                    sourceRevision = revision.toLong(),
                    events = fixtureEvents.take(revision),
                ),
            )
        }

    val externalAttention =
        vexJourney(
            id = "external-attention",
            title = "Events external information attention lattice",
            environments =
                listOf(
                    VexJourneyEnvironments.Baseline,
                    VexJourneyEnvironments.LargeText,
                    VexJourneyEnvironments.AlternateLocaleFallback,
                ),
        ) {
            start(HomeScreen)

            signal(externalSnapshotSignal(1))
            tap(AppShell.Elements.Events)
            tapDynamic(
                dynamicElement(
                    EventsPresentationRefs.attentionAction(
                        MessageRef,
                        "acknowledge",
                    ),
                ),
            )

            tap(AppShell.Elements.Home)
            signal(externalSnapshotSignal(2))
            tap(AppShell.Elements.Events)
            tapDynamic(
                dynamicElement(
                    EventsPresentationRefs.attentionAction(
                        CallRef,
                        "later",
                    ),
                ),
            )

            tap(AppShell.Elements.Home)
            signal(externalSnapshotSignal(3))
            tap(AppShell.Elements.Events)
            tapDynamic(
                dynamicElement(
                    EventsPresentationRefs.attentionAction(
                        MapRef,
                        "suppress",
                    ),
                ),
            )
        }

    override val journeys =
        listOf(
            externalAttention,
        )
}

// [VXG RealForever]
