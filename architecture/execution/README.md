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

Finite suspend work:

```kotlin
launchOnce(repository::refreshCatalog)
```

One-argument work stays type-safe:

```kotlin
launchOnce(repository::download, trackRef)
```

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

One active callable name may exist once within one `VexExecution`. Duplicate active refs fail closed.
If two different operations in one Execution share a callable name, give the Execution-owned
operation a distinct semantic source name instead of inventing an invisible runtime suffix.
Concurrent instances of the same parameterized callable are deliberately held until a real consumer
earns an instance-identity rule.

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

This provides exact lookup, duplicate rejection, deterministic active-run order, and surgical
cancellation. Terminal state remains available through the defensive `executions` projection after
the active Job leaves custody.

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

Use Relay when several changing truths must converge. Use `launchOnce` when one already-defined
semantic operation should execute once. Use `launchForCollect` only in an execution/runtime bridge
that truly owns a sustained Flow observation.

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

### Failed execution integrity in tests

Production keeps managed failures as retained `VexCoroutineState.FAILED` so one supervised sibling
does not crash unrelated work. The reusable test fixture closes the other half of that contract:
an unexpected retained `FAILED` state fails the JUnit scenario after execution-tree teardown.

A negative test that intentionally proves failure must acknowledge the exact handle:

```kotlin
expectManagedFailure(
    handle = handle,
    failureClass = "IllegalStateException",
    failureMessage = "expected failure",
)
```

That declaration is evidence expectation only. It never restarts the execution, never clears the
failure, and fails itself if the expected failure does not occur or does not match. If the ordinary
test body also throws, that body failure remains primary and the execution-integrity failure is
attached as suppressed evidence.

```text
unexpected FAILED
    -> test FAIL

expected FAILED + exact match
    -> accepted negative-test evidence

expected FAILED missing/mismatched
    -> test FAIL

body exception + managed FAILED
    -> body exception remains primary
    -> managed failure retained as suppressed evidence
```

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

`VexExecutionTest` protects the inherited PR #3 execution laws plus the new public-language rules:
source-derived refs, one-shot invocation, typed arguments, Flow-owned collection, sibling isolation,
source-only bounded recovery, downstream-consumer failure without auto-restart, force-stop suppression,
`joinStop` teardown proof, duplicate rejection, ordered registry exposure, defensive registry state,
and post-shutdown admission rejection.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
