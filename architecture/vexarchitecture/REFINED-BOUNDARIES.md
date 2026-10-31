# VexArchitecture — refined boundaries

[VXG RealForever]

> **REFERENCE SNAPSHOT — NOT A MERGE RECOMMENDATION**
>
> This document records the current refined architecture after reviewing the actual #206 source and
> the earlier ThemeMode / RelationshipId implementation. It distinguishes what survived review from
> what remains deliberately unresolved.

## 1. Semantic Family

Reference source:

~~~text
reference/behavior/VexFamily.kt
~~~

Preserved grammar:

~~~text
State
    current semantic truth

Control
    semantic request / affordance

Act
    lawful result of exact CURRENT State + Control

Effect
    requested runtime transformation
    != resulting State

CURRENT State + Control
-> Act?
-> admitted State? + Effect?
~~~

Permanent boundary:

~~~text
VexFamily
!= Store
!= logger
!= coroutine
!= provider
!= runtime identity registry
!= cancellation manager
~~~

Do not add:

~~~text
family.relationship(control)
relationship tables
execution keys
logger dependencies
runtime cancellation Controls
~~~

merely so another layer can recover information already present in the semantic call path.

## 2. ThemeMode language

Reference source:

~~~text
reference/ui/ThemeMode.kt
~~~

Preserved meaning:

~~~text
Control.Select(mode)
-> Act
-> Effect.Save(mode)
~~~

The selected mode remains ordinary Control / Effect payload unless a demonstrated runtime requirement
proves that the mode itself must scope independent active work.

Historical vocabulary:

~~~text
Save
-> local val persistence
-> saveLatest / saveState
-> PersistSelection
-> PersistenceExecution.persist
-> store.save
~~~

Only the movement of runtime mechanics out of Family is retained as an architectural improvement.
The vocabulary churn itself is not.

## 3. Relationship identity — preserve the requirement, not the old ceremony

The earlier implementation demonstrated a useful identity law:

~~~text
live owner scope
+ Control kind
+ optional typed ResourceId
= active relationship identity
~~~

It also demonstrated an important payload distinction:

~~~text
Play(track=A, position=10)
Play(track=A, position=99)
-> same relationship when TrackId is the identity-bearing child

Play(track=A)
Play(track=B)
-> different relationships
~~~

The refined architecture preserves **that requirement**.

It does **not** yet freeze the old exact Kotlin representation.

Held:

~~~text
exact RelationshipId class/interface shape
exact Control.Resource / ResourceId surface
whether RelationshipId must ever be public
exact cancellation signature
~~~

Rejected as premature replacement ceremony:

~~~text
family.relationship(control)
ThemeModeRelationships
automatic append(effect)
manual relationship registries
string-flattened identity
~~~

The target behavior is that identity-bearing information already present in the semantic objects
remains available to runtime custody without a second manually-authored description.

## 4. Execution — current #206 machinery is not the baseline

The current #206 implementation contains:

~~~text
OwnerToken
KCallable sourceIdentity
SourceRef
CoordinationKey
CoordinationIdentity
InstanceId
Handle
Snapshot
activeExecutions
active map
coordinations map
~~~

Those names are **review evidence**, not reconstruction requirements.

The useful requirements discovered while building them survive:

~~~text
two live runtime owners must not interfere

same callable name must not imply same active relationship

one runtime owner must not terminate a sibling merely because both share a root

completed work must leave active custody

Earliest / Latest / Queued overlap behavior must remain correct where actually required

source/upstream collection failure must remain distinguishable from downstream consumer failure

root shutdown must terminate the whole execution lifetime and reject later admission
~~~

The replacement implementation should prove those properties with the smallest structure possible.

### Present-custody law

Current direction:

~~~text
meaningful active relationship
-> actual Job(s)

Job completes
-> remove Job

no Jobs remain
-> relationship no longer active
~~~

Do not add a second `activeExecutions` truth merely to restate the Job registry.

### Occurrence law

A Job is already an exact runtime occurrence.

Do not promote an occurrence number into reusable architecture merely because tests or logs want an
ordering/correlation aid.

If an exact occurrence needs private internal distinction, that may remain implementation detail.

### Handle law

No current production consumer reviewed in this trajectory demonstrated a need to retain a public
Handle:

