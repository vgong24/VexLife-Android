# Family Circuit

[VXG RealForever]

This is the current construction guide for the Family Circuit foundation.

A fresh reader should be able to answer:

~~~text
who owns CURRENT truth?
what request is being made?
what does the current relationship mean?
when does a consequence become CURRENT?
who owns the resulting truth?
how does that truth return?
~~~

## Family root

Every Family has one CURRENT semantic State.

~~~text
VexFamily
  current
  displayState
  protected admit(State)
~~~

admit(State) is protected. External callers do not receive a generic setCurrent(...).

displayState is the projectable Family-owned output coordinate. Empty(current) adds no second truth.

## Control Family

Use VexControlFamily when another caller makes a semantic request through a Control.

~~~text
CURRENT State + Control
-> resolveAct(...)
-> Act
-> stay
   or
-> admit(next State)
~~~

The same resolveAct(...) law powers availability and activation. Activation always re-reads CURRENT State.

## Compound Family

Use VexCompoundFamily when one Family owns the CURRENT meaning of several observed truths.

~~~text
partial Input
+ retained Input
-> Input.latest(incoming)
-> complete latest Input
-> process(latestInput)
-> Output
-> COMMIT Compound Family State
-> Act only when Output changed
~~~

A concrete Compound Family names its own State:

~~~text
MultiCompoundState
  inputState
  outputState
  revision
~~~

That State is MultiFamily.current. There is no sibling Relay that owns another copy of the same relationship truth.

## Input

Input owns current-fact formation.

~~~text
Input.latest(incoming)
  partial observation
  + retained current facts
  -> complete latest Input
~~~

If Input contains mutable-backed data, deepCopy() owns detachment at the ownership boundary.

latest(...) does not calculate relationship meaning.

## Process

process(...) owns relationship calculation only.

~~~text
override fun process(
    latestStateInput: InputState,
): OutputState
~~~

Read it as:

~~~text
here is everything CURRENT
-> what does it mean?
-> return Output
~~~

process(...) does not mutate another Family, execute an Effect, consult previous Output as hidden
memory, or re-currentize partial Input.

If tomorrow's calculation needs a fact, that fact belongs in the current Input circuit.

## Output

Output is the CURRENT consequence of the complete Input relationship.

~~~text
Output != previous process memory
Output != another Family's State
Output != an event ledger
~~~

An Input-only change may advance Compound State revision while leaving Output unchanged. That does not
produce another Act.

## Commit and Act

The framework commits the next Compound State before Act runs.

~~~text
process
-> Output
-> COMMIT Family.current
-> committed Output consequence
-> Act
~~~

Only an Output-changing commit creates VexCommittedOutput.

act(...) runs after commit and outside the process lock. That allows resulting truth to return as a new
observation without re-entering a half-committed calculation.

## Ownership round trip

~~~text
Family A CURRENT
Family B CURRENT
Family C CURRENT
        |
        v
partial Input observations
        |
        v
Input.latest(...)
        |
        v
process(...)
        |
        v
Output
        |
        v
COMMIT Compound Family.current
        |
        v
Act
        |
        v
rightful target Family Control
        |
        v
target Family CURRENT
        |
        v
observe as NEW Input
        |
        +-------> process again
~~~

The target Family owns whether its State actually changes. The Compound Family never writes another
Family's State directly.

## Teaching suite

Read VexFamilyCircuitTestSampleSuite.

Start with:

~~~text
createSingleFamily()
createListFamily()
createBooleanFamily()
createMultiFamily(...)
~~~

They demonstrate:

~~~text
SingleFamily
  one current value

ListFamily
  nested list State

BooleanFamily
  Boolean State

MultiFamily
  several Family currents
  -> MultiInputState
  -> MultiOutputState
  -> MultiCompoundState
~~~

The names are structural on purpose. The sample is not an app feature.

## Current standard boundary

This PR establishes:

~~~text
Family CURRENT-State ownership
Control -> Act -> immediate State admission
DisplayState projectable coordinate
Compound Input currentization
process-only relationship calculation
Output consequence ownership
Compound State commit
Output-change-only Act admission
target Family Control boundary
observation -> NEW Input
~~~

This PR does not standardize asynchronous Effect custody:

~~~text
Replaceable
Sequenced
cancellation
long-running Operation result custody
~~~

Those remain the next execution-depth question and must preserve the Family Circuit laws above.

## Review questions

A reviewer should be able to explain:

1. Why MultiFamily.current is MultiCompoundState rather than a sibling Relay State.
2. Why MultiInputState.latest(...) is separate from MultiFamily.process(...).
3. Why unchanged Output does not create another Act.
4. Why act(...) runs after commit.
5. Why one Family requests another Family through Control rather than mutating its State.
6. Why observed target truth returns as NEW Input.
7. Which responsibilities remain outside this baseline.

// [VXG RealForever]
