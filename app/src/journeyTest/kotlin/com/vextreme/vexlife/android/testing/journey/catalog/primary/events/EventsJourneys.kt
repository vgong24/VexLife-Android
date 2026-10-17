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
    private const val MediaRef =
        "event.journey.external.media.004"

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
            VexExternalEvent(
                eventRef = MediaRef,
                sourceRef = SourceRef,
                kind = VexExternalEventKind.MEDIA,
                title = "Journey media update",
                detail = "External media-like information arrived outside Events.",
                occurredAtEpochMillis = 4_000L,
            ),
        )

    private fun externalSnapshot(
        revision: Int,
        eventCount: Int = revision,
    ): VexExternalEventSourceSnapshot =
        VexExternalEventSourceSnapshot(
            sourceRef = SourceRef,
            sourceRevision = revision.toLong(),
            events = fixtureEvents.take(eventCount),
        )

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

            externalCause(
                repository =
                    getRepository(
                        Repositories.events,
                    ),
                causeRef =
                    "cause.events.external.snapshot-001",
            ) {
                accept(
                    externalSnapshot(1),
                )
            }
            tap(AppShell.Elements.Events)
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    MessageRef,
                    "acknowledge",
                ),
            )

            tap(AppShell.Elements.Home)
            externalCause(
                repository =
                    getRepository(
                        Repositories.events,
                    ),
                causeRef =
                    "cause.events.external.snapshot-002",
            ) {
                accept(
                    externalSnapshot(
                        revision = 2,
                        eventCount = 4,
                    ),
                )
            }
            tap(AppShell.Elements.Events)

            // LATER moves Call out of the current queue and into the next sequence.
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    CallRef,
                    "later",
                ),
            )

            // SUSPEND keeps Map active but puts it behind unsuspended current-queue work.
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    MapRef,
                    "suspend",
                ),
            )

            // MEDIA is now the remaining normal current-queue item.
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    MediaRef,
                    "suppress",
                ),
            )

            // Suspended Map returns before the deferred next-sequence Call.
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    MapRef,
                    "acknowledge",
                ),
            )

            // Current queue drained: deferred Call becomes the next-sequence candidate.
            tapElementById(
                EventsPresentationRefs.attentionAction(
                    CallRef,
                    "acknowledge",
                ),
            )
        }

    override val journeys =
        listOf(
            externalAttention,
        )
}

// [VXG RealForever]
