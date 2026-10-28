
# Vex Behavior

[VXG RealForever]

This module preserves the geometry of **what a human can do from what is true now**.

## Start here

- IDE/code navigation: `VexBehaviorDocs`
- Canonical family/package construction pattern:
  [`VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)
- Review pressure/evidence:
  [`REVIEW-CHECKPOINT.md`](REVIEW-CHECKPOINT.md)

## SCAE architecture

**SCAE** names the Vex semantic architecture built around four explicit roles:

~~~text
STATE
    what is actually true now

CONTROL
    stable human-facing affordance or request identity

ACT
    what that Control means from current State

EFFECT
    runtime transformation that may be attempted
~~~

SCAE is more than an acronym for four source types. It names the architectural discipline that keeps
current truth, request identity, semantic meaning, runtime consequence, reality reconciliation and
cross-truth composition legible as separate questions.

Local Behavior uses the core equations:

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

Behavior is the local semantic rulebook for lawful SCAE relationships.

A concrete Connection closes the local reality loop:

~~~text
Effect
-> provider/runtime realization
-> observed truth
-> current State
~~~

Projection / optional DisplayState / UI are read and presentation boundaries around that semantic
truth; they are not additional SCAE roles.

When several independently-owned States have meaning only in combination, **Convergence Relay is the
fractal composition pattern of SCAE**:

~~~text
State A + State B + ...
-> Convergence
-> explicit relationship truth
-> optional bounded Control/request
-> rightful SCAE family
~~~

Convergence is not a fifth sequential SCAE stage. It may consume current truths and feed a bounded
Control/request back into another family at whatever semantic scale the relationship is earned.

Therefore a review question such as **"does this follow SCAE?"** includes both:

~~~text
local behavior
    State + Control -> Act
    State + Act -> Effect + admitted State
    Effect -> Connection -> observed State

cross-truth composition, when applicable
    independently-owned States
    -> Convergence relationship
    -> bounded Control/request
    -> rightful SCAE family
~~~

> Act names what the request means now. Effect names what reality may be asked to do about that
> meaning. State remains the current truth used to resolve both.

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
10. **Convergence is fractal SCAE composition.** Keep independently-owned local truths complete;
    combine them into explicit relationship truth rather than cross-multiplying them into one State
    family. Any requested correction returns through the rightful family's Control boundary.
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

## Prime implementations

Current production-shaped examples:

~~~text
app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/
app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/queue/
~~~

Search the code symbols:

~~~text
VexStreamPlaybackDocs
VexStreamQueueDocs
~~~

Playback answers Play/Pause. Queue independently answers Previous/Next/Shuffle/Clear. Their
separation is semantic: nearby media-player controls do not become one Behavior family merely
because the UI displays them together.

Queue Shuffle demonstrates that a one-shot action should not acquire fake persistent UI state merely
because the rendering API supports a selected treatment. Queue Clear demonstrates the truth law:
requesting a provider mutation can legitimately `.stay()` in the current State kind until provider
observation earns a different truth.

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

Queue is the second focused membrane. It exposes its Behavior Projection directly because no extra
view classification is currently earned; Mini Player and Context Dock still consume one
`VexStreamQueueExperience` rather than reinterpreting Media3 predicates independently.

A generic `VexExperience` abstraction remains held until a second real feature proves the same
surface across features, not merely two VexStream families.

## V2 two-scope refinement — sibling experiment, not replacement

The accepted V1 kernel remains the production-used contract. V2 exists beside it so the ThemeMode
Origin Walk can be proven in ordinary Kotlin without changing VexStream/Queue semantics in place.

The V2 proof must **consume Origin language rather than translate it into a second vocabulary**.

Current mapping:

~~~text
ORIGIN WALK                         KOTLIN V2
────────────────────────────────────────────────────────────
Family                              Family generic bound
State                               State<Family>
Control                             Control<Family>
Act                                 Act<Family>
Effect                              Effect<Family>

begin(mode = Replaceable)           begin(mode = Replaceable)
begin(mode = Sequenced)             begin(mode = Sequenced)

operation(...)                      operation(...)

.results {                          .results {
  Success { ... }                     success { ... }
  Failure { ... }                     failure { ... }
}                                   }

.stay()                             .stay()
.admit<State>()                     .admit<State>()
~~~

Kotlin method casing follows this repository's function naming convention; it does not rename the
semantic Success / Failure result cases.

The first V2 prototype briefly introduced implementation-translated nouns such as:

~~~text
AsyncResult
AsyncResults
EffectPlan
ExecutionMode
~~~

That was an Origin-Walk violation: those names were formed before checking whether the Walk had already
named the responsibilities. Current source removes those prototype nouns.

The source now uses only directly mapped V2 support identities:

~~~text
VexBehaviorV2BeginMode
VexBehaviorV2Begin
VexBehaviorV2Operation
VexBehaviorV2Results
VexBehaviorV2Result
VexBehaviorV2StateDisposition

VexBehaviorV2Relationship
VexBehaviorV2Resolution
VexBehaviorV2Blueprint
VexBehaviorV2BlueprintBuilder
~~~

The first group exists only to carry already-earned Origin relationships in Kotlin. It is not a new
foundation vocabulary.

Generic ordering preserves the V1 Family language:

~~~text
<Family, State, Act, Effect>
~~~

Control remains attached through `controls<Control>()`, exactly as in V1.

The Kotlin shape under proof is therefore:

~~~kotlin
state<ThemeState.NotSet> {
    on(
        act = ThemeAct.CommitDark,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Sequenced,
        ) {
            operation(
                ThemeEffect.SaveDark,
            ).results {
                success {
                    admit<ThemeState.Dark>()
                }

                failure {
                }
            }
        },
    ).stay()
}
~~~

That reads in the same order as the Origin Walk:

~~~text
NOW
  stay()

LATER
  begin Sequenced
  -> operation SaveDark
  -> Success
  -> admit Dark
~~~

V2 materializes a V1-compatible immediate graph so existing Control projection/resolution remains
reusable rather than being cloned.

The focused proof also demonstrates that one concrete immutable State family may satisfy both:

~~~text
VexBehaviorFamily.State<Family>
+
Relay VexState<State>
~~~

without making the production Behavior module depend on Relay.

V2 remains an experiment. Do not migrate V1 consumers merely because V2 exists. A migration is earned
when a real family needs the two-scope semantics and focused proof shows the relationship is more truthful
than its current Connection-local handling.

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
