package com.vextreme.vexlife.android.testing.crossplatform.vexstream

import android.content.ComponentName
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.maps.InMemoryVexMapsRepository
import com.vextreme.vexlife.android.data.maps.distribution.InMemoryVexMapsDistributionRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.vexstream.PairedVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamOfflineIndex
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamPresentationRefs
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamTrackPresentationScope
import com.vextreme.vexlife.android.testing.crossplatform.VexCrossPlatformEvidence
import com.vextreme.vexlife.android.ui.VexLifeApp
import com.vextreme.vexlife.android.ui.VexLifeSession
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in lived V3 desktop <-> Android proof.
 *
 * Ordinary connected-device verification skips this test unless the host orchestrator explicitly
 * supplies [VexCrossPlatformEvidence.ARG_FIXTURE] = [FIXTURE_REF]. The semantic UI triggers and
 * production VexStream repository/media owners remain real; only unrelated app repositories are
 * replaced with in-memory test implementations.
 *
 * Canonical locator:
 * test.cross-platform.vexstream.v3-android.local-stream-offline/v1
 */
@RunWith(
    AndroidJUnit4::class,
)
class VexStreamV3LocalStreamOfflineCrossPlatformTest {
    @get:Rule
    val compose =
        createComposeRule()

    @Test
    fun pairStreamDownloadAndPlayLocalAfterDesktopStops() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation
                .targetContext
        val evidence =
            VexCrossPlatformEvidence(
                context = context,
                arguments =
                    InstrumentationRegistry
                        .getArguments(),
                fixtureRef =
                    FIXTURE_REF,
            )

        assumeTrue(
            "Cross-platform fixture not selected",
            evidence.enabled,
        )

        val expectedDigest =
            requireNotNull(
                evidence.argument(
                    ARG_EXPECTED_MEDIA_SHA256,
                ),
            ) {
                "$ARG_EXPECTED_MEDIA_SHA256 is required"
            }
                .lowercase()
        VexStreamMediaSha256(
            expectedDigest,
        )

        evidence.reset()
        resetVexStreamState(
            context.filesDir,
            VexStreamConnectionStore(
                context,
            ),
        )
        evidence.checkpoint(
            phaseRef =
                "android.fixture.ready",
            status = "PASS",
            details =
                mapOf(
                    "expectedMediaSha256" to
                        expectedDigest,
                ),
        )

