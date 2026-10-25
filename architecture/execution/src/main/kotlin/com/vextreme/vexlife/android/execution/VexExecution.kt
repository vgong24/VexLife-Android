package com.vextreme.vexlife.android.execution

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty0
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Mandatory meta-pattern for Vex-owned coroutine execution.
 *
 * WHY THIS EXISTS:
 * Kotlin's raw launch/collect APIs correctly expose general coroutine freedom, but application code
 * then has to repeatedly decide ownership, containment, restart, keyed cancellation, and whether two
 * long-running collectors are actually concurrent. VexExecution centralizes those mechanics so a
 * feature author can focus on the semantic operation while the execution topology remains inspectable.
 *
 * A class that needs managed coroutine capability must make that architectural role explicit by
 * extending [VexExecution]. Ordinary UI, Experience, Behavior, repository, and domain classes do
 * not receive [VexCoroutineLauncher]; that implementation remains internal to this module.
 *
 * Callable references are executable source references, not decorative labels:
 *
 * launchOnce(repository::refresh)
 *   -> derives the runtime ref from this Execution + "refresh"
 *   -> invokes refresh exactly once
 *
 * launchForCollect(repository::states) { state -> ... }
 *   -> derives the runtime ref from this Execution + "states"
 *   -> invokes states()
 *   -> owns the resulting Flow collection
 *
 * Execution-shape rule:
 * - [launchOnce] is for one finite suspend operation;
 * - each [launchForCollect] call owns exactly one Flow collector and therefore one managed Job;
 * - several independent Flow sources use several launchForCollect calls, producing sibling Jobs
 *   that collect concurrently;
 * - do not hide raw Flow.collect or raw coroutine launch inside launchOnce to recreate execution
 *   topology outside this boundary.
 */
abstract class VexExecution protected constructor(
    private val executionRoot: VexExecutionRoot,
) {
    private val executionName: String
        get() =
            this::class.qualifiedName
                ?: this::class.simpleName
                ?: error(
                    "VexExecution requires a named source class",
                )

    /**
     * Runs one already-defined finite suspend operation under Vex execution custody.
     *
     * If one human/product intention requires several ordered suspend calls, compose those calls
     * inside the referenced suspend function and launch that operation once. Do not use launchOnce
     * as a container for sustained Flow collection.
     */
    protected fun <Result> launchOnce(
        action: KSuspendFunction0<Result>,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchOnce(
            ref = ref(action),
        ) {
            action()
        }

    protected fun <Argument, Result> launchOnce(
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchOnce(
            ref = ref(action),
        ) {
            action(argument)
        }

    protected fun <First, Second, Result> launchOnce(
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchOnce(
            ref = ref(action),
        ) {
            action(
                first,
                second,
            )
        }

    /**
     * Starts one maintained Flow observation.
     *
     * One invocation means:
     *
     * VexExecution
     * -> one registered managed Job
     * -> invoke [source]
     * -> collect that one Flow
     * -> deliver each value to [onEach]
     *
     * To observe several independent Flows concurrently, call launchForCollect once per source.
     * The calls become independently addressable sibling Jobs; no second never-ending collect is
     * placed sequentially behind the first.
     *
     * Recovery applies only to obtaining/subscribing to the source. If onEach throws while handling
     * a valid emitted value, the execution becomes FAILED with no automatic resubscription. Feature
     * and provider effects keep ownership of failure meaning and lawful retry.
     */
    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source: KFunction0<Source>,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchForCollect(
            ref = ref(source),
            restartOnSourceFailure = restartOnSourceFailure,
            source = source,
            onEach = onEach,
        )

    /** Property-reference form of the same one-source / one-managed-Job collection contract. */
    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source: KProperty0<Source>,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchForCollect(
            ref = ref(source),
            restartOnSourceFailure = restartOnSourceFailure,
            source = source::get,
            onEach = onEach,
        )

    /** Argument-bearing form of the same one-source / one-managed-Job collection contract. */
    protected fun <Argument, Value, Source : Flow<Value>> launchForCollect(
        source: KFunction1<Argument, Source>,
        argument: Argument,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchForCollect(
            ref = ref(source),
            restartOnSourceFailure = restartOnSourceFailure,
            source = {
                source(argument)
            },
            onEach = onEach,
        )

    /**
     * Temporarily hands the current managed execution to a caller-owned dispatcher.
     *
     * This is a structured context switch inside the same VexCoroutineHandle, not a second launch
     * surface and not a new execution identity. Use it when expensive preparation belongs on the
     * root's worker dispatcher but the final provider commit has an exact thread/looper affinity.
     *
     * The caller remains responsible for choosing the rightful dispatcher. VexExecution does not
     * assume that every "foreground" effect belongs to Dispatchers.Main.
     */
    protected suspend fun <Result> handoff(
        dispatcher: CoroutineDispatcher,
        action: suspend () -> Result,
    ): Result =
        withContext(
            dispatcher,
        ) {
            action()
        }

    protected fun forceStop(
        reference: KCallable<*>,
        reason: String = "force stopped",
    ): Boolean =
        executionRoot.coroutineLauncher.forceStop(
            ref = ref(reference),
            reason = reason,
        )

    protected suspend fun joinStop(
        reference: KCallable<*>,
        reason: String = "join stopped",
    ): Boolean =
        executionRoot.coroutineLauncher.joinStop(
            ref = ref(reference),
            reason = reason,
        )

    private fun ref(
        reference: KCallable<*>,
    ): VexCoroutineRef =
        VexCoroutineRef(
            "$executionName.${reference.name}",
        )
}

// [VXG RealForever]
