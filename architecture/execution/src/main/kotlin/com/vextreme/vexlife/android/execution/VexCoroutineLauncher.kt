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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Internal coroutine engine.
 *
 * Feature code never receives this type directly. [VexExecution] is the application-facing
 * architectural role; this class owns the Kotlin coroutine mechanics underneath that role.
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

    /** Sibling execution failure must not cancel unrelated managed work. */
    private val parentJob: Job =
        SupervisorJob()

    private val scope =
        CoroutineScope(
            dispatcher +
                parentJob +
                CoroutineName(
                    ROOT_COROUTINE_NAME,
                ),
        )

    private val registryLock = Any()
    private var acceptingWork = true

    private data class ManagedCoroutine(
        val job: Job,
    )

    /**
     * Active custody only.
     *
     * LinkedHashMap is intentional: exact ID lookup plus deterministic admission/inspection order.
     * Terminal executions leave this map but retain their final state in [executions].
     */
    private val active =
        LinkedHashMap<
            VexCoroutineRef,
            ManagedCoroutine
        >()

    private val mutableExecutions =
        MutableStateFlow<
            Map<VexCoroutineRef, VexCoroutineState>
        >(
            emptyMap(),
        )

    /**
     * Observable lifecycle state, including terminal disposition.
     *
     * Every published map is unmodifiable so consumers cannot mutate retained registry state by
     * casting the projection.
     */
    val executions:
        StateFlow<
            Map<VexCoroutineRef, VexCoroutineState>
        > =
        mutableExecutions.asStateFlow()

    fun launchOnce(
        ref: VexCoroutineRef,
        action: suspend () -> Unit,
    ): VexCoroutineHandle =
        launchManaged(
            ref = ref,
            restartOnFailure = false,
            action = action,
        )

    fun <Value> launchForCollect(
        ref: VexCoroutineRef,
        shouldRestart: Boolean,
        source: () -> Flow<Value>,
        onEach: suspend (Value) -> Unit,
    ): VexCoroutineHandle =
        launchManaged(
            ref = ref,
            restartOnFailure = shouldRestart,
        ) {
            source().collect { value ->
                onEach(value)
            }
        }

    fun forceStop(
        ref: VexCoroutineRef,
        reason: String,
    ): Boolean =
        forceStop(
            ref = ref,
            reason = reason,
            expectedJob = null,
        )

    suspend fun joinStop(
        ref: VexCoroutineRef,
        reason: String,
    ): Boolean =
        joinStop(
            ref = ref,
            reason = reason,
            expectedJob = null,
        )

    fun forceStopAll(
        reason: String,
    ) {
        synchronized(
            registryLock,
        ) {
            acceptingWork = false
        }

        parentJob.cancel(
            CancellationException(
                reason,
            ),
        )
    }

    suspend fun joinStopAll(
        reason: String,
    ) {
        synchronized(
            registryLock,
        ) {
            acceptingWork = false
        }

        parentJob.cancel(
            CancellationException(
                reason,
            ),
        )
        parentJob.join()
    }

    private fun launchManaged(
        ref: VexCoroutineRef,
        restartOnFailure: Boolean,
        action: suspend () -> Unit,
    ): VexCoroutineHandle {
        val mutableState =
            MutableStateFlow(
                VexCoroutineState(
                    ref = ref,
                    status =
                        VexCoroutineStatus.STARTING,
                    attempt = 0,
                    restartCount = 0,
                ),
            )

        lateinit var job: Job

        synchronized(
            registryLock,
        ) {
            check(
                acceptingWork &&
                    parentJob.isActive,
            ) {
                "VexExecutionRoot is stopped and cannot accept new work"
            }

            val existing =
                active[ref]

            check(
                existing == null ||
                    !existing.job.isActive,
            ) {
                "Coroutine ${ref.value} is already active"
            }

            job =
                scope.launch(
                    context =
                        CoroutineName(
                            ref.value,
                        ),
                    start =
                        CoroutineStart.LAZY,
                ) {
                    runManagedCoroutine(
                        ref = ref,
                        restartOnFailure =
                            restartOnFailure,
                        mutableState =
                            mutableState,
                        action = action,
                    )
                }

            active[ref] =
                ManagedCoroutine(
                    job = job,
                )

            updateRegistryLocked(
                mutableState.value,
            )
        }

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
                        VexCoroutineState(
                            ref = ref,
                            status =
                                VexCoroutineStatus
                                    .CANCELLED,
                            attempt =
                                mutableState
                                    .value
                                    .attempt,
                            restartCount =
                                mutableState
                                    .value
                                    .restartCount,
                            failureClass =
                                cause::class
                                    .simpleName,
                            failureMessage =
                                cause.message,
                        ),
                )
            }

            synchronized(
                registryLock,
            ) {
                if (
                    active[ref]?.job ===
                    job
                ) {
                    active.remove(
                        ref,
                    )
                }
            }
        }

        job.start()

        return object :
            VexCoroutineHandle {
            override val ref:
                VexCoroutineRef =
                ref

            override val state:
                StateFlow<
                    VexCoroutineState
                > =
                mutableState.asStateFlow()

            override fun forceStop(
                reason: String,
            ): Boolean =
                this@VexCoroutineLauncher
                    .forceStop(
                        ref = ref,
                        reason = reason,
                        expectedJob = job,
                    )

            override suspend fun joinStop(
                reason: String,
            ): Boolean =
                this@VexCoroutineLauncher
                    .joinStop(
                        ref = ref,
                        reason = reason,
                        expectedJob = job,
                    )

            override suspend fun awaitCompletion() {
                job.join()
            }
        }
    }

    /**
     * A handle acts on the exact Job it was created for. If the same source ref is later relaunched,
     * an old handle cannot accidentally stop the newer run.
     */
    private fun forceStop(
        ref: VexCoroutineRef,
        reason: String,
        expectedJob: Job?,
    ): Boolean {
        val job =
            synchronized(
                registryLock,
            ) {
                active[ref]
                    ?.job
                    ?.takeIf { activeJob ->
                        expectedJob == null ||
                            activeJob ===
                                expectedJob
                    }
            }
                ?: return false

        if (!job.isActive) {
            return false
        }

        job.cancel(
            CancellationException(
                reason,
            ),
        )
        return true
    }

    private suspend fun joinStop(
        ref: VexCoroutineRef,
        reason: String,
        expectedJob: Job?,
    ): Boolean {
        val job =
            synchronized(
                registryLock,
            ) {
                active[ref]
                    ?.job
                    ?.takeIf { activeJob ->
                        expectedJob == null ||
                            activeJob ===
                                expectedJob
                    }
            }
                ?: return false

        if (job.isActive) {
            job.cancel(
                CancellationException(
                    reason,
                ),
            )
        }

        job.join()
        return true
    }

    private suspend fun runManagedCoroutine(
        ref: VexCoroutineRef,
        restartOnFailure: Boolean,
        mutableState:
            MutableStateFlow<
                VexCoroutineState
            >,
        action: suspend () -> Unit,
    ) {
        var attempt = 0
        var restartCount = 0

        try {
            while (true) {
                attempt += 1

                publish(
                    mutableState =
                        mutableState,
                    state =
                        VexCoroutineState(
                            ref = ref,
                            status =
                                VexCoroutineStatus
                                    .RUNNING,
                            attempt = attempt,
                            restartCount =
                                restartCount,
                        ),
                )

                try {
                    /**
                     * The admitted operation remains one structured tree. If the referenced suspend
                     * function creates ordinary structured children, cancellation/join reaches them.
                     */
                    coroutineScope {
                        action()
                    }

                    publish(
                        mutableState =
                            mutableState,
                        state =
                            VexCoroutineState(
                                ref = ref,
                                status =
                                    VexCoroutineStatus
                                        .COMPLETED,
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
                    if (
                        !restartOnFailure ||
                        restartCount >=
                        maxCollectRestarts
                    ) {
                        publish(
                            mutableState =
                                mutableState,
                            state =
                                VexCoroutineState(
                                    ref = ref,
                                    status =
                                        VexCoroutineStatus
                                            .FAILED,
                                    attempt =
                                        attempt,
                                    restartCount =
                                        restartCount,
                                    failureClass =
                                        failure::class
                                            .simpleName,
                                    failureMessage =
                                        failure.message,
                                ),
                        )
                        return
                    }

                    restartCount += 1

                    publish(
                        mutableState =
                            mutableState,
                        state =
                            VexCoroutineState(
                                ref = ref,
                                status =
                                    VexCoroutineStatus
                                        .RESTARTING,
                                attempt = attempt,
                                restartCount =
                                    restartCount,
                                failureClass =
                                    failure::class
                                        .simpleName,
                                failureMessage =
                                    failure.message,
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
                    VexCoroutineState(
                        ref = ref,
                        status =
                            VexCoroutineStatus
                                .CANCELLED,
                        attempt = attempt,
                        restartCount =
                            restartCount,
                        failureClass =
                            cancelled::class
                                .simpleName,
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
                VexCoroutineState
            >,
        state: VexCoroutineState,
    ) {
        mutableState.value = state

        synchronized(
            registryLock,
        ) {
            updateRegistryLocked(
                state,
            )
        }
    }

    private fun updateRegistryLocked(
        state: VexCoroutineState,
    ) {
        mutableExecutions.value =
            java.util.Collections
                .unmodifiableMap(
                    LinkedHashMap(
                        mutableExecutions.value,
                    ).apply {
                        this[state.ref] =
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
