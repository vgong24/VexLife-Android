package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexAppScope
import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexCoroutineRef
import com.vextreme.vex.android.architecture.execution.VexCoroutineStatus
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import com.vextreme.vex.android.architecture.observability.VexImmediateLogExecutor
import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogFilter
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import com.vextreme.vex.android.architecture.observability.VexLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class VexRelayFirstPrinciplesTest {
    @Test
    fun relayRefRejectsUnboundedIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexRelayRef(
                "r".repeat(257),
            )
        }
    }

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

    private class ScreenVexRelay(
        relayRef: VexRelayRef = VexRelayRef("relay.test.screen"),
        logSink: VexLogSink = VexLogSink.None,
    ) : VexRelay<ScreenVexState>(
        relayRef = relayRef,
        initialState = ScreenVexState(),
        logSink = logSink,
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

        fun invalidRecursiveUpdate() {
            update { current ->
                screenChanged(ScreenName.EVENTS)

                current.copy(
                    screen = ScreenName.EVENTS,
                    revision = current.revision + 1L,
                )
            }
        }
    }

    private class ExplicitMeaningState(
        val value: Int,
        val nonSemanticDetail: String,
    ) : VexState<ExplicitMeaningState> {
        override fun deepCopy(): ExplicitMeaningState =
            ExplicitMeaningState(
                value = value,
                nonSemanticDetail = nonSemanticDetail,
            )

        override fun hasSameMeaningAs(other: ExplicitMeaningState): Boolean =
            value == other.value
    }

    private class ExplicitMeaningRelay(
        logSink: VexLogSink,
    ) : VexRelay<ExplicitMeaningState>(
        relayRef = VexRelayRef("relay.test.explicit-meaning"),
        initialState =
            ExplicitMeaningState(
                value = 1,
                nonSemanticDetail = "retained",
            ),
        logSink = logSink,
    ) {
        fun setValue(
            value: Int,
            nonSemanticDetail: String,
        ): ExplicitMeaningState =
            send(
                ExplicitMeaningState(
                    value = value,
                    nonSemanticDetail = nonSemanticDetail,
                ),
            )
    }

    private class InvalidNarrowMeaningState(
        val value: Int,
    ) : VexState<InvalidNarrowMeaningState> {
        override fun deepCopy(): InvalidNarrowMeaningState =
            InvalidNarrowMeaningState(value)

        override fun hasSameMeaningAs(other: InvalidNarrowMeaningState): Boolean =
            value == other.value

        override fun equals(other: Any?): Boolean =
            other is InvalidNarrowMeaningState

        override fun hashCode(): Int =
            0
    }

    private class InvalidNarrowMeaningRelay :
        VexRelay<InvalidNarrowMeaningState>(
            relayRef = VexRelayRef("relay.test.invalid-meaning"),
            initialState = InvalidNarrowMeaningState(1),
        ) {
        fun setValue(value: Int) {
            send(
                InvalidNarrowMeaningState(value),
            )
        }
    }

    private data class SomeItem(
        val itemId: String,
        val label: String,
        val tags: MutableList<String> = mutableListOf(),
    ) : VexDeepCopyable<SomeItem> {
        override fun deepCopy(): SomeItem =
            copy(
                tags = tags.toMutableList(),
            )
    }

    private data class ItemVexState(
        val sourceRevision: Long = 0L,
        val items: List<SomeItem> = emptyList(),
    ) : VexState<ItemVexState> {
        override fun deepCopy(): ItemVexState =
            copy(
                items = items.map { item ->
                    item.deepCopy()
                },
            )
    }

    private class ItemVexRelay :
        VexRelay<ItemVexState>(
            relayRef = VexRelayRef("relay.test.items"),
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
            relayRef = VexRelayRef("relay.test.interaction"),
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

    private class EventScreenVexCompoundRelay(
        private val screenRelay: ScreenVexRelay,
        private val itemRelay: ItemVexRelay,
        private val interactionRelay: InteractionVexRelay,
        logSink: VexLogSink = VexLogSink.None,
    ) : VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
        relayRef = VexRelayRef("relay.test.event-screen"),
        initialInputState = EventScreenInputState(),
        initialOutputState = EventScreenOutputState(),
        logSink = logSink,
    ) {
        override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
            listOf(
                vexRelayDependency(
                    coroutineRef = SCREEN_OBSERVER,
                    relay = screenRelay,
                ) { screenState ->
                    EventScreenInputState(
                        screen = screenState.deepCopy(),
                    )
                },
                vexRelayDependency(
                    coroutineRef = ITEM_OBSERVER,
                    relay = itemRelay,
                ) { itemState ->
                    EventScreenInputState(
                        items = itemState.deepCopy(),
                    )
                },
                vexRelayDependency(
                    coroutineRef = INTERACTION_OBSERVER,
                    relay = interactionRelay,
                ) { interactionState ->
                    EventScreenInputState(
                        interaction = interactionState.deepCopy(),
                    )
                },
            )

        override fun processStates(
            inputState: EventScreenInputState,
            currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
        ): VexCompoundState<EventScreenInputState, EventScreenOutputState> {
            val currentInputState =
                currentState.inputState

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

        companion object {
            val SCREEN_OBSERVER =
                VexCoroutineRef("relay.event-screen.observe.screen")
            val ITEM_OBSERVER =
                VexCoroutineRef("relay.event-screen.observe.items")
            val INTERACTION_OBSERVER =
                VexCoroutineRef("relay.event-screen.observe.interaction")
        }
    }


    @Test
    fun relayEmitsBoundedOwnerLogsWithoutDumpingState() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )
        val relay =
            ScreenVexRelay(
                logSink = logger,
            )

        relay.screenChanged(ScreenName.EVENTS)

        val records =
            logger.snapshot(
                VexLogFilter(
                    sourcePrefix = "relay.test.screen",
                    eventPrefix = "relay.",
                ),
            )

        assertEquals(
            listOf(
                "relay.input.received",
                "relay.state.changed",
            ),
            records.map {
                it.event.eventRef.value
            },
        )
        assertEquals(
            listOf("1", "1"),
            records.map {
                it.event.fields["transitionSequence"]
            },
        )
    }

    @Test
    fun relayLoggingFailureCannotChangeCommittedState() {
        val throwingSink =
            object : VexLogSink {
                override fun log(event: VexLogEvent) {
                    error("diagnostic sink unavailable")
                }
            }
        val relay =
            ScreenVexRelay(
                logSink = throwingSink,
            )

        relay.screenChanged(ScreenName.EVENTS)

        assertEquals(
            ScreenName.EVENTS,
            relay.current.screen,
        )
        assertEquals(
            1L,
            relay.current.revision,
        )
    }

    @Test
    fun relayUsesExplicitSemanticEqualityInsteadOfObjectIdentity() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )
        val relay =
            ExplicitMeaningRelay(
                logSink = logger,
            )

        val returned =
            relay.setValue(
                value = 1,
                nonSemanticDetail = "incoming-but-equivalent",
            )

        assertEquals(
            "retained",
            returned.nonSemanticDetail,
        )
        assertEquals(
            "retained",
            relay.current.nonSemanticDetail,
        )

        val records =
            logger.snapshot(
                VexLogFilter(
                    sourcePrefix = "relay.test.explicit-meaning",
                    eventPrefix = "relay.state.",
                ),
            )

        assertEquals(
            listOf("relay.state.unchanged"),
            records.map { record ->
                record.event.eventRef.value
            },
        )
    }

    @Test
    fun relayRejectsSemanticEqualityNarrowerThanStateFlowEquality() {
        val relay =
            InvalidNarrowMeaningRelay()

        assertFailsWith<IllegalStateException> {
            relay.setValue(2)
        }

        assertEquals(
            1,
            relay.current.value,
        )
    }

    @Test
    fun recursiveUpdateTransformFailsBeforeAnyInnerMutationCommits() {
        val relay =
            ScreenVexRelay()
        val before =
            relay.current

        assertFailsWith<IllegalStateException> {
            relay.invalidRecursiveUpdate()
        }

        assertEquals(
            before,
            relay.current,
        )
    }

    @Test
    fun simpleRelayCanUseDefaultProcessingAndTransitiveDeepCopyContract() {
        val mutableTags =
            mutableListOf("original")

        val mutableItems =
            mutableListOf(
                SomeItem(
                    itemId = "item.1",
                    label = "One",
                    tags = mutableTags,
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
        mutableTags += "nested mutation"

        assertEquals(
            listOf("item.1"),
            relay.current.items.map { item -> item.itemId },
        )
        assertEquals(
            listOf("original"),
            relay.current.items.single().tags,
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
    fun compoundConstructionIsInertUntilExplicitActivation() = runBlocking {
        val appScope = VexAppScope()
        val compoundRelay =
            EventScreenVexCompoundRelay(
                screenRelay = ScreenVexRelay(),
                itemRelay = ItemVexRelay(),
                interactionRelay = InteractionVexRelay(),
            )

        try {
            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
            assertTrue(appScope.executions.value.isEmpty())

            compoundRelay.activate(appScope)

            assertEquals(
                VexCompoundActivationState.ACTIVE,
                compoundRelay.activationState.value,
            )
            assertEquals(
                setOf(
                    EventScreenVexCompoundRelay.SCREEN_OBSERVER,
                    EventScreenVexCompoundRelay.ITEM_OBSERVER,
                    EventScreenVexCompoundRelay.INTERACTION_OBSERVER,
                ),
                appScope.executions.value.keys,
            )

            compoundRelay.deactivate()
            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun compoundRelayMergesNullableInputPatchesAgainstCurrentInput() = runBlocking {
        val appScope = VexAppScope()
        val screenRelay = ScreenVexRelay()
        val itemRelay = ItemVexRelay()
        val interactionRelay = InteractionVexRelay()
        val compoundRelay =
            EventScreenVexCompoundRelay(
                screenRelay = screenRelay,
                itemRelay = itemRelay,
                interactionRelay = interactionRelay,
            )

        try {
            compoundRelay.activate(appScope)

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

            compoundRelay.deactivate()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun concurrentChildRelaysDoNotOverwriteSiblingCompoundInput() = runBlocking {
        val appScope = VexAppScope()
        val screenRelay = ScreenVexRelay()
        val itemRelay = ItemVexRelay()
        val interactionRelay = InteractionVexRelay()
        val compoundRelay =
            EventScreenVexCompoundRelay(
                screenRelay = screenRelay,
                itemRelay = itemRelay,
                interactionRelay = interactionRelay,
            )

        try {
            compoundRelay.activate(appScope)

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

            compoundRelay.deactivate()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun parentCompoundRequiresActiveCompoundChild() = runBlocking {
        val appScope = VexAppScope()
        val child =
            EventScreenVexCompoundRelay(
                screenRelay = ScreenVexRelay(),
                itemRelay = ItemVexRelay(),
                interactionRelay = InteractionVexRelay(),
            )

        val parent =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.parent"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.parent.observe.child"),
                            relay = child,
                        ) {
                            EventScreenInputState()
                        },
                    )

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        try {
            assertFailsWith<IllegalStateException> {
                parent.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                parent.activationState.value,
            )
            assertTrue(
                appScope.executions.value.isEmpty(),
            )

            child.activate(appScope)
            parent.activate(appScope)

            assertEquals(
                VexCompoundActivationState.ACTIVE,
                child.activationState.value,
            )
            assertEquals(
                VexCompoundActivationState.ACTIVE,
                parent.activationState.value,
            )

            assertFailsWith<IllegalStateException> {
                child.deactivate()
            }

            assertEquals(
                VexCompoundActivationState.ACTIVE,
                child.activationState.value,
            )

            parent.deactivate()
            child.deactivate()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun failedParentActivationReleasesCompoundChildLease() = runBlocking {
        val appScope = VexAppScope()
        val child =
            EventScreenVexCompoundRelay(
                screenRelay = ScreenVexRelay(),
                itemRelay = ItemVexRelay(),
                interactionRelay = InteractionVexRelay(),
            )
        val conflictingRef =
            VexCoroutineRef("relay.test.parent.rollback.conflict")
        val conflictStarted =
            CompletableDeferred<Unit>()

        try {
            child.activate(appScope)

            val conflict =
                appScope.launchCoroutine(
                    coroutineRef = conflictingRef,
                ) {
                    conflictStarted.complete(Unit)
                    awaitCancellation()
                }

            withTimeout(2_000L) {
                conflictStarted.await()
            }

            val parent =
                object :
                    VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                        relayRef = VexRelayRef("relay.test.parent.rollback"),
                        initialInputState = EventScreenInputState(),
                        initialOutputState = EventScreenOutputState(),
                    ) {
                    override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                        listOf(
                            vexRelayDependency(
                                coroutineRef =
                                    VexCoroutineRef("relay.test.parent.rollback.observe.child"),
                                relay = child,
                            ) {
                                EventScreenInputState()
                            },
                            vexRelayDependency(
                                coroutineRef = conflictingRef,
                                relay =
                                    ScreenVexRelay(
                                        relayRef =
                                            VexRelayRef(
                                                "relay.test.parent.rollback.screen",
                                            ),
                                    ),
                            ) { state ->
                                EventScreenInputState(
                                    screen = state,
                                )
                            },
                        )

                    override fun processStates(
                        inputState: EventScreenInputState,
                        currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                    ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                        currentState
                }

            assertFailsWith<IllegalStateException> {
                parent.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                parent.activationState.value,
            )

            conflict.forceStop("release conflict")
            conflict.join()

            child.deactivate()

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                child.activationState.value,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun circularCompoundDependencyFailsBeforeAnyObserverStarts() = runBlocking {
        val appScope = VexAppScope()

        lateinit var relayA: VexCompoundRelay<EventScreenInputState, EventScreenOutputState>
        lateinit var relayB: VexCompoundRelay<EventScreenInputState, EventScreenOutputState>

        relayA =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.cycle.a"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.cycle.a.observe.b"),
                            relay = relayB,
                        ) {
                            EventScreenInputState()
                        },
                    )

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        relayB =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.cycle.b"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.cycle.b.observe.a"),
                            relay = relayA,
                        ) {
                            EventScreenInputState()
                        },
                    )

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        try {
            assertFailsWith<IllegalStateException> {
                relayA.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                relayA.activationState.value,
            )
            assertTrue(
                appScope.executions.value.isEmpty(),
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun nestedObserverRefConflictFailsBeforeParentObserverStarts() = runBlocking {
        val appScope = VexAppScope()
        val child =
            EventScreenVexCompoundRelay(
                screenRelay = ScreenVexRelay(),
                itemRelay = ItemVexRelay(),
                interactionRelay = InteractionVexRelay(),
            )

        try {
            child.activate(appScope)

            val childExecutionRefs =
                appScope.executions.value.keys.toSet()

            val parent =
                object :
                    VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                        relayRef = VexRelayRef("relay.test.nested-ref-conflict.parent"),
                        initialInputState = EventScreenInputState(),
                        initialOutputState = EventScreenOutputState(),
                    ) {
                    override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                        listOf(
                            vexRelayDependency(
                                coroutineRef = EventScreenVexCompoundRelay.SCREEN_OBSERVER,
                                relay = child,
                            ) {
                                EventScreenInputState()
                            },
                        )

                    override fun processStates(
                        inputState: EventScreenInputState,
                        currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                    ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                        currentState
                }

            assertFailsWith<IllegalStateException> {
                parent.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                parent.activationState.value,
            )
            assertEquals(
                childExecutionRefs,
                appScope.executions.value.keys.toSet(),
            )

            child.deactivate()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun duplicateRelayRefsFailBeforeAnyObserverStarts() = runBlocking {
        val appScope = VexAppScope()
        val duplicateRef =
            VexRelayRef("relay.test.duplicate-source")

        val compoundRelay =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.duplicate-source.parent"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.duplicate-source.one"),
                            relay = ScreenVexRelay(relayRef = duplicateRef),
                        ) { state ->
                            EventScreenInputState(
                                screen = state,
                            )
                        },
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.duplicate-source.two"),
                            relay = ScreenVexRelay(relayRef = duplicateRef),
                        ) { state ->
                            EventScreenInputState(
                                screen = state,
                            )
                        },
                    )

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        try {
            assertFailsWith<IllegalStateException> {
                compoundRelay.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
            assertTrue(
                appScope.executions.value.isEmpty(),
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun duplicateCompoundObserverRefsFailBeforeAnyObserverStarts() = runBlocking {
        val appScope = VexAppScope()
        val screenRelay = ScreenVexRelay()
        val itemRelay = ItemVexRelay()

        val compoundRelay =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.duplicate-observer-ref"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.duplicate"),
                            relay = screenRelay,
                        ) { state ->
                            EventScreenInputState(
                                screen = state,
                            )
                        },
                        vexRelayDependency(
                            coroutineRef = VexCoroutineRef("relay.test.duplicate"),
                            relay = itemRelay,
                        ) { state ->
                            EventScreenInputState(
                                items = state,
                            )
                        },
                    )

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        try {
            assertFailsWith<IllegalStateException> {
                compoundRelay.activate(appScope)
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
            assertTrue(
                appScope.executions.value.isEmpty(),
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun callerCancellationCannotStrandCompoundDeactivation() = runBlocking {
        val appScope = VexAppScope()
        val cleanupStarted = CompletableDeferred<Unit>()
        val allowCleanup = CompletableDeferred<Unit>()
        val observerRef =
            VexCoroutineRef("relay.test.slow-cleanup")
        val slowSourceRelay =
            ScreenVexRelay()

        val slowDependency =
            VexRelayDependency<EventScreenInputState>(
                coroutineRef = observerRef,
                sourceRelay = slowSourceRelay,
            ) { executionOwner, activationGate, _ ->
                executionOwner.launchCoroutine(
                    coroutineRef = observerRef,
                ) {
                    activationGate.await()

                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            cleanupStarted.complete(Unit)
                            allowCleanup.await()
                        }
                    }
                }
            }

        val compoundRelay =
            object :
                VexCompoundRelay<EventScreenInputState, EventScreenOutputState>(
                    relayRef = VexRelayRef("relay.test.deactivation"),
                    initialInputState = EventScreenInputState(),
                    initialOutputState = EventScreenOutputState(),
                ) {
                override fun relayDependencies(): List<VexRelayDependency<EventScreenInputState>> =
                    listOf(slowDependency)

                override fun processStates(
                    inputState: EventScreenInputState,
                    currentState: VexCompoundState<EventScreenInputState, EventScreenOutputState>,
                ): VexCompoundState<EventScreenInputState, EventScreenOutputState> =
                    currentState
            }

        try {
            compoundRelay.activate(appScope)

            val deactivation =
                launch {
                    compoundRelay.deactivate("test cancellation")
                }

            withTimeout(2_000L) {
                cleanupStarted.await()
            }

            deactivation.cancel()
            allowCleanup.complete(Unit)

            withTimeout(2_000L) {
                deactivation.join()
            }

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun partialDependencyActivationRollsBackAndCanRetry() = runBlocking {
        val appScope = VexAppScope()
        val conflictStarted = CompletableDeferred<Unit>()
        val conflict =
            appScope.launchCoroutine(
                coroutineRef = EventScreenVexCompoundRelay.ITEM_OBSERVER,
            ) {
                conflictStarted.complete(Unit)
                awaitCancellation()
            }

        val compoundRelay =
            EventScreenVexCompoundRelay(
                screenRelay = ScreenVexRelay(),
                itemRelay = ItemVexRelay(),
                interactionRelay = InteractionVexRelay(),
            )
        val initialState =
            compoundRelay.current
        val observedStates =
            mutableListOf<VexCompoundState<EventScreenInputState, EventScreenOutputState>>()
        val observer =
            launch {
                compoundRelay.currentState.collect { state ->
                    observedStates +=
                        state.deepCopy()
                }
            }

        try {
            yield()

            withTimeout(2_000L) {
                conflictStarted.await()
            }

            assertFailsWith<IllegalStateException> {
                compoundRelay.activate(appScope)
            }

            yield()

            assertEquals(
                listOf(initialState),
                observedStates,
            )

            assertEquals(
                VexCompoundActivationState.INACTIVE,
                compoundRelay.activationState.value,
            )
            assertEquals(
                EventScreenInputState(),
                compoundRelay.current.inputState,
            )
            assertEquals(
                EventScreenOutputState(),
                compoundRelay.current.outputState,
            )

            withTimeout(2_000L) {
                appScope.executions.first { states ->
                    states[EventScreenVexCompoundRelay.SCREEN_OBSERVER]
                        ?.status == VexCoroutineStatus.CANCELLED
                }
            }

            conflict.forceStop("release activation conflict")
            conflict.join()

            compoundRelay.activate(appScope)

            assertEquals(
                VexCompoundActivationState.ACTIVE,
                compoundRelay.activationState.value,
            )

            compoundRelay.deactivate()
        } finally {
            observer.cancelAndJoin()
            appScope.stopApplication("test complete")
        }
    }
}

// [VXG RealForever]
