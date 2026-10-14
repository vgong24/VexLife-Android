package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorBlueprintTest {
    private sealed interface QueueFamily :
        VexBehaviorFamily<
            QueueFamily
        >

    private sealed interface QueueState :
        VexBehaviorFamily.State<
            QueueFamily
        > {
        data object Empty :
            QueueState

        data class Positioned(
            val hasNext: Boolean,
            val wrapAtEnd: Boolean = false,
        ) : QueueState
    }

    private sealed interface QueueAct :
        VexBehaviorFamily.Act<
            QueueFamily
        > {
        data object Next :
            QueueAct

        data object Orphan :
            QueueAct
    }

    private sealed interface QueueEffect :
        VexBehaviorFamily.Effect<
            QueueFamily
        > {
        data object Advance :
            QueueEffect

        data object Wrap :
            QueueEffect
    }

    private enum class QueueControl :
        VexBehaviorFamily.Control<
            QueueFamily
        > {
        Next,
        Previous,
    }

    /**
     * One State KIND + one Act may have several conditional relationships when State flavor makes
     * those relationships mutually exclusive for any exact current value.
     */
    private fun queueBlueprint() =
        vexBehaviorBlueprint<
            QueueFamily,
            QueueState,
            QueueAct,
            QueueEffect
        >(
            behaviorRef = "example.queue",
        ) {
            state<QueueState.Empty>()

            state<QueueState.Positioned> {
                on(
                    act = QueueAct.Next,
                    effect = QueueEffect.Advance,
                    conditionName = "has-next",
                ) { current ->
                    current.hasNext
                }.stay()

                on(
                    act = QueueAct.Next,
                    effect = QueueEffect.Wrap,
                    conditionName = "at-end-and-wrap-enabled",
                ) { current ->
                    !current.hasNext &&
                        current.wrapAtEnd
                }.stay()
            }
        }

    @Test
    fun conditionalSelfRelationshipKeepsQueuePositioned() {
        val blueprint =
            queueBlueprint()

        assertNull(
            blueprint.resolve(
                current =
                    QueueState.Positioned(
                        hasNext = false,
                    ),
                act =
                    QueueAct.Next,
            ),
        )

        val next =
            blueprint.resolve(
                current =
                    QueueState.Positioned(
                        hasNext = true,
                    ),
                act =
                    QueueAct.Next,
            )

        assertSame(
            QueueEffect.Advance,
            next?.effect,
        )
        assertEquals(
            QueueState.Positioned::class,
            next?.admittedStateKind,
        )
        assertTrue(
            blueprint.describe()
                .contains(
                    "when has-next",
                ),
        )
    }

    @Test
    fun sameStateKindAndActCanResolveDifferentEffectFromStateFlavor() {
        val blueprint =
            queueBlueprint()

        val wrapped =
            blueprint.resolve(
                current =
                    QueueState.Positioned(
                        hasNext = false,
                        wrapAtEnd = true,
                    ),
                act =
                    QueueAct.Next,
            )

        assertSame(
            QueueEffect.Wrap,
            wrapped?.effect,
        )
        assertEquals(
            QueueState.Positioned::class,
            wrapped?.admittedStateKind,
        )

        assertEquals(
            listOf(
                QueueAct.Next,
            ),
            blueprint.availableActs(
                QueueState.Positioned(
                    hasNext = false,
                    wrapAtEnd = true,
                ),
            ),
        )
    }

    @Test
    fun incomingAndOutgoingAreDerivedFromAuthoredRelationships() {
        val blueprint =
            queueBlueprint()

        assertEquals(
            2,
            blueprint.outgoing(
                QueueState.Positioned::class,
            ).size,
        )
        assertEquals(
            2,
            blueprint.incoming(
                QueueState.Positioned::class,
            ).size,
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun overlappingSameStateAndActConditionsAreRejectedForCurrentValue() {
        val ambiguous =
            vexBehaviorBlueprint<
                QueueFamily,
                QueueState,
                QueueAct,
                QueueEffect
            >(
                behaviorRef =
                    "example.queue.ambiguous",
            ) {
                state<QueueState.Positioned> {
                    on(
                        act = QueueAct.Next,
                        effect =
                            QueueEffect.Advance,
                        conditionName =
                            "has-next",
                    ) { current ->
                        current.hasNext
                    }.stay()

                    on(
                        act = QueueAct.Next,
                        effect =
                            QueueEffect.Wrap,
                        conditionName =
                            "wrap-enabled",
                    ) { current ->
                        current.wrapAtEnd
                    }.stay()
                }
            }

        ambiguous.resolve(
            current =
                QueueState.Positioned(
                    hasNext = true,
                    wrapAtEnd = true,
                ),
            act =
                QueueAct.Next,
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun finiteControlVocabularyMustBeCompletelyLinked() {
        queueBlueprint()
            .controls<QueueControl> {
                control(
                    QueueControl.Next,
                    QueueAct.Next,
                )
            }
    }

    private enum class OrphanControl :
        VexBehaviorFamily.Control<
            QueueFamily
        > {
        Orphan,
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun controlCannotReferenceActNotAdmittedByItsBoundBehavior() {
        queueBlueprint()
            .controls<OrphanControl> {
                control(
                    OrphanControl.Orphan,
                    QueueAct.Orphan,
                )
            }
    }
}

// [VXG RealForever]
