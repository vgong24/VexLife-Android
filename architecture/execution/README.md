# VexAppScope — Execution Ownership

[VXG RealForever]

## Question

> I started asynchronous work. Who owns its complete lifetime, what is it called, how can I stop it,
> how do I know it failed, and when may it restart?

Use `VexAppScope`.

## Public shape

~~~text
Koin single
-> VexAppScope
     parentJob = SupervisorJob()
     named VexCoroutineRef -> managed Job
     observable execution registry

launchCoroutine(ref)
-> STARTING
-> RUNNING
-> COMPLETED | FAILED | CANCELLED

optional:
FAILED -> RESTARTING -> next attempt

forceStop(ref)
-> CANCELLED
-> never restart
~~~

## The ownership promise

A named execution owns its descendants.

The supplied coroutine block runs inside its managed structured scope. If it launches children,
force-stopping the named execution cancels and joins that descendant tree rather than leaving work
attached directly to the application scope.

~~~text
OWNER CLAIMS EXECUTION
-> OWNER CONTAINS DESCENDANTS
~~~

## Parent supervision

`parentJob = SupervisorJob()` prevents one failed child execution from automatically killing
unrelated siblings or the application execution owner.

Supervision does not imply restart. Restart is an explicit per-execution policy.

## Restart and cancellation

Default:

~~~text
VexCoroutineRestartPolicy.Never
~~~

Opt-in watcher/listener recovery:

~~~kotlin
VexCoroutineRestartPolicy.OnFailure(
    maxRestarts = 3,
    delayMillis = 1_000,
)
~~~

Cancellation wraps the entire retry loop, including restart delay.

Therefore:

~~~text
forceStop during work
or
forceStop during restart delay

-> CANCELLED
-> no relaunch
~~~

Restarting a coroutine mechanism is not semantic retry of an operation.

## Application shutdown

`stopApplication()` closes work admission before cancelling the parent tree.

A concurrent launch therefore has one of two outcomes:

~~~text
admitted before stop fence
-> becomes child
-> shutdown cancels it

arrives after stop fence
-> fails closed
~~~

There is no post-stop STARTING limbo.

## Registry ownership

The internal `LinkedHashMap<VexCoroutineRef, Job>` is private and synchronized.

Public execution state is exposed through a defensive StateFlow projection. Consumers may mutate a
casted copy without mutating VexAppScope's retained registry.

External observer/logger callbacks run after the registry lock is released.

## Observability

VexAppScope emits bounded structured lifecycle events to a `VexLogSink`:

~~~text
coroutine.starting
coroutine.running
coroutine.restarting
coroutine.completed
coroutine.cancelled
coroutine.failed
coroutine.cancel.requested
~~~

The coroutine ref is carried as stable origin identity. VexAppScope does not store BulkData logs.

## Koin boundary

Koin normally retains one app-wide VexAppScope. Koin owns composition/cardinality, not execution
semantics.

~~~text
KOIN_INSTANCE != EXECUTION_MEANING
~~~

## Still held

VexAppScope does not solve:

- asynchronous dependency readiness;
- durable operations across process death;
- semantic idempotency/retry;
- product-state meaning of execution failure;
- persistent execution history.

## Executable proof

Read:

~~~text
src/test/.../VexAppScopeTest.kt
~~~

It covers descendant containment, sibling supervision, bounded restart, force-stop during restart
delay, duplicate refs, post-stop launch rejection, defensive registry exposure, and structured
logging.

See `examples/APP_SCOPE.md` for the smallest usage template.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
