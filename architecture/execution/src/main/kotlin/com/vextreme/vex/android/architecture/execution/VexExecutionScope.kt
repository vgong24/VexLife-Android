package com.vextreme.vex.android.architecture.execution

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@JvmInline
value class VexExecutionName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexExecutionName must be non-blank"
        }
    }

    override fun toString(): String = value
}

enum class VexExecutionStatus {
    STARTING,
    RUNNING,
    RESTARTING,
    COMPLETED,
    CANCELLED,
    FAILED,
}

sealed interface VexRestartPolicy {
    data object Never : VexRestartPolicy

    data class OnFailure(
        val maxRestarts: Int,
        val delayMillis: Long = 0L,
    ) : VexRestartPolicy {
        init {
            require(maxRestarts >= 1) {
                "maxRestarts must be at least 1"
            }
            require(delayMillis >= 0L) {
                "delayMillis must be non-negative"
            }
        }
    }
}

data class VexExecutionState(
    val name: VexExecutionName,
    val status: VexExecutionStatus,
    val attempt: Int,
    val restartCount: Int,
    val failureClass: String? = null,
    val failureMessage: String? = null,
)

interface VexExecutionHandle {
    val name: VexExecutionName
    val state: StateFlow<VexExecutionState>

    fun cancel(
        reason: String = "cancelled",
    )

    suspend fun join()
}

/**
 * Named ownership boundary for asynchronous Kotlin work.
 *
 * The scope:
 * - gives each execution a stable human-readable name;
 * - isolates sibling failure with SupervisorJob;
 * - exposes current execution state for diagnostics/health;
 * - gives callers an explicit cancellation handle;
 * - supports opt-in bounded restart after failure.
 *
 * It does not make a coroutine into a semantic operation, durable identity, or retry authority.
 */
class VexExecutionScope private constructor(
    val name: VexExecutionName,
    parentScope: CoroutineScope,
) {
    private val registryLock = Any()
    private val supervisorJob =
        SupervisorJob(
            parentScope.coroutineContext[Job],
        )
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext.minusKey(Job) +
                supervisorJob +
                CoroutineName(name.value),
        )

    private val mutableExecutions =
        MutableStateFlow<Map<VexExecutionName, VexExecutionState>>(
            emptyMap(),
        )

    val executions: StateFlow<Map<VexExecutionName, VexExecutionState>> =
        mutableExecutions.asStateFlow()

    fun launch(
        executionName: VexExecutionName,
        restartPolicy: VexRestartPolicy = VexRestartPolicy.Never,
        onFailure: (VexExecutionState) -> Unit = {},
        block: suspend () -> Unit,
    ): VexExecutionHandle {
        synchronized(registryLock) {
            val existing = mutableExecutions.value[executionName]
            check(existing == null || existing.status.isTerminal()) {
                "Execution " + executionName.value + " is already active"
            }
        }

        val mutableState =
            MutableStateFlow(
                VexExecutionState(
                    name = executionName,
                    status = VexExecutionStatus.STARTING,
                    attempt = 0,
                    restartCount = 0,
                ),
            )

        updateRegistry(mutableState.value)

        lateinit var job: Job

        val handle =
            object : VexExecutionHandle {
                override val name: VexExecutionName =
                    executionName

                override val state: StateFlow<VexExecutionState> =
                    mutableState.asStateFlow()

                override fun cancel(reason: String) {
                    job.cancel(
                        CancellationException(reason),
                    )
                }

                override suspend fun join() {
                    job.join()
                }
            }

        job =
            scope.launch(
                CoroutineName(
                    name.value + "/" + executionName.value,
                ),
            ) {
                var attempt = 0
                var restartCount = 0

                while (true) {
                    attempt += 1
                    publish(
                        mutableState = mutableState,
                        state =
                            VexExecutionState(
                                name = executionName,
                                status = VexExecutionStatus.RUNNING,
                                attempt = attempt,
                                restartCount = restartCount,
                            ),
                    )

                    try {
                        block()

                        publish(
                            mutableState = mutableState,
                            state =
                                VexExecutionState(
                                    name = executionName,
                                    status = VexExecutionStatus.COMPLETED,
                                    attempt = attempt,
                                    restartCount = restartCount,
                                ),
                        )
                        return@launch
                    } catch (cancelled: CancellationException) {
                        publish(
                            mutableState = mutableState,
                            state =
                                VexExecutionState(
                                    name = executionName,
                                    status = VexExecutionStatus.CANCELLED,
                                    attempt = attempt,
                                    restartCount = restartCount,
                                    failureClass = cancelled::class.simpleName,
                                    failureMessage = cancelled.message,
                                ),
                        )
                        throw cancelled
                    } catch (failure: Throwable) {
                        val canRestart =
                            restartPolicy.canRestart(
                                restartCount = restartCount,
                            )

                        if (!canRestart) {
                            val failed =
                                VexExecutionState(
                                    name = executionName,
                                    status = VexExecutionStatus.FAILED,
                                    attempt = attempt,
                                    restartCount = restartCount,
                                    failureClass = failure::class.simpleName,
                                    failureMessage = failure.message,
                                )

                            publish(
                                mutableState = mutableState,
                                state = failed,
                            )
                            onFailure(failed)
                            return@launch
                        }

                        restartCount += 1

                        val restarting =
                            VexExecutionState(
                                name = executionName,
                                status = VexExecutionStatus.RESTARTING,
                                attempt = attempt,
                                restartCount = restartCount,
                                failureClass = failure::class.simpleName,
                                failureMessage = failure.message,
                            )

                        publish(
                            mutableState = mutableState,
                            state = restarting,
                        )
                        onFailure(restarting)

                        val restartDelay =
                            (restartPolicy as VexRestartPolicy.OnFailure)
                                .delayMillis

                        if (restartDelay > 0L) {
                            delay(restartDelay)
                        }
                    }
                }
            }

        return handle
    }

    fun cancelAll(
        reason: String = "execution scope cancelled",
    ) {
        supervisorJob.cancel(
            CancellationException(reason),
        )
    }

    private fun publish(
        mutableState: MutableStateFlow<VexExecutionState>,
        state: VexExecutionState,
    ) {
        mutableState.value = state
        updateRegistry(state)
    }

    private fun updateRegistry(
        state: VexExecutionState,
    ) {
        synchronized(registryLock) {
            mutableExecutions.value =
                mutableExecutions.value +
                    (state.name to state)
        }
    }

    private fun VexRestartPolicy.canRestart(
        restartCount: Int,
    ): Boolean =
        when (this) {
            VexRestartPolicy.Never ->
                false

            is VexRestartPolicy.OnFailure ->
                restartCount < maxRestarts
        }

    private fun VexExecutionStatus.isTerminal(): Boolean =
        this == VexExecutionStatus.COMPLETED ||
            this == VexExecutionStatus.CANCELLED ||
            this == VexExecutionStatus.FAILED

    companion object {
        fun root(
            name: VexExecutionName,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): VexExecutionScope =
            VexExecutionScope(
                name = name,
                parentScope =
                    CoroutineScope(
                        SupervisorJob() +
                            dispatcher +
                            CoroutineName(name.value + ".root"),
                    ),
            )

        fun childOf(
            name: VexExecutionName,
            parentScope: CoroutineScope,
        ): VexExecutionScope =
            VexExecutionScope(
                name = name,
                parentScope = parentScope,
            )
    }
}

// [VXG RealForever]
