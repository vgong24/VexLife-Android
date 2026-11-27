package com.vextreme.vexlife.android.vexstream

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.player.VexStreamFiniteRequest
import com.vextreme.vexlife.android.vexstream.player.VexStreamFiniteRequestOwners
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.player.requestVexStreamFiniteControl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What a compact surface outside the Music screen may show about current playback.
 *
 * Human question: "What's playing, and is it playing?" — asked from Home, the app-shell Now strip,
 * or (later) Vex context. This is a glance, not a second player: identity and metadata come from the
 * process Player Store, play intent and pending state from the process Playback Family.
 */
internal data class VexStreamNowPlaying(
    val title: String,
    val artist: String,
    val playRequested: Boolean,
    val transitioning: Boolean,
    val playPauseAvailable: Boolean,
    val ended: Boolean,
)

/**
 * Pure projection of retained Player Store truth plus current Playback Family State.
 *
 * Returns null when there is no current media, so a compact surface disappears rather than
 * pretending something is loaded.
 */
internal fun vexStreamNowPlaying(
    snapshot:
        VexStreamPlayerStore.Snapshot,
    playback:
        VexStreamPlaybackState,
): VexStreamNowPlaying? {
    val available =
        snapshot as?
            VexStreamPlayerStore.Snapshot.Available
            ?: return null
    if (!available.playback.hasMedia) {
        return null
    }
    val media =
        available.currentMedia

    return when (
        playback
    ) {
        VexStreamPlaybackState.NoMedia ->
            null

        is VexStreamPlaybackState.Media ->
            VexStreamNowPlaying(
                title =
                    media.title,
                artist =
                    media.artist,
                playRequested =
                    playback.playRequested,
                transitioning =
                    playback.requestPending,
                playPauseAvailable =
                    playback.playPauseAvailable,
                ended =
                    false,
            )

        is VexStreamPlaybackState.Ended ->
            VexStreamNowPlaying(
                title =
                    media.title,
                artist =
                    media.artist,
                playRequested =
                    false,
                transitioning =
                    playback.restartPending,
                playPauseAvailable =
                    playback.restartAvailable,
                ended =
                    true,
            )
    }
}

/**
 * Android configuration-lifetime composition owner for compact Now Playing surfaces outside the
 * Music screen.
 *
 * It borrows the process Player Store and Playback / Queue Families exactly like
 * [VexStreamMusicViewModel], but deliberately owns no MediaController: showing a glance must not
 * keep the playback Service bound. A tap borrows one controller for that one request through
 * [requestVexStreamFiniteControl], the same path the home-screen widget uses.
 */
internal class VexStreamNowPlayingViewModel(
    application:
        Application,
) :
    AndroidViewModel(
        application,
    ) {
    private val vexApplication =
        application as?
            VexApplication
            ?: error(
                "VexStream Now Playing requires VexApplication as the process composition root",
            )

    private val owners =
        vexApplication.dependencies.let { dependencies ->
            val module =
                vexApplication.dependencyModule

            VexStreamFiniteRequestOwners(
                playerStore =
                    dependencies.resolve(
                        module.playerStore,
                    ),
                playbackFamily =
                    dependencies.resolve(
                        module.playbackFamily,
                    ),
                queueFamily =
                    dependencies.resolve(
                        module.queueFamily,
                    ),
                executionRoot =
                    dependencies.resolve(
                        module.processExecutionRoot,
                    ),
            )
        }

    val nowPlaying:
        StateFlow<VexStreamNowPlaying?> =
        combine(
            owners.playerStore.states,
            owners.playbackFamily.states,
            ::vexStreamNowPlaying,
        ).stateIn(
            scope =
                viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    5_000,
                ),
            initialValue =
                vexStreamNowPlaying(
                    owners.playerStore.current,
                    owners.playbackFamily.currentState,
                ),
        )

    /** Human tap on a visible toggle: the listener sees current state, so PlayPause is truthful. */
    fun togglePlayback() {
        viewModelScope.launch {
            requestVexStreamFiniteControl(
                context =
                    getApplication(),
                owners =
                    owners,
                request =
                    VexStreamFiniteRequest.Playback(
                        VexStreamPlaybackControl.PlayPause,
                    ),
            )
        }
    }
}

/** One composed reading of Now Playing plus the one action a compact surface may offer. */
internal class VexStreamNowPlayingHandle(
    val nowPlaying:
        VexStreamNowPlaying?,
    val togglePlayback:
        () -> Unit,
)

/**
 * Live Now Playing for a compact surface.
 *
 * Returns null in Compose preview / Layoutlib, where no VexApplication process graph exists; preview
 * and screenshot fixtures pass a [VexStreamNowPlaying] to the presentational composable instead.
 */
@Composable
internal fun rememberVexStreamNowPlaying(): VexStreamNowPlayingHandle? {
    if (LocalInspectionMode.current) {
        return null
    }
    val owner: VexStreamNowPlayingViewModel =
        viewModel()
    val nowPlaying by
        owner
            .nowPlaying
            .collectAsStateWithLifecycle()

    return remember(
        nowPlaying,
        owner,
    ) {
        VexStreamNowPlayingHandle(
            nowPlaying =
                nowPlaying,
            togglePlayback =
                owner::togglePlayback,
        )
    }
}

// [VXG RealForever][VEXSTREAM][NOW-PLAYING][GLANCE]
