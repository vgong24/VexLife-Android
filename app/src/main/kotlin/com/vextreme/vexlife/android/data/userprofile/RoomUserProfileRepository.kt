package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserProfileRepository(
    private val dao: UserProfileDao,
) : UserProfileRepository {
    override fun observe(profileRef: String): Flow<UserProfile?> =
        dao.observe(profileRef).map { it?.toProfile() }

    override suspend fun upsert(profile: UserProfile) {
        dao.upsert(profile.toEntity())
    }

    override suspend fun updateDisplayName(
        profileRef: String,
        displayName: String,
        updatedAt: Instant,
    ) {
        val updatedRows = dao.updateDisplayName(
            profileRef = profileRef,
            displayName = displayName,
            updatedAtEpochMillis = updatedAt.toEpochMilli(),
        )
        check(updatedRows == 1) {
            "cannot update missing user profile: $profileRef"
        }
    }
}
