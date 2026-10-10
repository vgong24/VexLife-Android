package com.vextreme.vexlife.android

import org.junit.Assert.assertEquals
import org.junit.Test

class FoundationContractTest {
    @Test
    fun foundationIdentityRemainsExplicitWhileBootstrapIsIncomplete() {
        assertEquals("foundation.vexlife.android.standalone-build/v1", FoundationContract.REF)
        assertEquals("FOUNDATION_INCOMPLETE", FoundationContract.STATUS)
    }

    @Test
    fun sampleAppUsesAnExtendedRelayFamilyInsteadOfReplacingWholeInputState() {
        val relay = FoundationScreenRelay()

        assertEquals(
            FoundationScreenRelay.ScreenPresence.EXITED,
            relay.current.outputState.screenPresence,
        )

        relay.screenEntered()
        assertEquals(
            FoundationScreenRelay.ScreenPresence.ENTERED,
            relay.current.outputState.screenPresence,
        )

        relay.screenExited()
        assertEquals(
            FoundationScreenRelay.ScreenPresence.EXITED,
            relay.current.outputState.screenPresence,
        )
    }
}
