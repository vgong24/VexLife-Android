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

A feature owns State, Control, DisplayState, and one direct function:

~~~kotlin
override fun act(
    currentState: State,
    control: Control,
): VexAct<State>? =
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

A reader does not need to learn what state<State> or on<Control> means because those custom helpers
are no longer part of the current Family path.

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

## Act outcome

act(currentState, control) returns the authored outcome that activate(...) applies:

~~~text
null
  no Act exists for this exact State + Control pair

stay()
  current State stays current

admit(State)
  activate(...) writes State into StateRelay

optional begin(...)
  activate(...) starts the authored execution consequence
~~~

The return carrier is currently VexAct<State>. Its naming/shape remains a separate refinement
question; this change does not rename it or add another replacement type.

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
cross-cycle relationship memory and its rightful explicit supplier
provider / intent stable identity mapping
exact Family + Relay Single/Compound composition
Process OutputState -> rightful Family.currentState handoff
~~~

Repository Gradle/JVM proof remains PENDING.

<!-- [VXG RealForever][VEX-FAMILY][NATIVE-STATE-CONTROL-TRAVERSAL] -->
