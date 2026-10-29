# Vex Family

[VXG RealForever]

This module's current review candidate is the smallest readable Family circuit for:

~~~text
CURRENT State + incoming Control
-> Act outcome
-> stay / admit
-> optional begin
-> current State / execution
~~~

## Start here

Read FAMILY-CIRCUIT.md, then VexFamilyHighConfidenceTest.

Current runtime:

~~~text
Family.activate(control)
-> concrete Family.act(currentState, control)
-> when (currentState)
-> when (control)
-> stay / admit / optional begin
-> StateRelay / execution
~~~

State and Control traversal are native Kotlin sealed-type when expressions. The current candidate
does not use:

~~~text
VexBehaviorV2
resolve(...)
VexActs
VexActsBuilder
VexActRule
operator invoke
state<State> helper
on<Control> helper
VexStateDisposition
recursive Family self-generic
CompoundFamily
stateFactory
Family revision
post-process act(OutputState)
~~~

## Current State ownership

~~~text
private stateRelay
  MutableStateFlow<State>

currentState
  -> stateRelay value

states
  -> stateRelay.asStateFlow()
~~~

There is one live semantic State holder.

## DisplayState responsibility

`DisplayState` is part of the `VexFamily` contract rather than another Vex root:

~~~text
VexFamily
-> DisplayState
   -> StateOnly
~~~

`VexFamily.DisplayState.StateOnly(currentState)` is the complete default. It means current semantic
State already carries all display meaning this Family has earned. It is not a placeholder and does
not ask a feature to invent another ViewState / UiState / PresentationState.

A concrete Family should override `displayState` only when real consumers would otherwise repeat the
same derived interpretation of State. A richer feature-owned classification belongs under that
concrete Family, for example `ThemeModeFamily.DisplayState`, and implements
`VexFamily.DisplayState<ThemeModeFamily.State>`.

## State / Control relationship visibility

The Family relationship surface is intentionally exhaustive rather than sparse-by-default:

~~~text
declare all State possibilities
declare all Control possibilities

act(currentState, control)
-> exhaustive when(currentState)
   -> exhaustive when(control)
~~~

State remains the first traversal because current State is the truth against which an incoming
Control receives meaning.

At this semantic boundary, do not use `else` to hide future State or Control additions. A newly
added State or Control should create compiler-visible relationship work so its placement cannot be
silently forgotten. Controls may share one branch only when their reviewed authored outcome is
exactly the same.

Use:

~~~text
noAct()
    reviewed State + Control pair has no lawful Act

stay()
    lawful Act; current State remains current

admit(State)
    lawful Act; admit State
~~~

`noAct()` is explicit authoring language for absence; concrete Family source should not use a naked
`null` to communicate that semantic decision.

## Act / Begin ownership

The authored outcome tree is rooted under Family:

~~~text
VexFamily
-> Act<State>
   -> optional Begin<State>
      -> Save
         -> Results
            -> Success
            -> Failure
~~~

`Act` is the lawful outcome of one exact current State + incoming Control relationship. `Begin`
does not exist as a peer package capability. Its construction is closed and a concrete Family only
receives the protected `begin(...)` opening while authoring an Act.

The current Begin mode names are only a provisional adapter to the not-yet-conformed VexExecution
launch implementation; they are not a second owner of execution launch semantics.

## Logger boundary

Logging is an explicit dependency on the independent `VexLogger` root:

~~~text
VexFamily
-> VexLogger.Sink

VexLogger
-> Event
   -> Family
      -> accepted Family facts
~~~

There is no `VexFamilyLogger` root. Family keeps the Sink private and concrete Family decision code
does not receive logging capability. This keeps `act(currentState, control)` focused on relationship
meaning while the Family runtime records only facts it actually applies.

## Store responsibility

The current Family runtime consumes one exact storage capability:

~~~text
VexFamily
-> Store<State>
   -> save(State)
   -> WriteResult
      -> Success
      -> Failure
~~~

There is no standalone `VexStore` root in this candidate and no marker-only Store hierarchy.
A concrete Family may extend `VexFamily.Store<State>` locally if a real consumer earns more
operations, but `get`, `forget`, or Store observation are not implied by Save.

Store durability is also not another Family State owner:

~~~text
Store truth != Family.currentState
~~~

The Begin Save relationship owns its explicit success/failure consequences. There is no generic
`Operation -> StoreOperation -> Save` taxonomy until a second real operation proves a common
contract.

## Current feature-facing shape

~~~kotlin
class ThemeModeFamily(...) :
    VexFamily<State, Control>(...) {

    sealed interface State {
        data object Light : State
        data object Dark : State
    }

    sealed interface Control {
        data object Toggle : Control
    }

    override fun act(
        currentState: State,
        control: Control,
    ): VexFamily.Act<State>? =
        when (currentState) {
            State.Light ->
                when (control) {
                    Control.Toggle -> admit(State.Dark)
                }

            State.Dark ->
                when (control) {
                    Control.Toggle -> admit(State.Light)
                }
        }
}
~~~

## Deliberately still open

~~~text
serialized competing-cause State decision / stale async result
provider/repository observation -> semantic Family State
automatic UI observation/lifetime integration
cross-cycle relationship memory
provider / intent identity mapping
Process OutputState -> rightful Family.currentState handoff
exact Family + Relay Single/Compound composition
~~~

The current candidate should not be enlarged merely to fill those boxes.

<!-- [VXG RealForever][VEX-FAMILY][CURRENT-ENTRY] -->
