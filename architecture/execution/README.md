# VexExecution — owned coroutine execution

[VXG RealForever]

## Human question

> Which runtime owner owns this asynchronous work, which active semantic relationship does it belong
> to when coordination matters, and how can that work stop without inventing a second execution
> identity/history system?

The current answer is deliberately small:

~~~text
VexExecutionRoot
    shared coroutine tree
    terminal admission + shutdown

one live VexExecution owner
    ordinary source-addressed Jobs
    maintained relationships:
        RelationshipId -> actual Job(s)

Job
    runtime occurrence

VexLogger
    chronology / what happened
~~~

Permanent distinctions:

~~~text
JOB = RUNTIME OCCURRENCE

RELATIONSHIP_ID
!= JOB
!= CALLABLE
!= LOGGER FACT
!= DEPENDENCY ID

EXECUTION ACTIVE CUSTODY
!= LOGGER HISTORY

VEX_EXECUTION_OWNER
!= VEX_EXECUTION_ROOT
~~~

There is no public execution InstanceId, SourceRef, Handle, completed Snapshot registry, or
root-level activeExecutions projection.

## Authoring surface

There are two truthful entry shapes.

A class that genuinely owns a sustained body of source-addressed async work may extend VexExecution:

~~~kotlin
internal class ThingSession(
    root: VexExecutionRoot,
) : VexExecution(root) {
    fun refresh() {
        launchOnce(
            ::refreshForExecution,
        )
    }
}
~~~

When an already-existing semantic/runtime owner authors the work, direct entry preserves that
ownership while VexExecution supplies custody:

~~~kotlin
VexExecution.begin(
    executionRoot = root,
    owner = family,
    approach = accepted.realizationBegin.approach,
    relationshipId = accepted.relationshipId,
    operation = store::save,
    argument = effect.mode,
)

VexExecution.observe(
    executionRoot = root,
    owner = family,
    source = store::states,
) { observed ->
    ...
}
~~~

The direct surface retains owner-scoped custody internally. owner is the already-existing live scope,
not another semantic ID.

## Choose by meaning before choosing a code shape

Do not begin with "which Execution class should I create?". First identify what is already true:

~~~text
Does semantic/domain structure already define a maintained relationship?
    -> consume its RelationshipId + authored Approach

Is the exact callable / Flow source itself the local work being controlled?
    -> source-addressed launch / observation may be sufficient

Does an existing Session/service/domain owner already own the operation and its lifetime?
    -> keep the operation there; use VexExecution for custody

Is there a genuinely distinct sustained execution responsibility with its own lifetime/work set?
    -> a concrete VexExecution owner may be earned
~~~

These routes intentionally produce different Kotlin shapes while preserving the same responsibility
law. Similar syntax is not evidence that two meanings should share another abstraction, and different
syntax is not evidence that the meanings are unrelated.
### Finite work

~~~text
launchOnce(callable)
    fail closed while the same source is unfinished in this owner

launch(callable)
    every invocation is intentionally independent
    each returned Job is the occurrence
~~~

The callable-reference style is deliberate **only** for source-addressed work. It keeps the exact
thing being invoked and the narrow local source address as one authoring expression:

~~~kotlin
launchOnce(repository::refresh)
~~~

rather than duplicating it:

~~~kotlin
launchOnce(RefreshKey) {
    repository.refresh()
}
~~~

The callable is therefore private execution convenience for source duplicate/control behavior. This
does **not** establish a repository-wide "reflection pattern". Do not use Kotlin class/member scanning,
callable names, reflected class names, annotations, or runtime discovery to invent semantic,
dependency, Logger, protocol, or RelationshipId meaning.

~~~text
CALLABLE REFERENCE
    may be exact local source/work

CALLABLE NAME / REFLECTED TYPE NAME
    readable diagnostic text at most
    != semantic identity
    != durable identity
    != RelationshipId
~~~

### Maintained observation

~~~text
launchForCollect(flow source)
    one active observation of that source in this owner
    Job owns collection lifetime
~~~

Source creation/upstream failure may use bounded source recovery when enabled. Failure thrown while
consuming a valid emitted value is downstream failure and does not auto-resubscribe.

~~~text
SOURCE / SUBSCRIPTION FAILURE
    may recover when runtime owner opted in

ON_EACH / CONSUMER FAILURE
    no automatic restart
~~~

Cancellation is terminal.

### Partner receipt versus Store observation

The Partner patient zero pressures both execution-addressing modes at once:

~~~text
Connection receipt/admission
    live Connection execution owner
    + private runtime RelationshipId
    + Queued
    -> ordered finite receipt/admission Jobs

Store observation
    exact store::snapshots Flow source
    + live Family owner
    -> one maintained observation Job
~~~

Do not persist the runtime RelationshipId and do not substitute the Partner Connection.Id for it.
Connection.Id belongs to source/observation provenance; RelationshipId coordinates active Jobs.

Likewise, a Family-authored Pump request uses beginQueued(Effect) -> activate() -> Act.Accepted
RelationshipId + realizationBegin -> exact Effect-bound Method -> Act.Accepted.launch(...) ->
VexExecution. The Method/executor does not choose Queued again, and no intermediate follow-up object
reconstructs the authored Act.

## Maintained semantic relationships

