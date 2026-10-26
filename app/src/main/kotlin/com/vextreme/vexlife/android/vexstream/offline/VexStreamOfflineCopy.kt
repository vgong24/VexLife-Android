package com.vextreme.vexlife.android.vexstream.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamRepositoryResult
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class VexStreamOfflineCopyAction {
    Download,
    Remove,
}

internal data class VexStreamOfflineCopyRequest(
    val trackRef: VexStreamTrackRef,
    val action: VexStreamOfflineCopyAction,
    val expectedMediaSha256: VexStreamMediaSha256? = null,
    val expectedMediaByteLength: Long? = null,
)

/**
 * Owns only the execution lifetime of local-copy requests.
 *
 * The supplied VexLife session scope outlives rows, sheets and Activity recreation while the
 * in-process app session remains alive. The repository remains the operation/state/IO authority and
 * its StateFlow remains completion truth.
 *
 * A collection request is deliberately one sequential session-owned operation. Before each target,
 * current repository truth is re-read and exact media identity is revalidated. This means a stale
 * collection request cannot re-download an already completed copy or silently target changed media.
 *
 * This does not claim process-death survival, OS background resumption, WorkManager semantics, or a
 * generic operation framework.
 */
internal class VexStreamOfflineCopyConnection(
    private val repository: VexStreamMusicRepository,
    private val scope: CoroutineScope,
    private val canDownload: () -> Boolean = { true },
    private val requestDownloadPermission: (((() -> Unit)) -> Unit)? = null,
    private val canReadDeviceLibrary: () -> Boolean = { true },
    private val requestDeviceLibraryReadPermission: (((() -> Unit)) -> Unit)? = null,
) {
    /**
     * Requests only the Android read authority needed to rediscover durable shared audio after
     * VexLife loses MediaStore ownership (for example after Clear Data).
     *
     * Publication remains independently permissionless on API 29+ for rows this app owns.
     */
    fun ensureDeviceLibraryReadAccess(
        onGranted: () -> Unit,
    ): Boolean {
        if (canReadDeviceLibrary()) {
            return true
        }

        if (scope.isActive) {
            requestDeviceLibraryReadPermission
                ?.invoke {
                    if (scope.isActive) {
                        onGranted()
                    }
                }
        }

        return false
    }

    fun request(
        request: VexStreamOfflineCopyRequest,
    ): Job {
        if (
            request.action ==
            VexStreamOfflineCopyAction.Download &&
            !canDownload()
        ) {
            if (scope.isActive) {
                requestDownloadPermission
                    ?.invoke {
                        if (scope.isActive) {
                            request(
                                request,
                            )
                        }
                    }
            }

            return scope.launch {
                // Permission handoff is intentionally not a hidden session-scoped wait.
            }
        }

        return scope.launch {
            when (
                request.action
            ) {
                VexStreamOfflineCopyAction.Download ->
                    repository.downloadForOffline(
                        trackRef = request.trackRef,
                        expectedMediaSha256 =
                            request.expectedMediaSha256,
                        expectedMediaByteLength =
                            request.expectedMediaByteLength,
                    )

                VexStreamOfflineCopyAction.Remove ->
                    repository.removeOfflineCopy(
                        request.trackRef,
                    )
            }
        }
    }

    fun request(
        request: VexStreamOfflineCollectionRequest,
    ): Job {
        if (
            request.targets.isNotEmpty() &&
            !canDownload()
        ) {
            if (scope.isActive) {
                requestDownloadPermission
                    ?.invoke {
                        if (scope.isActive) {
                            request(
                                request,
                            )
                        }
                    }
            }

            return scope.launch {
                // Permission handoff is intentionally not a hidden session-scoped wait.
            }
        }

        return scope.launch {
            downloadCollection(
                request,
            )
        }
    }

    /**
     * One deliberate "make the phone current" pass over already-owned VexStream truth.
     *
     * This is not persistent Keep Offline / Sync. Each invocation first refreshes the current Desktop
     * catalog, then downloads only exact currently-missing audio targets, then asks the repository to
     * cache only admitted artwork that is still missing. Already-downloaded audio is never replayed,
     * and artwork remains ancillary to verified audio truth.
     *
     * On legacy Android, missing shared-storage permission holds only the audio portion. Existing
     * artwork reconciliation may still run before the permission handoff, and a granted retry
     * re-reads the refreshed catalog before forming exact download targets again.
     */
    fun requestLibraryReconciliation(): Job =
        scope.launch {
            val refresh =
                repository
                    .refreshCatalog()
            if (
                refresh !is
                VexStreamRepositoryResult.Accepted
            ) {
                return@launch
            }

            val downloadRequest =
                vexStreamOfflineCollectionPlan(
                    repository
                        .state
                        .value
                        .playableTracks(),
                ).requestOrNull()

            if (
                downloadRequest != null &&
                !canDownload()
            ) {
                repository
                    .backfillOfflineArtwork()

                if (scope.isActive) {
                    requestDownloadPermission
                        ?.invoke {
                            if (scope.isActive) {
                                requestLibraryReconciliation()
                            }
                        }
                }
                return@launch
            }

            downloadRequest
                ?.let {
                    downloadCollection(
                        it,
                    )
                }

            repository
                .backfillOfflineArtwork()
        }

    private suspend fun downloadCollection(
        request: VexStreamOfflineCollectionRequest,
    ) {
        request.targets.forEach { target ->
            val current =
                repository
                    .state
                    .value
                    .track(
                        target.trackRef,
                    )

            if (
                !target.isCurrentlyDownloadable(
                    current,
                )
            ) {
                return@forEach
            }

            repository.downloadForOffline(
                trackRef = target.trackRef,
                expectedMediaSha256 =
                    target.mediaSha256,
                expectedMediaByteLength =
                    target.mediaByteLength,
            )
        }
    }

    fun request(
        request: VexStreamOfflineCollectionRemovalRequest,
    ): Job =
        scope.launch {
            request.trackRefs.forEach { trackRef ->
                val current =
                    repository
                        .state
                        .value
                        .track(
                            trackRef,
                        )

                if (
                    current?.localCopy !is
                    VexStreamLocalCopyState.Downloaded
                ) {
                    return@forEach
                }

                repository.removeOfflineCopy(
                    trackRef,
                )
            }
        }
}

