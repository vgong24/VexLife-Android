# Vex App Scope / Execution Ownership

[VXG RealForever]

## The question comes first

> I started asynchronous work. What is it called, who owns it, how do I stop it, how do I know it
> failed, and when is it allowed to restart?

That is the job of architecture/execution.

Relay owns current semantic state relationships.
VexAppScope owns application-lifetime coroutine execution mechanics.

## Core shape

~~~text
VexAppScope
  parentJob = SupervisorJob()
  coroutineContext = parentJob + dispatcher + CoroutineName + exception handler

  active jobMap
    VexCoroutineRef -> Job

  execution state registry
    ref
    status
    attempt
    restartCount
    failure
~~~

Koin normally retains one VexAppScope for the application lifetime.

The Android Application does not need to become a CoroutineScope itself. It starts composition;
the singleton VexAppScope contains the application coroutine context.

## Why SupervisorJob is the parent

A child watcher dying must not automatically cancel its parent or unrelated siblings.

~~~text
parentJob = SupervisorJob()

home listener fails
!= item listener cancelled
!= relay observer cancelled
!= app execution scope cancelled
~~~

Failure propagation/restart remains explicit per child execution.

## Stable names

Every managed coroutine requires an explicit VexCoroutineRef.

Examples:

~~~text
relay.event-screen.observe.screen
relay.event-screen.observe.items
home.connection.listener
voice.session.reader
server.local-api.listener
~~~

Do not derive these refs from Kotlin class names, lambda identity, stack traces, or reflection.

~~~text
CODE_SYMBOL != STABLE_EXECUTION_REF
~~~

R8/obfuscation may rename implementation symbols. A VexCoroutineRef is intentionally authored
stable identity and may later be generated from a source-managed registry.

## Launch

~~~kotlin
appScope.launchCoroutine(
    coroutineRef = VexCoroutineRef("home.connection.listener"),
) {
    // owned coroutine work
}
~~~

The default restart policy is Never.

Restart must be deliberately requested:

~~~kotlin
restartPolicy =
    VexCoroutineRestartPolicy.OnFailure(
        maxRestarts = 3,
        delayMillis = 1_000,
    )
~~~

The implementation uses one loop inside the same managed Job. It does not recursively call
launchCoroutine().

## Force stop

~~~kotlin
appScope.forceStop(
    VexCoroutineRef("home.connection.listener"),
)
~~~

Force-stop cancels the existing managed Job.

Cancellation exits through CancellationException, which is never treated as an ordinary failure
and never enters restart logic.

~~~text
FORCE_STOP
-> CANCEL
-> CANCELLED
-> NO_RESTART
~~~

This prevents the classic "I killed it and the exception handler immediately resurrected it" race.

## Registry safety

The active jobMap is a LinkedHashMap so diagnostic ordering remains readable.

Registry access uses one short JVM lock.

A coroutine Mutex is not currently used because launch/stop reservation must be synchronous and
the protected critical sections perform no suspension. If registry mutation later becomes
suspending work, that assumption must be re-evaluated.

Duplicate active refs fail closed.

## Observability / VexLogger seam

VexAppScopeObserver receives execution-state changes and unhandled scope failures.

This is intentionally a small adapter seam. A future VexLogger / bounded BulkDataLogging
implementation can consume these semantic execution events without VexAppScope becoming the logging
owner.

~~~text
COROUTINE_STATE != RAW_LOG_LINE
VEX_APP_SCOPE != LOG_STORAGE_OWNER
~~~

Do not log every scheduler/runtime detail merely because it exists.

## Relay dependency observers

A Compound Relay dependency collector is asynchronous execution.

Therefore Relay now declares the relationship, while VexAppScope owns its coroutine:

~~~text
ScreenVexRelay
   |
   | currentState
   v
named execution:
relay.event-screen.observe.screen
   |
   v
EventScreenVexCompoundRelay partial input
~~~

Relay does not retain anonymous List<Job>.

## Construction is not readiness

A Koin object existing does not prove that an asynchronously initialized SDK/service is usable.

~~~text
KOIN_RESOLVED
!= INITIALIZED
!= READY
~~~

This is the null-race Victor remembers from SDK boot sequencing.

Do not hide startup readiness inside dependency injection or assume constructor completion means an
asynchronous dependency finished booting.

A later foundation should make startup/readiness explicit, likely as a small orchestration/readiness
architecture that can:

~~~text
construct dependencies
-> start named initialization work
-> await declared readiness
-> expose READY / FAILED / HELD
-> only then start dependent execution
~~~

That is intentionally not implemented inside VexAppScope yet. VexAppScope owns execution; startup
orchestration owns dependency ordering/readiness.

## Important semantic boundaries

~~~text
COROUTINE != OPERATION
JOB != DURABLE_IDENTITY
HAS_A_COROUTINE_SCOPE != OWNS_THE_SEMANTIC_OPERATION
RESTART_COROUTINE != RETRY_SEMANTIC_OPERATION
KOIN_RESOLVED != SERVICE_READY
~~~

A long-lived listener may legitimately restart.

A user action with external side effects may require idempotency, result admission, or durable
operation identity instead. VexAppScope must not silently retry it merely because its coroutine
failed.

## Historical lineage

The exact PenSDK-era class name was not recovered from durable source.

Victor recalls VexAppScope as the application singleton name, and the durable VexLife Android
architecture independently preserves the same category:

~~~text
explicit coroutine owner
parent/lifetime
Supervisor/failure-domain isolation
cancellation path
failure route
restart/retry distinction
clean-stop proof
~~~

The current source therefore uses VexAppScope while keeping deeper historical APIs explicitly
unclaimed.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION][VEX-APP-SCOPE] -->
