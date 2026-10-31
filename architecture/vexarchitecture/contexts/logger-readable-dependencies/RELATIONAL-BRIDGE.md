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

## 3. Do not collapse dependency identity into execution identity

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

## 4. UI element identity is context, not automatically Control identity

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

## 5. The "admit -> completionState" box remains TO DISCUSS

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

## 6. Dependency construction should be inspectable

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

## 7. Build ordering / memoization pressure

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

## 8. Logger relationship to dependency construction

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

## 9. Fresh-instance guardrails

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

## 10. Return point to Family

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
