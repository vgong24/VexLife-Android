package com.vextreme.vexlife.android.vexstream.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
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
) {
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
    }
}

@Composable
internal fun rememberVexStreamOfflineCopyConnection(
    repository: VexStreamMusicRepository,
    scope: CoroutineScope,
): VexStreamOfflineCopyConnection {
    val context =
        LocalContext.current
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

    return remember(
        repository,
        scope,
        context,
        permissionLauncher,
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
        )
    }
}

private val LEGACY_STORAGE_PERMISSIONS =
    arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

// [VXG RealForever]
