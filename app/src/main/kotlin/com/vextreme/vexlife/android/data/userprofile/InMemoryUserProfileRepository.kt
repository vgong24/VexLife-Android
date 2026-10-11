package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Preview/test implementation of the same profile repository contract.
 */
class InMemoryUserProfileRepository(
    initialProfiles: List<UserProfile> = emptyList(),
) : UserProfileRepository {
    private val profiles = MutableStateFlow(
        initialProfiles.associateBy(UserProfile::profileRef),
    )

    override fun observe(profileRef: String): Flow<UserProfile?> =
        profiles.map { it[profileRef] }

    override suspend fun upsert(profile: UserProfile) {
        profiles.value = profiles.value + (profile.profileRef to profile)
    }

    override suspend fun updateDisplayName(
        profileRef: String,
        displayName: String,
        updatedAt: Instant,
    ) {
        val existing = profiles.value[profileRef] ?: return
        profiles.value = profiles.value + (
            profileRef to existing.copy(
                displayName = displayName,
                updatedAt = updatedAt,
            )
        )
    }

    fun snapshot(profileRef: String): UserProfile? =
        profiles.value[profileRef]
}
