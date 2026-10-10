# Vex Relay

[VXG RealForever]

## What question does this architecture answer?

> A component has information changing from one or more places. How do I keep each source reliable,
> combine only the states that belong to this component, process one current relationship, and let
> other components observe the result?

The answer grows in three small pieces:

~~~text
VexState
  a copy-safe state value

VexRelay<State>
  one Source-of-Truth state relationship

VexCompoundRelay<InputState, OutputState>
  several related Source-of-Truth states -> one component relationship
~~~

Do not start with Compound Relay when one Relay is enough.

---

## 1. VexState — the copy contract

~~~kotlin
interface VexState<Self : VexState<Self>> {
    fun deepCopy(): Self
}
~~~

Example:

~~~kotlin
data class Item(
    val id: String,
    val tags: MutableList<String>,
) : VexDeepCopyable<Item> {

    override fun deepCopy(): Item =
        copy(
            tags = tags.toMutableList(),
        )
}

data class ItemVexState(
    val items: List<Item>,
) : VexState<ItemVexState> {

    override fun deepCopy(): ItemVexState =
        copy(
            items = items.map { it.deepCopy() },
        )
}
~~~

The self generic means ItemVexState.deepCopy() must return ItemVexState.

Important:

~~~text
data class copy()
!= deep copy
~~~

A copied parent can still share a mutable child.

VexDeepCopyable exists separately from VexState so a nested value can own recursive copy behavior
without pretending it is independently observable state.

The copy contract is transitive: every mutable ownership boundary reachable from a VexState must be
recursively detached or proven immutable. If Item later contains another mutable child, Item.deepCopy
must copy that child too.

Prefer recursively immutable values whenever practical. Kotlin cannot generally prove deep
immutability for arbitrary object graphs, so this remains an explicit source/test contract rather
than a magical reflection/serialization copy.

---

## 2. VexRelay — one Source of Truth

Use VexRelay when one state family can own its own current truth.

~~~kotlin
class ScreenVexRelay :
    VexRelay<ScreenVexState>(
        initialState = ScreenVexState(),
    ) {

    fun screenChanged(screen: ScreenName) {
        update { current ->
            current.copy(
                screen = screen,
                revision = current.revision + 1,
            )
        }
    }
}
~~~

The default processState simply admits the incoming state.

A state that has its own freshness rule can override processing:

~~~kotlin
class ItemVexRelay :
    VexRelay<ItemVexState>(ItemVexState()) {

    override fun processState(
        inputState: ItemVexState,
        currentState: ItemVexState,
    ): ItemVexState =
        if (inputState.sourceRevision > currentState.sourceRevision) {
            inputState.deepCopy()
        } else {
            currentState.deepCopy()
        }
}
~~~

That keeps "is this item update stale?" inside the Item Source of Truth rather than making every
screen that consumes items reinvent the rule.

---

## 3. VexCompoundState — input and output stay together

~~~text
VexCompoundState
  inputState
  outputState
  revision
~~~

The input and output remain in one current totality.

That matters because a consumer may need to inspect current input context without duplicating those
same fields into OutputState.

---

## 4. VexCompoundRelay — combine only the family that belongs together

Example:

~~~text
EventScreenVexCompoundRelay

InputState
  screen?
  items?
  interaction?

OutputState
  availability
  itemCount
  lastButtonId
~~~

The nullable top-level InputState fields are partial updates:

~~~text
screen = null
means:
"this incoming patch says nothing new about screen"

screen = ScreenVexState(...)
means:
"screen changed; reconcile with this state"
~~~

Important rule:

~~~text
TOP_LEVEL_NULL_IN_COMPOUND_INPUT = NO_UPDATE
~~~

If null itself has domain meaning, put that meaning inside the nested state instead of using the
top-level null marker for both purposes.

The processing shape is intentionally repetitive and readable:

