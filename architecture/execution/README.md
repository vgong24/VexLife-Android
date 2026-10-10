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

returned handle.forceStop()
-> CANCELLED
-> never restart
~~~

## The ownership promise

A named execution owns its descendants.

Cancellation capability follows that ownership: `VexExecutionOwner` exposes launch/observation, but
not global `forceStop(ref)`. The exact launcher receives a `VexCoroutineHandle`, and that handle
owns deliberate cancellation for its execution. Stable ref visibility is not cancellation authority.


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

Managed descendants must still cooperate with cancellation. Small `NonCancellable` finalizers are
valid when cleanup genuinely must complete, but they must remain bounded: `join()`,
`stopApplication()`, and Compound ownership release intentionally wait for descendants rather than
pretending work is gone while it is still running.

Therefore:

~~~text
forceStop during work
or
forceStop during restart delay

-> CANCELLED
-> no relaunch
~~~

Restarting a coroutine mechanism is not semantic retry of an operation.

Only ordinary `Exception` failures participate in `OnFailure` restart. Any `Error` bypasses
restart. Nonfatal invariant errors such as `AssertionError` are reflected as terminal `FAILED`;
true JVM-fatal classes (`VirtualMachineError`, `ThreadDeath`, `LinkageError`) bypass diagnostic
recovery and are rethrown from the coroutine exception handler to the platform failure path.

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

Once terminal shutdown has begun, the parent-tree join runs under `NonCancellable`. Cancellation of
the caller cannot turn "application scope stopped" into "cancellation requested but owned descendants
may still be cleaning up." Descendant cleanup itself must therefore remain bounded.

## Registry ownership

The internal `LinkedHashMap<VexCoroutineRef, Job>` is private and synchronized.

Public execution state is exposed through a defensive StateFlow projection. Consumers may mutate a
casted copy without mutating VexAppScope's retained registry.

Because the registry intentionally retains the latest state for each stable ref, VexAppScope also
enforces a configurable `maxRetainedExecutionRefs` ceiling (512 by default). Existing refs can be
reused indefinitely; a stream of new per-item/UUID refs fails closed instead of becoming an
unbounded retained-history leak.

Retained failure/cancellation text is capped at 2048 characters. Restart policy also rejects
`Int.MAX_VALUE` restarts so the final attempt cannot overflow the Int attempt counter.

Diagnostic logging runs after the registry lock is released. Execution state itself is observable
through `executions`; there is no second synchronous callback observer that can stall managed work.

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

Logging is best-effort and cannot own execution behavior. A broken/closed `VexLogSink` must not
prevent launch, cancellation, restart handling, terminal state publication, or application shutdown.

~~~text
LOGGING_FAILURE != EXECUTION_FAILURE
~~~

## Execution-owner extension boundary

`VexExecutionOwner` is sealed to `architecture/execution`. Product/features consume an execution
owner; they do not implement one ad hoc.

A future session/account/feature execution owner may still be added when a real narrower lifetime
earns it, but that implementation belongs in this foundation beside the structured-descendant,
reservation, cancellation, bounded-registry, and test contracts.

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
