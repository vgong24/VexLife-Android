# VexLogger — stable traceability architecture

[VXG RealForever]

> **STABLE ARCHITECTURE OWNER**
>
> VexLogger is the first architectural owner to consult when a feature, foundation, runtime, test,
> debugger, VexBoard projection, or future AI instance asks for "traceability", "history",
> "correlation", "what happened", "how did we get here", "prove the sequence", or similar temporal
> observability.
>
> Do not create a new trace ID, execution ledger, history registry, correlation manager, event root,
> or parallel logger merely because the desired information is temporal or diagnostic. First prove
> that the semantic owners + VexLogger chronology cannot already answer the question.

## 1. Ownership

VexLogger owns **chronological observation of accepted or observed facts**.

It does not own:

~~~text
Family semantic decisions
Control meaning
Effect meaning
RelationshipId meaning
active Job custody
provider/repository authority
navigation truth
presentation truth
business success/failure policy
VexBoard truth
~~~

Those rightful owners decide or observe their facts.

Then:

~~~text
rightful owner accepts / observes fact
-> VexLogger records that fact in chronology
-> downstream projections may persist / format / display it
~~~

Logging is observational. Logger failure must not rewrite, reject, retry, cancel, or otherwise change
the product fact being recorded.

## 2. Traceability routing rule

When someone asks to "add traceability", classify the need before adding architecture.

### Temporal question

Examples:

~~~text
What happened?
What happened first?
What did the user click?
What operation was requested?
Did it succeed or fail?
What State/presentation actually arrived?
How long was the observed interval?
What meaningful milestone followed?
~~~

Owner:

~~~text
VexLogger chronology
~~~

Do not answer a temporal question by adding identity or retained state to VexExecution.

### Semantic identity question

Examples:

~~~text
Which Track?
Which Account?
Which Family relationship?
Which Control/resource relationship?
~~~

Owner:

~~~text
the semantic/domain owner
RelationshipId when the question is active execution relationship identity
~~~

Logger records or projects that already-owned identity when useful. Logger does not mint a competing
semantic identity.

### Present execution question

Examples:

~~~text
Is this relationship active now?
Which Job(s) are currently executing for this relationship?
Cancel this active relationship.
~~~

Owner:

~~~text
VexExecution
RelationshipId -> actual Job(s)
~~~

Do not preserve completed execution snapshots merely to answer historical questions. Completed
history belongs to Logger.

### Human/debug projection question

Examples:

~~~text
Show me the causal trail.
Show me discrepancies.
Explain why the UI ended here.
~~~

Owner:

~~~text
VexBoard / debug projection
<- semantic owners
<- VexLogger chronology
<- current execution custody when present-state information is needed
~~~

Projection is not permission to create a new truth owner.

## 3. Fact law

Production Logger records facts at the point they become accepted or observed.

Examples:

~~~text
App opened
Control accepted
Button clicked
Form submitted
Save requested
Save succeeded
Save failed
Screen disappeared
Screen arrived
Dialog appeared
Dialog disappeared
Song ended
AutoAdvance selected next song
Song started
Provider connection lost
Provider connection restored
~~~

The exact event vocabulary may evolve with real consumers. The stable rule is:

> log the independently meaningful fact owned at that boundary; do not invent another event merely
> to narrate a relationship already derivable from chronological facts.

Example:

~~~text
Screen A disappeared
Screen B arrived
~~~

already establishes the observed temporal transition.

Do not additionally require:

~~~text
Transition(A, B)
~~~

unless that transition itself is an independently authored semantic fact rather than a derived
description.

## 4. User-visible truth is part of the causal trail

Backend/runtime success is not automatically the same as lived presentation success.

A useful trace may be:

~~~text
User selected Dark
Save requested
Save succeeded
Dark presentation arrived
~~~

This makes discrepancies observable:

~~~text
User selected Dark
Save succeeded
Light presentation remained / arrived
~~~

The Logger therefore must be able to receive facts from presentation/navigation owners as well as
runtime/semantic owners.

Do not infer "shown to the user" solely from a request, intended target, or successful backend
operation.

## 5. Expectation is not observation

Production history must not silently store an expected future result as though it already happened.

Bad pattern currently demonstrated by the reviewed source:

~~~text
calculate intended destination B
-> log screenAfter = B
-> apply navigation
~~~

At logging time, B has not yet been observed as current.

Refined law:

~~~text
production
    accepted / observed fact

debug
    optional expectation
    optional actual observation
    optional discrepancy
~~~

A debug surface may say:

~~~text
expected: Screen B
actual:   Screen C
~~~

but `expected` must never be serialized into the production fact field that means `actual`.

## 6. Chronology is the default correlation mechanism

If two facts can be related by their rightful semantic identity and chronological order, do that
before inventing another correlation ID.

Example:

~~~text
RelationshipId = ThemeMode.Select
Save requested
Save succeeded
Dark presentation arrived
~~~

The relationship identity already answers **which semantic relationship**.
Chronology answers **when and in what order**.

Do not add a process-global `InstanceId`, `TraceId`, `CorrelationId`, `SourceRef`, or public
Handle merely to make the sequence printable.

If exact occurrence-level correlation is genuinely ambiguous because multiple simultaneous
occurrences under the same semantic relationship must be distinguished, first ask:

~~~text
1. Is a missing semantic child/sub-ID actually the distinction?
2. Can the relationship's actual Job object provide the private runtime distinction while active?
3. Is this only a debug/persistence implementation detail rather than reusable architecture?
~~~

