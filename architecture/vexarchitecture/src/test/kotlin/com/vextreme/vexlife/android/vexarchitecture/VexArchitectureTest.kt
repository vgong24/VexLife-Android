package com.vextreme.vexlife.android.vexarchitecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class VexArchitectureTest {
    private data object CaseA
    private data object CaseB
    private data object CaseC

    private val becomeB =
        vexTransition<CaseA, CaseB>(
            "example.a-to-b",
        )

    private val becomeC =
        vexTransition<CaseA, CaseC>(
            "example.a-to-c",
        )

    private val returnToA =
        vexTransition<CaseB, CaseA>(
            "example.b-to-a",
        )

    @Test
    fun placementKeepsCurrentImplicitWhileRelationshipsStayPreciselyTyped() {
        val caseA =
            vexPlacement<CaseA> {
                arrivedBy(returnToA)
                can(becomeB)
                can(becomeC)
            }

        assertEquals(
            CaseA::class,
            caseA.caseType,
        )
        assertEquals(
            listOf(returnToA),
            caseA.arrivals,
        )
        assertEquals(
            listOf(
                becomeB,
                becomeC,
            ),
            caseA.transitions,
        )
    }

    @Test
    fun transitionSlotResolvesFromCurrentPlacementInsteadOfReinterpretingState() {
        val caseA =
            vexPlacement<CaseA> {
                can(becomeB)
            }
        val current =
            vexCurrent(
                case = CaseA,
                placement = caseA,
            )
        val slot =
            vexTransitionSlot(
                "example.primary-control",
                becomeB,
                returnToA,
            )

        assertSame(
            becomeB,
            current.availableTransition(slot),
        )
    }

    @Test
    fun transitionSlotIsAbsentWhenCurrentCaseOffersNoCandidate() {
        val caseC =
            vexPlacement<CaseC> {}
        val current =
            vexCurrent(
                case = CaseC,
                placement = caseC,
            )
        val slot =
            vexTransitionSlot(
                "example.primary-control",
                becomeB,
                returnToA,
            )

        assertNull(
            current.availableTransition(slot),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun transitionSlotRejectsAmbiguousCurrentPlacement() {
        val caseA =
            vexPlacement<CaseA> {
                can(becomeB)
                can(becomeC)
            }
        val slot =
            vexTransitionSlot(
                "example.bad-slot",
                becomeB,
                becomeC,
            )

        vexCurrent(
            case = CaseA,
            placement = caseA,
        ).availableTransition(slot)
    }
}

// [VXG RealForever]
