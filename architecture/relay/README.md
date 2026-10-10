# Vex Relay

`[VXG RealForever]`

## The question comes first

> **This component receives changing information. How does it keep one current relationship and
> expose what that relationship means?**

Use a **VexRelay**.

The smallest mental model is:

```text
component input
    |
    v
VexRelay
    |
    | reconcile one relationship
    v
VexCompositeState<InputState, OutputState>
    |
    v
observer
```

That is the public idea. The machinery should be learned only when the component needs more depth.

## Level 0 — screen enter / exit

A screen does not need operation records, effect admission, durable work, or a generic runtime just
to answer whether it is currently entered.

```kotlin
enum class ScreenEvent {
    ENTER,
    EXIT,
}

enum class ScreenPresence {
    ENTERED,
    EXITED,
}

val screenRelay = VexRelay(
    initialInput = ScreenEvent.EXIT,
) { event ->
    when (event) {
        ScreenEvent.ENTER -> ScreenPresence.ENTERED
        ScreenEvent.EXIT -> ScreenPresence.EXITED
    }
}

screenRelay.accept(ScreenEvent.ENTER)
screenRelay.current.output == ScreenPresence.ENTERED
```

That is the first-principles foundation.

## Level 1 — the input becomes relational

A Tooltip may care about more than one current fact:

```kotlin
data class TooltipInputState(
    val screenPresence: ScreenPresence,
    val targetAvailable: Boolean,
)

data class TooltipOutputState(
    val visible: Boolean,
)

val tooltipRelay = VexRelay(
    initialInput = TooltipInputState(
        screenPresence = ScreenPresence.EXITED,
        targetAvailable = false,
    ),
) { input ->
    TooltipOutputState(
        visible =
            input.screenPresence == ScreenPresence.ENTERED &&
            input.targetAvailable,
    )
}
```

Relay does not become more complicated merely because the component's relationship became richer.
The **component owns the names of its inputs and outputs**.

This is the human-readable continuation of the older
`VexCompoundState<InputState, OutputState>` idea: several facts can be represented by one typed
component input and reconciled into one coherent output.

## Level 2 — composite inputs grow with the component

A later Tooltip may add:

```text
screen presence
+ exact target identity
+ target geometry
+ safe window geometry
+ acknowledgement state
+ competing transient attention
```

Those facts belong under the Tooltip component because the question is still:

> "What does this Tooltip currently mean and what should it expose?"

They do not become unrelated top-level utility modules merely because each fact has state.

## Level 3 — only then add deeper Relay contracts

The accepted VexLife lineage already proved deeper concerns that can be added **under Relay** when
a real component needs them:

```text
observation: UNOBSERVED / EMPTY / PRESENT / HELD / UNAVAILABLE
snapshot identity: state / instance / revision / transition
latest-coherent multi-producer reconciliation
same value + distinct transition
semantic no-op suppression
operation != attempt != coroutine
external effect result admission
durable re-entry
bounded surface projection
```

Those are not prerequisites for understanding or using Level 0.

The hierarchy is:

```text
simple Relay relationship
-> richer component input/output
-> observation / identity when needed
-> accepted operations/effects when needed
-> durable/runtime behavior when needed
-> multiple bounded projections when needed
```

## Why this is under architecture/relay

The folder answers a reusable architectural question:

> **How does information enter a component relationship and become one coherent current output?**

It is not a bucket for "all state."

A component such as Tooltip, Snackbar, List, Conversation, or Media may each have its own state files
under its own component folder while reusing this Relay architecture.

## Historical lineage being preserved

The original VexLife direction was whole-first:

```text
StateCell
-> VexCompoundState<InputState, OutputState>
-> Android StateFlow projection
```

The later Android Runtime Architecture expanded the semantic boundary:

```text
typed intention
-> VexRelay / scoped runtime
-> accepted operation/effect work
-> admitted state
-> bounded projection
-> ViewModel / Compose / widget / notification
```

The previous Android implementation preserved many of those rules but distributed them across
`VexRuntimeWitness`, result admission, operation/executor types, and a separate state-projection
module. That made the rules stronger while making the parent idea harder to perceive.

This module restores the parent first.

## Not owned here yet

This first slice deliberately does **not** add:

- a generic semantic-ref universe;
- operation/attempt/result types;
- external effect execution;
- retries or durability;
- a transition/event ledger;
- Android lifecycle ownership;
- ViewModel or Compose policy;
- Tooltip or Snackbar behavior.

Those become Relay depth or component-local behavior only after a real relationship earns them.

## Naming collision

This Android architecture `VexRelay` is unrelated to the repository-task/evidence protocol named
VexRelay in Vextreme-SDK / Local Vex Operations.

The package boundary is explicit:

```text
com.vextreme.vex.android.architecture.relay.VexRelay
```

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][RELAY] -->
