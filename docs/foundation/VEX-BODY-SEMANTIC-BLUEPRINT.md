# Vex Body Semantic Blueprint — purpose bridged to form

[VXG RealForever]

## Why this exists

A fresh human or AI should be able to inspect an unfamiliar VexLife Android feature and recover more
than a prose translation of the implementation.

The desired descent is:

~~~text
human / product purpose
-> domain meaning
-> semantic primitives
-> relationships among those primitives
-> established architectural pattern
-> concrete source form
-> runtime / provider manifestation
-> proof
~~~

And the same path should be recoverable backward:

~~~text
implementation
-> source form
-> architectural role
-> relationship
-> semantic primitive
-> product purpose
~~~

This document exists to preserve that bridge.

## The realization

A fresh reader can easily arrive at source like:

~~~text
VexStreamPlaybackBehavior
VexStreamQueueBehavior
VexMapsDeliveryRouteNavigationConvergenceRelay
VexConversationHybridRelay
VexExecution
~~~

and learn five local implementations without ever learning **how those meanings relate**.

That failure mode scales badly. The next feature then looks novel merely because its nouns are
different, comments become English translations of control flow, and architectural reuse depends on
remembering which prior implementation happened to look similar.

The deeper observation is:

> **Domains are different, but many of the first-principle relationships inside them recur.**

For example:

~~~text
Playback
    progression through a media timeline
    -> traversal dynamics

Queue
    determines what may come before / after
    -> traversal ordering

Maps / Navigation
    reachable spatial topology + destination + current position
    -> terrain + coordinated traversal

Conversation
    meaning moves through speaker / recipient / channel relationships
    -> information traversal through addressing topology

Memory
    selected meaning remains recoverable across time
    -> continuity through temporal distance

Process
    work advances through dependencies toward an outcome
    -> traversal through dependency topology

VexExecution
    owns coroutine custody
    -> manifestation/execution mechanics
    -> NOT product-semantic traversal meaning
~~~

These are **not claims that all domains are secretly the same thing**.

The point is that a reader can recognize reusable primitive relationships beneath different domain
vocabulary while preserving what makes each domain truthful.

~~~text
SAME PRIMITIVE RELATIONSHIP
!= SAME DOMAIN
!= SAME OWNER
!= SAME IMPLEMENTATION
~~~

That recognition lets architecture travel without forcing implementation to.

A new feature should therefore not require a fresh human/AI to choose between:

~~~text
memorize every historical Vex noun
OR
reverse-engineer meaning from implementation
~~~

Instead, the reader should be able to ask:

~~~text
What is this feature trying to make possible?
What kind of truth / terrain / request / traversal / transformation is present?
Which of those meanings are independently owned?
How do they relate?
Which current Vex/Android pattern carries that relationship?
Why was that form earned?
What would prove the meaning survived if the form changed?
~~~

That is the perceptual shift this blueprint is trying to preserve.

### What a fresh reader should gain

After reading this document, a fresh human or AI should be able to:

1. **Recognize the same first-principle role under different local names.**
   A route leg, queue position, message path and process dependency are not the same domain object,
   but each may participate in a traversal/topology relationship.
2. **Keep nearby meanings separate when their truth or consequence differs.**
   Queue ordering is not Playback progression; Effect is not provider success; execution custody is
   not product meaning.
3. **Trace purpose into form instead of reconstructing purpose from form.**
   "Why does this owner exist?" should be answerable before reading every reducer/coroutine/provider
   detail.
4. **Reuse architecture without manufacturing abstractions.**
   Shared semantic shape may justify shared vocabulary long before it justifies a generic Kotlin
   interface.
5. **Refactor safely.**
   If implementation form changes later, the primitive relationship and its proof provide the
   continuity target.

A comment such as "combine these flows and derive output" mostly restates syntax. A semantic doorway
should instead help the reader understand **what kind of reality the owner models, why that
relationship exists, and why this source form was chosen to carry it**.

The word **Body** is an orientation metaphor for one coherent product reality composed from
independently-owned semantic parts. It does not introduce a root `VexBody` runtime object, one
inheritance tree, or a requirement that every subsystem use the same mechanism.

"Body" matters because coherence does not require every organ to use the same mechanism:

~~~text
ONE BODY
    shared purpose / causal philosophy / ownership discipline

MANY SEMANTIC PARTS
    different truths, terrains, requests, relationships and manifestations

MANY FORMS
    Behavior, Relay, repository, Connection, Execution, Compose, provider adapters
~~~

The architectural goal is not uniformity. It is **recoverable relationship**.

~~~text
ALIGNMENT
!= SAMENESS

ONE COHERENT BODY
!= ONE GIANT OWNER
!= ONE GIANT STATE
!= ONE UNIVERSAL INTERFACE
~~~

## What this map is — and is not

This is a **semantic coordinate system**.

It helps answer:

~~~text
What world of meaning is this source in?
What first-principle role does it play?
What other truths or requests does it relate?
What architectural pattern currently carries that meaning?
Why is that form useful here?
What neighboring meaning must remain separate?
What proves the meaning survived the implementation?
~~~

It is not:

