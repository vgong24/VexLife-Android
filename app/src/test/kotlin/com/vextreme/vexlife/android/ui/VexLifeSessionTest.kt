package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.shell.AppShell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VexLifeSessionTest {
    @Test
    fun runtimeNavigationAndPresentationBelongToTheSessionNotTheComposable() = runTest {
        val session = VexLifeSession(
            repositories = AppRepositories(
                logHistory = InMemoryLogHistoryRepository(),
                userProfile = InMemoryUserProfileRepository(),
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
