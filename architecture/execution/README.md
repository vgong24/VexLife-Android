# VexExecution — owned coroutine execution bridge

[VXG RealForever]

## Question

> I need asynchronous work. Where is it allowed to start, how is it named, how can it be stopped,
> and what prevents it from becoming uncontained coroutine machinery in ordinary feature code?

Put the work in a `VexExecution`.

`VexExecution` is the architectural meta-pattern. Ordinary feature/UI/domain classes do not receive
`VexCoroutineLauncher`; the launcher is internal foundation machinery. A class that needs managed
coroutine execution must make that role explicit by extending `VexExecution`.

```text
VexExecutionRoot
    -> internal VexCoroutineLauncher
    -> relationships[RelationshipId] -> owned Jobs

FeatureExecution<Owner> : VexExecution<Owner>
    -> launch(RelationshipId<Owner>, Approach, callable)
    -> launchForCollect(RelationshipId<Owner>, flowProvider) { value -> ... }
    -> forceStop(RelationshipId<Owner>)
    -> joinStop(RelationshipId<Owner>)

semantic owner
    -> supplies its typed RelationshipId<Owner>
    -> selects Approach
    -> Execution owns Job custody only
```

## Why this evolved from ordinary coroutine usage

This foundation is not a rejection of Kotlin coroutines. It is a response to repeated application-level
failure modes that appear when every feature is free to reconstruct coroutine ownership itself.

```text
TRADITIONAL / LOCAL CHOICE
    scope.launch { ... }

PRESSURE OBSERVED
    Which scope owns this?
    Who can stop it?
    Does cancellation suppress restart?
    Is the Job retained anywhere?
    Can a sibling failure kill unrelated work?
    Can this work outlive the Screen/Service/session that created it?

VEX RESPONSE
    VexExecution
    -> one sanctioned execution boundary
    -> named/keyed Job custody
    -> forceStop / joinStop
    -> supervised sibling ownership
```

The same evolution applies to Flow collection:

```text
TRADITIONAL
    launch {
        flowA.collect { ... }
        flowB.collect { ... }
    }

FAILURE
    flowA may never complete
    -> flowB never begins

VEX
    launchForCollect(flowA) { ... }
    launchForCollect(flowB) { ... }

RESULT
    one managed Job per Flow source
    -> concurrent sibling collectors
    -> independently addressable lifetime
```

And to finite work:

```text
TRADITIONAL
    scope.launch {
        callA()
        callB()
        callC()
    }

QUESTION
    Are A/B/C three independent executions,
    or one semantic operation implemented by three suspend calls?

VEX
    define one suspend operation when they are one intention
    -> launch(operationReference)

RESULT
    execution topology follows meaning rather than incidental call count
```

The architecture therefore removes repeated coroutine-lifetime decisions while leaving domain meaning
visible. Behavior, Relay, Connection/provider, repository, Compose, and WorkManager keep their own
rightful responsibilities.

## Public authoring language

Execution has one overlap contract:

~~~text
RelationshipId<Owner>
    WHICH semantic work is this?
    supplied by the rightful semantic owner

Approach.Independent
    every admitted Job remains independent even when the relationship key is the same

Approach.Earliest
    the already-running Job remains sufficient; a duplicate admission adds no Job

Approach.Latest
    newest admission supersedes unfinished predecessors
    predecessor teardown completes before newest work begins

Approach.Queued
    every admitted Job remains meaningful
    execute in arrival order
~~~

A callable never decides whether work is a duplicate. Its automatic `SourceRef` remains useful for diagnostics/navigation, but custody and control always use the relationship key.

For Family-originated work:

~~~text
live Family
+ Control name/type
+ optional typed Control ResourceId
=
Family RelationshipId<Family>
-> VexExecution RelationshipId<Family>
~~~

Family owns that composition. Concrete Family authoring chooses `begin` / `beginEarliest` / `beginLatest` / `beginQueued`; the Family bridge translates those authored meanings into the single Execution contract of `RelationshipId + Approach`. No product caller receives a Job handle or generated runtime ID.

A maintained Flow observation also receives a semantic relationship key:

~~~kotlin
launchForCollect(
    relationshipId = Relationship.RepositoryStates,
    source = repository::states,
) { state ->
    accept(state)
}
~~~

A duplicate observation for the same relationship uses Earliest semantics. Two different rightful relationships may observe the same source callable independently.

