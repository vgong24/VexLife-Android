# Family Circuit — direct readable runtime

[VXG RealForever]

The current candidate keeps the runtime call graph visible in ordinary Kotlin.

## Runtime

~~~text
caller / UI
-> Family.activate(control)
-> concrete Family.act(currentState, control)
-> when (currentState)
-> when (control)
-> stay() or admit(State)
-> optional begin(...)
-> StateRelay / execution
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

Family owns the relationship composition once and keeps it inside the Family contract:

~~~text
activate(control)
-> exact live Family
-> exact Control belonging to that Family
-> Control name/type included automatically
-> optional typed Control.resourceId
-> VexFamily.RelationshipId<Family>
-> VexExecution.RelationshipId<Family>
-> Approach
-> VexExecution-owned Job custody
~~~

Concrete Family authoring stays intentionally small:

~~~kotlin
begin {
    ...
}

beginEarliest {
    ...
}

beginLatest {
    ...
}

beginQueued {
    ...
}
~~~

Those functions are Family children. They do not expose Execution identity construction to the
caller. Family supplies its own typed relationship key and only the authored Approach changes.

~~~text
Control name
    always included by Family foundation

Control.Resource<Family, ExactId>
    exact typed narrowing when this Control needs a ResourceId

RelationshipId<Family>
    live Family + Control kind/name + optional exact ResourceId object

Approach
    Independent / Earliest / Latest / Queued
~~~

The full Control payload is not automatically identity. If `Control.Play(trackId, position)` needs
coordination per track, `Play` declares `Control.Resource<Family, TrackId>`; the actual TrackId
object is preserved in the relationship while `position` remains request payload.

Execution does not add another address around that Family relationship:

~~~text
NO Family -> public execution Handle
NO Family -> public execution InstanceId
NO Family -> callable-addressed forceStop / joinStop
NO Family -> SourceRef duplicate identity

VexExecution.SourceRef
    diagnostic source location only
~~~

If Family-owned work needs later control, the Family-owned RelationshipId is the address. Execution
owns the actual Jobs, cancellation, joining, queueing and overlap mechanics beneath that key.

Family StateStore observation follows the same rooted law:

~~~text
live Family
+ exact StateStore object
-> Family-owned StateStore observation relationship
-> VexExecution observation custody
-> observeStore(CURRENT, stored)
-> admitCurrent(...)
-> same StateRelay
~~~

The observation key is contained in Family; it is not a free-standing public operation and is not
derived from `StateStore::observableState` callable identity.

This keeps the semantic tree explicit:

~~~text
VexFamily
-> Control / ResourceId
-> RelationshipId
-> Begin
   -> VexExecution dependency

VexExecution
-> owns Job mechanics
-> does not become Family's semantic owner
~~~

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

The current earned storage path remains tree-shaped. A plain Store supports authored Save; a StateStore
specialization is earned only when durable truth is exactly Family State:

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
   -> get()
   -> forget()

VexFamily
-> Act
   -> Begin
      -> Save
         -> Results
            -> Success
            -> Failure
~~~

Family does not expose a broad untyped Store and rediscover State semantics through runtime casts.
The StateStore specialization is explicit in the type system and exists only where durable truth is
actually Family State. Durable Store truth still does not become a second live semantic State holder.

There is also no generic Operation hierarchy around the one current Save operation. A common
operation parent must be earned by another real operation, not anticipated.

## Act outcome

act(currentState, control) returns the authored outcome that activate(...) applies:

~~~text
noAct()
  reviewed absence: no lawful Act exists for this exact State + Control pair

stay()
  current State stays current

admit(State)
  activate(...) writes State into StateRelay

optional begin(...)
  activate(...) starts the authored execution consequence
~~~

The return carrier is `VexFamily.Act<State>`. Act is a Family child rather than another Vex root.
Its optional Begin child is opened only through Family's protected Act-authoring surface.

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
noAct()
stay()
admit(State)
begin(...)
process(...)
~~~

Except for the Vex domain functions themselves, State/Control traversal is ordinary Kotlin.

## StateRelay currentness

StateRelay is still the one live Family State holder. The missing reusable law is the admission
boundary around that holder:

~~~text
incoming Family-owned cause
+ exact CURRENT State
-> one serialized non-suspending decision
-> next CURRENT State
~~~

Control activation resolves `act(CURRENT State, Control)` and commits its immediate State inside that
same boundary. Delayed Begin results also re-enter StateRelay and may author their result against
exact CURRENT State:

~~~kotlin
success {
    admit { current ->
        if (/* still lawful */) nextState else current
    }
}
~~~

No revision/generation number is invented by Family. Currentness meaning remains feature-owned.

Already-reconciled external truth uses the same StateRelay boundary without becoming a fake Control:

~~~text
provider / repository observation
-> rightful Connection/source reconciliation
-> concrete Family source method
-> admitCurrent { CURRENT -> next semantic State }
-> StateRelay
~~~

`admitCurrent` is protected; there is still no public generic State setter.

A State-shaped Family Store is itself a StateRelay dependency; it does not require another observer owner:

~~~text
StateStore.observableState
-> execution-owned observation lifetime
-> observeStore(CURRENT, stored)
-> concrete Family decision
-> admitCurrent
-> same StateRelay
~~~

`StateStore` carries the already-earned `observableState / get / save / forget` capability. Plain
`Store` remains the smaller save-only capability for Families that have not earned durable State
observation.


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