- a mandatory package skeleton;
- a second feature-purpose source;
- a new runtime ontology registry;
- a requirement for one Kotlin interface per primitive;
- a replacement for SCAE, Convergence Relay, Elements, repositories, VexExecution, Journey or
  Feature Walk;
- permission to infer feature purpose from code when the owning feature source is absent or held;
- a claim that the current list of primitives is metaphysically complete.

Current accepted feature source still owns product meaning. This map helps a reader **locate** that
meaning against reusable first-principle questions.

## Do not put different dimensions into one taxonomy

Several things that appear near one another in source are not peers.

### Domain meaning

Domain meaning answers:

> What part of the human/product world are we modeling?

Examples:

~~~text
Playback
Queue
Conversation
Maps / Navigation
Events
Memory
Home
Relationships
Projects
Devices
~~~

Domains may keep growing. They are not universal architectural roles.

### Semantic primitives

Semantic primitives answer:

> What kind of first-principle meaning does this part of the domain carry?

Examples include current truth, topology, intention, traversal, transformation, observation and
projection.

### Form and substrate

Form/substrate answers:

> What source shape and technology currently carry the meaning?

Examples:

~~~text
Kotlin class / sealed interface / data class
VexBehaviorFamily
VexCompoundRelay
repository
service
ViewModel
StateFlow
coroutine
Media3
Room / SQLite
HTTP
JSON
Compose
~~~

A Kotlin interface is therefore not a peer of Conversation, and a database is not the meaning of
Memory.

Permanent distinction:

~~~text
DOMAIN
!= SEMANTIC PRIMITIVE
!= ARCHITECTURAL FORM
!= TECHNOLOGY / SUBSTRATE
~~~

## Current semantic primitive map

The names below are orientation vocabulary. Local source may use a more precise established term.
Identify the primitive by the **question it answers**, not by string matching.

| Primitive | First-principle question | Common Android realization when earned |
|---|---|---|
| **Identity** | What is this thing, and what remains the same while other facts change? | stable feature/ref/element/domain identity, family kinship |
| **Terrain / Topology** | Where can things exist, relate, or be reached? | route graph, timeline, queue order, channel/thread relationships, dependency graph |
| **State / Position** | What is actually true now / where are we now? | domain State, repository truth, Relay input/output truth |
| **Relationship / Convergence** | What new meaning exists only when independently-owned truths are considered together? | Compound Relay, relationship status, bounded reconciliation request |
| **Intention / Target** | What outcome or destination is being sought? | feature outcome, selected destination, desired work/result |
| **Engagement / Request** | What stable change/traversal can be requested? | SCAE Control, ordinary typed request, semantic interaction |
| **Interpretation** | What does that request mean from current truth? | SCAE Act or another explicit semantic resolution |
| **Traversal / Transition** | What path/change connects current truth to another reachable truth? | route, queue advance, state transition, message delivery path, dependency progression |
| **Transformation / Dynamics** | How may the traversal/change progress or be attempted? | SCAE Effect, playback rate/progression, bounded requested transformation |
| **Constraint / Authority** | What permits, blocks, limits, qualifies, or shapes the change? | Behavior condition, capability/permission/currentness gate, resource/provider constraint |
| **Manifestation / Connection** | What concrete owner touches runtime/provider reality? | Connection, repository/service, provider adapter, SDK/native boundary |
| **Observation** | What did reality/source report back? | repository/provider Flow, callback, result/receipt interpreted as source truth |
| **Projection / Experience** | What bounded meaning may another consumer or human perceive? | Behavior Projection, DisplayState, surface projection, UI |
| **Continuity** | What identity/truth must persist or remain recoverable through time/change? | durable repository state, lineage/revision/currentness, restart/recovery state |
| **Proof** | How do we know the claimed meaning/currentness/outcome is actually true? | focused test, Journey, Feature Walk, provider/device/lived proof |

Several primitives may be answered by one truthful owner. One primitive does not automatically earn
one class/file/interface.

Likewise, one current source object may contain several primitive responsibilities. That is a
refactoring signal only when those responsibilities already answer genuinely different questions,
have different owners/consequence radii, or need independent proof.

## SCAE inside the primitive map

SCAE is the reusable **local causal grammar** of the Body map:

~~~text
State
    current truth / position

Control
    stable engagement / request

Act
    interpretation of that request from current truth

Effect
    selected transformation that may be attempted
~~~

Canonical equations remain:

~~~text
Control + CURRENT State
-> Act

CURRENT State + Act
-> Effect + admitted State
~~~

Placed against the wider primitive map:

~~~text
State                  -> State / Position
Control                -> Engagement / Request
Act                    -> Interpretation
Effect                 -> Transformation / Dynamics
Connection             -> Manifestation
provider observation   -> Observation
Projection             -> Projection / Experience
Convergence            -> Relationship / Convergence among independently-owned current truths
~~~

SCAE does not need extra letters for Terrain, Projection, Connection, Proof or Convergence. Those
meanings surround and compose with the local causal circuit without becoming fake sequential stages.

## Convergence as fractal relationship composition

When independently-owned truths have meaning only in combination:

~~~text
complete truth A
complete truth B
complete truth C
        |
        v
explicit relationship
        |
        +-> relationship truth
        |
        +-> optional bounded request toward the rightful owner
