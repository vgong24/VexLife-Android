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
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamPresentationRefs
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamTrackPresentationScope
import com.vextreme.vexlife.android.testing.crossplatform.VexCrossPlatformEvidence
import com.vextreme.vexlife.android.ui.VexLifeApp
import com.vextreme.vexlife.android.ui.VexLifeSession
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in lived VexStream Desktop V4 <-> Android proof.
 *
 * Canonical relationship:
 *   test.cross-platform.vexstream.v4-android.local-stream-offline/v1
 *
 * This first safe implementation deliberately uses an already-approved Desktop/Android pairing.
 * It never clears, replaces, or revokes the human's ordinary VexStream pairing identity. A future
 * fresh-pair-from-zero mode requires a test-owned pairing namespace before it may be admitted.
 *
 * The real VexLife process restart is proved by two separate host-selected instrumentation phases:
 *
 *   Phase A
 *     existing pairing -> fixture catalog -> stream -> queue -> verified download
 *     -> Desktop stopped -> LOCAL_ONLY playback
 *
 *   HOST
 *     force-stop VexLife -> verify stopped -> invoke Phase B in a fresh instrumentation process
 *
 *   Phase B
 *     durable offline index restored -> local playback
 *     -> Desktop restarted -> reconnect -> remove fixture download -> catalog identity remains
 *
 * Ordinary connected-device suites skip this proof unless the exact fixture and phase arguments are
 * supplied. Logical child paths remain sequential on one physical Android device.
 */
@RunWith(AndroidJUnit4::class)
class VexStreamV4LocalStreamOfflineCrossPlatformTest {
    @get:Rule
    val compose =
        createComposeRule()

    @Test
    fun phaseAStreamQueueDownloadAndPlayLocalAfterDesktopStops() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation
                .targetContext
        val evidence =
            selectedEvidence(
                expectedPhase = PHASE_A,
            )

        requireSafeExistingPairingMode(
            evidence,
        )
        val expectedDigest =
            requireFixtureDigest(
                evidence,
            )

        evidence.resetRunEvidence()

        val connectionStore =
            VexStreamConnectionStore(
                context,
            )
        val remembered =
            connectionStore.load()
                ?: error(
                    "EXISTING_PAIRING_SAFE_MODE requires an already-approved " +
                        "VexStream pairing; ordinary pairing state was left untouched",
                )

        evidence.checkpoint(
            phaseRef = "android.fixture.ready",
            status = "PASS",
            details =
                mapOf(
                    "pairingMode" to PAIRING_MODE_EXISTING,
                    "displayName" to remembered.displayName,
                    "expectedMediaSha256" to expectedDigest,
                ),
        )

        val vexStream =
            PairedVexStreamMusicRepository
                .production(
                    context,
                )

        runBlocking {
            vexStream
                .reconnectPairedVexStream()
        }

        val ready =
            vexStream.state.value
        check(
            ready.hubState is
                VexStreamHubState.Ready
        ) {
            "Existing VexStream pairing did not reconnect to a ready Desktop V4 Hub: " +
                ready.hubState
        }

        val targetRef =
            targetTrackRef(
                vexStream = vexStream,
                expectedDigest = expectedDigest,
            )

        evidence.checkpoint(
            phaseRef = "android.catalog.fixture",
            status = "PASS",
            details =
                fixtureDetails(
                    vexStream = vexStream,
                    targetRef = targetRef,
                ),
        )

        withApp(
            vexStream = vexStream,
        ) { controller ->
            tap(
                HomeScreen
                    .VexStreamMusic
                    .elementId,
            )

            waitForTag(
                trackRow(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                ),
            )
            evidence.screenshot(
                "android.catalog.fixture",
            )

            val songsPlay =
                trackRowAction(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.PlayTrack,
                )

            tap(
                songsPlay,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "vexstream",
            )
            evidence.checkpoint(
                phaseRef = "android.playback.stream",
                status = "PASS",
                details =
                    playbackDetails(
                        controller,
                    ),
            )
            evidence.screenshot(
                "android.playback.stream",
            )

            tap(
                trackRowAction(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.TrackMenu,
                ),
            )
            tap(
                VexStreamPresentationRefs
                    .trackMenuAction(
                        scope =
                            VexStreamTrackPresentationScope.Songs,
                        trackRef = targetRef,
                        action =
                            VexStreamMusicScreen.AddTrackToQueue,
                    ),
            )
            tap(
                VexStreamMusicScreen
                    .ShowQueue
                    .elementId,
            )

            val secondOccurrence =
                VexStreamPresentationRefs
                    .queueEntryKey(
                        mediaId =
                            targetRef.value,
                        occurrence = 2,
                    )

            waitForTag(
                VexStreamPresentationRefs
                    .queueEntry(
                        secondOccurrence,
                    ),
            )
            evidence.checkpoint(
                phaseRef = "android.queue.fixture",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                        "queueOccurrence" to 2,
                    ),
            )
            evidence.screenshot(
                "android.queue.fixture",
            )
            tap(
                VexStreamMusicScreen
                    .CloseQueue
                    .elementId,
            )

