package com.vextreme.vexlife.android.media.vexstream

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.VexApplication
import java.util.concurrent.TimeUnit
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
    fun applicationEstablishesBackgroundRelayBeforeAnyServiceNeedsIt() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .applicationContext
        val application =
            context as
                VexApplication

        val providerName =
            application
                .dependencyModule
                .backgroundServiceRelay
                .readableName

        val relationship =
            application
                .dependencies
                .relationships()
                .single { candidate ->
                    candidate.provider ==
                        providerName
                }

        assertTrue(
            "VexApplication must establish app-scope background convergence during process startup",
            relationship.instantiated,
        )
    }

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
                .buildAsync()

        val controller =
            controllerFuture.get(
                15L,
                TimeUnit.SECONDS,
            )

        try {
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
        } finally {
            controller.release()
        }
    }
}

// [VXG RealForever][VEXARCHITECTURE][BACKGROUND-SERVICE][NATIVE-PROOF]