Finite execution never automatically retries. `launchForCollect` may use bounded recovery only for source/subscription failure. A failure thrown while consuming a valid emitted value becomes `FAILED` with no automatic resubscription. Intentional cancellation never restarts.

## Worker preparation and provider-affine commit

Some finite operations have two materially different execution contexts:

```text
human interaction
-> expensive / blocking preparation
-> thread-affine provider commit
```

Do not solve that by moving the provider itself onto a worker thread. Keep one semantic operation in
one managed Vex execution and make the thread boundary explicit:

```kotlin
internal suspend fun playForExecution(
    request: PlayRequest,
) {
    val prepared =
        preparePlayback(
            request,
        )

    handoff(
        mediaControllerDispatcher,
    ) {
        commitPlayback(
            prepared,
        )
    }
}
```

`launch(::playForExecution, request)` owns the whole operation. The root dispatcher owns the
ordinary worker phase. `handoff(dispatcher) { ... }` is only a structured context switch inside that
same managed Job; it does not create another execution ref, retry boundary, detached coroutine or
lifetime owner.

The target dispatcher belongs to the provider/thread-affinity owner. Do not globally equate
"foreground" with `Dispatchers.Main`: Android Media3, a native provider, a serial device API or a
test double may each have a more exact required dispatcher/looper.

Use this shape only when a real boundary exists. StateFlow/Compose consumers do not need a main-thread
handoff merely to observe immutable state; publisher/consumer machinery already owns that delivery.
The handoff exists for an actual thread-affine commit.

```text
BACKGROUND_PREPARATION != PROVIDER_COMMIT
HANDOFF != NEW_JOB
HANDOFF != NEW_OPERATION
HANDOFF != RETRY_POLICY
PROVIDER_AFFINITY != ALWAYS_MAIN
```

## Execution relationship identity

Execution keeps two distinct truths:

~~~text
VexExecution.RelationshipId<Owner>
    semantic execution key
    supplied by the rightful semantic owner
    generic Owner binding prevents cross-owner key substitution

VexExecution.SourceRef
    automatic source class + callable name
    diagnostic/navigation information only

VexExecution.Approach
    Independent / Earliest / Latest / Queued
    overlap handling for one RelationshipId
~~~

Therefore:

~~~text
RELATIONSHIP ID = CONTROL / CUSTODY ADDRESS
SOURCE REF      = DIAGNOSTIC SOURCE ADDRESS
SOURCE REF != RELATIONSHIP ID
~~~

The execution registry itself is relationship-keyed:

~~~text
relationships[RelationshipId]
    -> Approach
    -> ordered Jobs
    -> retained execution State evidence
~~~

There is deliberately no public runtime occurrence ID, returned Job control object, source-addressed stop API, or second owner/trigger/argument identity catalog. If a caller needs to stop or join work later, it uses the same semantic relationship key that admitted the work.

Family currently forms its relationship key from:

~~~text
live Family identity
+
Control name/type
+
optional typed ResourceId<Family>
~~~

That composition remains inside Family because Family owns the Control vocabulary. Execution owns coroutine custody and overlap behavior only.

`SourceRef` may change when source is renamed or obfuscated and is therefore not durable semantic identity. A RelationshipId is likewise a live runtime coordination identity unless its rightful semantic owner separately establishes a durable serialization contract.

## Collection recovery boundary

Collection recovery is deliberately narrower than "anything inside the collector failed":

```text
source() throws
OR
Flow upstream throws
-> eligible for bounded source recovery

onEach(value) throws
-> FAILED
-> restartCount unchanged
-> no automatic resubscription
```

This prevents VexExecution from silently becoming a business/provider retry engine. A consumer may
perform an effect after receiving a valid value; if that effect fails, the feature/Connection/Session
owns what that failure means and whether Retry is lawful.

Resubscription may replay source values. A StateFlow, for example, immediately emits its current
value to a new collector. Consumers that perform non-idempotent effects therefore need explicit
admission/currentness and should opt out of source recovery when replay would be unsafe.

## Root lifetime custody

```text
own VexExecutionRoot
-> own its shutdown
```

`VexExecutionRoot` currently owns an independent `SupervisorJob`. Merely retaining the root inside a
ViewModel, Service, or session does not parent its cancellation to that Android lifetime. The owner
must call `forceStopAll` / `joinStopAll` at its real terminal boundary.

