package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsNavigationServiceExecutionContractTest {
    private lateinit var observationFlow:
        Flow<Int>

    private val exactObservationSource:
        Flow<Int>
        get() =
            observationFlow

    @Test
    fun serviceUsesVexExecutionRootInsteadOfOwningAPrivateCoroutineScope() {
        val fields =
            VexMapsNavigationService::class.java
                .declaredFields

        assertTrue(
            fields.any { field ->
                VexExecutionRoot::class.java
                    .isAssignableFrom(
                        field.type,
                    )
            },
        )

        assertFalse(
            fields.any { field ->
                CoroutineScope::class.java
                    .isAssignableFrom(
                        field.type,
                    )
            },
        )
    }

    @Test
    fun cancellingMaintainedObservationRestartsOnlyAfterPredecessorCompletes() =
        runTest {
            val cleanupRelease =
                CompletableDeferred<Unit>()
            observationFlow =
                flow {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(
                            NonCancellable,
                        ) {
                            cleanupRelease.await()
                        }
                    }
                }

            val executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                )
            val owner =
                Any()
            val source =
                this@VexMapsNavigationServiceExecutionContractTest::
                    exactObservationSource

            var currentObservation:
                Job? =
                VexExecution.observe(
                    executionRoot =
                        executionRoot,
                    owner =
                        owner,
                    source =
                        source,
                    restartOnSourceFailure =
                        false,
                ) {
                    Unit
                }

            runCurrent()

            val predecessor =
                requireNotNull(
                    currentObservation,
                )
            predecessor.cancel()
            runCurrent()

            assertFalse(
                predecessor.isCompleted,
            )

            val duplicateAdmission =
                runCatching {
                    VexExecution.observe(
                        executionRoot =
                            executionRoot,
                        owner =
                            owner,
                        source =
                            source,
                        restartOnSourceFailure =
                            false,
                    ) {
                        Unit
                    }
                }

            assertTrue(
                duplicateAdmission.exceptionOrNull() is
                    IllegalStateException,
            )

            var restartCount =
                0
            val registerCompletionRestart = {
                restartVexMapsNavigationObservationAfterCompletion(
                    observation =
                        predecessor,
                    currentObservation = {
                        currentObservation
                    },
                    shouldRestart = {
                        true
                    },
                    restart = {
                        restartCount +=
                            1
                        currentObservation =
                            VexExecution.observe(
                                executionRoot =
                                    executionRoot,
                                owner =
                                    owner,
                                source =
                                    source,
                                restartOnSourceFailure =
                                    false,
                            ) {
                                Unit
                            }
                    },
                )
            }

            /*
             * Repeated prepared starts may register the same completion-aware intent while the old
             * callbackFlow is still cleaning up. Identity re-checking must still create one successor.
             */
            registerCompletionRestart()
            registerCompletionRestart()

            assertEquals(
                0,
                restartCount,
            )

            cleanupRelease.complete(
                Unit,
            )
            advanceUntilIdle()

            assertTrue(
                predecessor.isCompleted,
            )
            assertEquals(
                1,
                restartCount,
            )
            assertNotSame(
                predecessor,
                currentObservation,
            )
            assertTrue(
                requireNotNull(
                    currentObservation,
                ).isActive,
            )

            executionRoot.forceStopAll(
                "test complete",
            )
            advanceUntilIdle()
        }
}

// [VXG RealForever][VEXARCHITECTURE][NAVIGATION-SERVICE][VEXEXECUTION][CURRENT-MAIN]
