package com.vextreme.vexlife.android.screens.shared.maps

import java.io.File

/**
 * Recovers the last committed app-private pack around an interrupted replacement transaction.
 *
 * A replacement is committed only when the staging file has reached [current]. If [current] is
 * absent but [backup] remains, the process stopped after moving the previous committed pack aside,
 * so that previous pack is restored. A staging file is never promoted during recovery because it
 * represents work that did not cross the commit boundary.
 */
internal fun recoverVexMapsInterruptedPackReplacement(
    current: File,
    staging: File,
    backup: File,
) {
    if (current.exists()) {
        staging.delete()
        backup.delete()
        return
    }

    if (backup.exists()) {
        check(
            backup.renameTo(
                current,
            ),
        ) {
            "previous VexMaps pack could not be restored after interrupted replacement"
        }
    }

    staging.delete()

    if (current.exists()) {
        backup.delete()
    }
}

// [VXG RealForever]
