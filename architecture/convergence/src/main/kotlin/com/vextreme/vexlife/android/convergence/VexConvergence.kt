package com.vextreme.vexlife.android.convergence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Retained-CURRENT convergence mechanics for one rightful owner.
 *
 * The concrete owner supplies Update meaning and Current meaning. This root only serializes each
 * admitted Update against the exact retained Current and suppresses equal next meaning.
 */
abstract class VexConvergence<
    Update,
    Current,
>(
    initialCurrent:
        Current,
) {
    private val lock =
        Any()

    private var processing =
        false

    private val mutableConvergedStates =
        MutableStateFlow(
            initialCurrent,
        )

    protected val convergedStates:
        StateFlow<
            Current
        > =
        mutableConvergedStates
            .asStateFlow()

    protected val convergedCurrent:
        Current
        get() =
            synchronized(
                lock,
            ) {
                mutableConvergedStates.value
            }

    protected abstract fun converge(
        update:
            Update,
        current:
            Current,
    ):
        Current

    protected fun admit(
        update:
            Update,
    ):
        Current =
        synchronized(
            lock,
        ) {
            check(
                !processing,
            ) {
                "VexConvergence cannot recursively admit an update from converge()"
            }

            val current =
                mutableConvergedStates.value

            processing =
                true
            try {
                val next =
                    converge(
                        update =
                            update,
                        current =
                            current,
                    )

                if (
                    next !=
                    current
                ) {
                    mutableConvergedStates.value =
                        next
                }

                mutableConvergedStates.value
            } finally {
                processing =
                    false
            }
        }
}

// [VXG RealForever][VEX-CONVERGENCE]
