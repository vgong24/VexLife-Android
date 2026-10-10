package com.vextreme.vex.android.architecture.execution

import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Explicit stable identity for managed coroutine work.
 *
 * Do not derive this from Kotlin class/lambda names. R8/obfuscation may rename implementation
 * symbols; this ref is authored semantic/debug identity.
 */
@JvmInline
value class VexCoroutineRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexCoroutineRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

enum class VexCoroutineStatus {
    STARTING,
    RUNNING,
    RESTARTING,
    COMPLETED,
    CANCELLED,
    FAILED,
}

sealed interface VexCoroutineRestartPolicy {
    data object Never : VexCoroutineRestartPolicy

    data class OnFailure(
        val maxRestarts: Int,
        val delayMillis: Long = 0L,
    ) : VexCoroutineRestartPolicy {
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

data class VexCoroutineState(
    val ref: VexCoroutineRef,
    val status: VexCoroutineStatus,
    val attempt: Int,
    val restartCount: Int,
    val failureClass: String? = null,
    val failureMessage: String? = null,
)

interface VexAppScopeObserver {
    fun onCoroutineStateChanged(state: VexCoroutineState)

    fun onUnhandledScopeFailure(
        scopeName: String,
        failure: Throwable,
    )

    data object None : VexAppScopeObserver {
        override fun onCoroutineStateChanged(state: VexCoroutineState) = Unit

        override fun onUnhandledScopeFailure(
            scopeName: String,
            failure: Throwable,
        ) = Unit
    }
}

interface VexCoroutineHandle {
    val ref: VexCoroutineRef
    val state: StateFlow<VexCoroutineState>

    fun forceStop(
        reason: String = "force stopped",
    ): Boolean

    suspend fun join()
}

/**
 * Minimal execution-owner contract used by architecture modules such as Relay.
 *
 * VexAppScope is the application-wide implementation. A narrower future session/feature execution
 * owner may implement the same contract if a real lifetime earns it.
 */
interface VexExecutionOwner {
    val executions: StateFlow<Map<VexCoroutineRef, VexCoroutineState>>

    fun launchCoroutine(
        coroutineRef: VexCoroutineRef,
        restartPolicy: VexCoroutineRestartPolicy = VexCoroutineRestartPolicy.Never,
        block: suspend CoroutineScope.() -> Unit,
    ): VexCoroutineHandle

    fun forceStop(
        coroutineRef: VexCoroutineRef,
        reason: String = "force stopped",
    ): Boolean
}

/**
 * Application-level named coroutine owner.
 *
 * One parent SupervisorJob, one application CoroutineScope, named child work, observable state,
 * bounded restart, and explicit force-stop behavior.
 */
class VexAppScope(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val observer: VexAppScopeObserver = VexAppScopeObserver.None,
) : CoroutineScope,
    VexExecutionOwner {

    /** Child failure must not cancel this parent or unrelated sibling work. */
    private val parentJob: Job =
        SupervisorJob()

    private val exceptionHandler =
        CoroutineExceptionHandler { _, failure ->
            safelyObserve {
                observer.onUnhandledScopeFailure(
                    scopeName = APP_SCOPE_NAME,
                    failure = failure,
                )
            }
        }

    override val coroutineContext =
        dispatcher +
            parentJob +
            CoroutineName(APP_SCOPE_NAME) +
            exceptionHandler

    private val registryLock = Any()

    /**
     * Active work only. LinkedHashMap preserves stable diagnostic/start ordering.
     *
     * Access is guarded by registryLock; a coroutine Mutex is unnecessary for these short,
     * non-suspending registry critical sections.
     */
    private val jobMap =
        LinkedHashMap<VexCoroutineRef, Job>()

    private val mutableExecutions =
        MutableStateFlow<Map<VexCoroutineRef, VexCoroutineState>>(
            emptyMap(),
        )

    override val executions: StateFlow<Map<VexCoroutineRef, VexCoroutineState>> =
        mutableExecutions.asStateFlow()

    override fun launchCoroutine(
        coroutineRef: VexCoroutineRef,
        restartPolicy: VexCoroutineRestartPolicy,
        block: suspend CoroutineScope.() -> Unit,
    ): VexCoroutineHandle {
        val mutableState =
            MutableStateFlow(
                VexCoroutineState(
                    ref = coroutineRef,
                    status = VexCoroutineStatus.STARTING,
                    attempt = 0,
                    restartCount = 0,
                ),
            )

        lateinit var job: Job

        synchronized(registryLock) {
            val existing = jobMap[coroutineRef]
            check(existing == null || !existing.isActive) {
                "Coroutine " + coroutineRef.value + " is already active"
            }

            job =
                launch(
                    context =
                        CoroutineName(
                            APP_SCOPE_NAME + "/" + coroutineRef.value,
                        ),
                    start = CoroutineStart.LAZY,
                ) {
                    runManagedCoroutine(
                        coroutineRef = coroutineRef,
                        restartPolicy = restartPolicy,
                        mutableState = mutableState,
                        block = block,
                    )
                }

            jobMap[coroutineRef] = job
            publishLocked(mutableState.value)
        }

        job.invokeOnCompletion {
            synchronized(registryLock) {
                if (jobMap[coroutineRef] === job) {
                    jobMap.remove(coroutineRef)
                }
            }
        }

        job.start()

        return object : VexCoroutineHandle {
            override val ref: VexCoroutineRef =
                coroutineRef

            override val state: StateFlow<VexCoroutineState> =
                mutableState.asStateFlow()

            override fun forceStop(reason: String): Boolean =
                this@VexAppScope.forceStop(
                    coroutineRef = coroutineRef,
                    reason = reason,
                )

            override suspend fun join() {
                job.join()
            }
        }
    }

    /**
     * Force-stop never recursively relaunches the coroutine.
     * Restart only occurs for non-cancellation failure inside the same managed Job.
     */
    override fun forceStop(
        coroutineRef: VexCoroutineRef,
        reason: String,
    ): Boolean {
        val job =
            synchronized(registryLock) {
                jobMap[coroutineRef]
            }
                ?: return false

        job.cancel(
            CancellationException(reason),
        )
        return true
    }

    /** Terminal application shutdown. After this call VexAppScope cannot launch new work. */
    suspend fun stopApplication(
        reason: String = "application scope stopped",
    ) {
        parentJob.cancel(
            CancellationException(reason),
        )
        parentJob.join()
    }

    private suspend fun runManagedCoroutine(
        coroutineRef: VexCoroutineRef,
        restartPolicy: VexCoroutineRestartPolicy,
        mutableState: MutableStateFlow<VexCoroutineState>,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        var attempt = 0
        var restartCount = 0

        while (true) {
            attempt += 1

            publish(
                mutableState = mutableState,
                state =
                    VexCoroutineState(
                        ref = coroutineRef,
                        status = VexCoroutineStatus.RUNNING,
                        attempt = attempt,
                        restartCount = restartCount,
                    ),
            )

            try {
                block()

                publish(
                    mutableState = mutableState,
                    state =
                        VexCoroutineState(
                            ref = coroutineRef,
                            status = VexCoroutineStatus.COMPLETED,
                            attempt = attempt,
                            restartCount = restartCount,
                        ),
                )
                return
            } catch (cancelled: CancellationException) {
                publish(
                    mutableState = mutableState,
                    state =
                        VexCoroutineState(
                            ref = coroutineRef,
                            status = VexCoroutineStatus.CANCELLED,
                            attempt = attempt,
                            restartCount = restartCount,
                            failureClass = cancelled::class.simpleName,
                            failureMessage = cancelled.message,
                        ),
                )
                throw cancelled
            } catch (failure: Throwable) {
                if (!restartPolicy.canRestart(restartCount)) {
                    publish(
                        mutableState = mutableState,
                        state =
                            VexCoroutineState(
                                ref = coroutineRef,
                                status = VexCoroutineStatus.FAILED,
                                attempt = attempt,
                                restartCount = restartCount,
                                failureClass = failure::class.simpleName,
                                failureMessage = failure.message,
                            ),
                    )
                    return
                }

                restartCount += 1

                publish(
                    mutableState = mutableState,
                    state =
                        VexCoroutineState(
                            ref = coroutineRef,
                            status = VexCoroutineStatus.RESTARTING,
                            attempt = attempt,
                            restartCount = restartCount,
                            failureClass = failure::class.simpleName,
                            failureMessage = failure.message,
                        ),
                )

                val delayMillis =
                    (restartPolicy as VexCoroutineRestartPolicy.OnFailure)
                        .delayMillis

                if (delayMillis > 0L) {
                    delay(delayMillis)
                }
            }
        }
    }

    private fun publish(
        mutableState: MutableStateFlow<VexCoroutineState>,
        state: VexCoroutineState,
    ) {
        mutableState.value = state

        synchronized(registryLock) {
            publishLocked(state)
        }
    }

    private fun publishLocked(
        state: VexCoroutineState,
    ) {
        mutableExecutions.value =
            LinkedHashMap(mutableExecutions.value).apply {
                this[state.ref] = state
            }

        safelyObserve {
            observer.onCoroutineStateChanged(state)
        }
    }

    private fun safelyObserve(
        observation: () -> Unit,
    ) {
        runCatching(observation)
    }

    private fun VexCoroutineRestartPolicy.canRestart(
        restartCount: Int,
    ): Boolean =
        when (this) {
            VexCoroutineRestartPolicy.Never ->
                false

            is VexCoroutineRestartPolicy.OnFailure ->
                restartCount < maxRestarts
        }

    companion object {
        const val APP_SCOPE_NAME: String =
            "vex.app"
    }
}

// [VXG RealForever]
