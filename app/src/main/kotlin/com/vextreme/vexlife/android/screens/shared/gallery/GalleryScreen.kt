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
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewScenario
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.ScrollableColumnElement
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.swapTo
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.screens.shared.gallery.elements.ElementsGallery
import com.vextreme.vexlife.android.screens.shared.gallery.graph.AppGraphView
import com.vextreme.vexlife.android.runtime.AppActionRegistration
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime

object GalleryScreen : ScreenBlueprint(
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

    val ElementsTab = buttonRef(
        id = "page.bottom-navigation.elements",
        labelRes = R.string.gallery_elements,
        style = VexElementStyles.Navigation,
        intent = swapTo(ElementsPanel),
    )

    val AppGraphTab = buttonRef(
        id = "page.bottom-navigation.app-graph",
        labelRes = R.string.gallery_app_graph,
        style = VexElementStyles.Navigation,
        intent = swapTo(AppGraphPanel),
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
                            button(
                                id = "button",
                                labelRes = R.string.gallery_sample_button,
                                style = VexElementStyles.SecondaryAction,
                                intent = invoke(AppActions.GallerySample),
                            )
                        }
                        toggle(
                            id = "toggle",
                            labelRes = R.string.gallery_sample_toggle,
                            intent = invoke(AppActions.GallerySample),
                        )
                        fab(
                            id = "fab",
                            labelRes = R.string.gallery_sample_fab,
                            icon = ElementIcon.Add,
                            style = VexElementStyles.FabOption,
                            intent = invoke(AppActions.GallerySample),
                        )
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

/**
 * Scoped owner for Gallery's intentionally inert sample action.
 *
 * Keeping even the no-op sample registered prevents VexLifeApp from accumulating feature cases and
 * lets AppActionRouter detect accidental duplicate ownership.
 */
internal fun galleryActionRegistrations(): List<AppActionRegistration> =
    listOf(
        appAction(
            action = AppActions.GallerySample,
            ownerRef = GalleryScreen.screenId,
        ) {
            // Intentionally no product effect: this sample exists to exercise the reusable cell.
        },
    )

@VexScreenPreviews
@Composable
private fun GalleryScreenPreview() {
    VexPreviewHost(
        scenario = VexPreviewScenario(
            id = "gallery-elements",
            startScreen = GalleryScreen,
        ),
    ) { context ->
        GalleryScreenContent(context.runtime)
    }
}

@VexScreenPreviews
@Composable
private fun GalleryAppGraphPreview() {
    VexPreviewHost(
        scenario = VexPreviewScenario.galleryAppGraph(),
    ) { context ->
        GalleryScreenContent(context.runtime)
    }
}
