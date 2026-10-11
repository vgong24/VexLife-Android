package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
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
                vexStream = InMemoryVexStreamMusicRepository(),
                events = RelayEventsRepository(),
                eventsAttention = RelayEventsAttentionPreferenceRepository(),
                eventsAttentionLab = RelayEventsAttentionLabRepository(),
            ),
            scope = backgroundScope,
        ).appActions

        assertTrue(registrations[HomeScreen.VexStreamMusic].isEmpty())
        assertTrue(registrations[HomeScreen.TalkToVex].isEmpty())

        assertEquals(
            VexStreamMusicScreen.screenId,
            registrations[VexStreamMusicScreen.RefreshCatalog]
                .single()
                .ownerRef,
        )
        assertTrue(
            registrations[VexStreamMusicScreen.TogglePlayback]
                .isEmpty(),
        )
        assertEquals(
            TalkToVexScreen.screenId,
            registrations[TalkToVexScreen.PlaceholderAction]
                .single()
                .ownerRef,
        )
    }
}