The rightful semantic/runtime owner supplies RelationshipId.

Current identity / custody relationship:

~~~text
RelationshipId
    maintained relationship identity

VexExecution.admit(...)
    registers one lazy Job occurrence under that RelationshipId

Job
    one runtime occurrence held in custody for that relationship

Job.start()
    starts the already-admitted occurrence
    does NOT create, replace, rename, or reconstruct RelationshipId

callable / Job name / Logger chronology
    may describe or project runtime facts
    never become the maintained relationship identity
~~~

~~~text
RelationshipId -> actual Job(s)
~~~

This is intentionally different from source-addressed work. For Earliest / Latest / Queued,
RelationshipId is the one semantic coordination address. The operation function may still be passed
to the current engine so it can invoke the work and perform local implementation bookkeeping, but
that callable must never be interpreted as a peer relationship identity.

~~~text
SOURCE-ADDRESSED
    launchOnce(source)
    launch(source)
    launchForCollect(source)
    forceStop(source)
    joinStop(source)

    source reference is the narrow local address

RELATIONSHIP-ADDRESSED
    RelationshipId
    -> Earliest / Latest / Queued
    -> Job(s)

    callable is work to invoke
    callable != another RelationshipId
~~~

Only identity-bearing semantic coordinates belong in that ID. Ordinary operation payload does not
become identity merely because it is present.

Overlap behavior uses the same relationship address:

~~~text
Earliest
    existing unfinished Job is sufficient

Latest
    cancel unfinished predecessor Job(s)
    wait through predecessor teardown
    then run newest Job

Queued
    preserve admitted Job(s)
    run in arrival order
~~~

The same owner may cancel the relationship through:

~~~text
cancelActiveRelationship(RelationshipId)
~~~

Absent relationship is a no-op.

No CoordinationKey, OwnerToken, TraceId, SourceRef, or occurrence ID is added around the
relationship.

## Owner isolation

A VexExecutionRoot may be shared.

Therefore an individual execution owner can stop only its own work:

~~~text
forceStopOwned()
joinStopOwned()
~~~

That must not terminate sibling VexExecution owners using the same root.

Only the rightful root lifetime owner performs:

~~~text
root.forceStopAll()
root.joinStopAll()
~~~

Examples of real root lifetime owners include an application/ViewModel session or one Android
Service that created the root.

## Source-addressed local control

Some existing execution owners have one operation whose callable is already the narrow local address
needed for replacement/cancellation.

~~~text
forceStop(callable)
joinStop(callable)
~~~

This remains private/protected convenience inside the execution owner. The callable does not become a
cross-feature semantic RelationshipId.

When a real semantic relationship exists, prefer RelationshipId.

Do not "standardize" these two styles into one universal abstraction merely for consistency. Their
different authoring forms preserve different meanings: one addresses an exact local source; the
other addresses an already-authored semantic relationship.

## Structured work and provider handoff

Nested coroutines remain descendants of the one admitted Job.

A provider/thread-affinity boundary may use:

~~~kotlin
handoff(providerDispatcher) {
    providerCommit()
}
~~~

handoff changes execution context inside the same Job. It does not create another occurrence,
relationship, retry boundary, or lifetime owner.

## Operation/result callback boundary

For begin(...) / relationship operation helpers:

~~~text
beforeOperation
-> operation(argument)

operation throws
-> onFailure(operation failure)

operation returns result
-> onResult(result)

onResult throws
-> Job fails
-> does NOT call onFailure
~~~

~~~text
OPERATION FAILURE
!= RESULT-ADMISSION FAILURE
~~~

## Failure / history ownership

Execution does not retain completed history.

~~~text
active Job finishes
-> source/relationship custody removes that Job
-> empty relationship is removed

historical question
-> VexLogger
~~~

A failed managed Job remains a failed Kotlin Job occurrence. Semantic/provider owners report
meaningful accepted/observed failure facts to VexLogger when that history is required.

## Current production pressure

PR #209 exercises the refined execution law through:

- ThemeMode Save: Family Parameter.Mode is carried through Control/Effect; Act.Method.Save carries the exact accepted Effect; Act.Accepted.launch(...) verifies that Effect and consumes its live Family owner + Control.Select RelationshipId + authored Approach -> zero-argument direct VexExecution -> Job custody;
- VexMaps Distribution Session directly uses VexExecution for async work whose meaning and lifecycle the Session already owns;
- VexMaps destination/place search source-addressed replacement;
- VexStream application/session and service execution roots;
- Partner patient zero: inbound receipt/admission is queued under a private owner-local RelationshipId on each live Connection, while Store -> Family Flow observation remains exact-source addressed; durable Connection.Id provenance is deliberately not reused as runtime RelationshipId;
- owner-scoped shutdown while a root may host sibling owners.

The focused execution tests pressure:

- launchOnce duplicate rejection;
- independent Jobs as distinct runtime occurrences;
- Earliest / Latest / Queued;
- automatic relationship cleanup;
- relationship cancellation;
- sibling-owner isolation;
- structured child cancellation;
- source-vs-downstream collection failure;
- bounded source recovery;
- provider handoff;
- root terminal shutdown.

Repository-owned Gradle/JVM proof remains required on the exact candidate head before this PR may
claim the refinement proved.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
