
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
Origin Walk can pressure a more contained Family shape without silently rewriting existing consumers.

The governing rule is:

~~~text
Origin earns the language
-> Kotlin maps it
-> Kotlin does not rename it for convenience
~~~

### Family as the visible root

The current V2 experiment makes the Family the namespace/root:

~~~kotlin
object ThemeModeFamily :
    VexBehaviorFamily<ThemeModeFamily>,
    VexBehaviorFamily.Members<
        ThemeModeFamily,
        ThemeModeFamily.State,
        ThemeModeFamily.Control,
        ThemeModeFamily.Act,
        VexBehaviorV2Begin<ThemeModeFamily, ThemeModeFamily.State>,
    > {

    sealed interface State :
        VexBehaviorFamily.State<ThemeModeFamily> {

        data object NotSet : State
        data object Light : State
        data object Dark : State
    }

    enum class Control :
        VexBehaviorFamily.Control<ThemeModeFamily> {
        Toggle,
    }

    sealed interface Act :
        VexBehaviorFamily.Act<ThemeModeFamily> {

        data object CommitDark : Act
        data object SelectDark : Act
        data object SelectLight : Act
    }

    override val controls =
        Control.entries.toSet()
}
~~~

This creates self-identifying paths:

~~~text
ThemeModeFamily.State.NotSet
ThemeModeFamily.State.Light
ThemeModeFamily.State.Dark

ThemeModeFamily.Control.Toggle

ThemeModeFamily.Act.CommitDark
ThemeModeFamily.Act.SelectDark
ThemeModeFamily.Act.SelectLight
~~~

A reader no longer has to infer whether a top-level `ThemeMode` or `SelectDark` symbol is a State,
Control, Act or unrelated type.

`State` and `Act` are sealed regions. `Control` is an enum, so adding a Control automatically appears
through `Control.entries`; there is no second hand-maintained registration list of individual Controls.

### One member map, then inference

The `Members` contract records the role types once:

~~~text
ThemeModeFamily
  State   -> ThemeModeFamily.State
  Control -> ThemeModeFamily.Control
  Act     -> ThemeModeFamily.Act
  Effect  -> VexBehaviorV2Begin<ThemeModeFamily, ThemeModeFamily.State>
~~~

Behavior authoring then needs only the Family value:

~~~kotlin
vexBehaviorV2(
    ThemeModeFamily,
    behaviorRef = "theme-mode",
) {
    ...
}
~~~

Control binding likewise infers its Control type:

~~~kotlin
themeModeBehavior().controls {
    control(
        ThemeModeFamily.Control.Toggle,
        ThemeModeFamily.Act.SelectDark,
        ThemeModeFamily.Act.SelectLight,
    )
}
~~~

There is no repeated `<ThemeModeFamily, State, Act>` or `<Control>` at those use sites.

### Direct Origin mapping

~~~text
ORIGIN WALK
  Save(ThemeModeFamily, ThemeModeFamily.State.Dark)

KOTLIN INSIDE THE FAMILY-BOUND BEHAVIOR
  save(ThemeModeFamily.State.Dark)
~~~

The Family argument is already fixed by the surrounding Behavior scope.

Immediate form:

~~~kotlin
state<ThemeModeFamily.State.Light> {
    on(
        act = ThemeModeFamily.Act.SelectDark,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Replaceable,
        ) {
            save(
                ThemeModeFamily.State.Dark,
            ).results {
                success { }
                failure { }
            }
        },
    ).admit<ThemeModeFamily.State.Dark>()
}
~~~

Durability-gated form:

~~~kotlin
state<ThemeModeFamily.State.NotSet> {
    on(
        act = ThemeModeFamily.Act.CommitDark,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Sequenced,
        ) {
            save(
                ThemeModeFamily.State.Dark,
            ).results {
                success {
                    admit<ThemeModeFamily.State.Dark>()
                }

                failure { }
            }
        },
    ).stay()
}
~~~

The SCAE Effect is the structured `begin(...)` scope. `Save(...)` is the operation inside that Effect.
V2 does not invent a feature-local `SaveDark` Effect.

### Source-organization tradeoff

This Family-root form intentionally pressures the existing V1 seven-file package pattern.

Kotlin does not support partial nested type declarations across files. Therefore true paths such as:

~~~text
ThemeModeFamily.State.Dark
ThemeModeFamily.Control.Toggle
ThemeModeFamily.Act.SelectDark
~~~

mean those role declarations live under the Family root declaration rather than in independent
`ThemeModeState.kt`, `ThemeModeControl.kt`, and `ThemeModeAct.kt` declarations.

That is a real tradeoff, not an implementation accident:

~~~text
V1 PACKAGE EMPHASIS
  separate role files
  + shared Family generic coordinate

V2 FAMILY-ROOT PRESSURE
  one visible Family region
  + nested closed member regions
  + less scattered registration / naming ambiguity
~~~

The V2 experiment does not yet declare that every production Family must adopt this source organization.
It exists to determine whether the containment/readability benefit is worth changing the older file pattern.

### Fractal State compatibility

The focused proof also makes the nested State region satisfy Relay State:

~~~text
ThemeModeFamily.State
  : VexBehaviorFamily.State<ThemeModeFamily>
  + VexState<ThemeModeFamily.State>
~~~

so the same family-scoped State can participate in SCAE and Relay/Convergence without inventing another
semantic noun.

V2 remains an experiment. Do not migrate V1 consumers merely because V2 exists.

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
