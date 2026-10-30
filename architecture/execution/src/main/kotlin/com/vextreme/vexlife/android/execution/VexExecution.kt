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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Root contract for Vex-owned coroutine execution.
 *
 * Execution owns Jobs and their lifetime. Callers address meaningful work through a relationship
 * key supplied by the rightful semantic owner:
 *
 * RelationshipId<Owner>
 *     semantic key supplied by the owner that understands the work
 *
 * Approach
 *     how overlapping Jobs for that relationship interact
 *
 * SourceRef
 *     automatically derived source location for diagnostics/navigation only
 *
 * There is deliberately no public Job handle or runtime instance ID. A caller that later needs to
 * stop/join work already knows the semantic relationship it supplied when the work was admitted.
 */
abstract class VexExecution<
    Owner : Any,
> protected constructor(
    private val executionRoot:
        VexExecutionRoot,
) {
    /** Diagnostic source identity. Never use this as execution custody or semantic identity. */
    @JvmInline
    value class SourceRef internal constructor(
        val value:
            String,
    ) {
        init {
            require(
                value.isNotBlank(),
            ) {
                "VexExecution.SourceRef must be non-blank"
            }
        }

        override fun toString():
            String =
            value
    }

    /**
     * Explicit opt-in for code that supplies a rightful semantic relationship to Execution.
     *
     * The generic [Owner] binding is part of the contract: a relationship key belonging to one
     * semantic owner cannot be passed to an Execution surface bound to another owner.
     */
    @RequiresOptIn(
        message =
            "Execution relationship foundation only; use the rightful semantic owner's typed relationship boundary",
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
     * Semantic execution key supplied by the rightful owner.
     *
     * Execution coordinates the key; it does not derive semantic identity from a callable, an
     * argument tuple, a Job object, or a generated runtime occurrence ID.
     */
    @RelationshipFoundationApi
    interface RelationshipId<
        Owner : Any,
    >

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

    /**
     * Read-only execution observation.
     *
     * Relationship identity is the map key exposed by [VexExecutionRoot.executions]. [sourceRef]
     * remains diagnostic only.
     */
    data class State(
        val sourceRef:
            SourceRef,
        val status:
            Status,
        val attempt:
            Int,
        val restartCount:
            Int,
        val failureClass:
            String? =
            null,
        val failureMessage:
            String? =
            null,
    )

    private val executionName:
        String
        get() =
            this::class.qualifiedName
                ?: this::class.simpleName
                ?: error(
                    "VexExecution requires a named source class",
                )

    /**
     * Admit finite work under one owner-supplied relationship.
     *
     * [approach] is the only overlap policy. Source/callable identity never decides whether work is a
     * duplicate.
     */
    protected fun <Result> launch(
        relationshipId:
            RelationshipId<Owner>,
        approach:
            Approach,
        action:
            KSuspendFunction0<Result>,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launch(
            relationship =
                relationshipId,
            approach =
                approach,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launch(
        relationshipId:
            RelationshipId<Owner>,
        approach:
            Approach,
        action:
            KSuspendFunction1<Argument, Result>,
        argument:
            Argument,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launch(
            relationship =
                relationshipId,
            approach =
                approach,
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
        relationshipId:
            RelationshipId<Owner>,
        approach:
            Approach,
        action:
            KSuspendFunction2<First, Second, Result>,
        first:
            First,
        second:
            Second,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launch(
            relationship =
                relationshipId,
            approach =
                approach,
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
     * Maintain one long-lived Flow observation for one semantic relationship.
     *
     * A duplicate call for the same relationship uses Earliest semantics: the already-running
     * observation remains authoritative and no second Job is admitted. Different rightful
     * relationship keys may observe the same source independently.
     */
    protected fun <Value, Source : Flow<Value>> launchForCollect(
        relationshipId:
            RelationshipId<Owner>,
        source:
            KFunction0<Source>,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launchForCollect(
            relationship =
                relationshipId,
            sourceRef =
                sourceRef(
                    source,
                ),
            restartOnSourceFailure =
                restartOnSourceFailure,
            source =
                source,
            onEach =
                onEach,
        )

    protected fun <Value, Source : Flow<Value>> launchForCollect(
        relationshipId:
            RelationshipId<Owner>,
        source:
            KProperty0<Source>,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launchForCollect(
            relationship =
                relationshipId,
            sourceRef =
                sourceRef(
                    source,
                ),
            restartOnSourceFailure =
                restartOnSourceFailure,
            source =
                source::get,
            onEach =
                onEach,
        )

    protected fun <Argument, Value, Source : Flow<Value>> launchForCollect(
        relationshipId:
            RelationshipId<Owner>,
        source:
            KFunction1<Argument, Source>,
        argument:
            Argument,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Boolean =
        executionRoot.coroutineLauncher.launchForCollect(
            relationship =
                relationshipId,
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
            onEach =
                onEach,
        )

    protected suspend fun <Result> handoff(
        dispatcher:
            CoroutineDispatcher,
        action:
            suspend () -> Result,
    ):
        Result =
        withContext(
            dispatcher,
        ) {
            action()
        }

    /** Stop all currently active Jobs belonging to this semantic relationship. */
    protected fun forceStop(
        relationshipId:
            RelationshipId<Owner>,
        reason:
            String =
            "force stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.forceStopRelationship(
            relationship =
                relationshipId,
            reason =
                reason,
        )

    /** Stop and join all currently active Jobs belonging to this semantic relationship. */
    protected suspend fun joinStop(
        relationshipId:
            RelationshipId<Owner>,
        reason:
            String =
            "join stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.joinStopRelationship(
            relationship =
                relationshipId,
            reason =
                reason,
        )

    /** Wait for Jobs currently admitted under this relationship without changing them. */
    protected suspend fun awaitCompletion(
        relationshipId:
            RelationshipId<Owner>,
    ):
        Boolean =
        executionRoot.coroutineLauncher.awaitRelationship(
            relationship =
                relationshipId,
        )

    companion object {
        /**
         * Long-lived observation bridge used by another rightful semantic root.
         *
         * The caller supplies the relationship key. SourceRef is diagnostic only; observation
         * uniqueness is relationship-keyed.
         */
        @RelationshipFoundationApi
        fun <
            Owner : Any,
            Value,
            Source : Flow<Value>,
        > observe(
            executionRoot:
                VexExecutionRoot,
            owner:
                KClass<*>,
            relationshipId:
                RelationshipId<Owner>,
            source:
                KProperty0<Source>,
            onEach:
                suspend (Value) -> Unit,
        ):
            Boolean =
            executionRoot.coroutineLauncher.launchForCollect(
                relationship =
                    relationshipId,
                sourceRef =
                    sourceRef(
                        owner,
                        source,
                    ),
                restartOnSourceFailure =
                    true,
                source =
                    source::get,
                onEach =
                    onEach,
            )

        /**
         * Execute one semantic operation under the supplied relationship and overlap approach.
         *
         * Family and other semantic roots remain responsible for forming their own typed
         * RelationshipId. Execution owns only Job custody and overlap behavior.
         */
        @RelationshipFoundationApi
        fun <
            Owner : Any,
            Argument,
            Result,
        > begin(
            executionRoot:
                VexExecutionRoot,
            owner:
                KClass<*>,
            relationshipId:
                RelationshipId<Owner>,
            approach:
                Approach,
            operation:
                KSuspendFunction1<Argument, Result>,
            argument:
                Argument,
            beforeOperation:
                () -> Unit =
                {},
            onResult:
                (Result) -> Unit =
                {},
            onFailure:
                (Throwable) -> Unit =
                {},
        ):
            Boolean =
            executionRoot.coroutineLauncher.launch(
                relationship =
                    relationshipId,
                approach =
                    approach,
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

        private suspend fun <Argument, Result> executeOperation(
            operation:
                KSuspendFunction1<Argument, Result>,
            argument:
                Argument,
            beforeOperation:
                () -> Unit,
            onResult:
                (Result) -> Unit,
            onFailure:
                (Throwable) -> Unit,
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
            owner:
                KClass<*>,
            reference:
                KCallable<*>,
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
        reference:
            KCallable<*>,
    ):
        SourceRef =
        SourceRef(
            "$executionName.${reference.name}",
        )
}

// [VXG RealForever]
