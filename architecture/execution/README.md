# VexExecution — mastered runtime execution boundary

[VXG RealForever]

VexExecution is runtime mechanics. It is not semantic State, Control, Act, Effect, provider truth,
or an event ledger.

## Ownership tree

~~~text
VexExecutionRoot
    owns one execution-tree lifetime
    owns the dispatcher
    owns terminal root shutdown
    exposes active execution custody

VexExecution
    one exact live runtime owner under that root
    owns source-scoped launch / observation / cancellation
    owns owner-local coordination keys

Handle
    one exact admitted execution occurrence
    retains that occurrence's Snapshot through completion
~~~

The root and an execution owner are intentionally different boundaries.

~~~text
ROOT
    "all runtime work under this lifetime"

OWNER
    "the work belonging to this exact live runtime component"
~~~

Stopping one owner must not stop a sibling merely because the two owners use the same callable names.

## Identity layers

Execution keeps three different identities separate.

### SourceRef — diagnostics only

~~~text
owner class + callable name
-> SourceRef
~~~

SourceRef exists for readable diagnostics/navigation.

It is not runtime ownership and is not the uniqueness key.

Two live instances of the same execution class may legitimately expose the same SourceRef.

### Private callable identity — source ownership

The exact callable reference is retained privately by the execution engine.

~~~text
exact live VexExecution owner
+ exact callable identity
=
source ownership
~~~

This identity governs:

- launchOnce duplicate admission;
- long-lived observation duplicate admission;
- source-specific forceStop / joinStop.

It is deliberately not a public architecture noun.

This avoids false collisions between:

~~~text
Owner A -> Repository.states
Owner B -> Repository.states
~~~

even though both diagnostic SourceRefs may be identical.

It also avoids treating overloaded functions that happen to share a name as the same runtime source.

### CoordinationKey — owner-local overlap grouping

Some runtime work intentionally coordinates even when its callable/argument differs.

~~~text
exact live VexExecution owner
+ CoordinationKey
=
one maintained coordination relationship
~~~

CoordinationKey has no product meaning supplied by VexExecution. The concrete runtime owner decides
what work belongs together.

The same key value under two different live VexExecution owners does not coordinate across them.

## Finite work

### launch(...)

Every invocation is intentionally independent.

~~~text
same owner
same callable
multiple launch(...)
-> multiple independent InstanceIds
~~~

### launchOnce(...)

At most one unfinished invocation of that exact owner + callable is admitted.

~~~text
same owner + same callable + unfinished
-> second launchOnce fails closed

different live owner + same callable
-> independent and valid
~~~

This is source ownership, not semantic request identity.

## Coordinated finite work

VexExecution exposes three explicit owner-bound operations:

~~~text
launchEarliest(key, ...)
    existing unfinished member is sufficient
    later request receives the existing Handle

launchLatest(key, ...)
    newest request supersedes unfinished predecessors
    predecessor cleanup completes before newest work starts

launchQueued(key, ...)
    every accepted request remains meaningful
    work executes in acceptance order
~~~

There is no public Approach enum and no second static begin* API.

Feature/runtime code chooses one explicit operation at the callsite.

A CoordinationKey cannot silently change overlap behavior while unfinished members still exist.

## Long-lived observation

~~~text
observe(source, recovery, onEach)
~~~

Observation is owner-scoped.

A second active observation of the same exact owner + callable fails closed.

Another live execution owner may observe the same source independently.

### ObservationRecovery

Recovery belongs to the individual observation, not VexExecutionRoot.

~~~text
ObservationRecovery(
    maxRestarts,
    restartDelayMillis,
)
~~~

Only source creation or upstream Flow collection failure is recoverable.

~~~text
source fails
-> optional bounded restart

downstream onEach fails
-> execution FAILED
-> no source restart
~~~

This prevents a consumer bug from being misclassified as provider/source instability.

The default is:

~~~text
ObservationRecovery.None
~~~

so generic observation never silently assumes retry is lawful.

A sustained owner that has actually earned source recovery states it explicitly at the callsite:

~~~text
observe(
    source = ...,
    recovery = ObservationRecovery(...),
)
~~~

## Runtime occurrence observation

Every admitted execution gets:

~~~text
InstanceId
SourceRef
Snapshot
Handle
~~~

### Snapshot

VexExecution.Snapshot is runtime diagnostics, not semantic product State.