~~~kotlin
override fun processStates(
    inputState: EventScreenInputState,
    currentState: VexCompoundState<
        EventScreenInputState,
        EventScreenOutputState,
    >,
): VexCompoundState<EventScreenInputState, EventScreenOutputState> {

    val currentInputState = currentState.inputState

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
            screen = screen,
            items = items,
            interaction = interaction,
        )

    val nextOutputState =
        if (screen == null || items == null) {
            EventScreenOutputState.empty()
        } else {
            EventScreenOutputState(
                availability = READY,
                itemCount = items.items.size,
                lastButtonId = interaction?.lastButtonId,
            )
        }

    return currentState.copy(
        inputState = nextInputState,
        outputState = nextOutputState,
    )
}
~~~

This is the pattern Victor was recalling:

~~~text
incoming partial InputState
+ currentInputState
-> resolve variables once
-> process logic
-> form OutputState
-> return next CompoundState
~~~

It avoids repeated currentState.inputState.someNestedField chains throughout the business logic.

---

## 5. A Relay can be an input to another Relay

A Compound Relay can listen to smaller Source-of-Truth Relays:

~~~text
ScreenVexRelay ───────┐
ItemVexRelay ─────────┼─> EventScreenVexCompoundRelay
InteractionVexRelay ──┘
~~~

The base provides bindRelay(...).

The component maps each child state into one nullable input patch:

~~~kotlin
bindRelay(scope, screenRelay) { screenState ->
    EventScreenInputState(
        screen = screenState,
    )
}

bindRelay(scope, itemRelay) { itemState ->
    EventScreenInputState(
        items = itemState,
    )
}
~~~

This is the compositional foundation:

~~~text
small Relay
-> can stand alone

small Relay output
-> can become Compound Relay input

Compound Relay output
-> can later become another bounded parent's input
~~~

Do not duplicate the child's freshness/admission logic in the parent.

---

## 6. Race conditions

### Mechanical concurrency

Screen, item, and interaction updates may arrive concurrently.

Each Relay serializes its own processing.

The Compound Relay also serializes incoming child patches.

Therefore:

~~~text
screen patch
items patch
button patch
~~~

cannot each start from one stale aggregate state and overwrite the others.

Each Compound processing pass receives the latest retained CompoundState and merges only the fields
supplied by that patch.

### Semantic freshness

Serialization does not decide whether an external fact is semantically newer.

That belongs to the Source-of-Truth child Relay.

Example:

~~~text
ItemVexRelay current sourceRevision = 20
incoming sourceRevision = 10

-> ItemVexRelay rejects it
-> Compound Relay never needs to rediscover that rule
~~~

This preserves:

~~~text
ARRIVAL_ORDER != SEMANTIC_ORDER
~~~

---

## 7. Deep-copy boundary

Every VexState implements deepCopy().

The Relay copies before retention and before exposure.

That means:

~~~text
caller mutable object
!= Relay retained state

consumer exposed state
!= Relay retained state
~~~

Prefer immutable Kotlin state whenever practical. deepCopy remains the explicit safety boundary for
nested mutable ownership such as lists containing mutable objects.

---

## 8. Processing is not activated by Compose

Compose may observe:

~~~kotlin
val screenState by
    screenRelay.currentState.collectAsState()
~~~

But collectAsState() is an observer, not the semantic engine.

~~~text
send/update/process
  happens at the Relay

StateFlow
  projects latest current state

collectAsState()
  observes latest current state for Compose
~~~

This is intentional.

A Source-of-Truth Relay must not stop processing merely because a screen is not currently collecting
it.

For Compound Relay-to-Relay composition, bindRelay() collects a child StateFlow using an explicitly
provided CoroutineScope. The owner of that scope owns the binding lifetime; Relay does not invent a
hidden global scope.

Koin/Hilt/manual composition may later supply those Relay instances and scopes. The DI framework
does not own their semantics.

---

## 9. Why there is not a SharedFlow event buffer in the base yet

Victor remembers an earlier design using a SharedFlow/replay/buffer path so fast changes would not
disappear.

The durable source recovered for this rebuild proves the required intention, but not that exact
Kotlin buffering implementation.

This reconstruction therefore separates two responsibilities:

~~~text
Relay processing path
  serialized directly
  every send/update is processed before it returns

currentState: StateFlow
  current-state projection
  a slow observer may see latest state rather than every intermediate state
~~~

That is safe for Source-of-Truth current state and preserves:

~~~text
STATEFLOW != EVENT_LEDGER
~~~

