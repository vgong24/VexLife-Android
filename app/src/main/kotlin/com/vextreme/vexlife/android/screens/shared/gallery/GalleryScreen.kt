package com.vextreme.vexlife.android.screens.shared.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.ScrollableColumnElement
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.elements.swapTo
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.shared.gallery.elements.ElementsGallery
import com.vextreme.vexlife.android.screens.shared.gallery.graph.AppGraphView
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.ui.ThemeMode

object GalleryScreen : AppScreenBlueprint(
    screenKey = "gallery",
    titleRes = R.string.gallery_title,
) {
    /*
     * These established semantic refs intentionally retain their historical "page" segment.
     * Screen is the Kotlin/source destination vocabulary; semantic element IDs are a separate
     * compatibility boundary and are not renamed cosmetically once observed/logged.
     */
    val ElementsPanel = scrollableColumnRef("page.content.elements")
    val AppGraphPanel = columnRef("page.content.app-graph")

    val Content = swappableRef(
        id = "page.content",
        defaultChild = ElementsPanel,
    )

    val DialogModalSample = dialogRef(
        id = "page.content.elements.modal-samples.dialog",
    )

    val BottomSheetModalSample = modalBottomSheetRef(
        id = "page.content.elements.modal-samples.bottom-sheet",
    )

    val DialogModalTitle = textRef(
        id = "page.content.elements.modal-samples.dialog.title",
        textRes = R.string.gallery_dialog_sample_title,
        role = TextRole.Title,
    )

    val DialogModalBody = textRef(
        id = "page.content.elements.modal-samples.dialog.body",
        textRes = R.string.gallery_dialog_sample_body,
        role = TextRole.Supporting,
    )

    val BottomSheetContent = columnRef(
        "page.content.elements.modal-samples.bottom-sheet.content",
    )

    val BottomSheetTitle = textRef(
        id = "page.content.elements.modal-samples.bottom-sheet.content.title",
        textRes = R.string.gallery_sheet_sample_title,
        role = TextRole.Title,
    )

    val BottomSheetBody = textRef(
        id = "page.content.elements.modal-samples.bottom-sheet.content.body",
        textRes = R.string.gallery_sheet_sample_body,
        role = TextRole.Supporting,
    )

    /*
     * These sample actions are named because semantic journeys now address production actions by
     * their real element objects. Their IDs intentionally match the former inline DSL-generated IDs.
     */
    val SampleButton = buttonRef(
        id = "page.content.elements.samples.button-row.button",
        labelRes = R.string.gallery_sample_button,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.GallerySample),
    )

    val SampleToggle = toggleRef(
        id = "page.content.elements.samples.toggle",
        labelRes = R.string.gallery_sample_toggle,
        intent = invoke(AppActions.GallerySample),
    )

    val SampleFab = fabRef(
        id = "page.content.elements.samples.fab",
        labelRes = R.string.gallery_sample_fab,
        icon = ElementIcon.Add,
        style = VexElementStyles.FabOption,
        intent = invoke(AppActions.GallerySample),
    )

    val ShowDialogModalSample = buttonRef(
        id = "page.content.elements.modal-samples.actions.show-dialog",
        labelRes = R.string.gallery_show_dialog_sample,
        style = VexElementStyles.SecondaryAction,
        intent = open(DialogModalSample),
    )

    val ShowBottomSheetModalSample = buttonRef(
        id = "page.content.elements.modal-samples.actions.show-bottom-sheet",
        labelRes = R.string.gallery_show_sheet_sample,
        style = VexElementStyles.SecondaryAction,
        intent = open(BottomSheetModalSample),
    )

    val CloseDialogModalSample = buttonRef(
        id = "page.content.elements.modal-samples.dialog.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(DialogModalSample),
    )

    val CloseBottomSheetModalSample = buttonRef(
        id = "page.content.elements.modal-samples.bottom-sheet.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(BottomSheetModalSample),
    )

    val ElementsTab = buttonRef(
        id = "page.bottom-navigation.elements",
        labelRes = R.string.gallery_elements,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = Content,
            child = ElementsPanel,
        ),
    )

    val AppGraphTab = buttonRef(
        id = "page.bottom-navigation.app-graph",
        labelRes = R.string.gallery_app_graph,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = Content,
            child = AppGraphPanel,
        ),
    )

    override val content = layout {
        column("page") {
            swappable(Content) {
                scrollableColumn(ElementsPanel) {
                    text(
                        id = "intro",
                        textRes = R.string.gallery_intro,
                        role = TextRole.Supporting,
                    )
                    card("samples") {
                        text(
                            id = "description",
                            textRes = R.string.gallery_sample_text,
                            role = TextRole.Supporting,
                        )
                        row("button-row") {
                            button(SampleButton)
                        }
                        toggle(SampleToggle)
                        fab(SampleFab)
                    }

                    card("modal-samples") {
                        text(
                            id = "title",
                            textRes = R.string.gallery_modal_samples_title,
                            role = TextRole.Title,
                        )
                        text(
                            id = "description",
                            textRes = R.string.gallery_modal_samples_body,
                            role = TextRole.Supporting,
                        )
                        row("actions") {
                            button(ShowDialogModalSample)
                            button(ShowBottomSheetModalSample)
                        }
                        dialog(DialogModalSample) {
                            text(DialogModalTitle)
                            text(DialogModalBody)
                            button(CloseDialogModalSample)
                        }
                        modalBottomSheet(BottomSheetModalSample) {
                            column(BottomSheetContent) {
                                text(BottomSheetTitle)
                                text(BottomSheetBody)
                            }
                            button(CloseBottomSheetModalSample)
                        }
                    }
                }

                column(AppGraphPanel) {
                    text(
                        id = "intro",
                        textRes = R.string.gallery_graph_intro,
                        role = TextRole.Supporting,
                    )
                    card("viewport") {}
                }
            }

            row("bottom-navigation") {
                button(ElementsTab)
                button(AppGraphTab)
            }
        }
    }

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        AppActionRegistrations.of(
            appAction(
                action = AppActions.GallerySample,
                ownerRef = screenId,
            ) {
                // Intentionally no product effect: this sample exists to exercise the reusable cell.
            },
        )

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        themeMode: ThemeMode,
    ) {
        GalleryScreenContent(runtime)
    }
}

