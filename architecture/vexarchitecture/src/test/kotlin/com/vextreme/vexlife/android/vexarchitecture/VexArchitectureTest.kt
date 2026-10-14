package com.vextreme.vexlife.android.vexarchitecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexArchitectureTest {
    private data class CaseA(
        val allowB: Boolean = true,
    )
    private data object CaseB

    private class TestArchitecture(
        override val architectureRef: String,
        override val enabled: Boolean,
    ) : VexArchitectureContract {
        override lateinit var map: VexArchitectureMap
    }

    @Test
    fun architectureRootOwnsTypedLocalRelationshipsAndDerivedMap() {
        val architecture =
            TestArchitecture(
                architectureRef = "example.flow",
                enabled = true,
            )
        val becomeB =
            architecture.transition<CaseA, CaseB>(
                "example.a-to-b",
            )
        val returnToA =
            architecture.transition<CaseB, CaseA>(
                "example.b-to-a",
            )
        val caseA =
            architecture.placement<CaseA> {
                arrivedBy(returnToA)
                can(becomeB)
            }
        val caseB =
            architecture.placement<CaseB> {
                arrivedBy(becomeB)
                can(returnToA)
            }
        val primary =
            architecture.control(
                "example.primary",
                becomeB,
                returnToA,
            )

        architecture.map =
            architecture.architectureMap(
                placements =
                    listOf(
                        caseA,
                        caseB,
                    ),
                controls =
                    listOf(
                        primary,
                    ),
            )

        assertEquals(
            listOf(
                CaseA::class,
                CaseB::class,
            ),
            architecture.map.cases,
        )
        assertTrue(
            architecture.map
                .describe()
                .contains(
                    "CaseA",
                ),
        )
    }

    @Test
    fun namedCurrentConditionControlsAvailabilityWithoutChangingTopology() {
        val architecture =
            TestArchitecture(
                architectureRef = "example.conditional",
                enabled = true,
            )
        val becomeB =
            architecture.transition<CaseA, CaseB>(
                transitionRef =
                    "example.a-to-b",
                conditionRef =
                    "case-a-allows-b",
            ) { current ->
                current.allowB
            }
        val caseA =
            architecture.placement<CaseA> {
                can(becomeB)
            }
        val primary =
            architecture.control(
                "example.primary",
                becomeB,
            )

        assertSame(
            becomeB,
            vexCurrent(
                case =
                    CaseA(
                        allowB = true,
                    ),
                placement = caseA,
            ).availableTransition(
                primary,
            ),
        )
        assertNull(
            vexCurrent(
                case =
                    CaseA(
                        allowB = false,
                    ),
                placement = caseA,
            ).availableTransition(
                primary,
            ),
        )
    }

    @Test
    fun disabledArchitectureTurnsOffItsControlsAtTheRootBoundary() {
        val architecture =
            TestArchitecture(
                architectureRef = "example.disabled",
                enabled = false,
            )
        val becomeB =
            architecture.transition<CaseA, CaseB>(
                "example.a-to-b",
            )
        val caseA =
            architecture.placement<CaseA> {
                can(becomeB)
            }

        assertNull(
            vexCurrent(
                case = CaseA(),
                placement = caseA,
            ).availableTransition(
                architecture.control(
                    "example.primary",
                    becomeB,
                ),
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun controlCannotCrossArchitectureRealms() {
        val first =
            TestArchitecture(
                architectureRef = "example.first",
                enabled = true,
            )
        val second =
            TestArchitecture(
                architectureRef = "example.second",
                enabled = true,
            )
        val firstTransition =
            first.transition<CaseA, CaseB>(
                "example.first.a-to-b",
            )
        val secondCaseA =
            second.placement<CaseA> {}

        vexCurrent(
            case = CaseA(),
            placement = secondCaseA,
        ).availableTransition(
            first.control(
                "example.first.primary",
                firstTransition,
            ),
        )
    }
}

// [VXG RealForever]
