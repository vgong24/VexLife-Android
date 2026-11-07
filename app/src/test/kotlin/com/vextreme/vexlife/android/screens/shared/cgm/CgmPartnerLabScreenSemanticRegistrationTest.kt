package com.vextreme.vexlife.android.screens.shared.cgm

import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import org.junit.Assert.assertSame
import org.junit.Test

class CgmPartnerLabScreenSemanticRegistrationTest {
    @Test
    fun cgmLabButtonAndScreenAreOwnedByTheRegisteredScreenGraph() {
        assertSame(
            CgmPartnerLabScreen.ReceivePacket,
            AppScreens.registry.element(
                CgmPartnerLabScreen.ReceivePacket.elementId,
            ),
        )
        assertSame(
            CgmPartnerLabScreen,
            AppScreens.registry
                .ownerScreenOf(
                    CgmPartnerLabScreen.ReceivePacket,
                ),
        )
    }

    @Test
    fun debugEntryNavigatesToTheRegisteredCgmPartnerLabScreen() {
        val intent =
            DebugMenuScreen
                .OpenCgmPartnerLab
                .intent as
                ElementIntent.NavigateTo

        assertSame(
            CgmPartnerLabScreen,
            intent.screen,
        )
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-SCREEN]
