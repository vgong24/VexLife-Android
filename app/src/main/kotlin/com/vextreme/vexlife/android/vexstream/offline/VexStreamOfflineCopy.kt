package com.vextreme.vexlife.android.vexstream.offline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class VexStreamOfflineCopyAction {
    Download,
    Remove,
}

internal data class VexStreamOfflineCopyRequest(
    val trackRef: VexStreamTrackRef,
    val action: VexStreamOfflineCopyAction,
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
) {
    fun request(
        request: VexStreamOfflineCopyRequest,
    ): Job =
        scope.launch {
            when (
                request.action
            ) {
                VexStreamOfflineCopyAction.Download ->
                    repository.downloadForOffline(
                        request.trackRef,
                    )

                VexStreamOfflineCopyAction.Remove ->
                    repository.removeOfflineCopy(
                        request.trackRef,
                    )
            }
        }

    fun request(
        request: VexStreamOfflineCollectionRequest,
    ): Job =
        scope.launch {
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
                    target.trackRef,
                )
            }
        }
}

@Composable
internal fun rememberVexStreamOfflineCopyConnection(
    repository: VexStreamMusicRepository,
    scope: CoroutineScope,
): VexStreamOfflineCopyConnection =
    remember(
        repository,
        scope,
    ) {
        VexStreamOfflineCopyConnection(
            repository = repository,
            scope = scope,
        )
    }

// [VXG RealForever]
