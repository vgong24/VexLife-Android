package com.vextreme.vexlife.android.screens.shared.maps

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-local ownership gate for one app-private pack replacement transaction.
 *
 * Store instances are screen-scoped and may overlap briefly across Activity/configuration
 * recreation while an older blocking file copy is still unwinding. Sharing one gate per pack type
 * keeps those instances from mutating the same current/staging/backup files concurrently.
 */
internal class VexMapsPackReplacementGate {
    private val mutex =
        Mutex()
    private val recoveryLock =
        Any()
    private var recoveryCompleted =
        false

    fun recoverOnce(
        action: () -> Unit,
    ) {
        synchronized(recoveryLock) {
            if (recoveryCompleted) {
                return
            }

            action()
            recoveryCompleted = true
        }
    }

    suspend fun <T> withExclusiveReplacement(
        action: () -> T,
    ): T =
        mutex.withLock {
            action()
        }
}

// [VXG RealForever]
