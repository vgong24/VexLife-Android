# VexExecution — relationship-led runtime boundary

[VXG RealForever]

> **PR #206 review candidate — identity design reopened**
>
> The current implementation at the parent source head introduced several useful corrections, but
> its identity decomposition is **not** treated as mastered merely because each individual concept is
> internally coherent. This PR now re-derives the minimum runtime contract from the actual code
> relationships and the demonstrated consumers before any fresh human vetting is written.

VexExecution owns **present runtime custody**. It does not own semantic product truth, historical
sequence, human activity history, provider meaning, or a second identity vocabulary for facts that
already have a rightful parent.

## Whole-architecture position

~~~text
human / provider input
-> Family Control or already-observed reality
-> VexFamily
   -> CURRENT State + Control
   -> Act?
   -> admitted State? + Effect?

Effect
-> concrete Connection/runtime
   -> owns realization / provider interpretation / currentness
   -> forms the rightful runtime RelationshipId when execution is required

RelationshipId
-> VexExecution
   -> current active coroutine custody
   -> overlap behavior
   -> cancellation / joining
   -> Handle for one exact occurrence

accepted / observed facts
-> VexLogger
   -> chronology
   -> readable/debug/history projections

VexBoard / debug surfaces
-> project the semantic and chronological truth humans actually need
-> do not reverse-engineer meaning from execution implementation metadata
~~~

The arrows matter. A lower layer should not re-record enough metadata to reconstruct an upstream
relationship that was already known when the work was admitted.

## Target runtime responsibility

The smallest currently justified execution grammar is:

~~~text
VexExecutionRoot
    one execution-tree lifetime
    dispatcher / parent Job
    terminal root shutdown

RelationshipId
    one typed, rooted coordinate saying which runtime work belongs together

Approach
    Independent
    Earliest
    Latest
    Queued

Handle
    exact capability over one admitted coroutine occurrence

private ManagedOccurrence
    Job
    only the current runtime metadata execution actually needs
~~~

This is a candidate boundary under review. The exact public/private Kotlin representation still has
to be reconciled with the current implementation and proven before vetting.

## Relationship identity — root downward, not reverse reconstruction

The earlier Family pressure demonstrated a useful law:

~~~text
live owner
+ semantic relationship
+ optional typed child/resource identity
= one relationship coordinate
~~~

The important part is not that every RelationshipId must be Family-shaped. It is that the owner
forms identity **at the point where the meaning is already known**.

Examples:

~~~text
ThemeModeConnection
└── Persistence

DistributionSession
├── Preparation
└── LegPreparation
    └── optional typed leg/resource child

Family instance
└── Control.Play
    └── TrackId("abc")
~~~

A relationship may therefore be composable/hierarchical while preserving explicit equality laws for
its segments:

~~~text
live owner segment
    referential equality

type segment
    exact type equality

typed value/resource segment
    stable value equality

callable segment
    only when callable identity is itself the relationship meaning
~~~

Do not flatten typed identity into strings merely to make a map key.

### Why this matters

The runtime should be able to address the relationship it was given:

~~~text
is relationship X active?
which active children exist below X?
stop exact X
stop descendants of X when that operation is actually earned
~~~

It should not need to scan a bag of executions and reconstruct ancestry from:

~~~text
OwnerToken
+ callable
+ SourceRef
+ CoordinationKey
+ InstanceId
~~~

when the parent relationship was already known before launch.

## Relationship is not occurrence

One relationship may legitimately contain more than one accepted coroutine occurrence.

Queued work is the clearest pressure:

~~~text
Relationship: Upload(account-A)

occurrence
occurrence
occurrence
~~~

Therefore:

~~~text
RelationshipId
    which work belongs together

Approach
    how overlapping occurrences of that relationship behave

occurrence / Handle
    one actual coroutine execution
~~~

The occurrence must be distinguishable internally so Jobs can be controlled correctly. That does
**not** automatically earn a public/global InstanceId vocabulary.

If an external consumer later proves a need for exact concurrent-occurrence correlation, that
coordinate must be earned by that consumer. Logging/debug correlation is not by itself permission to
promote a process-global counter into semantic runtime identity.

