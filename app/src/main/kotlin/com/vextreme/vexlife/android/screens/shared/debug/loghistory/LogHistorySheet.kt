package com.vextreme.vexlife.android.screens.shared.debug.loghistory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import java.time.ZoneId

/**
 * Draggable Log History presentation.
 *
 * The semantic open/closed state still belongs to VexRuntime.presentation. Material's sheet state
 * only realizes that state on Android. It opens partially, can be dragged to full height, and can
 * be dragged/scrim-dismissed. Platform dismissal is recorded as "Dismissed" against the sheet's
 * semantic identity rather than inventing a Close-button click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogHistorySheet(
    runtime: VexRuntime,
    repository: LogHistoryRepository,
) {
    val history by repository.records.collectAsState(initial = emptyList())
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        onDismissRequest = {
            runtime.dismissPresentation(DebugMenuScreen.LogSheet)
        },
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.log_history),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        state = rememberScrollState(),
                        overscrollEffect = null,
                    ),
            ) {
                SelectionContainer {
                    Text(
                        text = VexLogger.formatHistory(
                            records = history,
                            zoneId = ZoneId.systemDefault(),
                        ),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

        }
    }
}
