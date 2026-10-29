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

## Execution RelationshipId material

Family passes the semantic relationship directly to Execution:

~~~text
activate(control)
-> Act.Begin(
       approach,
       arguments
   )
-> VexExecution.RelationshipId(
       owner = this live Family,
       trigger = Control kind,
       arguments = ordered tuple
   )
-> maintained execution map key
~~~

There is no intermediate Family execution ID.

Authoring stays small:

~~~kotlin
beginLatest {
    ...
}

beginLatest(
    accountId,
    itemId,
) {
    ...
}
~~~

The RelationshipId answers **which executions belong together**. `VexExecution.Approach` answers
**how overlaps are handled**.

~~~text
RelationshipId = owner + trigger + arguments
Approach     = Independent / Earliest / Latest / Queued
~~~

Zero arguments means Family + Control only. Arguments stack positionally when one Control needs
narrower subrelationships. Zero arguments and one explicit null are distinct.

Use stable immutable argument values. The full Control payload is request evidence; it is not
automatically execution RelationshipId material.

Runtime coordination is temporary: each completed Instance leaves its RelationshipId entry, and the
entry is deleted when the final unfinished Instance leaves. A later equal RelationshipId therefore
coordinates a fresh runtime occurrence rather than inheriting completed membership.

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

The current earned storage path is deliberately narrow and tree-shaped:

~~~text
VexFamily
-> Store<State>
   -> save(State)
   -> WriteResult
      -> Success
      -> Failure

VexFamily
-> Act
   -> Begin
      -> Save
         -> Results
            -> Success
            -> Failure
~~~

Family does not expose a broad Store and rediscover a narrower StateStore through runtime casts.
The Store contract contains only the operation the current Family runtime consumes. Durable Store
truth does not become a second live semantic State holder.

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

## Process remains separate

~~~kotlin
fun process(
    inputState: InputState,
): OutputState
~~~

The unresolved seam remains visible:

~~~text
OutputState
-> rightful StateRelay / Family.currentState handoff
~~~

This PR does not add CompoundFamily, stateFactory, Family revision, or post-process Act machinery.

## Current open edges

~~~text
serialized competing-cause State decision / stale async result
    -> held until a concrete Family proves the rightful currentness identity
cross-cycle relationship memory and its rightful explicit supplier
provider / intent stable identity mapping
exact Family + Relay Single/Compound composition
Process OutputState -> rightful Family.currentState handoff
~~~

Repository Gradle/JVM proof remains PENDING.

<!-- [VXG RealForever][VEX-FAMILY][NATIVE-STATE-CONTROL-TRAVERSAL] -->