~~~text
STARTING
RUNNING
RESTARTING
COMPLETED
CANCELLED
FAILED
~~~

A Snapshot may also carry:

- attempt;
- restartCount;
- failure class/message.

The name Snapshot is deliberate so AI/humans do not confuse this runtime record with
VexFamily.State.

### Handle

One Handle owns control/observation of one occurrence:

~~~text
instanceId
sourceRef
snapshot

forceStop()
joinStop()
awaitCompletion()
~~~

The Handle retains its terminal Snapshot even after root active custody no longer lists that
occurrence.

## Active root projection is not history

VexExecutionRoot.activeExecutions is:

~~~text
StateFlow<Map<InstanceId, Snapshot>>
~~~

and contains current active custody only.

Completed/cancelled/failed occurrences are removed from that map when their Jobs finish.

This prevents a long-lived app/session root from becoming an unbounded execution-history ledger.

If durable diagnostic history is required, it belongs to a separately-earned logging/diagnostic
surface rather than being smuggled into root StateFlow retention.

~~~text
STATEFLOW != EVENT_LEDGER
~~~

## Cancellation levels

Cancellation follows the ownership tree.

~~~text
Handle
-> one exact occurrence

VexExecution source stop
-> exact live owner + exact callable

VexExecution owner stop
-> all work owned by that exact live VexExecution

VexExecutionRoot stop
-> terminal shutdown of the whole execution tree
-> no further admission
~~~

Owner shutdown does not close the shared root.

Root shutdown does.

## SourceRef does not authorize cancellation

SourceRef may be equal across live owners.

Therefore:

~~~text
SourceRef equality
!= same owner
!= same source ownership
!= permission to cancel sibling work
~~~

The engine uses private owner + callable identity for cancellation.

## Semantic non-ownership

VexExecution deliberately does not own:

~~~text
Family State meaning
Control availability
Act meaning
Effect meaning
provider response interpretation
stale semantic result identity
request generation/revision
Store semantics
logging semantics
UI lifetime meaning
~~~

A concrete Connection/runtime may consume VexExecution to realize an Effect or maintain an
observation, but that does not move semantic meaning into Execution.

Example:

~~~text
ThemeModeFamily.Effect.PersistSelection
-> ThemeModeConnection
-> private PersistenceExecution
-> launchQueued(private CoordinationKey, ThemeModeStore.save)
~~~

The private key means only "these persistence attempts coordinate in this runtime owner."

It does not become ThemeMode semantic identity.

## One authoring surface

The mastered API intentionally removes the earlier split between:

~~~text
subclass launch* methods
AND
static/companion begin* methods
~~~

Current authoring is owner-bound through one VexExecution instance.

There is no current:

~~~text
begin(...)
beginEarliest(...)
beginLatest(...)
beginQueued(...)
beginReplaceable(...)
beginSequenced(...)
RelationshipFoundationApi
public Approach enum
generic onResult/onFailure operation wrapper
VexExecution handoff helper
~~~

Provider result interpretation and dispatcher handoff belong to the concrete runtime owner that
actually needs them.

## Testing

Execution tests should prove mechanics, not semantic architecture.

Important current properties include:

~~~text
same diagnostic SourceRef across two live owners
-> both may run

same owner + same callable + launchOnce
-> duplicate unfinished launch rejected

same owner + same observed callable
-> duplicate active observation rejected

different owner + same observed callable
-> independent observations allowed

source stop
-> affects only exact owner + callable

owner stop
-> affects only that owner

root stop
-> closes the whole tree and future admission

Earliest / Latest / Queued
-> owner-local CoordinationKey behavior

source failure
-> bounded ObservationRecovery

downstream consumer failure
-> FAILED, no automatic source restart
~~~

Virtual-time fixtures own scheduler/root setup only. Observation recovery remains visible at the
observation callsite.

## Current open edges

The following are not automatically promoted by this mastered runtime boundary:

~~~text
durable execution-history ledger
cross-process execution restoration
global semantic request IDs
generic provider retry policy
business-level timeout policy
generic Connection framework
~~~

Those require demonstrated consumers and rightful owners.

<!-- [VXG RealForever][VEXEXECUTION][MASTERED][OWNER-SCOPED][DIAGNOSTIC-SOURCE][ACTIVE-CUSTODY] -->
