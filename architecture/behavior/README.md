
# Vex Behavior

[VXG RealForever]

This module preserves the geometry of **what a human can do from what is true now**.

## Start here

- IDE/code navigation: `VexBehaviorDocs`
- Canonical family/package construction pattern:
  [`VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)
- **Fresh-entry V2 activation-currentness reference:** [`VexBehaviorUiRoundTripReferenceTest`](src/test/kotlin/com/vextreme/vexlife/android/behavior/VexBehaviorUiRoundTripReferenceTest.kt)
  — read this for the proved local loop: UI emits only Control -> live Family re-reads CURRENT semantic
  State -> selects the Act -> applies immediate stay/admit -> a later render reads the resulting
  DisplayState. It proves stale rendered State is not activation authority. It does **not** prove
  Compose observation, provider-observation currentness, async-result staleness, or Store convergence.
- **Family Circuit construction guide:** [`VexBehaviorV2FamilyCircuitTestSampleSuite`](src/test/kotlin/com/vextreme/vexlife/android/behavior/VexBehaviorV2FamilyCircuitTestSampleSuite.kt)
  — read this teaching suite when you need the current end-to-end Family Circuit construction shape
  rather than historical pressure. ComplexFamily itself extends the Compound Family foundation and
  owns ComplexCompoundState: partial Input -> latest complete Input -> process -> Output -> commit ->
  Act -> ScreenFamily Control -> admitted target CURRENT State -> observed NEW Input. The scenario is
  simulated and not product behavior.
- Review pressure/evidence:
  [`REVIEW-CHECKPOINT.md`](REVIEW-CHECKPOINT.md)





## Current Family Circuit foundation

The process simplification is now part of the V2 Family foundation rather than only a test-side Relay
experiment.

~~~text
VexBehaviorV2Family
  simple Family current-State case

VexBehaviorV2CompoundFamily
  Family specialization for a CURRENT relationship State
  -> concrete named Compound State
  -> InputState.latest(incoming)
  -> process(latestStateInput)
  -> OutputState
  -> Family.current commit
  -> Act only for Output-changing commits
~~~

The concrete Family owns the relationship. There is no sibling Compound Relay required to own a
second copy of current Input/Output truth.

VexBehaviorV2FamilyCircuitTestSampleSuite is the teaching surface for this current structure.

## V2 Origin-Walk test progression

The V2 tests are deliberately an **evolution sequence**, not three interchangeable examples.

Read them in this order:

~~~text
0. VexBehaviorV2FamilyCircuitTestSampleSuite
   current construction-teaching suite for the candidate Family Circuit Architecture
   -> ComplexFamily extends VexBehaviorV2CompoundFamily
   -> ComplexCompoundState is Family.current
   -> ScreenFamily + two independently-owned observation-only CURRENT truths
   -> partial ComplexInputState observations
   -> InputState.latest(...) forms one complete current input
   -> ComplexFamily.process(latestStateInput) calculates meaning only
   -> ComplexOutputState is the current relationship consequence
   -> Output-changing Family State commit becomes an Act consequence
   -> Act enters the rightful target Family through Control
   -> observed target CURRENT truth returns as NEW input
   -> input-only Compound revisions do not fabricate another Act

1. VexBehaviorV2Test
   ThemeMode patient zero
   -> Family-root authoring
   -> structural Act
   -> concrete stay/admit
   -> optional begin/Save/results
   -> local State-store execution/accounting

2. VexBehaviorUiRoundTripReferenceTest
   activation-currentness pressure
   -> UI reads DisplayState
   -> UI sends Control only
   -> Family re-reads CURRENT semantic State
   -> stale render is not activation authority

3. app/.../vexstream/playback/VexBehaviorV2PlaybackOriginPressureTest
   cross-feature edge pressure
   -> provider observation must reconcile with newer local intent
   -> payload Seek retains exact media identity/currentness
   -> Control availability derives from current semantic/provider capability
   -> runtime State + payload Control materialize an Act
   -> reconciled provider truth reaches a concrete Family admission seam

4. VexBehaviorV2StateRelayOriginPressureTest
   hard-pressure continuation after the teaching suite
   -> stale async completion re-evaluates against CURRENT State
   -> nested owned lists preserve stable identity + complete snapshot layers
   -> caller-owned mutable containers detach at the copy boundary
   -> independently-owned child currentness converges by stable ref
   -> arbitrary-depth State identity
   -> process-only Compound generic
   -> process -> commit -> Act -> observe -> NEW input
   -> Replaceable / Sequenced committed-Output runner pressure
~~~

The third class is intentionally a **pressure reference**, not the newest confirmed V2 template.

~~~text
PASSING PRESSURE TEST
!= V2 SUPPORTS THE FEATURE
!= PRODUCTION PLAYBACK SHOULD BE REFACTORED NOW
~~~

Its purpose is to make unresolved boundaries executable so a later Origin Walk cannot accidentally
erase them with prose or a convenient abstraction. Production Playback remains unchanged until a V2
candidate can satisfy those pressures without stealing Media3 ownership, duplicating availability
rules, losing payload identity or bypassing reconciliation.

Once a later candidate makes the pressure class read naturally without escape hatches, that candidate
can earn a new reference/proof layer and only then pressure production refactoring.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][TEST-EVOLUTION][PLAYBACK-PRESSURE] -->

## Current V2 currentness boundary

"Currentness" is not one mechanism. The Origin Walk currently distinguishes these separate questions:

~~~text
ACTIVATION CURRENTNESS
  UI sends Control only
  -> Family.activate(Control)
  -> Family re-reads its CURRENT semantic State
  -> stale rendered State is never sent back as authority

  STATUS: PROVED for the locally-owned ThemeMode patient zero


IMMEDIATE SEMANTIC CURRENTNESS
  selected Act
  -> stay()
     or
  -> admit(concrete State)
  -> Family.current changes immediately

  STATUS: PROVED for the locally-owned ThemeMode patient zero


ASYNC RESULT CURRENTNESS
  begin(...)
  -> Operation completes later
  -> result-scoped admit(...)

  QUESTION:
  is this result still current relative to newer Controls/observations?

  STATUS: OPEN
  current V2 has no general transition/result generation guard


EXTERNALLY OBSERVED CURRENTNESS
  Media3 / SystemAppearance / location / repository/provider observation
  -> reconciliation
  -> semantic Family current State

  STATUS: OPEN
  do not turn provider observations into fake Controls or expose arbitrary setCurrent(...)


UI OBSERVATION CURRENTNESS
  Family State / DisplayState changes
  -> Compose or another UI consumer reacts

  STATUS: OPEN
  current / displayState are readable snapshots; @Volatile is not an observable UI contract


DURABLE CURRENTNESS
  Family.current
  versus
  Family State Store durable State

  STATUS: distinction proved
  reconciliation/observation into live Family remains OPEN


INSTANCE / EXECUTION CURRENTNESS
  multiple Family instances or several queued episodes
  -> independent execution identity / lawful sequencing

  STATUS: OPEN
~~~

The stable law is therefore narrower than "Family owns all currentness":

~~~text
for locally-owned semantic State,
activation must re-read the rightful current owner

for externally-owned truth,
ownership and reconciliation must be earned from the real provider/Relay relationship
~~~

Do not add a generic setter, fake Control, Connection, Relay, observer, or transition token merely to
fill these open boxes. Let Playback / Select / Seek and SystemAppearance pressure the smallest
relationship that preserves rightful ownership.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][CURRENTNESS-BOUNDARY] -->


## SCAE architecture — accepted production lineage and general semantic law

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
6. **Admission != eventual provider outcome.** `.admit(state)` describes immediate semantic
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
    `.admit(state)`.
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

## Fresh-entry V2 reference specimen

The current V2 fresh-entry specimen is:

~~~text
VexBehaviorUiRoundTripReferenceTest
~~~

It deliberately proves **activation currentness only** for the local ThemeMode-shaped case:

~~~text
UI render reads Family.displayState
-> UI activation emits Control only
-> Family.activate(Control)
-> Family re-reads CURRENT semantic State
-> selects the authored Act
-> applies immediate stay/admit
-> a later render reads Family.displayState again
~~~

Stale-render proof:

~~~text
UI rendered Light
-> Family current becomes Dark before the UI activation
-> UI does not send Light/checked Boolean back as authority
-> activate(Toggle) re-reads CURRENT Dark
-> Dark + Toggle Act is selected
~~~

This reference does **not** claim:

~~~text
StateFlow / Compose automatic observation
Store ObservableState -> Family reconciliation
provider observation -> Family current
async result generation/currentness
multi-instance execution identity
~~~

Those belong to the explicit currentness boundary above and remain Origin pressure.

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
    ).admit(State.Dark)
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
    ).admit(State.Light)
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
                    admit(State.Dark)
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

### Runtime entrance — superseded by live Family activation

The early V2 POC exposed `Behavior.resolve(current, control)` directly. That entrance is superseded
by the live-Family patient zero:

~~~text
UI / caller
-> Family.activate(Control)
-> Family re-reads its own CURRENT semantic State
-> internal Act resolution
-> immediate stay/admit
~~~

This closes one specific currentness hole: callers cannot choose the semantic State used for
activation.

It does **not** establish Family as the universal current-State owner. Provider observation,
SystemAppearance, Store observation and other independently-owned truths still require a lawful
reconciliation relationship before they may affect semantic Family current State.

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
why payload-bearing Control matching was later earned by the Playback pressure threshold
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

3. Payload-bearing Control shapes are now authored by runtime Control type and actual value.
   The remaining question is how their provider Operations/results should be realized.
~~~

Those questions are held as the next refinement edge, not silently answered by the current POC.

V2 remains an experiment. Do not migrate V1 consumers merely because V2 exists.

## Deliberately held

The current kernel does not claim generic solutions for:

~~~text
provider Operation payload/result realization
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
    ).admit(State.Dark)
}

state<State.Dark> {
    on(
        control = Control.Toggle,
    ).admit(State.Light)
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
                        admit(State.Dark)
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


### V2 refinement — admission carries the concrete State value

Making Effect optional exposed one more hidden dependency in the earlier V2 shape.

The previous terminal spelling:

~~~kotlin
.admit<State.Dark>()
~~~

preserved only:

~~~text
State.Dark::class
~~~

That was sufficient while the POC also happened to carry `Save(State.Dark)`, because the reference
could borrow the Save value as the admitted value. Once an Act truthfully has no Effect, that shortcut
disappears.

The removal question is:

> If Behavior already knows the exact semantic State to admit, why should Runtime reconstruct it from
> a class token?

It should not.

V2 now spells immediate admission with the concrete State value:

~~~kotlin
state<State.Light> {
    on(
        control = Control.Toggle,
    ).admit(State.Dark)
}
~~~

and later result admission likewise carries a value:

~~~kotlin
success {
    admit(State.Dark)
}
~~~

The distinction becomes:

~~~text
stay()
  current State value remains authoritative
  -> no destination value is carried

admit(nextState)
  Behavior names the concrete next semantic truth
  -> the Act carries that State value

result { admit(nextState) }
  later consequence names the concrete later semantic truth
  -> the Result carries that State value
~~~

This matters beyond ThemeMode singleton objects. A data-bearing State must not lose its flavor and then
require a runtime registry to reconstruct it from a `KClass`.

The current V2 result grammar still does not claim dynamic provider-result payload mapping. If a future
Effect result needs runtime data to construct the next State, that real consumer must earn the
corresponding result contract rather than hiding it behind a class token.

So the current semantic output is now directly usable by a session owner:

~~~text
Behavior.resolve(CURRENT State, Control)
-> Act.immediateDisposition
-> Stay
   or
-> Admit(concrete State)
~~~

No second State-kind -> State-value translator is required.


---

## V2 Origin-Walk refinement — live Family + required Acts + DisplayState

The ThemeMode patient zero now keeps the current semantic and UI-facing contract at one discoverable
Family root:

~~~text
Family
  State
  Control
  DisplayState
  current
  acts
  activate(Control)
~~~

activate(Control) is foundational V2 language. The caller supplies only Control; the live Family
re-reads its own CURRENT State, selects the authored Act and applies that Act's immediate stay/admit
consequence.

Every live V2 Family has a displayState field. The foundation default is
VexBehaviorV2DisplayState.Empty(current), so display ownership remains explicit even when a Family has
not earned richer presentation categories. A Family may override that field with its own nested
DisplayState vocabulary while preserving the underlying semantic State.

DisplayState is presentation-facing output, not another State admitted by Behavior:

~~~text
admit(State)
-> changes semantic truth

current State
-> DisplayState
-> UI
~~~

The Act authoring surface is contiguous:

~~~kotlin
state<State.Light> {
    on(Control.Toggle) {
        admit(State.Dark)
    }
}

state<State.NotSet> {
    on(Control.Toggle) {
        stay()

        begin(mode = VexBehaviorV2BeginMode.Sequenced) {
            save(State.Dark)
                .results {
                    success {
                        admit(State.Dark)
                    }

                    failure {
                    }
                }
        }
    }
}
~~~

Direct stay()/admit(...) inside on { ... } is the immediate semantic disposition. The existing
begin(mode = Replaceable | Sequenced) scope remains the later-running Effect episode, and
Success/Failure remain operation result branches. No extra immediate, realize, or feature-specific
toggle function vocabulary was added.

activate(Control) returns the selected Act after applying its immediate consequence. Generic
provider/Store/third-party Effect execution is deliberately not invented by this checkpoint; that is the
next Origin pressure, with Media3/provider-backed families available as comparison evidence.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][LIVE-FAMILY][DISPLAY-STATE] -->


---

## V2 Origin-Walk refinement — begin executes Family Store operations

The next pressure makes the previously structural begin/Save episode executable without adding a
feature-specific Execution class.

~~~text
Family
  State
  Control
  DisplayState
  Store
  current
  acts
  activate(Control)

Act
  immediate:
    stay / admit

  execution:
    begin(mode)
      Operation
        StoreOperation
          Save(State)
~~~

The reusable storage relationship is:

~~~text
VexStore
  -> VexFamilyStateStore<Family, State>
       ObservableState
       Get()
       Save(State)
       Forget()

Concrete Family
  interface Store : VexFamilyStateStore<ThisFamily, State>
~~~

CURRENT semantic State and durable Store State remain different truths. An immediate admit(Dark) can
make Dark visible to UI while Save(Dark) is still background work.

Operation generalizes the slot under begin; it does not erase the Store verb. Save remains authored as
save(State). Media/provider operations can pressure additional operation families later without
pretending that Play/Pause are persistence.

begin(Replaceable) and begin(Sequenced) now enter the existing VexExecution coroutine custody. The
Behavior foundation does not add a ThemeModeExecution/FamilyExecution class. The source callable
Family.Store::save is both the invoked operation and the execution identity input.

The live Family accepts one VexFamilyLogger accountant. The app's existing VexLogger may implement
that contract; the Family never creates another logger.

~~~text
Control accepted
State admitted
Operation began
Operation Success / Failure
~~~

An empty success { } remains meaningful: the Operation succeeded and produced no additional semantic
State consequence, while the accountant still witnesses progression.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][FAMILY-STORE][BEGIN-EXECUTION][ACCOUNTANT] -->


---

## V2 stability boundary after cross-feature re-entry

The ThemeMode Origin Walk has now been reread against VexStream Playback/MediaStore, Maps
Convergence/offline stores and Conversation repository/hybrid projection pressure.

### Stable architecture to preserve

~~~text
Family as the discoverable semantic root

Family.State
Family.Control
required minimum Family.DisplayState role

Act = CURRENT State + Control scenario

on(Control) {
  immediate stay()/admit(concrete State)

  optional begin(mode) {
    Operation
      .results {
        Success { optional later consequence }
        Failure { optional later consequence }
      }
  }
}

Effect is optional
begin is the semantic execution phase
stronger operation verbs remain visible (Save stays Save)
one supplied accountant witnesses accepted facts but owns no truth
Relay / Convergence remains separate for independently-owned complete truths
~~~

These are the parts a fresh feature author may treat as the intended V2 direction.

### Deliberately narrowed after re-entry

DisplayState remains mandatory, but `VexBehaviorV2DisplayState<State>` no longer requires every
projection to expose the complete semantic State. The default `Empty(current)` retains State only as
the explicit fallback. Family-specific display cases may expose bounded UI fields instead.

Store remains discoverable from the Family, but storage is no longer equated with "persist the Family
State":

~~~text
VexStore
-> VexFamilyStore<Family>
     broad Family-visible Store capability

VexFamilyStateStore<Family, State>
     earned specialization for Get / ObservableState / Save(State) / Forget
~~~

ThemeMode uses the State-store specialization. VexStream media custody, Maps artifacts and
Conversation canonical repositories must keep their own rightful value/result domains.

The SharedPreferences boolean write result is now `VexStoreWriteResult`, not a universal
Operation-result contract. Rich provider Operations remain free to earn typed results.

The Family accountant is now an explicit constructor dependency. Tests/previews may explicitly choose
`VexFamilyLogger.None`; the foundation no longer silently installs a no-op accountant.

### Candidate thresholds reached

~~~text
runtime payload-bearing Control materialization
  actual CURRENT State + actual Control payload -> resolved Act

value-transforming immediate State
  dynamic Act may admit from actual current/control values

same-law Control availability
  isAvailable(Control) and activate(Control) resolve from the same Act rules

external provider admission seam
  provider/Connection reconciliation stays concrete
  -> concrete Family-specific method
  -> protected admit(State)
  -> no fake Control / no generic public setter
~~~

These are still candidate V2 refinements until repository proof and another real feature pressure them.

### Still OPEN — do not generalize by syntax

~~~text
concurrent admission serialization/currentness

async result staleness / transition generation

observable Family State / bounded DisplayState for Compose

typed provider Operation result payload

provider-affine Operation execution under begin

multiple Family-instance execution identity

Sequenced lane queue/custody semantics

cross-version durable identity / rename migration

privacy-bounded accountant payload
~~~

The current `VexBehaviorV2Family` runtime should therefore be read as the locally-owned ThemeMode
patient-zero realization of the stable authoring language, not as proof that Family itself owns every
provider/repository truth.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STABILITY-BOUNDARY] -->




### Why DisplayState remains mandatory even when Empty adds no new truth

The removal test for DisplayState is **not**:

> does `Empty(current)` add another semantic fact?

It does not.

The question is:

> if the DisplayState role disappears entirely, what guaranteed relationship disappears?

Without it, a fresh consumer has only semantic State and must decide for itself how that State becomes
human/UI/VexBoard-readable output:

~~~text
Family.current
-> ??? consumer-specific interpretation
-> UI / VexBoard
~~~

Keeping the role makes that relationship explicit even when the smallest lawful projection is simply:

~~~text
Family.current
-> DisplayState.Empty(current)
-> human/projectable output
~~~

So:

~~~text
MANDATORY DISPLAY ROLE
!= MANDATORY EXTRA DISPLAY TRUTH

Empty(current)
= explicit "no richer display classification earned"
~~~

A richer Family may expose bounded fields and must not carry the entire canonical State merely to
satisfy the role.

This remains worth pressure-testing against non-visual Families, but the current reason for mandatory
DisplayState is discoverability/projectability, not duplication of truth.




## Ecosystem-aware removal tests

Origin removal tests must follow the **relationship through the larger fractal ecosystem**, not stop
at the local class/file.

A local value may be information-redundant while its ownership coordinate remains structurally
necessary.

Use this sequence:

~~~text
1. What local information disappears?

2. What relationship disappears?

3. Which larger Family/fractal consumers rely on that relationship?
   UI
   VexBoard
   widgets
   diagnostics
   future projections

4. Would removal force those consumers to reconstruct or reinterpret upstream meaning?

5. If no correctness, ownership, projectability, continuity or proof relationship is lost anywhere
   in that ecosystem, remove it.
~~~

This is why `DisplayState.Empty(current)` survives its current removal test:

~~~text
Empty(current)
  adds no new semantic truth

DisplayState ROLE
  preserves the Family-owned coordinate for outward/projectable meaning
~~~

Think of `Empty(current)` as a structural zero:

~~~text
[]
  contains no elements
  but preserves "this is where this collection relationship lives"

DisplayState.Empty(current)
  contains no richer display classification
  but preserves "this is where this Family's projectable meaning lives"
~~~

Without that coordinate, a fresh implementation is forced to invent placement again:

~~~text
ThemeModeProjection?
ThemeModeUiState?
ThemeModeViewState?
Experience?
Composable mapper?
ViewModel property?
VexBoard adapter?
~~~

That is not simplification if every downstream consumer must rediscover the ownership boundary.

The rule is still bounded: mandatory DisplayState role does **not** require a custom display
vocabulary, full semantic State exposure, or a second truth owner.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][ECOSYSTEM-AWARE-REMOVAL] -->


## V2 threshold — runtime State + payload Control materialize the Act

Playback pressure earned one reusable refinement without deciding provider observation.

The exact singleton form remains unchanged:

~~~kotlin
state<State.Light> {
    on(Control.Toggle) {
        admit(State.Dark)
    }
}
~~~

A runtime payload Control may now author by Control shape:

~~~kotlin
state<State.Ready> {
    on<Control.Seek>(
        whenCurrent = { current, seek ->
            current.mediaId == seek.request.mediaId &&
                current.mediaItemIndex == seek.request.mediaItemIndex &&
                current.seekAvailable
        },
    ) { current, seek ->
        // Act is materialized from THESE runtime values.
        ...
    }
}
~~~

Relationship:

~~~text
actual CURRENT State value
+
actual incoming Control value
-> authored rule matches the State/Control shapes
-> currentness/availability predicate evaluates the actual values
-> one resolved Act is materialized from those values
~~~

The old exact-value `on(Control)` form remains a compact specialization for singleton Controls.

The Family also exposes `isAvailable(Control)`, derived from the exact same rule resolution as
`activate(Control)`. It is informative only:

~~~text
isAvailable(Control)
-> re-read CURRENT State
-> resolve same semantic rule

later activate(Control)
-> re-read CURRENT State AGAIN
-> resolve again
~~~

Therefore UI availability does not become activation authority and no second availability table is
introduced.

Dynamic Act authoring is declarative. Availability may materialize an Act but never executes
`begin(...)`; Operation execution starts only after activation selects the Act.

This threshold solves:

~~~text
payload-bearing Control matching
runtime Control payload reaching the Act
CURRENT State value participating in Act formation
same-law Control availability
value-dependent immediate State consequence
~~~

It intentionally does NOT solve:

~~~text
provider observation -> Family semantic current State
pending-intent/provider reconciliation ownership
provider-affine Seek Operation
async result staleness/generation
Compose observation
~~~

Those remain inside the Playback pressure class rather than being filled by a generic observer or
setCurrent(...) API.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][RUNTIME-ACT-MATERIALIZATION] -->


## V2 pressure candidate — concrete Family owns admission after external reconciliation

Playback pressure exposes a second useful relationship without earning a generic observation API.

The foundation now allows the concrete Family implementation to reuse semantic `admit(State)` after
its own rightful external-truth reconciliation:

~~~text
provider observation
-> provider/Connection-owned reconciliation
-> reconciled semantic State
-> concrete Family-specific admission method
-> protected Family.admit(State)
-> Family.current + accountant
~~~

The protected seam matters:

~~~text
UI / arbitrary provider caller
  CANNOT call setCurrent(...)

generic V2 foundation
  DOES NOT invent observe(...) / reconcile(...) for every Family

concrete Family
  may expose the exact provider/repository observation relationship it has actually earned
  and may call protected admit(...) only after that relationship produces semantic truth
~~~

The Playback pressure test reuses the existing `VexStreamPlaybackPending` and
`reconcileVexStreamPlaybackObservation(...)` logic. A stale provider callback therefore remains
subordinate to a newer local intent; a later matching provider observation clears pending before the
Family admits the reconciled semantic State.

This is still a candidate pressure result, not a production Playback migration. It does not yet solve
concurrent admission serialization, async-result generation guards, provider-affine Operations or
Compose observation.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][PROVIDER-RECONCILED-ADMISSION] -->


## Next runtime pressure — serialized semantic admission

The cross-feature review narrows the next runtime question substantially.

The remaining issue is no longer whether Family can represent payload Controls or receive reconciled
provider truth. Candidate paths now exist for both.

The unresolved question is:

~~~text
several lawful semantic-admission causes
  UI activation
  provider reconciliation
  async Operation result
  future Store/repository observation
        |
        v
who decides their exact order/currentness?
~~~

`@Volatile` is deliberately insufficient. It gives visibility, not ordering.

The next Origin pressure must preserve all of these distinctions:

~~~text
accepted newer Control
!= older async result finishing later

provider observation time
!= semantic admission authority by itself

serialized semantic admission
!= make Family own provider/repository truth

one ordering boundary
!= one giant lock around every feature responsibility
~~~

Do not add a mutex, transition generation, event queue, Relay, actor, or dispatcher rule until a real
race specimen proves what must be ordered together and what must remain independently owned.

Playback remains the preferred pressure source because it already has:
- newer pending intent versus stale provider observation;
- provider callbacks;
- Controls;
- provider capability availability.

The next useful test should add one **competing lawful Family admission cause** (for example an older
async result) and prove the failure before choosing the serialization/currentness mechanism.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][SERIALIZED-ADMISSION-PRESSURE] -->


## Origin pressure candidate — Family StateRelay

The remaining currentness problem now has direct V1 Relay evidence.

V1 `VexRelay` already proves the law:

~~~text
concurrent arrivals
-> one serialized update/send boundary
-> read exact retained CURRENT State
-> process incoming cause against CURRENT
-> publish semantic change only
~~~

The Playback Origin pressure class now contains a **test-only StateRelay candidate** that reuses
`VexRelay` directly.

The State-forward name is intentional:

~~~text
DisplayState
  display qualifies State
  -> outward/projectable State meaning

StateRelay
  State is foregrounded
  -> current-State custody / movement / admission relationship
~~~

The competing-admission specimen proves:

~~~text
old async intent begins
-> newer semantic intent becomes CURRENT
-> old completion arrives
-> StateRelay gives completion CURRENT State
-> old completion sees it no longer owns current meaning
-> no State regression

then:
current intent's completion arrives
-> still lawful against CURRENT
-> State advances
~~~

This is a stronger currentness rule than direct `admit(nextState)` from callbacks:

~~~text
incoming cause
+
CURRENT State at serialized boundary
-> lawful next State
~~~

### Why this is not foundation yet

The pressure fixture's State implements `VexState` so it can reuse V1 `VexRelay`. Current Behavior
Family State does not universally require `VexState`, and production Playback State currently does
not implement it.

Therefore this checkpoint does **not** yet decide:

~~~text
V2 Family.State : VexState
StateRelay : VexRelay
Behavior module -> production Relay dependency
new independent StateRelay kernel
~~~

Those are implementation-shape questions.

The next removal/type-fit walk is:

~~~text
Can Family StateRelay reuse VexRelay directly
without forcing an unrelated deep-copy/type contract?

If yes:
  specialize/reuse it.

If no:
  identify which exact VexRelay law is foundational below both,
  rather than copying synchronized currentness machinery.
~~~

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STATE-RELAY-PRESSURE] -->


## Why V1 Relay has the shape it does — and what StateRelay may inherit

The StateRelay pressure must not copy `VexRelay` merely because its implementation already exists.

V1 Relay was earned from several distinct failures:

~~~text
caller/collector aliases mutable retained truth
-> defensive deepCopy boundary

two causes race and each computes from a different stale snapshot
-> one serialized current-state decision boundary

one reducer recursively mutates the truth it is currently deciding
-> recursion guard

incoming cause is semantically equal to retained truth
-> suppress duplicate current-state publication

provider arrival order differs from provider semantic freshness
-> source-specific freshness remains in the source/reducer law

UI/consumer needs current truth, not every historical transition
-> StateFlow/current are current-state projections, not event ledgers

Compound dependencies change independently
-> retain one coherent input totality and fully re-derive bounded output meaning
~~~

These reasons must stay separate.

### What clearly transfers to Family State currentness

Family now has the same failure shape for several lawful semantic causes:

~~~text
Control activation
provider-reconciled semantic proposal
async Operation completion
future Store/repository observation
~~~

Therefore the transferable StateRelay law is:

~~~text
each lawful cause may arrive independently

BUT

semantic State advancement
-> enters one serialized boundary
-> reads exact CURRENT State there
-> decides whether the cause is still lawful/current
-> derives the next semantic State
-> publishes only if semantic meaning changed
~~~

This explains why the StateRelay pressure test uses `VexRelay.update { current -> ... }`: the old
completion does not carry authority to overwrite State directly. It is forced to answer:

> given CURRENT semantic State now, is my consequence still lawful?

### What does NOT automatically transfer

~~~text
VexState / deepCopy requirement
  V1 Relay needs defensive detachment across ownership boundaries.
  Behavior Family State does not yet universally promise this exact copy contract.

Compound InputState / OutputState
  Compound Relay owns a relationship among independently-owned truths.
  Family StateRelay would own one Family's semantic-State custody.
  Do not flatten provider/repository inputs into Family State merely to reuse Compound Relay.

revision
  useful V1 current-state lineage,
  but not yet proved necessary for every Family StateRelay.

dependency collection / activate(scope)
  belongs to Compound observation ownership.
  StateRelay should not become a universal provider observer.

source freshness
  remains with the rightful source/reconciliation law.
  Serialization orders admitted causes; it does not invent semantic freshness.
~~~

So the current question is **not**:

~~~text
Should Family inherit VexRelay?
~~~

It is:

~~~text
Does Family State currentness need the same retained-current / serialized-decision law?

If yes, which existing VexRelay guarantees are essential to that law,
and which are implementation/ownership details that should stay outside Family?
~~~

The test-only StateRelay currently answers the first question with evidence while holding the second
open.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STATE-RELAY-WHY] -->

## Nested current-State pressure — depth is data, not copy code

The earlier nested-list pressure exposed the right failure but still required feature-specific
recursive copy knowledge. That is not sufficient as a reusable Family currentness model.

The refined pressure normalizes the owned hierarchy by stable semantic identity:

~~~text
[A1[B1[C1]]], [A2[B2]]

becomes:

rootRefs = [A1, A2]

nodes:
  A1 -> children [B1]
  B1 -> children [C1]
  C1 -> children []
  A2 -> children [B2]
  B2 -> children []
~~~

Now hierarchy depth is **data**, not handwritten preservation logic.

Updating C1 means:

~~~text
CURRENT tree
-> resolve node C1 by ref
-> replace C1
-> retain the same root refs and every untouched node

A1 / B1 / A2 / B2 do not need to be reconstructed or named merely to survive.
~~~

The fourth Origin reference proves this at two scales:
- the exact `[A1[B1[C1]]], [A2[B2]]` shape;
- a generated 48-level chain where the deepest node changes without any feature-specific recursive
  copy code.

### Structural completeness

The normalized tree owns:

~~~text
rootRefs
node.ref
node.childRefs
node index
~~~

Every root/child ref must resolve to one current node. Dangling refs, duplicate refs, unreachable
nodes and child cycles are rejected by the snapshot rather than becoming silent display omissions.

Display traversal therefore reads one coherent current hierarchy:

~~~text
root refs
-> node
-> child refs
-> child node
-> ...
~~~

It does not use list index or retained object reference as semantic identity.

### Ownership detachment is separate

The snapshot detaches caller-owned structural collections at admission:

~~~text
caller mutates original root list / child list / node list afterward
-> retained current hierarchy remains unchanged
~~~

This preserves the V1 Relay reason for defensive detachment without making recursive deep-copy
knowledge the identity model.

Permanent distinction:

~~~text
STABLE REF
  -> which semantic thing / relationship is this?

SNAPSHOT DETACHMENT
  -> can another owner mutate my retained structure through an alias?

DEEP COPY
  != identity
~~~

The normalized tree pressure currently uses immutable fixture node values. A real node value that
contains mutable owned internals still needs its own lawful immutable/snapshot boundary. The tree
solves structural identity/hierarchy completeness; it does not magically freeze arbitrary mutable
domain payloads.

### Independently-owned child currentness remains Convergence

If B1/B2 have their own currentness owners, do not absorb their whole State into the normalized parent
tree merely for rendering.

~~~text
parent membership/hierarchy refs --------\
                                          -> Compound Convergence
child-owner current projections ---------/        |
                                                   v
                                            bounded current output
~~~

So the current law is:

~~~text
parent-owned nested structure
  -> normalized identity tree

independently-owned child currentness
  -> stable refs + Convergence
~~~

No universal StateTree type is admitted yet. The test fixture exists to pressure the reusable law.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][NESTED-STATE-TREE-PRESSURE] -->

## Fourth Origin reference — StateRelay and nested currentness

StateRelay/current-tree pressure lives in:

~~~text
VexBehaviorV2StateRelayOriginPressureTest
~~~

The fourth reference now pressures three different responsibilities:

~~~text
StateRelay
  serialized CURRENT-State decision
  stale completion re-evaluated against CURRENT

normalized owned State tree
  arbitrary-depth hierarchy by stable refs
  complete current structural snapshot
  no ancestor reconstruction required to preserve descendants

Compound Convergence
  parent refs/order/hierarchy + independently-owned child currentness
  -> one bounded coherent output
~~~

This avoids both:

~~~text
recursive manual copy chains
  -> feature author must remember every descendant to preserve it

Family-per-row explosion
  -> every nested value gains an unnecessary independent currentness owner
~~~

The deciding question remains ownership/currentness, not collection depth.

The normalized-tree isolated Kotlin proof passes for a 48-level chain and for caller-owned mutable
structural collections being detached after admission. Repository Gradle/JVM proof remains pending.

No universal StateTree or production StateRelay type is admitted by this checkpoint.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STATE-RELAY-FOURTH-REFERENCE] -->


## Live StateRelay Family loop — where tree mutation actually runs

The fourth Origin reference now includes an integrated test-only Family specimen.

The key placement is:

~~~text
UI / caller
-> Family.activate(Control)
-> StateRelay.process { CURRENT ->
     Acts.resolve(CURRENT, Control)
     -> selected Act
     -> immediate disposition
     -> next semantic State
   }
-> StateRelay publishes changed CURRENT State
-> Family.DisplayState derives from StateRelay.current
~~~

For the nested-tree specimen:

~~~text
Control.RenameNode(ref = C1)
-> StateRelay gives exact CURRENT Family State
-> Acts resolves RenameNode against that CURRENT State
-> Act admits:
     current.copy(
       tree = current.tree.updateLabel(C1, ...)
     )
-> StateRelay publishes NEXT
-> DisplayState walks the same NEXT tree
~~~

The reusable boundary is therefore **not** tree-specific:

~~~text
StateRelay
  owns:
    exact CURRENT read
    serialized current-State decision
    semantic publication

Acts / Family State
  own:
    what a Control means
    which State transformation is lawful

normalized tree
  owns:
    arbitrary-depth structural identity/completeness

DisplayState
  owns:
    bounded outward projection
~~~

A later async/provider result must re-enter the same StateRelay process boundary and be evaluated
against CURRENT State there. It does not receive direct write authority.

The current integrated specimen is still test-only and does not change VexBehaviorV2Family production
currentness yet.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STATE-RELAY-LIVE-LOOP] -->


## Process-only Compound template — reusable causal skeleton

The Origin review identified repeated ceremony in existing Compound Relays:

~~~text
incoming partial InputState
-> read retained current InputState
-> manually merge latest fields
-> relationship calculation
-> manually rebuild coherent InputState
-> manually construct OutputState
-> manually copy both into VexCompoundState
~~~

Only the relationship calculation should be feature-specific.

The fourth Origin reference now carries a test-only reusable template:

~~~kotlin
final override fun processStates(
    inputState: InputState,
    currentState: VexCompoundState<InputState, OutputState>,
): VexCompoundState<InputState, OutputState> {
    val latestStateInput =
        currentState.inputState.latest(inputState)

    val latestStateOutput =
        process(latestStateInput)

    return currentState.copy(
        inputState = latestStateInput,
        outputState = latestStateOutput,
    )
}

protected abstract fun process(
    latestStateInput: InputState,
): OutputState
~~~

A concrete relationship therefore reads only:

~~~kotlin
override fun process(
    latestStateInput: ExampleInputState,
): ExampleOutputState {
    // What does this complete current relationship mean?
}
~~~

### Where latest-input merging belongs

The retained InputState owns how one partial patch becomes one complete latest input:

~~~text
current retained InputState
+ incoming partial InputState
-> InputState.latest(incoming)
-> latestStateInput
~~~

That keeps patch/currentness mechanics beside the input shape and out of every relationship
calculation.

For example:

~~~kotlin
override fun latest(
    incoming: ExampleInputState,
): ExampleInputState =
    copy(
        left = incoming.left ?: left,
        right = incoming.right ?: right,
    )
~~~

The Relay author then never repeats that merge inside `process(...)`.

### Stronger readability law

A fresh reader should be able to witness every Compound relationship in the same sequence:

~~~text
WHAT ARRIVED?
  input patch

WHAT IS CURRENT NOW?
  latestStateInput

WHAT DOES THAT CURRENT RELATIONSHIP MEAN?
  process(latestStateInput)

WHAT IS EXPOSED?
  latestStateOutput

WHAT IS RETAINED?
  the concrete Family's CURRENT Compound State
~~~

This is the scalability value: the framework owns the repeated causal skeleton; each feature owns only
its relationship meaning.

### Existing-feature pressure

Current source suggests three categories:

~~~text
Conversation Hybrid
  already close to pure latestStateInput -> OutputState

Maps Style
  can likely fit once selection-change reset semantics live in InputState.latest(...)

Events / Navigation
  currently read previous OutputState as process memory
~~~

The last category is a useful pressure, not a reason to contaminate the generic process signature.

If a relationship needs retained memory to decide the next output, make that memory an explicit part
of the relationship's current processing state rather than making OutputState secretly serve as both:

~~~text
consumer output
AND
hidden reducer memory
~~~

The next Origin refinement should determine the smallest truthful representation for that retained
relationship memory while preserving the desirable public law:

~~~text
latestStateInput
-> process(...)
-> latestStateOutput
~~~

The earlier Relay pressure remains useful evidence, but the reusable law is now integrated into the
V2 Family foundation as VexBehaviorV2CompoundFamily. Production VexCompoundRelay remains unchanged;
a Compound Family no longer needs a sibling Relay merely to own a second copy of its Input/Output
relationship truth.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][PROCESS-ONLY-COMPOUND] -->


## Formalized process -> commit -> Act -> observe circuit

The process-only circuit is now represented directly by the Compound Family foundation and the clean
VexBehaviorV2FamilyCircuitTestSampleSuite teaching specimen.

The reusable relationship loop is:

~~~text
incoming partial InputState
+ retained current InputState
-> latestStateInput

latestStateInput
-> process(latestStateInput)
-> latestStateOutput

latestStateInput + latestStateOutput
-> commit concrete Family.current CompoundState

COMMITTED OutputState
-> Act / realization
-> bounded request to rightful other owner

other owner changes its own CURRENT State
-> observed owner truth
-> NEW partial InputState

NEW input
-> latestStateInput
-> process again
~~~

Permanent rule:

~~~text
OutputState
  = current relationship consequence

OutputState
  != hidden input memory

process(...)
  does NOT mutate another owner

Act
  does NOT run until the CompoundState carrying its OutputState is already committed
~~~

### Current teaching specimen

The clean construction suite now demonstrates the same law through a deliberately generic
ComplexFamily:

~~~text
ScreenFamily.State
MusicQueueState
ThemeModeState
        |
        v
ComplexInputState.latest(...)
        |
        v
ComplexFamily.process(latestStateInput)
        |
        v
ComplexOutputState
        |
        v
ComplexCompoundState = ComplexFamily.current
        |
        v
Act
        |
        v
ScreenFamily.Control.Show(...)
        |
        v
ScreenFamily CURRENT State
        |
        +---- observed as NEW ComplexInputState ----+
                                                    |
                                                    v
                                                process again
~~~

The end-to-end trace is:

~~~text
process:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)
commit:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)
act:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)
screen-family:PlayerSurface
observe:screen:PlayerSurface
process:Settled(PlayerSurface,track.alpha,Dark)
commit:Settled(PlayerSurface,track.alpha,Dark)
act:Settled(PlayerSurface,track.alpha,Dark)
~~~

This makes the ownership boundary explicit:

~~~text
relationship owner
  calculates what current truths mean

Act
  reacts to established relationship truth

target owner
  owns whether the requested reality actually changes

observation
  reports that new truth back as fresh input
~~~

### Why this matters for retained relationship memory

If a future `process(latestStateInput)` appears to require yesterday's OutputState, first ask what fact
that output was carrying.

If the fact matters to today's calculation, it needs a truthful current owner and must arrive in
`latestStateInput`. Do not turn OutputState into a covert feedback channel merely to preserve reducer
history.

The Family foundation and clean teaching suite now formalize this circuit. Existing Events, Maps,
Navigation and Conversation Relay implementations are deliberately unchanged; they remain migration
pressure rather than alternate standards.

Isolated Kotlin circuit proof: PASS.
Repository Gradle/JVM proof: PENDING.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][PROCESS-COMMIT-ACT-OBSERVE] -->

## Committed Output -> Act runner pressure

The next Origin pressure starts exactly where the ownership circuit above stops:

~~~text
COMMITTED OutputState
-> lawful Act realization
~~~

The new test-only runner pressure rejects two tempting shortcuts.

First, the full Compound revision is **not** itself an Act trigger. An InputState patch may change
retained current facts and advance the Compound revision while producing the same OutputState. That
is useful relationship currentness, but it is not a new consequence to execute.

Second, outputStates is a current-state projection, not an event ledger or command queue. A
production runner must not depend on collecting that projection later and pretending every committed
consequence is recoverable as an execution event.

The smallest current candidate is instead:

~~~text
process(latestStateInput)
-> next OutputState
-> COMMIT CompoundState(revision = N)

if Output meaning changed:
  form one ephemeral committed-Output receipt
    commit identity = existing Compound revision N
    output = committed OutputState

  deliver that receipt at the commit boundary
  -> Act runner

if only Input meaning changed:
  no Act receipt
~~~

No second Output revision counter is earned. The already-existing Compound revision is sufficient to
identify the exact commit **when** that commit actually changed Output meaning.

This distinction matters for both execution modes.

### Replaceable

~~~text
Output commit N
  NeedsApply(Dark)
  -> begin Replaceable reaction

new Compound revision N+1
  input changed only
  Output still NeedsApply(Dark)
  -> reaction remains current
  -> no replay / no cancellation

new Output-changing commit N+2
  Settled(Light)
  -> supersedes N
  -> cancel obsolete Replaceable reaction
  -> even though Settled has no Operation of its own
~~~

So Replaceable currentness follows the latest **Output-changing committed consequence**, not every
input/currentness revision and not merely the existence of another effect-bearing Act.

Cancellation changes execution lifetime. It does not erase or roll back the already-committed
relationship truth.

### Sequenced

~~~text
commit N
  NeedsApply(Dark)
  -> accept Dark episode

commit N+1
  NeedsApply(Light)
  -> accept Light episode after Dark

Dark target truth returns
  -> Input changes
  -> Output still NeedsApply(Light)
  -> Compound revision advances
  -> NO second Light receipt

Light target truth returns
  -> Output changes to Settled(Light)
  -> committed no-op consequence
~~~

Sequenced currentness is accepted episode custody/order. An older accepted episode does not become
illegal merely because a newer Output consequence was committed after it.

The pressure also delivers the same commit receipt twice and executes it once, making exact commit
identity the duplicate boundary.

### Cancellation and external reality

The test gate is intentionally placed before the rightful target owner changes so cooperative
Replaceable cancellation can prove the simplest case. This does **not** claim cancellation can undo
an external effect that already happened.

Permanent ownership remains:

~~~text
relationship commit
  owns relationship truth

Act runner
  owns reaction execution lifetime

target owner
  owns whether target reality changes

observation
  returns actual target truth as NEW InputState

cancellation
  != relationship rollback
  != external rollback
~~~

The current receipt helper wraps send(...) only inside the pressure fixture. That is not a
production seam: dependency-driven VexCompoundRelay.send(...) could bypass an outer wrapper.

If this law survives further pressure, a production post-commit seam must originate at the actual
Compound commit boundary and expose only the smallest changed-Output receipt needed by the runner.

No production Act runner, Relay hook, event ledger or new revision type is admitted by this
checkpoint.

Repository Gradle/JVM proof remains pending.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][COMMITTED-OUTPUT-ACT-RUNNER-PRESSURE] -->
