package com.vextreme.vexlife.android.cgm

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Small read-only semantic Family for the Partner circuit patient zero.
 *
 * CgmFamily does not receive transport bytes, decode Signals, or persist observations. It interprets
 * already-admitted Store truth into one semantic State and one outward DisplayState.
 */
internal class CgmFamily :
    VexFamily<
        CgmFamily,
        CgmFamily.State,
        CgmFamily.Control,
        CgmFamily.Effect
    >(
        initialState =
            State.Waiting,
    ) {

    sealed interface State :
        VexFamily.State<CgmFamily> {
        data object Waiting :
            State

        data class LatestAdmitted(
            val glucose:
                CgmPartner.Signal.Glucose,
            val storedObservationCount:
                Int,
        ) :
            State
    }

    /**
     * No product Control has been earned by this read-only CGM patient zero.
     *
     * The empty vocabulary is deliberate: the Debug Lab button simulates Partner input; it is not a
     * fake product Control asking Family to manufacture external truth.
     */
    sealed interface Control :
        VexFamily.Control<CgmFamily>

    /** No outbound CGM Effect is authored by the read-only patient zero. */
    sealed interface Effect :
        VexFamily.Effect<CgmFamily>

    sealed interface DisplayState :
        VexFamily.DisplayState<
            CgmFamily,
            State
        > {
        data object Waiting :
            DisplayState

        data class Reading(
            val mgDl:
                Int,
            val trend:
                CgmPartner.Trend,
            val sourceRef:
                String,
            val sequence:
                Long,
            val storedObservationCount:
                Int,
        ) :
            DisplayState
    }

    /**
     * Store truth has already crossed Partner decoding/observation/admission before entering Family.
     *
     * Family keeps Signal != State visible: the exact Partner Signal remains recognizable inside the
     * semantic State, but State additionally owns the application conclusion that this is the latest
     * admitted glucose currently available to this patient zero.
     */
    fun observeStore(
        snapshot:
            CgmStore.Snapshot,
    ):
        State {
        val next =
            snapshot
                .latestObservedGlucose
                ?.let { glucose ->
                    State.LatestAdmitted(
                        glucose =
                            glucose,
                        storedObservationCount =
                            snapshot
                                .observations
                                .size,
                    )
                }
                ?: State.Waiting

        return observe(
            next,
        )
    }

    override fun displayStateFor(
        state:
            State,
    ):
        DisplayState =
        when (state) {
            State.Waiting ->
                DisplayState.Waiting

            is State.LatestAdmitted ->
                DisplayState.Reading(
                    mgDl =
                        state.glucose
                            .mgDl,
                    trend =
                        state.glucose
                            .reportedTrend,
                    sourceRef =
                        state.glucose
                            .source
                            .deviceRef,
                    sequence =
                        state.glucose
                            .sequence,
                    storedObservationCount =
                        state
                            .storedObservationCount,
                )
        }

    override fun act(
        currentState:
            State,
        control:
            Control,
    ):
        VexFamily.Act<
            CgmFamily,
            State,
            Effect
        >? =
        error(
            "CgmFamily has no product Control in the read-only Partner patient zero",
        )
}

// [VXG RealForever][VEX-CGM-LABS][FAMILY-PATIENT-ZERO]
