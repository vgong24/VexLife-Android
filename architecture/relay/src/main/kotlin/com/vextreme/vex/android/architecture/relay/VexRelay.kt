package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small Source-of-Truth Relay for one state type.
 *
 * Processing is synchronous and serialized. Logging is submitted only after the state lock is
 * released so observability cannot become part of Relay correctness.
 */
abstract class VexRelay<State : VexState<State>>(
    val relayRef: VexLogRef,
    initialState: State,
    private val logSink: VexLogSink = VexLogSink.None,
) : VexObservableRelay<State> {
    private val lock = Any()
    private var processing = false

    private val mutableCurrentState =
        MutableStateFlow(initialState.deepCopy())

    override val currentState: StateFlow<State> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State = inputState.deepCopy()

    protected fun send(
        inputState: State,
        originRef: VexLogRef? = null,
    ): State {
        val processed =
            synchronized(lock) {
                processLocked(
                    inputState = inputState.deepCopy(),
                    originRef = originRef,
                )
            }

        processed.logs.forEach(logSink::log)

        return processed.state.deepCopy()
    }

    protected fun update(
        originRef: VexLogRef? = null,
        transform: (currentState: State) -> State,
    ): State {
        val processed =
            synchronized(lock) {
                val inputState =
                    transform(
                        mutableCurrentState.value.deepCopy(),
                    ).deepCopy()

                processLocked(
                    inputState = inputState,
                    originRef = originRef,
                )
            }

        processed.logs.forEach(logSink::log)

        return processed.state.deepCopy()
    }

    private fun processLocked(
        inputState: State,
        originRef: VexLogRef?,
    ): ProcessedState<State> {
        check(!processing) {
            "VexRelay cannot recursively send state from processState"
        }

        val currentState =
            mutableCurrentState.value.deepCopy()

        processing = true
        return try {
            val nextState =
                processState(
                    inputState.deepCopy(),
                    currentState.deepCopy(),
                ).deepCopy()

            val changed =
                nextState != currentState

            if (changed) {
                mutableCurrentState.value =
                    nextState.deepCopy()
            }

            ProcessedState(
                state = nextState.deepCopy(),
                logs =
                    listOf(
                        VexLogEvent(
                            sourceRef = relayRef,
                            eventRef = VexLogRef("relay.input.received"),
                            originRef = originRef,
                        ),
                        VexLogEvent(
                            sourceRef = relayRef,
                            eventRef =
                                VexLogRef(
                                    if (changed) {
                                        "relay.state.changed"
                                    } else {
                                        "relay.state.unchanged"
                                    },
                                ),
                            originRef = originRef,
                        ),
                    ),
            )
        } finally {
            processing = false
        }
    }

    private data class ProcessedState<State>(
        val state: State,
        val logs: List<VexLogEvent>,
    )
}

// [VXG RealForever]
