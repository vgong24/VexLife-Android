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
 * The root owns terminal admission + shutdown only. Active semantic relationships belong to each
 * live [VexExecution] owner, not to a second root-level execution registry.
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
