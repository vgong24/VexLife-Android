package com.vextreme.vex.android.architecture.relay

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.vextreme.vex.android.architecture.execution.VexExecutionName
import com.vextreme.vex.android.architecture.execution.VexExecutionScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class VexRelayFirstPrinciplesTest {
    private enum class ScreenName {
        NONE,
        EVENTS,
    }

    private data class ScreenVexState(
        val screen: ScreenName = ScreenName.NONE,
        val revision: Long = 0L,
    ) : VexState<ScreenVexState> {
        override fun deepCopy(): ScreenVexState =
            copy()
    }

    private class ScreenVexRelay :
        VexRelay<ScreenVexState>(
            initialState = ScreenVexState(),
        ) {
        fun screenChanged(screen: ScreenName) {
            update { current ->
                if (current.screen == screen) {
                    current
                } else {
                    current.copy(
                        screen = screen,
                        revision = current.revision + 1L,
                    )
                }
            }
        }
    }

    private data class SomeItem(
        val itemId: String,
        val label: String,
    )

    private data class ItemVexState(
        val sourceRevision: Long = 0L,
        val items: List<SomeItem> = emptyList(),
    ) : VexState<ItemVexState> {
        override fun deepCopy(): ItemVexState =
            copy(
                items = items.map { item ->
                    item.copy()
                },
            )
    }

    private class ItemVexRelay :
        VexRelay<ItemVexState>(
            initialState = ItemVexState(),
        ) {
        override fun processState(
            inputState: ItemVexState,
            currentState: ItemVexState,
        ): ItemVexState =
            if (inputState.sourceRevision <= currentState.sourceRevision) {
                currentState.deepCopy()
            } else {
                inputState.deepCopy()
            }

        fun itemsArrived(
            sourceRevision: Long,
            items: List<SomeItem>,
        ) {
            send(
                ItemVexState(
                    sourceRevision = sourceRevision,
                    items = items,
                ),
            )
        }
    }

    private data class InteractionVexState(
        val clickRevision: Long = 0L,
        val lastButtonId: String? = null,
    ) : VexState<InteractionVexState> {
        override fun deepCopy(): InteractionVexState =
            copy()
    }

    private class InteractionVexRelay :
        VexRelay<InteractionVexState>(
            initialState = InteractionVexState(),
        ) {
        fun buttonClicked(buttonId: String) {
            update { current ->
                current.copy(
                    clickRevision = current.clickRevision + 1L,
                    lastButtonId = buttonId,
                )
            }
        }
    }

    private data class EventScreenInputState(
        // Top-level null means "this incoming patch did not change this child state."
        val screen: ScreenVexState? = null,
        val items: ItemVexState? = null,
        val interaction: InteractionVexState? = null,
    ) : VexInputState<EventScreenInputState> {
        override fun deepCopy(): EventScreenInputState =
            copy(
                screen = screen?.deepCopy(),
                items = items?.deepCopy(),
                interaction = interaction?.deepCopy(),
            )
    }

    private enum class OutputAvailability {
        EMPTY,
        READY,
    }

    private data class EventScreenOutputState(
        val availability: OutputAvailability = OutputAvailability.EMPTY,
        val itemCount: Int = 0,
        val lastButtonId: String? = null,
    ) : VexOutputState<EventScreenOutputState> {
        override fun deepCopy(): EventScreenOutputState =
            copy()
    }

    /**
     * One organized complex-screen family.
     *
     * The child Relays own their own truth/admission rules.
     * This Compound Relay listens to their latest accepted state and reconciles one screen output.
     */
    private class EventScreenVexCompoundRelay(
        dependencyScope: VexExecutionScope,
        screenRelay: ScreenVexRelay,
        itemRelay: ItemVexRelay,
        interactionRelay: InteractionVexRelay,
    ) : VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
        initialInputState = EventScreenInputState(),
        initialOutputState = EventScreenOutputState(),
    ) {
        override fun processStates(
            inputState: EventScreenInputState,
            currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
        ): VexCompoundState<EventScreenInputState, EventScreenOutputState> {
            // Name the current input once; do not repeat currentState.inputState.* everywhere.
            val currentInputState = currentState.inputState

            // Incoming nullable fields are partial updates. Unspecified siblings stay current.
            val screen =
                inputState.screen
                    ?: currentInputState.screen
            val items =
                inputState.items
                    ?: currentInputState.items
            val interaction =
                inputState.interaction
                    ?: currentInputState.interaction

            val nextInputState =
                EventScreenInputState(
                    screen = screen?.deepCopy(),
                    items = items?.deepCopy(),
                    interaction = interaction?.deepCopy(),
                )

            // First emissions may arrive independently. Output stays explicitly EMPTY until the
            // screen's required Source-of-Truth child states have both been observed.
            val nextOutputState =
                if (screen == null || items == null) {
                    EventScreenOutputState(
                        availability = OutputAvailability.EMPTY,
                    )
                } else {
                    EventScreenOutputState(
                        availability = OutputAvailability.READY,
                        itemCount = items.items.size,
                        lastButtonId = interaction?.lastButtonId,
                    )
                }

            return currentState.copy(
                inputState = nextInputState,
                outputState = nextOutputState,
            )
        }

        init {
            observeDependencies(
                executionScope = dependencyScope,
                vexRelayDependency(
                    executionName = VexExecutionName("relay.event-screen.observe.screen"),
                    relay = screenRelay,
                ) { screenState ->
                    EventScreenInputState(
                        screen = screenState.deepCopy(),
                    )
                },
                vexRelayDependency(
                    executionName = VexExecutionName("relay.event-screen.observe.items"),
                    relay = itemRelay,
                ) { itemState ->
                    EventScreenInputState(
                        items = itemState.deepCopy(),
                    )
                },
                vexRelayDependency(
                    executionName = VexExecutionName("relay.event-screen.observe.interaction"),
                    relay = interactionRelay,
                ) { interactionState ->
                    EventScreenInputState(
                        interaction = interactionState.deepCopy(),
                    )
                },
            )
        }
    }

    @Test
    fun simpleRelayCanUseDefaultProcessingAndDeepCopyContract() {
        val mutableItems =
            mutableListOf(
                SomeItem(
                    itemId = "item.1",
                    label = "One",
                ),
            )
        val relay = ItemVexRelay()

        relay.itemsArrived(
            sourceRevision = 1L,
            items = mutableItems,
        )
        mutableItems +=
            SomeItem(
                itemId = "caller.mutation",
                label = "Should not leak",
            )

        assertEquals(
            listOf("item.1"),
            relay.current.items.map { item -> item.itemId },
        )

        val exposed = relay.current.items
        val reread = relay.current.items
        assertNotSame(exposed, reread)
    }

    @Test
    fun staleChildSourceRevisionIsRejectedBeforeCompoundComposition() {
        val relay = ItemVexRelay()

        relay.itemsArrived(
            sourceRevision = 20L,
            items = listOf(SomeItem("new", "New")),
        )
        relay.itemsArrived(
            sourceRevision = 10L,
            items = listOf(SomeItem("old", "Old")),
        )

        assertEquals(20L, relay.current.sourceRevision)
        assertEquals(listOf("new"), relay.current.items.map { it.itemId })
    }

    @Test
    fun compoundRelayMergesNullableInputPatchesAgainstCurrentInput() = runBlocking {
        val executionScope =
            VexExecutionScope.root(
                name = VexExecutionName("test.event-screen"),
            )
        val screenRelay = ScreenVexRelay()
        val itemRelay = ItemVexRelay()
        val interactionRelay = InteractionVexRelay()
        val compoundRelay =
            EventScreenVexCompoundRelay(
                dependencyScope = executionScope,
                screenRelay = screenRelay,
                itemRelay = itemRelay,
                interactionRelay = interactionRelay,
            )

        try {

            screenRelay.screenChanged(ScreenName.EVENTS)
            itemRelay.itemsArrived(
                sourceRevision = 1L,
                items =
                    listOf(
                        SomeItem("item.a", "A"),
                        SomeItem("item.b", "B"),
                    ),
            )
            interactionRelay.buttonClicked("button.more")

            val state =
                withTimeout(2_000L) {
                    compoundRelay.currentState.first { current ->
                        current.inputState.screen?.screen == ScreenName.EVENTS &&
                            current.inputState.items?.sourceRevision == 1L &&
                            current.inputState.interaction?.clickRevision == 1L
                    }
                }

            assertEquals(ScreenName.EVENTS, state.inputState.screen?.screen)
            assertEquals(2, state.inputState.items?.items?.size)
            assertEquals("button.more", state.inputState.interaction?.lastButtonId)
            assertEquals(OutputAvailability.READY, state.outputState.availability)
            assertEquals(2, state.outputState.itemCount)
            assertEquals("button.more", state.outputState.lastButtonId)
        } finally {
            executionScope.cancelAll("test complete")
        }
    }

    @Test
    fun concurrentChildRelaysDoNotOverwriteSiblingCompoundInput() = runBlocking {
        val executionScope =
            VexExecutionScope.root(
                name = VexExecutionName("test.event-screen"),
            )
        val screenRelay = ScreenVexRelay()
        val itemRelay = ItemVexRelay()
        val interactionRelay = InteractionVexRelay()
        val compoundRelay =
            EventScreenVexCompoundRelay(
                dependencyScope = executionScope,
                screenRelay = screenRelay,
                itemRelay = itemRelay,
                interactionRelay = interactionRelay,
            )

        try {

            val executor = Executors.newFixedThreadPool(3)
            val ready = CountDownLatch(3)
            val go = CountDownLatch(1)

            try {
                val futures =
                    listOf(
                        executor.submit {
                            ready.countDown()
                            go.await()
                            screenRelay.screenChanged(ScreenName.EVENTS)
                        },
                        executor.submit {
                            ready.countDown()
                            go.await()
                            itemRelay.itemsArrived(
                                sourceRevision = 7L,
                                items = listOf(SomeItem("event.concurrent", "Event")),
                            )
                        },
                        executor.submit {
                            ready.countDown()
                            go.await()
                            interactionRelay.buttonClicked("button.concurrent")
                        },
                    )

                assertTrue(
                    ready.await(
                        2L,
                        TimeUnit.SECONDS,
                    ),
                )
                go.countDown()
                futures.forEach { future ->
                    future.get(
                        2L,
                        TimeUnit.SECONDS,
                    )
                }
            } finally {
                executor.shutdownNow()
            }

            val state =
                withTimeout(2_000L) {
                    compoundRelay.currentState.first { current ->
                        current.inputState.screen?.screen == ScreenName.EVENTS &&
                            current.inputState.items?.sourceRevision == 7L &&
                            current.inputState.interaction?.clickRevision == 1L
                    }
                }

            assertEquals(ScreenName.EVENTS, state.inputState.screen?.screen)
            assertEquals(7L, state.inputState.items?.sourceRevision)
            assertEquals(
                listOf("event.concurrent"),
                state.inputState.items?.items?.map { it.itemId },
            )
            assertEquals(1L, state.inputState.interaction?.clickRevision)
            assertEquals(OutputAvailability.READY, state.outputState.availability)
        } finally {
            executionScope.cancelAll("test complete")
        }
    }
}

// [VXG RealForever]