@Composable
internal fun rememberVexStreamOfflineCopyConnection(
    repository: VexStreamMusicRepository,
    scope: CoroutineScope,
): VexStreamOfflineCopyConnection {
    val context =
        LocalContext.current
    val inspectionMode =
        LocalInspectionMode.current
    val state by
        repository.state.collectAsState()
    val pendingRetry =
        remember {
            mutableStateOf<
                (() -> Unit)?
            >(
                null,
            )
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions(),
        ) {
                grants,
            ->
            val granted =
                LEGACY_STORAGE_PERMISSIONS
                    .all { permission ->
                        grants[permission] == true ||
                            ContextCompat
                                .checkSelfPermission(
                                    context,
                                    permission,
                                ) ==
                            PackageManager
                                .PERMISSION_GRANTED
                    }
            val retry =
                pendingRetry.value
            pendingRetry.value =
                null

            if (granted) {
                retry?.invoke()
            }
        }

    val deviceLibraryReadPermission =
        vexStreamDeviceLibraryReadPermission(
            Build.VERSION.SDK_INT,
        )
    val deviceLibraryReadRetry =
        remember {
            mutableStateOf<
                (() -> Unit)?
            >(
                null,
            )
        }
    val deviceLibraryReadRequested =
        remember {
            mutableStateOf(false)
        }
    val deviceLibraryReadLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestPermission(),
        ) { granted ->
            val retry =
                deviceLibraryReadRetry.value
            deviceLibraryReadRetry.value =
                null

            if (granted) {
                retry?.invoke()
            }
        }

    val connection =
        remember(
            repository,
            scope,
            context,
            permissionLauncher,
            deviceLibraryReadLauncher,
            deviceLibraryReadPermission,
        ) {
            VexStreamOfflineCopyConnection(
            repository = repository,
            scope = scope,
            canDownload = {
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q ||
                    LEGACY_STORAGE_PERMISSIONS
                        .all { permission ->
                            ContextCompat
                                .checkSelfPermission(
                                    context,
                                    permission,
                                ) ==
                            PackageManager
                                .PERMISSION_GRANTED
                        }
            },
            requestDownloadPermission = {
                    retry,
                ->
                if (
                    pendingRetry.value ==
                    null
                ) {
                    pendingRetry.value =
                        retry
                    permissionLauncher.launch(
                        LEGACY_STORAGE_PERMISSIONS,
                    )
                }
            },
            canReadDeviceLibrary = {
                ContextCompat
                    .checkSelfPermission(
                        context,
                        deviceLibraryReadPermission,
                    ) ==
                    PackageManager.PERMISSION_GRANTED
            },
            requestDeviceLibraryReadPermission = {
                    retry,
                ->
                if (
                    deviceLibraryReadRetry.value ==
                    null
                ) {
                    deviceLibraryReadRetry.value =
                        retry
                    deviceLibraryReadLauncher.launch(
                        deviceLibraryReadPermission,
                    )
                }
            },
        )
    }

    LaunchedEffect(
        inspectionMode,
        state.connectionState,
        state.hubState,
        connection,
    ) {
        if (inspectionMode) {
            return@LaunchedEffect
        }

        if (
            state.connectionState !is
            VexStreamConnectionState.Paired
        ) {
            deviceLibraryReadRequested.value =
                false
            return@LaunchedEffect
        }

        if (
            state.hubState !is
            VexStreamHubState.Ready ||
            deviceLibraryReadRequested.value
        ) {
            return@LaunchedEffect
        }

        deviceLibraryReadRequested.value =
            true
        connection.ensureDeviceLibraryReadAccess {
            scope.launch {
                repository.refreshCatalog()
            }
        }
    }

    return connection
}

internal fun vexStreamDeviceLibraryReadPermission(
    apiLevel: Int,
): String =
    if (
        apiLevel >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private val LEGACY_STORAGE_PERMISSIONS =
    arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

// [VXG RealForever]