            tap(
                trackRowAction(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.DownloadTrack,
                ),
            )

            compose.waitUntil(
                timeoutMillis =
                    DOWNLOAD_TIMEOUT_MS,
            ) {
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    )
                    ?.localCopy is
                    VexStreamLocalCopyState.Downloaded
            }

            val downloaded =
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    )
                    ?.localCopy
                    as? VexStreamLocalCopyState.Downloaded
                    ?: error(
                        "fixture did not reach Downloaded state",
                    )

            assertEquals(
                expectedDigest,
                downloaded
                    .mediaSha256
                    .value,
            )
            assertTrue(
                downloaded.byteLength > 0L,
            )

            evidence.checkpoint(
                phaseRef = "android.download.verified",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
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
            evidence.screenshot(
                "android.download.verified",
            )

            evidence.checkpoint(
                phaseRef = "android.awaiting-desktop-stop",
                status = "WAITING",
                details =
                    mapOf(
                        "hostMarkerRelativePath" to
                            evidence
                                .hostMarkerRelativePath(
                                    HOST_MARKER_DESKTOP_STOPPED,
                                ),
                    ),
            )

            val hostStopReceipt =
                evidence.waitForHostMarker(
                    markerRef =
                        HOST_MARKER_DESKTOP_STOPPED,
                    timeoutMs =
                        HOST_SYNC_TIMEOUT_MS,
                )

            evidence.checkpoint(
                phaseRef = "android.desktop-stop-observed",
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
                            targetRef,
                        ),
                )
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                localOnly.availability,
            )

            evidence.checkpoint(
                phaseRef = "android.hub.local-only",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                    ),
            )
            evidence.screenshot(
                "android.hub.local-only",
            )

            tap(
                songsPlay,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "file",
            )

            evidence.checkpoint(
                phaseRef = "android.playback.local",
                status = "PASS",
                details =
                    playbackDetails(
                        controller,
                    ),
            )
            evidence.screenshot(
                "android.playback.local",
            )

            evidence.checkpoint(
                phaseRef = "android.phase-a.complete",
                status = "PASS",
                details =
                    mapOf(
                        "nextHostEffect" to
                            "FORCE_STOP_AND_START_PHASE_B",
                    ),
            )
        }
    }

    @Test
    fun phaseBRestorePlayReconnectRemoveAndKeepCatalogIdentity() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation
                .targetContext
        val evidence =
            selectedEvidence(
                expectedPhase = PHASE_B,
            )

        requireSafeExistingPairingMode(
            evidence,
        )
        val expectedDigest =
            requireFixtureDigest(
                evidence,
            )

        val vexStream =
            PairedVexStreamMusicRepository
                .production(
                    context,
                )

        val targetRef =
            targetTrackRef(
                vexStream = vexStream,
                expectedDigest = expectedDigest,
            )
        val restored =
            requireNotNull(
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    ),
            )

        assertEquals(
            VexStreamTrackAvailability.LOCAL_ONLY,
            restored.availability,
        )
        assertTrue(
            restored.localCopy is
                VexStreamLocalCopyState.Downloaded,
        )

        evidence.checkpoint(
            phaseRef = "android.offline-index.restored",
            status = "PASS",
            details =
                fixtureDetails(
                    vexStream = vexStream,
                    targetRef = targetRef,
                ),
        )

        withApp(
            vexStream = vexStream,
        ) { controller ->
            tap(
                HomeScreen
                    .VexStreamMusic
                    .elementId,
            )
            tap(
                VexStreamMusicScreen
                    .DownloadsTab
                    .elementId,
            )

            waitForTag(
                trackRow(
                    scope =
                        VexStreamTrackPresentationScope.Downloads,
                    targetRef = targetRef,
                ),
            )

            tap(
                trackRowAction(
                    scope =
                        VexStreamTrackPresentationScope.Downloads,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.PlayTrack,
                ),
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "file",
            )

            evidence.checkpoint(
                phaseRef = "android.playback.restored",
                status = "PASS",
                details =
                    playbackDetails(
                        controller,
                    ),
            )
            evidence.screenshot(
                "android.playback.restored",
            )

            evidence.checkpoint(
                phaseRef = "android.awaiting-desktop-restart",
                status = "WAITING",
                details =
                    mapOf(
                        "hostMarkerRelativePath" to
                            evidence
                                .hostMarkerRelativePath(
                                    HOST_MARKER_DESKTOP_RESTARTED,
                                ),
                    ),
            )

            val hostRestartReceipt =
                evidence.waitForHostMarker(
                    markerRef =
                        HOST_MARKER_DESKTOP_RESTARTED,
                    timeoutMs =
                        HOST_SYNC_TIMEOUT_MS,
                )

            evidence.checkpoint(
                phaseRef = "android.desktop-restart-observed",
                status = "PASS",
                details =
                    mapOf(
                        "hostReceipt" to
                            hostRestartReceipt,
                    ),
            )

            tap(
                VexStreamMusicScreen
                    .ManageConnection
                    .elementId,
            )
            tap(
                VexStreamMusicScreen
                    .Reconnect
                    .elementId,
            )

            compose.waitUntil(
                timeoutMillis =
                    RECONNECT_TIMEOUT_MS,
            ) {
                vexStream
                    .state
                    .value
                    .hubState is
                    VexStreamHubState.Ready
            }

            tap(
                VexStreamMusicScreen
                    .CloseConnectionMenu
                    .elementId,
            )

            val reconnected =
                requireNotNull(
                    vexStream
                        .state
                        .value
                        .track(
                            targetRef,
                        ),
                )
            assertEquals(
                VexStreamTrackAvailability.HUB_AND_LOCAL,
                reconnected.availability,
            )

            tap(
                VexStreamMusicScreen
                    .DownloadsTab
                    .elementId,
            )
            tap(
                trackRowAction(
                    scope =
                        VexStreamTrackPresentationScope.Downloads,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.RemoveTrackDownload,
                ),
            )

            compose.waitUntil(
                timeoutMillis =
                    UI_TIMEOUT_MS,
            ) {
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    )
                    ?.localCopy is
                    VexStreamLocalCopyState.NotDownloaded
            }

            tap(
                VexStreamMusicScreen
                    .SongsTab
                    .elementId,
            )
            waitForTag(
                trackRow(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                ),
            )

            assertNotNull(
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    ),
            )

            evidence.checkpoint(
                phaseRef = "android.download.removed",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                        "catalogIdentityPreserved" to true,
                    ),
            )
            evidence.screenshot(
                "android.download.removed",
            )

            evidence.checkpoint(
                phaseRef = "android.phase-b.complete",
                status = "PASS",
                details =
                    mapOf(
                        "nextHostEffect" to
                            "REMOVE_TEST_SOURCE_THEN_DELETE_FIXTURE_ROOT",
                    ),
            )
        }
    }

    private fun selectedEvidence(
        expectedPhase: String,
    ): VexCrossPlatformEvidence {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val arguments =
            InstrumentationRegistry
                .getArguments()

        assumeTrue(
            "Cross-platform fixture not selected",
            arguments
                .getString(
                    VexCrossPlatformEvidence.ARG_FIXTURE,
                )
                ?.trim() ==
                FIXTURE_REF,
        )
        assumeTrue(
            "Cross-platform phase not selected",
            arguments
                .getString(
                    VexCrossPlatformEvidence.ARG_PHASE,
                )
                ?.trim()
                ?.uppercase() ==
                expectedPhase,
        )

        return VexCrossPlatformEvidence(
            context =
                instrumentation
                    .targetContext,
            arguments = arguments,
            fixtureRef = FIXTURE_REF,
        )
    }

    private fun requireSafeExistingPairingMode(
        evidence: VexCrossPlatformEvidence,
    ) {
        val mode =
            evidence.argument(
                ARG_PAIRING_MODE,
            )
        check(
            mode ==
                PAIRING_MODE_EXISTING,
        ) {
            "Only EXISTING_PAIRING_SAFE_MODE is admitted by the first V4 lived harness; " +
                "fresh-pair automation remains held until a test-owned pairing namespace exists"
        }
    }

    private fun requireFixtureDigest(
        evidence: VexCrossPlatformEvidence,
    ): String {
        val digest =
            requireNotNull(
                evidence.argument(
                    ARG_EXPECTED_MEDIA_SHA256,
                ),
            ) {
                "$ARG_EXPECTED_MEDIA_SHA256 is required"
            }
                .lowercase()

        VexStreamMediaSha256(
            digest,
        )

        check(
            digest ==
                FIXTURE_SHA256,
        ) {
            "cross-platform fixture digest does not match the canonical VexStream test tone"
        }

        return digest
    }

    private fun targetTrackRef(
        vexStream: PairedVexStreamMusicRepository,
        expectedDigest: String,
    ): VexStreamTrackRef =
        vexStream
            .state
            .value
            .tracks
            .singleOrNull { item ->
                item.track
                    .mediaSha256
                    .value ==
                    expectedDigest
            }
            ?.track
            ?.trackRef
            ?: error(
                "exact VexStream fixture digest was not present in Android state",
            )

    private fun fixtureDetails(
        vexStream: PairedVexStreamMusicRepository,
        targetRef: VexStreamTrackRef,
    ): Map<String, Any?> {
        val item =
            requireNotNull(
                vexStream
                    .state
                    .value
                    .track(
                        targetRef,
                    ),
            )

        return mapOf(
            "trackRef" to
                item
                    .track
                    .trackRef
                    .value,
            "mediaSha256" to
                item
                    .track
                    .mediaSha256
                    .value,
            "mediaByteLength" to
                item
                    .track
                    .mediaByteLength,
            "availability" to
                item
                    .availability
                    .name,
        )
    }

    private fun withApp(
        vexStream: PairedVexStreamMusicRepository,
        block: (MediaController) -> Unit,
    ) {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
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

        try {
            compose.setContent {
                VexLifeApp(
                    session =
                        session,
                )
            }
            compose.waitForIdle()

            val controller =
                controllerFuture
                    .get(
                        MEDIA_CONTROLLER_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS,
                    )

            block(
                controller,
            )
        } finally {
            MediaController.releaseFuture(
                controllerFuture,
            )
            sessionScope.cancel()
        }
    }

    private fun waitForPlayback(
        controller: MediaController,
        targetRef: VexStreamTrackRef,
        expectedScheme: String,
    ) {
        compose.waitUntil(
            timeoutMillis =
                PLAYBACK_TIMEOUT_MS,
        ) {
            val item =
                controller
                    .currentMediaItem

            item
                ?.mediaId ==
                targetRef.value &&
                item.localConfiguration
                    ?.uri
                    ?.scheme ==
                expectedScheme &&
                controller.playerError ==
                null &&
                controller.playbackState in
                setOf(
                    Player.STATE_BUFFERING,
                    Player.STATE_READY,
                    Player.STATE_ENDED,
                )
        }
    }

    private fun playbackDetails(
        controller: MediaController,
    ): Map<String, Any?> =
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
        )

    private fun trackRow(
        scope: VexStreamTrackPresentationScope,
        targetRef: VexStreamTrackRef,
    ): String =
        VexStreamPresentationRefs
            .trackRow(
                scope = scope,
                trackRef = targetRef,
            )

    private fun trackRowAction(
        scope: VexStreamTrackPresentationScope,
        targetRef: VexStreamTrackRef,
        action:
            com.vextreme.vexlife.android.elements.ActionElement,
    ): String =
        VexStreamPresentationRefs
            .trackRowAction(
                scope = scope,
                trackRef = targetRef,
                action = action,
            )

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

    companion object {
        private const val FIXTURE_REF =
            "vexstream-v4-local-stream-offline"

        private const val FIXTURE_SHA256 =
            "602e675b3d27d2496ac968172b68ca1dc74ddaec455be92d41b51ec65fa359a7"

        private const val ARG_EXPECTED_MEDIA_SHA256 =
            "vexCrossPlatformExpectedMediaSha256"

        private const val ARG_PAIRING_MODE =
            "vexCrossPlatformPairingMode"

        private const val PAIRING_MODE_EXISTING =
            "EXISTING_PAIRING_SAFE_MODE"

        private const val PHASE_A =
            "A"

        private const val PHASE_B =
            "B"

        private const val HOST_MARKER_DESKTOP_STOPPED =
            "desktop-stopped"

        private const val HOST_MARKER_DESKTOP_RESTARTED =
            "desktop-restarted"

        private const val UI_TIMEOUT_MS =
            10_000L

        private const val DOWNLOAD_TIMEOUT_MS =
            60_000L

        private const val PLAYBACK_TIMEOUT_MS =
            30_000L

        private const val HUB_UNREACHABLE_TIMEOUT_MS =
            20_000L

        private const val RECONNECT_TIMEOUT_MS =
            30_000L

        private const val HOST_SYNC_TIMEOUT_MS =
            120_000L

        private const val MEDIA_CONTROLLER_TIMEOUT_SECONDS =
            15L
    }
}

// [VXG RealForever]
