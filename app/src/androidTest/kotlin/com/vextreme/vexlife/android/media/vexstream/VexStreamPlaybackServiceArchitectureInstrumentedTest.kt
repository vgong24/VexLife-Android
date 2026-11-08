package com.vextreme.vexlife.android.media.vexstream

import android.content.ComponentName
import android.os.Looper
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.VexApplication
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native smoke for the Playback Service side of the shared application Family circuit.
 *
 * Source/JVM proof owns the semantic identity assertion: VexApplication exposes one memoized
 * VexStreamPlaybackFamily and VexStreamPlaybackService resolves that exact application provider.
 * This connected proof owns the Android boundary that source proof cannot: the real
 * MediaSessionService must construct successfully and accept a MediaController connection while the
 * application-scoped semantic owner remains the same memoized object.
 *
 * The test deliberately does not mutate the playback queue or persisted app data.
 */
@RunWith(AndroidJUnit4::class)
class VexStreamPlaybackServiceArchitectureInstrumentedTest {
    @Test
    fun mediaSessionServiceStartsAgainstTheProcessSharedPlaybackFamily() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .applicationContext
        val application =
            context as
                VexApplication

        val before =
            application
                .dependencies
                .resolve(
                    application
                        .dependencyModule
                        .playbackFamily,
                )

        val controllerFuture =
            MediaController
                .Builder(
                    context,
                    SessionToken(
                        context,
                        ComponentName(
                            context,
                            VexStreamPlaybackService::class.java,
                        ),
                    ),
                )
                .setApplicationLooper(
                    Looper.getMainLooper(),
                )
                .buildAsync()
        val completed =
            CountDownLatch(
                1,
            )
        val failure =
            AtomicReference<
                Throwable?
            >()

        controllerFuture.addListener(
            {
                try {
                    val controller =
                        controllerFuture.get(
                            15L,
                            TimeUnit.SECONDS,
                        )

                    assertTrue(
                        "MediaController must connect to the production Playback MediaSessionService",
                        controller.isConnected,
                    )

                    val after =
                        application
                            .dependencies
                            .resolve(
                                application
                                    .dependencyModule
                                    .playbackFamily,
                            )

                    assertSame(
                        "Playback Service startup must not replace the process-scoped Playback Family",
                        before,
                        after,
                    )
                } catch (
                    throwable:
                        Throwable
                ) {
                    failure.set(
                        throwable,
                    )
                } finally {
                    MediaController.releaseFuture(
                        controllerFuture,
                    )
                    completed.countDown()
                }
            },
            ContextCompat.getMainExecutor(
                context,
            ),
        )

        assertTrue(
            "MediaController provider-lifecycle assertion timed out",
            completed.await(
                15L,
                TimeUnit.SECONDS,
            ),
        )

        failure.get()
            ?.let { throwable ->
                throw AssertionError(
                    "MediaController provider-lifecycle assertion failed",
                    throwable,
                )
            }
    }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE][NATIVE-PROOF]