A real production consumer should prove that custody before the foundation considers parent-Job
convenience or any lifecycle adapter.
## Control and custody

Control uses the same semantic key that admission used:

~~~text
forceStop(RelationshipId)
    cancel every currently unfinished Job owned by that relationship

joinStop(RelationshipId)
    cancel + join every currently unfinished Job owned by that relationship

VexExecutionRoot.forceStopAll / joinStopAll
    terminal control for the whole execution tree
~~~

There is no callable/source lookup for control and no returned Job control object to retain externally.

The internal relationship map owns both active Jobs and retained terminal State evidence:

~~~text
relationships
    LinkedHashMap<RelationshipId, MaintainedRelationship>

MaintainedRelationship
    Approach
    ordered managed Jobs / terminal evidence
~~~

A cancelling predecessor continues to occupy the relationship's active set until its Job is actually completed. Kotlin may report `isActive == false` while structured/non-cancellable teardown is still running; Latest or Queued admission may not skip that unfinished predecessor.

When a relationship has no unfinished Job, a later admission may choose a different Approach. While unfinished work exists, attempting to reinterpret the same relationship with a different Approach fails closed.

Cancellation remains Kotlin cooperative cancellation. Vex does not claim unsafe arbitrary JVM/native thread termination.

## Ownership laws

- one root `SupervisorJob` isolates sibling executions;
- one named managed Job remains stable across source-recovery attempts;
- nested structured work remains a descendant of that managed Job;
- `forceStop` / `joinStop` suppress restart because cancellation is terminal;
- root shutdown closes admission before cancelling the complete tree;
- work arriving after shutdown fails closed;
- external registry readers cannot mutate retained execution state.

## Boundaries

`VexExecution` owns coroutine execution mechanics. It does **not** replace:

- Behavior meaning;
- Relay convergence;
- Connection/provider ownership;
- repository dispatcher confinement (`withContext`, provider serialization, etc.);
- Compose presentation-local `LaunchedEffect` / lifecycle observation;
- WorkManager or other process-death durability;
- business retry/idempotency.

Use Relay when several changing truths must converge. Execution work is always admitted under a semantic RelationshipId and explicit Approach; ordinary feature code does not derive coordination from callable identity. Use `launchForCollect` only in an execution/runtime bridge that truly owns a sustained Flow observation.

These finite-launch relationships govern coroutine custody only. They do not decide whether a
Control is lawful, what semantic State should be admitted, whether an asynchronous result is still
semantically current, or whether provider truth is fresh.

## Why the test fixture exists

The test fixture follows the same rule as the Journey infrastructure: feature tests should declare the
meaningful scenario, while shared infrastructure owns mechanics that would otherwise be copied and
forgotten.

```text
TRADITIONAL TEST SETUP
    runTest
    + TestCoroutineScheduler
    + StandardTestDispatcher
    + VexExecutionRoot
    + try/finally
    + cancel/join cleanup

REPEATED RISK
    one test forgets cleanup
    -> collector/restart Job survives
    -> later test fails for the wrong reason

VEX TEST FIXTURE
    VexExecutionTestRule
    -> fresh scheduler/root before every test
    -> cancellation-first teardown after every test
    -> complete join before the next test begins
```

Feature construction intentionally remains in the concrete test's `@Before`. Hiding repository,
provider/fake, and real `VexExecution` construction behind a generic feature builder would shorten
the file while making the architecture harder for a new human or AI to perceive.

### Virtual-time decision map

Use the smallest scheduler action that matches the claim:

```text
runCurrent()
    run everything ready at the CURRENT virtual timestamp
    do not move time
    use this to inspect an intermediate concurrency boundary

advanceTimeBy(n)
    move virtual time by n
    use this only when passage of time is itself part of the contract
    (restart backoff, timeout, delayed cleanup, etc.)

advanceUntilIdle()
    keep advancing/running until no work is scheduled
    useful for "finish whatever can finish"
    avoid it when the test needs to inspect an in-between race state
```

This avoids the traditional flaky pattern of advancing arbitrary real/fake milliseconds merely to
"give the coroutine time to run." A race test instead follows:

```text
CAUSE
-> runCurrent()
-> OBSERVE

RELEASE EXTERNAL GATE / CHANGE INPUT
-> runCurrent()
-> OBSERVE

ADVANCE TIME
only when time itself is the behavior under test
```

Teardown is deliberately different from scenario progression:

