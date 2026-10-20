package com.vextreme.vexlife.android.vexstream.offline

/**
 * # VexStream Offline Collection
 *
 * Search this symbol when asking:
 *
 * "How much of this VexStream collection is already on this phone, and how do I download only the
 * media that is still missing?"
 *
 * Project/lane search alias: MassDownload.
 *
 * This class is documentation/navigation only. It owns no repository state, media bytes, collection
 * membership, coroutine lifetime, or Compose state.
 *
 * ## Human outcome
 *
 * A listener opens an album, artist collection, or playlist and can:
 *
 * - see how many songs are already verified on this phone;
 * - ask VexLife to Download missing;
 * - avoid re-requesting tracks already Downloaded or currently Downloading;
 * - keep exact per-track failure/unavailable truth visible instead of calling partial completion
 *   "synced".
 *
 * ## Current one-shot flow
 *
 * rendered collection
 * -> [vexStreamOfflineCollectionPlan]
 * -> current on-device / downloading / downloadable / blocked relationship
 * -> [VexStreamOfflineCollectionRequest]
 * -> [VexStreamOfflineCopyConnection]
 * -> repository downloadForOffline(...) one exact target at a time
 * -> repository observation updates local-copy truth
 * -> plan/projection is derived again from current truth
 *
 * [VexStreamOfflineCopyTarget] binds current Hub address to expected SHA-256 + byte length. The
 * Connection re-reads repository truth before every target so a stale rendered collection cannot
 * download changed media merely because a trackRef string still matches.
 *
 * ## What "Download missing" means
 *
 * Downloaded
 * -> already on device
 * -> do not request again
 *
 * Downloading
 * -> work already in flight
 * -> do not start a duplicate request
 *
 * NotDownloaded + Hub available
 * -> requestable missing target
 *
 * retryable Failed + Hub available
 * -> requestable missing target
 *
 * unavailable / non-retryable missing
 * -> remain blocked in the current projection
 *
 * Effect attempt never becomes success truth. The repository/media store owns verification and
 * publishes the resulting local-copy State.
 *
 * ## Why this is not persistent sync yet
 *
 * "Download missing now" is one bounded request over the exact collection the listener acted on.
 *
 * "Keep this collection available offline" is a different persistent intention. That later feature
 * would require durable collection identity, desired-offline intent, restart/background semantics,
 * and convergence against changing collection membership. Current album grouping is presentation
 * derived and is not promoted into durable identity merely to make that future design convenient.
 *
 * Therefore:
 *
 * ONE_SHOT_DOWNLOAD_MISSING
 * !=
 * PERSISTENT_KEEP_OFFLINE
 *
 * ON_DEVICE
 * !=
 * SYNC_POLICY_SATISFIED
 *
 * ## Runtime policy
 *
 * The first slice intentionally executes targets sequentially through the existing session-lifetime
 * Offline Copy Connection. The repository already serializes its own media operations and remains
 * the byte/state authority.
 *
 * Bounded parallel download, process-death resumption, WorkManager/background execution, free-space
 * admission, whole-library download, and multi-delete are held until a real product requirement
 * earns those owners.
 *
 * ## Proof map
 *
 * Pure plan/request laws:
 * -> focused JVM tests beside this package.
 *
 * Human collection action:
 * -> VexStream Feature Walk + Journey.
 *
 * Real byte/network/storage behavior:
 * -> repository/media-store + Android/provider proof where claimed.
 *
 * ## QuantumTest relationship
 *
 * QuantumTest does not own this feature.
 *
 * After the ordinary MassDownload vertical slice is proven, a useful branch-reuse checkpoint is a
 * partially downloaded collection: some exact media already verified locally, some missing and
 * requestable. Descendant branches may then pressure success, partial failure, Hub loss, and repeat
 * request behavior without turning the Quantum experiment into a second product model.
 */
@Suppress("unused")
internal class VexStreamOfflineDocs private constructor()

// [VXG RealForever]
