package com.vextreme.vexlife.android.screens.shared.maps

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsPackReplacementGateTest {
    @Test
    fun recoveryRunsOncePerProcessGate() {
        val gate =
            VexMapsPackReplacementGate()
        var runs = 0

        gate.recoverOnce {
            runs += 1
        }
        gate.recoverOnce {
            runs += 1
        }

        assertEquals(
            1,
            runs,
        )
    }

    @Test
    fun failedRecoveryCanBeRetried() {
        val gate =
            VexMapsPackReplacementGate()
        var runs = 0

        runCatching {
            gate.recoverOnce {
                runs += 1
                error("recovery failed")
            }
        }

        gate.recoverOnce {
            runs += 1
        }

        assertEquals(
            2,
            runs,
        )
    }

    @Test
    fun replacementTransactionsAreSerializedAcrossCallers() =
        runBlocking {
            val gate =
                VexMapsPackReplacementGate()
            val firstEntered =
                CountDownLatch(1)
            val releaseFirst =
                CountDownLatch(1)
            val events =
                Collections.synchronizedList(
                    mutableListOf<String>(),
                )

            val first =
                launch(
                    Dispatchers.Default,
                ) {
                    gate.withExclusiveReplacement {
                        events += "first-enter"
                        firstEntered.countDown()
                        check(
                            releaseFirst.await(
                                5,
                                TimeUnit.SECONDS,
                            ),
                        ) {
                            "first replacement was not released"
                        }
                        events += "first-exit"
                    }
                }

            assertTrue(
                firstEntered.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            val second =
                launch(
                    start =
                        CoroutineStart.UNDISPATCHED,
                ) {
                    gate.withExclusiveReplacement {
                        events += "second-enter"
                    }
                }

            assertFalse(
                second.isCompleted,
            )

            releaseFirst.countDown()
            first.join()
            second.join()

            assertEquals(
                listOf(
                    "first-enter",
                    "first-exit",
                    "second-enter",
                ),
                events.toList(),
            )
        }
}

// [VXG RealForever]
