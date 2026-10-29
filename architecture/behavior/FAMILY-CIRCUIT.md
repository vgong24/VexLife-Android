# Family Circuit — readable high-confidence boundary

[VXG RealForever]

This candidate keeps the Family language deliberately small and human-readable.

## Family

~~~text
<Feature>Family
  State
  Control
  DisplayState
  currentState
  activate(Control)
  Store? / begin(...) when earned
~~~

The live root is:

~~~kotlin
VexFamily<State, Control>
~~~

The two generics bind the State and Control contracts. There is no recursive Family self-generic in
the new path.

## State + Control -> Act

Act remains the authored relationship, not another feature vocabulary type.

~~~kotlin
state<State.Idle> {
    on<Control.Load> { currentState, control ->
        admit(...)
        begin(...) {
            ...
        }
    }
}
~~~

Read it literally:

~~~text
CURRENT State
+ incoming Control
-> authored Act meaning
-> stay() or admit(State)
-> optional begin(...)
~~~

Both singleton and payload Control forms receive currentState and control in the lambda so the causal
inputs stay visible.

The public Family entrance remains:

~~~kotlin
family.activate(control)
~~~

Family does not expose a resolve(...) API or an Act carrier.

## DisplayState

~~~text
Family.currentState
-> Family.displayState
-> UI / VexBoard / projectable consumers
~~~

DisplayState does not own another semantic truth.

When no richer projection is earned:

~~~kotlin
VexDisplayState.Empty(currentState)
~~~

is the structural zero.

## Functions and nouns

Current language rule:

~~~text
NOUNS
  Family
  State
  Control
  DisplayState
  InputState
  OutputState
  Store

VERBS
  activate(...)
  admit(...)
  begin(...)
  save(...)
  process(...)
~~~

The state/on grammar exists only to keep State + Control traversal canonical and visible. Internal
matching helpers are not architecture vocabulary.

## Convergence remains a separate open integration boundary

The accepted Relay/Origin process law remains:

~~~text
partial observations
+ retained CURRENT InputState
-> complete InputState
-> process(complete InputState)
-> ONE OutputState(fields...)
~~~

Process remains an ordinary function:

~~~kotlin
fun process(
    inputState: InputState,
): OutputState
~~~

This PR does not introduce CompoundFamily, stateFactory, Family-level revision, post-commit
act(OutputState), or another SCAE chain after Process.

## SIPOC continuity

| Layer | Supplier | Input | Process | Output | Customer |
| --- | --- | --- | --- | --- | --- |
| Act | caller + Family | currentState + Control | state/on authored meaning | stay/admit + optional begin | Family / execution |
| DisplayState | Family | currentState | project | DisplayState | UI / projectable consumers |
| Compound currentization | observed owners | partial + retained InputState | currentize | complete InputState | process |
| Process | Compound currentization | complete InputState | calculate once | OutputState(fields) | open Family/Relay integration boundary |

A local simplification is not complete if its Output no longer satisfies the next customer's Input.

## Intentionally open

~~~text
one serialized Family-current decision when Control and observation collide

cross-cycle relationship memory and its rightful explicit supplier

provider/intent stable identity mapping

exact Family + Relay Single/Compound composition

whether the remaining State/Control generics can be reduced further without losing a real contract
~~~

## Review path

~~~text
1. FAMILY-CIRCUIT.md
2. VexFamilyHighConfidenceTest.kt
3. VexFamily.kt
4. VexActsBuilder.kt
5. VexAct.kt only when inspecting internal Act carriers
6. VexFamilyStore.kt / VexFamilyLogger.kt when those depths matter
7. architecture/relay only when convergence/currentization is implicated
~~~

Repository Gradle/JVM proof remains PENDING.

<!-- [VXG RealForever][VEX-BEHAVIOR][FAMILY-CIRCUIT][READABLE-HIGH-CONFIDENCE] -->
