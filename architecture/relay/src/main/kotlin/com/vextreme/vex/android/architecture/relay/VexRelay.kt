package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smallest useful Vex Relay.
 *
 * Question answered:
 * "When this component receives new input, what is its one current reconciled relationship?"
 *
 * The Relay serializes input acceptance, derives output through [reconcile], and exposes one
 * read-only current [VexCompositeState].
 *
 * This first-principles layer intentionally does not own effects, retries, durability, operation
 * identity, UI lifecycle, or an event ledger. Those are added beneath the Relay only when a real
 * component needs them.
 */
class VexRelay<InputState, OutputState>(
    initialInput: InputState,
    private val reconcile: (InputState) -> OutputState,
) {
    private val lock = Any()
    private val mutableState = MutableStateFlow(
        VexCompositeState(
            input = initialInput,
            output = reconcile(initialInput),
        ),
    )

    val state: StateFlow<VexCompositeState<InputState, OutputState>> =
        mutableState.asStateFlow()

    val current: VexCompositeState<InputState, OutputState>
        get() = state.value

    fun accept(input: InputState): VexCompositeState<InputState, OutputState> =
        synchronized(lock) {
            commit(input)
        }

    fun evolveInput(
        transform: (InputState) -> InputState,
    ): VexCompositeState<InputState, OutputState> =
        synchronized(lock) {
            commit(transform(mutableState.value.input))
        }

    private fun commit(
        input: InputState,
    ): VexCompositeState<InputState, OutputState> {
        val current = mutableState.value
        val output = reconcile(input)

        if (input == current.input && output == current.output) {
            return current
        }

        return VexCompositeState(
            input = input,
            output = output,
            revision = current.revision + 1L,
        ).also { next ->
            mutableState.value = next
        }
    }
}

// [VXG RealForever]
