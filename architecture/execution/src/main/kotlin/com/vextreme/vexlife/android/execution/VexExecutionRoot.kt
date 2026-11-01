package com.vextreme.vexlife.android.execution

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
 * One root may host several sibling [VexExecution] owners. Those owners retain their own Job and
 * RelationshipId custody; the root only creates Jobs inside the shared lifetime and can terminate
 * the whole tree when the rightful lifetime owner ends.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * A shared root prevents each feature from inventing an uncontained/global coroutine scope while
 * preserving sibling failure isolation and one explicit terminal lifetime boundary.
 *
 * RESPONSIBILITY FENCE
 * Do not move RelationshipId maps, per-feature active-work registries, semantic state, Logger
 * history, dependency resolution, provider retry policy, or feature cancellation policy into the
 * root. Stopping one VexExecution owner must not become root shutdown; root shutdown is reserved for
 * the owner of the entire shared lifetime.
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
}

// [VXG RealForever]