~~~

That is the semantic primitive **Relationship / Convergence** expressed through the current Compound
Relay pattern.

Convergence does not erase the participating owners.

~~~text
RELATIONSHIP TRUTH
!= INPUT OWNER TRUTH

CONVERGENCE
!= FOREIGN EFFECT EXECUTION
~~~

A bounded correction returns through the rightful family's request/Control boundary.

## From meaning to source shape

Do not begin by asking which interface a primitive should implement.

Use this promotion ladder:

~~~text
MEANING
    the primitive distinction is useful for understanding

        |
        | addressability / continuity pressure
        v

STABLE IDENTITY / VALUE
    the distinction needs a durable ref/value

        |
        | compiler-visible role or invalid-state pressure
        v

TYPE
    a concrete type materially prevents confusion

        |
        | repeated executable invariant across real consumers
        v

INTERFACE / FOUNDATION
    shared behavior is worth enforcing in source
~~~

If removing a proposed type/interface loses no required correctness, continuity, authority,
addressability or proof, keep the primitive as documentation/feature meaning.

### Why some current interfaces are earned

`VexBehaviorFamily.State / Control / Act / Effect` earn compiler-visible roles because family/role
mixups are real invalid combinations.

`VexPort` earns a shared contract because several consumers need the same two-way semantic membrane:

~~~text
currentProjection
activate(Control)
~~~

`VexExecution` earns a foundation because recurring coroutine custody, identity, containment and
stop/join behavior must be enforced consistently.

By contrast, current source has not earned generic:

~~~text
VexTerrain
VexTraversal
VexIntention
VexObservation
~~~

interfaces merely because those primitive meanings are useful to reason with.

~~~text
REUSABLE MEANING
!= REUSABLE TYPE EQUATION
~~~

## Live example — Playback and Queue

Current entry points:

~~~text
app/.../vexstream/playback/VexStreamPlaybackDocs.kt
app/.../vexstream/queue/VexStreamQueueBehavior.kt
architecture/behavior/README.md
~~~

### Playback

Purpose:

> Let the human govern progression through the current media experience while keeping semantic
> meaning separate from Media3/provider mechanics.

Primitive map:

~~~text
DOMAIN
    Playback

TERRAIN / TOPOLOGY
    current media timeline

STATE / POSITION
    NoMedia / Media / Ended
    provider-derived current playback truth

ENGAGEMENT
    PlayPause Control

INTERPRETATION
    Play or Pause Act from current State

TRANSFORMATION
    RequestPlay / RequestPause / RestartCurrent Effect

MANIFESTATION
    Media3VexStreamPlaybackConnection

OBSERVATION
    Media3 / Player truth returns through the Connection

PROJECTION
    VexStreamPlaybackProjection
    optional Playback DisplayState for view treatment

PROOF
    focused Behavior + Connection tests
~~~

The useful semantic sentence is not "Playback has several classes."

It is:

> **Playback owns traversal dynamics through the current media timeline.**

### Queue

Queue answers a different question:

~~~text
DOMAIN
    Queue

TOPOLOGY
    ordered media context

STATE / POSITION
    Empty / NonEmpty + current/available neighbors

TRAVERSAL
    Previous / Next / wrap-like movement through ordering

CONSTRAINT / POLICY
    availability, shuffle, repeat/clear meaning in their rightful families

TRANSFORMATION
    queue Effects such as SeekPrevious / SeekNext / ShuffleFuture / ClearQueue
~~~

The deeper relationship is:

~~~text
Queue
    traversal ordering

Playback
    traversal dynamics through the current item
~~~

This first-principle distinction explains why nearby media controls do not belong in one giant
Playback family.

## Live example — VexMaps Distribution

Current entry point:

~~~text
app/.../screens/shared/maps/distribution/README.md
~~~

Primitive map:

~~~text
DOMAIN
    Distribution / Delivery Route

TERRAIN / TOPOLOGY
    delivery stops + route/navigation topology

INTENTION / TARGET
    carry out the human-entered delivery obligations

STATE
    Distribution Mode
    Delivery Route
    Navigation

TRAVERSAL
    recommended Plan / current Delivery Route leg

RELATIONSHIP / CONVERGENCE
    Distribution Mode + Delivery Route
    Delivery Route + Navigation

CONSTRAINT
    route identity, current leg/attempt truth, provider/currentness conditions

MANIFESTATION
    Navigation/provider realization

PROJECTION
    Distribution UI / Context Dock

PROOF
    focused Behavior/Convergence proof
    complete human Delivery Route walk remains explicitly distinct proof
~~~

The source-owned laws:

~~~text
DRAFT != PLAN
PLAN != DELIVERY_ROUTE_STATE
DELIVERY_ROUTE_STATE != NAVIGATION_STATE
CONVERGENCE != FOREIGN_EFFECT_EXECUTION
EXECUTION_CUSTODY != PRODUCT_MEANING
~~~

are examples of semantic primitives preventing nearby but non-equivalent meanings from collapsing.

## Live example — Conversation

Current entry point:

~~~text
app/.../conversation/README.md
~~~

Conversation does not need to be forced into a new SCAE package before its first-principle structure
is understandable.

