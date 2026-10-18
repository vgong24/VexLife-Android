package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.maps.InMemoryVexMapsRepository
import com.vextreme.vexlife.android.data.maps.distribution.InMemoryVexMapsDistributionRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.shell.AppShell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VexLifeSessionTest {
    @Test
    fun repeatedUiEntryDoesNotRestartTheLoggerSession() = runTest {
        val session = VexLifeSession(
            repositories = AppRepositories(
                logHistory = InMemoryLogHistoryRepository(),
                userProfile = InMemoryUserProfileRepository(),
                vexMaps = InMemoryVexMapsRepository(),
                vexMapsDistribution =
                    InMemoryVexMapsDistributionRepository(),
                vexStream = InMemoryVexStreamMusicRepository(),
                events = RelayEventsRepository(),
                eventsAttention = RelayEventsAttentionPreferenceRepository(),
                eventsAttentionLab = RelayEventsAttentionLabRepository(),
            ),
            scope = backgroundScope,
        )

        // A new Compose tree can invoke the app entry effect again after configuration recreation.
        // The retained VexLifeSession/runtime must treat that as the same semantic app session.
        session.runtime.appOpened()
        session.runtime.appOpened()

        assertEquals(
            1,
            session.logger.history.value.count { record ->
                record.kind == VexLogKind.AppOpened
            },
        )
    }

    @Test
    fun runtimeNavigationAndPresentationBelongToTheSessionNotTheComposable() = runTest {
        val session = VexLifeSession(
            repositories = AppRepositories(
                logHistory = InMemoryLogHistoryRepository(),
                userProfile = InMemoryUserProfileRepository(),
                vexMaps = InMemoryVexMapsRepository(),
                vexMapsDistribution =
                    InMemoryVexMapsDistributionRepository(),
                vexStream = InMemoryVexStreamMusicRepository(),
                events = RelayEventsRepository(),
                eventsAttention = RelayEventsAttentionPreferenceRepository(),
                eventsAttentionLab = RelayEventsAttentionLabRepository(),
            ),
            scope = backgroundScope,
        )

        session.runtime.activate(AppShell.Elements.DebugMenu)
        session.runtime.activate(DebugMenuScreen.ShowLog)

        val sameRuntimeAfterUiRecreation = session.runtime

        assertSame(session.runtime, sameRuntimeAfterUiRecreation)
        assertSame(DebugMenuScreen, sameRuntimeAfterUiRecreation.currentScreen)
        assertTrue(
            sameRuntimeAfterUiRecreation.presentation.isOpen(
                DebugMenuScreen.LogSheet,
            ),
        )
    }
}
