# Relational bridge — VexLogger, dependency construction, Family, and UI

[VXG RealForever]

> This document connects the SKETCH source in `SOURCE-SKETCHES.md` to the settled architecture in
> PR #207 without pretending the dependency system or final Logger event schema is already designed.

## 1. What is already settled from #207

The sibling snapshot establishes:

~~~text
VexLogger
    owns chronological traceability

VexExecution
    owns current active custody:
    RelationshipId -> actual Job(s)

VexFamily
    owns semantic State / Control / Act / Effect meaning
    and stays free of Logger / Store / coroutine mechanics

production Logger fact
    accepted or observed reality

debug expectation
    separate from production fact
~~~

This context must not weaken those boundaries.

## 2. What the dependency sketches add

The dependency sketches introduce a separate problem:

> How do live dependencies become available through readable relationship mappings without hiding
> the construction graph inside conventional DI magic?

The useful shape is relational rather than locator-oriented:

~~~text
Module declaration
    |
    v
dependency prerequisites
    |
    v
build/memoize exact live instance
    |
    v
lifetime placement
    |
    v
explicit consumer dependency
~~~

This may eventually become `VexDependencyFactory` / `VexInjectible` / Modules, but those names are
still SKETCH vocabulary.

### Bridge recommendation — construction mechanics belong to the constructor side

The sketch currently places graph work on `VexInjectible.build()`.

The stronger relational direction is:

~~~text
dependency declaration
    says what is required / how this node is formed

VexDependencyFactory (or equivalent construction owner)
    owns graph traversal
    owns prerequisite ordering
    owns memoization
    owns cycle/error handling
    owns lifetime-aware reuse
~~~

Do not make every constructed object recursively understand the dependency graph just because it can
participate in that graph.

A Family may be *constructed by* the dependency system without knowing that a dependency system
exists.

## 3. Family participation must not make Family a DI object

The sketch says:

~~~text
FamilyA implements "Injectable"
~~~

Treat that as SKETCH pressure, not the preferred bridge.

The refined direction to pressure-test is:

~~~text
FamilyModule / provider declaration
    knows how to construct FamilyA
    and what FamilyA's surrounding Connection/runtime requires

FamilyA
    knows semantic State / Control / Act / Effect only
~~~

This keeps the same readable construction relationship without adding:

~~~text
getDependencyId()
build()
container awareness
scope awareness
graph traversal
~~~

to the semantic Family abstraction itself.

If a future implementation earns a marker/interface for tooling, it must prove why the marker adds
correctness that cannot remain in the module/provider declaration. It must not smuggle graph mechanics
back into Family.

## 4. Dependency identity is readable, but class name is not automatically identity

The sketch says:

~~~text
getDependencyId() = this::className
~~~

Human-readable class/type names are useful **labels**.

Do not automatically make class name the complete dependency key.

The actual construction relationship may eventually need to distinguish:

~~~text
same implementation class
different lifetime

same implementation class
different configured provider

same interface
different rightful implementation

same dependency kind
different owning graph/module
~~~

Do not invent qualifiers in advance. Preserve only this rule:

> dependency identity must be derivable from the readable relationship declaration and contain only
> the distinctions actually required for correct construction/reuse.

Class/type name may be sufficient where it genuinely is sufficient.

## 5. Do not collapse dependency identity into execution identity

Two maps may legitimately exist for different truths:

~~~text
dependency construction
    dependency key -> live dependency instance

execution custody
    RelationshipId -> actual Job(s)
~~~

They must not be merged merely because both are maps.

The first answers:

~~~text
Which dependency instance should this consumer receive/reuse?
~~~

The second answers:

~~~text
Which semantic active work relationship currently owns Job(s)?
~~~

The sketch's `InstanceId` must therefore be renamed/refined if necessary to avoid implying execution
occurrence identity.

## 6. UI consumption should remain explicit

The UI sketch's strongest property is that dependency consumption is visible:

~~~text
VexScreen
-> VexRow(dependencyFamilyA)
-> VexUIComponentA(dependencyFamilyA, familyStateA)
-> VexToggle(...)
~~~

Prefer explicit parameter relationships over hidden calls such as:

~~~text
container.get<FamilyA>()
serviceLocator.resolve(...)
ambient global dependency lookup
~~~

The dependency system may construct/route the live object, but the consumer relationship should remain
readable at the consumption boundary.

