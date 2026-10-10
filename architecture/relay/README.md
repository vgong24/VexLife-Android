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

Every Relay has an explicit `VexRelayRef` owned by F1 itself. Observability may project that ref,
but logging does not define Relay identity.

A component subclasses VexRelay and exposes intention-revealing methods such as:

~~~text
screenEntered()
itemsArrived(...)
buttonClicked(...)
~~~

The base serializes processing and defensively copies retained/exposed state.

`VexState.hasSameMeaningAs(...)` defines semantic equality. Data classes get ordinary structural
equality by default; unusual state types can override it explicitly. Object identity is never an
implicit Relay transition rule.

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

The returned state's `revision` must remain equal to the current revision. Revision advancement is
base-owned and occurs only after the Relay has established a semantic input/output change.

## Declared topology

A Compound Relay declares its child relationships through `relayDependencies()`. That declaration
is evaluated lazily once after construction and retained for the Relay instance's lifetime.

~~~text
reactivation
-> restart observation of the same relationship

reactivation
!= silently replace the dependency graph
~~~

Compound input is private to those declared child dependencies. If another fact must affect a
Compound relationship, give that fact its rightful child Relay instead of creating a hidden write
channel.

`VexObservableRelay` is sealed around the two rightful bases (`VexRelay` and
`VexCompoundRelay`). Features may subclass those bases, but cannot create a proxy Relay shape that
forwards another owner's StateFlow while hiding the real topology from graph validation.

Before any observer starts, activation rejects:

- an empty dependency declaration (use `VexRelay` for one truth);
- duplicate dependency coroutine refs;
- two different Relay objects using one `VexRelayRef` inside the reachable graph;
- direct or indirect Compound dependency cycles.

This keeps recursive composition a directed acyclic relationship graph rather than relying on
runtime feedback to eventually settle.

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

Activation is transactional **to external state observers**, not merely to retained memory.

Every dependency observer starts behind one shared activation gate:

~~~text
reserve child leases
-> admit every named observer
-> ACTIVE
-> open activation gate
-> child StateFlows may now emit
~~~

If any dependency fails before the gate opens:

~~~text
stop/join already-started observers
release child leases
emit no provisional Compound currentState
return to INACTIVE
throw failure
~~~

There is no compensating "publish then restore" sequence for consumers to witness. The caller can
repair the conflict and retry.

When a Compound depends on another Compound, the child must already be `ACTIVE`. Successful parent
activation acquires a dependent lease on that child. The child cannot deactivate while any active
parent still depends on it.

~~~text
activate leaves -> parents

deactivate parents -> leaves
~~~

The parent never auto-activates a child's relationship because observing another owner does not
transfer that owner's lifecycle authority. Parent-held child leases also make parent-before-child
deactivation an enforced invariant rather than a call-order convention.

Owned observer cleanup is joined to completion. Any dependency's own `NonCancellable` finalizer must
therefore be bounded; the Relay will not claim ownership release while a child is still cleaning up.

`deactivate()` stops and joins all dependency observers before returning to INACTIVE. Rollback and
normal deactivation use the same ownership-release primitive. Child observer cleanup, Compound-child
lease release, and the owner's own bookkeeping all complete under `NonCancellable`; caller
cancellation cannot strand half-released ownership.

## Processing purity / visible relationships

`update { ... }`, `processState(...)`, and `processStates(...)` run inside their owner's
serialized mutation boundary. They are bounded semantic admission/derivation boundaries. Simple
Relay guards the entire update transform too, so reentrant same-Relay mutations fail before an inner
transition can commit.

~~~text
PROCESSING
-> derive/admit this owner's next state

PROCESSING
!= dispatch external effect
!= mutate another Relay
!= create a hidden relationship edge
~~~

If another truth affects a Compound, declare it as a dependency. If work/effects must happen, route
them through the rightful execution/operation owner outside the state derivation lock. This keeps
cycle detection and architecture tracing representative of the real graph.

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

Dependency observation carries the child Relay's semantic identity into the parent as `originRef`.
The named observer coroutine remains separately visible through VexAppScope and the declared topology,
so semantic origin and execution carrier are not collapsed.

Each successfully processed Relay input also carries an owner-local `transitionSequence`. Logging is
submitted only after the state lock is released, so concurrent callers may reach the logger in a
different wall-clock order; `transitionSequence` preserves the Relay's actual serialized processing
order without making the logger part of state correctness.

Logging is best-effort. A broken or closed diagnostic sink cannot turn an already-committed Relay
transition into an operation failure.

No full state graph is logged automatically.

`ACTIVE` means dependency observation was successfully activated. It is not a health claim that
every dependency execution will remain RUNNING forever; execution health remains observable through
the owning `VexExecutionOwner`.

## Executable proof

Read:

~~~text
src/test/.../VexRelayFirstPrinciplesTest.kt
~~~

It covers recursive deep-copy safety, stale child rejection, explicit inert construction,
observer-transactional activation, nullable patch merging, concurrent child updates, cycle/ref
preflight, recursive Compound leases, cancellation-safe release, retryable activation, and Relay
logging.

See `examples/SCREEN_AND_COMPOUND.md` for the reusable template shape.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][RELAY] -->
