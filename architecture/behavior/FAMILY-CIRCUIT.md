# Family Circuit — direct readable runtime

[VXG RealForever]

The current candidate keeps the runtime call graph visible in ordinary Kotlin.

## Runtime

~~~text
caller / UI / provider / Store observation
-> typed Family Control
-> Family.activate(control)
-> concrete Family.act(currentState, control)
-> when (currentState)
-> when (control)
-> null / stay() / admit(State)
-> optional saveLatest(State)
-> StateRelay / storage execution
~~~

There is no separately constructed Acts table, builder, rule registry, hidden invoke operator, or
custom State/Control branching DSL between activate(...) and the concrete Family logic.

## Concrete Family

A feature owns State, Control, one direct function, and only a richer DisplayState when real consumers earn one:

~~~kotlin
override fun act(
    currentState: State,
    control: Control,
): VexFamily.Act<State>? =
    when (currentState) {
        State.Light ->
            when (control) {
                Control.Toggle ->
                    admit(State.Dark)
            }

        State.Dark ->
            when (control) {
                Control.Toggle ->
                    admit(State.Light)
            }
    }
~~~

Both traversals are Kotlin-native exhaustive when expressions over feature-owned sealed types.
State remains first because current State is the truth against which incoming Control receives
meaning.

Do not use `else` at this semantic boundary. New State or Control vocabulary should create
compiler-visible relationship work. Controls may be grouped in one branch only when they have the
same exact reviewed outcome.

A reader does not need to learn what state<State> or on<Control> means because those custom helpers
are no longer part of the current Family path.

## Family kinship

State and Control are not merely arbitrary non-null types. One concrete Family coordinate binds the
two vocabularies:

~~~text
VexFamily<Family, State, Control>
    Family  : VexFamily<Family, State, Control>
    State   : VexFamily.State<Family>
    Control : VexFamily.Control<Family>
~~~

Family-owned children that carry semantic State require that Family-bound State type, so
DisplayState / Store / Act cannot be formed around an arbitrary unrelated value type.

The recursive constraint exists only at the root to prove the coordinate is the actual owning
Family. Descendants then carry the already-bound State relationship instead of repeating the whole
root equation.

## State ownership

~~~text
private stateRelay
  MutableStateFlow<State>

Family.currentState
  -> stateRelay value

Family.states
  -> stateRelay.asStateFlow()

activate(...)
  -> Act admitting State
  -> setCurrentState(State)
  -> stateRelay.value = State
~~~

StateRelay remains the one live State holder.

## DisplayState responsibility

Display interpretation has an explicit place in the Family tree without requiring every Family to
invent another state vocabulary:

~~~text
VexFamily
-> DisplayState
   -> StateOnly(currentState)

ConcreteFamily
-> DisplayState        # only when a richer repeated classification is earned
~~~

`VexFamily.DisplayState.StateOnly` is a complete default, not an unfinished placeholder. It tells a
fresh reader that DisplayState already has a rightful owner and that no parallel ViewState / UiState /
PresentationState should be created merely to fill a perceived gap.

## Execution relationship identity

Family owns the relationship composition once:

~~~text
activate(control)
-> exact live Family
-> exact Control belonging to that Family
-> Control name/type included automatically
-> optional typed Control.resourceId
-> VexFamily.RelationshipId<Family>
-> VexExecution.RelationshipId
-> maintained execution coordination
~~~

Concrete maintained Save authoring stays intentionally small:

~~~kotlin
saveLatest(
    nextState,
)
~~~

~~~text
Control identity
    always included by Family foundation

Control.resourceId
    optional typed narrowing when this Control needs resource identity

RelationshipId
    live Family + exact Control identity + optional exact ResourceId object

SaveLatest
    storage execution for the triggering Control using Latest relationship semantics
~~~

The full Control payload is not automatically identity. If Control.Play(trackId, position) needs
coordination per track, Play exposes resourceId = trackId; position remains request payload.
Enum Control entries remain distinct exact Control identities.

There is no generic Begin authoring tree or arbitrary execution argument bag on the Direct Family
surface.

## Logger boundary

Family consumes logging through the independent logger root:

~~~text
VexFamily
-> VexLogger.Sink
   -> VexLogger.Event.Family.*
~~~

The Sink is private to the Family runtime. Concrete `act(...)` authoring does not receive logger
access, so inspecting relationship meaning cannot accidentally emit accepted runtime facts.

## Store / Save relationship

The current earned storage path is:

~~~text
VexFamily
-> Store<State>
   -> save(State)
   -> WriteResult
      -> Success
      -> Failure

VexFamily
-> StateStore<State>
   -> Store<State>
   -> observableState

VexFamily
-> Act
   -> immediate admitted State?
   -> optional Save<State>
~~~

StateStore adds only the capability Direct Family actually needs generically: durable State
observation. Concrete storage may keep local read/reset helpers without making them architecture.

Save success/failure are storage/execution facts. They have no direct State-writing path.

## Act outcome

act(currentState, control) returns the authored outcome that activate(...) applies:

~~~text
null
  no lawful Act exists for this exact CURRENT State + Control

stay()
  lawful Act; immediate State remains CURRENT

admit(State)
  lawful Act; activate(...) admits State to StateRelay before Save starts

optional saveLatest(State)
  activate(...) starts the authored durable Save after immediate State meaning is applied
~~~

The return carrier is VexFamily.Act<State>. The source does not carry a second result-side State
admission grammar.

## Human-visible language

~~~text
Family
State
Control
DisplayState
currentState
states

activate(control)
act(currentState, control)
when (currentState)
when (control)
null
stay()
admit(State)
saveLatest(State)
process(...)
~~~

Except for the Vex domain functions themselves, State/Control traversal is ordinary Kotlin.

## One semantic State doorway

StateRelay is the one live Family State holder, and act(CURRENT State, Control) is the only semantic
doorway that may author Family State:

~~~text
incoming semantic cause
+ exact CURRENT State
-> typed Control
-> act(CURRENT State, Control)
-> null / stay / admit
-> same StateRelay
~~~

Provider/repository reconciliation and durable Store observation do not receive a second setter-like
path. They return typed Control meaning:

~~~text
provider / repository / Store observation
-> concrete Family Control
-> activate(control)
-> act(CURRENT State, control)
-> stay / admit / null
~~~

The runtime serializes activate(...) so each Control resolves against exact CURRENT State.

A future asynchronous completion that changes Family meaning must also return as a typed Control.
The feature must carry whatever identity is truly required to reject obsolete results; serialization
is not claimed to solve stale-result identity by itself.

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
