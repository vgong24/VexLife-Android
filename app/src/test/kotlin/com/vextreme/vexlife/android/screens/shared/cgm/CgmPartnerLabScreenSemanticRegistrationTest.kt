package com.vextreme.vexlife.android.screens.shared.cgm

import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import org.junit.Assert.assertSame
import org.junit.Test

class CgmPartnerLabScreenSemanticRegistrationTest {
    @Test
    fun cgmLabButtonAndScreenAreOwnedByTheRegisteredScreenGraph() {
        listOf(
            CgmPartnerLabScreen.ReceivePacket,
            CgmPartnerLabScreen.ReceiveHttpResponse,
        ).forEach { element ->
            assertSame(
                element,
                AppScreens.registry.element(
                    element.elementId,
                ),
            )
            assertSame(
                CgmPartnerLabScreen,
                AppScreens.registry
                    .ownerScreenOf(
                        element,
                    ),
            )
        }
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
