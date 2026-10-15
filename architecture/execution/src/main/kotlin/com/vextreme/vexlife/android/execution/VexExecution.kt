package com.vextreme.vexlife.android.execution

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlinx.coroutines.flow.Flow

/**
 * Mandatory meta-pattern for Vex-owned coroutine execution.
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

    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source: KFunction0<Source>,
        shouldRestart: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchForCollect(
            ref = ref(source),
            shouldRestart = shouldRestart,
            source = source,
            onEach = onEach,
        )

    protected fun <Argument, Value, Source : Flow<Value>> launchForCollect(
        source: KFunction1<Argument, Source>,
        argument: Argument,
        shouldRestart: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        executionRoot.coroutineLauncher.launchForCollect(
            ref = ref(source),
            shouldRestart = shouldRestart,
            source = {
                source(argument)
            },
            onEach = onEach,
        )

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
