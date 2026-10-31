# Family Circuit — direct readable runtime

[VXG RealForever]

The current candidate keeps the runtime call graph visible in ordinary Kotlin.

## Runtime

~~~text
Connection / caller
-> Family.activate(control)
-> Family.act(CURRENT State, Control)
-> null / stay(effect?) / admit(State, effect?)
-> immediate admitted State applied
-> Act returned
-> concrete Connection realizes optional Effect
~~~

Provider/runtime reality returns separately:

~~~text
provider/runtime facts
-> concrete Connection reconciliation/currentness gate
-> semantic Family State
-> Family.observe(State)
-> same StateRelay
~~~

There is no Acts table, builder, rule registry, hidden invoke operator, Store DSL, result-admission
callback grammar, or custom State/Control branching language in this path.

## Concrete Family

A concrete Family owns State, Control, Effect, one direct relationship function, and only a richer
DisplayState when repeated consumer interpretation earns one:

~~~kotlin
override fun act(
    currentState: State,
    control: Control,
): VexFamily.Act<State, Effect>? =
    when (currentState) {
        State.Light ->
            when (control) {
                Control.Toggle ->
                    admit(
                        State.Dark,
                        effect = Effect.PersistSelection(State.Dark),
                    )
            }

        State.Dark ->
            when (control) {
                Control.Toggle ->
                    admit(
                        State.Light,
                        effect = Effect.PersistSelection(State.Light),
                    )
            }
    }
~~~

State is the current semantic truth coordinate. Control is the semantic request/affordance input.
Effect is the runtime transformation request; it never claims the resulting State.

## Family kinship

~~~text
VexFamily<Family, State, Control, Effect>
    State   : VexFamily.State<Family>
    Control : VexFamily.Control<Family>
    Effect  : VexFamily.Effect<Family>
~~~

The Family coordinate prevents semantic roles from unrelated Families being substituted into the
same relationship.

## State ownership

~~~text
private stateRelay

Family.currentState
  -> stateRelay.value

Family.states
  -> stateRelay.asStateFlow()

activate(Control)
  -> lawful Act
  -> immediate admittedState?
  -> stateRelay

observe(State)
  -> already-reconciled semantic reality
  -> stateRelay
~~~

StateRelay remains the one live Family State holder.

`activate` and `observe` are not interchangeable:

~~~text
activate(Control)
    request meaning -> Act

observe(State)
    reality already interpreted -> State
~~~

`observe` accepts no current-State lambda and performs no stale-result inference.

## DisplayState / Projection responsibility

Reusable outward meaning preserves exact owner coordinates:

~~~text
VexFamily.DisplayState<Family, State>

VexFamily.Projection<
    Family,
    State,
    DisplayState<Family, State>
>
~~~

The Family owns the derivation path:

~~~text
State
-> displayStateFor(State)
-> Projection(
       state = that exact State,
       displayState = display derived from that exact State
   )
~~~

StateOnly is the complete default when no richer display distinction is earned.

A concrete Family that owns richer display meaning overrides displayStateFor(...). It may also
narrow projectionFor(...) / currentProjection to its concrete DisplayState type.

This makes the reusable arrow compiler-visible instead of relying on a human/AI remembering that
adjacent projection objects "probably belong together."

~~~text
FAMILY COORDINATE
    carried through State
    carried through DisplayState
    carried through Projection
~~~

Projection remains derived outward meaning, never another current-State owner.

## Act / Effect outcome

~~~text
CURRENT State + Control
-> Act?

Act
-> admittedState?
-> effect?
~~~

Use:

~~~text
null
    no lawful Act

stay(effect?)
    lawful Act; immediate State remains CURRENT

admit(State, effect?)
    lawful Act; State becomes current immediately
~~~

The exact activation ordering is:

~~~text
resolve Act
-> apply admittedState
-> return Act
-> Connection may realize effect
~~~

Family never realizes the Effect itself.

## Connection / reality boundary

Concrete Connections own runtime reality:

~~~text
Family
    semantic State / Control / Act / Effect

Connection
    Effect realization
    provider/runtime dependency
    provider facts -> semantic State mapping
    stale async/currentness gates
    lifetime/concurrency mechanics
~~~

This is the same responsibility shape already demonstrated by the existing VexStream Playback
Connection. Direct Family does not create a second generic Connection framework until repeated
Direct-Family implementations prove an exact shared contract.

### ThemeMode

~~~text
ThemeModeFamily
-> Control.Select(mode)
-> Act
   -> stay/admit
   -> Effect.PersistSelection(mode)

ThemeModeConnection
-> realizes PersistSelection
-> ThemeModeStore.save(mode)
-> owns ordered persistence runtime mechanics

fresh process
-> ThemeModeStore.current()
-> ThemeModeFamily(initialState)
~~~

Persistence is one Effect realization, not a Family concept.

## Human-visible language

~~~text
Family
State
Control
Act
Effect
DisplayState
currentState
states

activate(Control)
observe(State)
act(CURRENT State, Control)
stay(effect?)
admit(State, effect?)
~~~

Provider/storage/execution nouns remain outside the semantic Family unless they are themselves the
feature's semantic vocabulary.


## Process / Compound current State

Current accepted Relay already carries the complete processed relationship as
`VexCompoundState<InputState, OutputState>`:

~~~kotlin
protected abstract fun processStates(
    inputState: InputState,
    currentState: VexCompoundState<InputState, OutputState>,
): VexCompoundState<InputState, OutputState>
~~~

`processStates(...)` returns the coherent next InputState + derived OutputState together. The
Compound Relay then admits that whole result as one current State:

~~~text
incoming InputState
+ exact CURRENT VexCompoundState
-> processStates(...)
-> next VexCompoundState(inputState, outputState)
-> one current-State commit
~~~

If only retained Input meaning changes, current State changes while `outputStates` stays quiet. If
derived Output meaning changes too, the same current-State commit carries both changes. UI and other
consumers may read `current`, `currentInput`, or `currentOutput` without reconstructing the
relationship.

The old `OutputState -> Family.currentState` gap was incomplete because OutputState is only one
part of the processed Compound State; it is not separately admitted after processing.

This PR does not add CompoundFamily, stateFactory, Family revision, or post-process Act machinery.

## Current open edges

~~~text
cross-cycle relationship memory and its rightful explicit supplier
provider / intent stable identity mapping
exact future Family + Relay Single/Compound composition beyond the established Compound current-State pattern
~~~

Exact-head repository-owned `vexVerifyJvm` proof passed for PR #193, and the reviewed candidate tree
is the tree merged on main. This foundation does not claim device/Journey/ScreenCrawler or lived-product
acceptance from that PR.

<!-- [VXG RealForever][VEX-FAMILY][NATIVE-STATE-CONTROL-TRAVERSAL] -->
