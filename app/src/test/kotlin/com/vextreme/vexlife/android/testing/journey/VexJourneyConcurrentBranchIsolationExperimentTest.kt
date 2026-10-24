package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStoreResult
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamOfflineIndex
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryResult
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexCoroutineHandle
import com.vextreme.vexlife.android.execution.VexCoroutineState
import com.vextreme.vexlife.android.execution.VexCoroutineStatus
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Second bounded QuantumTest/Vroomba experiment.
 *
 * The merged route-model POC already proves canonical checkpoint identity, restored-suffix
 * equivalence, and child-specific route failure evidence. This test pressures only the next
 * question:
 * can one exact VexStream semantic/resource snapshot seed three independently-owned Android/JVM
 * branch worlds whose managed executions are concurrently admitted without weakening VexExecution
 * identity or sharing mutable storage?
 *
 * This remains test-local. It does not snapshot Compose, Media3, Binder, Context, coroutine Jobs,
 * sockets, or a real Android process.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VexJourneyConcurrentBranchIsolationExperimentTest {
    @Test
    fun oneSnapshotRunsThreeIsolatedBranchFuturesOnOneDeterministicScheduler() {
        val snapshot = formVerifiedDownloadSnapshot()
        val scheduler = TestCoroutineScheduler()
        val dispatcher = StandardTestDispatcher(scheduler)
        val gate = CompletableDeferred<Unit>()
        val branches =
            listOf(BRANCH_A, BRANCH_B, BRANCH_C)
                .associateWith { branchRef ->
                    snapshot.restoreBranch(
                        branchRef = branchRef,
                        dispatcher = dispatcher,
                    )
                }

        val branchA = branches.getValue(BRANCH_A)
        val branchB = branches.getValue(BRANCH_B)
        val branchC = branches.getValue(BRANCH_C)

        try {
            assertIndependentEqualStarts(snapshot, branches.values.toList())

            TestScope(dispatcher).runTest {
                val handles =
                    mapOf(
                        BRANCH_A to
                            BranchExecution(branchA.executionRoot, gate) {
                                branchA.completedPrefix += "remove-local-copy"
                                check(branchA.mediaStore.remove(branchA.downloadedCopy()))
                                checkAccepted(
                                    branchA.repository.removeOfflineCopy(TRACK_REF),
                                )
                                check(
                                    branchA.trackAvailability() ==
                                        VexStreamTrackAvailability.HUB_ONLY,
                                )
                                branchA.completedPrefix += "HUB_ONLY"
                            }.start(),
                        BRANCH_B to
                            BranchExecution(branchB.executionRoot, gate) {
                                branchB.completedPrefix += "hub-unavailable"
                                branchB.repository.publishHubUnavailable(
                                    VexStreamHubState.Unreachable,
                                )
                                check(
                                    branchB.trackAvailability() ==
                                        VexStreamTrackAvailability.LOCAL_ONLY,
                                )
                                check(
                                    branchB.mediaStore.verify(
                                        branchB.downloadedCopy(),
                                    ),
                                )
                                branchB.completedPrefix += "LOCAL_ONLY"
                                error(BRANCH_B_FAILURE)
                            }.start(),
                        BRANCH_C to
                            BranchExecution(branchC.executionRoot, gate) {
                                val priorRepository = branchC.repository
                                val restoredOffline = branchC.offlineIndex.load()
                                check(
                                    restoredOffline.track(TRACK_REF)?.availability ==
                                        VexStreamTrackAvailability.LOCAL_ONLY,
                                )
                                branchC.completedPrefix +=
                                    "fresh-offline-index:LOCAL_ONLY"

                                branchC.repository =
                                    InMemoryVexStreamMusicRepository(
                                        restoredOffline.copy(
                                            connectionState =
                                                snapshot.state.connectionState,
                                        ),
                                    )
                                assertNotSame(priorRepository, branchC.repository)

                                checkAccepted(
                                    branchC.repository.reconnectPairedVexStream(),
                                )
                                check(
                                    branchC.trackAvailability() ==
                                        VexStreamTrackAvailability.HUB_AND_LOCAL,
                                )
                                check(
                                    branchC.mediaStore.verify(
                                        branchC.downloadedCopy(),
                                    ),
                                )
                                branchC.completedPrefix +=
                                    "reconnect:HUB_AND_LOCAL"
                            }.start(),
                    )

                runCurrent()

                // The same source-derived execution ref can exist simultaneously because each
                // branch owns a separate execution root. branchRef + VexCoroutineRef is unique.
                assertEquals(
                    1,
                    handles.values.map { handle -> handle.ref }.distinct().size,
                )
                assertEquals(
                    3,
                    handles.map { (branchRef, handle) ->
                        "$branchRef:${handle.ref.value}"
                    }.distinct().size,
                )
                branches.forEach { (branchRef, world) ->
                    val handle = handles.getValue(branchRef)
                    assertEquals(
                        VexCoroutineStatus.RUNNING,
                        handle.state.value.status,
                    )
                    assertEquals(
                        VexCoroutineStatus.RUNNING,
                        world.executionRoot.executions.value
                            .getValue(handle.ref)
                            .status,
                    )
                }

                gate.complete(Unit)
                runCurrent()
                handles.values.forEach { handle -> handle.awaitCompletion() }

                val traces =
                    listOf(
                        branchA.trace(snapshot.snapshotRef, ROUTE_A, handles.getValue(BRANCH_A)),
                        branchB.trace(snapshot.snapshotRef, ROUTE_B, handles.getValue(BRANCH_B)),
                        branchC.trace(snapshot.snapshotRef, ROUTE_C, handles.getValue(BRANCH_C)),
                    )

                assertEquals(
                    listOf(
                        VexCoroutineStatus.COMPLETED,
                        VexCoroutineStatus.FAILED,
                        VexCoroutineStatus.COMPLETED,
                    ),
                    traces.map { trace -> trace.execution.status },
                )
                assertEquals(
                    BRANCH_B_FAILURE,
                    traces.single { trace -> trace.branchRef == BRANCH_B }
                        .execution.failureMessage,
                )
                assertEquals(
                    listOf("hub-unavailable", "LOCAL_ONLY"),
                    traces.single { trace -> trace.branchRef == BRANCH_B }
                        .completedPrefix,
                )
                assertEquals(
                    3,
                    traces.map { trace ->
                        Triple(trace.snapshotRef, trace.branchRef, trace.routeRef)
                    }.distinct().size,
                )

                assertEquals(
                    VexStreamTrackAvailability.HUB_ONLY,
                    branchA.trackAvailability(),
                )
                assertEquals(
                    VexStreamTrackAvailability.LOCAL_ONLY,
                    branchB.trackAvailability(),
                )
                assertEquals(
                    VexStreamTrackAvailability.HUB_AND_LOCAL,
                    branchC.trackAvailability(),
                )

                // A mutated only its own state and storage. B/C retain valid local bytes, and the
                // captured snapshot remains unchanged.
                assertFalse(
                    branchA.mediaStore.verify(snapshot.state.downloadedCopy()),
                )
                assertTrue(branchB.mediaStore.verify(branchB.downloadedCopy()))
                assertTrue(branchC.mediaStore.verify(branchC.downloadedCopy()))
                assertEquals(
                    VexStreamTrackAvailability.HUB_AND_LOCAL,
                    snapshot.state.track(TRACK_REF)?.availability,
                )
                snapshot.resources.forEach { resource ->
                    assertEquals(resource.sha256, resource.bytes.toByteArrayExact().sha256())
                }
            }
        } finally {
            branches.values.forEach { world ->
                world.executionRoot.forceStopAll(
                    "concurrent branch experiment complete",
                )
            }
            // A TestCoroutineScheduler must be allowed to deliver cancellation before a blocking
            // join. This keeps cleanup truthful even when an assertion fails before the branch
            // gate is released.
            scheduler.advanceUntilIdle()
            runBlocking {
                branches.values.forEach { world ->
                    world.executionRoot.joinStopAll(
                        "concurrent branch experiment complete",
                    )
                }
            }
            branches.values.forEach(BranchWorld::deleteStorage)
        }

        branches.values.forEach { world ->
            assertFalse(world.storageRoot.exists())
            assertTrue(
                world.executionRoot.executions.value.values.all { state ->
                    state.status == VexCoroutineStatus.COMPLETED ||
                        state.status == VexCoroutineStatus.CANCELLED ||
                        state.status == VexCoroutineStatus.FAILED
                },
            )
        }
    }

    private fun assertIndependentEqualStarts(
        snapshot: VexStreamSnapshot,
        branches: List<BranchWorld>,
    ) {
        branches.forEach { branch ->
            assertEquals(snapshot.state, branch.repository.state.value)
            assertNotSame(snapshot.state, branch.repository.state.value)
            assertEquals(
                VexStreamTrackAvailability.HUB_AND_LOCAL,
                branch.trackAvailability(),
            )
            assertTrue(branch.mediaStore.verify(branch.downloadedCopy()))
        }

        branches.forEachIndexed { index, branch ->
            branches.drop(index + 1).forEach { sibling ->
                assertNotSame(branch.repository.state.value, sibling.repository.state.value)
                assertTrue(branch.storageRoot != sibling.storageRoot)
                assertNotSame(branch.executionRoot, sibling.executionRoot)
            }
        }
    }

    private fun formVerifiedDownloadSnapshot(): VexStreamSnapshot {
        val seedRoot = Files.createTempDirectory("vexstream-quantum-seed-").toFile()

        try {
            val mediaStore = VexStreamMediaStore.production(seedRoot)
            val offlineIndex =
                VexStreamOfflineIndex.production(
                    contextFilesDir = seedRoot,
                    mediaStore = mediaStore,
                )
            val mediaBytes = FIXTURE_MEDIA.toByteArray()
            val mediaDigest = VexStreamMediaSha256(mediaBytes.sha256())
            val stored =
                mediaStore.store(
                    expectedSha256 = mediaDigest,
                    expectedByteLength = mediaBytes.size.toLong(),
                    input = mediaBytes.inputStream(),
                ) as VexStreamMediaStoreResult.Stored
            val state =
                VexStreamMusicState(
                    hubState =
                        VexStreamHubState.Ready(
                            hubRef = "hub.concurrent.fixture",
                            catalogRevision = "catalog.concurrent.fixture.1",
                        ),
                    connectionState =
                        VexStreamConnectionState.Paired(
                            displayName = "VexStream Concurrent Fixture",
                        ),
                    tracks =
                        listOf(
                            VexStreamTrackState(
                                track =
                                    VexStreamTrack(
                                        trackRef = TRACK_REF,
                                        mediaSha256 = mediaDigest,
                                        mediaByteLength = mediaBytes.size.toLong(),
                                        title = "Concurrent Fixture",
                                        artist = "VexStream QA",
                                    ),
                                hubAvailable = true,
                                localCopy = stored.localCopy,
                            ),
                        ),
                )

            check(offlineIndex.persist(state))

            val mediaPath = "vexstream/media/${mediaDigest.value}.media"
            val indexPath = "vexstream/offline-index.properties"
            val resources =
                listOf(
                    seedRoot.resolve(mediaPath).asSnapshotResource(mediaPath),
                    seedRoot.resolve(indexPath).asSnapshotResource(indexPath),
                )
            val copiedState = state.branchCopy()
            return VexStreamSnapshot(
                snapshotRef = copiedState.snapshotRef(resources),
                state = copiedState,
                resources = resources,
                provenanceRouteRef = "pair/catalog/download-verified",
                semanticCheckpoint = "download-verified",
            )
        } finally {
            seedRoot.deleteRecursively()
        }
    }

    private fun VexStreamSnapshot.restoreBranch(
        branchRef: String,
        dispatcher: CoroutineDispatcher,
    ): BranchWorld {
        val root =
            Files.createTempDirectory(
                "vexstream-quantum-${branchRef.lowercase()}-",
            ).toFile()

        resources.forEach { resource -> resource.restoreInto(root) }

        val mediaStore = VexStreamMediaStore.production(root)
        val offlineIndex =
            VexStreamOfflineIndex.production(
                contextFilesDir = root,
                mediaStore = mediaStore,
            )
        val copiedState = state.branchCopy()
        check(mediaStore.verify(copiedState.downloadedCopy()))
        check(
            offlineIndex.load().track(TRACK_REF)?.availability ==
                VexStreamTrackAvailability.LOCAL_ONLY,
        )

        return BranchWorld(
            branchRef = branchRef,
            storageRoot = root,
            mediaStore = mediaStore,
            offlineIndex = offlineIndex,
            repository = InMemoryVexStreamMusicRepository(copiedState),
            executionRoot =
                VexExecutionRoot(
                    dispatcher = dispatcher,
                    maxCollectRestarts = 0,
                    collectRestartDelayMillis = 0L,
                ),
        )
    }

    private fun VexStreamMusicState.branchCopy(): VexStreamMusicState =
        copy(
            tracks =
                tracks.map { item ->
                    item.copy(
                        track = item.track.copy(),
                        localCopy =
                            when (val local = item.localCopy) {
                                is VexStreamLocalCopyState.Downloading -> local.copy()
                                is VexStreamLocalCopyState.Downloaded -> local.copy()
                                is VexStreamLocalCopyState.Failed -> local.copy()
                                VexStreamLocalCopyState.NotDownloaded -> local
                            },
                    )
                },
            playlists =
                playlists.map { playlist ->
                    playlist.copy(trackRefs = playlist.trackRefs.toList())
                },
        )

    private fun VexStreamMusicState.downloadedCopy(): VexStreamLocalCopyState.Downloaded =
        requireNotNull(
            track(TRACK_REF)?.localCopy as? VexStreamLocalCopyState.Downloaded,
        )

    private fun VexStreamMusicState.snapshotRef(
        resources: List<SnapshotResource>,
    ): String {
        val hub =
            requireNotNull(
                hubState as? VexStreamHubState.Ready,
            )
        val connection =
            requireNotNull(
                connectionState as? VexStreamConnectionState.Paired,
            )
        val canonical =
            buildString {
                fun field(
                    name: String,
                    value: Any?,
                ) {
                    val text = value?.toString() ?: "<null>"
                    append(name)
                        .append(':')
                        .append(text.length)
                        .append(':')
                        .append(text)
                        .append('\n')
                }

                field("schema", "vexstream.concurrent-snapshot/v1")
                field("hubRef", hub.hubRef)
                field("catalogRevision", hub.catalogRevision)
                field("pairedDisplayName", connection.displayName)

                tracks
                    .sortedBy { item -> item.track.trackRef.value }
                    .forEach { item ->
                        val local =
                            requireNotNull(
                                item.localCopy as? VexStreamLocalCopyState.Downloaded,
                            )
                        field("trackRef", item.track.trackRef.value)
                        field("mediaSha256", item.track.mediaSha256.value)
                        field("mediaByteLength", item.track.mediaByteLength)
                        field("title", item.track.title)
                        field("artist", item.track.artist)
                        field("album", item.track.album)
                        field("genre", item.track.genre)
                        field("year", item.track.year)
                        field("durationSeconds", item.track.durationSeconds)
                        field("playbackStartSeconds", item.track.playbackStartSeconds)
                        field("playbackEndSeconds", item.track.playbackEndSeconds)
                        field("hubAvailable", item.hubAvailable)
                        field("localMediaRef", local.localMediaRef)
                        field("localSha256", local.mediaSha256.value)
                        field("localByteLength", local.byteLength)
                    }

                playlists
                    .sortedBy { playlist -> playlist.playlistRef.value }
                    .forEach { playlist ->
                        field("playlistRef", playlist.playlistRef.value)
                        field("playlistName", playlist.name)
                        playlist.trackRefs.forEach { trackRef ->
                            field("playlistTrackRef", trackRef.value)
                        }
                    }

                resources
                    .sortedBy(SnapshotResource::relativePath)
                    .forEach { resource ->
                        field("resourcePath", resource.relativePath)
                        field("resourceSha256", resource.sha256)
                    }
            }

        return "snapshot:vexstream:" +
            canonical.toByteArray().sha256()
    }

    private fun File.asSnapshotResource(relativePath: String): SnapshotResource {
        val bytes = readBytes()
        return SnapshotResource(
            relativePath = relativePath,
            sha256 = bytes.sha256(),
            bytes = bytes.toList(),
        )
    }

    private fun SnapshotResource.restoreInto(root: File) {
        require(!relativePath.startsWith("/"))
        require(".." !in relativePath.split('/'))

        val exactBytes = bytes.toByteArrayExact()
        check(exactBytes.sha256() == sha256)

        val target = root.resolve(relativePath)
        target.parentFile?.mkdirs()
        target.writeBytes(exactBytes)
        check(target.readBytes().sha256() == sha256)
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { byte ->
                "%02x".format(byte)
            }

    private fun List<Byte>.toByteArrayExact(): ByteArray =
        ByteArray(size) { index -> get(index) }

    private fun checkAccepted(result: VexStreamRepositoryResult) {
        check(result is VexStreamRepositoryResult.Accepted)
    }

    private class BranchExecution(
        root: VexExecutionRoot,
        private val gate: CompletableDeferred<Unit>,
        private val action: suspend () -> Unit,
    ) : VexExecution(root) {
        fun start(): VexCoroutineHandle = launchOnce(::runBranch)

        private suspend fun runBranch() {
            gate.await()
            action()
        }
    }

    private data class VexStreamSnapshot(
        val snapshotRef: String,
        val state: VexStreamMusicState,
        val resources: List<SnapshotResource>,
        val provenanceRouteRef: String,
        val semanticCheckpoint: String,
    )

    private data class SnapshotResource(
        val relativePath: String,
        val sha256: String,
        val bytes: List<Byte>,
    )

    private data class BranchTrace(
        val snapshotRef: String,
        val branchRef: String,
        val routeRef: String,
        val completedPrefix: List<String>,
        val execution: VexCoroutineState,
    )

    private data class BranchWorld(
        val branchRef: String,
        val storageRoot: File,
        val mediaStore: VexStreamMediaStore,
        val offlineIndex: VexStreamOfflineIndex,
        var repository: InMemoryVexStreamMusicRepository,
        val executionRoot: VexExecutionRoot,
        val completedPrefix: MutableList<String> = mutableListOf(),
    ) {
        fun downloadedCopy(): VexStreamLocalCopyState.Downloaded =
            requireNotNull(
                repository.state.value.track(TRACK_REF)?.localCopy as?
                    VexStreamLocalCopyState.Downloaded,
            )

        fun trackAvailability(): VexStreamTrackAvailability =
            requireNotNull(repository.state.value.track(TRACK_REF)).availability

        fun trace(
            snapshotRef: String,
            routeRef: String,
            handle: VexCoroutineHandle,
        ): BranchTrace =
            BranchTrace(
                snapshotRef = snapshotRef,
                branchRef = branchRef,
                routeRef = routeRef,
                completedPrefix = completedPrefix.toList(),
                execution = handle.state.value,
            )

        fun deleteStorage() {
            storageRoot.deleteRecursively()
        }
    }

    private companion object {
        val TRACK_REF = VexStreamTrackRef("hub.concurrent.fixture")

        const val BRANCH_A = "branch-a-remove"
        const val BRANCH_B = "branch-b-offline-failure"
        const val BRANCH_C = "branch-c-restore-reconnect"

        const val ROUTE_A = "snapshot/remove-local-download"
        const val ROUTE_B = "snapshot/hub-unavailable/local-only/fail"
        const val ROUTE_C = "snapshot/fresh-offline-restore/reconnect"

        const val BRANCH_B_FAILURE =
            "synthetic branch B failure after LOCAL_ONLY"
        const val FIXTURE_MEDIA =
            "VexStream concurrent branch isolation fixture"
    }
}

// [VXG RealForever]
