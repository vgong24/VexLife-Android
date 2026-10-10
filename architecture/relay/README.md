# Vex Relay

[VXG RealForever]

## The question comes first

> This component has several things changing around it. How do I keep those changes in one family,
> process the latest family state safely, and expose what the component currently means?

Use a VexRelay.

~~~text
named component changes
        |
        v
component Relay
  InputState family
        |
        | processLatestState(...)
        v
  OutputState family
        |
        v
observers
~~~

The important boundary is not "all state in the app." It is one scoped component relationship.

## The template pattern

A Relay is meant to be extended. The component owns its nested state family.

~~~kotlin
class EventTooltipRelay :
    VexRelay<EventTooltipRelay.InputState, EventTooltipRelay.OutputState>(...) {

    data class InputState(
        val screen: ScreenState,
        val events: EventState,
        val interaction: InteractionState,
        val target: TargetState,
    ) : VexInputState

    data class OutputState(
        val tooltip: TooltipOutput,
        val snackbar: SnackbarOutput,
    ) : VexOutputState

    override fun processLatestState(
        inputState: InputState,
    ): OutputState {
        // one synchronous reconciliation of this component family
    }

    fun screenEntered() {
        send { input ->
            input.copy(screen = input.screen.copy(...))
        }
    }

    fun eventArrived(event: Event) {
        send { input ->
            input.copy(events = input.events.with(event))
        }
    }

    fun buttonClicked(button: UiElementRef) {
        send { input ->
            input.copy(interaction = input.interaction.clicked(button))
        }
    }
}
~~~

Callers do not replace the whole InputState.

They send the part that changed through a meaningful component method. The Relay applies that
partial transformation to the latest aggregate InputState while it owns the serialization lock.

That is how a screen change, event arrival, and button click can happen without overwriting sibling
state.

## Level 0 — screen enter / exit

The sample app starts with the smallest useful family:

~~~text
FoundationScreenRelay
  InputState
    screenPresence

  OutputState
    screenPresence
~~~

Its public API is screenEntered() and screenExited(). Nothing outside the Relay constructs a
replacement whole state.

## Level 1 — several nested input states

A complex screen can still be one understandable family:

~~~text
EventTooltipRelay
  InputState
    screen
    events
    interaction
    target

  processLatestState

  OutputState
    tooltip
    snackbar
~~~

This is organized complexity, not a God class, when all of those facts participate in one scoped
screen/component relationship.

If a state does not belong to that relationship, it should not be placed in that Relay.

## Race condition rule 1 — mechanical concurrency

Suppose these arrive at almost the same time:

~~~text
screenEntered()
eventArrived(...)
buttonClicked(...)
~~~

The base Relay serializes send.

Each transformation:

~~~text
lock Relay
-> read latest aggregate InputState
-> give the transform a defensive copy
-> change only its nested piece
-> snapshot the result
-> process latest state synchronously
-> commit one new Relay state
-> unlock
~~~

So three callers cannot all read the same old whole-state object and overwrite one another.

The contract test runs screen/event/button changes concurrently and requires all three results to
survive.

## Race condition rule 2 — semantic ordering

Serialization answers "which update entered this Relay first?"

It does not answer "which external fact is semantically newer?"

Example:

~~~text
event source revision 20 arrives
event source revision 10 arrives later
~~~

The Event Tooltip nested EventState owns that source-ordering rule and rejects revision 10.

Another component may use a timestamp, operation identity, generation, or no ordering at all.

~~~text
MECHANICAL_SERIALIZATION != SEMANTIC_FRESHNESS
~~~

## Repeated identical actions

The same button can be clicked twice. If both clicks matter, the component interaction state must
make that distinction visible, for example:

~~~text
buttonId = same
clickRevision = 1 -> 2
~~~

The Relay does not guess whether equal-looking values mean "no event."

This preserves:

~~~text
SAME_VALUE != SAME_TRANSITION
STATEFLOW != EVENT_LEDGER
~~~

without requiring a full event ledger in the base Relay.

## Defensive snapshots / deep copy

The original State Relay protected against mutable ownership sharing. This Relay keeps that
contract.

Every Relay requires an InputState snapshotter and OutputState snapshotter. The component owns those
copy rules because only the component knows its nested values.

The base Relay copies:

- initial input immediately;
- current input before a send transform receives it;
- transformed input before retention;
- input again before processLatestState;
- output before retention;
- input and output on every exposed current / StateFlow read.

Prefer immutable component state anyway. The snapshot boundary prevents accidental mutable
ownership from silently crossing the Relay boundary.

## Input revision versus output revision

VexRelayState exposes inputRevision and outputRevision.

Example:

~~~text
Tooltip screen = EXITED
targetAvailable changes false -> true

inputRevision  0 -> 1
outputRevision 0 -> 0
visible        false
~~~

A real input relationship changed, but reconciled output meaning did not.

Later:

~~~text
screen EXITED -> ENTERED

inputRevision  1 -> 2
outputRevision 0 -> 1
visible        false -> true
~~~

This is the simple first step toward the deeper historical distinction between received input,
coherent state, and emitted semantic output.

## processLatestState is intentionally synchronous

The subclass implements processLatestState(inputState).

It answers:

> Given this component's latest admitted input family, what is its current output family?

It must not launch network work, retry, perform long-running repository effects, or recursively send
another Relay input.

Those deeper behaviors can later surround Relay through the accepted operation/effect architecture.

## State family, not state junk drawer

Many component folders may contain InputState, OutputState, or nested state types.

That is good. The parent tells you state of what.

~~~text
architecture/relay
views/container/tooltip
views/container/snackbar
views/list
conversation
media
~~~

Do not centralize every state type into one global state directory merely because they all contain
the word "state."

## Historical lineage

This restores the shape preserved by the original VexLife State Relay:

~~~text
StateCell
-> VexCompoundState<InputState, OutputState>
-> latest coherent reconciliation
-> output
-> Android projection
~~~

The later Android Architecture Studio correctly expanded this into operation identity, effects,
result admission, durable re-entry, scoped runtimes, and bounded projections.

The prior Android implementation preserved many of those rules, but the parent idea became hard to
see because the pieces were spread across runtime/admission/executor/projection types.

This repository rebuilds from the readable parent outward.

## Not in the base Relay yet

- observation enum / unobserved-empty-held-unavailable;
- stateRef / instanceRef / transitionRef;
- multiple independently versioned producer snapshots;
- operation / attempt / result contracts;
- effect execution;
- retry / durability;
- WorkManager re-entry;
- ViewModel policy;
- Tooltip placement;
- Snackbar arbitration.

Those are real historical concerns. Add them beneath Relay or within a component when the next
concrete relationship needs them.

## Naming collision

This Android architecture VexRelay is unrelated to the Vextreme-SDK repository-task protocol that
is also named VexRelay.

Android package:

~~~text
com.vextreme.vex.android.architecture.relay
~~~

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][RELAY] -->
