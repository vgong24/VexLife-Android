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
data class ItemVexState(
    val items: List<Item>,
) : VexState<ItemVexState> {

    override fun deepCopy(): ItemVexState =
        copy(
            items = items.map { it.copy() },
        )
}
~~~

The self generic means ItemVexState.deepCopy() must return ItemVexState.

Only the state container needs this contract. Nested properties may be primitives, immutable data
classes, lists, enums, or other VexStates as the component requires.

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
