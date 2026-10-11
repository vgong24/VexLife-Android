package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vex.talkToVexActionRegistrations
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.vexStreamMusicActionRegistrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureActionRegistrationsTest {
    private val registrations = AppActionRegistrations.combine(
        vexStreamMusicActionRegistrations(),
        talkToVexActionRegistrations(),
    )

    @Test
    fun homeNavigationButtonsDoNotPretendToOwnAppEffects() {
        assertTrue(registrations[HomeScreen.VexStreamMusic].isEmpty())
        assertTrue(registrations[HomeScreen.TalkToVex].isEmpty())
    }

    @Test
    fun destinationInvokeButtonsResolveBackToTheirFeatureOwners() {
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
