package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileEntityTest {
    @Test
    fun roomMappingPreservesDomainProfile() {
        val profile = UserProfile(
            profileRef = "profile.victor",
            displayName = "Victor",
            updatedAt = Instant.parse("2026-10-11T03:00:00Z"),
        )

        assertEquals(profile, profile.toEntity().toProfile())
    }
}
