
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
- Review pressure/evidence:
  [`REVIEW-CHECKPOINT.md`](REVIEW-CHECKPOINT.md)





## V2 Origin-Walk test progression

The V2 tests are deliberately an **evolution sequence**, not three interchangeable examples.

Read them in this order:

~~~text
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
   -> current exact-value V2 Control matching does NOT yet generalize runtime payloads
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

### Still OPEN — do not generalize by syntax

~~~text
payload-bearing Controls
  Click(songId)
  Seek(position)
  Send(message)
  Select(routeId)

value-transforming State
  State.Volume(x) + Increase
  Cart(items) + Remove(itemId)

externally observed current truth
  Media3
  location
  provider/repository truth
  SystemAppearance convergence

observable Family State / DisplayState for Compose

Control availability / UI affordance projection

typed Operation result payload

multiple Family-instance execution identity

Sequenced lane queue/custody semantics

provider-affine handoff from begin without local coroutine folklore

cross-version durable identity / rename migration

privacy-bounded accountant payload
~~~

The current `VexBehaviorV2Family` runtime should therefore be read as the locally-owned ThemeMode
patient-zero realization of the stable authoring language, not as proof that Family itself owns every
provider/repository truth.

<!-- [VXG RealForever][VEX-BEHAVIOR][V2][STABILITY-BOUNDARY] -->
