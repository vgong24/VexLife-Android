package com.vextreme.vexlife.android.data.userprofile

import java.time.Instant

/**
 * App/domain representation of one user profile.
 *
 * This is intentionally small. Future VexHome/VexStream/device relationships are NOT embedded here;
 * those relationships deserve their own owner once pairing/identity/transport rules are known.
 */
data class UserProfile(
    val profileRef: String,
    val displayName: String,
    val updatedAt: Instant,
)
