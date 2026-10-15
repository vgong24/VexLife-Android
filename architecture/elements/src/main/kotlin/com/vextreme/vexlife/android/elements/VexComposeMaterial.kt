package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Vex-owned Android realization of a registered AlertDialog.
 *
 * Entering the Vex component automatically establishes the dialog element as the inherited semantic
 * parent for title/body/custom content.
 */
@Composable
fun VexAlertDialog(
    element: DialogElement,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = VexModifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    VexComposeScope(
        parent = element,
    ) {
        AlertDialog(
            modifier =
                modifier.vex(
                    element,
                ),
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            dismissButton =
                dismissButton
                    ?: {},
            title = title,
            text = text,
        )
    }
}

/**
 * Vex-owned Android realization of a registered Material modal bottom sheet.
 *
 * Platform drag/sheet mechanics remain Material3's responsibility; semantic parent continuity and
 * inspectable root identity are inherited by construction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VexModalBottomSheet(
    element: ModalBottomSheetElement,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = VexModifier,
    dragHandle: @Composable (() -> Unit)? = {
        BottomSheetDefaults.DragHandle()
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    VexComposeScope(
        parent = element,
    ) {
        ModalBottomSheet(
            modifier =
                modifier.vex(
                    element,
                ),
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = dragHandle,
            content = content,
        )
    }
}

// [VXG RealForever]
