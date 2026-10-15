package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen

/**
 * Gallery realizations of the two modal presentation styles preserved by the foundation.
 *
 * The registered modal is the Vex Compose parent. Nested Vex primitives declare only local IDs;
 * their complete semantic addresses are inherited from that parent, matching the same nested paths
 * declared in GalleryScreen's Blueprint.
 *
 * Material owns the platform modal mechanics. Vex owns the architectural entry point, semantic
 * identity, nested continuity and reusable cells rendered inside those platform surfaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryModalSamples(
    runtime: VexRuntime,
) {
    if (runtime.presentation.isOpen(GalleryScreen.DialogModalSample)) {
        VexComposeScope(
            parent = GalleryScreen.DialogModalSample,
        ) {
            AlertDialog(
                modifier =
                    VexModifier.vex(
                        GalleryScreen.DialogModalSample,
                    ),
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
    }

    if (runtime.presentation.isOpen(GalleryScreen.BottomSheetModalSample)) {
        VexComposeScope(
            parent = GalleryScreen.BottomSheetModalSample,
        ) {
            ModalBottomSheet(
                modifier =
                    VexModifier
                        .fillMaxHeight()
                        .vex(
                            GalleryScreen.BottomSheetModalSample,
                        ),
                onDismissRequest = {
                    runtime.dismissPresentation(
                        GalleryScreen.BottomSheetModalSample,
                    )
                },
                sheetState =
                    rememberModalBottomSheetState(
                        skipPartiallyExpanded = false,
                    ),
                dragHandle = {
                    BottomSheetDefaults.DragHandle()
                },
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
}

// [VXG RealForever]
