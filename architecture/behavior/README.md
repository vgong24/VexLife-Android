# Vex Family

[VXG RealForever]

The Direct `VexFamily` foundation accepted on main through
[PR #193](https://github.com/vgong24/VexLife-Android/pull/193) is the smallest readable Family circuit for:

~~~text
CURRENT State + incoming Control
-> Act?
-> null / stay / admit
-> optional saveState
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
-> optional saveState
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
   -> admittedState?
   -> saveState?
~~~

Act is the lawful outcome of one exact CURRENT State + incoming Control relationship.

Direct Family no longer carries a nested result-side State-writing language. The only execution
consequence currently earned by a real Direct-Family production consumer is durable Save(State), so
the source names that capability directly instead of generalizing ahead of a second effect.

~~~text
Act immediate State
-> applied first

optional saveState
-> starts Store.save(saveState) afterward
-> successful return / thrown failure are execution facts
-> cannot author Family State
~~~

If a future asynchronous result changes Family meaning, that meaning must return as a typed Control
and re-enter:

~~~text
act(CURRENT State, Control)
~~~

before another State can be admitted.

## Execution relationship identity

Family owns the maintained Save relationship identity once:

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

Feature authoring does **not** choose Earliest / Latest / Queued. The semantic surface is only:

~~~kotlin
stay(
    saveState = state,
)

admit(
    state,
    saveState = state,
)
~~~

The current runtime persists accepted Save consequences in Control-acceptance order. That execution
strategy is mechanics beneath Family; it is not another feature-facing semantic vocabulary.

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

The shared Family storage contract is deliberately one operation:

~~~text
VexFamily.Store<State>
-> save(State)
-> successful return OR thrown failure
~~~

Family does not invent a second shared result wrapper when VexExecution already owns operation
success/failure.

Store truth is not another live Family State owner:

~~~text
Store truth != Family.currentState
~~~

Direct Family currently has **no generic live Store-observation contract**. ThemeMode's demonstrated
durability requirement is smaller:

~~~text
fresh process/session
-> read durable ThemeMode once
-> construct ThemeModeFamily(initialState = durable value)

accepted Select(mode)
-> immediate stay/admit
-> optional saveState = mode
-> Store.save(mode)
~~~

Concrete storage may keep local current()/forget() mechanics without teaching those operations to
every future Family Store.

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

## One live Control-to-State doorway

Inside a live Direct Family, StateRelay is the one State holder and Control meaning enters through:

~~~text
Control
-> activate(control)
-> act(CURRENT State, control)
-> null / stay / admit
-> same StateRelay
~~~

activate(...) serializes this sequence and applies an Act's immediate admitted State **before**
starting its optional durable Save.

Control remains the finite affordance/request vocabulary. Provider/repository observations are not
silently reclassified as Controls merely to force every kind of truth through one API.

No generic live external-truth reconciliation API is claimed by Direct Family today. A concrete
consumer must first demonstrate that need and its rightful semantics. ThemeMode does not need one:
its durable selection is read once before fresh Family construction, then explicit Controls own live
changes.

Likewise, Save completion has no State-writing callback. If a future asynchronous result must change
Family State, that feature must first earn the exact semantic return path and any identity required
to distinguish obsolete results; the current architecture does not pretend serialization alone
solves that problem.

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
generic live provider/repository observation -> Family State reconciliation
automatic UI observation/lifetime integration
cross-cycle relationship memory
provider / intent identity mapping
exact future Family + Relay Single/Compound composition beyond the established Compound current-State pattern
~~~

These remain separate questions; they do not require another State holder or an OutputState handoff.

<!-- [VXG RealForever][VEX-FAMILY][CURRENT-ENTRY] -->
