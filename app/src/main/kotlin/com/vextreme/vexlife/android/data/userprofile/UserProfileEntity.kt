package com.vextreme.vexlife.android.data.userprofile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room schema for profile data.
 *
 * Explicit table/column names are the database compatibility contract. Do not use JSON annotations
 * such as @SerializedName/@SerialName as a substitute for SQL schema names.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "profile_ref")
    val profileRef: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

internal fun UserProfile.toEntity(): UserProfileEntity =
    UserProfileEntity(
        profileRef = profileRef,
        displayName = displayName,
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
    )

internal fun UserProfileEntity.toProfile(): UserProfile =
    UserProfile(
        profileRef = profileRef,
        displayName = displayName,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    )
