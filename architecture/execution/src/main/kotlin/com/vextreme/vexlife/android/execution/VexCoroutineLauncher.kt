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
 * Active custody is keyed by exact [VexExecution.InstanceId].
 *
 * SourceRef remains diagnostic only. Source uniqueness/cancellation is scoped by the exact live
 * VexExecution owner token, and maintained overlap is scoped by owner + [VexExecution.CoordinationKey].
 */
internal class VexCoroutineLauncher(
    dispatcher:
        CoroutineDispatcher,
) {

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

    private enum class CoordinationPolicy {
        Earliest,
        Latest,
        Queued,
    }

    private class CoordinationIdentity(
        val ownerToken:
            Any,
        val key:
            VexExecution.CoordinationKey,
    ) {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                CoordinationIdentity &&
                ownerToken ===
                other.ownerToken &&
                key ==
                other.key

        override fun hashCode():
            Int =
            31 *
                System.identityHashCode(
                    ownerToken,
                ) +
                key.hashCode()
    }

    private data class MaintainedCoordination(
        var policy:
            CoordinationPolicy,
        val instances:
            LinkedHashSet<
                VexExecution.InstanceId
            > =
            linkedSetOf(),
    )

    private data class ManagedExecution(
        val ownerToken:
            Any,
        val instanceId:
            VexExecution.InstanceId,
        val sourceIdentity:
            Any,
        val sourceRef:
            VexExecution.SourceRef,
        val coordination:
            CoordinationIdentity?,
        val job:
            Job,
        val mutableState:
            MutableStateFlow<
                VexExecution.Snapshot
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

    private val coordinations =
        LinkedHashMap<
            CoordinationIdentity,
            MaintainedCoordination
        >()

    private val mutableActiveExecutions =
        MutableStateFlow<
            Map<
                VexExecution.InstanceId,
                VexExecution.Snapshot
            >
        >(
            emptyMap(),
        )

    val activeExecutions:
        StateFlow<
            Map<
                VexExecution.InstanceId,
                VexExecution.Snapshot
            >
        > =
        mutableActiveExecutions.asStateFlow()

    fun launchOnce(
        ownerToken:
            Any,
        sourceIdentity:
            Any,
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

            check(
                active.values.none { managed ->
                    managed.ownerToken ===
                        ownerToken &&
                        managed.sourceIdentity ==
                        sourceIdentity &&
                        !managed.job.isCompleted
                },
            ) {
                "Coroutine " +
                    sourceRef.value +
                    " is already active"
            }

            createManagedLocked(
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    null,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    false,
                observationRecovery =
                    null,
                action =
                    action,
            )
        }

    fun launch(
        ownerToken:
            Any,
        sourceIdentity:
            Any,
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
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    null,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    false,
                observationRecovery =
                    null,
                action =
                    action,
            )
        }

    fun launchEarliest(
        ownerToken:
            Any,
        key:
            VexExecution.CoordinationKey,
        sourceIdentity:
            Any,
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

            val coordination =
                coordinationLocked(
                    ownerToken =
                        ownerToken,
                    key =
                        key,
                    policy =
                        CoordinationPolicy.Earliest,
                )

            val existing =
                activeCoordinationMembersLocked(
                    coordination,
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
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    coordination,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    false,
                observationRecovery =
                    null,
                action =
                    action,
            )
        }

    fun launchLatest(
        ownerToken:
            Any,
        key:
            VexExecution.CoordinationKey,
        sourceIdentity:
            Any,
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

            val coordination =
                coordinationLocked(
                    ownerToken =
                        ownerToken,
                    key =
                        key,
                    policy =
                        CoordinationPolicy.Latest,
                )

            val predecessors =
                activeCoordinationMembersLocked(
                    coordination,
                )

            predecessors
                .forEach { predecessor ->
                    predecessor.job.cancel(
                        CancellationException(
                            "superseded by latest execution for coordination key " +
                                key,
                        ),
                    )
                }

            createManagedLocked(
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    coordination,
                predecessors =
                    predecessors
                        .map {
                            it.job
                        },
                maintainedObservation =
                    false,
                observationRecovery =
                    null,
                action =
                    action,
            )
        }

    fun launchQueued(
        ownerToken:
            Any,
        key:
            VexExecution.CoordinationKey,
        sourceIdentity:
            Any,
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

            val coordination =
                coordinationLocked(
                    ownerToken =
                        ownerToken,
                    key =
                        key,
                    policy =
                        CoordinationPolicy.Queued,
                )

            val predecessors =
                activeCoordinationMembersLocked(
                    coordination,
                )

            createManagedLocked(
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    coordination,
                predecessors =
                    predecessors
                        .map {
                            it.job
                        },
                maintainedObservation =
                    false,
                observationRecovery =
                    null,
                action =
                    action,
            )
        }

    fun <Value> observe(
        ownerToken:
            Any,
        sourceIdentity:
            Any,
        sourceRef:
            VexExecution.SourceRef,
        recovery:
            VexExecution.ObservationRecovery,
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
                    managed.ownerToken ===
                        ownerToken &&
                        managed.maintainedObservation &&
                        managed.sourceIdentity ==
                        sourceIdentity &&
                        !managed.job.isCompleted
                },
            ) {
                "Flow source " +
                    sourceRef.value +
                    " is already actively observed"
            }

            createManagedLocked(
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                sourceRef =
                    sourceRef,
                coordination =
                    null,
                predecessors =
                    emptyList(),
                maintainedObservation =
                    true,
                observationRecovery =
                    recovery,
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
        ownerToken:
            Any,
        sourceIdentity:
            Any,
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
                        managed.ownerToken ===
                            ownerToken &&
                            managed.sourceIdentity ==
                            sourceIdentity &&
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
        ownerToken:
            Any,
        sourceIdentity:
            Any,
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
                        managed.ownerToken ===
                            ownerToken &&
                            managed.sourceIdentity ==
                            sourceIdentity &&
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

    fun forceStopOwner(
        ownerToken:
            Any,
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
                        managed.ownerToken ===
                            ownerToken &&
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

    suspend fun joinStopOwner(
        ownerToken:
            Any,
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
                        managed.ownerToken ===
                            ownerToken &&
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

    private fun coordinationLocked(
        ownerToken:
            Any,
        key:
            VexExecution.CoordinationKey,
        policy:
            CoordinationPolicy,
    ):
        MaintainedCoordination {
        val identity =
            CoordinationIdentity(
                ownerToken =
                    ownerToken,
                key =
                    key,
            )
        val existing =
            coordinations[
                identity
            ]

        if (
            existing ==
            null
        ) {
            return MaintainedCoordination(
                policy =
                    policy,
            ).also { maintained ->
                coordinations[
                    identity
                ] =
                    maintained
            }
        }

        activeCoordinationMembersLocked(
            existing,
        )

        if (
            existing.instances
                .isEmpty()
        ) {
            existing.policy =
                policy
            return existing
        }

        check(
            existing.policy ==
                policy,
        ) {
            "Execution coordination key " +
                key +
                " is already active as " +
                existing.policy +
                " and cannot also be " +
                policy
        }

        return existing
    }

    private fun activeCoordinationMembersLocked(
        maintained:
            MaintainedCoordination,
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
        ownerToken:
            Any,
        sourceIdentity:
            Any,
        sourceRef:
            VexExecution.SourceRef,
        coordination:
            CoordinationIdentity?,
        predecessors:
            List<Job>,
        maintainedObservation:
            Boolean,
        observationRecovery:
            VexExecution.ObservationRecovery?,
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
                VexExecution.Snapshot(
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
                    observationRecovery =
                        observationRecovery,
                    mutableState =
                        mutableState,
                    action =
                        action,
                )
            }

        val managed =
            ManagedExecution(
                ownerToken =
                    ownerToken,
                sourceIdentity =
                    sourceIdentity,
                instanceId =
                    instanceId,
                sourceRef =
                    sourceRef,
                coordination =
                    coordination,
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

        coordination?.let { identity ->
            requireNotNull(
                coordinations[
                    identity
                ],
            ) {
                "Execution coordination must be established before admission"
            }
                .instances +=
                instanceId
        }

        updateActiveRegistryLocked(
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
                        VexExecution.Snapshot(
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
                removeActiveRegistryLocked(
                    instanceId,
                )

                coordination?.let { identity ->
                    coordinations[
                        identity
                    ]
                        ?.let { maintained ->
                            maintained.instances.remove(
                                instanceId,
                            )

                            if (
                                maintained.instances
                                    .isEmpty()
                            ) {
                                coordinations.remove(
                                    identity,
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

            override val snapshot:
                StateFlow<
                    VexExecution.Snapshot
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
        observationRecovery:
            VexExecution.ObservationRecovery?,
        mutableState:
            MutableStateFlow<
                VexExecution.Snapshot
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
                        VexExecution.Snapshot(
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
                            VexExecution.Snapshot(
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
                        sourceFailure !=
                            null &&
                            observationRecovery !=
                            null &&
                            restartCount <
                            observationRecovery.maxRestarts

                    if (
                        !canRestart
                    ) {
                        publish(
                            mutableState =
                                mutableState,
                            state =
                                VexExecution.Snapshot(
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
                            VexExecution.Snapshot(
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

                    val restartDelayMillis =
                        requireNotNull(
                            observationRecovery,
                        ).restartDelayMillis

                    if (
                        restartDelayMillis >
                        0L
                    ) {
                        delay(
                            restartDelayMillis,
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
                    VexExecution.Snapshot(
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
                VexExecution.Snapshot
            >,
        state:
            VexExecution.Snapshot,
    ) {
        mutableState.value =
            state

        synchronized(
            registryLock,
        ) {
            updateActiveRegistryLocked(
                state,
            )
        }
    }

    private fun updateActiveRegistryLocked(
        state:
            VexExecution.Snapshot,
    ) {
        mutableActiveExecutions.value =
            java.util.Collections
                .unmodifiableMap(
                    LinkedHashMap(
                        mutableActiveExecutions.value,
                    ).apply {
                        this[
                            state.instanceId
                        ] =
                            state
                    },
                )
    }

    private fun removeActiveRegistryLocked(
        instanceId:
            VexExecution.InstanceId,
    ) {
        if (
            !mutableActiveExecutions
                .value
                .containsKey(
                    instanceId,
                )
        ) {
            return
        }

        mutableActiveExecutions.value =
            java.util.Collections
                .unmodifiableMap(
                    LinkedHashMap(
                        mutableActiveExecutions.value,
                    ).apply {
                        remove(
                            instanceId,
                        )
                    },
                )
    }

    private companion object {
        const val ROOT_COROUTINE_NAME =
            "vex.execution"
    }
}

// [VXG RealForever]
