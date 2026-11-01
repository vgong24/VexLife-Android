@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

/**
 * Active asynchronous-work owner for one concrete runtime responsibility.
 *
 * WHAT THIS IS
 * VexExecution owns current Job custody, overlap behavior, local source-addressed control, and
 * relationship-addressed cancellation for work admitted by one live execution owner.
 *
 * WHAT IT RELATES TO
 * [VexExecutionRoot] supplies the shared coroutine lifetime. A rightful semantic/runtime owner
 * supplies [RelationshipId] when work belongs to a maintained semantic relationship. Source-
 * addressed helpers may accept exact callable/Flow references when the source itself is the narrow
 * local address. Completed historical meaning belongs to VexLogger, not Execution.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * The permanent active-work law is:
 *
 *     RelationshipId -> actual Job(s)
 *
 * The actual Job is the runtime occurrence. This keeps present custody inspectable without minting
 * a second public InstanceId, SourceRef, Handle, Snapshot/history registry, or root-level duplicate
 * active-execution truth. The two authoring styles are intentionally distinct: source-addressed
 * helpers answer "this exact local source"; relationship-addressed helpers answer "this semantic
 * relationship".
 *
 * RESPONSIBILITY FENCE
 * Do not derive semantic identity from callable names, operation payload, Logger coordinates,
 * dependency providers, or Job occurrence. Do not add a peer RelationshipKey/Coordinate/OwnerToken
 * around RelationshipId. Do not make VexExecution a history owner, dependency container, provider
 * adapter, or semantic decision engine. Callable references are local execution convenience only
 * and must not be generalized into reflection-driven architecture.
 */
