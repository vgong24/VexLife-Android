package com.vextreme.vexlife.android.screens.shared.debug.loghistory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
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
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.runtime.VexRuntime
import java.time.ZoneId

@Composable
fun LogHistoryDialog(
    runtime: VexRuntime,
    repository: LogHistoryRepository,
) {
    val history by repository.records.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = { runtime.activate(DebugMenuScreen.CloseLog) },
        title = {
            Text(stringResource(R.string.log_history))
        },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = VexLogger.formatHistory(history, ZoneId.systemDefault()),
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        confirmButton = {
            VexButton(
                element = DebugMenuScreen.CloseLog,
                handler = runtime,
            )
        },
    )
}