Primitive map:

~~~text
DOMAIN
    Conversation

IDENTITY
    participant / speaker / recipient / channel / thread / message

TERRAIN / TOPOLOGY
    addressing + thread/channel relationships

INTENTION
    carry meaning between participants with explicit authorship/addressing

STATE
    current canonical Conversation/thread projection owned outside Android where applicable

TRAVERSAL
    message meaning crosses the addressing topology
    information may be projected/duplicated without leaving its origin

MANIFESTATION
    repository / server adapter / Connected Services client at their rightful boundaries

OBSERVATION
    repository/server history projected into Android Conversation State

RELATIONSHIP / CONVERGENCE
    Hybrid Relay resolves mixed-provenance current truth without becoming another Conversation owner

PROJECTION
    VexConversationSurfaceProjection

CONTINUITY
    durable message sequence/head/history meaning

PROOF
    state/repository/adapter/Hybrid Relay focused tests
~~~

This lets a fresh reader recognize Conversation as **meaning traversal through relationship topology**
without pretending its current source must use the same class names as Playback.

## Live example — Events

Current entry point:

~~~text
docs/foundation/EVENTS-CONVERGENCE-PRACTICUM.md
~~~

The practicum demonstrates:

~~~text
external provider/source truth
        |
        v
normalized EventsRepository truth
        +
attention preference
        +
current Screen / visible targets / transient occupancy
        |
        v
Convergence
        |
        v
candidate identity + availability
        |
        v
anchored human-attention Projection
~~~

First-principle reading:

~~~text
OBSERVATION
    bounded external source snapshot

STATE
    feed + preferences + screen/presentation context

RELATIONSHIP
    which current event, if any, is eligible human attention now?

CONSTRAINT
    other Screen / target unreachable / transient occupied / handled preference

PROJECTION
    candidate + availability + anchored overlay

PROOF
    focused Relay proof + canonical Journey / ScreenCrawler path
~~~

The Relay's significance is not "it combines Flows." It exists because independent truths gain one
new relationship meaning when considered together.

## Live example — Elements / Vex Compose

Current entry point:

~~~text
architecture/elements/README.md
~~~

Elements demonstrates that presentation also has first-principle meaning:

~~~text
IDENTITY
    registered Screen/Element identity

TERRAIN / TOPOLOGY
    nested semantic parent/child placement

PROJECTION
    current product meaning realized as Compose presentation

MANIFESTATION
    Compose / Modifier / Android rendering

PROOF
    stable semantic IDs, preview/gallery/product proof
~~~

`VexModifier` and `VexComposeScope` do not become semantic truth owners. Their purpose is to preserve
identity and presentation continuity across concrete Compose form.

## Live example — VexExecution

Current entry point:

~~~text
architecture/execution/README.md
~~~

`VexExecution` is a useful counterexample to "everything is SCAE."

It answers:

~~~text
MANIFESTATION / EXECUTION CUSTODY
    which coroutine-owned operation is running?

TOPOLOGY
    one managed Job per independent sustained execution

CONTINUITY
    stable execution identity and terminal state during the runtime lifetime

CONSTRAINT
    duplicate admission, cancellation, shutdown, bounded source recovery
~~~

It intentionally does **not** decide product meaning, provider retry meaning, State truth or lawful
SCAE Effects.

The source form is a shared abstract class because repeated coroutine-custody invariants earned
compiler/runtime enforcement.

## Domain meaning may be reduced without being erased

A domain name remains valuable even when a deeper primitive explains it.

Examples:

~~~text
Playback
    traversal dynamics through media timeline

Queue
    traversal ordering

Maps
    terrain + coordinated traversal

Conversation
    meaning traversal through addressing topology

Memory
    continuity of selected meaning through time

Process
    traversal through dependency topology toward an outcome
~~~

The reduction is useful when it reveals reusable relationships.

It becomes harmful if it erases domain-specific truth.

~~~text
EVERYTHING CAN BE DESCRIBED AS TRAVERSAL
!=
EVERYTHING SHOULD SHARE ONE TRAVERSAL INTERFACE
~~~

## Semantic provenance — purpose bridged to form

For architecture/source explanations, preserve this chain:

~~~text
PURPOSE
    Why does the human/product need this?

DOMAIN
    What world of meaning is involved?

PRIMITIVE
    What kind of first-principle role is this?

RELATIONSHIP
    What does it connect, transform, constrain, observe or preserve?

FORM
    What current architecture/source shape carries that relationship?

WHY THIS FORM
    What concrete pressure made this representation useful?

BOUNDARY
    What neighboring meaning does it explicitly not own?

PROOF
    What demonstrates that the meaning survived the form?
~~~

A useful KDoc/comment deepens this chain at the narrowest point the source cannot express by naming
alone.

Weak:

~~~text
"Combine route state and navigation state."
~~~

Stronger:

~~~text
"Delivery Route owns the work/leg truth. Navigation owns provider guidance.

This Convergence exists because neither truth alone can answer whether Navigation currently realizes
the exact Delivery Route leg. Its output is relationship truth only; any requested correction returns
through the rightful family's Control boundary."
~~~

The stronger explanation remains useful even if the underlying reducer, Flow plumbing or constructor
shape changes.

