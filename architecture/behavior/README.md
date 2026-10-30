# Vex Family

[VXG RealForever]

The Direct `VexFamily` foundation accepted on main through
[PR #193](https://github.com/vgong24/VexLife-Android/pull/193) is the smallest readable Family circuit for:

~~~text
CURRENT State + incoming Control
-> Act?
-> null / stay / admit
-> optional saveLatest(State)
-> current State / storage execution
~~~

## Current foundation state

Two behavior foundations intentionally coexist today:

~~~text
existing VexBehaviorFamily / SCAE
    accepted
    still has production consumers

Direct VexFamily
    accepted on main through PR #193
    owns the newer direct State + Control -> Act relationship model
~~~

Direct `VexFamily` did **not** wholesale replace the existing Behavior/SCAE implementation.
Migration/evolution remains consumer-driven. A fresh worker should not delete or rewrite older
Behavior source merely because the Direct Family foundation exists.

PR #193 is the acceptance/origin reference for the Direct Family identity/custody refinement:
https://github.com/vgong24/VexLife-Android/pull/193

Architecture evolution in this repository now applies the repo-owned **Subtractive Completion**
posture from docs/CULTURE.md: additions start rejected, original failures must be replayed against
proposed fixes, and mechanics stay private unless removing them would lose a demonstrated semantic
requirement.

## Start here

Read FAMILY-CIRCUIT.md, then VexFamilyHighConfidenceTest.

Current runtime:

~~~text
Family.activate(control)
-> concrete Family.act(currentState, control)
-> when (currentState)
-> when (control)
-> null / stay / admit
-> optional saveLatest(State)
-> StateRelay / storage execution
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

## Family kinship

The direct Family closes compile-time kinship with one recursive bound at the root, without
propagating recursive self-generics through every child:

~~~text
VexFamily<Family, State, Control>

Family
    -> must itself be VexFamily<Family, State, Control>

State
    -> VexFamily.State<Family>

Control
    -> VexFamily.Control<Family>
~~~

A concrete Family therefore binds its vocabulary to one exact Family coordinate:

~~~kotlin
class ThemeModeFamily(...) :
    VexFamily<
        ThemeModeFamily,
        ThemeModeFamily.State,
        ThemeModeFamily.Control
    >(...) {

    sealed interface State :
        VexFamily.State<ThemeModeFamily>

    sealed interface Control :
        VexFamily.Control<ThemeModeFamily>
}
~~~

This prevents another Family's Control from being combined with this Family's State and also
prevents an arbitrary marker type from pretending to be the Family coordinate. The concrete root
must actually be the VexFamily that owns the vocabulary.

Family-owned children such as `DisplayState`, `Store`, and `Act` require a Family-bound State
type, so kinship continues down the tree without repeating the recursive root equation at every call
site.

~~~text
ROOT SAFETY
    Family coordinate must itself be the owning VexFamily

FAMILY SAFETY
    ThemeModeFamily.State + MapsFamily.Control
    -> does not satisfy VexFamily<ThemeModeFamily, ...>

ROLE SAFETY
    State and Control occupy different Family marker roles
~~~

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
null
    no lawful Act exists for this exact CURRENT State + Control

stay()
    lawful Act; immediate semantic State remains CURRENT

admit(State)
    lawful Act; State is admitted before any authored Save begins
~~~

## Act / Save ownership

The current earned outcome tree is deliberately small:

~~~text
VexFamily
-> Act<State>
   -> immediate admitted State?
   -> optional Save<State>
~~~

Act is the lawful outcome of one exact CURRENT State + incoming Control relationship.

Direct Family no longer carries a nested result-side State-writing language. The only execution
consequence currently earned by a real Direct-Family production consumer is durable Save(State), so
the source names that capability directly instead of generalizing ahead of a second effect.

~~~text
Act immediate State
-> applied first

optional Save
-> starts afterward
-> success/failure are storage/execution facts
-> cannot author Family State
~~~

If a future asynchronous result changes Family meaning, that meaning must return as a typed Control
and re-enter:

~~~text
act(CURRENT State, Control)
~~~

before another State can be admitted.

## Execution relationship identity

Family owns the maintained Save relationship boundary:

~~~text
live Family
+ exact Control identity
+ optional stable Control.resourceId
=
VexFamily.RelationshipId<Family>
-> VexExecution.RelationshipId
~~~

Control identity is always present. resourceId only narrows repeated semantic resources for that
same Control kind; ordinary Control payload does not silently become relationship identity.

The only currently earned maintained execution authoring surface is:

~~~kotlin
saveLatest(state)
~~~

which means the Save associated with the triggering Control uses Latest relationship semantics.

A concrete Control may expose an exact typed ResourceId when Control kind alone is too broad:

~~~kotlin
data class TrackId(
    val value: String,
) : VexFamily.ResourceId<PlayerFamily>

data class Play(
    override val resourceId: TrackId,
    val position: Int,
) : Control
~~~

position remains request payload. Equal resource values on different Control kinds do not collide
because exact Control identity remains part of the relationship.

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

The shared storage contract carries only capabilities currently required by Direct Family:

~~~text
VexFamily.Store<State>
-> save(State)
-> WriteResult

VexFamily.StateStore<State>
-> Store<State>
-> observableState
~~~

get() and forget() are not generic Family architecture. A concrete storage realization may expose
read/reset helpers for its own local needs without teaching that CRUD shape to every future Store.

Store truth is not another Family State owner:

~~~text
Store truth != Family.currentState
~~~

Durable State observation has no direct State-writing privilege. An observed State is turned into a
typed Family Control and enters the same semantic doorway:

~~~text
StateStore.observableState
-> typed Control
-> act(CURRENT State, Control)
-> null / stay / admit
-> same StateRelay
~~~

Execution owns observation lifetime only. Store owns durable bytes/State. Family owns what the
observation means.

## Current feature-facing shape

~~~kotlin
class ThemeModeFamily(...) :
    VexFamily<
        ThemeModeFamily,
        ThemeModeFamily.State,
        ThemeModeFamily.Control
    >(...) {

    sealed interface State :
        VexFamily.State<ThemeModeFamily> {
        data object Light : State
        data object Dark : State
    }

    sealed interface Control :
        VexFamily.Control<ThemeModeFamily> {
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

## One semantic State doorway

StateRelay remains the one live Family State holder, and act(CURRENT State, Control) is the one
semantic State-authoring doorway.

~~~text
incoming semantic cause
-> typed Family Control
-> act(CURRENT State, Control)
-> null / stay / admit
-> same StateRelay
~~~

This applies equally to human intent, provider/repository reconciliation, and durable Store
observation. Those sources may use different concrete methods/adapters, but if they change Family
State they must eventually present typed Control meaning to the Family.

The runtime serializes activate(...), so Control meaning is resolved against exact CURRENT State.
There is no second protected setter-like semantic API and no delayed result callback that can write
State behind act(...).

The current durable path is therefore:

~~~text
StateStore.observableState
-> Family-owned Control.Stored(...)
-> activate(control)
-> act(CURRENT State, control)
-> stay / admit
-> same StateRelay
~~~

A future asynchronous result that has semantic meaning follows the same law:

~~~text
operation result
-> typed result Control
-> act(CURRENT State, resultControl)
-> stay / admit / null
~~~

The feature must carry whatever identity is actually required to distinguish an obsolete result from
the current request. Serialization alone is not claimed to solve stale-result identity.

## Compound current State

For a Compound relationship, the processed result is already the complete current State:

~~~text
incoming InputState
+ exact CURRENT VexCompoundState
-> processStates(...)
-> VexCompoundState(inputState, outputState)
-> one current-State commit
~~~

`inputState` is the coherent retained input family and `outputState` is the meaning derived from
that input by the processing layer. Either may change on a processing pass; when both change they are
made current together rather than admitting OutputState separately afterward.

The established Relay surfaces expose that same current relationship as `current`,
`currentInput`, `currentOutput`, `states`, `inputStates`, and `outputStates`.

Therefore the old:

~~~text
Process OutputState -> Family.currentState
~~~

gap is not a missing Output bridge. OutputState alone was an incomplete representation of the
processed Compound State.

## Deliberately still open

~~~text
automatic UI observation/lifetime integration
cross-cycle relationship memory
provider / intent identity mapping
exact future Family + Relay Single/Compound composition beyond the established Compound current-State pattern
~~~

These remain separate questions; they do not require another State holder or an OutputState handoff.

<!-- [VXG RealForever][VEX-FAMILY][CURRENT-ENTRY] -->
