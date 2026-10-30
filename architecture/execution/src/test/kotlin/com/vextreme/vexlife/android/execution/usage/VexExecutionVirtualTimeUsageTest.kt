package com.vextreme.vexlife.android.execution.usage

import com.vextreme.vexlife.android.execution.VexCoroutineHandle
import com.vextreme.vexlife.android.execution.VexCoroutineStatus
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
 * The reusable rule owns scheduler/root setup and guaranteed teardown. This class deliberately owns
 * its feature-like dependency construction in [setUp] so a developer can see the real pieces under
 * test without command-clicking through a hidden "buildFeature()" scenario framework.
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
                /*
                 * Simulate teardown that must finish before replacement work may start.
                 * NonCancellable is intentional in this test fake only to prove joinStop waits
                 * through a resource cleanup boundary.
                 */
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
        root: VexExecutionRoot,
        private val repository:
            ExampleRepository,
    ) : VexExecution(root) {
        fun observePrimary(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            observe(
                repository::primaryStates,
            ) { value ->
                values += value
            }

        fun observeSecondary(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            observe(
                repository::secondaryStates,
            ) { value ->
                values += value
            }

        fun observeRestartable():
            VexCoroutineHandle =
            observe(
                repository::restartableStates,
            ) {}

        fun runCleanupOperation():
            VexCoroutineHandle =
            launchOnce(
                repository::
                    operationWithCleanup,
            )
    }

    @Test
    fun independentFlowSourcesStartAsConcurrentSiblingJobs() =
        vexExecution.runTest {
            val primary =
                mutableListOf<Int>()
            val secondary =
                mutableListOf<Int>()

            val first =
                execution.observePrimary(
                    primary,
                )
            val second =
                execution.observeSecondary(
                    secondary,
                )

            /*
             * Both managed Jobs are admitted before the scheduler runs either one. runCurrent()
             * therefore proves the second collector is not sequenced behind the first forever
             * collection.
             */
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
                VexCoroutineStatus.RUNNING,
                first.state.value.status,
            )
            assertEquals(
                VexCoroutineStatus.RUNNING,
                second.state.value.status,
            )
        }

    @Test
    fun restartDelayCanBeProvedWithoutWallClockWaiting() =
        vexExecution.runTest {
            val handle =
                execution
                    .observeRestartable()

            runCurrent()

            assertEquals(
                1,
                repository
                    .restartAttempts
                    .get(),
            )
            assertEquals(
                VexCoroutineStatus
                    .RESTARTING,
                handle.state.value.status,
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
                VexCoroutineStatus.FAILED,
                handle.state.value.status,
            )
        }

    @Test
    fun joinStopWaitsUntilStructuredCleanupActuallyFinishes() =
        vexExecution.runTest {
            val handle =
                execution
                    .runCleanupOperation()

            runCurrent()

            /*
             * Start joinStop as a separately scheduled test action so the test can inspect the
             * interval while cleanup is still pending.
             */
            var stopCompleted = false
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.currentCoroutineContext(),
            ).launch {
                handle.joinStop(
                    "replace operation",
                )
                stopCompleted = true
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
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
        }
}

// [VXG RealForever]
