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

A concrete execution owner extends VexExecution.

~~~kotlin
internal class ThingExecution(
    root: VexExecutionRoot,
    private val repository: ThingRepository,
) : VexExecution(root) {
    fun start() {
        launchForCollect(
            repository::states,
        ) { state ->
            accept(state)
        }
    }

    fun refresh() {
        launchOnce(
            repository::refresh,
        )
    }
}
~~~

### Finite work

~~~text
launchOnce(callable)
    fail closed while the same source is unfinished in this owner

launch(callable)
    every invocation is intentionally independent
    each returned Job is the occurrence
~~~

Callable identity is private execution convenience for source-addressed cancellation/duplicate
protection. It is not semantic identity and is not exported as a durable/source protocol.

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

## Maintained semantic relationships

The rightful semantic/runtime owner supplies RelationshipId.

~~~text
RelationshipId -> actual Job(s)
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

- ThemeMode Save: Control.Select kind -> Family/Store binding-owned RelationshipId -> Latest Job;
- VexMaps Distribution maintained observations + finite operations;
- VexMaps destination/place search source-addressed replacement;
- VexStream application/session and service execution roots;
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
