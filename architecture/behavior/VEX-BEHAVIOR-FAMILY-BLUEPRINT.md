
# Vex Behavior Family Package Blueprint

[VXG RealForever]

This is the canonical construction pattern for one durable **local SCAE Behavior family**.

The canonical SCAE definition lives in [Vex Behavior](README.md#scae-architecture). This blueprint
constructs the local State / Control / Act / Effect family; Convergence Relay is SCAE's fractal
cross-State composition pattern, not a fifth family role.

It exists so a new feature does not have to reverse-engineer VexStream Playback to discover what the architecture expects.

VexStream Playback remains the prime worked implementation, but the example does not own the pattern.

## Two traversal modes

The documentation pattern is intentionally dual:

~~~text
IDE / Android Studio
    <Name>Docs.kt
    -> KDoc links jump to real symbols

GitHub / web
    this Markdown blueprint
    -> relative links jump to exact source files
~~~

Use Markdown for long-form laws, diagrams, split rules, and reviewable explanation.

Use Docs.kt for searchable code-native orientation.

Neither replaces source authority.

## The durable shape

A production Behavior family should normally be discoverable as one human feature/subfeature package:

~~~text
<feature-or-subfeature>/
├── <Name>Docs.kt
├── <Name>Family.kt
├── <Name>State.kt
├── <Name>Control.kt
├── <Name>Act.kt
├── <Name>Effect.kt
└── <Name>Behavior.kt
~~~

Those seven names answer seven different questions:

| File | Question |
|---|---|
| <Name>Docs.kt | Where do I start and what source answers each question? |
| <Name>Family.kt | Which State / Control / Act / Effect types are compiler-bound together? |
| <Name>State.kt | What can truthfully be true now? |
| <Name>Control.kt | What stable human-facing affordances exist? |
| <Name>Act.kt | What can the human/product mean from current truth? |
| <Name>Effect.kt | What runtime transformation may be attempted? |
| <Name>Behavior.kt | How are State + Act -> Effect -> admitted State relationships authored? |

This is the semantic core.

Do not add files merely to make the directory visually symmetrical.

## Runtime boundary first; supporting roles are earned

The semantic core does not arrive with an optional-file bundle.

The next question is simply:

~~~text
Does this family cross a runtime/provider boundary?
~~~

If no, the family may remain only:

~~~text
Docs
Family
State
Control
Act
Effect
Behavior
~~~

If yes, earn one concrete Connection.

### Connection

The Connection is the reality boundary between semantic language and provider/runtime language:

~~~text
provider/runtime
    ↕ observe / realize / reconcile
Connection
    ↕
Behavior family / Projection
~~~

A concrete Connection must answer:

1. What provider/runtime does it bridge?
2. Who owns this Connection's lifetime?
3. What currentness/revision gates must it preserve?
4. What concurrency/serialization guarantees must it preserve?

A Connection may implement `VexPort` or `VexPortFlow`, but keeps an ordinary concrete role name:

~~~text
Media3VexStreamPlaybackConnection
VexMapsLocationConnection
~~~

Do not rename every implementation to Port.

This corresponds naturally to Ports & Adapters / Hexagonal architecture:

~~~text
VexPort
    semantic port

concrete Connection
    adapter between semantic family and provider/runtime
~~~

Use that established understanding before inventing another architectural noun.

### Lifetime ownership

Do not manufacture a generic Host abstraction or Host file by default.

The rightful lifetime owner may already be:

~~~text
ViewModel
Session
Service
repository/composition owner
another existing platform owner
~~~

The family should be able to answer **who owns the Connection's lifetime** without requiring a new architectural primitive.

### Reconciliation data

`Pending` is not a peer architecture role and is never a fifth Behavior concept.

It is ordinary Connection-owned data when admitted local intent and provider acknowledgement differ in time:

~~~text
admitted local request
!= provider acknowledgement so far
~~~

Extract a dedicated file/type only when the reconciliation logic is large enough to benefit from it.

### Display State

A feature may wrap an already-derived output in one sealed `DisplayState` when the UI currently
repeats conditionals to decide how that output should be treated.

This is a **deterministic read classification**, not another truth owner or state machine:

~~~text
arrived Projection
-> classify current display condition
-> DisplayState
-> UI treatment
~~~

VexStream Playback is the first pressure example:

~~~text
no Projection
-> Unknown

Projection(State.NoMedia)
-> Inactive(projection)

Projection(pending admitted intent)
-> Transitioning(projection)

ordinary displayable Projection
-> Active(projection)
~~~

The Projection remains the semantic output. DisplayState merely preserves the condition that would
otherwise be expressed as scattered `if` / `null` / fallback checks in each view.

A view may render two DisplayState cases identically today and differently later. That does not
change the source truth.

Do not add DisplayState when the UI has no meaningful conditional treatment to preserve, and do not
promote a generic `VexDisplayState` until repeated real consumers earn the same algebra.

### Presentation

Presentation may still derive visual vocabulary such as icon, enabled treatment, labels, or styling
from a Projection or DisplayState.

Presentation does not own semantic truth and is not another `VexBehaviorFamily` member.

### Experience membrane

If Compose repeatedly assembles the same Behavior plumbing:

~~~text
Connection / VexPortFlow
Projection observation
DisplayState classification
Control activation
~~~

earn one feature-local `<Name>Experience` membrane:

~~~text
<Name>Experience
    displayState
    activate(Control)
~~~

The Experience owns **composition of the read/write boundary**, not semantic truth.

Internally:

~~~text
provider/runtime
    ↓
Connection / VexPortFlow
    ↓
Projection
    + provider-derived read data when needed
    ↓
DisplayState
    ↑
Experience
    ↓ activate(Control)
VexPort
~~~

Compose should depend on the Experience instead of knowing how Connection, PortFlow, Projection and
DisplayState are assembled.

This keeps screens scalable when several independent experiences coexist:

~~~kotlin
val playback =
    rememberVexStreamPlaybackExperience()

// later, only when independently earned:
val maps =
    rememberVexMapsLocationExperience()

val calls =
    rememberVexCallsExperience()
~~~

Do not hide unrelated provider operations inside the Experience merely to make the membrane look
perfect. If Previous / Next / Seek / Queue are still explicitly outside the admitted Behavior slice,
their remaining provider path should stay visible until that semantic seam is earned.

Do not mint a generic `VexExperience<T>` from one feature. Let a second real Experience prove the
common type equation first.

### Human-facing aliases

Use feature-local aliases when foundation generics are correct but too noisy for ordinary feature/Compose code.

Example:

~~~text
VexStreamPlaybackConnection
VexStreamPlaybackConnectionFlow
VexStreamPlaybackProjection
~~~

The alias hides ceremony, not safety.

# The Family contract

Every durable Behavior family declares one compile-time family identity:

~~~kotlin
sealed interface ExampleFamily :
    VexBehaviorFamily<
        ExampleFamily
    >
~~~

Then each semantic alphabet belongs to it:

~~~kotlin
sealed interface ExampleState :
    VexBehaviorFamily.State<
        ExampleFamily
    >

enum class ExampleControl :
    VexBehaviorFamily.Control<
        ExampleFamily
    >

sealed interface ExampleAct :
    VexBehaviorFamily.Act<
        ExampleFamily
    >

sealed interface ExampleEffect :
    VexBehaviorFamily.Effect<
        ExampleFamily
    >
~~~

This produces two forms of compiler protection:

~~~text
ROLE SAFETY
    State cannot occupy Act / Control / Effect position

FAMILY SAFETY
    AnotherFamily.Control cannot be combined with ExampleState / ExampleAct
~~~

Family is compile-time kinship only.

It is not:

~~~text
runtime owner
service
singleton
state store
coordinator
convergence owner
~~~

# The semantic equations

Always write the full dependency equations rather than shorthand that hides State:

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

State is current context feeding several decisions.

It is **not** an object that must be mechanically piped through every layer and it is not a reason to build one giant `when(state)` switchboard.

## Behavior is the semantic rulebook

~~~text
State / Control / Act / Effect
    vocabulary

Behavior
    lawful relationships among that vocabulary
~~~

`VexBehaviorBlueprint` should remain independent of provider APIs, Android lifecycle, UI rendering, repositories, and concurrency mechanics.

Behavior answers:

~~~text
Given CURRENT State and Act,
what Effect is lawful
and what State kind is immediately admitted?
~~~

Control projection separately answers:

~~~text
Given CURRENT State and this Control,
what Act does this affordance mean now?
~~~

## Semantic-first Control meaning

Do not make a Boolean the source of meaning when it immediately creates the question "why?".

Prefer:

~~~text
Control meaning:
    Available(Act.Play)
    Available(Act.Pause)
    Unavailable
~~~

with Boolean convenience such as `isAvailable` derived afterward.

This preserves the semantic reason while still supporting ordinary UI conditions.

# State kind versus State flavor

Do not create another State type merely because one field changes.

Create a distinct State kind when the difference changes the semantic neighborhood:

~~~text
what Acts have meaning
what Controls resolve
what the truth fundamentally means
~~~

Keep ordinary variation as data/flavor when it remains the same kind of truth.

Example:

~~~text
Paused
Playing(speed = Normal)
Playing(speed = FastForward)
Playing(speed = Rewind)
~~~

Fast-forward can remain flavor inside Playing.

# Same State kind + same Act

A data-rich State may need the same Act to select different Effects.

That does not automatically require more State subclasses.

~~~text
Positioned(hasNext=true)
+ Next
-> Advance

Positioned(hasNext=false, wrapAtEnd=true)
+ Next
-> Wrap
~~~

Several same-State-kind + same-Act relationships are valid when their conditions are mutually exclusive for the exact current State value.

If more than one becomes valid simultaneously, the Blueprint rejects ambiguity rather than choosing by declaration order.

# Admission is not provider outcome

A Behavior relationship declares the immediate admitted State kind, not a guaranteed final provider result.

~~~text
Idle
+ Locate
-> RequestCurrentLocation
-> admit Finding
~~~

The provider may later report:

~~~text
Ready
Denied
Disabled
Unavailable
Error
~~~

without contradicting the graph.

Use admit<State>() when Effect admission changes the State kind.

Use stay() when the same State kind remains current while data/flavor changes.

A concrete Connection may construct the admitted State value when runtime information such as generation, revision, timestamps, pending flags, or provider-specific data is required.

The value must agree with the graph's admitted State kind.

# Connection is the reality boundary

The Behavior family is the semantic rulebook; the concrete Connection bridges it to reality.

~~~text
provider/runtime
    ↕ observe / realize / reconcile
Connection
    ↕
State / Behavior / Projection
~~~

The Connection may naturally be the busiest concrete class because it speaks both languages.

That is acceptable when the responsibilities remain visible:

~~~text
provider observation -> semantic State
Control activation -> re-resolve current Act
Act -> Behavior -> Effect
Effect -> provider realization
provider result -> reconcile/admit current State
~~~

Do not move provider, lifecycle, or concurrency mechanics into Behavior merely to make the Connection shorter.

# Behavior DSL integrity

The Behavior DSL itself is foundational language.

Canonical relationship terminals are:

~~~kotlin
state<Current> {
    on(Act, Effect)
        .stay()

    on(OtherAct, OtherEffect)
        .admit<NextState>()
}
~~~

Every authored `on(...)` must terminate in exactly one `.stay()` or `.admit<State>()`.

An unfinished relationship is not ignored. Blueprint construction fails because every relationship
must answer:

> What State kind is admitted when this Effect is accepted for dispatch?

`NextState` is constrained to the same Family's State vocabulary and the Blueprint verifies that
the admitted State kind is registered.

The old `.to<State>()` spelling remains only as a compatibility alias for `.admit<State>()`.

# VexPort boundary

The reusable two-way semantic port is:

~~~text
VexPort<Family, State, Control, Act>
    currentProjection
    activate(Control)

VexPortFlow<Family, State, Control, Act>
    VexPort
    + StateFlow<BehaviorProjection>
~~~

The UI sends Control identity back, not the Act rendered earlier.

~~~text
render:
    Control + State A -> Act A

time passes
State changes

click:
    activate(Control)
    -> re-read CURRENT State B
    -> maybe Act B
~~~

This protects stale render -> activation meaning.

It does **not** replace:

~~~text
request generation
revision
expected key
latest-request gate
Relay admission
provider timeout/failure recovery
~~~

Those remain concrete-owner responsibilities.

VexPort also does not prescribe locking, dispatcher ownership, actor serialization, or provider threading.

# Cross-family behavior stays convergence-based

No family may realize another family's Effect directly.

If Family A changes and Family B should respond:

~~~text
A completes its own loop
-> new A truth
-> AB convergence evaluates A + B
-> bounded desired relationship/request toward B
-> B receives its own Control through B's Port
-> B re-resolves against CURRENT B State
-> B Behavior selects B Effect
-> B completes its own loop
-> new B truth
-> convergence evaluates again
~~~

Rule:

> **No cross-family Effect bypass.**

A convergence node remains one-way/pure:

~~~text
dependency truths / projections
-> convergence output / desired relationship
~~~

If that output requires another family to change, a separate activation/reconciliation boundary carries a **Control**, not that family's Effect.

This gives the safe fractal form:

~~~text
A + B
-> AB relational truth

AB + C + D + E
-> higher convergence
~~~

Higher convergence consumes coherent envelopes/projections without flattening independent families into a Cartesian mega-State.

## Sequential semantics do not require sequential I/O

Provider observation, fetch, cache, or preparation work may run concurrently:

~~~text
parallel provider preparation
-> validated current truths
-> small serialized semantic decisions
~~~

The causal safety preference is:

~~~text
complete local truth-changing loop
-> publish current truth
-> next dependent convergence decision
~~~

Optimize expensive I/O around that path rather than deleting the path and mutating downstream families directly.

# Industry correspondence — reassurance, not renaming

The current pattern has recognizable correspondence with established architecture ideas:

~~~text
DDD / bounded contexts
    -> semantic Family and split boundaries

Vertical Slice
    -> keep one behavioral question cohesive locally

Hexagonal / Ports & Adapters
    -> VexPort + concrete Connection/provider boundary

CQRS / read models
    -> Projection as derived current read model

statechart ideas
    -> keep orthogonal truths independent instead of Cartesian mega-states
~~~

These correspondences are useful checks, not instructions to rename Vex concepts.

Before inventing another reusable primitive, first ask whether established architecture vocabulary already describes the pressure.

# The split law — when one package becomes two

Do not split because a file is long.

Split when the semantic question has actually split.

A sibling family/package is earned when several of these become true:

1. It has an independently meaningful current truth.
2. Its Controls / Acts / Effects can be understood independently.
3. It may evolve or be observed without the original family.
4. It could be projected or converged independently.
5. Its provider/currentness/lifetime mechanics differ materially.
6. A developer can ask about it as a separate behavioral question.

Example:

~~~text
"How is playback behaving?"
    -> vexstream/playback/

"How is the queue behaving?"
    -> possibly vexstream/queue/
       only if Queue earns its own State / Control / Act / Effect semantics
~~~

Do NOT split merely because:

~~~text
one file got large
there are many enum members
the provider has several helper classes
a feature has several screens
~~~

Those may be organization pressures, but they do not automatically create another Behavior family.

# Fractal package shape

The pattern repeats at whatever semantic scale earns it:

~~~text
vexstream/
    playback/
        VexStreamPlaybackDocs
        VexStreamPlaybackFamily
        State / Control / Act / Effect / Behavior

    queue/                      # only if earned later
        VexStreamQueueDocs
        VexStreamQueueFamily
        State / Control / Act / Effect / Behavior

vexmaps/
    location/                   # strong candidate / pressure test
        VexMapsLocationDocs
        VexMapsLocationFamily
        State / Control / Act / Effect / Behavior

    navigation/                 # only if navigation later earns a separate family
        ...
~~~

The repeating shape is durable.

The number of families is not predetermined.

# How to build a new family

Start with the human question:

What isolated behavioral question am I trying to make understandable?

Then:

## 0. Ground the question in feature source

Before defining SCAE, recover only the context this family is allowed to summarize:

~~~text
purpose source
    feature README / practicum / current accepted semantic owner

whole-human example
    canonical Feature Walk / Journey / lived contract
    OR explicit absence / hold

focused proof
    exact Behavior test / relationship case

boundary
    what this family must not own or claim
~~~

Do not infer feature purpose from the Behavior conditions themselves.

## 1. Name the family

<Name>Family should describe the human/domain question, not the mechanism.

## 2. Define truthful State

Ask:

~~~text
What can truthfully be true now?
Which distinctions change what can happen next?
Which distinctions are only flavor/data?
~~~

Do not start from an existing UI enum merely because its names look convenient.

## 3. Define Controls

Ask:

What stable affordances can the human interact with?

Controls are identity, not current meaning.

## 4. Define Acts

Ask:

What human/product verbs can those Controls mean from current State?

## 5. Define Effects

Ask:

What runtime/provider transformation is attempted for each Act?

Effect is not provider success.

## 6. Author Behavior neighborhoods

~~~text
CURRENT State + Act
-> Effect + admitted State
~~~

Every `on(...)` must terminate in exactly one:

~~~text
.stay()
.admit<NextState>()
~~~

Do not leave a relationship terminal implicit.

Keep current truth visible.

## 7. Add a Connection only if required

If the family crosses a provider/runtime boundary, add one concrete Connection.

That Connection must answer:

~~~text
what provider/runtime does it bridge?
who already owns its lifetime?
what async currentness gates must survive?
what concurrency/serialization guarantees must survive?
~~~

Use the rightful existing lifetime owner when one already exists. Do not create a generic Host abstraction by default.

Do not move provider/currentness/concurrency responsibilities into VexPort or Behavior merely to make the Connection smaller.

## 8. Add feature-local aliases when generics leak

If ordinary callers repeatedly see the full generic equation, hide it behind meaningful local names.

## 9. Add <Name>Docs.kt

Make the package searchable and traversable in the IDE.

The Docs node should answer questions and link directly to source symbols. It is also the broad
source-context doorway for the family:

~~~text
accepted feature-purpose source
-> exact behavioral question
-> canonical human example or explicit absence
-> source-navigation map
~~~

State / Control / Act / Effect / Behavior KDoc should then document only the consequence local to
that role. Do not copy the whole feature story into every file, and do not invent an attractive
product rationale from a DSL condition merely to make the comment sound meaningful.

# Prime implementation — VexStream Playback

VexStream Playback is the first production-shaped implementation of this blueprint.

Package:

~~~text
app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/
~~~

| Meta role | VexStream Playback |
|---|---|
| Navigation root | [VexStreamPlaybackDocs](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackDocs.kt) |
| Family | [VexStreamPlaybackFamily](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackFamily.kt) |
| State | [VexStreamPlaybackState](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackState.kt) |
| Control | [VexStreamPlaybackControl](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackControl.kt) |
| Act | [VexStreamPlaybackAct](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackAct.kt) |
| Effect | [VexStreamPlaybackEffect](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackEffect.kt) |
| Behavior | [vexStreamPlaybackBehavior()](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackBehavior.kt) |
| Connection | [Media3VexStreamPlaybackConnection](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/Media3VexStreamPlaybackConnection.kt) |
| Lifetime host | [VexStreamPlaybackConnectionViewModel](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackConnectionViewModel.kt) |
| Pending reconciliation | [VexStreamPlaybackPending](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackPending.kt) |
| UI membrane | [VexStreamPlaybackExperience](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackExperience.kt) |
| Provider read data | [VexStreamPlaybackReadState](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackReadState.kt) |
| Display classification | [VexStreamPlaybackDisplayState](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackDisplayState.kt) |
| Presentation | [VexStreamPlaybackControlPresentation](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackPresentation.kt) |
| Human-facing aliases | [VexStreamPlaybackConnection / Flow / Projection](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackTypes.kt) |

Important: VexStream Playback demonstrates the meta-blueprint.

It does not define every future family's exact file list.

# Acceptance questions for a new family

A family is healthy when a developer can answer:

1. Where do I start?
2. What can truthfully be true?
3. What Controls exist?
4. What does each Control mean from current State?
5. What Effect does each Act request?
6. What State is admitted immediately?
7. Who realizes the Effect?
8. Who owns provider truth?
9. What protects stale activation?
10. What protects stale async completion?
11. What concurrency/currentness guarantees belong to the concrete owner?
12. Does every on(...) explicitly end in stay() or admit<State>()?
13. Is Control meaning preserved semantically as Available(Act) / Unavailable before Boolean convenience?
14. If views branch on the arrived Projection, is that condition authored once as a feature-local DisplayState rather than repeated as booleans/null checks?
15. If Compose assembles Port + Projection + DisplayState repeatedly, has the feature earned one <Name>Experience membrane?
16. Does that Experience expose only the admitted behavior/display surface rather than hiding unrelated provider debt?
17. Is this still one behavioral question, or has it earned a sibling family?

If those answers require reconstructing a hidden switchboard, the package has lost the pattern.

<!-- [VXG RealForever][VEX-BEHAVIOR][FAMILY-PACKAGE-BLUEPRINT] -->
