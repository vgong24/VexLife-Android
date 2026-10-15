package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexAlertDialog
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen

/**
 * Gallery realizations of the two modal presentation styles preserved by the Vex Compose foundation.
 *
 * Entering a Vex modal automatically establishes its registered element as the inherited semantic
 * parent. Nested cells therefore declare only local IDs while preserving the same full paths as the
 * GalleryScreen Blueprint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryModalSamples(
    runtime: VexRuntime,
) {
    if (runtime.presentation.isOpen(GalleryScreen.DialogModalSample)) {
        VexAlertDialog(
            element = GalleryScreen.DialogModalSample,
            onDismissRequest = {
                runtime.dismissPresentation(
                    GalleryScreen.DialogModalSample,
                )
            },
            title = {
                VexText(
                    id = "title",
                    text =
                        stringResource(
                            R.string.gallery_dialog_sample_title,
                        ),
                    role = TextRole.Title,
                )
            },
            text = {
                VexText(
                    id = "body",
                    text =
                        stringResource(
                            R.string.gallery_dialog_sample_body,
                        ),
                    role = TextRole.Supporting,
                )
            },
            confirmButton = {
                VexButton(
                    element =
                        GalleryScreen.CloseDialogModalSample,
                    handler = runtime,
                )
            },
        )
    }

    if (runtime.presentation.isOpen(GalleryScreen.BottomSheetModalSample)) {
        VexModalBottomSheet(
            element = GalleryScreen.BottomSheetModalSample,
            modifier =
                VexModifier.fillMaxHeight(),
            onDismissRequest = {
                runtime.dismissPresentation(
                    GalleryScreen.BottomSheetModalSample,
                )
            },
            sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = false,
                ),
        ) {
            VexColumn(
                id = "content",
                modifier =
                    VexModifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
            ) {
                VexText(
                    id = "title",
                    text =
                        stringResource(
                            R.string.gallery_sheet_sample_title,
                        ),
                    role = TextRole.Title,
                )
                VexText(
                    id = "body",
                    text =
                        stringResource(
                            R.string.gallery_sheet_sample_body,
                        ),
                    modifier =
                        VexModifier.padding(
                            vertical = 12.dp,
                        ),
                    role = TextRole.Supporting,
                )
            }

            VexButton(
                element =
                    GalleryScreen.CloseBottomSheetModalSample,
                handler = runtime,
                modifier =
                    VexModifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
            )
        }
    }
}

// [VXG RealForever]
