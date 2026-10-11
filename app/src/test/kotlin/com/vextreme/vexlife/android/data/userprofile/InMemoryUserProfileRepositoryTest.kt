package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryUserProfileRepositoryTest {
    @Test
    fun upsertThenUpdateDisplayNameUsesTheSameRepositoryContract() = runTest {
        val repository = InMemoryUserProfileRepository()
        val profile = UserProfileFixture.build {
            profileRef("profile.victor")
            displayName("Victor")
        }.profile

        assertNull(repository.observe(profile.profileRef).first())

        repository.upsert(profile)
        assertEquals("Victor", repository.observe(profile.profileRef).first()?.displayName)

        repository.updateDisplayName(
            profileRef = profile.profileRef,
            displayName = "Victor · VexLife",
            updatedAt = Instant.parse("2026-10-11T03:00:00Z"),
        )

        assertEquals(
            "Victor · VexLife",
            repository.observe(profile.profileRef).first()?.displayName,
        )
    }
}
