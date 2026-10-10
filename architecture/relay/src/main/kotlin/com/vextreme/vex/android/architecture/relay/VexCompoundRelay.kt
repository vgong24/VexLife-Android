package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class VexCompoundActivationState {
    INACTIVE,
    ACTIVATING,
    ACTIVE,
    DEACTIVATING,
}

/**
 * Relay for one organized component relationship with a separate input family and output family.
 *
 * Construction is inert. Dependency observation begins only after explicit [activate].
 */
abstract class VexCompoundRelay<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val relayRef: VexLogRef,
    initialInputState: InputState,
    initialOutputState: OutputState,
    private val logSink: VexLogSink = VexLogSink.None,
) : VexObservableRelay<VexCompoundState<InputState, OutputState>> {
    private val stateLock = Any()
    private var processing = false

    private val activationMutex = Mutex()
    private var dependencyHandles: List<VexCoroutineHandle> =
        emptyList()

    private val mutableActivationState =
        MutableStateFlow(
            VexCompoundActivationState.INACTIVE,
        )

    val activationState: StateFlow<VexCompoundActivationState> =
        mutableActivationState.asStateFlow()

    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    override val currentState: StateFlow<VexCompoundState<InputState, OutputState>> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    protected open fun relayDependencies(): List<VexRelayDependency<InputState>> =
        emptyList()

    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    suspend fun activate(
        executionOwner: VexExecutionOwner,
    ) {
        try {
            activationMutex.withLock {
                check(mutableActivationState.value == VexCompoundActivationState.INACTIVE) {
                    "VexCompoundRelay can only activate from INACTIVE"
                }

                mutableActivationState.value =
                    VexCompoundActivationState.ACTIVATING

                val stateBeforeActivation =
                    synchronized(stateLock) {
                        mutableCurrentState.value.deepCopy()
                    }

                val started =
                    mutableListOf<VexCoroutineHandle>()

                try {
                    for (dependency in relayDependencies()) {
                        started +=
                            dependency.observe(
                                executionOwner = executionOwner,
                                sendInput = { inputState, originRef ->
                                    send(
                                        inputState = inputState,
                                        originRef = originRef,
                                    )
                                },
                            )
                    }

                    dependencyHandles =
                        started.toList()

                    mutableActivationState.value =
                        VexCompoundActivationState.ACTIVE
                } catch (failure: Throwable) {
                    rollbackStartedDependencies(
                        started = started,
                        reason = "compound relay activation rollback",
                    )

                    dependencyHandles =
                        emptyList()

                    synchronized(stateLock) {
                        mutableCurrentState.value =
                            stateBeforeActivation.deepCopy()
                    }

                    mutableActivationState.value =
                        VexCompoundActivationState.INACTIVE

                    throw failure
                }
            }
        } catch (failure: Throwable) {
            logSink.log(
                VexLogEvent(
                    sourceRef = relayRef,
                    eventRef = VexLogRef("compound.activation.failed"),
                    fields =
                        mapOf(
                            "failureClass" to
                                (failure::class.simpleName ?: "unknown"),
                        ),
                ),
            )
            throw failure
        }

        logSink.log(
            VexLogEvent(
                sourceRef = relayRef,
                eventRef = VexLogRef("compound.activated"),
                fields =
                    mapOf(
                        "dependencyCount" to
                            dependencyHandles.size.toString(),
                    ),
            ),
        )
    }

    suspend fun deactivate(
        reason: String = "compound relay deactivated",
    ) {
        val deactivated =
            activationMutex.withLock {
                if (mutableActivationState.value == VexCompoundActivationState.INACTIVE) {
                    return@withLock false
                }

                check(mutableActivationState.value == VexCompoundActivationState.ACTIVE) {
                    "VexCompoundRelay can only deactivate from ACTIVE"
                }

                mutableActivationState.value =
                    VexCompoundActivationState.DEACTIVATING

                val handles =
                    dependencyHandles

                handles.forEach { handle ->
                    handle.forceStop(reason)
                }

                handles.forEach { handle ->
                    handle.join()
                }

                dependencyHandles =
                    emptyList()

                mutableActivationState.value =
                    VexCompoundActivationState.INACTIVE

                true
            }

        if (deactivated) {
            logSink.log(
                VexLogEvent(
                    sourceRef = relayRef,
                    eventRef = VexLogRef("compound.deactivated"),
                ),
            )
        }
    }

    protected fun send(
        inputState: InputState,
        originRef: VexLogRef? = null,
    ): VexCompoundState<InputState, OutputState> {
        val processed =
            synchronized(stateLock) {
                processLocked(
                    inputState = inputState.deepCopy(),
                    originRef = originRef,
                )
            }

        processed.logs.forEach(logSink::log)

        return processed.state.deepCopy()
    }

    private fun processLocked(
        inputState: InputState,
        originRef: VexLogRef?,
    ): ProcessedCompound<InputState, OutputState> {
        check(!processing) {
            "VexCompoundRelay cannot recursively send state from processStates"
        }

        val currentState =
            mutableCurrentState.value.deepCopy()

        processing = true
        return try {
            val processed =
                processStates(
                    inputState.deepCopy(),
                    currentState.deepCopy(),
                ).deepCopy()

            val meaningChanged =
                processed.inputState != currentState.inputState ||
                    processed.outputState != currentState.outputState

            if (!meaningChanged) {
                return ProcessedCompound(
                    state = currentState.deepCopy(),
                    logs =
                        listOf(
                            VexLogEvent(
                                sourceRef = relayRef,
                                eventRef = VexLogRef("relay.input.received"),
                                originRef = originRef,
                                fields =
                                    mapOf(
                                        "revision" to currentState.revision.toString(),
                                    ),
                            ),
                            VexLogEvent(
                                sourceRef = relayRef,
                                eventRef = VexLogRef("relay.state.unchanged"),
                                originRef = originRef,
                                fields =
                                    mapOf(
                                        "revision" to currentState.revision.toString(),
                                    ),
                            ),
                        ),
                )
            }

            val nextState =
                VexCompoundState(
                    inputState = processed.inputState.deepCopy(),
                    outputState = processed.outputState.deepCopy(),
                    revision = currentState.revision + 1L,
                )

            mutableCurrentState.value =
                nextState.deepCopy()

            ProcessedCompound(
                state = nextState.deepCopy(),
                logs =
                    listOf(
                        VexLogEvent(
                            sourceRef = relayRef,
                            eventRef = VexLogRef("relay.input.received"),
                            originRef = originRef,
                            fields =
                                mapOf(
                                    "revision" to currentState.revision.toString(),
                                ),
                        ),
                        VexLogEvent(
                            sourceRef = relayRef,
                            eventRef = VexLogRef("relay.state.changed"),
                            originRef = originRef,
                            fields =
                                mapOf(
                                    "revision" to nextState.revision.toString(),
                                ),
                        ),
                    ),
            )
        } finally {
            processing = false
        }
    }

    private suspend fun rollbackStartedDependencies(
        started: List<VexCoroutineHandle>,
        reason: String,
    ) {
        started.forEach { handle ->
            handle.forceStop(reason)
        }

        started.forEach { handle ->
            handle.join()
        }
    }

    private data class ProcessedCompound<
        InputState : VexInputState<InputState>,
        OutputState : VexOutputState<OutputState>,
    >(
        val state: VexCompoundState<InputState, OutputState>,
        val logs: List<VexLogEvent>,
    )
}

// [VXG RealForever]
