# Relay examples

## Small Source-of-Truth Relay

~~~kotlin
data class ScreenVexState(
    val screen: ScreenName,
) : VexState<ScreenVexState> {
    override fun deepCopy() = copy()
}

class ScreenVexRelay(
    logSink: VexLogSink,
) : VexRelay<ScreenVexState>(
    relayRef = VexRelayRef("relay.screen"),
    initialState = ScreenVexState(ScreenName.NONE),
    logSink = logSink,
) {
    fun screenChanged(screen: ScreenName) {
        update {
            it.copy(screen = screen)
        }
    }
}
~~~

## Compound relationship

~~~kotlin
data class EventInputState(
    val screen: ScreenVexState? = null,
    val items: ItemVexState? = null,
) : VexInputState<EventInputState> {
    override fun deepCopy() = copy(
        screen = screen?.deepCopy(),
        items = items?.deepCopy(),
    )
}

class EventCompoundRelay(
    private val screenRelay: ScreenVexRelay,
    private val itemRelay: ItemVexRelay,
    logSink: VexLogSink,
) : VexCompoundRelay<EventInputState, EventOutputState>(
    relayRef = VexRelayRef("relay.screen.events"),
    initialInputState = EventInputState(),
    initialOutputState = EventOutputState(),
    logSink = logSink,
) {
    override fun relayDependencies() = listOf(
        vexRelayDependency(
            coroutineRef = VexCoroutineRef("relay.screen.events.observe.screen"),
            relay = screenRelay,
        ) { EventInputState(screen = it) },
        vexRelayDependency(
            coroutineRef = VexCoroutineRef("relay.screen.events.observe.items"),
            relay = itemRelay,
        ) { EventInputState(items = it) },
    )

    override fun processStates(
        inputState: EventInputState,
        currentState: VexCompoundState<EventInputState, EventOutputState>,
    ): VexCompoundState<EventInputState, EventOutputState> {
        val currentInput = currentState.inputState

        val screen = inputState.screen ?: currentInput.screen
        val items = inputState.items ?: currentInput.items

        val nextInput = EventInputState(
            screen = screen,
            items = items,
        )

        val nextOutput = processEventScreen(
            screen = screen,
            items = items,
        )

        return currentState.copy(
            inputState = nextInput,
            outputState = nextOutput,
        )
    }
}

// post-construction
eventRelay.activate(vexAppScope)
~~~

For recursive Compound relationships, activate leaves before parents and release parents before
children. Parent activation holds a dependent lease on each Compound child:

~~~kotlin
childCompound.activate(vexAppScope)
parentCompound.activate(vexAppScope)

// later
parentCompound.deactivate()
childCompound.deactivate()
~~~

Activation fails before observer startup for cycles, duplicate Relay refs, or conflicting observer
coroutine refs. Do not mutate another Relay from `processState`, `processStates`, or a dependency
mapper; relationships must stay visible in the declared topology.
