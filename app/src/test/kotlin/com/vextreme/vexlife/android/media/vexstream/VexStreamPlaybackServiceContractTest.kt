package com.vextreme.vexlife.android.media.vexstream

import com.vextreme.vexlife.android.behavior.VexService
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackServiceContractTest {
    @Test
    fun serviceBindsPlaybackCurrentWithoutOwningADuplicatePlaybackState() {
        val serviceClass =
            VexStreamPlaybackService::class.java

        assertTrue(
            VexService::class.java
                .isAssignableFrom(
                    serviceClass,
                ),
        )

        assertFalse(
            serviceClass
                .declaredFields
                .any { field ->
                    VexStreamPlaybackState::class.java
                        .isAssignableFrom(
                            field.type,
                        )
                },
        )
    }
}

// [VXG RealForever][VEXARCHITECTURE][PLAYBACK-SERVICE][CURRENT-BINDING]
