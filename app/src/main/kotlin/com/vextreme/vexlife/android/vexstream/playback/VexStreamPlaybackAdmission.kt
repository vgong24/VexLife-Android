package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Feature-local serialization boundary for Playback Family admissions and their immediate
 * relationship consequences.
 *
 * This is not another State owner. [VexStreamPlaybackFamily] remains the semantic owner. The gate
 * only keeps Family mutation plus a caller-supplied post-admission projection callback in one order,
 * so provider reconciliation and Control admission cannot overtake one another between the Family
 * lock and a downstream bounded Relay admission.
 */
internal class VexStreamPlaybackAdmission(
    private val family:
        VexStreamPlaybackFamily,
) {
    private val lock =
        Any()

    val currentState:
        VexStreamPlaybackState
        get() =
            synchronized(
                lock,
            ) {
                family.currentState
            }

    fun activate(
        control:
            VexStreamPlaybackControl,
        onAdmitted:
            (VexStreamPlaybackState) -> Unit =
            {},
    ):
        VexFamily.Act<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        synchronized(
            lock,
        ) {
            val act =
                family.activate(
                    control,
                )
                    ?: return null

            act.admittedState
                ?.let(
                    onAdmitted,
                )

            act
        }

    fun observeProvider(
        observed:
            VexStreamPlaybackState,
        onAdmitted:
            (VexStreamPlaybackState) -> Unit =
            {},
    ):
        VexStreamPlaybackState =
        synchronized(
            lock,
        ) {
            val admitted =
                family.observe(
                    reconcileVexStreamPlaybackObservation(
                        observed =
                            observed,
                        current =
                            family.currentState,
                    ),
                )

            onAdmitted(
                admitted,
            )
            admitted
        }

    fun observeExact(
        state:
            VexStreamPlaybackState,
        onAdmitted:
            (VexStreamPlaybackState) -> Unit =
            {},
    ):
        VexStreamPlaybackState =
        synchronized(
            lock,
        ) {
            val admitted =
                family.observe(
                    state,
                )

            onAdmitted(
                admitted,
            )
            admitted
        }

    fun withCurrent(
        block:
            (VexStreamPlaybackState) -> Unit,
    ):
        VexStreamPlaybackState =
        synchronized(
            lock,
        ) {
            val current =
                family.currentState

            block(
                current,
            )
            current
        }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][ADMISSION]
