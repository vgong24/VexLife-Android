package com.vextreme.vex.android.architecture.execution

import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

private fun VexCoroutineStatus.isTerminal(): Boolean =
    this == VexCoroutineStatus.COMPLETED ||
        this == VexCoroutineStatus.CANCELLED ||
        this == VexCoroutineStatus.FAILED

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
    private val logSink: VexLogSink = VexLogSink.None,
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

            logSink.log(
                VexLogEvent(
                    sourceRef = APP_SCOPE_LOG_REF,
                    eventRef = VexLogRef("scope.unhandled.failure"),
                    fields =
                        mapOf(
                            "failureClass" to
                                (failure::class.simpleName ?: "unknown"),
                        ),
                ),
            )
        }

    override val coroutineContext =
        dispatcher +
            parentJob +
            CoroutineName(APP_SCOPE_NAME) +
            exceptionHandler

    private val registryLock = Any()
    private var acceptingWork = true

    /** Active jobs only; registryLock owns every access. */
    private val jobMap =
        LinkedHashMap<VexCoroutineRef, Job>()

    private val mutableExecutions =
        MutableStateFlow<Map<VexCoroutineRef, VexCoroutineState>>(
            emptyMap(),
        )

    override val executions: StateFlow<Map<VexCoroutineRef, VexCoroutineState>> =
        DefensiveExecutionRegistryFlow(
            mutableExecutions.asStateFlow(),
        )

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
            check(acceptingWork && parentJob.isActive) {
                "VexAppScope is stopped and cannot accept new work"
            }

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
            updateRegistryLocked(mutableState.value)
        }

        notifyStateChanged(mutableState.value)

        job.invokeOnCompletion { cause ->
            val fallbackState =
                if (
                    cause is CancellationException &&
                    !mutableState.value.status.isTerminal()
                ) {
                    VexCoroutineState(
                        ref = coroutineRef,
                        status = VexCoroutineStatus.CANCELLED,
                        attempt = mutableState.value.attempt,
                        restartCount = mutableState.value.restartCount,
                        failureClass = cause::class.simpleName,
                        failureMessage = cause.message,
                    )
                } else {
                    null
                }

            if (fallbackState != null) {
                publish(
                    mutableState = mutableState,
                    state = fallbackState,
                )
            }

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

    override fun forceStop(
        coroutineRef: VexCoroutineRef,
        reason: String,
    ): Boolean {
        val job =
            synchronized(registryLock) {
                jobMap[coroutineRef]
            }
                ?: return false

        logSink.log(
            VexLogEvent(
                sourceRef = APP_SCOPE_LOG_REF,
                eventRef = VexLogRef("coroutine.cancel.requested"),
                originRef = VexLogRef(coroutineRef.value),
            ),
        )

        job.cancel(
            CancellationException(reason),
        )
        return true
    }

    /**
     * Stop admission first, then cancel/join the whole supervised tree.
     *
     * launchCoroutine and this admission fence share registryLock, so a launch either becomes an
     * admitted child that shutdown cancels or it fails closed after admission stops.
     */
    suspend fun stopApplication(
        reason: String = "application scope stopped",
    ) {
        synchronized(registryLock) {
            acceptingWork = false
        }

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

        try {
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
                    /**
                     * The block receives the managed attempt's structured scope, not VexAppScope.
                     * Any nested launch is therefore a descendant of this named execution.
                     */
                    coroutineScope {
                        block()
                    }

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
        }
    }

    private fun publish(
        mutableState: MutableStateFlow<VexCoroutineState>,
        state: VexCoroutineState,
    ) {
        mutableState.value = state

        synchronized(registryLock) {
            updateRegistryLocked(state)
        }

        notifyStateChanged(state)
    }

    private fun updateRegistryLocked(
        state: VexCoroutineState,
    ) {
        mutableExecutions.value =
            LinkedHashMap(mutableExecutions.value).apply {
                this[state.ref] = state
            }
    }

    /**
     * External observers never execute under registryLock.
     */
    private fun notifyStateChanged(
        state: VexCoroutineState,
    ) {
        safelyObserve {
            observer.onCoroutineStateChanged(state)
        }

        logSink.log(
            VexLogEvent(
                sourceRef = APP_SCOPE_LOG_REF,
                eventRef = state.status.logEventRef(),
                originRef = VexLogRef(state.ref.value),
                fields =
                    buildMap {
                        put(
                            "attempt",
                            state.attempt.toString(),
                        )
                        put(
                            "restartCount",
                            state.restartCount.toString(),
                        )
                        state.failureClass?.let { failureClass ->
                            put(
                                "failureClass",
                                failureClass,
                            )
                        }
                    },
            ),
        )
    }

    private fun safelyObserve(
        observation: () -> Unit,
    ) {
        runCatching(observation)
    }

    private fun VexCoroutineStatus.logEventRef(): VexLogRef =
        when (this) {
            VexCoroutineStatus.STARTING ->
                VexLogRef("coroutine.starting")
            VexCoroutineStatus.RUNNING ->
                VexLogRef("coroutine.running")
            VexCoroutineStatus.RESTARTING ->
                VexLogRef("coroutine.restarting")
            VexCoroutineStatus.COMPLETED ->
                VexLogRef("coroutine.completed")
            VexCoroutineStatus.CANCELLED ->
                VexLogRef("coroutine.cancelled")
            VexCoroutineStatus.FAILED ->
                VexLogRef("coroutine.failed")
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

        val APP_SCOPE_LOG_REF: VexLogRef =
            VexLogRef("execution.vex.app")
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class DefensiveExecutionRegistryFlow(
    private val source: StateFlow<Map<VexCoroutineRef, VexCoroutineState>>,
) : StateFlow<Map<VexCoroutineRef, VexCoroutineState>> {
    override val replayCache: List<Map<VexCoroutineRef, VexCoroutineState>>
        get() = listOf(value)

    override val value: Map<VexCoroutineRef, VexCoroutineState>
        get() = LinkedHashMap(source.value)

    override suspend fun collect(
        collector: FlowCollector<Map<VexCoroutineRef, VexCoroutineState>>,
    ): Nothing =
        source.collect { value ->
            collector.emit(
                LinkedHashMap(value),
            )
        }
}

// [VXG RealForever]
