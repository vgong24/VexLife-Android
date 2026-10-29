# Family Circuit

[VXG RealForever]

This is the clean current-entry guide for the Family Circuit foundation.

It describes the implementation in this PR, not the Origin-Walk history that produced it.

## Family root

Every live Family has one CURRENT semantic State.

~~~text
VexFamily
  current
  displayState
  protected admit(State)
~~~

admit(State) is protected. External callers do not receive a generic setCurrent(...).

displayState is the Family-owned projectable output coordinate. The default
VexBehaviorV2DisplayState.Empty(current) adds no second truth.

## Control-driven Family

Use VexBehaviorV2Family when callers make semantic requests through Controls.

~~~text
Family
  State
  Control
  acts
  activate(Control)
~~~

Acts are authored with vexActs(...).

~~~text
CURRENT State + Control
-> resolve/materialize VexBehaviorV2Act
-> immediate stay()
   or
-> immediate admit(next State)
-> optional begin(...)
~~~

isAvailable(Control) and activate(Control) use the same CURRENT State + Control law.

Activation always re-reads CURRENT State. Earlier UI availability never becomes activation authority.

## Compound Family

Use VexBehaviorV2CompoundFamily when one Family owns the CURRENT meaning of several observed truths.

A concrete Compound Family gives its State ordinary readable names:

~~~text
MultiInputState
MultiOutputState
MultiCompoundState
MultiFamily
~~~

and:

~~~text
MultiFamily.current
  = MultiCompoundState(
      inputState,
      outputState,
      revision,
    )
~~~

There is no sibling Compound Relay owning a second copy of the same relationship truth.

## Input currentization

Input implements VexBehaviorV2CompoundInputState.

~~~text
partial incoming Input
+ retained CURRENT Input
-> Input.latest(incoming)
-> complete latestStateInput
~~~

latest(...) owns partial-input currentization only.

If Input contains mutable-backed data, deepCopy() owns detachment at the ownership boundary.

## Relationship calculation

The concrete Compound Family implements:

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

process(...) does not:

~~~text
mutate another owner
execute an Effect
consult previous Output as hidden process memory
re-currentize partial Input
~~~

If a future calculation needs a fact, that fact belongs in the CURRENT Input circuit.

## Output

Output implements VexBehaviorV2CompoundOutputState.

Output is the CURRENT consequence of the complete relationship.

~~~text
Output != previous process memory
Output != another Family's State
Output != event ledger
~~~

An Input-only currentness change may advance the Compound State revision while leaving Output unchanged.

That does not create another Act consequence.

## Commit then Act

The Compound Family infrastructure owns the repeated circuit:

~~~text
partial Input
-> latest(...)
-> process(...)
-> Output
-> assemble next Compound State
-> COMMIT Family.current
-> VexBehaviorV2CommittedOutput only when Output changed
-> act(...)
~~~

act(...) runs after the next Compound State is already CURRENT and after the process/commit lock is
released.

That matters because an Act may request another rightful Family to change, then observe that resulting
CURRENT truth back into this Family without re-entering a half-committed calculation.

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

One owner remains responsible for each truth.

The Compound Family never writes another Family's State directly.

## Teaching suite

Read:

~~~text
VexBehaviorV2FamilyCircuitTestSampleSuite
~~~

Start with the constructor forms at the top:

~~~text
createSingleFamily()
  -> SingleFamily
  -> State(value)

createListFamily()
  -> ListFamily
  -> State(groups -> items)

createBooleanFamily()
  -> BooleanFamily
  -> State(enabled)

createMultiFamily(...)
  -> MultiFamily
  -> MultiInputState
  -> MultiOutputState
  -> MultiCompoundState
~~~

The names are intentionally structural rather than product-specific.

MultiFamily is the complete circuit example.

When its committed Output requires a list change:

~~~text
MultiFamily act
-> ListFamily.Control.Add(...)
-> ListFamily.activate(Control)
-> ListFamily.current changes
-> MultiFamily observes ListFamily.current as NEW Input
-> process again
~~~

The suite also proves that changing Input while leaving Output meaning unchanged advances relationship
currentness without fabricating another Act.

## Execution depth

The exact current V2 source also carries the existing begin(mode) execution capability:

~~~text
Replaceable
Sequenced
~~~

That execution substrate is included because the current VexBehaviorV2Family implementation depends
on it.

The Family Circuit law remains:

~~~text
semantic relationship truth is established first
execution custody does not become another semantic truth owner
~~~

Longer-running result/currentness pressure may continue to refine execution without changing the
ownership circuit above.

## Review path

A fresh reviewer should read in this order:

~~~text
1. FAMILY-CIRCUIT.md
2. VexBehaviorV2FamilyCircuitTestSampleSuite.kt
3. VexBehaviorV2.kt
4. VexBehaviorV2Act.kt
5. VexBehaviorV2Builder.kt
6. VexBehaviorV2Store.kt / VexBehaviorV2Logger.kt when those depths matter
~~~

Then explain:

~~~text
Why is MultiFamily.current MultiCompoundState?

Why is latest(...) separate from process(...)?

Why does unchanged Output not create another Act?

Why does act(...) run only after commit?

Why does MultiFamily enter ListFamily through Control?

Why does ListFamily.current return as NEW Input?

What does execution own, and what does it NOT own?
~~~

## Source relationship

This clean PR ports the current winning source from the latest Origin-Walk candidate.

Historical pressure tests and app-specific specimens are intentionally not included here.

// [VXG RealForever]
