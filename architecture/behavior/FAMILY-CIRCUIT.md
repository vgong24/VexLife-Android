# Family Circuit — direct readable runtime

[VXG RealForever]

The current candidate keeps the runtime call graph visible in source.

## Family runtime

~~~text
caller / UI
-> Family.activate(control)
-> Family.act(currentState, control)
-> state<State> / on<Control>
-> stay() or admit(State)
-> optional begin(...)
-> StateRelay / execution
~~~

There is no separately constructed `VexActs` table, no `VexActsBuilder`, no stored `VexActRule`,
and no hidden `operator fun invoke` between activate(...) and the concrete Family's State + Control
logic.

## Concrete Family

A concrete Family owns the direct Act function:

~~~kotlin
override fun act(
    currentState: State,
    control: Control,
): VexAct<State>? =
    state<State.Light>(
        currentState,
        control,
    ) { _, control ->
        on(
            expectedControl = Control.Toggle,
            control = control,
        ) {
            admit(State.Dark)
        }
    }
        ?: state<State.Dark>(
            currentState,
            control,
        ) { _, control ->
            on(
                expectedControl = Control.Toggle,
                control = control,
            ) {
                admit(State.Light)
            }
        }
~~~

`state` and `on` are runtime branching helpers called from that function. They do not register a
relationship table during Family construction.

## State ownership

~~~text
private StateRelay
  MutableStateFlow<State>

Family.currentState
  -> StateRelay.value

Family.states
  -> StateRelay.asStateFlow()

activate(...)
  -> Act.Admit(State)
  -> setCurrentState(State)
  -> StateRelay.value = State
~~~

StateRelay remains the one live State holder.

## Act outcome

The direct act(...) function returns one small Act outcome:

~~~text
no Act
  null

stay
  current State remains current

admit(State)
  activate(...) writes State into StateRelay

optional begin(...)
  activate(...) starts the authored execution consequence
~~~

There is no separate StateDisposition object.

## Human-visible nouns and verbs

~~~text
NOUNS
  Family
  State
  Control
  DisplayState
  Act
  StateRelay
  InputState
  OutputState
  Store

VERBS
  activate(...)
  act(...)
  state(...)
  on(...)
  stay()
  admit(...)
  begin(...)
  process(...)
~~~

## Process remains separate

~~~kotlin
fun process(
    inputState: InputState,
): OutputState
~~~

The current unresolved seam remains:

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

<!-- [VXG RealForever][VEX-BEHAVIOR][FAMILY][DIRECT-RUNTIME] -->
