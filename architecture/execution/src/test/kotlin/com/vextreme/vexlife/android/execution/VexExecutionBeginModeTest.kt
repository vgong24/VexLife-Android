package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexExecutionBeginModeTest {
    private object FamilyOwner

    private class Operation {
        val firstStarted =
            CompletableDeferred<Unit>()
        val firstRelease =
            CompletableDeferred<Unit>()
        val calls =
            mutableListOf<String>()

        suspend fun save(
            value: String,
        ):
            String {
            calls +=
                "start:" +
                    value

            if (
                value ==
                "first"
            ) {
                firstStarted.complete(
                    Unit,
                )

                try {
                    firstRelease.await()
                } finally {
                    calls +=
                        "finish:first"
                }
            }

            calls +=
                "return:" +
                    value
            return value
        }
    }

    @Test
    fun replaceableCancelsAndJoinsPriorSameOperationBeforeNewestRuns() =
        runTest {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                )
            val operation =
                Operation()
            val results =
                mutableListOf<String>()

            try {
                VexExecution.beginReplaceable(
                    executionRoot =
                        root,
                    owner =
                        FamilyOwner::class,
                    operation =
                        operation::save,
                    argument =
                        "first",
                    onResult =
                        results::add,
                )

                runCurrent()

                assertTrue(
                    operation.firstStarted.isCompleted,
                )

                val second =
                    VexExecution.beginReplaceable(
                        executionRoot =
                            root,
                        owner =
                            FamilyOwner::class,
                        operation =
                            operation::save,
                        argument =
                            "second",
                        onResult =
                            results::add,
                    )

                runCurrent()
                second.awaitCompletion()

                assertEquals(
                    listOf(
                        "start:first",
                        "finish:first",
                        "start:second",
                        "return:second",
                    ),
                    operation.calls,
                )
                assertEquals(
                    listOf(
                        "second",
                    ),
                    results,
                )
            } finally {
                root.forceStopAll()
            }
        }

    @Test
    fun sequencedWaitsForPriorSameOperationAndPreservesAcceptedOrder() =
        runTest {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                )
            val operation =
                Operation()
            val results =
                mutableListOf<String>()

            try {
                val first =
                    VexExecution.beginSequenced(
                        executionRoot =
                            root,
                        owner =
                            FamilyOwner::class,
                        operation =
                            operation::save,
                        argument =
                            "first",
                        onResult =
                            results::add,
                    )

                runCurrent()

                val second =
                    VexExecution.beginSequenced(
                        executionRoot =
                            root,
                        owner =
                            FamilyOwner::class,
                        operation =
                            operation::save,
                        argument =
                            "second",
                        onResult =
                            results::add,
                    )

                runCurrent()

                assertEquals(
                    listOf(
                        "start:first",
                    ),
                    operation.calls,
                )

                operation.firstRelease.complete(
                    Unit,
                )

                runCurrent()
                first.awaitCompletion()
                second.awaitCompletion()

                assertEquals(
                    listOf(
                        "start:first",
                        "finish:first",
                        "return:first",
                        "start:second",
                        "return:second",
                    ),
                    operation.calls,
                )
                assertEquals(
                    listOf(
                        "first",
                        "second",
                    ),
                    results,
                )
            } finally {
                root.forceStopAll()
            }
        }
}

// [VXG RealForever]
