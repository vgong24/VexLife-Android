
# Vex Behavior

[VXG RealForever]

This module preserves the geometry of **what a human can do from what is true now**.

## Start here

- IDE/code navigation: `VexBehaviorDocs`
- Canonical family/package construction pattern:
  [`VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)
- Review pressure/evidence:
  [`REVIEW-CHECKPOINT.md`](REVIEW-CHECKPOINT.md)

## Four semantic moves

~~~text
STATE
    what is actually true now

CONTROL
    stable human-facing affordance

ACT
    what that Control means from current State

EFFECT
    runtime transformation that should be attempted
~~~

Core equations:

~~~text
Control + CURRENT State
-> Act

CURRENT State + Act
-> Effect + admitted State

CURRENT State + Controls
-> Projection

Projection
-> optional feature DisplayState
-> UI treatment
~~~

Behavior is the semantic rulebook for lawful relationships among State / Control / Act / Effect.
A concrete Connection is the reality boundary that observes provider truth, realizes Effects, and
reconciles the resulting State.

> Act asks. Effect attempts the travel. State tells us what reality answered. Control decides which
> Act one affordance represents from current truth.

## Core laws

1. **State is truth.** Effect never claims provider success.
2. **Resolve from current State.** Do not depend on hidden traversal history.
3. **State kind versus flavor is semantic.** New types are earned by changed behavioral topology.
4. **Control != Act.** One stable affordance may mean different Acts from different States.
5. **Act != Effect.** Human/product language stays separate from provider/runtime operation language.
6. **Admission != eventual provider outcome.** `.admit<State>()` describes immediate semantic
   topology after Effect acceptance.
7. **Family safety is compile-time.** One F-bounded family ties State / Control / Act / Effect
   together.
8. **VexPort currentness is activation currentness.** Async completion gates remain concrete-owner
   responsibilities.
9. **Concurrency remains concrete.** VexPort does not prescribe locks, dispatchers, or Relay
   serialization.
10. **Convergence stays above complete local truths.** Do not cross-multiply independent domains
    into one State family.
11. **Control meaning is semantic-first.** Prefer `Available(Act)` / `Unavailable`; Boolean
    availability is derived convenience.
12. **Every relationship terminates explicitly.** Every `on(...)` must end in `.stay()` or
    `.admit<State>()`.
13. **DisplayState is a deterministic view classification, not another truth owner.** Use it only
    when views would otherwise repeat conditional treatment of an already-derived Projection.

## Durable package pattern

A production family normally begins as:

~~~text
<Name>Docs.kt
<Name>Family.kt
<Name>State.kt
<Name>Control.kt
<Name>Act.kt
<Name>Effect.kt
<Name>Behavior.kt
~~~

Optional Connection / Host / Pending / Presentation / Types files are added only when those
responsibilities are real.

When one family gets large, **split by independent behavioral question, not by line count**.

See the meta-blueprint for the full split law and VexStream Playback mapping.

## Prime implementation

Current production-shaped example:

~~~text
app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/
~~~

Search the code symbol:

~~~text
VexStreamPlaybackDocs
~~~

from there.

## Experience membrane

When Compose repeatedly needs the same Behavior plumbing, the feature may expose one local
`<Name>Experience` boundary:

~~~text
<Name>Experience
    displayState
    activate(Control)
~~~

The Experience observes the internal Port/Projection and supplies the final DisplayState. It does
not own semantic truth.

VexStream Playback is the first example:

~~~text
VexStreamPlaybackExperience
    -> VexStreamPlaybackDisplayState
    -> activate(VexStreamPlaybackControl)
~~~

Mini Player and Context Dock no longer assemble Playback Connection + Projection + DisplayState
themselves.

A generic `VexExperience` abstraction remains held until a second real feature proves the same
surface.

## Deliberately held

The current kernel does not claim generic solutions for:

~~~text
runtime request payloads
generic UIElement -> Control binding
generic heterogeneous convergence envelope
generic concurrency/locking
generic async failure/result grammar
~~~

Let real consumers earn those extensions.

<!-- [VXG RealForever][VEX-BEHAVIOR] -->
