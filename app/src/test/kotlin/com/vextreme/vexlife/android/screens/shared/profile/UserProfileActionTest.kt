package com.vextreme.vexlife.android.screens.shared.profile

import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.runtime.AppActionRequest
import com.vextreme.vexlife.android.runtime.AppActionRouter
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileActionTest {
    @Test
    fun saveRegistrationPersistsTheSubmittedSnapshot() = runTest {
        val repository = InMemoryUserProfileRepository()
        val now = Instant.parse("2026-10-11T05:00:00Z")
        val router = AppActionRouter(
            registrations = userProfileActionRegistrations(
                repository = repository,
                scope = this,
                now = { now },
            ),
        )

        router.dispatch(
            AppActionRequest(
                action = com.vextreme.vexlife.android.runtime.AppActions.SaveUserProfile,
                source = UserProfileScreen.Save,
                submission = FormSubmission.of(
                    UserProfileScreen.DisplayName to "Victor",
                ),
            ),
        )
        advanceUntilIdle()

        val profile = repository.snapshot(
            com.vextreme.vexlife.android.data.userprofile.UserProfileRefs.local,
        )
        assertEquals("Victor", profile?.displayName)
        assertEquals(now, profile?.updatedAt)
    }
}