## 7. UI element identity is context, not automatically Control identity

The Logger sketch currently says:

~~~text
UI ButtonClicked
-> Family.Control(getIdFromComposable)
~~~

That is an important relationship to discuss, but it should not silently collapse:

~~~text
elementId
==
Control identity
==
RelationshipId
~~~

A cleaner relational reading to pressure-test is:

~~~text
elementId
    identifies the UI source fact

Control
    identifies the semantic request

RelationshipId
    identifies the active execution relationship when runtime work exists

VexLogger chronology
    connects accepted/observed facts over time
~~~

Example:

~~~text
element.buttonA Clicked
FamilyA.Control.Toggle accepted
FamilyA State Active admitted
presentation toggle shown active
~~~

Those facts may be adjacent and causally connected without requiring the UI element ID itself to
become the Family relationship identity.

This is a BRIDGE INTERPRETATION, not a final event schema.

## 8. Logger observes the semantic/runtime path without living inside Family

The sketch contains:

~~~text
fun activate(composableId, parameters) {
    log(composableId, Control)
}
~~~

Preserve the causal relationship, not that physical placement.

The stronger bridge is:

~~~text
UI boundary
    knows elementId and user action

-> creates / sends semantic Control

VexFamily.activate(Control)
    remains semantic
    returns Act
    may admit State
    may request Effect

Connection/runtime observing boundary
    sees accepted Act / Effect / provider result / presentation result

-> reports accepted/observed facts

VexLogger
    records chronology
~~~

Do not add Logger or element IDs to pure `VexFamily.act(...)` merely to make tracing convenient.

## 9. The "admit -> completionState" box remains TO DISCUSS

The sketch says:

~~~text
admit
-> state phase
-> log(relationshipId, completionState)
~~~

There are multiple possible facts hiding in that one box:

~~~text
semantic State was admitted
runtime Effect was requested
runtime work began
runtime work succeeded/failed/cancelled
provider reality was observed
presentation actually arrived
~~~

Those facts may have different rightful owners and times.

Therefore do not invent one universal `completionState` just to make tracing convenient.

Use the Logger law:

~~~text
rightful owner accepts/observes fact
-> observing boundary reports fact
-> VexLogger records chronology
~~~

and preserve each fact at the boundary where it becomes true.

## 10. VexMetaData must not become a dumping ground

The sketch contains:

~~~text
VexMetaData
    Common Placements
~~~

That is not yet enough to establish a metadata architecture.

Before adding a shared metadata object, ask whether the information already exists naturally in:

~~~text
object placement
constructor/module relationship
UI element identity
Control type
RelationshipId
lifetime owner
VexLogger chronology
~~~

Do not create a metadata bag merely to restate those relationships.

If repeated cross-cutting metadata is eventually demonstrated, give each field a rightful consumer and
owner rather than making `VexMetaData` an unbounded escape hatch.

## 11. Dependency construction should be inspectable

The current human goal favors a relationship graph that can be read without executing opaque
framework/compiler resolution.

A future implementation should make it possible to inspect something conceptually like:

~~~text
AppModule
    LogHistoryRepository
    ThemeModeStore
    DeliveryService?

FamilyModule
    ThemeModeFamily
        depends on: [what its Connection/runtime actually needs]

    FamilyA
        depends on: [...]

VexAppScope
    owns: [long-running/service dependencies]

VexActivity
    owns/receives: [...]

VexScreen("screenA")
    receives FamilyA

VexRow
    receives FamilyA explicitly
~~~

This is not a demand for a giant registry DSL. The readable map should be the smallest truthful
representation that makes construction and ownership inspectable.

## 12. Build ordering / memoization pressure

The sketch proposes:

~~~text
dependency modules first
-> recursively build prerequisites
-> memoize created instances
-> build Families after prerequisites are ready
~~~

The useful requirement appears to be:

~~~text
a dependency is constructed once per rightful lifetime/identity
and downstream consumers receive the already-built instance
~~~

Still TO DISCUSS:

~~~text
exact dependency key type
whether class identity is sufficient
cycle detection behavior
module composition syntax
scope/lifetime declaration syntax
error reporting
optional dependency semantics
server-side delivery construction
~~~

Do not harden these merely to imitate another DI framework.

## 13. Logger relationship to dependency construction

VexLogger may itself be a dependency supplied through this relationship system.

That does not make the dependency graph a logging owner.

