package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant

/**
 * Chain-buildable profile data for previews/tests.
 *
 * Example:
 *
 * val victor = UserProfileFixture.build {
 *     profileRef("profile.local")
 *     displayName("Victor")
 * }
 *
 * val renamed = victor.modify {
 *     displayName("Victor · VexHome")
 * }
 */
data class UserProfileFixture(
    val profile: UserProfile,
) {
    fun modify(
        block: UserProfileFixtureBuilder.() -> Unit,
    ): UserProfileFixture =
        UserProfileFixtureBuilder(profile)
            .apply(block)
            .build()

    companion object {
        fun build(
            block: UserProfileFixtureBuilder.() -> Unit = {},
        ): UserProfileFixture =
            UserProfileFixtureBuilder(
                UserProfile(
                    profileRef = "profile.local",
                    displayName = "VexLife User",
                    updatedAt = Instant.parse("2026-10-11T02:00:00Z"),
                ),
            ).apply(block).build()
    }
}

class UserProfileFixtureBuilder internal constructor(
    seed: UserProfile,
) {
    private var profileRef: String = seed.profileRef
    private var displayName: String = seed.displayName
    private var updatedAt: Instant = seed.updatedAt

    fun profileRef(value: String) {
        require(value.isNotBlank()) { "profileRef must not be blank" }
        profileRef = value
    }

    fun displayName(value: String) {
        require(value.isNotBlank()) { "displayName must not be blank" }
        displayName = value
    }

    fun updatedAt(value: Instant) {
        updatedAt = value
    }

    fun build(): UserProfileFixture =
        UserProfileFixture(
            UserProfile(
                profileRef = profileRef,
                displayName = displayName,
                updatedAt = updatedAt,
            ),
        )
}