## Overlap policy belongs to the relationship

~~~text
Independent
    each accepted occurrence is independent

Earliest
    an existing unfinished occurrence is sufficient

Latest
    newest accepted occurrence supersedes unfinished predecessors

Queued
    every accepted occurrence remains meaningful and runs in admission order
~~~

These are policies over one RelationshipId.

A separate CoordinationKey is not currently accepted as necessary merely because Earliest / Latest /
Queued need a map key. If the same object answers "which work coordinates?", it is already expressing
a relationship.

## Source/callable is implementation unless meaning says otherwise

A callable such as:

~~~text
store::save
session::prepareForExecution
repository::restorePersistedState
~~~

is ordinarily **how** a relationship is realized today.

It is not automatically **what the relationship means**.

For example:

~~~text
ThemeMode / Persistence
-> today: ThemeModeStore::save
-> tomorrow: repository::persistThemeMode
~~~

The relationship should survive that implementation change.

Callable identity may remain private execution metadata where required to invoke/cancel a specific
implementation path. It should enter RelationshipId only when the caller deliberately says that
callable identity is itself the relationship boundary.

## Historical sequence does not belong in VexExecution

Execution owns the current active set it needs to behave correctly.

VexLogger owns accepted/observed chronology.

~~~text
VexExecution
    "what work is active now, and how do I control it?"

VexLogger
    "what accepted/observed facts happened, and in what order?"
~~~

Do not turn VexExecutionRoot into a second event ledger.

Likewise, do not add a global execution number merely so history can later be sorted when the logger
already owns chronology. An internal storage/correlation mechanism may exist when implementation
requires it; that does not make it architectural identity.

## Handle

A Handle is the natural exact control capability for one admitted occurrence:

~~~text
forceStop()
joinStop()
awaitCompletion()
current runtime status if a real consumer needs it
~~~

The Handle does not need a human-readable SourceRef in order to control its Job.

Readable meaning should come from the RelationshipId / owning semantic labels. Source-code navigation
metadata can be attached privately or projected by debug tooling without becoming another identity
system.

## Current implementation findings — not yet accepted as the target

At the source head that triggered this re-review, VexExecution currently contains:

~~~text
OwnerToken
KCallable sourceIdentity
SourceRef
CoordinationKey
CoordinationIdentity
process-global InstanceId
Snapshot
activeExecutions keyed by InstanceId
~~~

Those changes solved real observed edge cases:

~~~text
same class/callable under two live owners must not collide
callable name text must not be treated as exact callable identity
one feature owner must not shut down the shared root
SourceRef text must not authorize cancellation
terminal history must not accumulate forever in root StateFlow
source failure must not be confused with downstream consumer failure
~~~

The findings survive.

The **identity fragmentation does not automatically survive**.

Before the execution boundary is called mastered again, the implementation must be challenged against
whether one rooted RelationshipId plus private occurrence mechanics preserves the same correctness
with less vocabulary and less reverse mapping.

## VexLogger boundary

VexExecution does not create history for VexBoard.

When a runtime fact is meaningful enough to observe:

~~~text
the rightful owner accepts/observes the fact
-> VexLogger records it in chronology
-> VexBoard / LogHistory / debug projections may display it
~~~

Execution should not duplicate logger sequence, user-readable labels, or causal history merely so a
future projection can reconstruct what the relationship already meant.

See:

~~~text
architecture/logging/README.md
~~~

for the whole-architecture logging boundary now included in this PR.

## Still held

~~~text
exact final Kotlin shape of hierarchical RelationshipId
whether any public occurrence identifier is actually earned
generic descendant-query API beyond demonstrated consumers
durable/cross-process runtime restoration
generic timeout/retry policy
generic provider retry policy
logger debug-span correlation for truly concurrent identical relationships
~~~

Held questions remain held. They are not reasons to preserve speculative identity machinery.

<!-- [VXG RealForever][VEXEXECUTION][RELATIONSHIP-LED][PR206-REVIEW-CANDIDATE] -->
