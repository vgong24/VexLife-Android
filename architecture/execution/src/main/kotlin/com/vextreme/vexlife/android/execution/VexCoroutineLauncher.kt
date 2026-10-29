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
 * Three runtime surfaces stay intentionally separate:
 *
 * active[Instance]
 *     exact unfinished coroutine custody
 *
 * maintainedByRelationshipId[RelationshipId]
 *     overlap coordination only while unfinished Instances belong to that RelationshipId
 *
 * executions[Instance]
 *     observable lifecycle snapshots; terminal State may remain after active/maintained cleanup
 *
 * RelationshipId is the one semantic execution identity. SourceRef remains diagnostic only.
 */
internal class VexCoroutineLauncher(
    dispatcher: CoroutineDispatcher,
    private val maxCollectRestarts: Int,
    private val collectRestartDelayMillis: Long,
) {
    init {
        require(maxCollectRestarts >= 0) {
            "maxCollectRestarts must be non-negative"
        }
        require(collectRestartDelayMillis >= 0L) {
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

    private var nextInstanceValue =
        1L

    private data class RelationshipEntry(
        var approach:
            VexExecution.Approach,
        val instances:
            LinkedHashSet<
                VexExecution.Instance
            > =
            linkedSetOf(),
    )

    private data class ManagedExecution(
        val instance:
            VexExecution.Instance,
        val sourceRef:
            VexExecution.SourceRef,
        val relationshipId:
            VexExecution.RelationshipId?,
        val job:
            Job,
        val mutableState:
            MutableStateFlow<
                VexExecution.State
            >,
        val maintainedObservation:
            Boolean,
    )

    private class VexCollectionSourceFailure(
        val sourceFailure:
            Throwable,
    ) :
        RuntimeException(
            sourceFailure,
        )

    private val active =
        LinkedHashMap<
            VexExecution.Instance,
            ManagedExecution
        >()

    /*
     * Ephemeral overlap-coordination map.
     *
     * Completion removes the finished Instance from its entry. When the final Instance leaves, the
     * RelationshipId entry itself is removed. A later equal RelationshipId therefore begins from a
     * fresh empty coordination entry rather than inheriting stale runtime membership.
     */
    private val maintainedByRelationshipId =
        LinkedHashMap<
            VexExecution.RelationshipId,
            RelationshipEntry
        >()

    private val mutableExecutions =
        MutableStateFlow<
            Map<
                VexExecution.Instance,
                VexExecution.State
            >
        >(
            emptyMap(),
        )

    val executions:
        StateFlow<
            Map<
                VexExecution.Instance,
                VexExecution.State
            >
        > =
        mutableExecutions.asStateFlow()

    fun launch(
        sourceRef:
            VexExecution.SourceRef,
        action:
            suspend () -> Unit,
    ):
        VexExecution.Handle =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationshipId =
                    null,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    false,
                restartRecoverableFailure =
                    false,
                action =
                    action,
            )
        }

    fun launchEarliest(
        relationshipId:
            VexExecution.RelationshipId,
        sourceRef:
            VexExecution.SourceRef,
        action:
            suspend () -> Unit,
    ):
        VexExecution.Handle =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            val maintained =
                relationshipEntryLocked(
                    relationshipId =
                        relationshipId,
                    approach =
                        VexExecution.Approach.Earliest,
                )

            val existing =
                activeRelationshipMembersLocked(
                    maintained,
                )
                    .firstOrNull()

            if (
                existing !=
                null
            ) {
                return@synchronized handleFor(
                    existing,
                )
            }

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationshipId =
                    relationshipId,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    false,
                restartRecoverableFailure =
                    false,
                action =
                    action,
            )
        }

    fun launchLatest(
        relationshipId:
            VexExecution.RelationshipId,
        sourceRef:
            VexExecution.SourceRef,
        action:
            suspend () -> Unit,
    ):
        VexExecution.Handle =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            val maintained =
                relationshipEntryLocked(
                    relationshipId =
                        relationshipId,
                    approach =
                        VexExecution.Approach.Latest,
                )

            val predecessors =
                activeRelationshipMembersLocked(
                    maintained,
                )

            predecessors
                .forEach { predecessor ->
                    predecessor.job.cancel(
                        CancellationException(
                            "superseded by latest execution for " +
                                relationshipId,
                        ),
                    )
                }

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationshipId =
                    relationshipId,
                predecessors =
                    predecessors
                        .map {
                            it.job
                        },
                maintainedObservation =
                    false,
                restartRecoverableFailure =
                    false,
                action =
                    action,
            )
        }

    fun launchQueued(
        relationshipId:
            VexExecution.RelationshipId,
        sourceRef:
            VexExecution.SourceRef,
        action:
            suspend () -> Unit,
    ):
        VexExecution.Handle =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            val maintained =
                relationshipEntryLocked(
                    relationshipId =
                        relationshipId,
                    approach =
                        VexExecution.Approach.Queued,
                )

            val predecessors =
                activeRelationshipMembersLocked(
                    maintained,
                )

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationshipId =
                    relationshipId,
                predecessors =
                    predecessors
                        .map {
                            it.job
                        },
                maintainedObservation =
                    false,
                restartRecoverableFailure =
                    false,
                action =
                    action,
            )
        }

    fun <Value> launchForCollect(
        sourceRef:
            VexExecution.SourceRef,
        restartOnSourceFailure:
            Boolean,
        source:
            () -> Flow<Value>,
        onEach:
            suspend (Value) -> Unit,
    ):
        VexExecution.Handle =
        synchronized(
            registryLock,
        ) {
            requireAcceptingWorkLocked()

            check(
                active.values.none { managed ->
                    managed.maintainedObservation &&
                        managed.sourceRef ==
                        sourceRef &&
                        !managed.job.isCompleted
                },
            ) {
                "Flow source " +
                    sourceRef.value +
                    " is already actively observed"
            }

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationshipId =
                    null,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    true,
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
        }

    fun forceStopSource(
        sourceRef:
            VexExecution.SourceRef,
        reason:
            String,
    ):
        Boolean {
        val jobs =
            synchronized(
                registryLock,
            ) {
                active.values
                    .filter { managed ->
                        managed.sourceRef ==
                            sourceRef &&
                            !managed.job.isCompleted
                    }
                    .map {
                        it.job
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

    suspend fun joinStopSource(
        sourceRef:
            VexExecution.SourceRef,
        reason:
            String,
    ):
        Boolean {
        val jobs =
            synchronized(
                registryLock,
            ) {
                active.values
                    .filter { managed ->
                        managed.sourceRef ==
                            sourceRef &&
                            !managed.job.isCompleted
                    }
                    .map {
                        it.job
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

    private fun requireAcceptingWorkLocked() {
        check(
            acceptingWork &&
                parentJob.isActive,
        ) {
            "VexExecutionRoot is stopped and cannot accept new work"
        }
    }

    private fun relationshipEntryLocked(
        relationshipId:
            VexExecution.RelationshipId,
        approach:
            VexExecution.Approach,
    ):
        RelationshipEntry {
        val existing =
            maintainedByRelationshipId[
                relationshipId
            ]

        if (
            existing ==
            null
        ) {
            return RelationshipEntry(
                approach =
                    approach,
            ).also { maintained ->
                maintainedByRelationshipId[
                    relationshipId
                ] =
                    maintained
            }
        }

        activeRelationshipMembersLocked(
            existing,
        )

        if (
            existing.instances
                .isEmpty()
        ) {
            existing.approach =
                approach
            return existing
        }

        check(
            existing.approach ==
                approach,
        ) {
            "Execution relationshipId " +
                relationshipId +
                " is already active as " +
                existing.approach +
                " and cannot also be " +
                approach
        }

        return existing
    }

    private fun activeRelationshipMembersLocked(
        maintained:
            RelationshipEntry,
    ):
        List<
            ManagedExecution
        > {
        val members =
            maintained.instances
                .mapNotNull(
                    active::get,
                )
                .filter {
                    !it.job.isCompleted
                }

        maintained.instances.retainAll(
            members
                .mapTo(
                    linkedSetOf(),
                ) {
                    it.instance
                },
        )

        return members
    }

    private fun createManagedLocked(
        sourceRef:
            VexExecution.SourceRef,
        relationshipId:
            VexExecution.RelationshipId?,
        predecessors:
            List<Job>,
        maintainedObservation:
            Boolean,
        restartRecoverableFailure:
            Boolean,
        action:
            suspend () -> Unit,
    ):
        VexExecution.Handle {
        val instance =
            VexExecution.Instance(
                nextInstanceValue++,
            )

        val mutableState =
            MutableStateFlow(
                VexExecution.State(
                    instance =
                        instance,
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
                        sourceRef.value +
                            "#" +
                            instance.value,
                    ),
                start =
                    CoroutineStart.LAZY,
            ) {
                predecessors
                    .forEach { predecessor ->
                        predecessor.join()
                    }

                runManagedCoroutine(
                    instance =
                        instance,
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
                instance =
                    instance,
                sourceRef =
                    sourceRef,
                relationshipId =
                    relationshipId,
                job =
                    job,
                mutableState =
                    mutableState,
                maintainedObservation =
                    maintainedObservation,
            )

        active[
            instance
        ] =
            managed

        relationshipId?.let { key ->
            requireNotNull(
                maintainedByRelationshipId[
                    key
                ],
            ) {
                "Execution relationshipId " +
                    key +
                    " must be established before admission"
            }
                .instances +=
                instance
        }

        updateRegistryLocked(
            mutableState.value,
        )

        /*
         * Terminal cleanup has two distinct jobs:
         * 1. remove this Instance from active runtime custody;
         * 2. remove it from maintained overlap coordination, deleting the RelationshipId entry when
         *    no unfinished Instances remain.
         *
         * mutableExecutions is intentionally separate retained observation state and is not used to
         * decide Earliest / Latest / Queued behavior.
         */
        job.invokeOnCompletion { cause ->
            if (
                cause is
                    CancellationException &&
                !mutableState
                    .value
                    .status
                    .isTerminal()
            ) {
                publish(
                    mutableState =
                        mutableState,
                    state =
                        VexExecution.State(
                            instance =
                                instance,
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
                active.remove(
                    instance,
                )

                relationshipId?.let { key ->
                    maintainedByRelationshipId[
                        key
                    ]
                        ?.let { maintained ->
                            maintained.instances.remove(
                                instance,
                            )

                            if (
                                maintained.instances
                                    .isEmpty()
                            ) {
                                maintainedByRelationshipId.remove(
                                    key,
                                )
                            }
                        }
                }
            }
        }

        job.start()

        return handleFor(
            managed,
        )
    }

    private fun handleFor(
        managed:
            ManagedExecution,
    ):
        VexExecution.Handle =
        object :
            VexExecution.Handle {
            override val instance:
                VexExecution.Instance =
                managed.instance

            override val sourceRef:
                VexExecution.SourceRef =
                managed.sourceRef

            override val state:
                StateFlow<
                    VexExecution.State
                > =
                managed.mutableState
                    .asStateFlow()

            override fun forceStop(
                reason:
                    String,
            ):
                Boolean {
                if (
                    !managed.job
                        .isActive
                ) {
                    return false
                }

                managed.job.cancel(
                    CancellationException(
                        reason,
                    ),
                )
                return true
            }

            override suspend fun joinStop(
                reason:
                    String,
            ):
                Boolean {
                if (
                    !managed.job.isActive &&
                    managed.job.isCompleted
                ) {
                    return false
                }

                if (
                    managed.job.isActive
                ) {
                    managed.job.cancel(
                        CancellationException(
                            reason,
                        ),
                    )
                }

                managed.job.join()
                return true
            }

            override suspend fun awaitCompletion() {
                managed.job.join()
            }
        }

    private suspend fun runManagedCoroutine(
        instance:
            VexExecution.Instance,
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
                            instance =
                                instance,
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
                                instance =
                                    instance,
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
                                    instance =
                                        instance,
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
                                instance =
                                    instance,
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
                        instance =
                            instance,
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
            updateRegistryLocked(
                state,
            )
        }
    }

    private fun updateRegistryLocked(
        state:
            VexExecution.State,
    ) {
        mutableExecutions.value =
            java.util.Collections
                .unmodifiableMap(
                    LinkedHashMap(
                        mutableExecutions.value,
                    ).apply {
                        this[
                            state.instance
                        ] =
                            state
                    },
                )
    }

    private companion object {
        const val ROOT_COROUTINE_NAME =
            "vex.execution"
    }
}

// [VXG RealForever]
