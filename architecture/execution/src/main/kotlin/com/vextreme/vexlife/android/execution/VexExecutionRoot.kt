package com.vextreme.vexlife.android.execution

import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Shared coroutine lifetime for one bounded runtime/session/service tree.
 *
 * WHAT THIS IS
 * VexExecutionRoot owns the shared SupervisorJob/scope boundary, admission while that lifetime is
 * alive, and terminal shutdown of the complete tree.
 *
 * WHAT IT RELATES TO
 * One root may host several sibling [VexExecution] owners. For direct VexExecution entry points it
 * also retains the foundation-owned execution scope associated with the already-existing live owner
 * object. RelationshipId -> Job(s) custody still lives inside VexExecution, not in another semantic
 * root registry.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * A shared root prevents each feature from inventing an uncontained/global coroutine scope while
 * preserving sibling failure isolation and one explicit terminal lifetime boundary.
 *
 * RESPONSIBILITY FENCE
 * Do not move semantic RelationshipId composition, feature verbs, semantic State, Logger history,
 * dependency resolution, provider retry policy, or feature cancellation policy into the root.
 * Direct-owner retention exists only to preserve VexExecution custody without requiring a wrapper
 * class. Root shutdown remains reserved for the owner of the entire shared lifetime.
 */
class VexExecutionRoot(
    dispatcher:
        CoroutineDispatcher =
        Dispatchers.Default,
    internal val maxCollectRestarts:
        Int =
        3,
    internal val collectRestartDelayMillis:
        Long =
        250L,
) {
    init {
        require(
            maxCollectRestarts >=
                0,
        ) {
            "maxCollectRestarts must be non-negative"
        }
        require(
            collectRestartDelayMillis >=
                0L,
        ) {
            "collectRestartDelayMillis must be non-negative"
        }
    }

    private val lock =
        Any()

    private var acceptingWork =
        true

    private val parentJob:
        Job =
        SupervisorJob()

    private val scope =
        CoroutineScope(
            dispatcher +
                parentJob +
                CoroutineName(
                    "vex.execution.root",
                ),
        )

    /*
     * Direct VexExecution entry points preserve owner-scoped custody for an already-existing live
     * owner whose lifetime is bounded by this root.
     *
     * IdentityHashMap is intentional: that live owner object is the scope. Callers reuse the same
     * stable owner for related execution; ephemeral Control/request objects are not execution owners.
     * This map is foundation plumbing only, not semantic identity.
     */
    private val directExecutions =
        IdentityHashMap<
            Any,
            DirectExecution
        >()

    internal fun directExecution(
        owner:
            Any,
    ):
        VexExecution =
        synchronized(
            lock,
        ) {
            check(
                acceptingWork &&
                    parentJob.isActive,
            ) {
                "VexExecutionRoot is stopped and cannot accept a direct execution owner"
            }

            directExecutions
                .getOrPut(
                    owner,
                ) {
                    DirectExecution(
                        this,
                    )
                }
        }

    /**
     * Create one lazy managed Job after verifying the shared lifetime still accepts work.
     *
     * async is used deliberately: failures remain attached to the Job occurrence rather than
     * becoming process-global uncaught coroutine exceptions. Semantic/runtime owners decide how an
     * operation failure is observed or logged.
     */
    internal fun createJob(
        name:
            String,
        action:
            suspend () -> Unit,
    ):
        Job =
        synchronized(
            lock,
        ) {
            check(
                acceptingWork &&
                    parentJob.isActive,
            ) {
                "VexExecutionRoot is stopped and cannot accept new work"
            }

            scope.async(
                context =
                    CoroutineName(
                        name,
                    ),
                start =
                    CoroutineStart.LAZY,
            ) {
                action()
            }
        }

    fun forceStopAll(
        reason:
            String =
            "execution root force stopped",
    ) {
        synchronized(
            lock,
        ) {
            acceptingWork =
                false
            directExecutions.clear()
        }

        parentJob.cancel(
            CancellationException(
                reason,
            ),
        )
    }

    suspend fun joinStopAll(
        reason:
            String =
            "execution root join stopped",
    ) {
        forceStopAll(
            reason,
        )
        parentJob.join()
    }

    /**
     * Foundation-owned implementation used only by VexExecution's direct reusable entry points.
     *
     * It exists so owner-scoped Job/Relationship custody remains exactly the same whether a caller
     * extends VexExecution for a genuinely sustained execution owner or calls the reusable
     * VexExecution.begin/observe surface directly. Product code must not name or construct this.
     */
    private class DirectExecution(
        root:
            VexExecutionRoot,
    ) :
        VexExecution(
            root,
        )
}

// [VXG RealForever]
