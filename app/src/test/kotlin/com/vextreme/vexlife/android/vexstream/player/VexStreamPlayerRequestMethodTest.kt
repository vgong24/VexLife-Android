package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffect
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffect
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffect
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlayerRequestMethodTest {
    @Test
    fun finiteMethodsReportProviderDispatchRefusalAsLawfulFailure() =
        runBlocking {
            val connection =
                FakeRequestConnection(
                    dispatched =
                        false,
                )

            assertTrue(
                VexStreamPlaybackFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamPlaybackEffect.RequestPlay,
                ).invoke() is
                    VexMethod.Result.Failure,
            )
            assertTrue(
                VexStreamQueueFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamQueueEffect.SeekNext,
                ).invoke() is
                    VexMethod.Result.Failure,
            )
            assertTrue(
                VexStreamRepeatFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamRepeatEffect.RequestEnableAll,
                ).invoke() is
                    VexMethod.Result.Failure,
            )
        }

    @Test
    fun finiteMethodsReportDispatchedProviderRequestsAsSuccess() =
        runBlocking {
            val connection =
                FakeRequestConnection(
                    dispatched =
                        true,
                )

            assertTrue(
                VexStreamPlaybackFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamPlaybackEffect.RequestPause,
                ).invoke() is
                    VexMethod.Result.Success,
            )
            assertTrue(
                VexStreamQueueFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamQueueEffect.ClearQueue,
                ).invoke() is
                    VexMethod.Result.Success,
            )
            assertTrue(
                VexStreamRepeatFamily.Act.Method(
                    connection =
                        connection,
                    effect =
                        VexStreamRepeatEffect.RequestDisable,
                ).invoke() is
                    VexMethod.Result.Success,
            )
        }

    private class FakeRequestConnection(
        private val dispatched:
            Boolean,
    ) :
        VexStreamPlayerPartner.Connection.Request {
        override val id =
            VexStreamPlayerPartner.Connection.Id(
                value =
                    "test-player-request",
                occurrence =
                    1L,
            )

        override suspend fun play():
            Boolean =
            dispatched

        override suspend fun pause():
            Boolean =
            dispatched

        override suspend fun restartCurrent():
            Boolean =
            dispatched

        override suspend fun previous():
            Boolean =
            dispatched

        override suspend fun next():
            Boolean =
            dispatched

        override suspend fun shuffleFuture():
            Boolean =
            dispatched

        override suspend fun clearQueue():
            Boolean =
            dispatched

        override suspend fun setRepeatEnabled(
            enabled:
                Boolean,
        ):
            Boolean =
            dispatched
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-REQUEST][METHOD-PROOF]