        val vexStream =
            PairedVexStreamMusicRepository.production(
                context,
            )
        val repositories =
            AppRepositories(
                logHistory =
                    InMemoryLogHistoryRepository(),
                userProfile =
                    InMemoryUserProfileRepository(),
                vexMaps =
                    InMemoryVexMapsRepository(),
                vexMapsDistribution =
                    InMemoryVexMapsDistributionRepository(),
                vexStream = vexStream,
                events =
                    RelayEventsRepository(),
                eventsAttention =
                    RelayEventsAttentionPreferenceRepository(),
                eventsAttentionLab =
                    RelayEventsAttentionLabRepository(),
            )
        val sessionScope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.Main.immediate,
            )
        val session =
            VexLifeSession(
                repositories =
                    repositories,
                scope =
                    sessionScope,
            )
        val controllerFuture =
            MediaController.Builder(
                context,
                SessionToken(
                    context,
                    ComponentName(
                        context,
                        VexStreamPlaybackService::class.java,
                    ),
                ),
            ).buildAsync()

        try {
            compose.setContent {
                VexLifeApp(
                    session =
                        session,
                )
            }
            compose.waitForIdle()

            tap(
                HomeScreen
                    .ConnectVexStreamMusic
                    .elementId,
            )
            waitForTag(
                VexStreamMusicScreen
                    .FindNearby
                    .elementId,
            )
            evidence.checkpoint(
                phaseRef =
                    "android.vexstream.screen",
                status = "PASS",
            )

            tap(
                VexStreamMusicScreen
                    .FindNearby
                    .elementId,
            )
            compose.waitUntil(
                timeoutMillis =
                    DISCOVERY_TIMEOUT_MS,
            ) {
                val connection =
                    vexStream
                        .state
                        .value
                        .connectionState
                connection is
                    VexStreamConnectionState.ConfirmCode ||
                    connection is
                    VexStreamConnectionState.Failed
            }

            val confirmation =
                vexStream
                    .state
                    .value
                    .connectionState
                    as? VexStreamConnectionState.ConfirmCode

            if (
                confirmation == null
            ) {
                val failure =
                    vexStream
                        .state
                        .value
                        .connectionState as?
                        VexStreamConnectionState.Failed
                evidence.checkpoint(
                    phaseRef =
                        "android.discovery.resolve",
                    status = "FAIL",
                    details =
                        mapOf(
                            "connectionState" to
                                vexStream
                                    .state
                                    .value
                                    .connectionState
                                    .toString(),
                            "reasonCode" to
                                failure
                                    ?.reasonCode,
                        ),
                )
                error(
                    "VexStream discovery/TLS did not reach confirmation",
                )
            }

            evidence.checkpoint(
                phaseRef =
                    "android.discovery.resolve",
                status = "PASS",
                details =
                    mapOf(
                        "displayName" to
                            confirmation
                                .displayName,
                        "pairingCode" to
                            confirmation
                                .pairingCode,
                    ),
            )

            tap(
                VexStreamMusicScreen
                    .ConfirmPairingCode
                    .elementId,
            )
            compose.waitUntil(
                timeoutMillis =
                    PAIRING_REQUEST_TIMEOUT_MS,
            ) {
                when (
                    vexStream
                        .state
                        .value
                        .connectionState
                ) {
                    is VexStreamConnectionState.WaitingForDesktopApproval,
                    is VexStreamConnectionState.Paired,
                    is VexStreamConnectionState.Failed,
                    -> true

                    else -> false
                }
            }
            evidence.checkpoint(
                phaseRef =
                    "android.pairing.requested",
                status = "PASS",
            )

            compose.waitUntil(
                timeoutMillis =
                    PAIRING_APPROVAL_TIMEOUT_MS,
            ) {
                val state =
                    vexStream
                        .state
                        .value
                (
                    state.connectionState is
                        VexStreamConnectionState.Paired &&
                        state.hubState is
                        VexStreamHubState.Ready
                ) ||
                    state.connectionState is
                    VexStreamConnectionState.Failed
            }

            val pairedState =
                vexStream
                    .state
                    .value
            check(
                pairedState.connectionState is
                    VexStreamConnectionState.Paired &&
                    pairedState.hubState is
                    VexStreamHubState.Ready
            ) {
                "Pairing/catalog did not become ready: " +
                    pairedState
                        .connectionState +
                    " / " +
                    pairedState
                        .hubState
            }

            val target =
                pairedState
                    .tracks
                    .singleOrNull {
                        it.track
                            .mediaSha256
                            .value ==
                            expectedDigest
                    }
                    ?: error(
                        "Expected V3 fixture digest was not present in Android catalog",
                    )

            evidence.checkpoint(
                phaseRef =
                    "android.catalog.fixture",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to
                            target
                                .track
                                .trackRef
                                .value,
                        "mediaSha256" to
                            target
                                .track
                                .mediaSha256
                                .value,
                        "mediaByteLength" to
                            target
                                .track
                                .mediaByteLength,
                    ),
            )

            val controller =
                controllerFuture
                    .get(
                        MEDIA_CONTROLLER_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    )

            val playActionId =
                VexStreamPresentationRefs
                    .trackRowAction(
                        scope =
                            VexStreamTrackPresentationScope.Songs,
                        trackRef =
                            target
                                .track
                                .trackRef,
                        action =
                            VexStreamMusicScreen.PlayTrack,
                    )
            tap(
                playActionId,
            )

            compose.waitUntil(
                timeoutMillis =
                    STREAM_TIMEOUT_MS,
            ) {
                val item =
                    controller
                        .currentMediaItem
                item
                    ?.mediaId ==
                    target
                        .track
                        .trackRef
                        .value &&
                    item.localConfiguration
                        ?.uri
                        ?.scheme ==
                    "vexstream" &&
                    controller.playerError ==
                    null &&
                    controller.playbackState in
                    setOf(
                        Player.STATE_BUFFERING,
                        Player.STATE_READY,
                        Player.STATE_ENDED,
                    )
            }

            evidence.checkpoint(
                phaseRef =
                    "android.stream.media3",
                status = "PASS",
                details =
                    mapOf(
                        "mediaId" to
                            controller
                                .currentMediaItem
                                ?.mediaId,
                        "uriScheme" to
                            controller
                                .currentMediaItem
                                ?.localConfiguration
                                ?.uri
                                ?.scheme,
                        "playbackState" to
                            controller
                                .playbackState,
                    ),
            )

            tap(
                VexStreamPresentationRefs
                    .trackRowAction(
                        scope =
                            VexStreamTrackPresentationScope.Songs,
                        trackRef =
                            target
                                .track
                                .trackRef,
                        action =
                            VexStreamMusicScreen.TrackMenu,
                    ),
            )
            tap(
                VexStreamPresentationRefs
                    .trackMenuAction(
                        scope =
                            VexStreamTrackPresentationScope.Songs,
                        trackRef =
                            target
                                .track
                                .trackRef,
                        action =
                            VexStreamMusicScreen.DownloadTrack,
                    ),
            )

            compose.waitUntil(
                timeoutMillis =
                    DOWNLOAD_TIMEOUT_MS,
            ) {
                when (
                    vexStream
                        .state
                        .value
                        .track(
                            target
                                .track
                                .trackRef,
                        )
                        ?.localCopy
                ) {
                    is VexStreamLocalCopyState.Downloaded,
                    is VexStreamLocalCopyState.Failed,
                    -> true

                    else -> false
                }
            }

            val downloaded =
                vexStream
                    .state
                    .value
                    .track(
                        target
                            .track
                            .trackRef,
                    )
                    ?.localCopy
                    as? VexStreamLocalCopyState.Downloaded
                    ?: error(
                        "Android did not retain a verified local copy",
                    )

            assertEquals(
                target
                    .track
                    .mediaSha256,
                downloaded
                    .mediaSha256,
            )
            assertEquals(
                target
                    .track
                    .mediaByteLength,
                downloaded
                    .byteLength,
            )

            evidence.checkpoint(
                phaseRef =
                    "android.download.verified",
                status = "PASS",
                details =
                    mapOf(
                        "mediaSha256" to
                            downloaded
                                .mediaSha256
                                .value,
                        "byteLength" to
                            downloaded
                                .byteLength,
                        "localMediaRef" to
                            downloaded
                                .localMediaRef,
                    ),
            )
            evidence.checkpoint(
                phaseRef =
                    "android.awaiting-desktop-stop",
                status = "WAITING",
            )

            val hostStopReceipt =
                evidence.waitForHostMarker(
                    markerRef =
                        HOST_MARKER_DESKTOP_STOPPED,
                    timeoutMs =
                        HOST_STOP_TIMEOUT_MS,
                )
            evidence.checkpoint(
                phaseRef =
                    "android.desktop-stop-observed",
                status = "PASS",
                details =
                    mapOf(
                        "hostReceipt" to
                            hostStopReceipt,
                    ),
            )

            controller.pause()
            controller.clearMediaItems()

            tap(
                VexStreamMusicScreen
                    .ManageConnection
                    .elementId,
            )
            waitForTag(
                VexStreamMusicScreen
                    .RefreshCatalog
                    .elementId,
            )
            tap(
                VexStreamMusicScreen
                    .RefreshCatalog
                    .elementId,
            )

            compose.waitUntil(
                timeoutMillis =
                    HUB_UNREACHABLE_TIMEOUT_MS,
            ) {
                vexStream
                    .state
                    .value
                    .hubState is
                    VexStreamHubState.Unreachable
            }
            tap(
                VexStreamMusicScreen
                    .CloseConnectionMenu
                    .elementId,
            )

            val localOnly =
                requireNotNull(
                    vexStream
                        .state
                        .value
                        .track(
                            target
                                .track
                                .trackRef,
                        ),
                )
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                localOnly
                    .availability,
            )
            evidence.checkpoint(
                phaseRef =
                    "android.hub-unreachable-local-only",
                status = "PASS",
            )

            tap(
                playActionId,
            )
            compose.waitUntil(
                timeoutMillis =
                    LOCAL_PLAYBACK_TIMEOUT_MS,
            ) {
                val item =
                    controller
                        .currentMediaItem
                item
                    ?.mediaId ==
                    target
                        .track
                        .trackRef
                        .value &&
                    item.localConfiguration
                        ?.uri
                        ?.scheme ==
                    "file" &&
                    controller.playerError ==
                    null &&
                    controller.playbackState in
                    setOf(
                        Player.STATE_BUFFERING,
                        Player.STATE_READY,
                        Player.STATE_ENDED,
                    )
            }

            evidence.checkpoint(
                phaseRef =
                    "android.local-playback.media3",
                status = "PASS",
                details =
                    mapOf(
                        "mediaId" to
                            controller
                                .currentMediaItem
                                ?.mediaId,
                        "uriScheme" to
                            controller
                                .currentMediaItem
                                ?.localConfiguration
                                ?.uri
                                ?.scheme,
                        "playbackState" to
                            controller
                                .playbackState,
                    ),
            )

            val restored =
                VexStreamOfflineIndex.production(
                    contextFilesDir =
                        context.filesDir,
                    mediaStore =
                        VexStreamMediaStore.production(
                            contextFilesDir =
                                context.filesDir,
                        ),
                )
                    .load()
                    .track(
                        target
                            .track
                            .trackRef,
                    )
            assertNotNull(
                restored,
            )
            assertTrue(
                restored
                    ?.localCopy is
                    VexStreamLocalCopyState.Downloaded,
            )
            evidence.checkpoint(
                phaseRef =
                    "android.offline-index.reload",
                status = "PASS",
            )
            evidence.checkpoint(
                phaseRef =
                    "android.complete",
                status = "PASS",
            )
        } catch (
            failure: Throwable,
        ) {
            evidence.checkpoint(
                phaseRef =
                    "android.complete",
                status = "FAIL",
                details =
                    mapOf(
                        "type" to
                            failure
                                .javaClass
                                .name,
                        "message" to
                            failure
                                .message,
                        "connectionState" to
                            runCatching {
                                vexStream
                                    .state
                                    .value
                                    .connectionState
                                    .toString()
                            }
                                .getOrNull(),
                        "hubState" to
                            runCatching {
                                vexStream
                                    .state
                                    .value
                                    .hubState
                                    .toString()
                            }
                                .getOrNull(),
                    ),
            )
            throw failure
        } finally {
            MediaController.releaseFuture(
                controllerFuture,
            )
            sessionScope.cancel()
        }
    }

    private fun tap(
        elementId: String,
    ) {
        waitForTag(
            elementId,
        )
        runCatching {
            compose
                .onNodeWithTag(
                    elementId,
                )
                .performScrollTo()
        }
        compose
            .onNodeWithTag(
                elementId,
            )
            .performClick()
        compose.waitForIdle()
    }

    private fun waitForTag(
        elementId: String,
    ) {
        compose.waitUntil(
            timeoutMillis =
                UI_TIMEOUT_MS,
        ) {
            compose
                .onAllNodesWithTag(
                    elementId,
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun resetVexStreamState(
        filesDir: File,
        connectionStore: VexStreamConnectionStore,
    ) {
        connectionStore.clear()
        File(
            filesDir,
            "vexstream",
        )
            .deleteRecursively()
    }

    companion object {
        private const val FIXTURE_REF =
            "vexstream-v3-local-stream-offline"
        private const val ARG_EXPECTED_MEDIA_SHA256 =
            "vexCrossPlatformExpectedMediaSha256"
        private const val HOST_MARKER_DESKTOP_STOPPED =
            "desktop-stopped"

        private const val UI_TIMEOUT_MS =
            10_000L
        private const val DISCOVERY_TIMEOUT_MS =
            55_000L
        private const val PAIRING_REQUEST_TIMEOUT_MS =
            10_000L
        private const val PAIRING_APPROVAL_TIMEOUT_MS =
            90_000L
        private const val STREAM_TIMEOUT_MS =
            30_000L
        private const val DOWNLOAD_TIMEOUT_MS =
            60_000L
        private const val HOST_STOP_TIMEOUT_MS =
            90_000L
        private const val HUB_UNREACHABLE_TIMEOUT_MS =
            20_000L
        private const val LOCAL_PLAYBACK_TIMEOUT_MS =
            30_000L
        private const val MEDIA_CONTROLLER_TIMEOUT_SECONDS =
            15L
    }
}

// [VXG RealForever]
