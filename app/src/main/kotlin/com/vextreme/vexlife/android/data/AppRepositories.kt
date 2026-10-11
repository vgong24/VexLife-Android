package com.vextreme.vexlife.android.data

import android.content.Context
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.data.loghistory.RoomLogHistoryRepository

/**
 * Small composition surface for data repositories used by the app.
 *
 * This is NOT a service locator for arbitrary behavior and there is intentionally no generic
 * BaseRepository<T>. Each data area earns its own repository contract from real domain needs.
 */
data class AppRepositories(
    val logHistory: LogHistoryRepository,
)

object AppRepositoryFactory {
    fun production(context: Context): AppRepositories {
        val database = VexLifeDatabaseFactory.get(context)

        return AppRepositories(
            logHistory = RoomLogHistoryRepository(
                dao = database.logHistoryDao(),
            ),
        )
    }
}