## Fresh-reader algorithm

When entering unfamiliar source:

~~~text
1. Read the accepted feature-purpose/home source.
2. Name the domain in ordinary language.
3. Ask which semantic primitives are materially present.
4. Identify the relationships among those primitives.
5. Follow the existing architectural owner for each relationship.
6. Separate semantic form from technology/provider substrate.
7. Preserve boundaries and explicit absences/holds.
8. Find the proof that owns the claim.
9. Only then judge whether the current source shape is clear, overloaded, or missing a rightful owner.
~~~

Do not infer missing product purpose from a clean primitive mapping.

A perfect map of implementation meaning is still not permission to invent a human outcome the
accepted feature source does not claim.

## Authoring algorithm

The current Feature Experience Language can now be read more deeply as:

~~~text
HUMAN SITUATION
-> PURPOSE / OUTCOME
-> DOMAIN
-> SEMANTIC PRIMITIVES
-> RELATIONSHIPS
-> ESTABLISHED ARCHITECTURAL PATTERN
-> NAME / PLACEMENT
-> MINIMUM SOURCE FORM
-> PROOF
~~~

The feature experience questions map naturally:

~~~text
WHAT
    domain / identity

WHERE
    terrain / topology / source-destination relationship

NOW / WHEN
    current State / constraints

WHY / OUTCOME
    intention / target

WHAT CAN I DO?
    engagement / Control

WHAT DOES THAT MEAN NOW?
    interpretation / Act

HOW DOES IT CHANGE / TRAVEL?
    traversal / transition

WHAT SHOULD RUNTIME ATTEMPT?
    transformation / Effect

WHAT MAY BLOCK OR QUALIFY IT?
    constraint / authority

WHO / WHAT REALIZES IT?
    manifestation / Connection / provider / repository / service

WHAT DID REALITY ANSWER?
    observation / resulting State

WHAT DO THEY SEE?
    Projection / presentation

WHAT MUST SURVIVE?
    continuity

HOW DO WE KNOW?
    proof
~~~

Do not manufacture a source object for every row. The map exists so source form follows meaning,
rather than meaning being reconstructed from source form afterward.

## Repository / portable-meaning boundary

This document is an Android-local authoring/orientation foundation backed by current Android examples.

It does not make Android the universal VexLife semantic owner.

When a portable VexLife contract exists:

~~~text
portable product meaning
    -> consume explicitly

Android semantic mapping
    -> preserve that meaning in native form

Android-only lifecycle/provider/rendering/proof
    -> remain Android-owned
~~~

A future cross-repository semantic blueprint may reuse the learned primitives. That promotion should
preserve product-specific ownership rather than copying Android implementation vocabulary outward.

## Checkpoint — Functional State Hoisting

> **Status: exploratory checkpoint, not accepted runtime law.**
>
> This section preserves a live architecture investigation so a later human or AI can continue the
> reasoning without reconstructing it from chat or flattening it into conventional wrapper/service
> patterns. It intentionally records alternatives and unresolved edges. Do not read an example below
> as proof that its exact Kotlin syntax has been selected.

### Why this pressure exists

The current Family shape already preserves semantic depth:

~~~text
Family
├─ State
├─ Control
├─ Effect
└─ Act
   └─ Begin
      ├─ Effect
      └─ Approach
~~~

An accepted Act may also carry the maintained RelationshipId authored from the live Family + Control
relationship.

The current ThemeMode realization eventually leaves that semantic tree and becomes ordinary Kotlin
mechanics:

~~~text
Effect.Save(mode)
-> choose store::save
-> pass mode as an argument
-> ask VexExecution to invoke that operation
~~~

That works, but it creates a recurring readability pressure: the source has already expressed
meaning through Family / Act / Effect, then reconstructs the executable call at a flatter syntax
level where a fresh author can again invent arbitrary functions, wrappers, coordinators, execution
mirrors or duplicated arguments.

The investigation is therefore not "how can every function become a framework object?"

It is:

> When a function is specifically the executable continuation of one Family Act, can the Family
> preserve that relationship all the way down to the callable Method and its semantic Parameters,
> instead of losing the relationship at the final Kotlin function call?

### Fractal continuation under Act

The candidate semantic depth is:

~~~text
Family
├─ State
├─ Control
├─ Effect
└─ Act
   ├─ Begin / Approach
   ├─ Parameter
   └─ Method
~~~

The important naming distinction is:

~~~text
Parameter describes WHAT THE INPUT IS.
Method describes WHAT THE OPERATION DOES.
~~~

For ThemeMode:

~~~text
ThemeModeFamily.Act.Parameter.Mode
ThemeModeFamily.Act.Method.Save
~~~

not:

~~~text
ThemeModeFamily.Act.Parameter.Save
ThemeModeFamily.Act.Method.Save
~~~

The latter repeats the Method name at the Parameter depth and loses the dimensional distinction.

A provisional shape is therefore closer to:

~~~kotlin
sealed interface Parameter :
    VexFamily.Act.Parameter<ThemeModeFamily> {

    data class Mode(
        val mode: ThemeMode,
    ) : Parameter
}

