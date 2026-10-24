package com.vextreme.vexlife.android.testing.crossplatform.vexstream

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryRejection
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryResult
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
 * pairing. FRESH_TEST_PAIRING_NAMESPACE requires the dedicated .crossplatform application identity,
 * so pairing, Media3 playback, offline bytes and restart restoration share one isolated app sandbox
 * without clearing ordinary VexLife pairing/download state.
 *
 * The ordinary process-restart path is proved by host-selected Phase A -> Phase B.
 * Clear Data durability uses the same isolated app identity with Phase A -> host pm clear -> Phase C.
 *
 *   Phase A
 *     fresh test pairing -> fixture catalog -> stream
 *     -> single-track local-stream/offline: queue -> verified durable download
 *     -> MassDownload: verified first download -> exact collection Download missing
 *     -> Desktop stopped -> content:// LOCAL_ONLY playback
 *
 *   HOST / process-restart scenario
 *     force-stop VexLife -> verify stopped -> invoke Phase B in a fresh instrumentation process
 *
 *   Phase B
 *     durable offline index restored -> content:// local playback
 *     -> Desktop restarted -> reconnect -> remove fixture download -> catalog identity remains
 *
 *   HOST / Clear Data scenario
 *     retain Phase-A evidence -> pm clear isolated .crossplatform package only
 *     -> verify app-private VexStream state is absent while shared media remains
 *     -> restart Desktop + fresh test pairing -> invoke Phase C
 *
 *   Phase C
 *     fresh app identity -> current catalog -> durable-copy rediscovery
 *     -> ALREADY_DOWNLOADED without media transfer
 *     -> rebuilt offline index -> Desktop stopped -> content:// local playback
 *     -> run-owned shared media cleanup
 *
 * MassDownload is a narrower lived claim. Queue remains an independently preserved sibling claim of
 * the broader local-stream/offline relationship and is not silently re-proved by MassDownload.
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
        createAndroidComposeRule<
            ComponentActivity
        >()

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
        val expectedTrackRef =
            requireFixtureTrackRef(
                evidence,
            )

        evidence.resetRunEvidence()
        evidence.checkpoint(
            phaseRef = "android.phase-a.started",
            status = "PASS",
            details =
                mapOf(
                    "pairingMode" to pairingMode.name,
                    "expectedMediaSha256" to expectedDigest,
                    "expectedTrackRef" to expectedTrackRef.value,
                ),
        )

        val massDownloadTargets =
            optionalMassDownloadTargets(
                evidence,
            )
        val massDownloadScenario =
            massDownloadTargets
                .isNotEmpty()
        val queueClaim =
            if (massDownloadScenario) {
                "NOT_PART_OF_MASS_DOWNLOAD_E2E"
            } else {
                "REQUIRED"
            }

        evidence.checkpoint(
            phaseRef = "android.scenario.scope",
            status = "PASS",
            details =
                mapOf(
                    "scenario" to
                        if (massDownloadScenario) {
                            "MASS_DOWNLOAD"
                        } else {
                            "LOCAL_STREAM_OFFLINE"
                        },
                    "queueClaim" to queueClaim,
                    "massDownloadTargetCount" to
                        massDownloadTargets.size,
                ),
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
                    "massDownloadTargetCount" to
                        massDownloadTargets.size,
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
            val homeEntryElementId =
                when (pairingMode) {
                    PairingMode.FRESH_TEST_PAIRING_NAMESPACE ->
                        HomeScreen
                            .ConnectVexStreamMusic
                            .elementId

                    PairingMode.EXISTING_PAIRING_SAFE_MODE ->
                        HomeScreen
                            .VexStreamMusic
                            .elementId
                }

            val openVexStreamAction =
                evidence.beginWalkAction(
                    walkRef = WALK_REF,
                    actionRef = "android.action.open-vexstream",
                    expectedResultRef = "android.result.vexstream-screen-open",
                    details =
                        mapOf(
                            "targetElementId" to homeEntryElementId,
                            "pairingMode" to pairingMode.name,
                        ),
                )

            tap(
                homeEntryElementId,
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

            val fixtureTarget =
                waitForFixtureCatalog(
                    vexStream = vexStream,
                    expectedTrackRef = expectedTrackRef,
                    expectedDigest = expectedDigest,
                )
            val targetRef =
                fixtureTarget.trackRef

            evidence.checkpoint(
                phaseRef = "android.catalog.fixture",
                status = "PASS",
                details = fixtureTarget.details,
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
                            "catalogAvailability" to
                                fixtureTarget
                                    .availability
                                    .name,
                        ),
                )
            tapTrackAction(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.PlayTrack,
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

            if (
                !massDownloadScenario
            ) {
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
                tapTrackAction(
                    scope =
                        VexStreamTrackPresentationScope.Songs,
                    targetRef = targetRef,
                    action =
                        VexStreamMusicScreen.TrackMenu,
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
            }

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
            tapTrackAction(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.DownloadTrack,
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

            if (
                massDownloadTargets
                    .isNotEmpty()
            ) {
                val expectedStates =
                    massDownloadTargets.map { target ->
                        requireNotNull(
                            vexStream
                                .state
                                .value
                                .track(
                                    target.trackRef,
                                ),
                        ) {
                            "MassDownload expected track is absent from the real Desktop catalog: " +
                                target.trackRef.value
                        }.also { state ->
                            check(
                                state.track.mediaSha256 ==
                                    target.mediaSha256 &&
                                    state.track.mediaByteLength ==
                                    target.mediaByteLength &&
                                    state.hubAvailable,
                            ) {
                                "MassDownload exact target identity drifted before activation: " +
                                    target.trackRef.value
                            }
                        }
                    }

                val collectionArtist =
                    (
                        expectedStates
                            .map {
                                it.track.artist
                            } +
                            requireNotNull(
                                vexStream
                                    .state
                                    .value
                                    .track(
                                        targetRef,
                                    ),
                            ).track.artist
                    )
                        .distinct()
                        .single()
                        .also { artist ->
                            check(
                                artist.isNotBlank(),
                            ) {
                                "MassDownload lived collection requires one non-blank shared artist"
                            }
                        }
                val collectionKey =
                    "artist:$collectionArtist"
                val openCollectionRef =
                    VexStreamPresentationRefs
                        .collectionRowAction(
                            collectionKey =
                                collectionKey,
                            action =
                                VexStreamMusicScreen
                                    .OpenCollection,
                        )

                tap(
                    VexStreamMusicScreen
                        .ArtistsTab
                        .elementId,
                )
                waitForTag(
                    openCollectionRef,
                )
                tap(
                    openCollectionRef,
                )
                waitForTag(
                    VexStreamPresentationRefs
                        .collectionOfflineStatus(
                            collectionKey,
                        ),
                )
                waitForTag(
                    VexStreamMusicScreen
                        .DownloadMissing
                        .elementId,
                )

                val massDownloadAction =
                    evidence.beginWalkAction(
                        walkRef =
                            WALK_REF,
                        actionRef =
                            "android.action.download-collection-missing",
                        expectedResultRef =
                            "android.result.collection-offline-complete",
                        details =
                            mapOf(
                                "collectionKey" to
                                    collectionKey,
                                "alreadyLocalTrackRef" to
                                    targetRef.value,
                                "missingTrackRefs" to
                                    massDownloadTargets
                                        .map {
                                            it.trackRef.value
                                        },
                            ),
                    )

                tap(
                    VexStreamMusicScreen
                        .DownloadMissing
                        .elementId,
                )

                compose.waitUntil(
                    timeoutMillis =
                        DOWNLOAD_TIMEOUT_MS,
                ) {
                    massDownloadTargets
                        .all { target ->
                            (
                                vexStream
                                    .state
                                    .value
                                    .track(
                                        target.trackRef,
                                    )
                                    ?.localCopy as?
                                    VexStreamLocalCopyState.Downloaded
                            )?.let { local ->
                                local.mediaSha256 ==
                                    target.mediaSha256 &&
                                    local.byteLength ==
                                    target.mediaByteLength
                            } == true
                        }
                }

                val preservedFirst =
                    vexStream
                        .state
                        .value
                        .track(
                            targetRef,
                        )
                        ?.localCopy
                        as? VexStreamLocalCopyState.Downloaded
                        ?: error(
                            "MassDownload replaced or lost the already-downloaded first track",
                        )
                assertEquals(
                    expectedDigest,
                    preservedFirst
                        .mediaSha256
                        .value,
                )

                val completionDetails =
                    mapOf(
                        "collectionKey" to
                            collectionKey,
                        "onDeviceTrackRefs" to
                            (
                                listOf(
                                    targetRef.value,
                                ) +
                                    massDownloadTargets.map {
                                        it.trackRef.value
                                    }
                            ),
                        "downloadedMissingCount" to
                            massDownloadTargets.size,
                        "queueClaim" to queueClaim,
                    )
                evidence.checkpoint(
                    phaseRef =
                        "android.mass-download.complete",
                    status =
                        "PASS",
                    details =
                        completionDetails,
                )
                evidence.completeWalkAction(
                    action =
                        massDownloadAction,
                    observedResultRef =
                        "android.result.collection-offline-complete",
                    details =
                        completionDetails,
                )

                tap(
                    VexStreamMusicScreen
                        .CollectionBack
                        .elementId,
                )
                tap(
                    VexStreamMusicScreen
                        .SongsTab
                        .elementId,
                )
            }

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
                expectedScheme = "content",
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
                        "queueClaim" to queueClaim,
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
        val expectedTrackRef =
            requireFixtureTrackRef(
                evidence,
            )

        evidence.checkpoint(
            phaseRef = "android.phase-b.started",
            status = "PASS",
            details =
                mapOf(
                    "expectedMediaSha256" to expectedDigest,
                    "expectedTrackRef" to expectedTrackRef.value,
                ),
        )
        val massDownloadTargets =
            optionalMassDownloadTargets(
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

        val restored =
            restoredOfflineFixture(
                vexStream = vexStream,
                expectedTrackRef = expectedTrackRef,
                expectedDigest = expectedDigest,
            )
        val targetRef =
            restored
                .track
                .trackRef

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
                    restored,
                ),
        )

        if (
            massDownloadTargets
                .isNotEmpty()
        ) {
            val restoredTargets =
                massDownloadTargets.map { target ->
                    requireNotNull(
                        vexStream
                            .state
                            .value
                            .track(
                                target.trackRef,
                            ),
                    ) {
                        "MassDownload restart did not restore ${target.trackRef.value}"
                    }.also { state ->
                        val local =
                            state.localCopy as?
                                VexStreamLocalCopyState.Downloaded
                                ?: error(
                                    "MassDownload restart local copy missing for ${target.trackRef.value}",
                                )
                        assertEquals(
                            target.mediaSha256,
                            local.mediaSha256,
                        )
                        assertEquals(
                            target.mediaByteLength,
                            local.byteLength,
                        )
                        assertEquals(
                            VexStreamTrackAvailability.LOCAL_ONLY,
                            state.availability,
                        )
                    }
                }

            evidence.checkpoint(
                phaseRef =
                    "android.mass-download.restart-restored",
                status =
                    "PASS",
                details =
                    mapOf(
                        "restoredTrackRefs" to
                            (
                                listOf(
                                    targetRef.value,
                                ) +
                                    restoredTargets.map {
                                        it.track.trackRef.value
                                    }
                            ),
                        "restoredCount" to
                            (
                                restoredTargets.size +
                                    1
                            ),
                    ),
            )
        }

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

            revealTrack(
                scope =
                    VexStreamTrackPresentationScope.Downloads,
                targetRef = targetRef,
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
            tapTrackAction(
                scope =
                    VexStreamTrackPresentationScope.Downloads,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.PlayTrack,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "content",
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

            if (
                massDownloadTargets
                    .isNotEmpty()
            ) {
                val reconnectedTargets =
                    massDownloadTargets.map { target ->
                        requireNotNull(
                            vexStream
                                .state
                                .value
                                .track(
                                    target.trackRef,
                                ),
                        ) {
                            "MassDownload reconnect lost ${target.trackRef.value}"
                        }.also { state ->
                            val local =
                                state.localCopy as?
                                    VexStreamLocalCopyState.Downloaded
                                    ?: error(
                                        "MassDownload reconnect local copy missing for ${target.trackRef.value}",
                                    )
                            assertEquals(
                                target.mediaSha256,
                                local.mediaSha256,
                            )
                            assertEquals(
                                target.mediaByteLength,
                                local.byteLength,
                            )
                            assertEquals(
                                VexStreamTrackAvailability.HUB_AND_LOCAL,
                                state.availability,
                            )
                        }
                    }
                evidence.checkpoint(
                    phaseRef =
                        "android.mass-download.reconnected",
                    status =
                        "PASS",
                    details =
                        mapOf(
                            "trackRefs" to
                                (
                                    listOf(
                                        targetRef.value,
                                    ) +
                                        reconnectedTargets.map {
                                            it.track.trackRef.value
                                        }
                                ),
                            "count" to
                                (
                                    reconnectedTargets.size +
                                        1
                                ),
                        ),
                )
            }

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
            tapTrackAction(
                scope =
                    VexStreamTrackPresentationScope.Downloads,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.RemoveTrackDownload,
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
            revealTrack(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                targetRef = targetRef,
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
                val vexStreamRoot =
                    File(
                        vexStreamContext.filesDir,
                        "vexstream",
                    )
                check(
                    vexStreamRoot
                        .deleteRecursively(),
                ) {
                    "failed to delete isolated VexStream test namespace"
                }
                check(
                    !vexStreamRoot.exists(),
                ) {
                    "isolated VexStream test namespace remains after cleanup"
                }

                evidence.checkpoint(
                    phaseRef = "android.test-namespace.cleaned",
                    status = "PASS",
                    details =
                        mapOf(
                            "deviceRef" to deviceRef,
                            "hostReceipt" to revokeReceipt,
                            "massDownloadTrackCount" to
                                (
                                    massDownloadTargets.size +
                                        1
                                ),
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

    @Test
    fun phaseCClearDataFreshPairRediscoverAndPlayLocalWithoutRedownload() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val context =
            instrumentation
                .targetContext
        val evidence =
            selectedEvidence(
                expectedPhase = PHASE_C,
            )
        activeEvidence =
            evidence

        val pairingMode =
            requirePairingMode(
                evidence,
            )
        check(
            pairingMode ==
                PairingMode.FRESH_TEST_PAIRING_NAMESPACE,
        ) {
            "Clear Data durability requires the isolated fresh test pairing namespace"
        }

        val expectedDigest =
            requireFixtureDigest(
                evidence,
            )
        val expectedTrackRef =
            requireFixtureTrackRef(
                evidence,
            )

        evidence.resetRunEvidence()
        evidence.checkpoint(
            phaseRef = "android.phase-c.started",
            status = "PASS",
            details =
                mapOf(
                    "expectedMediaSha256" to expectedDigest,
                    "expectedTrackRef" to expectedTrackRef.value,
                    "clearDataPackage" to context.packageName,
                ),
        )

        val deviceLibraryReadPermission =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        if (
            context.checkSelfPermission(
                deviceLibraryReadPermission,
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation
                .grantRuntimePermission(
                    context.packageName,
                    deviceLibraryReadPermission,
                )
        }
        check(
            context.checkSelfPermission(
                deviceLibraryReadPermission,
            ) ==
                PackageManager.PERMISSION_GRANTED,
        ) {
            "Clear Data Phase C requires the platform shared-audio read permission after package ownership is lost"
        }
        evidence.checkpoint(
            phaseRef =
                "android.clear-data.device-library-read-permission",
            status = "PASS",
            details =
                mapOf(
                    "permission" to
                        deviceLibraryReadPermission,
                    "apiLevel" to
                        Build.VERSION.SDK_INT,
                ),
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

        check(
            connectionStore.load() ==
                null,
        ) {
            "Clear Data Phase C must start without retained pairing"
        }
        check(
            !File(
                vexStreamContext.filesDir,
                "vexstream/offline-index.properties",
            ).exists(),
        ) {
            "Clear Data Phase C must start without the app-private offline index"
        }

        resetFreshTestNamespace(
            context = vexStreamContext,
            connectionStore = connectionStore,
        )
        val deviceRef =
            connectionStore.deviceRef()
        val vexStream =
            PairedVexStreamMusicRepository
                .production(
                    vexStreamContext,
                )

        check(
            vexStream
                .state
                .value
                .tracks
                .isEmpty(),
        ) {
            "Clear Data Phase C must begin without restored app-private track state"
        }

        evidence.checkpoint(
            phaseRef = "android.clear-data.private-state-empty",
            status = "PASS",
            details =
                mapOf(
                    "deviceRef" to deviceRef,
                    "offlineIndexPresent" to false,
                    "retainedTrackCount" to 0,
                ),
        )

        withApp(
            vexStream = vexStream,
        ) { controller ->
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
                        "Clear Data fresh pairing did not reach code confirmation: " +
                            vexStream
                                .state
                                .value
                                .connectionState,
                    )

            evidence.checkpoint(
                phaseRef = "android.clear-data.discovery.resolve",
                status = "PASS",
                details =
                    mapOf(
                        "displayName" to
                            confirmation.displayName,
                        "deviceRef" to deviceRef,
                        "pairingCode" to
                            confirmation.pairingCode,
                    ),
            )

            tap(
                VexStreamMusicScreen
                    .ConfirmPairingCode
                    .elementId,
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.pairing.requested",
                status = "WAITING",
                details =
                    mapOf(
                        "deviceRef" to deviceRef,
                    ),
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

            val ready =
                vexStream
                    .state
                    .value
            check(
                ready.connectionState is
                    VexStreamConnectionState.Paired &&
                    ready.hubState is
                    VexStreamHubState.Ready
            ) {
                "Clear Data fresh pairing/catalog did not become ready: " +
                    ready.connectionState +
                    " / " +
                    ready.hubState
            }

            val recovered =
                waitForFixtureCatalog(
                    vexStream = vexStream,
                    expectedTrackRef = expectedTrackRef,
                    expectedDigest = expectedDigest,
                )
            val targetRef =
                recovered.trackRef
            val recoveredState =
                requireNotNull(
                    vexStream
                        .state
                        .value
                        .track(
                            targetRef,
                        ),
                )
            val localCopy =
                recoveredState.localCopy as?
                    VexStreamLocalCopyState.Downloaded
                    ?: error(
                        "Clear Data catalog recovery did not rediscover the durable shared copy",
                    )

            assertEquals(
                VexStreamTrackAvailability.HUB_AND_LOCAL,
                recoveredState.availability,
            )
            assertTrue(
                localCopy
                    .localMediaRef
                    .startsWith(
                        "content://",
                    ),
            )
            assertEquals(
                expectedDigest,
                localCopy
                    .mediaSha256
                    .value,
            )
            assertEquals(
                recoveredState
                    .track
                    .mediaByteLength,
                localCopy.byteLength,
            )
            assertTrue(
                File(
                    vexStreamContext.filesDir,
                    "vexstream/offline-index.properties",
                ).isFile,
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.rediscovered",
                status = "PASS",
                details =
                    fixtureDetails(
                        recoveredState,
                    ) +
                        mapOf(
                            "localMediaRef" to
                                localCopy.localMediaRef,
                            "offlineIndexRebuilt" to true,
                            "deviceRef" to deviceRef,
                        ),
            )

            val repeatDownload =
                runBlocking {
                    vexStream
                        .downloadForOffline(
                            trackRef = targetRef,
                            expectedMediaSha256 =
                                recoveredState
                                    .track
                                    .mediaSha256,
                            expectedMediaByteLength =
                                recoveredState
                                    .track
                                    .mediaByteLength,
                        )
                }
            val rejected =
                repeatDownload as?
                    VexStreamRepositoryResult.Rejected
                    ?: error(
                        "rediscovered durable copy unexpectedly admitted another download",
                    )
            assertEquals(
                VexStreamRepositoryRejection
                    .ALREADY_DOWNLOADED,
                rejected.reason,
            )
            assertEquals(
                targetRef,
                rejected.trackRef,
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.no-redownload",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                        "reason" to rejected.reason.name,
                        "localMediaRef" to
                            localCopy.localMediaRef,
                    ),
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.awaiting-desktop-stop",
                status = "WAITING",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                        "hostMarkerRelativePath" to
                            evidence
                                .hostMarkerRelativePath(
                                    HOST_MARKER_CLEAR_DATA_DESKTOP_STOPPED,
                                ),
                    ),
            )

            val hostStopReceipt =
                evidence.waitForHostMarker(
                    markerRef =
                        HOST_MARKER_CLEAR_DATA_DESKTOP_STOPPED,
                    timeoutMs =
                        HOST_SYNC_TIMEOUT_MS,
                )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.desktop-stop-observed",
                status = "PASS",
                details =
                    mapOf(
                        "hostReceipt" to
                            hostStopReceipt,
                    ),
            )

            val refreshResult =
                runBlocking {
                    vexStream.refreshCatalog()
                }
            val refreshRejected =
                refreshResult as?
                    VexStreamRepositoryResult.Rejected
                    ?: error(
                        "Clear Data local-only proof expected the stopped Desktop refresh to fail closed",
                    )
            assertEquals(
                VexStreamRepositoryRejection
                    .HUB_NOT_READY,
                refreshRejected.reason,
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
            val localOnlyCopy =
                localOnly.localCopy as?
                    VexStreamLocalCopyState.Downloaded
                    ?: error(
                        "Clear Data local-only state lost the rediscovered shared copy",
                    )
            assertEquals(
                VexStreamTrackAvailability.LOCAL_ONLY,
                localOnly.availability,
            )
            assertEquals(
                localCopy.localMediaRef,
                localOnlyCopy.localMediaRef,
            )

            tapTrackAction(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.PlayTrack,
            )
            waitForPlayback(
                controller = controller,
                targetRef = targetRef,
                expectedScheme = "content",
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.playback.local",
                status = "PASS",
                details =
                    playbackDetails(
                        controller,
                    ) +
                        mapOf(
                            "localMediaRef" to
                                localOnlyCopy.localMediaRef,
                        ),
            )

            onMainThread {
                controller.pause()
                controller.clearMediaItems()
            }

            val removed =
                runBlocking {
                    vexStream
                        .removeOfflineCopy(
                            targetRef,
                        )
                }
            val accepted =
                removed as?
                    VexStreamRepositoryResult.Accepted
                    ?: error(
                        "Clear Data proof could not remove its run-owned shared media",
                    )
            assertEquals(
                targetRef,
                accepted.trackRef,
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.shared-test-media.removed",
                status = "PASS",
                details =
                    mapOf(
                        "trackRef" to targetRef.value,
                    ),
            )

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.awaiting-test-pairing-revoke",
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
            val vexStreamRoot =
                File(
                    vexStreamContext.filesDir,
                    "vexstream",
                )
            check(
                vexStreamRoot
                    .deleteRecursively(),
            ) {
                "failed to delete Clear Data isolated VexStream test namespace"
            }

            evidence.checkpoint(
                phaseRef =
                    "android.clear-data.test-namespace.cleaned",
                status = "PASS",
                details =
                    mapOf(
                        "deviceRef" to deviceRef,
                        "hostReceipt" to revokeReceipt,
                    ),
            )
            evidence.checkpoint(
                phaseRef = "android.phase-c.complete",
                status = "PASS",
                details =
                    mapOf(
                        "clearDataSurvived" to true,
                        "rediscoveredWithoutRedownload" to true,
                        "localPlaybackScheme" to "content",
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

            PairingMode.FRESH_TEST_PAIRING_NAMESPACE -> {
                check(
                    productContext.packageName ==
                        CROSS_PLATFORM_APPLICATION_ID,
                ) {
                    "FRESH_TEST_PAIRING_NAMESPACE requires the isolated cross-platform application identity"
                }
                productContext
            }
        }

    private fun resetFreshTestNamespace(
        context: Context,
        connectionStore: VexStreamConnectionStore,
    ) {
        check(
            context.packageName ==
                CROSS_PLATFORM_APPLICATION_ID,
        ) {
            "fresh test namespace cleanup requires the isolated cross-platform application identity"
        }
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

    private fun requireFixtureTrackRef(
        evidence: VexCrossPlatformEvidence,
    ): VexStreamTrackRef =
        VexStreamTrackRef(
            requireNotNull(
                evidence.argument(
                    ARG_EXPECTED_TRACK_REF,
                ),
            ) {
                "$ARG_EXPECTED_TRACK_REF is required"
            }.trim(),
        )

    private fun waitForFixtureCatalog(
        vexStream: PairedVexStreamMusicRepository,
        expectedTrackRef: VexStreamTrackRef,
        expectedDigest: String,
    ): FixtureCatalogTarget {
        var observedTarget:
            FixtureCatalogTarget? =
            null

        compose.waitUntil(
            timeoutMillis =
                CATALOG_FIXTURE_TIMEOUT_MS,
        ) {
            val state =
                vexStream
                    .state
                    .value

            if (
                state.connectionState is
                VexStreamConnectionState.Failed
            ) {
                return@waitUntil true
            }

            val item =
                state
                    .track(
                        expectedTrackRef,
                    )
                    ?: return@waitUntil false

            check(
                item
                    .track
                    .mediaSha256
                    .value ==
                    expectedDigest,
            ) {
                "exact VexStream fixture trackRef was rebound to unexpected media bytes"
            }

            observedTarget =
                FixtureCatalogTarget(
                    trackRef = expectedTrackRef,
                    availability = item.availability,
                    details =
                        mapOf(
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
                            "title" to
                                item
                                    .track
                                    .title,
                            "artist" to
                                item
                                    .track
                                    .artist,
                        ),
                )

            true
        }

        val state =
            vexStream
                .state
                .value
        check(
            state.connectionState !is
                VexStreamConnectionState.Failed,
        ) {
            "VexStream failed while waiting for exact fixture trackRef: " +
                state.connectionState
        }

        return requireNotNull(
            observedTarget,
        ) {
            "exact VexStream fixture trackRef did not become available in Android catalog state"
        }
    }

    private data class MassDownloadTargetExpectation(
        val trackRef: VexStreamTrackRef,
        val mediaSha256: VexStreamMediaSha256,
        val mediaByteLength: Long,
    )

    private fun optionalMassDownloadTargets(
        evidence: VexCrossPlatformEvidence,
    ): List<MassDownloadTargetExpectation> {
        val encoded =
            evidence.argument(
                ARG_MASS_DOWNLOAD_TARGETS,
            )
                ?: return emptyList()

        val targets =
            encoded
                .split(
                    ",",
                )
                .filter(
                    String::isNotBlank,
                )
                .mapIndexed {
                        index,
                        token,
                    ->
                    val fields =
                        token.split(
                            ":",
                        )
                    check(
                        fields.size == 3,
                    ) {
                        "MassDownload target $index must be trackRef:sha256:byteLength"
                    }
                    val byteLength =
                        fields[2]
                            .toLongOrNull()
                            ?: error(
                                "MassDownload target $index byte length is invalid",
                            )

                    MassDownloadTargetExpectation(
                        trackRef =
                            VexStreamTrackRef(
                                fields[0],
                            ),
                        mediaSha256 =
                            VexStreamMediaSha256(
                                fields[1],
                            ),
                        mediaByteLength =
                            byteLength,
                    )
                }

        check(
            targets
                .map {
                    it.trackRef
                }
                .distinct()
                .size ==
                targets.size,
        ) {
            "MassDownload lived target refs must be unique"
        }
        check(
            targets.all {
                it.mediaByteLength > 0L
            },
        ) {
            "MassDownload lived target byte lengths must be positive"
        }

        return targets
    }

    private data class FixtureCatalogTarget(
        val trackRef: VexStreamTrackRef,
        val availability: VexStreamTrackAvailability,
        val details: Map<String, Any?>,
    )

    private fun restoredOfflineFixture(
        vexStream: PairedVexStreamMusicRepository,
        expectedTrackRef: VexStreamTrackRef,
        expectedDigest: String,
    ) =
        requireNotNull(
            vexStream
                .state
                .value
                .track(
                    expectedTrackRef,
                ),
        ) {
            "exact VexStream fixture trackRef was not restored from the offline index"
        }.also { item ->
            check(
                item
                    .track
                    .mediaSha256
                    .value ==
                    expectedDigest,
            ) {
                "restored VexStream fixture trackRef was rebound to unexpected media bytes"
            }
        }

    private fun fixtureDetails(
        item: com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState,
    ): Map<String, Any?> =
        mapOf(
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
                (
                    observation.playbackState ==
                        Player.STATE_READY ||
                        (
                            observation.playbackState ==
                                Player.STATE_ENDED &&
                                observation.currentPositionMs > 0L
                        )
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
            "currentPositionMs" to observation.currentPositionMs,
            "isPlaying" to observation.isPlaying,
            "mediaTitle" to observation.mediaTitle,
            "mediaArtist" to observation.mediaArtist,
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
                currentPositionMs =
                    controller.currentPosition,
                isPlaying =
                    controller.isPlaying,
                mediaTitle =
                    item
                        ?.mediaMetadata
                        ?.title
                        ?.toString(),
                mediaArtist =
                    item
                        ?.mediaMetadata
                        ?.artist
                        ?.toString(),
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
        val currentPositionMs: Long,
        val isPlaying: Boolean,
        val mediaTitle: String?,
        val mediaArtist: String?,
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

    /**
     * LazyColumn scroll identity is the row's root Play semantic ref.
     *
     * Nested row/menu/download refs remain exact action identities, but the production Card owns the
     * Play ref at the lazy-item root. Scroll by that exact trackRef-root first, then resolve the exact
     * nested action. This keeps collection size / viewport placement out of target selection.
     */
    private fun revealTrack(
        scope: VexStreamTrackPresentationScope,
        targetRef: VexStreamTrackRef,
    ) {
        val rootPlayRef =
            trackRowAction(
                scope = scope,
                targetRef = targetRef,
                action =
                    VexStreamMusicScreen.PlayTrack,
            )

        compose
            .onNode(
                hasScrollToIndexAction(),
            )
            .performScrollToNode(
                hasTestTag(
                    rootPlayRef,
                ),
            )

        waitForTag(
            rootPlayRef,
        )
    }

    private fun tapTrackAction(
        scope: VexStreamTrackPresentationScope,
        targetRef: VexStreamTrackRef,
        action:
            com.vextreme.vexlife.android.elements.ActionElement,
    ) {
        revealTrack(
            scope = scope,
            targetRef = targetRef,
        )

        val elementId =
            trackRowAction(
                scope = scope,
                targetRef = targetRef,
                action = action,
            )

        val useUnmergedTree =
            waitForTag(
                elementId,
            )
        compose
            .onNodeWithTag(
                elementId,
                useUnmergedTree =
                    useUnmergedTree,
            )
            .performClick()

        compose.waitForIdle()
    }

    private fun tap(
        elementId: String,
    ) {
        val useUnmergedTree =
            waitForTag(
                elementId,
            )

        runCatching {
            compose
                .onNodeWithTag(
                    elementId,
                    useUnmergedTree =
                        useUnmergedTree,
                )
                .performScrollTo()
        }

        compose
            .onNodeWithTag(
                elementId,
                useUnmergedTree =
                    useUnmergedTree,
            )
            .performClick()

        compose.waitForIdle()
    }

    /**
     * Returns whether the exact Vex semantic element was found only in the unmerged tree.
     *
     * Popup/material boundaries may expose an exact testTag below a merged semantics owner. That is
     * still the same Vex-owned semantic address, not a text/position fallback. Product action is
     * invoked once through whichever exact tree actually owns the tag.
     */
    private fun waitForTag(
        elementId: String,
    ): Boolean {
        var useUnmergedTree =
            false

        val failure =
            runCatching {
                compose.waitUntil(
                    timeoutMillis =
                        UI_TIMEOUT_MS,
                ) {
                    val mergedPresent =
                        compose
                            .onAllNodesWithTag(
                                elementId,
                            )
                            .fetchSemanticsNodes()
                            .isNotEmpty()

                    if (mergedPresent) {
                        useUnmergedTree =
                            false
                        true
                    } else {
                        val unmergedPresent =
                            compose
                                .onAllNodesWithTag(
                                    elementId,
                                    useUnmergedTree = true,
                                )
                                .fetchSemanticsNodes()
                                .isNotEmpty()

                        if (unmergedPresent) {
                            useUnmergedTree =
                                true
                        }
                        unmergedPresent
                    }
                }
            }.exceptionOrNull()
                ?: return useUnmergedTree

        val mergedCount =
            runCatching {
                compose
                    .onAllNodesWithTag(
                        elementId,
                    )
                    .fetchSemanticsNodes()
                    .size
            }.getOrDefault(
                0,
            )
        val unmergedCount =
            runCatching {
                compose
                    .onAllNodesWithTag(
                        elementId,
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .size
            }.getOrDefault(
                0,
            )

        val hostState =
            runCatching {
                val activity =
                    compose.activity
                "lifecycle=" +
                    activity.lifecycle.currentState +
                    " finishing=" +
                    activity.isFinishing +
                    " destroyed=" +
                    activity.isDestroyed +
                    " windowFocus=" +
                    activity.hasWindowFocus()
            }.getOrElse { activityFailure ->
                "[activity state unavailable: " +
                    "${activityFailure::class.simpleName}: " +
                    "${activityFailure.message}]"
            }

        activeEvidence
            ?.screenshot(
                phaseRef =
                    "android.semantic-wait-failure",
                details =
                    mapOf(
                        "elementId" to elementId,
                        "mergedCount" to mergedCount,
                        "unmergedCount" to
                            unmergedCount,
                        "hostState" to hostState,
                    ),
            )

        val mergedTree =
            runCatching {
                compose
                    .onRoot()
                    .printToString(
                        maxDepth = 12,
                    )
            }.getOrElse { treeFailure ->
                "[merged semantics unavailable: " +
                    "${treeFailure::class.simpleName}: " +
                    "${treeFailure.message}]"
            }
        val unmergedTree =
            runCatching {
                compose
                    .onRoot(
                        useUnmergedTree = true,
                    )
                    .printToString(
                        maxDepth = 12,
                    )
            }.getOrElse { treeFailure ->
                "[unmerged semantics unavailable: " +
                    "${treeFailure::class.simpleName}: " +
                    "${treeFailure.message}]"
            }

        val compactFailure =
            "VEX_SEMANTIC_WAIT_FAILURE " +
                "elementId=$elementId " +
                "mergedCount=$mergedCount " +
                "unmergedCount=$unmergedCount " +
                "host=$hostState"

        throw AssertionError(
            buildString {
                appendLine(
                    "Timed out waiting for Vex semantic element: $elementId",
                )
                appendLine(
                    "Compose host activity:",
                )
                appendLine(
                    hostState,
                )
                appendLine(
                    "Merged Compose semantics:",
                )
                appendLine(
                    mergedTree,
                )
                appendLine(
                    "Unmerged Compose semantics:",
                )
                appendLine(
                    unmergedTree,
                )
                appendLine(
                    compactFailure,
                )
            }.trimEnd(),
            failure,
        )
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

        private const val CROSS_PLATFORM_APPLICATION_ID =
            "com.vextreme.vexlife.android.crossplatform"

        private const val ARG_EXPECTED_MEDIA_SHA256 =
            "vexCrossPlatformExpectedMediaSha256"

        private const val ARG_EXPECTED_TRACK_REF =
            "vexCrossPlatformExpectedTrackRef"

        private const val ARG_MASS_DOWNLOAD_TARGETS =
            "vexCrossPlatformMassDownloadTargets"

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

        private const val PHASE_C =
            "C"

        private const val HOST_MARKER_DESKTOP_STOPPED =
            "desktop-stopped"

        private const val HOST_MARKER_DESKTOP_RESTARTED =
            "desktop-restarted"

        private const val HOST_MARKER_CLEAR_DATA_DESKTOP_STOPPED =
            "clear-data-desktop-stopped"

        private const val HOST_MARKER_TEST_PAIRING_REVOKED =
            "test-pairing-revoked"

        private const val UI_TIMEOUT_MS =
            10_000L

        private const val DISCOVERY_TIMEOUT_MS =
            55_000L

        private const val PAIRING_APPROVAL_TIMEOUT_MS =
            90_000L

        private const val CATALOG_FIXTURE_TIMEOUT_MS =
            60_000L

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
