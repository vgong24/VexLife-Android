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
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import java.time.ZoneId

/**
 * Draggable Log History presentation.
 *
 * The semantic open/closed state still belongs to VexRuntime.presentation. Material's sheet state
 * only realizes that state on Android. Dragging the sheet down dismisses it without inventing a
 * synthetic "Clicked" log event; the explicit Close button remains available for accessibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogHistorySheet(
    runtime: VexRuntime,
    repository: LogHistoryRepository,
) {
    val history by repository.records.collectAsState(initial = emptyList())
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        onDismissRequest = {
            runtime.presentation.close(DebugMenuScreen.LogSheet)
        },
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                    .verticalScroll(rememberScrollState()),
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

            VexButton(
                element = DebugMenuScreen.CloseLog,
                handler = runtime,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
