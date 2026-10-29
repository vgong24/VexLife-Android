@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.execution

import java.util.concurrent.CancellationException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
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
 * RelationshipId
 *     semantic identity supplied by the rightful owner; Execution only coordinates it
 *
 * Approach
 *     how overlapping executions sharing that RelationshipId interact
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
     * Explicit opt-in for foundation code that bridges a rightful semantic relationship into the
     * execution engine.
     *
     * Product/feature code should not implement RelationshipId directly. Direct Family owns the
     * typed Control -> ResourceId -> Family RelationshipId boundary.
     */
    @RequiresOptIn(
        message =
            "Foundation relationship bridge only; use the rightful semantic owner's typed relationship boundary",
        level =
            RequiresOptIn.Level.ERROR,
    )
    @Retention(
        AnnotationRetention.BINARY,
    )
    @Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
    )
    annotation class RelationshipFoundationApi

    /**
     * Maintained semantic execution identity supplied by the rightful semantic owner.
     *
     * Execution coordinates this identity; it does not invent trigger strings, argument tuples,
     * or a second semantic-ID vocabulary.
     */
    @RelationshipFoundationApi
    interface RelationshipId

    private data class LegacyRelationshipId(
        val owner:
            KClass<*>,
        val sourceRef:
            SourceRef,
    ) :
        RelationshipId

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

    /**
     * Preserved fail-closed finite launch contract.
     *
     * A second unfinished invocation from the same source is rejected. Use [launch] only when each
     * invocation is intentionally independent.
     */
    protected fun <Result> launchOnce(
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launchOnce(
            sourceOwner =
                this,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchOnce(
        action: KSuspendFunction1<Argument, Result>,
        argument: Argument,
    ): Handle =
        executionRoot.coroutineLauncher.launchOnce(
            sourceOwner =
                this,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchOnce(
        action: KSuspendFunction2<First, Second, Result>,
        first: First,
        second: Second,
    ): Handle =
        executionRoot.coroutineLauncher.launchOnce(
            sourceOwner =
                this,
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

    protected fun <Result> launch(
        action: KSuspendFunction0<Result>,
    ): Handle =
        executionRoot.coroutineLauncher.launch(
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
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
            sourceOwner =
                this,
            sourceRef =
                sourceRef(
                    reference,
                ),
            reason = reason,
        )

    companion object {
        fun <Argument, Result> begin(
            executionRoot: VexExecutionRoot,
            owner: KClass<*>,
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginEarliest(
            executionRoot: VexExecutionRoot,
            owner:
                KClass<*>,
            relationshipId:
                RelationshipId,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchEarliest(
                relationship =
                    relationshipId,
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginLatest(
            executionRoot: VexExecutionRoot,
            owner:
                KClass<*>,
            relationshipId:
                RelationshipId,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchLatest(
                relationship =
                    relationshipId,
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginQueued(
            executionRoot: VexExecutionRoot,
            owner:
                KClass<*>,
            relationshipId:
                RelationshipId,
            operation: KSuspendFunction1<Argument, Result>,
            argument: Argument,
            beforeOperation: () -> Unit = {},
            onResult: (Result) -> Unit = {},
            onFailure: (Throwable) -> Unit = {},
        ): Handle =
            executionRoot.coroutineLauncher.launchQueued(
                relationship =
                    relationshipId,
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

        @Deprecated(
            "Use beginLatest with an explicit VexExecution.RelationshipId",
        )
        fun <Argument, Result> beginReplaceable(
            executionRoot: VexExecutionRoot,
            owner: KClass<*>,
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
                    LegacyRelationshipId(
                        owner =
                            owner,
                        sourceRef =
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
            "Use beginQueued with an explicit VexExecution.RelationshipId",
        )
        fun <Argument, Result> beginSequenced(
            executionRoot: VexExecutionRoot,
            owner: KClass<*>,
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
                    LegacyRelationshipId(
                        owner =
                            owner,
                        sourceRef =
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
            beforeOperation()

            val result =
                try {
                    operation(
                        argument,
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
                    return
                }

            onResult(
                result,
            )
        }

        private fun sourceRef(
            owner: KClass<*>,
            reference: KCallable<*>,
        ):
            SourceRef {
            val ownerName =
                owner.qualifiedName
                    ?: owner.simpleName
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
