package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.ui.VexLifeSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureActionRegistrationsTest {
    @Test
    fun registeredScreensAutomaticallyContributeTheirActionOwners() = runTest {
        val registrations = VexLifeSession(
            repositories = AppRepositories(
                logHistory = InMemoryLogHistoryRepository(),
                userProfile = InMemoryUserProfileRepository(),
            ),
            scope = backgroundScope,
        ).appActions

        assertTrue(registrations[HomeScreen.VexStreamMusic].isEmpty())
        assertTrue(registrations[HomeScreen.TalkToVex].isEmpty())

        assertEquals(
            VexStreamMusicScreen.screenId,
            registrations[VexStreamMusicScreen.PlaceholderAction]
                .single()
                .ownerRef,
        )
        assertEquals(
            TalkToVexScreen.screenId,
            registrations[TalkToVexScreen.PlaceholderAction]
                .single()
                .ownerRef,
        )
    }
}
