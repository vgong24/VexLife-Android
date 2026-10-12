package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsPlaceSearchQueryTest {
    @Test
    fun blankQueryDoesNotReachSqliteFts() {
        assertNull(
            vexMapsPlaceMatchQuery(
                "   --  ",
            ),
        )
    }

    @Test
    fun wordsBecomeDeterministicPrefixTokens() {
        assertEquals(
            "coffee* AND main* AND street*",
            vexMapsPlaceMatchQuery(
                " Coffee, Main Street ",
            ),
        )
    }

    @Test
    fun punctuationCannotBecomeFtsSyntax() {
        assertEquals(
            "a* AND b* AND c*",
            vexMapsPlaceMatchQuery(
                "a + b / c",
            ),
        )
    }

    @Test
    fun placeSearchRequestGateAcceptsOnlyLatestGeneration() {
        val gate =
            VexMapsPlaceSearchRequestGate()

        val first =
            gate.begin()
        assertEquals(
            true,
            gate.accepts(first),
        )

        val second =
            gate.begin()
        assertEquals(
            false,
            gate.accepts(first),
        )
        assertEquals(
            true,
            gate.accepts(second),
        )
    }

    @Test
    fun queryEditOrImportInvalidationRejectsInFlightSearch() {
        val gate =
            VexMapsPlaceSearchRequestGate()
        val request =
            gate.begin()

        gate.invalidate()

        assertEquals(
            false,
            gate.accepts(request),
        )
    }

    @Test
    fun queryTokenCountIsBounded() {
        assertEquals(
            "one* AND two* AND three* AND four* AND five* AND six* AND seven* AND eight*",
            vexMapsPlaceMatchQuery(
                "one two three four five six seven eight nine ten",
            ),
        )
    }
}

// [VXG RealForever]