sealed class Method :
    VexFamily.Act.Method<
        ThemeModeFamily,
        Parameter,
        Output
    > {
    // concrete Methods fill this Family-owned vocabulary
}
~~~

The exact generic coordinates and Output form are not settled. The invariant being explored is the
relationship path:

~~~text
ThemeModeFamily
-> Act
-> Parameter
-> Mode
-> mode: ThemeMode
~~~

A primitive field is no longer understood only as "the argument to whatever function happens to be
nearby." Its owning semantic path remains recoverable from the type.

### Parameter is semantic input state, not Method arguments renamed

A Method with several inputs should not necessarily become:

~~~kotlin
Method.Route(
    origin,
    destination,
    travelMode,
    region,
)
~~~

The state-hoisting candidate is:

~~~kotlin
Parameter.Route(
    origin = origin,
    destination = destination,
    travelMode = travelMode,
    region = region,
)
~~~

and the Method consumes that one semantic input state.

This makes the number of fields inside a Parameter irrelevant to the Method grammar. One field and
five fields are still one semantic Parameter value.

It also means the Parameter can remain intact while moving through the Act relationship instead of
being decomposed into primitives and later reconstructed from positional arguments.

Possible rule under investigation:

> A Family Act Method consumes one Family-bound Parameter subtype rather than accepting loose
> business/domain arguments directly.

That rule is **not yet accepted**. In particular, a real zero-input Method must pressure whether the
correct expression is no Parameter, Kotlin Unit, an explicit semantic absence, or another smaller
shape. Do not manufacture `Parameter.None` merely for symmetry without a real consumer.

### Do not erase the actual Method behind another binding stage

A previous exploration incorrectly introduced shapes such as:

~~~text
method(...)
-> through(store)
-> customer(...)
-> launch()
~~~

That is specifically **not** the simplification being investigated here.

If `Act.Method.Save` is the actual executable Method, then the ThemeMode Store may belong inside the
Method because the Store is part of how that Method is physically realized:

~~~kotlin
class Save(
    private val store: ThemeModeStore,
) : Method() {

    override suspend operator fun invoke(
        parameters: Parameter.Mode,
    ): ThemeModeStore.Result =
        store.save(
            parameters.mode,
        )
}
~~~

There is no additional `.through(store)` layer in this shape.

The Method owns the executable call. VexExecution still owns Job custody / overlap / cancellation.
The Method does not become a second semantic owner merely because it contains the concrete dependency
needed to perform its call.

This distinction is central:

~~~text
Act.Method
    semantically-addressed executable function

VexExecution
    occurrence / lifetime / overlap custody

not:

Act.Method
    descriptor that another runtime/runner/binder must interpret
~~~

If a proposed design needs `MethodSpec -> MethodBinding -> Runtime -> Executor` just to invoke the
function, re-check whether the design has flattened the simple relationship and rebuilt it as
traditional ceremony.

### Where should the Parameter value live?

This checkpoint intentionally preserves three variants because they expose different ownership
properties.

#### Variant A — Method owns its bound Parameter

~~~kotlin
Method.Save(
    store = store,
    parameters =
        Parameter.Mode(
            mode = ThemeMode.Dark,
        ),
)
~~~

Meaning:

~~~text
Method value
= fully bound executable operation
= concrete dependency + semantic input state
~~~

Potential strengths:

~~~text
self-contained callable value
invoke() needs no later argument
easy to hand around as one complete Method occurrence
~~~

Pressure:

If the accepted Act already owns the same Parameter, binding it again to the Method creates duplicate
state and a mismatch seam.

#### Variant B — Act hoists Parameter; Method consumes it

~~~text
accepted Act
├─ Effect
├─ Parameter.Mode(Dark)
├─ Approach
└─ RelationshipId

Method.Save(store)
    accepts Parameter.Mode
~~~

Possible callsite:

~~~kotlin
act
    .launchWith()
    .method(
        ThemeModeFamily.Act.Method.Save(
            store,
        ),
    )
~~~

The execution boundary supplies the Act's already-authored Parameter to the Method:

~~~text
Act.Parameter.Mode(Dark)
+
Method.Save(store)
-> Save.invoke(Parameter.Mode(Dark))
~~~

This is the strongest current **functional state-hoisting** candidate because the semantic input
exists once. The caller cannot accidentally reconstruct `Mode(Light)` while launching an Act that
was accepted for `Mode(Dark)`.

Potential invariant:

~~~text
Act owns the invocation's semantic input state.
Method owns the concrete executable function.
VexExecution joins them without inventing either.
~~~

This is not yet proven against enough Families to call permanent architecture.

#### Variant C — caller binds Parameter again during Method construction

~~~kotlin
act
    .launchWith()
    .method(
        Method.Save(
            store,
            Parameter.Mode(Dark),
        ),
    )
~~~

This is explicit and may be useful if Act does not own Parameter.

But if Act already owns Parameter, the caller can theoretically provide a different value from the
one that caused the Act. That would reintroduce duplicated authorship.

Therefore Variant C should not be chosen merely because constructor syntax is familiar.

### `launchWith` should consume authored Approach, not ask the caller to choose again

A tempting syntax is:

~~~kotlin
act
    .launchWith(
        Approach.Latest,
    )
    .method(...)
