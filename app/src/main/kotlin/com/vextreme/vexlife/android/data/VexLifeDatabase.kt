package com.vextreme.vexlife.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vextreme.vexlife.android.data.loghistory.LogHistoryDao
import com.vextreme.vexlife.android.data.loghistory.LogHistoryEntity

/**
 * Android-local persistence substrate.
 *
 * Keep entity registration/migrations here. Do not let pages depend on this database directly;
 * pages consume repository contracts. Future backend/network sources can therefore coexist with or
 * replace local storage policy without changing UI code.
 */
@Database(
    entities = [
        LogHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VexLifeDatabase : RoomDatabase() {
    abstract fun logHistoryDao(): LogHistoryDao
}

object VexLifeDatabaseFactory {
    private const val DatabaseName = "vexlife.db"

    @Volatile
    private var instance: VexLifeDatabase? = null

    fun get(context: Context): VexLifeDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VexLifeDatabase::class.java,
                DatabaseName,
            ).build().also { instance = it }
        }
}
