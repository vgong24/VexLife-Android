package com.vextreme.vexlife.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.blueprint.DebugElements
import com.vextreme.vexlife.android.blueprint.Screens
import com.vextreme.vexlife.android.elements.LocalElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexScrollableColumn
import com.vextreme.vexlife.android.elements.VexToggleRow
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.VexRuntime
import java.time.ZoneId

@Composable
fun DebugMenuScreen(
    runtime: VexRuntime,
    logger: VexLogger,
    darkTheme: Boolean,
) {
    val history by logger.history

    VexScrollableColumn(
        element = DebugElements.Root,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(Screens.DebugMenu.titleRes),
            style = MaterialTheme.typography.headlineLarge,
        )

        VexToggleRow(
            element = DebugElements.DarkMode,
            checked = darkTheme,
        )

        VexContainer(DebugElements.LogHistory) {
            Text(
                text = stringResource(DebugElements.LogHistoryTitle.textRes),
                style = MaterialTheme.typography.titleLarge,
            )

            VexRow(DebugElements.LogHistoryActions) {
                VexButton(DebugElements.Show)
                VexButton(DebugElements.Reset)
            }

            Text(
                text = stringResource(DebugElements.Helper.textRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (runtime.presentation.isOpen(DebugElements.LogDialog)) {
        val handler = LocalElementActionHandler.current
        AlertDialog(
            onDismissRequest = { handler.activate(DebugElements.CloseLogDialog) },
            title = {
                Text(stringResource(DebugElements.LogHistoryTitle.textRes))
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
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                VexButton(DebugElements.CloseLogDialog)
            },
        )
    }
}