~~~text
ThemeMode PersistenceExecution.persist()
    returned Handle
    caller ignored it

VexMapsDistributionExecution
    ignored launch returns
    cancelled by re-supplying the callable
~~~

Therefore a public Handle is held, not presumed.

Preserve its underlying requirements only if a real consumer demonstrates them.

### Cancellation law

Current code such as:

~~~text
forceStop(session::prepareForExecution)
~~~

addresses meaningful work through its implementation callable.

That boundary is under refinement.

Target principle:

~~~text
cancel meaningful active relationship
not callable
not SourceRef
not InstanceId
not retained Handle
~~~

Exact API remains held until the relationship identity representation is settled.

## 5. Overlap behavior

The behavioral meanings remain useful:

~~~text
Independent
    accepted work does not coordinate with prior work

Earliest
    existing unfinished related work is sufficient

Latest
    newest related work supersedes unfinished predecessors

Queued
    all accepted related work remains meaningful and executes in admission order
~~~

These behaviors do not by themselves justify a second `CoordinationKey` identity vocabulary.

If the runtime already knows which active relationship the work belongs to, overlap policy operates
on that relationship.

## 6. Observation and recovery

Useful distinction preserved:

~~~text
source creation / upstream Flow failure
!=
downstream onEach consumer failure
~~~

The current generic defaults:

~~~text
maxRestarts = 3
restartDelayMillis = 250
~~~

are not accepted merely because they exist.

Recovery policy must remain with the runtime owner that can lawfully decide whether retry is valid.

## 7. Logger — chronology, not duplicate narration

VexLogger belongs in the architecture review because it owns temporal observation.

Target responsibility:

~~~text
rightful owner accepts / observes fact
-> Logger records fact in chronology
-> downstream projections may retain / display it
~~~

Examples of independently meaningful facts:

~~~text
App opened
Screen arrived
Screen disappeared
Button clicked
Save requested
Save succeeded
Save failed
Song ended
AutoAdvance selected next song
Song started
~~~

Do not persist a second narration when adjacent facts already prove the relationship.

~~~text
A disappeared
B arrived

already proves temporal A -> B
~~~

### Current logger fields under challenge

Current implementation includes:

~~~text
sequence
durationSincePreviousSeconds
screenBeforeId
screenAfterId
section
~~~

Questions:

~~~text
sequence
    storage/order mechanic or semantic fact?

durationSincePreviousSeconds
    derivable from timestamps?

screenBeforeId / screenAfterId
    observed facts or predicted transition narration?

section
    independent truth or formatting/session bookkeeping?
~~~

Current `VexRuntime` often calculates a navigation target and logs it before `applyIntent(...)`
actually performs the navigation. That is expectation mixed with production history.

Refinement target:

~~~text
production
    observed / accepted facts

debug
    optional expectation
    optional discrepancy detection
~~~

Expectation must never be silently promoted into production truth.

## 8. VexBoard / human projection

VexBoard should consume semantic/runtime/logging truth rather than force the architecture to invent
human-readable implementation IDs.

Prefer:

~~~text
ThemeMode
Select Dark
Save requested
Save succeeded
Dark shown
~~~

over:

~~~text
OwnerToken
CoordinationKey
SourceRef
InstanceId
~~~

Implementation diagnostics may be expandable detail if a real debugging consumer earns them.

## 9. What future reconstruction should challenge first

A successor should not begin by copying #206 source.

Start from this question:

> What is the smallest runtime structure that can keep the already-known Control/sub-ID relationship
> attached to actual active Jobs without requiring another relationship-authoring API?

Then pressure that candidate against the demonstrated consumers:

~~~text
ThemeMode Save ordering
Distribution launchOnce
Distribution preparation / leg cancellation
long-lived observation
owner isolation
root shutdown
source-vs-downstream failure distinction
~~~

Only after the runtime shape survives those pressures should Logger and VexBoard projection be
implemented around it.

## 10. Snapshot non-claims

This snapshot does **not** claim:

~~~text
RelationshipId final API
VexExecution final implementation
VexLogger final record schema
Gradle/JVM PASS
device PASS
VettedWithHuman
MASTERED
merge readiness
~~~

Its purpose is continuity without false authority.

<!-- [VXG RealForever][VEXARCHITECTURE][REFINED-BOUNDARIES][REFERENCE-ONLY] -->
