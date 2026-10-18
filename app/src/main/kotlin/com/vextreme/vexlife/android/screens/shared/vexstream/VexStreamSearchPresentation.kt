package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.VexTextField
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyConnection
import com.vextreme.vexlife.android.vexstream.track.rememberVexStreamTrackPlaybackConnection

/**
 * Search presentation for the VexStream library.
 *
 * Search query state remains Screen-local presentation state. Track/catalog truth and download
 * effects remain with their repository owners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexStreamSearchSheet(
    runtime: VexRuntime,
    tracks: List<VexStreamTrackState>,
    offlineCopyConnection: VexStreamOfflineCopyConnection?,
    controller: androidx.media3.session.MediaController?,
    resolver: VexStreamPlaybackSourceResolver,
    query: String,
    onQueryChange: (String) -> Unit,
    onVisitArtist: (String) -> Unit,
) {
    val trackPlaybackConnection =
        rememberVexStreamTrackPlaybackConnection(
            controller = controller,
            resolver = resolver,
        )

    VexModalBottomSheet(
        element = VexStreamMusicScreen.SearchSheet,
        modifier =
            VexModifier.fillMaxHeight(),
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.SearchSheet,
            )
        },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            ),
    ) {
        VexColumn(
            id = "content",
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            VexTextField(
                element = VexStreamMusicScreen.SearchQuery,
                value = query,
                onValueChange = onQueryChange,
            )

            if (query.isBlank()) {
                VexText(
                    element = VexStreamMusicScreen.SearchPrompt,
                    modifier =
                        VexModifier.padding(
                            vertical = 20.dp,
                        ),
                )
            } else if (tracks.isEmpty()) {
                VexText(
                    element =
                        VexStreamMusicScreen.SearchNoResults,
                    modifier =
                        VexModifier.padding(
                            vertical = 20.dp,
                        ),
                )
            } else {
                VexStableLazyColumn(
                    listRef =
                        VexStreamPresentationRefs
                            .SearchResultsList,
                    items = tracks,
                    itemRef = {
                        it.track.trackRef.value
                    },
                    contentType = {
                        "vexstream-track-row"
                    },
                    modifier =
                        VexModifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp),
                ) { track ->
                    VexStreamTrackCard(
                        runtime = runtime,
                        presentationScope =
                            VexStreamTrackPresentationScope.Search,
                        state = track,
                        contextTracks = tracks,
                        playbackConnection =
                            trackPlaybackConnection,
                        offlineCopyConnection =
                            offlineCopyConnection,
                        onVisitArtist = onVisitArtist,
                    )
                }
            }

            VexButton(
                element = VexStreamMusicScreen.CloseSearch,
                handler = runtime,
                modifier =
                    VexModifier.fillMaxWidth(),
            )
        }
    }
}

// [VXG RealForever]
