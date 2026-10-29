package com.vextreme.vexlife.android.execution

import java.util.concurrent.CancellationException
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty0
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Root contract for Vex-owned coroutine execution.
 *
 * Execution meaning is split into distinct facts:
 *
 * SourceRef
 *     automatically derived from source code for diagnostics/navigation
 *
 * InstanceId
 *     automatically generated for one exact admitted execution
 *
 * Relationship
 *     the actual maintained-map coordinate: live owner + semantic trigger + ordered arguments
 *
 * Approach
 *     how overlapping executions in that Relationship interact
 */
abstract class VexExecution protected constructor(
    private val executionRoot: VexExecutionRoot,
) {
    /** Diagnostic source identity derived automatically from the source callable. */
    @JvmInline
    value class SourceRef internal constructor(
        val value: String,
    ) {
        init {
            require(value.isNotBlank()) {
                "VexExecution.SourceRef must be non-blank"
            }
        }

        override fun toString(): String = value
    }

    /** Exact runtime identity of one admitted execution instance. */
    @JvmInline
    value class InstanceId internal constructor(
        val value: Long,
    ) {
        init {
            require(value > 0L) {
                "VexExecution.InstanceId must be positive"
            }
        }

        override fun toString(): String = value.toString()
    }

    /**
     * Exact maintained execution relationship.
     *
     * Owner uses referential identity. Trigger and arguments use ordinary value equality.
     * Arguments are copied at construction so the relationship tuple itself cannot be reordered.
     */
    class Relationship internal constructor(
        internal val owner:
            Any,
        val trigger:
            Any,
        arguments:
            List<Any?>,
    ) {
        val arguments:
            List<Any?> =
            arguments.toList()

        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                Relationship &&
                owner ===
                other.owner &&
                trigger ==
                other.trigger &&
                arguments ==
                other.arguments

        override fun hashCode():
            Int {
            var result =
                System.identityHashCode(
                    owner,
                )
            result =
                31 *
                    result +
                    trigger.hashCode()
            result =
                31 *
                    result +
                    arguments.hashCode()
            return result
        }

        override fun toString():
            String =
            buildString {
                append(
                    trigger,
                )
                if (
                    arguments.isNotEmpty()
                ) {
                    append(
                        arguments,
                    )
                }
            }
    }

    enum class Approach {
        Independent,
        Earliest,
        Latest,
        Queued,
    }

    enum class Status {
        STARTING,
        RUNNING,
        RESTARTING,
        COMPLETED,
        CANCELLED,
        FAILED,
    }

    data class State(
        val instanceId: InstanceId,
        val sourceRef: SourceRef,
        val status: Status,
        val attempt: Int,
        val restartCount: Int,
        val failureClass: String? = null,
        val failureMessage: String? = null,
    )

    interface Handle {
        val instanceId: InstanceId
        val sourceRef: SourceRef
        val state: StateFlow<State>

        @Deprecated(
            "Use sourceRef; ref was source identity, not execution-instance identity",
        )
        val ref: SourceRef
            get() = sourceRef

        fun forceStop(
            reason: String = "force stopped",
        ): Boolean

        suspend fun joinStop(
            reason: String = "join stopped",
        ): Boolean

        suspend fun awaitCompletion()
    }

    private val executionName: String
        get() =
            this::class.qualifiedName
                ?: this::class.simpleName
                ?: error(
                    "VexExecution requires a named source class",
                )

    @Deprecated(
        "Use launch; every invocation is independently admitted",
    )
    protected fun <Result> launchOnce(
        action: KSuspendFunction0<Result>,
    ): Handle =
        launch(
            action,
        )

    @Deprecated(
        "Use launch; every invocation is independently admitted",
    )
    protected fun <Argument, Result> launchOnce(
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        launch(
            action,
            argument,
        )

    @Deprecated(
        "Use launch; every invocation is independently admitted",
    )
    protected fun <First, Second, Result> launchOnce(
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        launch(
            action,
            first,
            second,
        )

    protected fun <Result> launch(
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launch(
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launch(
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        executionRoot.coroutineLauncher.launch(
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launch(
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        executionRoot.coroutineLauncher.launch(
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    protected fun <Result> launchEarliest(
        trigger: Any,
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launchEarliest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchEarliest(
        trigger: Any,
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        executionRoot.coroutineLauncher.launchEarliest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchEarliest(
        trigger: Any,
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        executionRoot.coroutineLauncher.launchEarliest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    protected fun <Result> launchLatest(
        trigger: Any,
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launchLatest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchLatest(
        trigger: Any,
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        executionRoot.coroutineLauncher.launchLatest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchLatest(
        trigger: Any,
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        executionRoot.coroutineLauncher.launchLatest(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    protected fun <Result> launchQueued(
        trigger: Any,
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launchQueued(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchQueued(
        trigger: Any,
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        executionRoot.coroutineLauncher.launchQueued(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchQueued(
        trigger: Any,
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        executionRoot.coroutineLauncher.launchQueued(
            relationship =
                relationship(
                    owner =
                        this,
                    trigger =
                        trigger,
                ),
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    /**
     * Maintained one-per-source Flow observation.
     *
     * This is deliberately separate from finite launch relationships. A second active observation of
     * the same source fails closed rather than inheriting ordinary launch's independent-instance law.
     */
    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source: KFunction0<Source>,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): Handle =
        executionRoot.coroutineLauncher.launchForCollect(
            sourceRef =
                sourceRef(
                    source,
                ),
            restartOnSourceFailure =
                restartOnSourceFailure,
            source = source,
            onEach = onEach,
        )

    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source: KProperty0<Source>,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): Handle =
        executionRoot.coroutineLauncher.launchForCollect(
            sourceRef =
                sourceRef(
                    source,
                ),
            restartOnSourceFailure =
                restartOnSourceFailure,
            source = source::get,
            onEach = onEach,
        )

    protected fun <Argument, Value, Source : Flow<Value>> launchForCollect(
        source: KFunction1<Argument, Source>,
        argument: Argument,
        restartOnSourceFailure: Boolean = true,
        onEach: suspend (Value) -> Unit,
    ): Handle =
        executionRoot.coroutineLauncher.launchForCollect(
            sourceRef =
                sourceRef(
                    source,
                ),
            restartOnSourceFailure =
                restartOnSourceFailure,
            source = {
                source(
                    argument,
                )
            },
            onEach = onEach,
        )

    protected suspend fun <Result> handoff(
        dispatcher: CoroutineDispatcher,
        action: suspend () -> Result,
    ): Result =
        withContext(
            dispatcher,
        ) {
            action()
        }

    /** Stops every currently active execution instance sourced from [reference]. */
    protected fun forceStop(
        reference: KCallable<*>,
        reason: String = "force stopped",
    ): Boolean =
        executionRoot.coroutineLauncher.forceStopSource(
            sourceRef =
                sourceRef(
                    reference,
                ),
            reason = reason,
        )

    /** Stops and joins every currently active execution instance sourced from [reference]. */
    protected suspend fun joinStop(
        reference: KCallable<*>,
        reason: String = "join stopped",
    ): Boolean =
        executionRoot.coroutineLauncher.joinStopSource(
            sourceRef =
                sourceRef(
                    reference,
                ),
            reason = reason,
        )

    companion object {
        fun relationship(
            owner:
                Any,
            trigger:
                Any,
            arguments:
                List<Any?> =
                emptyList(),
        ):
            Relationship =
            Relationship(
                owner =
                    owner,
                trigger =
                    trigger,
                arguments =
                    arguments,
            )

        fun <Argument, Result> begin(
            executionRoot: VexExecutionRoot,
            owner: Any,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launch(
                sourceRef =
                    sourceRef(
                        owner,
                        operation,
                    ),
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }

        fun <Argument, Result> beginEarliest(
            executionRoot: VexExecutionRoot,
            relationship:
                Relationship,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchEarliest(
                relationship =
                    relationship,
                sourceRef =
                    sourceRef(
                        relationship.owner,
                        operation,
                    ),
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }

        fun <Argument, Result> beginLatest(
            executionRoot: VexExecutionRoot,
            relationship:
                Relationship,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchLatest(
                relationship =
                    relationship,
                sourceRef =
                    sourceRef(
                        relationship.owner,
                        operation,
                    ),
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }

        fun <Argument, Result> beginQueued(
            executionRoot: VexExecutionRoot,
            relationship:
                Relationship,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchQueued(
                relationship =
                    relationship,
                sourceRef =
                    sourceRef(
                        relationship.owner,
                        operation,
                    ),
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }

        @Deprecated(
            "Use beginLatest with an explicit VexExecution.Relationship",
        )
        fun <Argument, Result> beginReplaceable(
            executionRoot: VexExecutionRoot,
            owner: Any,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle {
            val sourceRef =
                sourceRef(
                    owner,
                    operation,
                )

            return executionRoot.coroutineLauncher.launchLatest(
                relationship =
                    relationship(
                        owner =
                            owner,
                        trigger =
                            sourceRef,
                    ),
                sourceRef =
                    sourceRef,
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }
        }

        @Deprecated(
            "Use beginQueued with an explicit VexExecution.Relationship",
        )
        fun <Argument, Result> beginSequenced(
            executionRoot: VexExecutionRoot,
            owner: Any,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle {
            val sourceRef =
                sourceRef(
                    owner,
                    operation,
                )

            return executionRoot.coroutineLauncher.launchQueued(
                relationship =
                    relationship(
                        owner =
                            owner,
                        trigger =
                            sourceRef,
                    ),
                sourceRef =
                    sourceRef,
            ) {
                executeOperation(
                    operation =
                        operation,
                    argument =
                        argument,
                    beforeOperation =
                        beforeOperation,
                    onResult =
                        onResult,
                    onFailure =
                        onFailure,
                )
            }
        }

        private suspend fun <Argument, Result> executeOperation(
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit,
            onResult: (Result) -> Unit,
            onFailure: (Throwable) -> Unit,
        ) {
            try {
                beforeOperation()
                onResult(
                    operation(
                        argument,
                    ),
                )
            } catch (
                cancelled:
                    CancellationException,
            ) {
                throw cancelled
            } catch (
                failure:
                    Throwable,
            ) {
                onFailure(
                    failure,
                )
            }
        }

        private fun sourceRef(
            owner: Any,
            reference: KCallable<*>,
        ):
            SourceRef {
            val ownerClass =
                owner::class

            val ownerName =
                ownerClass.qualifiedName
                    ?: ownerClass.simpleName
                    ?: error(
                        "VexExecution begin owner requires a named source class",
                    )

            return SourceRef(
                ownerName +
                    "." +
                    reference.name,
            )
        }
    }

    private fun sourceRef(
        reference: KCallable<*>,
    ):
        SourceRef =
        SourceRef(
            "$executionName.${reference.name}",
        )
}

// [VXG RealForever]
