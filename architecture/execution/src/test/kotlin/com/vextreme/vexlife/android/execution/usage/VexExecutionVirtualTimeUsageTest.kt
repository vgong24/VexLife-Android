@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.execution.usage

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.execution.testing.VexExecutionTestRule
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Consumer-side execution test blueprint.
 *
 * The consumer supplies semantic relationship keys. VexExecution owns Jobs and their lifetime; no
 * Job handle or runtime occurrence ID escapes back into the feature.
 */
class VexExecutionVirtualTimeUsageTest {
    @get:Rule
    val vexExecution =
        VexExecutionTestRule(
            maxCollectRestarts = 1,
            collectRestartDelayMillis =
                1_000L,
        )

    private lateinit var repository:
        ExampleRepository

    private lateinit var execution:
        ExampleFeatureExecution

    @Before
    fun setUp() {
        repository =
            ExampleRepository()

        execution =
            ExampleFeatureExecution(
                root = vexExecution.root,
                repository = repository,
            )
    }

    private class ExampleRepository {
        val restartAttempts =
            AtomicInteger(0)

        fun primaryStates(): Flow<Int> =
            flow {
                emit(1)
                awaitCancellation()
            }

        fun secondaryStates(): Flow<Int> =
            flow {
                emit(2)
                awaitCancellation()
            }

        fun restartableStates(): Flow<Int> =
            flow {
                restartAttempts
                    .incrementAndGet()
                error(
                    "restartable collector failed",
                )
            }

        suspend fun operationWithCleanup() {
            try {
                awaitCancellation()
            } finally {
                withContext(
                    NonCancellable,
                ) {
                    delay(
                        1_000L,
                    )
                }
            }
        }
    }

    private class ExampleFeatureExecution(
        root:
            VexExecutionRoot,
        private val repository:
            ExampleRepository,
    ) :
        VexExecution<
            ExampleFeatureExecution
        >(
            root,
        ) {
        enum class Relationship :
            VexExecution.RelationshipId<
                ExampleFeatureExecution
            > {
            Primary,
            Secondary,
            Restartable,
            Cleanup,
        }

        fun observePrimary(
            values:
                MutableList<Int>,
        ):
            Boolean =
            launchForCollect(
                relationshipId =
                    Relationship.Primary,
                source =
                    repository::primaryStates,
            ) { value ->
                values +=
                    value
            }

        fun observeSecondary(
            values:
                MutableList<Int>,
        ):
            Boolean =
            launchForCollect(
                relationshipId =
                    Relationship.Secondary,
                source =
                    repository::secondaryStates,
            ) { value ->
                values +=
                    value
            }

        fun observeRestartable():
            Boolean =
            launchForCollect(
                relationshipId =
                    Relationship.Restartable,
                source =
                    repository::restartableStates,
            ) {}

        fun runCleanupOperation():
            Boolean =
            launch(
                relationshipId =
                    Relationship.Cleanup,
                approach =
                    VexExecution.Approach.Earliest,
                action =
                    repository::operationWithCleanup,
            )

        suspend fun joinCleanup():
            Boolean =
            joinStop(
                relationshipId =
                    Relationship.Cleanup,
                reason =
                    "replace operation",
            )
    }

    private fun state(
        relationship:
            ExampleFeatureExecution.Relationship,
    ):
        VexExecution.State =
        requireNotNull(
            vexExecution.root
                .executions
                .value[
                    relationship
                ],
        )
            .last()

    @Test
    fun independentFlowRelationshipsStartAsConcurrentSiblingJobs() =
        vexExecution.runTest {
            val primary =
                mutableListOf<Int>()
            val secondary =
                mutableListOf<Int>()

            assertTrue(
                execution.observePrimary(
                    primary,
                ),
            )
            assertTrue(
                execution.observeSecondary(
                    secondary,
                ),
            )

            runCurrent()

            assertEquals(
                listOf(1),
                primary,
            )
            assertEquals(
                listOf(2),
                secondary,
            )
            assertEquals(
                VexExecution.Status.RUNNING,
                state(
                    ExampleFeatureExecution.Relationship.Primary,
                ).status,
            )
            assertEquals(
                VexExecution.Status.RUNNING,
                state(
                    ExampleFeatureExecution.Relationship.Secondary,
                ).status,
            )
        }

    @Test
    fun restartDelayCanBeProvedWithoutWallClockWaiting() =
        vexExecution.runTest {
            assertTrue(
                execution.observeRestartable(),
            )

            runCurrent()

            assertEquals(
                1,
                repository
                    .restartAttempts
                    .get(),
            )
            assertEquals(
                VexExecution.Status.RESTARTING,
                state(
                    ExampleFeatureExecution.Relationship.Restartable,
                ).status,
            )

            advanceTimeBy(
                999L,
            )
            runCurrent()

            assertEquals(
                1,
                repository
                    .restartAttempts
                    .get(),
            )

            advanceTimeBy(
                1L,
            )
            runCurrent()

            assertEquals(
                2,
                repository
                    .restartAttempts
                    .get(),
            )
            assertEquals(
                VexExecution.Status.FAILED,
                state(
                    ExampleFeatureExecution.Relationship.Restartable,
                ).status,
            )
        }

    @Test
    fun relationshipJoinStopWaitsUntilStructuredCleanupActuallyFinishes() =
        vexExecution.runTest {
            assertTrue(
                execution
                    .runCleanupOperation(),
            )

            runCurrent()

            var stopCompleted =
                false

            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.currentCoroutineContext(),
            ).launch {
                execution.joinCleanup()
                stopCompleted =
                    true
            }

            runCurrent()

            assertFalse(
                stopCompleted,
            )

            advanceTimeBy(
                999L,
            )
            runCurrent()

            assertFalse(
                stopCompleted,
            )

            advanceTimeBy(
                1L,
            )
            runCurrent()

            assertTrue(
                stopCompleted,
            )
            assertEquals(
                VexExecution.Status.CANCELLED,
                state(
                    ExampleFeatureExecution.Relationship.Cleanup,
                ).status,
            )
        }
}

// [VXG RealForever]
