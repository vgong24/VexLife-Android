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

Begin carries only `VexExecution.Approach`. It does not accept relationship arguments or manufacture
identity.

## Execution relationship identity

Family owns the maintained relationship boundary:

~~~text
VexFamily.Control<Family>
    -> Control name is always included

optional VexFamily.ResourceId<Family>
    -> narrows repeated instances of that same Control

live Family
+ Control name/type
+ optional typed ResourceId
=
VexFamily.RelationshipId<Family>
    -> VexExecution.RelationshipId
~~~

`activate(control)` already owns the exact Control that earned the Act. The Family runtime therefore
forms the relationship identity automatically. Concrete `beginEarliest { ... }`,
`beginLatest { ... }`, and `beginQueued { ... }` choose only overlap behavior.

~~~kotlin
beginLatest {
    save(nextState).results {
        success()
        failure()
    }
}
~~~

There is no `trigger: Any`, no `vararg Any?`, and no second Family `ExecutionId` authoring surface.

A concrete Control may bind an exact ResourceId type when Control kind alone is too broad:

~~~kotlin
data class TrackId(
    val value: String,
) : VexFamily.ResourceId<PlayerFamily>

data class Play(
    override val resourceId: TrackId,
    val position: Int,
) :
    Control,
    VexFamily.Control.Resource<
        PlayerFamily,
        TrackId
    >
~~~

The foundation still supplies `Play` as part of the relationship identity. The concrete Control declares
the exact ID type; an `AccountId` cannot be substituted into this `Play` merely because both IDs belong
to the same Family. ResourceId itself is not flattened to a shared String inside the relationship key.
Other Control payload such as `position` remains request evidence.

~~~text
Control payload
!= automatic relationship identity

Control kind/name
= always relationship identity

typed ResourceId
= optional narrowing identity

Approach
= Independent / Earliest / Latest / Queued
~~~

The live Family instance scopes the resulting RelationshipId, so equal Control/ResourceId values on two
live Family instances remain separate.

`SourceRef` remains automatic diagnostic/source information. `InstanceId` remains the exact
per-launch occurrence. Neither substitutes for the semantic RelationshipId.

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

## Deliberately still open

~~~text
serialized competing-cause State decision / stale async result
    -> intentionally unresolved generically
    -> do not reintroduce Family revision as a guessed currentness oracle
provider/repository observation -> semantic Family State
automatic UI observation/lifetime integration
cross-cycle relationship memory
provider / intent identity mapping
Process OutputState -> rightful Family.currentState handoff
exact Family + Relay Single/Compound composition
~~~

The current candidate should not be enlarged merely to fill those boxes.

<!-- [VXG RealForever][VEX-FAMILY][CURRENT-ENTRY] -->
