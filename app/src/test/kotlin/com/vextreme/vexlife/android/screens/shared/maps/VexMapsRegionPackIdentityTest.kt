package com.vextreme.vexlife.android.screens.shared.maps

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsRegionPackIdentityTest {
    @Test
    fun alreadyNewerStagingTimestampIsPreserved() {
        val staging =
            tempRegionFile()

        check(
            staging.setLastModified(
                2_000L,
            ),
        )

        val observed =
            staging.lastModified()
        val result =
            ensureVexMapsRegionPackReplacementTimestamp(
                staging = staging,
                previousLastModifiedMillis =
                    observed - 1L,
                nowMillis = observed - 1L,
            )

        assertEquals(
            observed,
            result,
        )
    }

    @Test
    fun collidingTimestampAdvancesBeforeReplacement() {
        val staging =
            tempRegionFile()

        check(
            staging.setLastModified(
                2_000L,
            ),
        )

        val previous =
            staging.lastModified()
        val advanced =
            ensureVexMapsRegionPackReplacementTimestamp(
                staging = staging,
                previousLastModifiedMillis =
                    previous,
                nowMillis = previous,
            )

        assertTrue(
            advanced >
                previous,
        )
        assertEquals(
            advanced,
            staging.lastModified(),
        )
    }

    private fun tempRegionFile(): File =
        kotlin.io.path
            .createTempFile(
                prefix = "vexmaps-region-",
                suffix = ".tar",
            )
            .toFile()
            .apply {
                writeText(
                    "region",
                )
                deleteOnExit()
            }
}

// [VXG RealForever]
