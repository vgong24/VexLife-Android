# Vex Relay

[VXG RealForever]

## Question

> This component has one or more truths changing around it. How do I keep each Source of Truth
> reliable, combine only the relationships that belong together, and expose one current result?

Use `VexRelay` for one truth. Use `VexCompoundRelay` when several child truths form one scoped
relationship.

## Small Relay

~~~text
VexState<Self>
-> VexRelay<State>
-> currentState: StateFlow<State>
~~~

A component subclasses VexRelay and exposes intention-revealing methods such as:

~~~text
screenEntered()
itemsArrived(...)
buttonClicked(...)
~~~

The base serializes processing and defensively copies retained/exposed state.

## Deep-copy contract

~~~text
data class copy() != deep copy
~~~

`VexDeepCopyable` exists so nested mutable ownership can recursively detach without pretending
every nested object is independently observable state.

The copy contract is transitive.

## Compound Relay

~~~text
ScreenVexRelay ───────┐
ItemVexRelay ─────────┼─> EventScreenVexCompoundRelay
InteractionVexRelay ──┘
~~~

A compound input uses nullable top-level fields as partial patches:

~~~text
screen = null
-> this patch says nothing new about screen
~~~

`processStates()` merges each supplied patch with `currentState.inputState`, derives output, and
returns one next `VexCompoundState<InputState, OutputState>`.

## Construction versus activation

Construction is inert.

~~~text
construct Compound Relay
-> INACTIVE

activate(VexExecutionOwner)
-> start all named dependency observers
-> ACTIVE
~~~

Do not launch dependency collectors from a constructor.

Activation is transactional. If one dependency fails to start:

~~~text
stop/join already-started observers
restore pre-activation Compound state
return to INACTIVE
throw failure
~~~

The caller can repair the conflict and retry.

`deactivate()` stops and joins all dependency observers before returning to INACTIVE.

## Mechanical versus semantic races

Relay serialization prevents sibling partial updates from overwriting one another.

Semantic freshness remains with the smallest rightful Source-of-Truth Relay:

~~~text
arrival order != semantic order
~~~

Example: ItemVexRelay can reject source revision 10 after revision 20 before a parent ever sees it.

## StateFlow boundary

Processing does not depend on Compose or another collector.

~~~text
send/update/process
-> semantic processing

StateFlow
-> latest-state projection

collectAsState()
-> observer
~~~

StateFlow is not an event ledger.

## Observability

Every Relay has a stable `relayRef`.

Owner-level logging emits bounded structural facts after state locks are released:

~~~text
relay.input.received
relay.state.changed
relay.state.unchanged
compound.activated
compound.activation.failed
compound.deactivated
~~~

Dependency observation carries the named coroutine ref into the parent as `originRef`.

No full state graph is logged automatically.

## Executable proof

Read:

~~~text
src/test/.../VexRelayFirstPrinciplesTest.kt
~~~

It covers recursive deep-copy safety, stale child rejection, explicit inert construction, activation,
nullable patch merging, concurrent child updates, rollback + state restoration, retryable activation,
and Relay logging.

See `examples/SCREEN_AND_COMPOUND.md` for the reusable template shape.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][RELAY] -->
