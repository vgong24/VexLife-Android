package com.vextreme.vexlife.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vextreme.vexlife.android.data.loghistory.LogHistoryDao
import com.vextreme.vexlife.android.data.loghistory.LogHistoryEntity
import com.vextreme.vexlife.android.data.maps.VexMapsDao
import com.vextreme.vexlife.android.data.maps.VexMapsStateEntity
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDao
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraftEntity
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStopEntity
import com.vextreme.vexlife.android.data.userprofile.UserProfileDao
import com.vextreme.vexlife.android.data.userprofile.UserProfileEntity

/**
 * Android-local persistence substrate.
 *
 * Keep entity registration/migrations here. Do not let Screens depend on this database directly;
 * Screens consume repository contracts. Future backend/network sources can therefore coexist with or
 * replace local storage policy without changing UI code.
 */
@Database(
    entities = [
        LogHistoryEntity::class,
        UserProfileEntity::class,
        VexMapsStateEntity::class,
        VexMapsDistributionDraftEntity::class,
        VexMapsDistributionStopEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class VexLifeDatabase : RoomDatabase() {
    abstract fun logHistoryDao(): LogHistoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun vexMapsDao(): VexMapsDao
    abstract fun vexMapsDistributionDao(): VexMapsDistributionDao

    companion object {
        /**
         * v1 contained only log_history and used Kotlin-derived camelCase SQL column names.
         *
         * v2 does two compatibility changes:
         * 1. moves Log History to explicit snake_case SQL names without losing existing rows;
         * 2. adds the independent user_profile domain.
         *
         * Existing v1 candidate installs upgrade through this migration before later migrations run,
         * so naming cleanup is treated as real schema evolution rather than cosmetics.
         */
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE log_history_v2 (
                        database_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_ref TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        timestamp_epoch_millis INTEGER NOT NULL,
                        duration_since_previous_seconds INTEGER,
                        kind TEXT NOT NULL,
                        subject_ref TEXT NOT NULL,
                        action TEXT NOT NULL,
                        screen_before_id TEXT,
                        screen_after_id TEXT,
                        section INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO log_history_v2 (
                        database_id,
                        session_ref,
                        sequence,
                        timestamp_epoch_millis,
                        duration_since_previous_seconds,
                        kind,
                        subject_ref,
                        action,
                        screen_before_id,
                        screen_after_id,
                        section
                    )
                    SELECT
                        databaseId,
                        sessionRef,
                        sequence,
                        timestampEpochMillis,
                        durationSincePreviousSeconds,
                        kind,
                        subjectRef,
                        action,
                        screenBeforeId,
                        screenAfterId,
                        section
                    FROM log_history
                    """.trimIndent(),
                )

                db.execSQL("DROP TABLE log_history")
                db.execSQL("ALTER TABLE log_history_v2 RENAME TO log_history")

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_log_history_session_ref_sequence
                    ON log_history(session_ref, sequence)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_log_history_timestamp_epoch_millis
                    ON log_history(timestamp_epoch_millis)
                    """.trimIndent(),
                )

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

        /**
         * v3 persists optional structured diagnostic detail captured at explicit submit boundaries.
         * Existing rows remain valid with null details.
         */
        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE log_history ADD COLUMN details_json TEXT",
                )
            }
        }

        /**
         * v4 adds one bounded Maps-local state row. It stores no location history: only the latest
         * accepted fix plus the selected online map style.
         */
        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vex_maps_state (
                        state_ref TEXT NOT NULL,
                        last_location_latitude REAL,
                        last_location_longitude REAL,
                        last_location_accuracy_meters REAL,
                        last_location_captured_at_epoch_millis INTEGER,
                        online_style_ref TEXT,
                        updated_at_epoch_millis INTEGER NOT NULL,
                        PRIMARY KEY(state_ref)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v5 adds durable Distribution obligations as a separate Maps-owned persistence boundary.
         * Human input order remains source truth; optimized order is intentionally not stored here.
         */
        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vex_maps_distribution_draft (
                        draft_ref TEXT NOT NULL,
                        revision INTEGER NOT NULL,
                        return_to_start INTEGER NOT NULL,
                        updated_at_epoch_millis INTEGER NOT NULL,
                        PRIMARY KEY(draft_ref)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vex_maps_distribution_stop (
                        stop_ref TEXT NOT NULL,
                        draft_ref TEXT NOT NULL,
                        place_ref TEXT,
                        name TEXT NOT NULL,
                        subtitle TEXT,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        input_order INTEGER NOT NULL,
                        PRIMARY KEY(stop_ref),
                        FOREIGN KEY(draft_ref)
                            REFERENCES vex_maps_distribution_draft(draft_ref)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_vex_maps_distribution_stop_draft_ref
                    ON vex_maps_distribution_stop(draft_ref)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_vex_maps_distribution_stop_draft_ref_input_order
                    ON vex_maps_distribution_stop(draft_ref, input_order)
                    """.trimIndent(),
                )
            }
        }
    }
}

/**
 * Process-local singleton factory for the Room database.
 *
 * The singleton prevents accidental multiple database instances in the current app process. It does
 * not define repository policy; callers should normally enter through [AppRepositoryFactory].
 */
object VexLifeDatabaseFactory {
    private const val DATABASE_NAME = "vexlife.db"

    @Volatile
    private var instance: VexLifeDatabase? = null

    fun get(context: Context): VexLifeDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VexLifeDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(
                    VexLifeDatabase.migration1To2,
                    VexLifeDatabase.migration2To3,
                    VexLifeDatabase.migration3To4,
                    VexLifeDatabase.migration4To5,
                )
                .build()
                .also { instance = it }
        }
}
