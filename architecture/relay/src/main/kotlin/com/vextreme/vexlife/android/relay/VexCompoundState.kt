package com.vextreme.vexlife.android.relay

/**
 * Retained totality of one Compound Relay.
 *
 * [inputState] is the latest coherent input family retained from its dependencies.
 * [outputState] is the current derived meaning exposed to consumers.
 * [revision] advances only when either retained input meaning or derived output meaning changes.
 *
 * This revision is current-state lineage, not a substitute for a durable event ledger.
 */
data class VexCompoundState<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    val inputState: InputState,
    val outputState: OutputState,
    val revision: Long = 0L,
) : VexState<VexCompoundState<InputState, OutputState>> {
    init {
        require(revision >= 0L) { "revision must be non-negative" }
    }

    override fun deepCopy(): VexCompoundState<InputState, OutputState> {
        // STEP 1 — ask each semantic branch for an ownership-safe snapshot.
        // Mutable-backed branches detach here; immutable branches return themselves.
        val inputSnapshot =
            inputState.deepCopy()
        val outputSnapshot =
            outputState.deepCopy()

        // STEP 2 — the Compound wrapper itself is immutable. If neither child needed detachment,
        // this exact wrapper is already safe for the new owner and allocating another wrapper would
        // add no isolation at all.
        val childrenAlreadyShareable =
            inputSnapshot === inputState &&
                outputSnapshot === outputState

        if (childrenAlreadyShareable) {
            return this
        }

        // STEP 3 — at least one child detached, so return one wrapper containing those safe snapshots.
        return copy(
            inputState = inputSnapshot,
            outputState = outputSnapshot,
        )
    }
}

// [VXG RealForever]
