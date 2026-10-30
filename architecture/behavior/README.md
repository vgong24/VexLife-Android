# Vex Family

[VXG RealForever]

The Direct `VexFamily` foundation accepted on main through
[PR #193](https://github.com/vgong24/VexLife-Android/pull/193) is the smallest readable Family circuit for:

~~~text
CURRENT State + incoming Control
-> Act?
-> admitted State? + Effect?

Effect
-> concrete Connection/runtime attempts transformation
-> observed semantic State
-> Family.observe(State)
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
Connection.activate(control)
-> Family.activate(control)
-> Family.act(CURRENT State, control)
-> null / stay / admit
-> immediate State admission
-> return optional Effect
-> Connection realizes Effect
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

The direct Family closes compile-time kinship across the semantic vocabulary:

~~~text
VexFamily<Family, State, Control, Effect>

Family
  -> exact owning Family coordinate

State
  -> VexFamily.State<Family>

Control
  -> VexFamily.Control<Family>

Effect
  -> VexFamily.Effect<Family>
~~~

State / Control / Effect from unrelated Families cannot be substituted into the same semantic root.
Act carries only the already-bound State + Effect pair.

## State / Control relationship visibility

The Family relationship remains directly readable Kotlin:

~~~text
act(CURRENT State, Control)
-> Act?
~~~

Use:

~~~text
null
    no lawful Act exists for this exact CURRENT State + Control

stay(effect?)
    lawful Act; immediate semantic State remains CURRENT

admit(State, effect?)
    lawful Act; State is admitted immediately
~~~

State remains the first semantic coordinate because current truth is what gives an incoming Control
meaning.

Concrete Families should prefer exhaustive State/Control traversal when the vocabularies are finite.
Do not use a catch-all merely to hide a newly-added semantic possibility.

## Act / Effect ownership

Act is the lawful resolved outcome of exact CURRENT State + Control:

~~~text
Act
-> admittedState?
-> effect?
~~~

The ordering is exact:

~~~text
Family.activate(control)
-> act(CURRENT State, control)
-> apply admittedState when present
-> return Act
~~~

Family does **not** realize Effect.

Effect is a runtime transformation request:

~~~text
Effect
!= resulting State
!= provider success
!= coroutine
!= Store
!= network callback
~~~

A concrete Connection/runtime decides how that Effect is attempted.

This preserves the causal loop recovered by the Origin Walk:

~~~text
CURRENT State
-> Control
-> Act
-> Effect
-> runtime/provider attempt
-> observed reality
-> semantic State
-> observe(State)
-> repeat
~~~

## Observation boundary

Provider/repository/runtime truth does not become a fake Control.

When a concrete Connection has already:

- interpreted provider/runtime facts into one truthful Family State;
- applied any required stale-result/request-identity gate;
- preserved its own concurrency/currentness guarantees;

it may publish that already-semantic reality through:

~~~kotlin
family.observe(state)
~~~

`observe(State)` is deliberately boring. It does not receive CURRENT State, does not run a decision
lambda, and does not infer whether an observation is still current. Those meanings belong to the
Connection that owns the provider/runtime relationship.

So the two Family entrances have different jobs:

~~~text
activate(Control)
    semantic request -> lawful Act -> immediate admitted State + Effect

observe(State)
    already-reconciled reality -> current State
~~~

Both publish into the same StateRelay.

## Concrete Connection boundary

A feature crosses provider/storage/execution/lifecycle reality through a concrete Connection when
that boundary is actually needed.

~~~text
Family
    owns State / Control / Act / Effect meaning

Connection
    owns provider/runtime dependencies
    owns Effect realization
    owns provider observation -> semantic State mapping
    owns stale async/currentness gates required by that provider
    owns its rightful lifetime/concurrency mechanics
~~~

Do not move Connection mechanics into Family merely because one patient-zero implementation needs
SharedPreferences, Media3, HTTP, a model, or coroutines.

No generic Direct-Family Connection framework is introduced by this candidate. ThemeMode and the
existing VexStream playback architecture already demonstrate the **role**; another shared interface
should be earned only if the concrete connections repeat an actual contract rather than merely share
a name.

## ThemeMode patient zero

ThemeMode now maps cleanly without teaching persistence to Family:

~~~text
ThemeModeFamily
    State
        NotSet / Light / Dark

    Control
        Select(mode)

    Effect
        PersistSelection(mode)

Select(mode)
-> act(CURRENT State, Select)
-> stay/admit
-> PersistSelection(mode)
~~~

`ThemeModeConnection` owns the runtime side:

~~~text
fresh process
-> ThemeModeStore.current()
-> ThemeModeFamily(initialState)

accepted PersistSelection(mode)
-> ThemeModeConnection
-> ordered VexExecution persistence
-> ThemeModeStore.save(mode)
~~~

Selecting the already-current mode remains meaningful:

~~~text
stay(
    effect = PersistSelection(mode)
)
~~~

State stays current while the explicit choice is still remembered durably.

## Runtime/mechanics non-ownership

Direct VexFamily no longer depends on:

~~~text
VexExecution
VexExecutionRoot
VexLogger
Store
SharedPreferences
coroutine lifetime
execution Approach
relationship identity
~~~

Those may exist in concrete runtime boundaries without becoming Family semantic nouns.


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
generic Direct-Family Connection interface beyond current concrete Connections
automatic UI observation/lifetime integration
cross-cycle relationship memory
provider / intent identity mapping
exact future Family + Relay Single/Compound composition beyond the established Compound current-State pattern
~~~

These remain separate questions; they do not require another State holder or an OutputState handoff.

<!-- [VXG RealForever][VEX-FAMILY][CURRENT-ENTRY] -->
