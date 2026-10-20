package com.vextreme.vexlife.android.behavior.example.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MapsLocationPressureExampleTest {
    @Test
    fun noPrecisePermissionExposesNoLocationAct() {
        val connection =
            MapsLocationConnectionExample(
                MapsLocationState
                    .NoPrecisePermission,
            )

        assertNull(
            connection.currentProjection[
                MapsLocationControl
                    .UsePreciseLocation
            ].act,
        )
    }

    @Test
    fun activationSynchronouslyAdmitsFindingBeforeProviderOutcome() {
        val connection =
            MapsLocationConnectionExample(
                MapsLocationState
                    .PrecisePermission(
                        acquisition =
                            MapsLocationAcquisition
                                .Idle,
                    ),
            )

        assertSame(
            MapsLocationAct.Locate,
            connection.currentProjection[
                MapsLocationControl
                    .UsePreciseLocation
            ].act,
        )

        connection.activate(
            MapsLocationControl
                .UsePreciseLocation,
        )

        val admitted =
            connection
                .currentProjection
                .state as
                MapsLocationState
                    .PrecisePermission

        assertEquals(
            MapsLocationAcquisition
                .Finding,
            admitted.acquisition,
        )
        assertEquals(
            connection.requestedGeneration,
            admitted.requestGeneration,
        )
        assertNull(
            connection.currentProjection[
                MapsLocationControl
                    .UsePreciseLocation
            ].act,
        )
    }

    @Test
    fun providerOutcomeMayDifferFromAdmittedStateKind() {
        val connection =
            MapsLocationConnectionExample(
                MapsLocationState
                    .PrecisePermission(
                        acquisition =
                            MapsLocationAcquisition
                                .Idle,
                    ),
            )

        connection.activate(
            MapsLocationControl
                .UsePreciseLocation,
        )

        val generation =
            requireNotNull(
                connection
                    .requestedGeneration,
            )

        assertTrue(
            connection.providerResult(
                generation =
                    generation,
                observed =
                    MapsLocationState
                        .ServicesDisabled,
            ),
        )

        assertSame(
            MapsLocationState
                .ServicesDisabled,
            connection
                .currentProjection
                .state,
        )
    }

    @Test
    fun staleAsyncCompletionDoesNotReplaceNewerRequestState() {
        val connection =
            MapsLocationConnectionExample(
                MapsLocationState
                    .PrecisePermission(
                        acquisition =
                            MapsLocationAcquisition
                                .Idle,
                    ),
            )

        connection.activate(
            MapsLocationControl
                .UsePreciseLocation,
        )
        val firstGeneration =
            requireNotNull(
                connection
                    .requestedGeneration,
            )

        assertTrue(
            connection.providerResult(
                generation =
                    firstGeneration,
                observed =
                    MapsLocationState
                        .Failed(
                            reason =
                                "timeout",
                        ),
            ),
        )

        connection.activate(
            MapsLocationControl
                .UsePreciseLocation,
        )
        val secondGeneration =
            requireNotNull(
                connection
                    .requestedGeneration,
            )

        assertTrue(
            secondGeneration >
                firstGeneration,
        )

        assertFalse(
            connection.providerResult(
                generation =
                    firstGeneration,
                observed =
                    MapsLocationState
                        .PrecisePermission(
                            acquisition =
                                MapsLocationAcquisition
                                    .Idle,
                            currentFix =
                                MapsLocationFix(
                                    latitudeE6 =
                                        34123456,
                                    longitudeE6 =
                                        -118123456,
                                ),
                            requestGeneration =
                                firstGeneration,
                        ),
            ),
        )

        val current =
            connection
                .currentProjection
                .state as
                MapsLocationState
                    .PrecisePermission

        assertEquals(
            MapsLocationAcquisition
                .Finding,
            current.acquisition,
        )
        assertEquals(
            secondGeneration,
            current.requestGeneration,
        )
    }
}

// [VXG RealForever]
