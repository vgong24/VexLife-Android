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
 * Active custody is keyed by exact [VexExecution.InstanceId]. Maintained overlap handling is keyed
 * directly by [VexExecution.Relationship]: live owner + semantic trigger + ordered arguments.
 * Source identity remains diagnostic/source-addressing information only.
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

    private data class MaintainedRelationship(
        var approach:
            VexExecution.Approach,
        val instances:
            LinkedHashSet<
                VexExecution.InstanceId
            > =
            linkedSetOf(),
    )

    private data class ManagedExecution(
        val instanceId:
            VexExecution.InstanceId,
        val sourceRef:
            VexExecution.SourceRef,
        val relationship:
            VexExecution.Relationship?,
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
            VexExecution.InstanceId,
            ManagedExecution
        >()

    private val relationships =
        LinkedHashMap<
            VexExecution.Relationship,
            MaintainedRelationship
        >()

    private val mutableExecutions =
        MutableStateFlow<
            Map<
                VexExecution.InstanceId,
                VexExecution.State
            >
        >(
            emptyMap(),
        )

    val executions:
        StateFlow<
            Map<
                VexExecution.InstanceId,
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
                relationship =
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
        relationship:
            VexExecution.Relationship,
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
                relationshipLocked(
                    relationship =
                        relationship,
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
                relationship =
                    relationship,
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
        relationship:
            VexExecution.Relationship,
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
                relationshipLocked(
                    relationship =
                        relationship,
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
                                relationship,
                        ),
                    )
                }

            createManagedLocked(
                sourceRef =
                    sourceRef,
                relationship =
                    relationship,
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
        relationship:
            VexExecution.Relationship,
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
                relationshipLocked(
                    relationship =
                        relationship,
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
                relationship =
                    relationship,
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
                relationship =
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

    private fun relationshipLocked(
        relationship:
            VexExecution.Relationship,
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
                    it.instanceId
                },
        )

        return members
    }

    private fun createManagedLocked(
        sourceRef:
            VexExecution.SourceRef,
        relationship:
            VexExecution.Relationship?,
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
        val instanceId =
            VexExecution.InstanceId(
                nextInstanceValue++,
            )

        val mutableState =
            MutableStateFlow(
                VexExecution.State(
                    instanceId =
                        instanceId,
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
                            instanceId.value,
                    ),
                start =
                    CoroutineStart.LAZY,
            ) {
                predecessors
                    .forEach { predecessor ->
                        predecessor.join()
                    }

                runManagedCoroutine(
                    instanceId =
                        instanceId,
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
                instanceId =
                    instanceId,
                sourceRef =
                    sourceRef,
                relationship =
                    relationship,
                job =
                    job,
                mutableState =
                    mutableState,
                maintainedObservation =
                    maintainedObservation,
            )

        active[
            instanceId
        ] =
            managed

        relationship?.let { key ->
            requireNotNull(
                relationships[
                    key
                ],
            ) {
                "Execution relationship " +
                    key +
                    " must be established before admission"
            }
                .instances +=
                instanceId
        }

        updateRegistryLocked(
            mutableState.value,
        )

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
                            instanceId =
                                instanceId,
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
                    instanceId,
                )

                relationship?.let { key ->
                    relationships[
                        key
                    ]
                        ?.let { maintained ->
                            maintained.instances.remove(
                                instanceId,
                            )

                            if (
                                maintained.instances
                                    .isEmpty()
                            ) {
                                relationships.remove(
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
            override val instanceId:
                VexExecution.InstanceId =
                managed.instanceId

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
        instanceId:
            VexExecution.InstanceId,
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
                            instanceId =
                                instanceId,
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
                                instanceId =
                                    instanceId,
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
                                    instanceId =
                                        instanceId,
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
                                instanceId =
                                    instanceId,
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
                        instanceId =
                            instanceId,
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
                            state.instanceId
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
