package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Template for one scoped component relationship.
 *
 * A component subclasses VexRelay, owns one aggregate InputState and OutputState family, implements
 * [processLatestState], and exposes meaningful send methods for the pieces it accepts.
 *
 * Callers do not replace the full InputState. Subclasses use [send] so concurrent partial changes
 * are applied serially to the latest aggregate state.
 */
abstract class VexRelay<InputState : VexInputState, OutputState : VexOutputState>(
    initialInputState: InputState,
    private val inputSnapshotter: VexStateSnapshotter<InputState>,
    private val outputSnapshotter: VexStateSnapshotter<OutputState>,
) {
    private val lock = Any()
    private var processing = false

    // Copy immediately so caller mutation after construction cannot alter the initial Relay input.
    private val initialInputSeed: InputState =
        inputSnapshotter.snapshot(initialInputState)

    private val mutableState: MutableStateFlow<VexRelayState<InputState, OutputState>> by lazy {
        val retainedInput = inputSnapshotter.snapshot(initialInputSeed)
        val output = outputSnapshotter.snapshot(
            processLatestState(inputSnapshotter.snapshot(retainedInput)),
        )
        MutableStateFlow(
            VexRelayState(
                inputState = retainedInput,
                outputState = output,
                inputRevision = 0L,
                outputRevision = 0L,
            ),
        )
    }

    val state: StateFlow<VexRelayState<InputState, OutputState>> by lazy {
        DefensiveCopyStateFlow(
            source = mutableState.asStateFlow(),
            expose = ::expose,
        )
    }

    val current: VexRelayState<InputState, OutputState>
        get() = expose(mutableState.value)

    /**
     * The component's one synchronous input -> output relationship.
     *
     * Long-running work, repository effects, retries, or recursive Relay mutation do not belong
     * here. They can be added around the Relay later when a component genuinely needs them.
     */
    protected abstract fun processLatestState(inputState: InputState): OutputState

    /**
     * Atomically send one partial input change.
     *
     * The transform receives a defensive copy of the latest aggregate InputState while the Relay
     * owns the serialization lock. That means simultaneous screen/event/button updates cannot each
     * start from the same stale whole-state snapshot and erase one another.
     */
    protected fun send(
        transform: (InputState) -> InputState,
    ): VexRelayState<InputState, OutputState> =
        synchronized(lock) {
            check(!processing) {
                "VexRelay input cannot be sent recursively from processLatestState"
            }

            val current = mutableState.value
            val workingInput = inputSnapshotter.snapshot(current.inputState)
            val transformedInput = transform(workingInput)
            val nextInput = inputSnapshotter.snapshot(transformedInput)

            if (nextInput == current.inputState) {
                return@synchronized expose(current)
            }

            processing = true
            try {
                val nextOutput = outputSnapshotter.snapshot(
                    processLatestState(inputSnapshotter.snapshot(nextInput)),
                )
                val outputChanged = nextOutput != current.outputState

                val next = VexRelayState(
                    inputState = nextInput,
                    outputState = nextOutput,
                    inputRevision = current.inputRevision + 1L,
                    outputRevision =
                        current.outputRevision + if (outputChanged) 1L else 0L,
                )

                mutableState.value = retain(next)
                expose(next)
            } finally {
                processing = false
            }
        }

    private fun retain(
        relayState: VexRelayState<InputState, OutputState>,
    ): VexRelayState<InputState, OutputState> =
        relayState.copy(
            inputState = inputSnapshotter.snapshot(relayState.inputState),
            outputState = outputSnapshotter.snapshot(relayState.outputState),
        )

    private fun expose(
        relayState: VexRelayState<InputState, OutputState>,
    ): VexRelayState<InputState, OutputState> =
        relayState.copy(
            inputState = inputSnapshotter.snapshot(relayState.inputState),
            outputState = outputSnapshotter.snapshot(relayState.outputState),
        )
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class DefensiveCopyStateFlow<
    InputState : VexInputState,
    OutputState : VexOutputState,
>(
    private val source: StateFlow<VexRelayState<InputState, OutputState>>,
    private val expose: (
        VexRelayState<InputState, OutputState>,
    ) -> VexRelayState<InputState, OutputState>,
) : StateFlow<VexRelayState<InputState, OutputState>> {
    override val replayCache: List<VexRelayState<InputState, OutputState>>
        get() = listOf(value)

    override val value: VexRelayState<InputState, OutputState>
        get() = expose(source.value)

    override suspend fun collect(
        collector: FlowCollector<VexRelayState<InputState, OutputState>>,
    ): Nothing =
        source.collect(
            object : FlowCollector<VexRelayState<InputState, OutputState>> {
                override suspend fun emit(
                    value: VexRelayState<InputState, OutputState>,
                ) {
                    collector.emit(expose(value))
                }
            },
        )
}

// [VXG RealForever]