If a future component requires every transition to be delivered to another consumer, that should
earn an explicit transition/intention channel with declared buffering/backpressure/replay semantics.
Do not silently turn StateFlow into that channel.

---

## 10. First-emission / EMPTY behavior

Compound child states may arrive independently.

Example:

~~~text
screen observed
items not observed yet
~~~

The Compound Relay can retain the known screen input while OutputState remains EMPTY.

When items arrive:

~~~text
screen current
+ items current
-> process latest coherent component input
-> READY output
~~~

Later Relay depth can restore the full historical observation vocabulary:

~~~text
UNOBSERVED
EMPTY
PRESENT
HELD
UNAVAILABLE
~~~

without changing the simple parent pattern.

---

## 11. Component-first organization

Do not create one global state directory.

A future repository can look like:

~~~text
architecture/
  relay/

views/
  container/
    base/
    tooltip/
    snackbar/
  list/
    base/
~~~

Each component may contain its own Relay/state types.

The parent tells a human state of what.

---

## 12. Koin composes Relay owners; it does not own Relay semantics

For Android app composition, inject Relay owners themselves:

~~~text
Koin
  ScreenVexRelay          single
  ItemVexRelay            single
  InteractionVexRelay     single
  EventScreenCompoundRelay single / explicitly scoped owner
~~~

Do not inject frozen snapshots:

~~~text
WRONG:
EventScreenCompoundRelay(
  screenState = screenRelay.current
)

RIGHT:
EventScreenCompoundRelay(
  screenRelay = screenRelay,
  itemRelay = itemRelay,
  interactionRelay = interactionRelay,
  dependencyScope = applicationOrSessionRelayScope,
)
~~~

The Compound Relay observes those child owners and receives their future currentState changes.

For canonical app-wide Source-of-Truth Relays, application-level singleton composition is the normal
shape. If a truth is explicitly session/account/feature bounded, the Relay can instead be one
instance in that declared scope.

A factory is not the ordinary owner of canonical truth because requesting it again would create
another independent Relay state universe.

~~~text
CARDINALITY + SCOPE > "make everything singleton"
~~~

Koin stays above architecture/relay. The Relay module has no Koin imports and remains reusable with
manual construction, tests, another DI backend, or generated composition.

### Observation lifetime

A Compound Relay must not invent or own anonymous coroutine Jobs.

Dependency collectors are named executions owned by architecture/execution:

~~~text
VexAppScope

  relay.event-screen.observe.screen
  relay.event-screen.observe.items
  relay.event-screen.observe.interaction
~~~

The current helper:

~~~text
observeDependencies(
  executionOwner,
  vexRelayDependency(
    coroutineRef = ...,
    relay = screenRelay,
  ) { partial screen input },
  ...
)
~~~

starts each child observation exactly once.

Relay owns the state relationship.
Execution owns coroutine names, cancellation, supervision, failure visibility and explicit restart.

If a Compound Relay is application-singleton truth, give it the application execution scope.
If it is session-scoped truth, give it that session's execution scope.

Child Relays do not hold Activity, Fragment, Composable, ViewModel or parent references.

### Lazy versus eager Koin construction

A Koin single may be created lazily when first requested.

If a particular Compound Relay must observe app-wide dependencies before any screen asks for it,
that definition may be deliberately eager (created at application start). Do this only for Relays
whose semantics actually require pre-UI observation; eager construction has startup cost.

Compose remains the leaf observer:

~~~text
Source-of-Truth child Relays
        ↓
Compound Relay
        ↓
currentState StateFlow
        ↓
ViewModel/plain holder if useful
        ↓
Compose collectAsState()
~~~

No layer above reaches downward to mutate another Relay's retained state directly.

---

## Historical lineage

The durable VexLife foundation preserved:

~~~text
StateCell
-> VexCompoundState<InputState, OutputState>
-> serialized latest-coherent reconciliation
-> Android StateFlow projection
~~~

and later expanded Relay/runtime ownership into operations, effects, durable re-entry, and bounded
presentation projections.

This repository is rebuilding that architecture from the smallest reusable pattern outward rather
than starting from the mature runtime's implementation fragments.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][RELAY] -->
