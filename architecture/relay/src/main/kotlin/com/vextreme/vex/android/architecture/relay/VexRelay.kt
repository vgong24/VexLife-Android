package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import com.vextreme.vex.android.architecture.observability.logSafely
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
    override val relayRef: VexRelayRef,
    initialState: State,
    private val logSink: VexLogSink = VexLogSink.None,
) : VexObservableRelay<State> {
    private val lock = Any()
    private var mutationInProgress = false
    private var transitionSequence = 0L

    private val mutableCurrentState =
        MutableStateFlow(initialState.deepCopy())

    override val currentState: StateFlow<State> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * Bounded semantic admission/derivation only.
     *
     * Do not dispatch work or mutate another Relay from this state-lock boundary. Cross-owner
     * relationships belong in explicit dependencies/operations so topology stays perceivable.
     */
    protected open fun processState(
        inputState: State,
        currentState: State,
    ): State = inputState.deepCopy()

    protected fun send(
        inputState: State,
        originRef: VexLogRef? = null,
    ): State {
        val processed =
            mutateLocked {
                processLocked(
                    inputState = inputState.deepCopy(),
                    originRef = originRef,
                )
            }

        processed.logs.forEach { event ->
            logSink.logSafely(event)
        }

        return processed.state
    }

    /**
     * The transform is part of this Relay's mutation transaction.
     *
     * It must derive one next state from the supplied snapshot; it must not call another mutation
     * method on this Relay or dispatch cross-owner effects.
     */
    protected fun update(
        originRef: VexLogRef? = null,
        transform: (currentState: State) -> State,
    ): State {
        val processed =
            mutateLocked {
                val inputState =
                    transform(
                        mutableCurrentState.value.deepCopy(),
                    ).deepCopy()

                processLocked(
                    inputState = inputState,
                    originRef = originRef,
                )
            }

        processed.logs.forEach { event ->
            logSink.logSafely(event)
        }

        return processed.state
    }

    private fun <Result> mutateLocked(
        mutation: () -> Result,
    ): Result =
        synchronized(lock) {
            check(!mutationInProgress) {
                "VexRelay cannot recursively mutate state from update/processState"
            }

            mutationInProgress = true
            try {
                mutation()
            } finally {
                mutationInProgress = false
            }
        }

    private fun processLocked(
        inputState: State,
        originRef: VexLogRef?,
    ): ProcessedState<State> {
        val currentState =
            mutableCurrentState.value.deepCopy()

        return run {
            val nextState =
                processState(
                    inputState.deepCopy(),
                    currentState.deepCopy(),
                ).deepCopy()

            val sameMeaning =
                nextState.hasSameMeaningAs(currentState)

            check(nextState != currentState || sameMeaning) {
                "VexState semantic equality cannot distinguish values that Kotlin equals() " +
                    "collapses because StateFlow uses equals() for conflation"
            }

            check(transitionSequence < Long.MAX_VALUE) {
                "VexRelay transitionSequence exhausted"
            }
            val nextTransitionSequence =
                transitionSequence + 1L
            val completedTransitionSequence =
                nextTransitionSequence.toString()

            val changed =
                !sameMeaning
            val retainedState =
                if (changed) {
                    nextState.deepCopy()
                } else {
                    null
                }
            val exposedState =
                if (changed) {
                    nextState.deepCopy()
                } else {
                    currentState.deepCopy()
                }

            val logs =
                listOf(
                    VexLogEvent(
                        sourceRef = VexLogRef(relayRef.value),
                        eventRef = VexLogRef("relay.input.received"),
                        originRef = originRef,
                        fields =
                            mapOf(
                                "transitionSequence" to completedTransitionSequence,
                            ),
                    ),
                    VexLogEvent(
                        sourceRef = VexLogRef(relayRef.value),
                        eventRef =
                            VexLogRef(
                                if (changed) {
                                    "relay.state.changed"
                                } else {
                                    "relay.state.unchanged"
                                },
                            ),
                        originRef = originRef,
                        fields =
                            mapOf(
                                "transitionSequence" to completedTransitionSequence,
                            ),
                    ),
                )

            if (retainedState != null) {
                mutableCurrentState.value =
                    retainedState
            }
            transitionSequence =
                nextTransitionSequence

            ProcessedState(
                state = exposedState,
                logs = logs,
            )
        }
    }

    private data class ProcessedState<State>(
        val state: State,
        val logs: List<VexLogEvent>,
    )
}

// [VXG RealForever]