~~~

Current Family already authors Approach through:

~~~text
begin
beginEarliest
beginLatest
beginQueued
~~~

and accepted Act already carries the resulting Begin / Approach plus RelationshipId where required.

So a reduction-first form is more likely:

~~~kotlin
act
    .launchWith()
    .method(...)
~~~

where `launchWith()` exposes the execution continuation already authored by the Act.

If a temporary return type such as `MethodFollowUp` is needed so Kotlin exposes only `.method(...)`
next, that type is compile-time sequencing machinery, not a new semantic owner and not permission to
invent a new runtime vocabulary.

Before adding a new LaunchType / Runner / Executor category, test whether current `VexExecution`
already owns that exact responsibility. Reuse or reshape the existing execution boundary rather than
polluting the architecture with a synonym.

### Effect, Parameter and Method are different coordinates

The investigation must not collapse these simply because the names are nearby.

Candidate reading:

~~~text
Effect
    WHAT transformation has the Act requested?

Parameter
    WITH WHAT semantic input?

Method
    WHAT actual executable operation realizes it?

Method Output
    WHAT did that physical operation return?
~~~

ThemeMode pressure suggests one potentially clean decomposition:

~~~text
Effect.Save
Parameter.Mode(Dark)
Method.Save(store)
Method Output = ThemeModeStore.Result
~~~

This is different from today's:

~~~text
Effect.Save(Dark)
~~~

because the Parameter may become the rightful home for `Dark` as executable input state.

That possibility is attractive because duplicating the same `mode` in both:

~~~text
Effect.Save(mode)
Parameter.Mode(mode)
~~~

would create two representations of one semantic input.

However this is still an investigation, not permission to strip payload from every Effect. Some
Effects may genuinely own data as part of their transformation meaning. Pressure each real Family
before generalizing.

A more aggressive reduction would eliminate separate Effect data and let Method + Parameter express
everything:

~~~text
Act
├─ Method.Save
│  └─ Parameter.Mode(Dark)
└─ Approach.Latest
~~~

But a bound Method containing `ThemeModeStore` cannot be created during pure `Family.act(...)`
without making Family know provider/runtime construction. Splitting that into MethodSpec + bound
Method would likely recreate the ceremony this investigation is trying to remove.

That is why the current pressure still favors keeping pure Act-authored semantic consequence separate
from the later bound executable Method.

### Method Output should not be confused with Effect or resulting State

If `Method.Save` actually calls:

~~~kotlin
store.save(
    parameters.mode,
)
~~~

then its truthful return type is whatever the Store operation actually returns, for example:

~~~text
ThemeModeStore.Result
~~~

not automatically:

~~~text
Effect.Save
Family.State
~~~

Permanent distinction under this investigation:

~~~text
Effect
    requested transformation

Method
    executable realization

Method Output
    physical/provider operation result

observed/reconciled Family State
    semantic truth after the rightful interpreter knows what happened
~~~

Do not use Functional State Hoisting to silently restore a universal:

~~~text
method result -> Family State
~~~

grammar.

Outside truth still returns through the owner that can truthfully interpret it.

### Parameter root should preserve meaning, not erase it into `Any?`

A possible sketch used a common `any: Any?` field to express that every Parameter contains a value.

That would weaken the desired relationship:

~~~text
Parameter -> Any?
~~~

instead of preserving:

~~~text
Parameter
-> Mode
-> mode: ThemeMode
~~~

Composite Parameters make the problem even clearer:

~~~kotlin
data class Route(
    val origin: VexMapCoordinate,
    val destination: VexMapCoordinate,
    val travelMode: VexMapsTravelMode,
)
~~~

There is no truthful single generic `Any?` value that improves this relationship.

Current reduction preference:

~~~text
VexFamily.Act.Parameter<Family>
    family-bound semantic role marker

concrete Parameter subtype
    exact named semantic fields
~~~

Add a generic Value coordinate only if a demonstrated executable invariant requires it.

### Do not recursively wrap every State field just because Parameter exists

Functional State Hoisting does **not** currently imply:

~~~text
Family.State
-> State.Parameter
-> wrapper for every State field
~~~

State and Act.Parameter answer different questions.

~~~text
Family.State
    what is semantically true now?

Family.Act.Parameter
    what semantic input is carried into this Act's executable Method?
~~~

A Parameter may contain a State value or a value derived from State when that is genuinely the
Method's input, but that does not make Parameters the universal representation of State internals.

The fractal is useful when each depth preserves a distinct relationship. Recursing purely for visual
symmetry is the same architecture pollution in a different costume.

### Convergence / CompoundState pressure

This checkpoint is intentionally **not** isolated to ThemeMode.

Current convergence law remains:

~~~text
complete truth A
complete truth B
complete truth C
        |
        v
explicit relationship
        |
        +-> relationship truth
        |
        +-> optional bounded request toward the rightful owner
~~~

A Compound Relay combines independently-owned current truths. It does not become the owner of each
Family's executable inputs.

Therefore a first distinction to preserve is:

~~~text
Family.State
    current owner truth

Family.Act.Parameter
    semantic input to one Family Act Method

Compound / convergence relationship truth
    meaning that exists only across independently-owned truths
