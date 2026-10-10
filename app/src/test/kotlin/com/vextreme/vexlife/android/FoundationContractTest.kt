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
    fun screenRelayProcessesBeforeComposeEverCollectsIt() {
        val relay = FoundationScreenRelay()

        relay.screenEntered()

        assertEquals(
            FoundationScreenVexState.ScreenPresence.ENTERED,
            relay.current.screenPresence,
        )
        assertEquals(1L, relay.current.revision)
    }
}
