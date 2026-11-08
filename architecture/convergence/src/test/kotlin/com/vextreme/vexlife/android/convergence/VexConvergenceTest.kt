package com.vextreme.vexlife.android.convergence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexConvergenceTest {
    private data class Current(
        val value:
            Int,
    )

    private class Fixture(
        initial:
            Current,
    ) :
        VexConvergence<
            Int,
            Current
        >(
            initialCurrent =
                initial,
        ) {
        val current:
            Current
            get() =
                convergedCurrent

        fun send(
            update:
                Int,
        ):
            Current =
            admit(
                update,
            )

        override fun converge(
            update:
                Int,
            current:
                Current,
        ):
            Current =
            current.copy(
                value =
                    current.value +
                        update,
            )
    }

    @Test
    fun equalNextMeaningRetainsTheExistingCurrentInstance() {
        val initial =
            Current(
                value =
                    7,
            )
        val fixture =
            Fixture(
                initial,
            )

        val returned =
            fixture.send(
                0,
            )

        assertSame(
            initial,
            returned,
        )
        assertSame(
            initial,
            fixture.current,
        )
    }

    @Test
    fun admittedUpdateConvergesAgainstTheRetainedCurrent() {
        val fixture =
            Fixture(
                Current(
                    value =
                        4,
                ),
            )

        fixture.send(
            3,
        )
        fixture.send(
            5,
        )

        assertEquals(
            Current(
                value =
                    12,
            ),
            fixture.current,
        )
    }
}

// [VXG RealForever][VEX-CONVERGENCE][PROOF]
