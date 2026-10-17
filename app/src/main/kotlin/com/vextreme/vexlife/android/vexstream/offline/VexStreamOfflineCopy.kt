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
 * Owns only the execution lifetime of one local-copy request.
 *
 * The supplied VexLife session scope outlives rows, sheets and Activity recreation while the
 * in-process app session remains alive. The repository remains the operation/state/IO authority and
 * its StateFlow remains completion truth.
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