abstract class VexExecution protected constructor(
    private val executionRoot:
        VexExecutionRoot,
) {
    @RequiresOptIn(
        message =
            "Foundation relationship bridge only; use the rightful semantic/runtime owner's typed relationship boundary",
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
     * The one maintained-work address supplied by the rightful semantic/runtime owner.
     *
     * WHAT IT RELATES TO
     * RelationshipId is the key for the canonical RelationshipId -> Job(s) active-custody map.
     * Earliest / Latest / Queued and cancelActiveRelationship(...) all address that same map.
     *
     * WHY IT EXISTS
     * It preserves the already-authored semantic distinction that says which active work belongs
     * together while leaving each actual Job as the private runtime occurrence.
     *
     * RESPONSIBILITY FENCE
     * Execution coordinates this identity; it does not invent it from callable names, argument bags,
     * arbitrary payload, Logger IDs, dependency IDs, or occurrence tokens. Do not wrap it in another
     * RelationshipKey/Coordinate, author a parallel registry, or persist it as a generic trace ID.
     * The smallest rightful owner should derive it from semantic structure that already exists.
     */
    @RelationshipFoundationApi
    interface RelationshipId

    enum class Approach {
        Independent,
        Earliest,
        Latest,
        Queued,
    }

    private data class RelationshipLane(
        val approach:
            Approach,
        val jobs:
            LinkedHashSet<
                Job
            > =
            linkedSetOf(),
    )

    private val lock =
        Any()

    /** All current work owned by this exact VexExecution object. */
    private val ownedJobs =
        linkedSetOf<
            Job
        >()

    /**
     * Private source index used only for local source-addressed launch/cancel convenience.
     *
     * This is deliberately not exposed as architecture and never replaces RelationshipId.
     */
    private val jobsBySource =
        linkedMapOf<
            KCallable<*>,
            LinkedHashSet<Job>
        >()

    /** One maintained observation per exact source within this owner. */
    private val observationBySource =
        linkedMapOf<
            KCallable<*>,
            Job
        >()

    /**
     * The one canonical maintained-work map.
     *
     * No activeExecutions/InstanceId/Handle projection restates this truth.
     */
    private val relationships =
        linkedMapOf<
            RelationshipId,
            RelationshipLane
        >()

    private val executionName:
        String
        get() =
            this::class.qualifiedName
                ?: this::class.simpleName
                ?: "VexExecution"

    /**
     * Source-addressed finite work.
     *
     * The callable-reference style is deliberate here: the exact source supplied by the author is
     * both the operation Vex invokes and the narrow local address used for duplicate/source control
     * inside this VexExecution owner. This avoids a second manually-authored key beside the work.
     *
     * Do not generalize this into reflection-driven architecture. This API does not authorize class
     * scanning, member discovery, callable-name semantic IDs, durable protocol IDs, Logger identity,
     * dependency identity, or RelationshipId construction.
     *
     * Mapping:
     *
     *     exact callable source
     *         -> local source-addressed Job custody
     *
     * not:
     *
     *     callable
     *         -> semantic relationship identity
     */
    protected fun <Result> launchOnce(
        action:
            KSuspendFunction0<Result>,
    ):
        Job =
        launchOnceSource(
            source =
                action,
        ) {
            action()
        }

    protected fun <Argument, Result> launchOnce(
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Job =
        launchOnceSource(
            source =
                action,
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchOnce(
        action:
            KSuspendFunction2<
                First,
                Second,
                Result
            >,
        first:
            First,
        second:
            Second,
    ):
        Job =
        launchOnceSource(
            source =
                action,
        ) {
            action(
                first,
                second,
            )
        }

    /**
     * Independent source work uses the same exact callable-reference convenience as [launchOnce],
     * but every invocation is intentionally a distinct Job occurrence. The callable is a local
     * source coordinate only; it does not become semantic or durable identity.
     */
    protected fun <Result> launch(
        action:
            KSuspendFunction0<Result>,
    ):
        Job =
        launchSource(
            source =
                action,
        ) {
            action()
        }

    protected fun <Argument, Result> launch(
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Job =
        launchSource(
            source =
                action,
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launch(
        action:
            KSuspendFunction2<
                First,
                Second,
                Result
            >,
        first:
            First,
        second:
            Second,
    ):
        Job =
        launchSource(
            source =
                action,
        ) {
            action(
                first,
                second,
            )
        }

    /**
     * One maintained Flow observation for this exact source inside this exact execution owner.
     *
     * The function/property reference is intentional because the source itself is the thing being
     * observed and the local question is "is this exact source already under observation?". A lambda
     * plus a separately-authored key would duplicate that relationship and could drift.
     *
     * This is source-addressed observation only. It must not be extrapolated into reflection-based
     * dependency discovery, semantic Control identity, RelationshipId, Logger identity, or a general
     * Vex convention that Kotlin names define architecture.
     *
     * Mapping:
     *
     *     exact Flow source
     *         -> one active observation Job in this owner
     *
     * Source/subscription failure may use the configured bounded recovery mechanic when explicitly
     * enabled. Failure thrown while consuming a valid emitted value is downstream failure and is
     * never auto-restarted.
     */
    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source:
            KFunction0<Source>,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Job =
        launchObservation(
            sourceKey =
                source,
            restartOnSourceFailure =
                restartOnSourceFailure,
            source =
                source,
            onEach =
                onEach,
        )

    protected fun <Value, Source : Flow<Value>> launchForCollect(
        source:
            KProperty0<Source>,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Job =
        launchObservation(
            sourceKey =
                source,
            restartOnSourceFailure =
                restartOnSourceFailure,
            source =
                source::get,
            onEach =
                onEach,
        )

    protected fun <Argument, Value, Source : Flow<Value>> launchForCollect(
        source:
            KFunction1<
                Argument,
                Source
            >,
        argument:
            Argument,
        restartOnSourceFailure:
            Boolean =
            true,
        onEach:
            suspend (Value) -> Unit,
    ):
        Job =
        launchObservation(
            sourceKey =
                source,
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

    /** Cancel every unfinished Job admitted from this exact source within this owner. */
    protected fun forceStop(
        reference:
            KCallable<*>,
        reason:
            String =
            "force stopped",
    ):
        Boolean {
        val jobs =
            synchronized(
                lock,
            ) {
                jobsBySource[
                    reference
                ]
                    .orEmpty()
                    .filter {
                        !it.isCompleted
                    }
            }

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }
        return true
    }

    /** Cancel and join every unfinished Job admitted from this source within this owner. */
    protected suspend fun joinStop(
        reference:
            KCallable<*>,
        reason:
            String =
            "join stopped",
    ):
        Boolean {
        val jobs =
            synchronized(
                lock,
            ) {
                jobsBySource[
                    reference
                ]
                    .orEmpty()
                    .filter {
                        !it.isCompleted
                    }
            }

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }
        jobs.forEach { job ->
            job.join()
        }
        return true
    }

    /**
     * Cancel every Job currently owned by this exact VexExecution without terminating sibling owners
     * that happen to share the same VexExecutionRoot.
     */
    protected fun forceStopOwned(
        reason:
            String =
            "execution owner force stopped",
    ):
        Boolean {
        val jobs =
            synchronized(
                lock,
            ) {
                ownedJobs
                    .filter {
                        !it.isCompleted
                    }
            }

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }
        return true
    }

    /** Cancel and join all work owned by this exact VexExecution. */
    protected suspend fun joinStopOwned(
        reason:
            String =
            "execution owner join stopped",
    ):
        Boolean {
        val jobs =
            synchronized(
                lock,
            ) {
                ownedJobs
                    .filter {
                        !it.isCompleted
                    }
            }

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }
        jobs.forEach { job ->
            job.join()
        }
        return true
    }

    /** Cancel the current Job(s) for one exact active semantic relationship. */
    @RelationshipFoundationApi
    protected fun cancelActiveRelationship(
        relationshipId:
            RelationshipId,
        reason:
            String =
            "active relationship cancelled",
    ):
        Boolean {
        val jobs =
            synchronized(
                lock,
            ) {
                relationships[
                    relationshipId
                ]
                    ?.jobs
                    .orEmpty()
                    .filter {
                        !it.isCompleted
                    }
            }

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }
        return true
    }

    protected fun <Argument, Result> begin(
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
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
        Job =
        launchSource(
            source =
                operation,
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

    /**
     * Relationship-addressed maintained work.
     *
     * RelationshipId is the semantic/coordination address. The operation callable is supplied only
     * because the engine must invoke the work and currently keeps narrow local source bookkeeping.
     * It is NOT another relationship key and must not be exported, persisted, logged, compared, or
     * scaled into a second identity system.
     *
     * If a future implementation can remove callable bookkeeping here without losing a demonstrated
     * execution requirement, RelationshipId remains the architecture and the callable bookkeeping is
     * the thing to subtract.
     *
     * Mapping:
     *
     *     RelationshipId
     *         -> actual maintained Job(s)
     *
     * never:
     *
     *     RelationshipId + callable
     *         -> two peer identities for the same relationship
     */
    @RelationshipFoundationApi
    protected fun <Argument, Result> beginEarliest(
        relationshipId:
            RelationshipId,
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
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
        Job =
        beginRelationship(
            approach =
                Approach.Earliest,
            relationshipId =
                relationshipId,
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

    @RelationshipFoundationApi
    protected fun <Argument, Result> beginLatest(
        relationshipId:
            RelationshipId,
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
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
        Job =
        beginRelationship(
            approach =
                Approach.Latest,
            relationshipId =
                relationshipId,
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

    @RelationshipFoundationApi
    protected fun <Argument, Result> beginQueued(
        relationshipId:
            RelationshipId,
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
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
        Job =
        beginRelationship(
            approach =
                Approach.Queued,
            relationshipId =
                relationshipId,
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

    private fun launchOnceSource(
        source:
            KCallable<*>,
        action:
            suspend () -> Unit,
    ):
        Job =
        synchronized(
            lock,
        ) {
            check(
                jobsBySource[
                    source
                ]
                    .orEmpty()
                    .none {
                        !it.isCompleted
                    },
            ) {
                "Execution source " +
                    source.name +
                    " is already active in " +
                    executionName
            }

            admitSourceJobLocked(
                source =
                    source,
                action =
                    action,
            )
        }
            .also { job ->
                job.start()
            }

    private fun launchSource(
        source:
            KCallable<*>,
        action:
            suspend () -> Unit,
    ):
        Job =
        synchronized(
            lock,
        ) {
            admitSourceJobLocked(
                source =
                    source,
                action =
                    action,
            )
        }
            .also { job ->
                job.start()
            }

    private fun <Value> launchObservation(
        sourceKey:
            KCallable<*>,
        restartOnSourceFailure:
            Boolean,
        source:
            () -> Flow<Value>,
        onEach:
            suspend (Value) -> Unit,
    ):
        Job =
        synchronized(
            lock,
        ) {
            check(
                observationBySource[
                    sourceKey
                ]
                    ?.isCompleted !=
                    false,
            ) {
                "Flow source " +
                    sourceKey.name +
                    " is already actively observed in " +
                    executionName
            }

            admitSourceJobLocked(
                source =
                    sourceKey,
                action = {
                    collectWithRecovery(
                        restartOnSourceFailure =
                            restartOnSourceFailure,
                        source =
                            source,
                        onEach =
                            onEach,
                    )
                },
            ).also { job ->
                observationBySource[
                    sourceKey
                ] =
                    job
            }
        }
            .also { job ->
                job.start()
            }

    private fun admitSourceJobLocked(
        source:
            KCallable<*>,
        action:
            suspend () -> Unit,
    ):
        Job {
        val job =
            executionRoot.createJob(
                name =
                    executionName +
                        "." +
                        source.name,
                action =
                    action,
            )

        ownedJobs +=
            job
        jobsBySource
            .getOrPut(
                source,
            ) {
                linkedSetOf()
            } +=
            job

        job.invokeOnCompletion {
            synchronized(
                lock,
            ) {
                ownedJobs.remove(
                    job,
                )

                jobsBySource[
                    source
                ]
                    ?.let { jobs ->
                        jobs.remove(
                            job,
                        )
                        if (
                            jobs.isEmpty()
                        ) {
                            jobsBySource.remove(
                                source,
                            )
                        }
                    }

                if (
                    observationBySource[
                        source
                    ] ===
                    job
                ) {
                    observationBySource.remove(
                        source,
                    )
                }
            }
        }

        return job
    }

    private fun <Argument, Result> beginRelationship(
        approach:
            Approach,
        relationshipId:
            RelationshipId,
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
        beforeOperation:
            () -> Unit,
        onResult:
            (Result) -> Unit,
        onFailure:
            (Throwable) -> Unit,
    ):
        Job {
        var shouldStart =
            false

        val job =
            synchronized(
                lock,
            ) {
                val existingLane =
                    relationships[
                        relationshipId
                    ]

                existingLane
                    ?.jobs
                    ?.removeAll { job ->
                        job.isCompleted
                    }

                val activeJobs =
                    existingLane
                        ?.jobs
                        .orEmpty()
                        .toList()

                if (
                    existingLane !=
                    null &&
                    activeJobs.isNotEmpty()
                ) {
                    check(
                        existingLane.approach ==
                            approach,
                    ) {
                        "Execution relationship is already active as " +
                            existingLane.approach +
                            " and cannot also be " +
                            approach
                    }
                }

                if (
                    approach ==
                    Approach.Earliest &&
                    activeJobs.isNotEmpty()
                ) {
                    return@synchronized activeJobs.first()
                }

                val lane =
                    if (
                        existingLane ==
                            null ||
                        (
                            activeJobs.isEmpty() &&
                                existingLane.approach !=
                                approach
                        )
                    ) {
                        RelationshipLane(
                            approach =
                                approach,
                        ).also {
                            relationships[
                                relationshipId
                            ] =
                                it
                        }
                    } else {
                        existingLane
                    }

                val predecessors =
                    when (
                        approach
                    ) {
                        Approach.Independent ->
                            emptyList()

                        Approach.Earliest ->
                            emptyList()

                        Approach.Latest,
                        Approach.Queued,
                        ->
                            activeJobs
                    }

                val admitted =
                    executionRoot.createJob(
                        name =
                            executionName +
                                "." +
                                operation.name,
                    ) {
                        predecessors
                            .forEach { predecessor ->
                                predecessor.join()
                            }

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

                ownedJobs +=
                    admitted
                jobsBySource
                    .getOrPut(
                        operation,
                    ) {
                        linkedSetOf()
                    } +=
                    admitted
                lane.jobs +=
                    admitted

                admitted.invokeOnCompletion {
                    synchronized(
                        lock,
                    ) {
                        ownedJobs.remove(
                            admitted,
                        )

                        jobsBySource[
                            operation
                        ]
                            ?.let { jobs ->
                                jobs.remove(
                                    admitted,
                                )
                                if (
                                    jobs.isEmpty()
                                ) {
                                    jobsBySource.remove(
                                        operation,
                                    )
                                }
                            }

                        relationships[
                            relationshipId
                        ]
                            ?.let { currentLane ->
                                currentLane.jobs.remove(
                                    admitted,
                                )
                                if (
                                    currentLane.jobs
                                        .isEmpty()
                                ) {
                                    relationships.remove(
                                        relationshipId,
                                    )
                                }
                            }
                    }
                }

                if (
                    approach ==
                    Approach.Latest
                ) {
                    activeJobs.forEach { predecessor ->
                        predecessor.cancel(
                            CancellationException(
                                "superseded by latest execution relationship",
                            ),
                        )
                    }
                }

                shouldStart =
                    true
                admitted
            }

        if (
            shouldStart
        ) {
            job.start()
        }

        return job
    }

    private suspend fun <Value> collectWithRecovery(
        restartOnSourceFailure:
            Boolean,
        source:
            () -> Flow<Value>,
        onEach:
            suspend (Value) -> Unit,
    ) {
        var restartCount =
            0

        while (
            true
        ) {
            try {
                val observed =
                    try {
                        source()
                    } catch (
                        cancelled:
                            CancellationException,
                    ) {
                        throw cancelled
                    } catch (
                        failure:
                            Throwable,
                    ) {
                        throw SourceFailure(
                            failure,
                        )
                    }

                observed
                    .catch { failure ->
                        if (
                            failure is
                            CancellationException
                        ) {
                            throw failure
                        }

                        throw SourceFailure(
                            failure,
                        )
                    }
                    .collect { value ->
                        onEach(
                            value,
                        )
                    }

                return
            } catch (
                cancelled:
                    CancellationException,
            ) {
                throw cancelled
            } catch (
                failure:
                    SourceFailure,
            ) {
                if (
                    !restartOnSourceFailure ||
                    restartCount >=
                    executionRoot.maxCollectRestarts
                ) {
                    throw failure.sourceFailure
                }

                restartCount +=
                    1

                delay(
                    executionRoot.collectRestartDelayMillis,
                )
            }
        }
    }

    private suspend fun <Argument, Result> executeOperation(
        operation:
            KSuspendFunction1<
                Argument,
                Result
            >,
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

        /*
         * Result handling is a separate boundary. If it throws, that is not retroactively provider
         * operation failure and must not invoke onFailure(operation failure).
         */
        onResult(
            result,
        )
    }

    /**
     * Direct reusable foundation entry points.
     *
     * These exist specifically so a semantic/runtime owner that already has the rightful
     * RelationshipId or exact observed source does NOT need to manufacture a matching
     * <Feature>Execution class merely to reach begin/observe.
     *
     * [owner] is the already-existing live object whose execution custody must stay isolated from
     * sibling owners sharing the same [VexExecutionRoot]. It is scope, not semantic identity.
     *
     * Feature/domain verbs stay with their rightful owner:
     *
     *     owner.addStop(...)
     *         -> VexExecution.begin/...
     *
     * not:
     *
     *     owner.addStop(...)
     *         -> AddStopExecution.addStop(...)
     *         -> launch(...)
     */
    companion object {
        fun <Value, Source : Flow<Value>> observe(
            executionRoot:
                VexExecutionRoot,
            owner:
                Any,
            source:
                KProperty0<Source>,
            restartOnSourceFailure:
                Boolean =
                true,
            onEach:
                suspend (Value) -> Unit,
        ):
            Job =
            executionRoot
                .directExecution(
                    owner,
                )
                .launchForCollect(
                    source =
                        source,
                    restartOnSourceFailure =
                        restartOnSourceFailure,
                    onEach =
                        onEach,
                )

        fun <Argument, Result> begin(
            executionRoot:
                VexExecutionRoot,
            owner:
                Any,
            operation:
                KSuspendFunction1<
                    Argument,
                    Result
                >,
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
            Job =
            executionRoot
                .directExecution(
                    owner,
                )
                .begin(
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginEarliest(
            executionRoot:
                VexExecutionRoot,
            owner:
                Any,
            relationshipId:
                RelationshipId,
            operation:
                KSuspendFunction1<
                    Argument,
                    Result
                >,
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
            Job =
            executionRoot
                .directExecution(
                    owner,
                )
                .beginEarliest(
                    relationshipId =
                        relationshipId,
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginLatest(
            executionRoot:
                VexExecutionRoot,
            owner:
                Any,
            relationshipId:
                RelationshipId,
            operation:
                KSuspendFunction1<
                    Argument,
                    Result
                >,
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
            Job =
            executionRoot
                .directExecution(
                    owner,
                )
                .beginLatest(
                    relationshipId =
                        relationshipId,
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

        @RelationshipFoundationApi
        fun <Argument, Result> beginQueued(
            executionRoot:
                VexExecutionRoot,
            owner:
                Any,
            relationshipId:
                RelationshipId,
            operation:
                KSuspendFunction1<
                    Argument,
                    Result
                >,
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
            Job =
            executionRoot
                .directExecution(
                    owner,
                )
                .beginQueued(
                    relationshipId =
                        relationshipId,
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

    private class SourceFailure(
        val sourceFailure:
            Throwable,
    ) :
        RuntimeException(
            sourceFailure,
        )
}

// [VXG RealForever]
