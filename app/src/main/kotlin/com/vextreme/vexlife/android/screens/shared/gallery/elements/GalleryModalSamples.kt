package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen

/**
 * Gallery realizations of the two modal presentation styles preserved by the foundation.
 *
 * Their semantic identities/open state live in GalleryScreen + VexRuntime.presentation. This file
 * only demonstrates the Android-native realization differences.
 *
 * The AlertDialog example deliberately preserves the centered-dialog pattern that Log History used
 * before Log History graduated to a draggable bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryModalSamples(
    runtime: VexRuntime,
) {
    if (runtime.presentation.isOpen(GalleryScreen.DialogModalSample)) {
        AlertDialog(
            onDismissRequest = {
                runtime.dismissPresentation(GalleryScreen.DialogModalSample)
            },
            title = {
                Text(stringResource(R.string.gallery_dialog_sample_title))
            },
            text = {
                Text(stringResource(R.string.gallery_dialog_sample_body))
            },
            confirmButton = {
                VexButton(
                    element = GalleryScreen.CloseDialogModalSample,
                    handler = runtime,
                )
            },
        )
    }

    if (runtime.presentation.isOpen(GalleryScreen.BottomSheetModalSample)) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = {
                runtime.dismissPresentation(GalleryScreen.BottomSheetModalSample)
            },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false,
            ),
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.gallery_sheet_sample_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.gallery_sheet_sample_body),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                VexButton(
                    element = GalleryScreen.CloseBottomSheetModalSample,
                    handler = runtime,
                )
            }
        }
    }
}
