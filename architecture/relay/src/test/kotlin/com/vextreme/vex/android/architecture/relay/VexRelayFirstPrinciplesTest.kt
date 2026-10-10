package com.vextreme.vex.android.architecture.relay

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexRelayFirstPrinciplesTest {
    private class ScreenRelay :
        VexRelay<ScreenRelay.InputState, ScreenRelay.OutputState>(
            initialInputState = InputState(),
            inputSnapshotter = VexStateSnapshotter { state -> state.copy() },
            outputSnapshotter = VexStateSnapshotter { state -> state.copy() },
        ) {
        data class InputState(
            val presence: Presence = Presence.EXITED,
        ) : VexInputState

        data class OutputState(
            val presence: Presence,
        ) : VexOutputState

        enum class Presence {
            ENTERED,
            EXITED,
        }

        override fun processLatestState(
            inputState: InputState,
        ): OutputState =
            OutputState(
                presence = inputState.presence,
            )

        fun screenEntered() {
            send { input ->
                input.copy(presence = Presence.ENTERED)
            }
        }

        fun screenExited() {
            send { input ->
                input.copy(presence = Presence.EXITED)
            }
        }
    }

    @Test
    fun screenEnterExitIsTheSmallestRelayFamily() {
        val relay = ScreenRelay()

        assertEquals(ScreenRelay.Presence.EXITED, relay.current.outputState.presence)
        assertEquals(0L, relay.current.inputRevision)
        assertEquals(0L, relay.current.outputRevision)

        relay.screenEntered()
        assertEquals(ScreenRelay.Presence.ENTERED, relay.current.outputState.presence)
        assertEquals(1L, relay.current.inputRevision)
        assertEquals(1L, relay.current.outputRevision)

        relay.screenExited()
        assertEquals(ScreenRelay.Presence.EXITED, relay.current.outputState.presence)
        assertEquals(2L, relay.current.inputRevision)
        assertEquals(2L, relay.current.outputRevision)
    }

    @Test
    fun sendingTheSameSubstateDoesNotInventAnotherRevision() {
        val relay = ScreenRelay()

        relay.screenExited()

        assertEquals(0L, relay.current.inputRevision)
        assertEquals(0L, relay.current.outputRevision)
    }

    /**
     * One scoped screen/component Relay family.
     *
     * It owns several nested input substates because they participate in one Event Tooltip
     * relationship. The base Relay never needs to know what those substates mean.
     */
    private class EventTooltipRelay :
        VexRelay<EventTooltipRelay.InputState, EventTooltipRelay.OutputState>(
            initialInputState = InputState(),
            inputSnapshotter = VexStateSnapshotter { state -> state.deepCopy() },
            outputSnapshotter = VexStateSnapshotter { state -> state.deepCopy() },
        ) {
        enum class ScreenPresence {
            ENTERED,
            EXITED,
        }

        data class ScreenState(
            val presence: ScreenPresence = ScreenPresence.EXITED,
        )

        data class EventState(
            val sourceRevision: Long = 0L,
            val eventIds: MutableList<String> = mutableListOf(),
        ) {
            fun deepCopy(): EventState =
                copy(
                    eventIds = eventIds.toMutableList(),
                )
        }

        data class InteractionState(
            val clickRevision: Long = 0L,
            val lastButtonId: String? = null,
        )

        data class TargetState(
            val available: Boolean = false,
        )

        data class InputState(
            val screen: ScreenState = ScreenState(),
            val events: EventState = EventState(),
            val interaction: InteractionState = InteractionState(),
            val target: TargetState = TargetState(),
        ) : VexInputState {
            fun deepCopy(): InputState =
                copy(
                    screen = screen.copy(),
                    events = events.deepCopy(),
                    interaction = interaction.copy(),
                    target = target.copy(),
                )
        }

        data class TooltipOutput(
            val visible: Boolean,
            val eventId: String?,
        )

        data class SnackbarOutput(
            val visible: Boolean,
            val buttonId: String?,
            val interactionRevision: Long,
        )

        data class OutputState(
            val tooltip: TooltipOutput,
            val snackbar: SnackbarOutput,
        ) : VexOutputState {
            fun deepCopy(): OutputState =
                copy(
                    tooltip = tooltip.copy(),
                    snackbar = snackbar.copy(),
                )
        }

        override fun processLatestState(
            inputState: InputState,
        ): OutputState {
            val currentEvent = inputState.events.eventIds.lastOrNull()
            return OutputState(
                tooltip = TooltipOutput(
                    visible =
                        inputState.screen.presence == ScreenPresence.ENTERED &&
                            inputState.target.available &&
                            currentEvent != null,
                    eventId = currentEvent,
                ),
                snackbar = SnackbarOutput(
                    visible = inputState.interaction.clickRevision > 0L,
                    buttonId = inputState.interaction.lastButtonId,
                    interactionRevision = inputState.interaction.clickRevision,
                ),
            )
        }

        fun screenEntered() {
            send { input ->
                input.copy(
                    screen = input.screen.copy(
                        presence = ScreenPresence.ENTERED,
                    ),
                )
            }
        }

        fun screenExited() {
            send { input ->
                input.copy(
                    screen = input.screen.copy(
                        presence = ScreenPresence.EXITED,
                    ),
                )
            }
        }

        fun targetAvailabilityChanged(
            available: Boolean,
        ) {
            send { input ->
                input.copy(
                    target = input.target.copy(
                        available = available,
                    ),
                )
            }
        }

        /**
         * External event ordering is semantic, not mechanical.
         *
         * The Relay serializes arrival; this nested state decides whether the arriving producer
         * revision is actually newer than the event truth already admitted.
         */
        fun eventArrived(
            sourceRevision: Long,
            eventId: String,
        ) {
            send { input ->
                if (sourceRevision <= input.events.sourceRevision) {
                    input
                } else {
                    input.copy(
                        events = input.events.copy(
                            sourceRevision = sourceRevision,
                            eventIds =
                                (input.events.eventIds + eventId).toMutableList(),
                        ),
                    )
                }
            }
        }

        /**
         * Repeating the same button still means a new interaction, so interactionRevision changes.
         */
        fun buttonClicked(
            buttonId: String,
        ) {
            send { input ->
                input.copy(
                    interaction = input.interaction.copy(
                        clickRevision = input.interaction.clickRevision + 1L,
                        lastButtonId = buttonId,
                    ),
                )
            }
        }
    }

    @Test
    fun unrelatedSubstateUpdateDoesNotOverwriteItsSiblings() {
        val relay = EventTooltipRelay()

        relay.targetAvailabilityChanged(true)
        relay.eventArrived(
            sourceRevision = 1L,
            eventId = "event.1",
        )
        relay.screenEntered()

        relay.buttonClicked("button.more")

        val input = relay.current.inputState
        assertEquals(EventTooltipRelay.ScreenPresence.ENTERED, input.screen.presence)
        assertEquals(true, input.target.available)
        assertEquals(listOf("event.1"), input.events.eventIds)
        assertEquals(1L, input.interaction.clickRevision)
        assertEquals("button.more", input.interaction.lastButtonId)
    }

    @Test
    fun concurrentScreenEventAndButtonChangesSerializeWithoutLostUpdates() {
        val relay = EventTooltipRelay()
        relay.targetAvailabilityChanged(true)

        val executor = Executors.newFixedThreadPool(3)
        val ready = CountDownLatch(3)
        val go = CountDownLatch(1)

        try {
            val futures = listOf(
                executor.submit {
                    ready.countDown()
                    go.await()
                    relay.screenEntered()
                },
                executor.submit {
                    ready.countDown()
                    go.await()
                    relay.eventArrived(
                        sourceRevision = 7L,
                        eventId = "event.concurrent",
                    )
                },
                executor.submit {
                    ready.countDown()
                    go.await()
                    relay.buttonClicked("button.concurrent")
                },
            )

            assertTrue(ready.await(2L, TimeUnit.SECONDS))
            go.countDown()
            futures.forEach { future ->
                future.get(2L, TimeUnit.SECONDS)
            }
        } finally {
            executor.shutdownNow()
        }

        val state = relay.current
        assertEquals(
            EventTooltipRelay.ScreenPresence.ENTERED,
            state.inputState.screen.presence,
        )
        assertEquals(7L, state.inputState.events.sourceRevision)
        assertEquals(
            listOf("event.concurrent"),
            state.inputState.events.eventIds,
        )
        assertEquals(1L, state.inputState.interaction.clickRevision)
        assertTrue(state.outputState.tooltip.visible)
        assertTrue(state.outputState.snackbar.visible)
    }

    @Test
    fun olderExternalEventCannotOverwriteNewerNestedEventState() {
        val relay = EventTooltipRelay()

        relay.eventArrived(
            sourceRevision = 20L,
            eventId = "event.new",
        )
        val afterNew = relay.current

        relay.eventArrived(
            sourceRevision = 10L,
            eventId = "event.old",
        )
        val afterOld = relay.current

        assertEquals(afterNew.inputRevision, afterOld.inputRevision)
        assertEquals(20L, afterOld.inputState.events.sourceRevision)
        assertEquals(listOf("event.new"), afterOld.inputState.events.eventIds)
    }

    @Test
    fun repeatedSameButtonClickRemainsARealInputChange() {
        val relay = EventTooltipRelay()

        relay.buttonClicked("button.same")
        relay.buttonClicked("button.same")

        assertEquals(2L, relay.current.inputState.interaction.clickRevision)
        assertEquals(2L, relay.current.outputState.snackbar.interactionRevision)
    }

    @Test
    fun inputCanChangeWithoutPretendingOutputMeaningChanged() {
        val relay = EventTooltipRelay()

        relay.targetAvailabilityChanged(true)

        assertEquals(1L, relay.current.inputRevision)
        assertEquals(0L, relay.current.outputRevision)
        assertFalse(relay.current.outputState.tooltip.visible)
    }

    @Test
    fun exposedNestedMutableValuesCannotRewriteRetainedState() {
        val relay = EventTooltipRelay()
        relay.eventArrived(
            sourceRevision = 1L,
            eventId = "event.safe",
        )

        val exposed = relay.current.inputState.events.eventIds
        exposed += "consumer.mutation"

        assertEquals(
            listOf("event.safe"),
            relay.current.inputState.events.eventIds,
        )
    }
}

// [VXG RealForever]
