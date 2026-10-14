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
    }

    private enum class QueueControl :
        VexBehaviorFamily.Control<
            QueueFamily
        > {
        Next,
        Previous,
    }

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
            next?.destination,
        )
        assertTrue(
            blueprint.describe()
                .contains(
                    "when has-next",
                ),
        )
    }

    @Test
    fun incomingAndOutgoingAreDerivedFromOneAuthoredRelationship() {
        val blueprint =
            queueBlueprint()

        assertEquals(
            1,
            blueprint.outgoing(
                QueueState.Positioned::class,
            ).size,
        )
        assertEquals(
            1,
            blueprint.incoming(
                QueueState.Positioned::class,
            ).size,
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
