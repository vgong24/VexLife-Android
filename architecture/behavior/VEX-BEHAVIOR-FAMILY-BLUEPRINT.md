
# Vex Behavior Family Package Blueprint

[VXG RealForever]

This is the canonical construction pattern for a durable Behavior family.

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

## Optional roles are earned

A family may additionally need:

~~~text
<Name>Connection.kt
<Name>ConnectionHost.kt
<Name>Presentation.kt
<Name>Pending.kt
<Name>Types.kt
~~~

Only create them when the responsibility actually exists.

### Connection

Use a concrete Connection when a provider/runtime boundary must:

~~~text
observe provider truth
-> form semantic State

activate(Control)
-> re-resolve current Act
-> select Effect
-> realize Effect through provider/runtime

later provider observation
-> admit/reject/update semantic State
~~~

A Connection may implement VexPort or VexPortFlow, but the concrete class keeps an ordinary role name:

~~~text
Media3VexStreamPlaybackConnection
VexMapsLocationConnection
~~~

Do not rename every implementation to Port.

### ConnectionHost

Use a host only when platform lifetime needs a real owner.

Examples:

~~~text
Android ViewModel
session
service
repository composition root
~~~

The host owns lifetime, not semantic truth.

### Presentation

Use a feature-local Presentation mapping when several consumers need the same visual interpretation:

~~~text
BehaviorProjection
    -> icon
    -> enabled
    -> selected
    -> label
    -> spinner/visual treatment
~~~

Presentation is not another behavior truth.

### Pending

Use a feature-local pending/reconciliation seam only when local admission and delayed provider observation differ in time.

~~~text
admitted local request
!=
provider acknowledgement so far
~~~

Pending implementation must not become a generic substitute for provider truth.

### Types

Use feature-local type aliases when foundation generics are correct but too noisy for ordinary feature/Compose code.

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

# The four-move semantic grammar

Every family answers the same four questions:

~~~text
STATE
    what is actually true now?

CONTROL
    what stable affordance did the human interact with?

ACT
    what does that Control mean from current State?

EFFECT
    what runtime transformation should be attempted?
~~~

Causal reading:

~~~text
observed State
-> Control resolves Act
-> Act selects Effect
-> Effect is admitted for dispatch
-> immediate admitted State
-> provider observation
-> next truthful State
~~~

Act asks. Effect attempts the travel. State tells us what reality answered. Control decides which Act one affordance represents from current truth.

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

# VexPort boundary

The reusable two-way boundary is:

~~~text
VexPort<Family, State, Control, Act>
    currentProjection
    activate(Control)

VexPortFlow<Family, State, Control, Act>
    VexPort
    + StateFlow<BehaviorProjection>
~~~

The UI sends Control identity back.

It does not execute the Act it rendered earlier.

~~~text
render:
    Control -> Act A

time passes
State changes

click:
    activate(Control)
    -> re-resolve from CURRENT State
    -> maybe Act B
~~~

This protects stale render -> activation meaning.

It does NOT replace async completion gates such as:

~~~text
request generation
revision
expected key
latest-request gate
Relay admission
~~~

Those remain concrete-owner responsibilities.

VexPort also does not prescribe locking, dispatcher ownership, actor serialization, or provider threading.

# Higher convergence stays above families

Do not cross-multiply independent truths into one Behavior family.

Avoid:

~~~text
PlayingAndIncomingCall
LocationReadyAndNavigationModalOpen
PausedAndProviderDisconnectedAndEventWaiting
~~~

Prefer:

~~~text
Playback Projection -----\
Location Projection ------+-> higher convergence / attention owner
Events Projection --------/
~~~

One Behavior family answers one local behavioral question.

Convergence coordinates several already-complete truths.

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
current State
+ Act
-> Effect
-> admitted State kind
~~~

Keep current truth visible.

## 7. Add a Connection only if required

If the family crosses into provider/runtime/lifecycle mechanics, add a concrete Connection.

Preserve:

~~~text
provider ownership
async currentness gates
concurrency/serialization guarantees
~~~

Do not move those into VexPort by assumption.

## 8. Add feature-local aliases when generics leak

If ordinary callers repeatedly see the full generic equation, hide it behind meaningful local names.

## 9. Add <Name>Docs.kt

Make the package searchable and traversable in the IDE.

The Docs node should answer questions and link directly to source symbols.

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
| Lifetime host | [VexStreamPlaybackConnectionViewModel](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackConnectionHost.kt) |
| Pending reconciliation | [VexStreamPlaybackPending](../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackPending.kt) |
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
12. Is this still one behavioral question, or has it earned a sibling family?

If those answers require reconstructing a hidden switchboard, the package has lost the pattern.

<!-- [VXG RealForever][VEX-BEHAVIOR][FAMILY-PACKAGE-BLUEPRINT] -->
