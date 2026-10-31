package com.vextreme.vexlife.android.execution.usage

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.execution.testing.VexExecutionTestRule
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
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

class VexExecutionVirtualTimeUsageTest {
    @get:Rule
    val vexExecution =
        VexExecutionTestRule(
            maxCollectRestarts = 1,
            collectRestartDelayMillis = 1_000,
        )

    private lateinit var repository: ExampleRepository
    private lateinit var execution: ExampleFeatureExecution

    @Before
    fun setUp() {
        repository = ExampleRepository()
        execution =
            ExampleFeatureExecution(
                root = vexExecution.root,
                repository = repository,
            )
    }

    private class ExampleRepository {
        val restartAttempts = AtomicInteger(0)

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
                restartAttempts.incrementAndGet()
                error("restartable collector failed")
            }

        suspend fun operationWithCleanup() {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    delay(1_000)
                }
            }
        }
    }

    private class ExampleFeatureExecution(
        root: VexExecutionRoot,
        private val repository: ExampleRepository,
    ) : VexExecution(root) {
        fun observePrimary(values: MutableList<Int>): Job =
            launchForCollect(repository::primaryStates) { values += it }

        fun observeSecondary(values: MutableList<Int>): Job =
            launchForCollect(repository::secondaryStates) { values += it }

        fun observeRestartable(): Job =
            launchForCollect(repository::restartableStates) {}

        fun runCleanupOperation(): Job =
            launchOnce(repository::operationWithCleanup)

        suspend fun stopCleanup(): Boolean =
            joinStop(
                repository::operationWithCleanup,
                "replace operation",
            )
    }

    @Test
    fun independentFlowSourcesStartAsConcurrentSiblingJobs() =
        vexExecution.runTest {
            val primary = mutableListOf<Int>()
            val secondary = mutableListOf<Int>()

            val first = execution.observePrimary(primary)
            val second = execution.observeSecondary(secondary)

            runCurrent()

            assertEquals(listOf(1), primary)
            assertEquals(listOf(2), secondary)
            assertTrue(first.isActive)
            assertTrue(second.isActive)
        }

    @Test
    fun restartDelayCanBeProvedWithoutWallClockWaiting() =
        vexExecution.runTest {
            val job = execution.observeRestartable()

            runCurrent()
            assertEquals(1, repository.restartAttempts.get())

            advanceTimeBy(999)
            runCurrent()
            assertEquals(1, repository.restartAttempts.get())

            advanceTimeBy(1)
            runCurrent()
            assertEquals(2, repository.restartAttempts.get())
            assertTrue(job.isCompleted)
        }

    @Test
    fun joinStopWaitsUntilStructuredCleanupActuallyFinishes() =
        vexExecution.runTest {
            execution.runCleanupOperation()
            runCurrent()

            var stopCompleted = false
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.currentCoroutineContext(),
            ).launch {
                execution.stopCleanup()
                stopCompleted = true
            }

            runCurrent()
            assertFalse(stopCompleted)

            advanceTimeBy(999)
            runCurrent()
            assertFalse(stopCompleted)

            advanceTimeBy(1)
            runCurrent()
            assertTrue(stopCompleted)
        }
}

// [VXG RealForever]
