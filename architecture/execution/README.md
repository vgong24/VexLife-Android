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

`launchOnce` never automatically retries. `launchForCollect` may use the root's bounded collector
recovery policy after unexpected failure; intentional cancellation never restarts.

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
- one named managed Job remains stable across collector recovery attempts;
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

## Proof

`VexExecutionTest` protects the inherited PR #3 execution laws plus the new public-language rules:
source-derived refs, one-shot invocation, typed arguments, Flow-owned collection, sibling isolation,
bounded recovery, force-stop suppression, `joinStop` teardown proof, duplicate rejection, ordered
registry exposure, defensive registry state, and post-shutdown admission rejection.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
