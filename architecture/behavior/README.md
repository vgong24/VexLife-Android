
# Vex Behavior

[VXG RealForever]

This module preserves the geometry of **what a human can do from what is true now**.

## Start here

- IDE/code navigation: `VexBehaviorDocs`
- Canonical family/package construction pattern:
  [`VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)
- **Fresh-entry round-trip reference:** [`VexBehaviorUiRoundTripReferenceTest`](src/test/kotlin/com/vextreme/vexlife/android/behavior/VexBehaviorUiRoundTripReferenceTest.kt)
  — read this when you need the vetted UI intent -> CURRENT observed State -> Family Behavior -> Act ->
  Effect/Save -> ObservableState -> UI loop, including the stale-render currentness case.
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

## Fresh-entry reference specimen

The canonical **look-here-first working specimen** for the current V2 Origin-Walk shape is:

~~~text
VexBehaviorUiRoundTripReferenceTest
~~~

It is intentionally a JVM reference rather than production Compose code. It proves one readable semantic
round trip without making `:architecture:behavior` depend on app/UI runtime:

~~~text
Composable-facing observation
-> onClick emits Family.Control
-> live boundary reads CURRENT State
-> Family.Behavior resolves the Act
-> Act carries Effect + immediate stay/admit
-> Connection realizes Save
-> Store ObservableState is observed
-> one semantic State owner updates
-> UI observes the resulting State
~~~

It also proves the stale-render case:

~~~text
UI rendered Light
-> observed truth becomes Dark before click
-> click does NOT pass stale Light back as authority
-> activation re-reads CURRENT Dark
-> Dark + Toggle Act is selected
~~~

The test comments preserve why each boundary exists and which pieces remain deliberately held. AppActions,
Journey ActionElements, real Compose rendering, coroutine lifetime, and device evidence remain in their
rightful foundations rather than being duplicated into this reference.

## V2 Origin-Walk refinement — sibling experiment, not replacement

The accepted V1 kernel remains production-used and is intentionally left intact above. V2 exists beside
it so the ThemeMode Origin Walk can pressure the smallest complete semantic circuit without silently
rewriting existing VexStream / Queue consumers.

The governing rule is:

~~~text
Origin earns the language
-> Kotlin realizes it
-> Kotlin does not manufacture a type merely because V1 had one
~~~

### What the POC discovered

The first V2 shape copied V1 literally:

~~~text
State + Control
-> Act value

State + Act value
-> Effect + State consequence
~~~

ThemeMode exposed a duplication:

~~~text
State.Light + Control.Toggle
-> Act.SelectDark

then:

State.Light + Act.SelectDark
-> Save(State.Dark) + admit State.Dark
~~~

The same State meaning was authored twice, and `Act.SelectDark` repeated meaning already visible in the
behavioral scenario.

The removal test then asked:

~~~text
remove ThemeModeFamily.Act.SelectDark

did we lose:
  which State was current?        no
  which Control entered?          no
  which Effect was attempted?     no
  which State consequence occurs? no
~~~

So the intermediate **Act value/type hierarchy was not earned for this POC**.

What survived was the Act as the behavior scenario itself:

~~~text
CURRENT State + Control
-> Act {
     Effect
     immediate stay/admit
     optional later result admission
   }
~~~

V2 therefore uses `Act` as the authored `on(...)` relationship/process, not as another Family member
vocabulary that a fresh feature must invent.

### Family as the visible root

Current patient-zero shape:

~~~kotlin
object ThemeModeFamily :
    VexBehaviorFamily<ThemeModeFamily> {

    sealed interface State :
        VexBehaviorFamily.State<ThemeModeFamily> {

        data object NotSet : State
        data object Light : State
        data object Dark : State
    }

    sealed interface Control :
        VexBehaviorFamily.Control<ThemeModeFamily> {

        data object Toggle : Control
    }

    val Behavior =
        vexBehaviorV2(
            family = this,
            behaviorRef = "theme-mode",
        ) {
            // Acts are authored here as State + Control scenarios.
        }
}
~~~

This gives one discoverable semantic root:

~~~text
ThemeModeFamily
  State
  Control
  Behavior
    Acts
      Effects
~~~

There is deliberately no `ThemeModeFamily.Act` in V2.

The `VexBehaviorFamily.Act<Family>` foundation marker remains only because V1 production consumers still
use the older Act-as-vocabulary contract. Its KDoc records that compatibility boundary so a fresh AI does
not recreate a V2 Family.Act merely for symmetry.

### State and Control are contained, not registered twice

State and Control remain Family-scoped sealed regions:

~~~text
ThemeModeFamily.State.NotSet
ThemeModeFamily.State.Light
ThemeModeFamily.State.Dark

ThemeModeFamily.Control.Toggle
~~~

No `Members<...>` registry exists.

A Control region is sealed rather than enum-only, so another Family may later carry payload:

~~~kotlin
sealed interface Control :
    VexBehaviorFamily.Control<SongFamily> {

    data class Click(
        val songId: SongId,
    ) : Control
}
~~~

The current ThemeMode proof resolves exact Control values because Toggle is a singleton. Matching
payload-bearing Control **shapes** such as `Click(songId)` remains the next explicit pressure; V2 does not
hide that unfinished edge behind an unearned matcher framework.

### The authored Act

ThemeMode's immediate Toggle scenario now reads:

~~~kotlin
state<State.Light> {
    on(
        control = Control.Toggle,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Replaceable,
        ) {
            save(
                State.Dark,
            ).results {
                success { }
                failure { }
            }
        },
    ).admit<State.Dark>()
}
~~~

Read it as one sentence:

~~~text
WHILE State.Light is current
ON Control.Toggle
ACT by:
  begin Replaceable
  Save(State.Dark)
  admit State.Dark immediately
~~~

The same Control in another State is a different Act:

~~~kotlin
state<State.Dark> {
    on(
        control = Control.Toggle,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Replaceable,
        ) {
            save(
                State.Light,
            ).results {
                success { }
                failure { }
            }
        },
    ).admit<State.Light>()
}
~~~

The durability-gated Act preserves the already-earned two temporal scopes:

~~~kotlin
state<State.NotSet> {
    on(
        control = Control.Toggle,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Sequenced,
        ) {
            save(
                State.Dark,
            ).results {
                success {
                    admit<State.Dark>()
                }

                failure { }
            }
        },
    ).stay()
}
~~~

So:

~~~text
NOW
  stay in the current State

LATER
  Save succeeds
  -> admit State.Dark
~~~

Removing the Family.Act value did not weaken Save / Success / Failure / later-admission semantics.

### Runtime entrance

The runtime-facing semantic call is now one operation:

~~~kotlin
val act =
    ThemeModeFamily.Behavior.resolve(
        current = currentState,
        control = incomingControl,
    )
~~~

The caller does not perform:

~~~text
Control blueprint
-> Act value
-> Behavior blueprint
~~~

Instead Behavior selects the one authored Act whose State + Control input matches.

That lets the Connection remain the reality/currentness boundary:

~~~text
receive semantic Control
-> read CURRENT State
-> Family.Behavior.resolve(State, Control)
-> realize Act.effect
-> observe provider/runtime truth
-> reconcile current State
~~~

The POC therefore removes semantic assembly from the Connection without turning Family into a runtime
service.

### Effect language remains Origin-owned

The SCAE Effect is still the structured `begin(...)` scope, and Save is the operation inside it:

~~~text
begin(mode = Replaceable | Sequenced) {
  Save(State)
    .results {
      Success { optional later admit }
      Failure { optional later admit }
    }
}
~~~

Kotlin uses target-native casing:

~~~kotlin
begin(...) {
    save(State.Dark)
        .results {
            success { }
            failure { }
        }
}
~~~

The surrounding Behavior already fixes Family, so `save(State.Dark)` is the target realization of
`Save(ThemeModeFamily, ThemeModeFamily.State.Dark)` without repeating Family as a runtime token.

### Why the code comments are unusually causal

The V2 source and focused test deliberately record the POC history, not just API mechanics.

A fresh AI should be able to recover:

~~~text
why no Family.Act exists in V2
why Control is sealed rather than enum-only
why Behavior is exposed from the Family root
why on(State + Control) is called the Act
why V1's Act marker still exists
why payload-bearing Control matching remains held
~~~

The comments exist to prevent an apparently reasonable "cleanup" from recreating the superseded
Control->Act-value->Effect layering.

### Source-organization tradeoff

True paths such as:

~~~text
ThemeModeFamily.State.Dark
ThemeModeFamily.Control.Toggle
ThemeModeFamily.Behavior
~~~

mean those declarations live under the Family root. Kotlin does not provide partial nested-type
declarations across independent files.

That intentionally pressures the older V1 seven-file package pattern:

~~~text
V1
  separate Family / State / Control / Act / Effect / Behavior types

V2 patient zero
  one visible Family root
    State
    Control
    Behavior
      Acts
        Effects
~~~

This POC does not yet mandate migration of existing V1 families.

### Fractal State compatibility

The focused proof also keeps the same nested State region compatible with Relay:

~~~text
ThemeModeFamily.State
  : VexBehaviorFamily.State<ThemeModeFamily>
  + VexState<ThemeModeFamily.State>
~~~

No second `ThemeState` noun is introduced.

### Next pressure after the end-to-end circuit

The ThemeMode path is now sufficiently closed to move the Origin Walk to the next questions rather than
recreating the same semantic bridge:

~~~text
1. What should Behavior itself be as a Family member?
   value / function / stable contract / another shape?

2. What does UI actually need to see from Family.Behavior?
   current State?
   available semantic Controls?
   projection?
   activation surface?

3. How should payload-bearing Control shapes be authored/matched
   without returning to registration lists or an extra Act-value layer?
~~~

Those questions are held as the next refinement edge, not silently answered by the current POC.

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


### V2 refinement — Effect is earned per Act

The real Debug Menu UI/runtime walk added a pressure that the earlier persistence specimen had hidden.

Current Android owns Theme Mode only as in-process session truth:

~~~text
VexLifeSession.darkTheme
  -> VexLifeApp / DebugMenu checked projection

DebugMenu activation
  -> VexLifeSession.toggleDarkMode()
  -> the SAME current session truth changes
~~~

`VexLifeSession` explicitly does not claim process-death restoration. The VexSync work outcome asks for
the dark/light Theme Mode feature and Origin/Android round trip; it does not independently require
durable persistence.

Therefore V2 must not require every Act to manufacture an Effect merely because the builder previously
accepted only `on(Control, Effect)`.

The current V2 rule is now:

~~~text
CURRENT State + Control
-> Act {
     optional Effect
     immediate stay/admit
     optional later result admission when an Effect exists
   }
~~~

For the current in-process ThemeMode transition:

~~~kotlin
state<State.Light> {
    on(
        control = Control.Toggle,
    ).admit<State.Dark>()
}

state<State.Dark> {
    on(
        control = Control.Toggle,
    ).admit<State.Light>()
}
~~~

No `NoOpEffect`, fake Save or persistence layer is introduced.

The already-earned two-scope Effect language remains intact for an Act that really needs it:

~~~kotlin
state<State.NotSet> {
    on(
        control = Control.Toggle,
        effect = begin(
            mode = VexBehaviorV2BeginMode.Sequenced,
        ) {
            save(State.Dark)
                .results {
                    success {
                        admit<State.Dark>()
                    }
                    failure { }
                }
        },
    ).stay()
}
~~~

So:

~~~text
EFFECT ABSENT
  = this Act asks no external/runtime reality owner to transform anything

EFFECT PRESENT
  = this Act explicitly asks reality/runtime to do work

NO EFFECT
!= NO-OP EFFECT
!= EFFECT FAILURE
~~~

This refinement does not yet choose the production ThemeMode runtime object. The current target evidence
instead says to start from the existing session-owned current truth and add another owner only if a later
persistence/System/provider/lifecycle pressure actually earns one.
