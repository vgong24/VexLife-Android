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
    fun sampleAppConsumesReusableStateProjectionModule() {
        val projection = FoundationContract.statusProjection()
        assertEquals(FoundationContract.STATUS_STATE_REF, projection.state.value.stateRef)
        assertEquals(FoundationContract.STATUS, projection.state.value.valueOrNull)
    }
}
