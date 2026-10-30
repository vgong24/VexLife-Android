package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState

internal data class VexMapsStyleSelection(
    val styleRef: String,
    val styleUri: String,
    val revision: Long,
) {
    init {
        require(styleRef.isNotBlank()) {
            "styleRef must not be blank"
        }
        require(styleUri.isNotBlank()) {
            "styleUri must not be blank"
        }
        require(revision > 0L) {
            "style selection revision must be positive"
        }
    }
}

internal data class VexMapsStyleFailure(
    val selection: VexMapsStyleSelection,
    val message: String,
)

internal enum class VexMapsStyleConvergenceStatus {
    UNOBSERVED,
    NEEDS_APPLY,
    APPLYING,
    APPLIED,
    FAILED,
}

internal data class VexMapsStyleConvergenceInputState(
    val selection: VexMapsStyleSelection? = null,
    val requested: VexMapsStyleSelection? = null,
    val loaded: VexMapsStyleSelection? = null,
    val failure: VexMapsStyleFailure? = null,
) : VexInputState<VexMapsStyleConvergenceInputState> {
    override fun latest(
        incoming:
            VexMapsStyleConvergenceInputState,
    ):
        VexMapsStyleConvergenceInputState {
        val latestSelection =
            incoming.selection
                ?: selection

        if (latestSelection == null) {
            return VexMapsStyleConvergenceInputState()
        }

        val selectionChanged =
            incoming.selection != null &&
                incoming.selection.revision !=
                selection?.revision

        val latestFailure =
            if (selectionChanged) {
                null
            } else {
                incoming.failure
                    ?: failure
            }

        return VexMapsStyleConvergenceInputState(
            selection =
                latestSelection.copy(),
            requested =
                if (selectionChanged) {
                    null
                } else {
                    (
                        incoming.requested
                            ?: requested
                    )?.copy()
                },
            loaded =
                if (selectionChanged) {
                    null
                } else {
                    (
                        incoming.loaded
                            ?: loaded
                    )?.copy()
                },
            failure =
                latestFailure?.copy(
                    selection =
                        latestFailure.selection.copy(),
                ),
        )
    }

    override fun deepCopy():
        VexMapsStyleConvergenceInputState =
        copy(
            selection = selection?.copy(),
            requested = requested?.copy(),
            loaded = loaded?.copy(),
            failure =
                failure?.copy(
                    selection =
                        failure.selection.copy(),
                ),
        )
}

internal data class VexMapsStyleConvergenceOutputState(
    val status:
        VexMapsStyleConvergenceStatus =
        VexMapsStyleConvergenceStatus.UNOBSERVED,
    val selection: VexMapsStyleSelection? = null,
    val failureMessage: String? = null,
) : VexOutputState<VexMapsStyleConvergenceOutputState> {
    override fun deepCopy():
        VexMapsStyleConvergenceOutputState =
        copy(
            selection = selection?.copy(),
        )
}

/**
 * Converges human style intent with what the MapLibre adapter has actually applied.
 *
 * This Relay does not call MapLibre, own the MapView, or persist preferences. It owns the meaning of
 * "selected", "being applied", "confirmed loaded", and "failed" for one style revision.
 */
internal class VexMapsStyleConvergenceRelay :
    VexCompoundRelay<
        VexMapsStyleConvergenceInputState,
        VexMapsStyleConvergenceOutputState
    >(
        relayRef =
            "relay.vexmaps.style.convergence",
        initialInputState =
            VexMapsStyleConvergenceInputState(),
        initialOutputState =
            VexMapsStyleConvergenceOutputState(),
    ) {
    fun styleSelected(
        styleRef: String,
        styleUri: String,
    ) {
        val previous =
            currentInput.selection
        val sameMeaning =
            previous?.styleRef == styleRef &&
                previous.styleUri == styleUri

        if (
            sameMeaning &&
            currentOutput.status !=
            VexMapsStyleConvergenceStatus.FAILED
        ) {
            return
        }

        send(
            VexMapsStyleConvergenceInputState(
                selection =
                    VexMapsStyleSelection(
                        styleRef = styleRef,
                        styleUri = styleUri,
                        revision =
                            (previous?.revision ?: 0L) +
                                1L,
                    ),
            ),
        )
    }

    fun applyStarted(
        selection: VexMapsStyleSelection,
    ) {
        send(
            VexMapsStyleConvergenceInputState(
                requested =
                    selection.copy(),
            ),
        )
    }

    fun styleLoaded(
        selection: VexMapsStyleSelection,
    ) {
        send(
            VexMapsStyleConvergenceInputState(
                loaded =
                    selection.copy(),
            ),
        )
    }

    fun currentApplyFailed(
        message: String,
    ) {
        val output =
            currentOutput
        val selection =
            output.selection
                ?: return

        if (
            output.status !=
            VexMapsStyleConvergenceStatus
                .APPLYING
        ) {
            return
        }

        send(
            VexMapsStyleConvergenceInputState(
                failure =
                    VexMapsStyleFailure(
                        selection =
                            selection.copy(),
                        message =
                            message.ifBlank {
                                "MapLibre style load failed"
                            },
                    ),
            ),
        )
    }

    override fun process(
        latestStateInput:
            VexMapsStyleConvergenceInputState,
        retainedState:
            VexCompoundState<
                VexMapsStyleConvergenceInputState,
                VexMapsStyleConvergenceOutputState
            >,
    ):
        VexMapsStyleConvergenceOutputState {
        val selection =
            latestStateInput.selection
                ?: return VexMapsStyleConvergenceOutputState()
        val requested =
            latestStateInput.requested
        val loaded =
            latestStateInput.loaded
        val failure =
            latestStateInput.failure

        fun VexMapsStyleSelection.matchesCurrent():
            Boolean =
            styleRef == selection.styleRef &&
                styleUri == selection.styleUri &&
                revision == selection.revision

        val currentFailure =
            failure
                ?.takeIf {
                    it.selection.matchesCurrent()
                }

        val status =
            when {
                loaded?.matchesCurrent() == true ->
                    VexMapsStyleConvergenceStatus
                        .APPLIED

                currentFailure != null ->
                    VexMapsStyleConvergenceStatus
                        .FAILED

                requested?.matchesCurrent() == true ->
                    VexMapsStyleConvergenceStatus
                        .APPLYING

                else ->
                    VexMapsStyleConvergenceStatus
                        .NEEDS_APPLY
            }

        return VexMapsStyleConvergenceOutputState(
            status =
                status,
            selection =
                selection.copy(),
            failureMessage =
                currentFailure?.message,
        )
    }

}

// [VXG RealForever]
