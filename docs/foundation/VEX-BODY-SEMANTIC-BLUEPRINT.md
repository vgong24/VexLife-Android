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