~~~

Do not feed foreign-family Parameters into CompoundState merely because Parameters are now more
strongly typed. A Parameter is not automatically current truth.

However Family-bound Parameters may make convergence **more precise** at the boundary where a
relationship truth produces a bounded request toward one rightful owner.

For example, a convergence output may determine:

~~~text
relationship truth
-> bounded request to Family A
-> Family A Control
-> Family A Act
-> Family A Act.Parameter
-> Family A Act.Method
~~~

The Parameter then retains provenance:

~~~text
this executable input belongs to Family A's Act vocabulary
~~~

instead of becoming an unscoped primitive extracted from a compound relationship.

That may strengthen Compound/Relay architecture because the relationship can return toward a Family
without becoming the executor of that Family's Method.

Open questions to pressure with real convergence consumers:

1. Should CompoundState contain only complete State values / relationship truth, with Parameters
   appearing only after a bounded request re-enters a rightful Family?
2. Can a relationship owner itself have an Act.Parameter when the **relationship** genuinely owns an
   executable Method, rather than borrowing Parameters from participating Families?
3. If a Compound relationship needs several inputs to one relationship-owned Method, is one
   relationship-scoped composite Parameter the correct state-hoisted input?
4. Does any current Relay actually benefit from exposing a Parameter, or would doing so merely turn
   internal reducer inputs into public architecture?
5. Can the provenance path remain explicit enough that a future author can tell:
   `FamilyA.State`, `FamilyB.State`, `Relationship.State`, and `FamilyA.Act.Parameter` apart
   without comments reconstructing ownership?

Do not answer these by manufacturing a universal CompoundParameter. Follow real relationship
pressure.

### What "fractal" means in this checkpoint

Fractal does not mean every level has identical nouns.

It means the same ownership discipline remains recognizable as depth increases:

~~~text
ThemeModeFamily
    owns one semantic world

ThemeModeFamily.Act
    owns one lawful consequence in that world

ThemeModeFamily.Act.Parameter.Mode
    owns the semantic input shape for a Method

ThemeModeFamily.Act.Method.Save
    owns the executable Save call

Mode.mode
    remains a typed ThemeMode value inside that semantic path
~~~

Depth preserves provenance instead of forcing future readers to infer it from:

~~~text
function name
argument position
file location
constructor convention
historical chat
~~~

This is the quality target:

> The source should let a human or AI talk its way through the relationship in language that scales,
> then arrive at code whose type path preserves that same language. Renaming or rearranging syntax
> must not be able to silently erase the relationship.

### Current strongest candidate to pressure, not a settled API

The smallest currently interesting composition is:

~~~text
Control.Select(Dark)
        |
        v
Family.act(CURRENT State, Control)
        |
        v
Act
├─ admitted State?
└─ Begin
   ├─ Effect.Save
   ├─ Parameter.Mode(Dark)
   └─ Approach.Latest
        |
        v
accepted RelationshipId
        |
        v
act.launchWith()
        |
        v
Method.Save(store)
        |
        v
VexExecution invokes
Method.Save(
    Parameter.Mode(Dark)
)
        |
        v
ThemeModeStore.Result
        |
        v
rightful result / observation interpretation
        |
        v
Family State truth when actually known
~~~

Possible surface syntax:

~~~kotlin
family
    .activate(
        Control.Select(
            ThemeMode.Dark,
        ),
    )
    ?.launchWith()
    ?.method(
        ThemeModeFamily.Act.Method.Save(
            store,
        ),
    )
~~~

The point of this example is not the exact method names.

The important relationships are:

~~~text
Parameter describes input meaning, not Method name.
Parameter may be hoisted once on the Act.
Method contains the actual concrete callable dependency.
Approach is authored once by the Act.
RelationshipId is authored once by the Family/Control relationship.
VexExecution supplies mechanics rather than another feature API.
Method Output remains distinct from Effect and semantic State.
Convergence remains relationship truth, not foreign Method execution.
~~~

Before source implementation, pressure this candidate against at least ThemeMode, one multi-field
Method, one zero-input Method, one source-addressed execution case, and one Compound Relay /
convergence case. The purpose of that pressure is subtraction: discover which coordinates are
actually primal and remove anything that exists only because familiar 2D coding style expected it.

<!-- [VXG RealForever][VEX-BODY][FUNCTIONAL-STATE-HOISTING][CHECKPOINT][PROVISIONAL] -->

## Deliberately held

Current source does **not** establish:

~~~text
a VexBody runtime singleton
a universal semantic-primitive registry
a generic VexTerrain interface
a generic VexTraversal interface
a generic VexObservation interface
a mandatory Body package for every feature
automatic SCAE conversion of every domain
automatic refactoring from primitive similarity
a claim that these primitives are the final possible ontology
~~~

Let real feature pressure refine the map.

The enduring requirement is smaller:

> A fresh human or AI should be able to recover **purpose -> primitive meaning -> relationship ->
> form -> proof** without needing predecessor chat and without mistaking an implementation mechanism
> for the meaning it carries.

<!-- [VXG RealForever][VEX-BODY][SEMANTIC-BLUEPRINT][PURPOSE-TO-FORM] -->
