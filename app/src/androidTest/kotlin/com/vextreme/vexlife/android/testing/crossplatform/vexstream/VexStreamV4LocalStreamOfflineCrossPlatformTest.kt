package com.vextreme.vexlife.android.testing.crossplatform.vexstream

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
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
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
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
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/**
 * Opt-in lived VexStream Desktop V4 <-> Android proof.
 *
 * Canonical relationship:
 *   test.cross-platform.vexstream.v4-android.local-stream-offline/v1
 *
 * Pairing mode is explicit. EXISTING_PAIRING_SAFE_MODE preserves an already-approved ordinary
 * pairing. FRESH_TEST_PAIRING_NAMESPACE uses Android device-protected storage for an isolated
 * pairing/offline namespace that survives the real process restart without clearing ordinary
 * credential-protected VexLife pairing/download state.
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
    private var activeEvidence:
        VexCrossPlatformEvidence? =
        null

    @get:Rule
    val failureEvidence =
        object : TestWatcher() {
            override fun failed(
                e: Throwable,
                description: Description,
            ) {
                activeEvidence
                    ?.failure(
                        e,
                    )
            }
        }

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
        activeEvidence =
            evidence

        val pairingMode =
            requirePairingMode(
                evidence,
            )
        val expectedDigest =
            requireFixtureDigest(
                evidence,
            )

        evidence.resetRunEvidence()

        val vexStreamContext =
            pairingContext(
                productContext = context,
                pairingMode = pairingMode,
            )
        val connectionStore =
            VexStreamConnectionStore(
                vexStreamContext,
            )

        if (
            pairingMode ==
            PairingMode.FRESH_TEST_PAIRING_NAMESPACE
        ) {
            resetFreshTestNamespace(
                context = vexStreamContext,
                connectionStore = connectionStore,
            )
        }

        val deviceRef =
            connectionStore.deviceRef()

        evidence.checkpoint(
            phaseRef = "android.fixture.ready",
            status = "PASS",
            details =
                mapOf(
                    "pairingMode" to pairingMode.name,
                    "deviceRef" to deviceRef,
                    "expectedMediaSha256" to expectedDigest,
                ),
        )

        val vexStream =
            PairedVexStreamMusicRepository
                .production(
                    vexStreamContext,
                )

        if (
            pairingMode ==
            PairingMode.EXISTING_PAIRING_SAFE_MODE
        ) {
            check(
                connectionStore.load() != null,
            ) {
                "EXISTING_PAIRING_SAFE_MODE requires an already-approved VexStream pairing"
            }
            runBlocking {
                vexStream
                    .reconnectPairedVexStream()
            }
        }

        withApp(
            vexStream = vexStream,
        ) { controller ->
            val openVexStreamAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.open-vexstream",
                    expectedResultRef = "android.result.vexstream-screen-open",
                    details =
                        mapOf(
                            "targetElementId" to
                                HomeScreen
                                    .VexStreamMusic
                                    .elementId,
                        ),
                )

            tap(
                HomeScreen
                    .VexStreamMusic
                    .elementId,
            )

            if (
                pairingMode ==
                PairingMode.FRESH_TEST_PAIRING_NAMESPACE
            ) {
                waitForTag(
                    VexStreamMusicScreen
                        .FindNearby
                        .elementId,
                )
                evidence.completeWalkAction(
                    action = openVexStreamAction,
                    observedResultRef = "android.result.vexstream-screen-open",
                    details =
                        mapOf(
                            "visibleElementId" to
                                VexStreamMusicScreen
                                    .FindNearby
                                    .elementId,
                        ),
                )

                val findNearbyAction =
                    evidence.beginWalkAction(
                        walkRef = WALK_REF,
                        actionRef = "android.action.find-nearby",
                        expectedResultRef = "android.result.pairing-code-visible",
                        details =
                            mapOf(
                                "targetElementId" to
                                    VexStreamMusicScreen
                                        .FindNearby
                                        .elementId,
                            ),
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
                    vexStream
                        .state
                        .value
                        .connectionState is
                        VexStreamConnectionState.ConfirmCode ||
                        vexStream
                            .state
                            .value
                            .connectionState is
                        VexStreamConnectionState.Failed
                }

                val confirmation =
                    vexStream
                        .state
                        .value
                        .connectionState
                        as? VexStreamConnectionState.ConfirmCode
                        ?: error(
                            "fresh test pairing did not reach code confirmation: " +
                                vexStream
                                    .state
                                    .value
                                    .connectionState,
                        )

                val discoveryDetails =
                    mapOf(
                        "displayName" to confirmation.displayName,
                        "deviceRef" to deviceRef,
                        "pairingCode" to confirmation.pairingCode,
                    )

                evidence.checkpoint(
                    phaseRef = "android.discovery.resolve",
                    status = "PASS",
                    details = discoveryDetails,
                )
                evidence.completeWalkAction(
                    action = findNearbyAction,
                    observedResultRef = "android.result.pairing-code-visible",
                    details = discoveryDetails,
                )

                val confirmPairingAction =
                    evidence.beginWalkAction(
                        walkRef = WALK_REF,
                        actionRef = "android.action.confirm-pairing-code",
                        expectedResultRef = "android.result.pairing-request-pending",
                        details =
                            mapOf(
                                "targetElementId" to
                                    VexStreamMusicScreen
                                        .ConfirmPairingCode
                                        .elementId,
                                "pairingCode" to confirmation.pairingCode,
                            ),
                    )

                tap(
                    VexStreamMusicScreen
                        .ConfirmPairingCode
                        .elementId,
                )

                evidence.checkpoint(
                    phaseRef = "android.pairing.requested",
                    status = "WAITING",
                    details =
                        mapOf(
                            "deviceRef" to deviceRef,
                        ),
                )
                evidence.completeWalkAction(
                    action = confirmPairingAction,
                    observedResultRef = "android.result.pairing-request-pending",
                    details =
                        mapOf(
                            "deviceRef" to deviceRef,
                        ),
                )
            } else {
                evidence.completeWalkAction(
                    action = openVexStreamAction,
                    observedResultRef = "android.result.vexstream-screen-open",
                    details =
                        mapOf(
                            "pairingMode" to pairingMode.name,
                        ),
                )
            }

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

            val ready =
                vexStream.state.value
            check(
                ready.connectionState is
                    VexStreamConnectionState.Paired &&
                    ready.hubState is
                    VexStreamHubState.Ready
            ) {
                "VexStream pairing/catalog did not become ready: " +
                    ready.connectionState +
                    " / " +
                    ready.hubState
            }

            evidence.checkpoint(
                phaseRef = "android.pairing.ready",
                status = "PASS",
                details =
                    mapOf(
                        "pairingMode" to pairingMode.name,
                        "deviceRef" to deviceRef,
                    ),
            )

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

            val streamPlayAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.play-stream",
                    expectedResultRef = "android.result.stream-playing",
                    details =
                        mapOf(
                            "targetElementId" to songsPlay,
                            "trackRef" to targetRef.value,
                        ),
                )
            tap(
                songsPlay,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "vexstream",
            )
            val streamPlaybackDetails =
                playbackDetails(
                    controller,
                )
            evidence.checkpoint(
                phaseRef = "android.playback.stream",
                status = "PASS",
                details = streamPlaybackDetails,
            )
            evidence.completeWalkAction(
                action = streamPlayAction,
                observedResultRef = "android.result.stream-playing",
                details = streamPlaybackDetails,
            )

            val addQueueAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.add-to-queue",
                    expectedResultRef = "android.result.queue-second-occurrence-visible",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
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
            val queueDetails =
                mapOf(
                    "trackRef" to targetRef.value,
                    "queueOccurrence" to 2,
                )
            evidence.checkpoint(
                phaseRef = "android.queue.fixture",
                status = "PASS",
                details = queueDetails,
            )
            evidence.completeWalkAction(
                action = addQueueAction,
                observedResultRef = "android.result.queue-second-occurrence-visible",
                details = queueDetails,
            )
            tap(
                VexStreamMusicScreen
                    .CloseQueue
                    .elementId,
            )

            val downloadAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.download-track",
                    expectedResultRef = "android.result.download-verified",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
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

            val downloadDetails =
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
                )
            evidence.checkpoint(
                phaseRef = "android.download.verified",
                status = "PASS",
                details = downloadDetails,
            )
            evidence.completeWalkAction(
                action = downloadAction,
                observedResultRef = "android.result.download-verified",
                details = downloadDetails,
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

            onMainThread {
                controller.pause()
                controller.clearMediaItems()
            }

            val offlineRefreshAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.refresh-after-desktop-stop",
                    expectedResultRef = "android.result.hub-local-only",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
                )
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

            val localOnlyDetails =
                mapOf(
                    "trackRef" to targetRef.value,
                )
            evidence.checkpoint(
                phaseRef = "android.hub.local-only",
                status = "PASS",
                details = localOnlyDetails,
            )
            evidence.completeWalkAction(
                action = offlineRefreshAction,
                observedResultRef = "android.result.hub-local-only",
                details = localOnlyDetails,
            )

            val localPlayAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.play-local",
                    expectedResultRef = "android.result.local-playing",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
                )
            tap(
                songsPlay,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "file",
            )

            val localPlaybackDetails =
                playbackDetails(
                    controller,
                )
            evidence.checkpoint(
                phaseRef = "android.playback.local",
                status = "PASS",
                details = localPlaybackDetails,
            )
            evidence.completeWalkAction(
                action = localPlayAction,
                observedResultRef = "android.result.local-playing",
                details = localPlaybackDetails,
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
        activeEvidence =
            evidence

        val pairingMode =
            requirePairingMode(
                evidence,
            )
        val expectedDigest =
            requireFixtureDigest(
                evidence,
            )

        val vexStreamContext =
            pairingContext(
                productContext = context,
                pairingMode = pairingMode,
            )
        val connectionStore =
            VexStreamConnectionStore(
                vexStreamContext,
            )
        val deviceRef =
            connectionStore.deviceRef()

        val vexStream =
            PairedVexStreamMusicRepository
                .production(
                    vexStreamContext,
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
            val restoredDownloadsAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.phase-b.action.open-restored-downloads",
                    expectedResultRef = "android.phase-b.result.download-visible",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
                )
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
            evidence.completeWalkAction(
                action = restoredDownloadsAction,
                observedResultRef = "android.phase-b.result.download-visible",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                    ),
            )

            val restoredPlayAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.phase-b.action.play-restored",
                    expectedResultRef = "android.phase-b.result.restored-playing",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
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

            val restoredPlaybackDetails =
                playbackDetails(
                    controller,
                )
            evidence.checkpoint(
                phaseRef = "android.playback.restored",
                status = "PASS",
                details = restoredPlaybackDetails,
            )
            evidence.completeWalkAction(
                action = restoredPlayAction,
                observedResultRef = "android.phase-b.result.restored-playing",
                details = restoredPlaybackDetails,
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

            val reconnectAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.phase-b.action.reconnect",
                    expectedResultRef = "android.phase-b.result.hub-and-local",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
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
            evidence.completeWalkAction(
                action = reconnectAction,
                observedResultRef = "android.phase-b.result.hub-and-local",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                        "availability" to reconnected.availability.name,
                    ),
            )

            tap(
                VexStreamMusicScreen
                    .DownloadsTab
                    .elementId,
            )
            val removeDownloadAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.phase-b.action.remove-download",
                    expectedResultRef = "android.phase-b.result.download-removed-catalog-preserved",
                    details =
                        mapOf(
                            "trackRef" to targetRef.value,
                        ),
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

            val removedDetails =
                mapOf(
                    "trackRef" to targetRef.value,
                    "catalogIdentityPreserved" to true,
                )
            evidence.checkpoint(
                phaseRef = "android.download.removed",
                status = "PASS",
                details = removedDetails,
            )
            evidence.completeWalkAction(
                action = removeDownloadAction,
                observedResultRef = "android.phase-b.result.download-removed-catalog-preserved",
                details = removedDetails,
            )

            if (
                pairingMode ==
                PairingMode.FRESH_TEST_PAIRING_NAMESPACE
            ) {
                evidence.checkpoint(
                    phaseRef = "android.awaiting-test-pairing-revoke",
                    status = "WAITING",
                    details =
                        mapOf(
                            "deviceRef" to deviceRef,
                            "hostMarkerRelativePath" to
                                evidence
                                    .hostMarkerRelativePath(
                                        HOST_MARKER_TEST_PAIRING_REVOKED,
                                    ),
                        ),
                )

                val revokeReceipt =
                    evidence.waitForHostMarker(
                        markerRef =
                            HOST_MARKER_TEST_PAIRING_REVOKED,
                        timeoutMs =
                            HOST_SYNC_TIMEOUT_MS,
                    )

                connectionStore.clear()
                File(
                    vexStreamContext.filesDir,
                    "vexstream",
                ).deleteRecursively()

                evidence.checkpoint(
                    phaseRef = "android.test-namespace.cleaned",
                    status = "PASS",
                    details =
                        mapOf(
                            "deviceRef" to deviceRef,
                            "hostReceipt" to revokeReceipt,
                        ),
                )
            }

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

    private fun requirePairingMode(
        evidence: VexCrossPlatformEvidence,
    ): PairingMode =
        when (
            evidence.argument(
                ARG_PAIRING_MODE,
            )
        ) {
            PAIRING_MODE_EXISTING ->
                PairingMode.EXISTING_PAIRING_SAFE_MODE

            PAIRING_MODE_FRESH_TEST ->
                PairingMode.FRESH_TEST_PAIRING_NAMESPACE

            else ->
                error(
                    "vexCrossPlatformPairingMode must be one of: " +
                        "$PAIRING_MODE_EXISTING, $PAIRING_MODE_FRESH_TEST",
                )
        }

    private fun pairingContext(
        productContext: Context,
        pairingMode: PairingMode,
    ): Context =
        when (pairingMode) {
            PairingMode.EXISTING_PAIRING_SAFE_MODE ->
                productContext

            PairingMode.FRESH_TEST_PAIRING_NAMESPACE ->
                StorageScopedContext(
                    productContext
                        .createDeviceProtectedStorageContext(),
                )
        }

    /**
     * Preserve the selected storage domain through production owners that correctly ask for
     * applicationContext to avoid retaining an Activity/test component context.
     */
    private class StorageScopedContext(
        base: Context,
    ) : ContextWrapper(base) {
        override fun getApplicationContext(): Context =
            this
    }

    private fun resetFreshTestNamespace(
        context: Context,
        connectionStore: VexStreamConnectionStore,
    ) {
        connectionStore.clear()
        File(
            context.filesDir,
            "vexstream",
        ).deleteRecursively()
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
            onMainThread {
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
            }

        var primaryFailure: Throwable? =
            null
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
        } catch (
            failure: Throwable,
        ) {
            primaryFailure =
                failure
            throw failure
        } finally {
            val releaseFailure =
                runCatching {
                    onMainThread {
                        MediaController.releaseFuture(
                            controllerFuture,
                        )
                    }
                }.exceptionOrNull()

            sessionScope.cancel()

            if (
                primaryFailure !=
                null
            ) {
                releaseFailure
                    ?.let(
                        primaryFailure::addSuppressed,
                    )
            } else if (
                releaseFailure !=
                null
            ) {
                throw releaseFailure
            }
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
            val observation =
                controllerObservation(
                    controller,
                )

            observation.mediaId ==
                targetRef.value &&
                observation.uriScheme ==
                expectedScheme &&
                observation.playerErrorAbsent &&
                observation.playbackState in
                setOf(
                    Player.STATE_BUFFERING,
                    Player.STATE_READY,
                    Player.STATE_ENDED,
                )
        }
    }

    private fun playbackDetails(
        controller: MediaController,
    ): Map<String, Any?> {
        val observation =
            controllerObservation(
                controller,
            )
        return mapOf(
            "mediaId" to observation.mediaId,
            "uriScheme" to observation.uriScheme,
            "playbackState" to observation.playbackState,
        )
    }

    private fun controllerObservation(
        controller: MediaController,
    ): ControllerObservation =
        onMainThread {
            val item =
                controller
                    .currentMediaItem

            ControllerObservation(
                mediaId = item?.mediaId,
                uriScheme =
                    item
                        ?.localConfiguration
                        ?.uri
                        ?.scheme,
                playerErrorAbsent =
                    controller.playerError ==
                        null,
                playbackState =
                    controller.playbackState,
            )
        }

    private fun <T> onMainThread(
        block: () -> T,
    ): T {
        if (
            Looper.myLooper() ==
            Looper.getMainLooper()
        ) {
            return block()
        }

        val value =
            AtomicReference<T?>()
        val failure =
            AtomicReference<Throwable?>()

        InstrumentationRegistry
            .getInstrumentation()
            .runOnMainSync {
                try {
                    value.set(
                        block(),
                    )
                } catch (
                    throwable: Throwable,
                ) {
                    failure.set(
                        throwable,
                    )
                }
            }

        failure.get()
            ?.let {
                throw it
            }

        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    private data class ControllerObservation(
        val mediaId: String?,
        val uriScheme: String?,
        val playerErrorAbsent: Boolean,
        val playbackState: Int,
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

    private enum class PairingMode {
        EXISTING_PAIRING_SAFE_MODE,
        FRESH_TEST_PAIRING_NAMESPACE,
    }

    companion object {
        private const val WALK_REF =
            "test.cross-platform.vexstream.v4-android.local-stream-offline/v1"

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

        private const val PAIRING_MODE_FRESH_TEST =
            "FRESH_TEST_PAIRING_NAMESPACE"

        private const val PHASE_A =
            "A"

        private const val PHASE_B =
            "B"

        private const val HOST_MARKER_DESKTOP_STOPPED =
            "desktop-stopped"

        private const val HOST_MARKER_DESKTOP_RESTARTED =
            "desktop-restarted"

        private const val HOST_MARKER_TEST_PAIRING_REVOKED =
            "test-pairing-revoked"

        private const val UI_TIMEOUT_MS =
            10_000L

        private const val DISCOVERY_TIMEOUT_MS =
            55_000L

        private const val PAIRING_APPROVAL_TIMEOUT_MS =
            90_000L

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