```text
WRONG TEARDOWN
    advanceUntilIdle() on a still-active tree
    -> may let leaked/restarting work continue until it happens to settle

VEX TEARDOWN
    close/cancel first
    -> advance only cancelled cleanup
    -> join complete tree
```

## Deterministic feature-test pattern

The execution module exposes reusable **test fixtures** for downstream feature tests. The fixture
owns only the universal mechanics that would otherwise be repeated in every class:

```text
fresh TestCoroutineScheduler per test
-> StandardTestDispatcher
-> VexExecutionRoot
-> scenario executes
-> cancel execution tree
-> advance cancelled cleanup
-> join tree before the next test
```

Typical feature-test shape:

```kotlin
@get:Rule
val vexExecution =
    VexExecutionTestRule()

private lateinit var repository:
    InMemoryThingRepository

private lateinit var execution:
    ThingExecution

@Before
fun setUp() {
    repository =
        InMemoryThingRepository()

    execution =
        ThingExecution(
            root = vexExecution.root,
            repository = repository,
        )
}

@Test
fun example() =
    vexExecution.runTest {
        execution.doThing()
        runCurrent()

        // scenario-specific assertions
    }
```

The split is intentional:

- the reusable rule owns scheduler/root creation and guaranteed teardown;
- the feature's `@Before` visibly owns how the real class-under-test is assembled;
- individual tests own only scenario arrangement, causes, scheduling boundaries, and assertions;
- feature-specific `@After` is needed only for resources that intentionally live outside the
  VexExecution tree.

Teardown never advances an active tree "until idle." It first closes/cancels the Vex execution root,
then advances the already-cancelled virtual scheduler so structured cleanup can finish, and finally
joins the tree. This prevents one test's collector/restart job from leaking into the next test while
also avoiding a cleanup strategy that could hide the leak by simply letting it keep running.

Canonical consumer-style example:

```text
src/test/.../execution/usage/VexExecutionVirtualTimeUsageTest.kt
```

It deliberately builds its repository and real `VexExecution` in `@Before`, then proves:
- two independent `launchForCollect` calls begin as concurrent sibling Jobs;
- bounded restart delay can be advanced without real sleeping;
- `joinStop` remains incomplete until structured cleanup really finishes.

Do not extract feature-specific scenario builders merely to shorten tests. Promote a feature-owned
fixture/fake only when repeated data/provider setup becomes a real burden and the extracted helper
still makes the underlying architecture obvious.

## Proof

The focused `VexExecutionTest` source now protects both inherited behavior and the new identity
relationships.

Current execution proofs present in source:

~~~text
same callable + different RelationshipIds
    both Jobs admitted
    stopping one relationship leaves the other running

same RelationshipId + Independent
    multiple concurrent Jobs admitted

same RelationshipId + Earliest
    later duplicate admission adds no Job

same RelationshipId + Latest
    newest waits through predecessor teardown

same RelationshipId + Queued
    every admitted Job executes in arrival order

launchForCollect
    same relationship -> one active observation
    different relationships -> same source may be observed independently
~~~

Family's focused proof separately establishes that Control name is always included, arbitrary Control
payload is not identity, typed ResourceId narrows identity when provided, and two live Family instances
remain distinct.

The source also keeps sibling isolation, bounded source recovery, structured cancellation/teardown,
registry defensiveness, typed callable invocation, and post-shutdown admission rejection.

Exact-head repository-owned `vexVerifyJvm` proof passed for the typed Control/RelationshipId
candidate in PR #193, and that exact candidate tree is now merged on main. GitHub-hosted workflow/status
evidence was not produced for that head, so this remains a local/repository-owned proof claim rather
than a hosted-CI claim. Device/Journey/ScreenCrawler and lived-product acceptance are separate.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->


## Operation/result callback ownership

For `VexExecution.begin(...)`, the suspend operation is the operation boundary:

~~~text
beforeOperation callback
-> operation(argument)
   throws -> onFailure(operation failure)

operation returns Result
-> onResult(Result)

onResult throws
-> execution FAILED
-> does NOT call onFailure
~~~

This distinction matters for Family result admission. A Store/provider operation that returned
successfully must not be rewritten as provider failure merely because later semantic result handling
failed.

```text
OPERATION FAILURE
!= RESULT-ADMISSION FAILURE
```

<!-- [VXG RealForever][VEXEXECUTION][OPERATION-RESULT-OWNERSHIP] -->
