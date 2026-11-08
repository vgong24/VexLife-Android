package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small retained-CURRENT convergence kernel shared by owners that admit partial updates.
 *
 * WHAT THIS OWNS
 * - one retained CURRENT value;
 * - one serialized non-suspending update boundary;
 * - update + exact retained CURRENT -> next CURRENT;
 * - semantic no-op suppression when the next value equals the retained value.
 *
 * WHAT THIS DOES NOT OWN
 * Relay dependency meaning, Partner identity, provider conversion, Family semantics, VexExecution
 * observation lifetime, persistence, defensive-copy policy, or partial-invalidation policy.
 *
 * A concrete owner decides what Update and Current mean. VexCompoundRelay keeps its full
 * relationship reevaluation law above this kernel; a Partner-facing Store may instead replace only
 * the facet named by an admitted Partner Signal while retaining the other already-current facets.
 *
 * Current values supplied to this kernel must be immutable or otherwise safely owned by the concrete
 * consumer. Owners that require defensive copies keep that policy at their own boundary.
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

    /**
     * Calculate one next CURRENT from one admitted update and the exact retained CURRENT.
     *
     * This method must not suspend or recursively admit another update. IO/provider waits belong
     * before this boundary.
     */
    protected abstract fun converge(
        update:
            Update,
        current:
            Current,
    ):
        Current

    /** Serialize and admit one update, publishing only when retained meaning changes. */
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
