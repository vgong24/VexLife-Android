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

## 3. Relationship identity — settled law

The relationship layer itself is no longer held.

The refined architecture uses `RelationshipId` as the active-work identity.

Its meaning comes from structure that already exists:

~~~text
owning live semantic/runtime scope
+ Control kind
+ only the typed child/sub-IDs that actually distinguish that Control relationship
= RelationshipId
~~~

The earlier implementation demonstrated the payload distinction clearly:

~~~text
Play(track=A, position=10)
Play(track=A, position=99)
-> same RelationshipId when TrackId is the identity-bearing child

Play(track=A)
Play(track=B)
-> different RelationshipIds
~~~

Ordinary payload does not become identity merely because it is present on the Control.

The relationship must remain an **invisible consequence of the already-authored semantic structure**.
Do not add a second authoring ceremony such as:

~~~text
family.relationship(control)
ThemeModeRelationships
manual relationship registries
automatic append(effect)
string-flattened relationship descriptions
~~~

The exact Kotlin spelling may still be made smaller during implementation. That is an implementation
choice, not an unresolved architecture question.

## 4. Execution — settled active-custody law

The current #206 machinery is not the baseline.

The refined execution law is:

~~~text
one live VexExecution owner

active:
    RelationshipId
    -> actual Job(s)
~~~

The Job is the runtime occurrence.

Admission places the Job under the already-known RelationshipId.

Completion removes the Job.

~~~text
Job completes
-> remove Job from RelationshipId

no Jobs remain
-> remove RelationshipId
~~~

Therefore:

~~~text
map contains RelationshipId
= active work exists for that relationship

map does not contain RelationshipId
= no active work exists for that relationship
~~~

No second `activeExecutions` projection is required to restate what the Job map already knows.

The object boundary of one live VexExecution owner already supplies owner isolation. A separate
`OwnerToken` is not architectural identity.

The following #206 concepts are not carried forward as required architecture:

~~~text
OwnerToken
KCallable sourceIdentity as relationship identity
SourceRef
CoordinationKey
CoordinationIdentity
process-global InstanceId
public Handle
Snapshot history
activeExecutions
callable-based cancellation identity
~~~

Useful requirements discovered while building #206 still survive:

~~~text
two live runtime owners must not interfere

same callable name must not imply same relationship

completed work must leave active custody automatically

root shutdown must terminate the shared execution lifetime

source/upstream collection failure must remain distinguishable from downstream consumer failure
~~~

Those requirements are proved against the smaller relationship-led structure rather than preserving
the machinery that happened to discover them.

### Overlap behavior

The overlap meanings remain:

~~~text
Independent
    accepted work does not coordinate with prior work

Earliest
    an existing unfinished Job under this RelationshipId is sufficient

Latest
    cancel unfinished Job(s) under this RelationshipId
    then admit the newest Job

Queued
    preserve accepted Job(s) under this RelationshipId
    execute them in admission order
~~~

All operate on the same RelationshipId.

There is no second CoordinationKey identity.

### Cancellation

Cancellation is settled at the same address:

~~~text
cancelActiveRelationship(RelationshipId)
~~~

Meaning:

~~~text
find the Job(s) currently stored under that RelationshipId
-> cancel them

relationship absent
-> no-op
~~~

Cancellation does not need:

~~~text
Handle.forceStop()
callable re-supply
SourceRef
InstanceId
Control capability boilerplate
~~~

A feature may still have its own semantic user-facing Cancel Control when the product meaning requires
one. That feature meaning is separate from the foundational runtime operation above.

### Root lifetime

`VexExecutionRoot` remains the shared lifetime boundary for terminal shutdown / dispatcher custody.

It does not need to become the identity owner for every active relationship.

One VexExecution owner should not terminate a sibling merely because both share the same root.

## 5. What remains implementation detail versus held architecture

The following are **not unresolved architectural questions**:

~~~text
RelationshipId is the active-work identity
active custody is RelationshipId -> actual Job(s)
Jobs auto-remove on completion
empty relationships auto-remove
Earliest / Latest / Queued operate on the same RelationshipId
cancelActiveRelationship(RelationshipId) addresses the same map
~~~

Still open only at implementation-detail level:

~~~text
smallest Kotlin type declaration for RelationshipId
smallest typed way to expose Control identity-bearing sub-IDs
exact private collection type used for Job(s)
whether any convenience return value is useful for launch/cancel
~~~

Those choices must not reintroduce a second identity or history layer.

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
RelationshipId / VexExecution exact Kotlin spelling
VexLogger final record schema
Gradle/JVM PASS
device PASS
VettedWithHuman
MASTERED
merge readiness
~~~

Its purpose is continuity without false authority.

<!-- [VXG RealForever][VEXARCHITECTURE][REFINED-BOUNDARIES][REFERENCE-ONLY] -->
