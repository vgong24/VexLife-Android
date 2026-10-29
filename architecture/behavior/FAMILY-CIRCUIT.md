# Family Circuit — high-confidence boundary

[VXG RealForever]

This document intentionally preserves only the Family conclusions that survived the Origin Walk.

It does **not** promote unresolved Compound/Relay integration pressure into a production Family
hierarchy merely because an experiment compiled.

## Family is the semantic root

The current V2 Family surface is:

~~~text
<Feature>Family
  State
  Control
  DisplayState
  current
  acts
  activate(Control)
  Store?               when durable custody is actually earned
~~~

State is the Family's semantic CURRENT truth.

External callers do not receive a generic setCurrent(...). A concrete next State must first become
lawful through the Family's semantic relationship or through a concrete owner-specific reconciliation
boundary.

## Structural Act

V2 keeps the Origin-Walk removal result:

~~~text
CURRENT State + Control
-> authored Act scenario
-> immediate stay()
   or admit(concrete State)
-> optional begin(mode) execution
~~~

There is no required V2 Family.Act value hierarchy.

The authored State + Control relationship is the Act.

Payload Controls materialize that relationship from the actual CURRENT State value and actual Control
payload. isAvailable(Control) and activate(Control) use the same relationship, and activation always
re-reads CURRENT State.

## DisplayState

DisplayState is a required Family-owned projectable-output coordinate.

~~~text
Family.current
-> Family.displayState
-> UI / VexBoard / widgets / diagnostics
~~~

DisplayState does not become another semantic truth owner.

When no richer projection is earned:

~~~text
VexBehaviorV2DisplayState.Empty(current)
~~~

is the structural zero: it adds no second truth while preserving where outward/projectable meaning
belongs.

## begin / Store / accountant

An Act may optionally enter:

~~~text
begin(Replaceable | Sequenced)
  -> Operation
~~~

Execution custody does not become semantic ownership.

The currently exercised Store specialization keeps these truths distinct:

~~~text
Family.current
  semantic truth now

Family.Store
  durable custody
~~~

The Family logger/accountant may witness accepted Control, State admission and Operation outcomes.
It does not own or reverse Family truth.

## Convergence / Process boundary

Several independently-owned CURRENT truths may need to be composed. That concern already has an
accepted Relay processing foundation and remains separate from the high-confidence Family runtime in
this PR.

The process law earned by Relay / Origin pressure is:

~~~text
partial observations
+ retained CURRENT InputState
-> complete InputState
-> process(complete InputState)
-> ONE OutputState(fields...)
~~~

Process calculates relationship meaning once.

OutputState may contain several simultaneously valid processed fields. A field may have its own
ordering, precedence or availability calculation when the domain requires it; OutputState does not
need to become a sealed topology merely so downstream code can switch on it.

The following are deliberately **not** admitted by this PR:

~~~text
VexBehaviorV2CompoundFamily
VexBehaviorV2CompoundFamilyState
stateFactory
Family-level revision
post-commit act(OutputState) callback
CommittedOutput as a Family concept
sealed Output solely to recover Act selection
~~~

Those were recent formalization attempts around still-open pressure.

## SIPOC continuity

Before changing a Family layer, preserve the exact handoff:

| Layer | Supplier | Input | Process | Output | Customer |
| --- | --- | --- | --- | --- | --- |
| Act | caller + Family | CURRENT State + Control | authored scenario | stay/admit + optional begin | Family / execution |
| DisplayState | Family | CURRENT State | project | DisplayState | UI / projectable consumers |
| Single observation | rightful observed owner | observed State + retained current | reconcile | next semantic State | Family currentness boundary |
| Compound currentization | observed owners | partial + retained InputState | currentize | complete InputState | Process |
| Process | Compound currentization | complete InputState | derive once | OutputState(fields) | unresolved Family/Relay integration boundary |

A layer is not complete merely because its local code looks simpler. Its Output must still satisfy the
next consumer's expected Input.

## Open edges — held, not papered over

These remain real architecture questions:

1. **One serialized Family-current decision**
   - Control/Act and external observation may both lawfully move Family State.
   - StateRelay pressure proved the need for an exact CURRENT decision boundary.
   - No universal Family runtime mechanism is admitted yet.

2. **Cross-cycle relationship memory**
   - Some convergence relationships need continuity such as retained candidate identity.
   - Previous Output must not silently become hidden Process input.
   - The rightful supplier of any such CURRENT fact must be explicit.

3. **Provider / intent identity**
   - Stable semantic identity is preferred over local request counters.
   - A provider adapter may need to map Family intent identity to provider-owned identity before an
     observation can be trusted.
   - No universal identity mapper is admitted.

4. **Exact Family + Relay Kotlin composition**
   - Relay.Single / Relay.Compound describe useful mechanical roles.
   - Their exact nesting/integration under Family is still pressure.
   - Kotlin generic inconvenience is not authority to create another Family species.

## Review path

Read:

~~~text
1. FAMILY-CIRCUIT.md
2. VexBehaviorV2HighConfidenceTest.kt
3. VexBehaviorV2.kt
4. VexBehaviorV2Act.kt
5. VexBehaviorV2Builder.kt
6. VexBehaviorV2Store.kt / VexBehaviorV2Logger.kt when those depths matter
7. architecture/relay only when convergence/currentization is implicated
~~~

A fresh reviewer should be able to explain:

~~~text
Why is Family the semantic root?

Why does activate(Control) re-read CURRENT State?

Why is the State + Control scenario the Act rather than another Act value hierarchy?

Why is DisplayState required without becoming another truth owner?

Why does execution custody remain beneath semantic truth?

Why is InputState -> Process -> OutputState a convergence law rather than a second SCAE chain?

Which Family/Relay seams remain intentionally open?
~~~

## Source boundary

The V2 implementation in this PR is restored to the last #192 source point before the recent
Compound-Family teaching/foundation formalization began:

~~~text
89e1d27c9e28d2cca5192da9bbc10d3e493b40cd
~~~

Later #192 commits remain useful Origin/pressure history. They are not automatically promoted merely
because they are newer.

Repository Gradle/JVM proof for this cleaned PR remains PENDING.

<!-- [VXG RealForever][VEX-BEHAVIOR][FAMILY-CIRCUIT][HIGH-CONFIDENCE] -->
