package com.vextreme.vexlife.android.screens.shared.partners

import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import org.junit.Assert.assertSame
import org.junit.Test

class PartnersCircuitLabScreenSemanticRegistrationTest {
    @Test
    fun partnerCircuitControlsBelongToTheRegisteredScreen() {
        listOf(
            PartnersCircuitLabScreen.Automation,
            PartnersCircuitLabScreen.ReceiveCgm,
            PartnersCircuitLabScreen.ReceivePen,
            PartnersCircuitLabScreen.RequestPump,
            PartnersCircuitLabScreen.ReceivePumpOutcome,
        ).forEach { element ->
            assertSame(
                element,
                AppScreens.registry.element(
                    element.elementId,
                ),
            )
            assertSame(
                PartnersCircuitLabScreen,
                AppScreens.registry
                    .ownerScreenOf(
                        element,
                    ),
            )
        }
    }

    @Test
    fun debugEntryNavigatesToTheRegisteredPartnersCircuitLabScreen() {
        val intent =
            DebugMenuScreen
                .OpenPartnersCircuitLab
                .intent as
                ElementIntent.NavigateTo

        assertSame(
            PartnersCircuitLabScreen,
            intent.screen,
        )
    }
}

// [VXG RealForever][VEX-PARTNER][FEEDBACK-CIRCUIT-SCREEN-PROOF]