@Composable
fun GalleryScreenContent(
    runtime: VexRuntime,
) {
    val selected = runtime.presentation.selected(GalleryScreen.Content)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                NavigationBarItem(
                    selected = selected.elementId == GalleryScreen.ElementsPanel.elementId,
                    onClick = { runtime.activate(GalleryScreen.ElementsTab) },
                    modifier = Modifier.vexElement(GalleryScreen.ElementsTab),
                    icon = { Text("E") },
                    label = { Text(stringResource(R.string.gallery_elements)) },
                )
                NavigationBarItem(
                    selected = selected.elementId == GalleryScreen.AppGraphPanel.elementId,
                    onClick = { runtime.activate(GalleryScreen.AppGraphTab) },
                    modifier = Modifier.vexElement(GalleryScreen.AppGraphTab),
                    icon = { Text("G") },
                    label = { Text(stringResource(R.string.gallery_app_graph)) },
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = selected.elementId,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "gallery-panel",
        ) { elementId ->
            if (elementId == GalleryScreen.AppGraphPanel.elementId) {
                AppGraphView(runtime)
            } else {
                ElementsGallery(runtime)
            }
        }
    }
}

@VexScreenPreviews
@Composable
private fun GalleryScreenPreview() {
    VexPreviewHost(
        id = "gallery-elements",
        startScreen = GalleryScreen,
    ) { context ->
        GalleryScreenContent(context.runtime)
    }
}

@VexScreenPreviews
@Composable
private fun GalleryAppGraphPreview() {
    VexPreviewHost(
        id = "gallery-app-graph",
        startScreen = GalleryScreen,
        previewSetup = {
            /*
             * Elements/AppGraph are peer panels INSIDE GalleryScreen. Production tabs use swapTo;
             * this preview only seeds which child should be visible initially.
             */
            selectChild(
                container = GalleryScreen.Content,
                child = GalleryScreen.AppGraphPanel,
            )
        },
    ) { context ->
        GalleryScreenContent(context.runtime)
    }
}