Likewise, the dependency factory may produce/route an optional delivery service for Logger, but:

~~~text
Logger fact formation
!= delivery

delivery failure
!= fact failure

dependency construction
!= causal history
~~~

Potential direction:

~~~text
VexLogger
-> local chronology
-> downstream projection/delivery adapter(s)
~~~

The sketch's:

~~~text
serverSideDeliveryOption: IDeliveryService? = null
~~~

therefore belongs under downstream delivery discussion, not inside the definition of what a Logger
fact means.

Preferred direction:

~~~text
VexLogger
    forms / accepts chronological fact

downstream projection(s)
    local LogHistory
    Logcat
    optional server delivery
~~~

A dependency graph may construct and connect those projections, but delivery availability/failure
cannot redefine whether the Logger fact exists.

## 14. Two-sided connectability map

The architecture has two different directions that meet at explicit live objects.

### Side A — construction / dependency connectability

~~~text
Modules / provider declarations
-> dependency prerequisites
-> construction owner traverses graph
-> memoized instance per rightful identity/lifetime
-> explicit lifetime placement
-> explicit live object delivered to consumer
~~~

This side answers:

~~~text
Where did this object come from?
What did it require?
Which instance is reused?
Who owns its lifetime?
Who consumes it?
~~~

### Side B — semantic / observation connectability

~~~text
explicit live Family / Connection / runtime object
-> UI action / semantic Control
-> Family Act
-> State admission and/or Effect request
-> Connection/runtime realization
-> provider/presentation observation
-> VexLogger chronological facts
-> downstream history/debug/server projection
~~~

This side answers:

~~~text
What did the user/request mean?
What semantic action was accepted?
What runtime work was requested?
What actually happened?
What actually appeared?
In what order?
~~~

### The bridge

The bridge between the sides is **the explicit object graph**, not a universal ID:

~~~text
construction side
    delivers Family / Connection / Store / repository / Logger / services

semantic side
    uses those explicit objects according to their rightful responsibilities
~~~

Do not make one ID span both sides merely for convenience.

## 15. Fresh-instance guardrails

A successor must not use these sketches to justify:

~~~text
a service locator hidden behind get<T>()
reflection-based dependency lookup with no readable graph
a second TraceId/InstanceId because dependency construction has an instance key
a universal completionState
logging inside pure VexFamily.act(...)
elementId as automatic semantic Control identity
RelationshipId as dependency-container identity
a new logger root for dependency construction
~~~

Before adding machinery, preserve the relational sentence:

~~~text
readable dependency relationship
-> rightful live instance
-> explicit consumer
-> semantic action
-> accepted/observed fact
-> VexLogger chronology
~~~

## 16. Return point to Family

The intended return to Family happens only after Logger + dependency relationships are sufficiently
clear.

Then pressure the Family connection with questions like:

~~~text
What does Family actually need injected?
What belongs in Connection instead?
Which dependency lifetime owns that object?
How is that relationship readable?
Which facts are semantic Family facts?
Which facts are runtime/provider facts?
Which observing boundary reports them to Logger?
~~~

Do not make Family a dependency container or Logger host merely to connect the pieces.

<!-- [VXG RealForever][RELATIONAL-BRIDGE][LOGGER][DEPENDENCY-FACTORY][FAMILY] -->


## 17. Topology, Koin comparison and UI-ID protocol

Detailed bridge note:

~~~text
TOPOLOGY-AND-ID-PROTOCOL.md
~~~

The important additional insight is that this architecture is not trying merely to recreate Koin with
different names.

The construction engine may share ordinary DI behaviors:

~~~text
prerequisite resolution
lifetime ownership
memoized/scoped reuse
constructor delivery
~~~

but Vex's intended extra property is:

~~~text
the relationship map remains readable architecture
~~~

Also, current Elements source already defines `elementId` as the stable semantic address used by
registry relationships, logging and test tags, and nested Screen Blueprint declarations already
inherit the Screen coordinate.

Therefore a click does not need a new Logger ID merely to become remotely interpretable.

Preserve the coordinates:

~~~text
elementId       -> UI source
Control         -> semantic request
RelationshipId  -> active runtime relationship
Logger order    -> chronology
Blueprint map   -> human-readable interpretation
~~~

A future VexSync/server projection may use the matching Blueprint identity map to resolve stable IDs
back to Screen/path/kind/human names. Exact cross-version/currentness mechanics remain TO DISCUSS.