Only after those fail may an occurrence correlation value be earned, and it remains as narrow and
private as the demonstrated need permits. It must not become semantic identity by convenience.

## 7. Do not duplicate derivable data

Chronology should prefer primary facts over redundant narration.

Examples of values that are normally derivable:

~~~text
duration between adjacent timestamps
screen visit duration from observed arrival/disappearance timestamps
A -> B temporal adjacency from ordered facts
completed-history status from success/failure/cancellation facts
~~~

A storage/indexing layer may keep derived data for a demonstrated performance/storage reason, but
that does not promote the derived field into architectural truth.

Specifically, the reviewed current fields:

~~~text
durationSincePreviousSeconds
screenBeforeId
screenAfterId
section
sequence
~~~

must each justify their own need.

- timestamps are the primary chronology; duration is normally derived;
- observed Screen arrival/disappearance facts are primary; predicted before/after pairs are not;
- `section` must own independent meaning rather than formatting convenience;
- `sequence` may be a deterministic storage/order mechanic but is not semantic or execution
  identity merely because it is monotonic.

## 8. One Logger root, many fact producers

Do not create `VexExecutionLogger`, `VexFamilyLogger`, `NavigationTraceLogger`, or other peer
roots just because different owners emit different facts.

The architecture is:

~~~text
Family / feature semantic owner ---------+
Connection / provider owner -------------+
VexExecution ----------------------------+--> VexLogger chronology
Navigation / presentation owner ---------+
application/runtime owner ---------------+
                                         |
                                         +--> Logcat projection
                                         +--> durable LogHistory projection
                                         +--> VexBoard/debug projection
                                         +--> readable human history
~~~

Namespacing/event typing underneath VexLogger may be useful when earned. It must not become a second
truth hierarchy.

The current `VexLogger.Event.Family` tree is historical implementation evidence from an older
Family architecture; it is not automatically part of the refined event schema.

## 9. Failure isolation

A diagnostic observer is not allowed to become product authority.

~~~text
fact succeeds
logger sink fails
repository append fails
Logcat fails
VexBoard projection fails

=> original fact remains succeeded
~~~

Likewise:

~~~text
fact fails
logger succeeds

=> Logger cannot transform that failure into success
~~~

This applies to:

~~~text
VexLogger
LogHistoryPersistenceQueue
Logcat
export/debug adapters
VexBoard projections
~~~

Traceability follows truth. It does not control truth.

## 10. Persistence and projections

The reviewed app already demonstrates the correct directional boundary:

~~~text
VexLogger
-> VexLogRecord
-> LogHistoryPersistenceQueue
-> LogHistoryRepository
-> Room / in-memory implementation
-> LogHistory UI
~~~

and separately:

~~~text
VexLogger
-> VexLogcat
~~~

Persistence and Logcat are downstream projections.

They do not make the Logger's in-memory representation the only possible canonical schema, and they
do not gain authority to dictate semantic/runtime identifiers upstream.

A future schema migration should start from the stable Logger laws in this document, not preserve
legacy columns merely because they exist in Room.

## 11. Traceability anti-accretion rule

Before adding any of the following:

~~~text
TraceId
CorrelationId
InstanceId
SourceRef
execution history map
completed Snapshot registry
parallel event bus
new logger root
relationship history registry
before/after transition record
duration field
status ledger
~~~

the proposing change must answer:

~~~text
What exact question cannot be answered by:
    rightful semantic identity
    + RelationshipId for active execution
    + actual current Job custody
    + VexLogger chronology
    + downstream projection?

Is the missing information a fact, identity, current-state question, or presentation projection?

Who owns that information already?

Why can Logger not record the owner's accepted/observed fact?

What correctness is lost if this new structure does not exist?
~~~

If no required correctness is lost, do not add it.

"Traceability", "observability", "debuggability", "auditability", "future proofing", or "VexBoard
needs it" are not by themselves sufficient reasons to mint new architecture.

## 12. Current-source deltas to correct in a future implementation PR

The current production/review source remains useful evidence, but these shapes are not stable
architecture merely because they exist:

~~~text
VexLogRecord.screenBeforeId / screenAfterId
    current runtime sometimes fills screenAfter from intended navigation before navigation happens

durationSincePreviousSeconds
    derivable from timestamp chronology

VexLogger internal Compose State<List<VexLogRecord>>
    may duplicate durable LogHistory ownership; consumer need must be proven

VexLogger.Event.Family OperationBegan/Success/Failure with KCallable
    reflects older Family/execution coupling and callable-oriented tracing

section
    requires an independent semantic/session need

sequence
    may remain persistence ordering but is not semantic identity

screenScopes()
    currently reconstructs visits from before/after transition-shaped records;
    refined history should prefer actual arrival/disappearance facts
~~~

These are review targets, not instructions to delete blindly. Preserve the requirement only where a
real consumer proves it.

## 13. Stable summary

~~~text
SEMANTIC MEANING
    rightful Family/domain owner

ACTIVE EXECUTION
    RelationshipId -> actual Job(s)

CHRONOLOGICAL TRACEABILITY
    VexLogger

DURABLE HISTORY
    downstream LogHistory projection

LIVE DEVELOPER OUTPUT
    downstream Logcat projection

HUMAN/DEBUG CAUSAL VIEW
    VexBoard/debug projection

EXPECTATION
    debug-only unless/until observed as fact
~~~

When a future instance reaches for another tracing mechanism, start here.

<!-- [VXG RealForever][VEXARCHITECTURE][VEXLOGGER][STABLE-TRACEABILITY-OWNER] -->
