package com.vextreme.vexlife.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vextreme.vexlife.android.data.loghistory.LogHistoryDao
import com.vextreme.vexlife.android.data.loghistory.LogHistoryEntity
import com.vextreme.vexlife.android.data.userprofile.UserProfileDao
import com.vextreme.vexlife.android.data.userprofile.UserProfileEntity

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
        UserProfileEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VexLifeDatabase : RoomDatabase() {
    abstract fun logHistoryDao(): LogHistoryDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        /**
         * v1 contained only log_history. v2 adds the independent user_profile domain.
         *
         * New installs create v2 directly; this migration protects anyone who already ran the v1
         * database candidate on-device while the foundation was evolving.
         */
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        profile_ref TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        updated_at_epoch_millis INTEGER NOT NULL,
                        PRIMARY KEY(profile_ref)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
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
            )
                .addMigrations(VexLifeDatabase.Migration1To2)
                .build()
                .also { instance = it }
        }
}
