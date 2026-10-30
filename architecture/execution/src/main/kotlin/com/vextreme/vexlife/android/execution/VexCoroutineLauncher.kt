@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.execution

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Internal coroutine engine.
 *
 * Active Job custody is rooted in semantic [VexExecution.RelationshipId] keys. A relationship owns
 * the active Jobs admitted for it and [VexExecution.Approach] is the only overlap policy.
 * [VexExecution.SourceRef] is retained only for diagnostics.
 */
internal class VexCoroutineLauncher(
    dispatcher:
        CoroutineDispatcher,
    private val maxCollectRestarts:
        Int,
    private val collectRestartDelayMillis:
        Long,
) {
    init {
        require(
            maxCollectRestarts >=
                0,
        ) {
            "maxCollectRestarts must be non-negative"
        }
        require(
            collectRestartDelayMillis >=
                0L,
        ) {
            "collectRestartDelayMillis must be non-negative"
        }
    }

    private val parentJob:
        Job =
        SupervisorJob()

    private val scope =
        CoroutineScope(
            dispatcher +
                parentJob +
                CoroutineName(
                    ROOT_COROUTINE_NAME,
                ),
        )

    private val registryLock =
        Any()

    private var acceptingWork =
        true

    private data class MaintainedRelationship(
        var approach:
            VexExecution.Approach,
        val executions:
            LinkedHashSet<
                ManagedExecution
            > =
            linkedSetOf(),
    )

    private class ManagedExecution(
        val sourceRef:
            VexExecution.SourceRef,
        val job:
            Job,
        val mutableState:
            MutableStateFlow<
                VexExecution.State
            >,
    )

    private class VexCollectionSourceFailure(
        val sourceFailure:
            Throwable,
    ) :
        RuntimeException(
            sourceFailure,
        )

    private val relationships =
        LinkedHashMap<
            VexExecution.RelationshipId<*>,
            MaintainedRelationship
        >()

    private val mutableExecutions =
        MutableStateFlow<
            Map<
                VexExecution.RelationshipId<*>,
                List<
                    VexExecution.State
                >
            >
        >(
            emptyMap(),
        )

    val executions:
        StateFlow<
            Map<
                VexExecution.RelationshipId<*>,
                List<
                    VexExecution.State
                >
            >
        > =
        mutableExecutions.asStateFlow()

    fun launch(
        relationship:
            VexExecution.RelationshipId<*>,
        approach:
            VexExecution.Approach,
        sourceRef:
            VexExecution.SourceRef,
        action:
            suspend () -> Unit,
    ):
        Boolean =
        launchManaged(
            relationship =
                relationship,
            approach =
                approach,
            sourceRef =
                sourceRef,
            restartRecoverableFailure =
                false,
            action =
                action,
        )

    fun <Value> launchForCollect(
        relationship:
            VexExecution.RelationshipId<*>,
        sourceRef:
            VexExecution.SourceRef,
        restartOnSourceFailure:
            Boolean,
        source:
            () -> Flow<Value>,
        onEach:
            suspend (Value) -> Unit,
    ):
        Boolean =
        launchManaged(
            relationship =
                relationship,
            approach =
                VexExecution.Approach.Earliest,
            sourceRef =
                sourceRef,
            restartRecoverableFailure =
                restartOnSourceFailure,
        ) {
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
                    throw VexCollectionSourceFailure(
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

                    throw VexCollectionSourceFailure(
                        failure,
                    )
                }
                .collect { value ->
                    onEach(
                        value,
                    )
                }
        }

    fun forceStopRelationship(
        relationship:
            VexExecution.RelationshipId<*>,
        reason:
            String,
    ):
        Boolean {
        val jobs =
            activeJobs(
                relationship,
            )

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

    suspend fun joinStopRelationship(
        relationship:
            VexExecution.RelationshipId<*>,
        reason:
            String,
    ):
        Boolean {
        val jobs =
            activeJobs(
                relationship,
            )

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

    suspend fun awaitRelationship(
        relationship:
            VexExecution.RelationshipId<*>,
    ):
        Boolean {
        val jobs =
            activeJobs(
                relationship,
            )

        if (
            jobs.isEmpty()
        ) {
            return false
        }

        jobs.forEach { job ->
            job.join()
        }

        return true
    }

    fun forceStopAll(
        reason:
            String,
    ) {
        synchronized(
            registryLock,
        ) {
            acceptingWork =
                false
        }

        parentJob.cancel(
            CancellationException(
                reason,
            ),
        )
    }

    suspend fun joinStopAll(
        reason:
            String,
    ) {
        synchronized(
            registryLock,
        ) {
            acceptingWork =
                false
        }

        parentJob.cancel(
            CancellationException(
                reason,
            ),
        )

        parentJob.join()
    }

    private fun launchManaged(
        relationship:
            VexExecution.RelationshipId<*>,
        approach:
            VexExecution.Approach,
        sourceRef:
            VexExecution.SourceRef,
        restartRecoverableFailure:
            Boolean,
        action:
            suspend () -> Unit,
    ):
        Boolean =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            val maintained =
                relationshipLocked(
                    relationship =
                        relationship,
                    approach =
                        approach,
                )

            val active =
                activeRelationshipMembersLocked(
                    maintained,
                )

            when (
                approach
            ) {
                VexExecution.Approach.Independent ->
                    createManagedLocked(
                        relationship =
                            relationship,
                        maintained =
                            maintained,
                        sourceRef =
                            sourceRef,
                        predecessors =
                            emptyList(),
                        restartRecoverableFailure =
                            restartRecoverableFailure,
                        action =
                            action,
                    )

                VexExecution.Approach.Earliest -> {
                    if (
                        active.isNotEmpty()
                    ) {
                        false
                    } else {
                        createManagedLocked(
                            relationship =
                                relationship,
                            maintained =
                                maintained,
                            sourceRef =
                                sourceRef,
                            predecessors =
                                emptyList(),
                            restartRecoverableFailure =
                                restartRecoverableFailure,
                            action =
                                action,
                        )
                    }
                }

                VexExecution.Approach.Latest -> {
                    active.forEach { predecessor ->
                        predecessor.job.cancel(
                            CancellationException(
                                "superseded by latest execution for " +
                                    relationship,
                            ),
                        )
                    }

                    createManagedLocked(
                        relationship =
                            relationship,
                        maintained =
                            maintained,
                        sourceRef =
                            sourceRef,
                        predecessors =
                            active.map {
                                it.job
                            },
                        restartRecoverableFailure =
                            restartRecoverableFailure,
                        action =
                            action,
                    )
                }

                VexExecution.Approach.Queued ->
                    createManagedLocked(
                        relationship =
                            relationship,
                        maintained =
                            maintained,
                        sourceRef =
                            sourceRef,
                        predecessors =
                            active.map {
                                it.job
                            },
                        restartRecoverableFailure =
                            restartRecoverableFailure,
                        action =
                            action,
                    )
            }
        }

    private fun activeJobs(
        relationship:
            VexExecution.RelationshipId<*>,
    ):
        List<Job> =
        synchronized(
            registryLock,
        ) {
            relationships[
                relationship
            ]
                ?.let(
                    ::activeRelationshipMembersLocked,
                )
                .orEmpty()
                .map {
                    it.job
                }
        }

    private fun requireAcceptingWorkLocked() {
        check(
            acceptingWork &&
                parentJob.isActive,
        ) {
            "VexExecutionRoot is stopped and cannot accept new work"
        }
    }

    private fun relationshipLocked(
        relationship:
            VexExecution.RelationshipId<*>,
        approach:
            VexExecution.Approach,
    ):
        MaintainedRelationship {
        val existing =
            relationships[
                relationship
            ]

        if (
            existing ==
            null
        ) {
            return MaintainedRelationship(
                approach =
                    approach,
            ).also { maintained ->
                relationships[
                    relationship
                ] =
                    maintained
            }
        }

        val active =
            activeRelationshipMembersLocked(
                existing,
            )

        if (
            active.isEmpty()
        ) {
            existing.approach =
                approach
            return existing
        }

        check(
            existing.approach ==
                approach,
        ) {
            "Execution relationship " +
                relationship +
                " is already active as " +
                existing.approach +
                " and cannot also be " +
                approach
        }

        return existing
    }

    private fun activeRelationshipMembersLocked(
        maintained:
            MaintainedRelationship,
    ):
        List<
            ManagedExecution
        > {
        return maintained.executions
            .filter {
                !it.job.isCompleted
            }
    }

    private fun createManagedLocked(
        relationship:
            VexExecution.RelationshipId<*>,
        maintained:
            MaintainedRelationship,
        sourceRef:
            VexExecution.SourceRef,
        predecessors:
            List<Job>,
        restartRecoverableFailure:
            Boolean,
        action:
            suspend () -> Unit,
    ):
        Boolean {
        val mutableState =
            MutableStateFlow(
                VexExecution.State(
                    sourceRef =
                        sourceRef,
                    status =
                        VexExecution.Status.STARTING,
                    attempt =
                        0,
                    restartCount =
                        0,
                ),
            )

        lateinit var job:
            Job

        job =
            scope.launch(
                context =
                    CoroutineName(
                        sourceRef.value,
                    ),
                start =
                    CoroutineStart.LAZY,
            ) {
                predecessors.forEach { predecessor ->
                    predecessor.join()
                }

                runManagedCoroutine(
                    sourceRef =
                        sourceRef,
                    restartRecoverableFailure =
                        restartRecoverableFailure,
                    mutableState =
                        mutableState,
                    action =
                        action,
                )
            }

        val managed =
            ManagedExecution(
                sourceRef =
                    sourceRef,
                job =
                    job,
                mutableState =
                    mutableState,
            )

        maintained.executions +=
            managed

        updateRegistryLocked()

        job.invokeOnCompletion { cause ->
            if (
                cause is
                    CancellationException &&
                !mutableState.value
                    .status
                    .isTerminal()
            ) {
                publish(
                    mutableState =
                        mutableState,
                    state =
                        VexExecution.State(
                            sourceRef =
                                sourceRef,
                            status =
                                VexExecution.Status.CANCELLED,
                            attempt =
                                mutableState.value.attempt,
                            restartCount =
                                mutableState.value.restartCount,
                            failureClass =
                                cause::class.simpleName,
                            failureMessage =
                                cause.message,
                        ),
                )
            }

            synchronized(
                registryLock,
            ) {
                updateRegistryLocked()
            }
        }

        job.start()
        return true
    }

    private suspend fun runManagedCoroutine(
        sourceRef:
            VexExecution.SourceRef,
        restartRecoverableFailure:
            Boolean,
        mutableState:
            MutableStateFlow<
                VexExecution.State
            >,
        action:
            suspend () -> Unit,
    ) {
        var attempt =
            0

        var restartCount =
            0

        try {
            while (
                true
            ) {
                attempt +=
                    1

                publish(
                    mutableState =
                        mutableState,
                    state =
                        VexExecution.State(
                            sourceRef =
                                sourceRef,
                            status =
                                VexExecution.Status.RUNNING,
                            attempt =
                                attempt,
                            restartCount =
                                restartCount,
                        ),
                )

                try {
                    coroutineScope {
                        action()
                    }

                    publish(
                        mutableState =
                            mutableState,
                        state =
                            VexExecution.State(
                                sourceRef =
                                    sourceRef,
                                status =
                                    VexExecution.Status.COMPLETED,
                                attempt =
                                    attempt,
                                restartCount =
                                    restartCount,
                            ),
                    )
                    return
                } catch (
                    cancelled:
                        CancellationException,
                ) {
                    throw cancelled
                } catch (
                    failure:
                        Throwable,
                ) {
                    val sourceFailure =
                        failure as?
                            VexCollectionSourceFailure

                    val reportedFailure =
                        sourceFailure
                            ?.sourceFailure
                            ?: failure

                    val canRestart =
                        restartRecoverableFailure &&
                            sourceFailure !=
                            null &&
                            restartCount <
                            maxCollectRestarts

                    if (
                        !canRestart
                    ) {
                        publish(
                            mutableState =
                                mutableState,
                            state =
                                VexExecution.State(
                                    sourceRef =
                                        sourceRef,
                                    status =
                                        VexExecution.Status.FAILED,
                                    attempt =
                                        attempt,
                                    restartCount =
                                        restartCount,
                                    failureClass =
                                        reportedFailure::class.simpleName,
                                    failureMessage =
                                        reportedFailure.message,
                                ),
                        )
                        return
                    }

                    restartCount +=
                        1

                    publish(
                        mutableState =
                            mutableState,
                        state =
                            VexExecution.State(
                                sourceRef =
                                    sourceRef,
                                status =
                                    VexExecution.Status.RESTARTING,
                                attempt =
                                    attempt,
                                restartCount =
                                    restartCount,
                                failureClass =
                                    reportedFailure::class.simpleName,
                                failureMessage =
                                    reportedFailure.message,
                            ),
                    )

                    if (
                        collectRestartDelayMillis >
                        0L
                    ) {
                        delay(
                            collectRestartDelayMillis,
                        )
                    }
                }
            }
        } catch (
            cancelled:
                CancellationException,
        ) {
            publish(
                mutableState =
                    mutableState,
                state =
                    VexExecution.State(
                        sourceRef =
                            sourceRef,
                        status =
                            VexExecution.Status.CANCELLED,
                        attempt =
                            attempt,
                        restartCount =
                            restartCount,
                        failureClass =
                            cancelled::class.simpleName,
                        failureMessage =
                            cancelled.message,
                    ),
            )

            throw cancelled
        }
    }

    private fun publish(
        mutableState:
            MutableStateFlow<
                VexExecution.State
            >,
        state:
            VexExecution.State,
    ) {
        mutableState.value =
            state

        synchronized(
            registryLock,
        ) {
            updateRegistryLocked()
        }
    }

    private fun updateRegistryLocked() {
        val projection =
            linkedMapOf<
                VexExecution.RelationshipId<*>,
                List<
                    VexExecution.State
                >
            >()

        relationships.forEach {
                (relationship, maintained),
            ->
            val states =
                maintained.executions
                    .map {
                        it.mutableState.value
                    }

            projection[
                relationship
            ] =
                java.util.Collections.unmodifiableList(
                    states,
                )
        }

        mutableExecutions.value =
            java.util.Collections.unmodifiableMap(
                projection,
            )
    }

    private companion object {
        const val ROOT_COROUTINE_NAME =
            "vex.execution"
    }
}

// [VXG RealForever]
