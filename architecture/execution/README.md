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
    -> private LinkedHashMap<VexCoroutineRef, Job>

FeatureExecution : VexExecution
    -> launchOnce(callable)
    -> launchForCollect(flowProvider) { value -> ... }
    -> forceStop(callable)
    -> joinStop(callable)
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
    -> launchOnce(operationReference)

RESULT
    execution topology follows meaning rather than incidental call count
```

The architecture therefore removes repeated coroutine-lifetime decisions while leaving domain meaning
visible. Behavior, Relay, Connection/provider, repository, Compose, and WorkManager keep their own
rightful responsibilities.

## Public authoring language

### Draft finite-launch relationship — source conformance pending

The current source still exposes `launchOnce`, `launchReplaceable`, and `launchSequenced`.
Do **not** infer the final execution relationship from those names. This draft has now established the
intended human-facing meanings below; implementation and tests still need to be brought into
conformance.

```text
launch(...)
    every invocation is an independent execution instance
    the same callable may have many concurrent instances

launchEarliest(...)
    the earliest active execution for one maintained lane wins
    overlapping later launches add no new execution while that one remains active

launchLatest(...)
    the latest launch supersedes the active execution for one maintained lane
    predecessor cancellation/teardown completes before the latest begins

launchQueued(...)
    every launch for one maintained lane remains meaningful
    launches execute in arrival order
```

The four shapes are deliberately different:

```text
launch

A -------->
  B -------->
    C -------->


launchEarliest

A -------->
  B x
    C x


launchLatest

A ----x
     B ----x
          C -------->


launchQueued

A ----->
       B ----->
              C ----->
```

`Earliest` is not an error/rejection policy. It means the already-active earliest launch is
sufficient for that maintained relationship, so a colliding later request does not need a duplicate
execution. `Queued` is different: the later request still matters and must run after its
predecessor.

The ordinary `launch(...)` form has **no maintained sibling relationship**. Calling the same
operation ten times may produce ten concurrent execution instances. Extra relationship language is
used only when extra coordination is actually intended.

The current implementation has not yet been renamed or reshaped to this contract. Until that source
conformance is completed, treat the older finite-launch method names as provisional implementation
vocabulary rather than architectural authority.

A maintained Flow observation may reference either a Flow-returning function or Flow property:

```kotlin
launchForCollect(repository::states) { state ->
    accept(state)
}
```

```kotlin
launchForCollect(session::states) { state ->
    accept(state)
}
```

The callable is both the source reference and the thing Vex invokes. A callable is never passed only
as a decorative label while unrelated work runs in a second lambda.

`launchOnce` never automatically retries. `launchForCollect` may use the root's bounded recovery only
for source/subscription failure. A failure thrown while consuming a valid emitted value becomes
`FAILED` with no automatic resubscription. Intentional cancellation never restarts.

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

`launchOnce(::playForExecution, request)` owns the whole operation. The root dispatcher owns the
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

## Source-derived execution identity

The runtime ref is derived from:

```text
VexExecution source class
+
callable.name
```

For example:

```text
VexStreamExecution.refreshCatalog
```

This is a source-derived runtime address, not a durable serialized product identity. Renaming source
may rename the execution ref. That is deliberate: this foundation avoids a second hand-maintained ID
catalog merely to address coroutine work.

The callable-derived ref is **source / operation identity**, not sufficient execution-instance
identity and not automatically a maintained-lane identity.

The intended distinction is:

```text
source identity
    what operation/source is this?

execution instance identity
    which particular launch is this?

maintained lane identity
    only when Earliest / Latest / Queued explicitly relate repeated launches
```

Therefore:

```text
SAME CALLABLE
!= SAME EXECUTION INSTANCE
!= AUTOMATICALLY SAME MAINTAINED LANE
```

An ordinary `launch(...)` needs a distinct execution instance for every invocation even when the
callable is the same. Earliest / Latest / Queued need an explicit stable lane relationship against
which overlapping launches are compared.

The current launcher still keys active custody by one callable-derived `VexCoroutineRef` and
therefore does not yet satisfy this intended distinction. That implementation gap is part of the
current draft refinement; do not preserve duplicate rejection merely because it is present today.

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
## Control

```text
forceStop(callable)
    request cancellation of the exact registered run
    return without waiting for teardown

joinStop(callable)
    request cancellation
    wait until that Job reaches terminal state
```

`joinStop` is the replacement/restart-safe control when the next operation must not begin until the
old contained work is actually gone.

Cancellation remains Kotlin cooperative cancellation. Vex does not claim unsafe arbitrary JVM/native
thread termination.

## Custody and sequence context

Active runs are retained privately in insertion order:

```text
LinkedHashMap<VexCoroutineRef, ManagedCoroutine>
```

This currently provides exact lookup and deterministic active-run order, but the single-ref map also
reveals the draft mismatch above: one callable-derived ref cannot represent several independent
concurrent instances of the same source operation. The conformed design must preserve exact
per-instance handle custody while giving maintained Earliest / Latest / Queued lanes their own
explicit relationship identity. Terminal state must remain defensively observable without allowing
an older execution/lane generation to overwrite newer lane truth.

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

Use Relay when several changing truths must converge. For finite work, choose the smallest relationship
that matches the actual intention: independent `launch`, maintained `launchEarliest`,
`launchLatest`, or `launchQueued`. Use `launchForCollect` only in an execution/runtime bridge
that truly owns a sustained Flow observation.

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

The existing `VexExecutionTest` still protects inherited execution behavior such as typed callable
invocation, Flow-owned collection, sibling isolation, bounded source recovery, cancellation/teardown,
registry defensiveness, and post-shutdown admission rejection.

The new finite-launch relationship is **not yet source-proved**. Its conformance proof must explicitly
cover at least:

```text
launch
    same callable may have several concurrent execution instances

launchEarliest
    first active lane execution remains
    later overlapping launches do not create duplicate work

launchLatest
    predecessor is cancelled
    predecessor teardown completes before latest begins

launchQueued
    every admitted launch runs in arrival order

lane generations
    predecessor terminal publication cannot overwrite newer maintained-lane truth
```

Until those tests exist at the exact current head, do not claim the finite-launch contract as
implementation-complete.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
