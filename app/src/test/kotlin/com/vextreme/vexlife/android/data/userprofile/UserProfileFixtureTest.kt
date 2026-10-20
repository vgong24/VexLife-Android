package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileFixtureTest {
    @Test
    fun fixtureCanBeModifiedWithoutChangingItsBaseProfile() {
        val base = UserProfileFixture.build {
            profileRef("profile.victor")
            displayName("Victor")
        }

        val renamed = base.modify {
            displayName("Victor · VexLife")
            updatedAt(Instant.parse("2026-10-11T03:00:00Z"))
        }

        assertEquals("Victor", base.profile.displayName)
        assertEquals("Victor · VexLife", renamed.profile.displayName)
        assertEquals(base.profile.profileRef, renamed.profile.profileRef)
    }
}
