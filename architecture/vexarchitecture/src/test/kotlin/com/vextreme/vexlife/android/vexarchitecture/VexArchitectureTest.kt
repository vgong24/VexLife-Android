package com.vextreme.vexlife.android.vexarchitecture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proof for the reusable semantic-directory contract.
 *
 * The examples deliberately use meaningless A/B Cases so the test protects the architecture grammar
 * itself instead of accidentally proving one VexStream implementation.
 */
class VexArchitectureTest {
    /** Type-only coordinate for this test realm. */
    private sealed interface ExampleRealm : VexRealm

    /** Current truth with one condition used to prove conditional availability. */
    private data class CaseA(
        val allowB: Boolean = true,
    ) : VexCase<ExampleRealm>

    /** Destination truth is data-free because only relationship topology matters here. */
    private data object CaseB : VexCase<ExampleRealm>

    /**
     * Mutable test root used only to prove parent/realm gating.
     *
     * Production feature flags remain feature-owned; the reusable contract should not invent a flag
     * registry merely to test effectiveEnabled.
     */
    private class ExampleArchitecture(
        override val architectureRef: String,
        override val parent: VexArchitectureContract? = null,
        private var enabledNow: Boolean = true,
    ) : VexRealmArchitecture<ExampleRealm> {
        override val enabled: Boolean
            get() = enabledNow

        /** Test-only mutation of this architecture's own participation bit. */
        fun setEnabled(
            value: Boolean,
        ) {
            enabledNow = value
        }
    }

    @Test
    fun typedRelationshipsProduceOneDerivedReadableMap() {
        val architecture =
            ExampleArchitecture(
                architectureRef = "example.flow",
            )
        val becomeB =
            architecture.transition<
                ExampleRealm,
                CaseA,
                CaseB
            >(
                name = "become-b",
            )
        val returnToA =
            architecture.transition<
                ExampleRealm,
                CaseB,
                CaseA
            >(
                name = "return-a",
            )
        val caseA =
            architecture.placement<
                ExampleRealm,
                CaseA
            > {
                arrivedBy(returnToA)
                can(becomeB)
            }
        val caseB =
            architecture.placement<
                ExampleRealm,
                CaseB
            > {
                arrivedBy(becomeB)
                can(returnToA)
            }
        val primary =
            architecture.control(
                name = "primary",
                becomeB,
                returnToA,
            )

        val map =
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

        assertTrue(
            map.describe()
                .contains(
                    "example.flow.become-b",
                ),
        )
        assertTrue(
            map.describe()
                .contains(
                    "CaseA",
                ),
        )
    }

    @Test
    fun namedCurrentConditionChangesAvailabilityWithoutChangingTopology() {
        val architecture =
            ExampleArchitecture(
                architectureRef =
                    "example.conditional",
            )
        val becomeB =
            architecture.transition<
                ExampleRealm,
                CaseA,
                CaseB
            >(
                name = "become-b",
                conditionName =
                    "case-a-allows-b",
            ) { current ->
                current.allowB
            }
        val caseA =
            architecture.placement<
                ExampleRealm,
                CaseA
            > {
                can(becomeB)
            }
        val primary =
            architecture.control(
                name = "primary",
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
    fun parentRealmDisablesDescendantControlResolution() {
        val parent =
            object : VexArchitectureContract {
                override val architectureRef =
                    "example"
                override val parent:
                    VexArchitectureContract? =
                    null
                override var enabled: Boolean =
                    true
            }
        val architecture =
            ExampleArchitecture(
                architectureRef =
                    "example.child",
                parent = parent,
            )
        val becomeB =
            architecture.transition<
                ExampleRealm,
                CaseA,
                CaseB
            >(
                name = "become-b",
            )
        val caseA =
            architecture.placement<
                ExampleRealm,
                CaseA
            > {
                can(becomeB)
            }
        val primary =
            architecture.control(
                name = "primary",
                becomeB,
            )

        assertTrue(
            architecture.effectiveEnabled,
        )
        assertSame(
            becomeB,
            vexCurrent(
                CaseA(),
                caseA,
            ).availableTransition(
                primary,
            ),
        )

        parent.enabled = false

        assertFalse(
            architecture.effectiveEnabled,
        )
        assertNull(
            vexCurrent(
                CaseA(),
                caseA,
            ).availableTransition(
                primary,
            ),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun duplicateControlSemanticIdentityIsRejected() {
        val architecture =
            ExampleArchitecture(
                architectureRef =
                    "example.identity",
            )
        val becomeB =
            architecture.transition<
                ExampleRealm,
                CaseA,
                CaseB
            >(
                name = "become-b",
            )
        val caseA =
            architecture.placement<
                ExampleRealm,
                CaseA
            > {
                can(becomeB)
            }
        val caseB =
            architecture.placement<
                ExampleRealm,
                CaseB
            > {
                arrivedBy(becomeB)
            }

        architecture.architectureMap(
            placements =
                listOf(
                    caseA,
                    caseB,
                ),
            controls =
                listOf(
                    architecture.control(
                        "primary",
                        becomeB,
                    ),
                    architecture.control(
                        "primary",
                        becomeB,
                    ),
                ),
        )
    }
}

// [VXG RealForever]
