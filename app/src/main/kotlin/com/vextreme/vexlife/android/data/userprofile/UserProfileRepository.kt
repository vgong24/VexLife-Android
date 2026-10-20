package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Domain-specific repository contract for profile identity/presentation.
 *
 * A page or future "User Hub" can depend on this without knowing whether profile state is local,
 * remote, cached, or synchronized.
 */
interface UserProfileRepository {
    fun observe(profileRef: String): Flow<UserProfile?>

    suspend fun upsert(profile: UserProfile)

    /**
     * Updates an existing profile and fails if [profileRef] does not exist.
     *
     * Use [upsert] when "create if missing" is the intended operation.
     */
    suspend fun updateDisplayName(
        profileRef: String,
        displayName: String,
        updatedAt: Instant,
    )
}
