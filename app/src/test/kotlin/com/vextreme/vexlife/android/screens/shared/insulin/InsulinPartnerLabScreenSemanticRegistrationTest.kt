package com.vextreme.vexlife.android.screens.shared.insulin

import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import org.junit.Assert.assertSame
import org.junit.Test

class InsulinPartnerLabScreenSemanticRegistrationTest {
    @Test
    fun insulinLabControlsBelongToTheRegisteredScreen() {
        listOf(
            InsulinPartnerLabScreen.ReceivePenPacket,
            InsulinPartnerLabScreen.RequestPumpDelivery,
            InsulinPartnerLabScreen.ReceivePumpDelivery,
        ).forEach { element ->
            assertSame(
                element,
                AppScreens.registry.element(
                    element.elementId,
                ),
            )
            assertSame(
                InsulinPartnerLabScreen,
                AppScreens.registry
                    .ownerScreenOf(
                        element,
                    ),
            )
        }
    }

    @Test
    fun debugEntryNavigatesToTheRegisteredInsulinPartnerLabScreen() {
        val intent =
            DebugMenuScreen
                .OpenInsulinPartnerLab
                .intent as
                ElementIntent.NavigateTo

        assertSame(
            InsulinPartnerLabScreen,
            intent.screen,
        )
    }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-CIRCUIT-SCREEN-PROOF]
