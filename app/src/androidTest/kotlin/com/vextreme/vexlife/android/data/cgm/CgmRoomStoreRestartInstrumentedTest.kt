package com.vextreme.vexlife.android.data.cgm

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.cgm.CgmFamily
import com.vextreme.vexlife.android.cgm.CgmPartner
import com.vextreme.vexlife.android.cgm.CgmPartnerLab
import com.vextreme.vexlife.android.data.VexLifeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CgmRoomStoreRestartInstrumentedTest {
    @Test
    fun restoredRoomObservationRebuildsStoreAndFamilyWithoutPretendingNewPartnerReceipt() =
        runBlocking {
            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext
                    .applicationContext
            val databaseName =
                "cgm-partner-restart-" +
                    System.nanoTime() +
                    ".db"

            context.deleteDatabase(
                databaseName,
            )

            val originalObservation =
                CgmPartner.Connection.Observation(
                    connectionId =
                        CgmPartner.Connection.Id(
                            value =
                                "cgm.synthetic.connection",
                        ),
                    signal =
                        CgmPartner.Signal.Glucose(
                            source =
                                CgmPartner.Source(
                                    deviceRef =
                                        "cgm-synthetic",
                                    acquisitionEpoch =
                                        "epoch-a",
                                ),
                            sequence =
                                7L,
                            capturedAtEpochMillis =
                                7_000L,
                            mgDl =
                                121,
                            reportedTrend =
                                CgmPartner.Trend.Rising,
                        ),
                    observedAtEpochMillis =
                        7_100L,
                )

            val firstDatabase =
                database(
                    context =
                        context,
                    name =
                        databaseName,
                )

            try {
                val firstStore =
                    RoomCgmStore(
                        dao =
                            firstDatabase
                                .cgmObservationDao(),
                    )

                firstStore.commit(
                    originalObservation,
                )

                val admitted =
                    firstStore
                        .readCurrent()

                assertEquals(
                    1,
                    admitted
                        .observations
                        .size,
                )
                assertSame(
                    originalObservation
                        .signal,
                    admitted
                        .observations
                        .single()
                        .signal,
                )
            } finally {
                firstDatabase.close()
            }

            /*
             * New RoomDatabase + new Store + new Family models the process-local objects being gone.
             * The database file is the only thing crossing this boundary.
             */
            val secondDatabase =
                database(
                    context =
                        context,
                    name =
                        databaseName,
                )

            try {
                val restoredStore =
                    RoomCgmStore(
                        dao =
                            secondDatabase
                                .cgmObservationDao(),
                    )
                val restoredFamily =
                    CgmFamily()
                val restoredLab =
                    CgmPartnerLab(
                        store =
                            restoredStore,
                        family =
                            restoredFamily,
                    )

                try {
                    val restoredState =
                        withTimeout(
                            2_000L,
                        ) {
                            restoredFamily
                                .states
                                .first { state ->
                                    state is
                                        CgmFamily.State.LatestAdmitted
                                }
                        } as
                            CgmFamily.State.LatestAdmitted

                    val restoredSnapshot =
                        restoredStore
                            .readCurrent()
                    val restoredObservation =
                        restoredSnapshot
                            .observations
                            .single()
                    val restoredSignal =
                        restoredObservation
                            .signal as
                            CgmPartner.Signal.Glucose

                    /*
                     * Historical meaning/provenance survived; process-local object identity did not.
                     */
                    assertEquals(
                        originalObservation,
                        restoredObservation,
                    )
                    assertNotSame(
                        originalObservation,
                        restoredObservation,
                    )
                    assertEquals(
                        originalObservation.signal,
                        restoredSignal,
                    )
                    assertNotSame(
                        originalObservation.signal,
                        restoredSignal,
                    )

                    /*
                     * Family uses the reconstructed Store value itself. It does not ask Partner
                     * Decoder/Receiver to manufacture another receipt during restoration.
                     */
                    assertSame(
                        restoredSignal,
                        restoredState.glucose,
                    )
                    assertEquals(
                        1,
                        restoredState
                            .storedObservationCount,
                    )

                    /*
                     * Restore itself inserted nothing.
                     */
                    assertEquals(
                        1,
                        restoredStore
                            .readCurrent()
                            .observations
                            .size,
                    )

                    /*
                     * The next live synthetic receipt continues the same acquisition epoch at 8,
                     * rather than starting a second sequence-1 history after restart.
                     */
                    restoredLab
                        .receiveSyntheticPacket()
                        .join()

                    val afterNextReceipt =
                        restoredStore
                            .readCurrent()

                    assertEquals(
                        3,
                        afterNextReceipt
                            .observations
                            .size,
                    )
                    assertEquals(
                        8L,
                        afterNextReceipt
                            .latestObservedGlucose
                            ?.sequence,
                    )
                    assertTrue(
                        afterNextReceipt
                            .latestObservedTemperature !=
                            null,
                    )
                } finally {
                    restoredLab.close()
                }
            } finally {
                secondDatabase.close()
                context.deleteDatabase(
                    databaseName,
                )
            }
        }

    private fun database(
        context:
            android.content.Context,
        name:
            String,
    ):
        VexLifeDatabase =
        Room.databaseBuilder(
            context,
            VexLifeDatabase::class.java,
            name,
        )
            .addMigrations(
                VexLifeDatabase.migration1To2,
                VexLifeDatabase.migration2To3,
                VexLifeDatabase.migration3To4,
                VexLifeDatabase.migration4To5,
                VexLifeDatabase.migration5To6,
            )
            .build()
}

// [VXG RealForever][VEX-CGM-LABS][ROOM-RESTART-PROOF]
