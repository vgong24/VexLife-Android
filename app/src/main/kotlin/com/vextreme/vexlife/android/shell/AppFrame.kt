package com.vextreme.vexlife.android.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.ui.look.VexAmbientField
import com.vextreme.vexlife.android.ui.look.VexAmbientLiveness

/**
 * The app's coordinate-space boundary inside the Android window.
 *
 * Android owns status bars, cutouts, gesture/navigation areas, and other system insets. AppFrame
 * first reduces the raw window to [WindowInsets.safeDrawing], then gives Scaffold ownership of the
 * app bar, screen viewport, bottom navigation, and FAB slot inside that safe rectangle.
 *
 * Debugging layout:
 * - if the whole app collides with phone/system chrome, start here;
 * - if only one screen lays itself out incorrectly, follow [content] into that screen's ScreenBlueprint;
 * - [runtime.currentScreen] tells you which screen should currently occupy the viewport.
 */
@Composable
internal fun AppFrame(
    runtime: VexRuntime,
    toolbarActions: List<ButtonElement> = emptyList(),
    // The convergence deck layer. The running app passes VexDeckHost; fixtures pass VexDeckLayer.
    deck: @Composable (modifier: Modifier, onFootprintChanged: (VexDeckFootprint) -> Unit) -> Unit = { _, _ -> },
    // Ambient light input. Null reads live glances; preview/screenshot fixtures pass their own.
    ambientLiveness: VexAmbientLiveness? = null,
    content: @Composable () -> Unit,
) {
    // One resolved Back plan drives both Android native Back and the toolbar affordance below.
    // Do not re-derive "can go back" independently here.
    val nextBack = runtime.nextBack

    // Android Studio Preview reuses the real frame visuals but has no meaningful native Back host.
    if (!LocalInspectionMode.current) {
        BackHandler(enabled = nextBack != null) {
            nextBack?.let { runtime.goBack(it) }
        }
    }

    val liveAmbient =
        rememberVexAmbientLiveness()
    var deckFootprint by remember { mutableStateOf(VexDeckFootprint()) }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Behind everything, including system bars: VexLife's ambient light.
        VexAmbientField(
            liveness = ambientLiveness ?: liveAmbient,
        )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                VexAppToolBar(
                    currentScreen = runtime.currentScreen,
                    runtime = runtime,
                    showBack = nextBack?.showToolbarBack == true,
                    actions = toolbarActions,
                )
            },
            bottomBar = {
                if (runtime.isPrimary()) {
                    PrimaryBottomNavigation(
                        currentScreen = runtime.currentScreen,
                        runtime = runtime,
                    )
                }
            },

        ) { innerPadding ->
            /*
             * Layers, back to front: the Screen (a stage Screen draws under the deck; others keep
             * clear of it through VexDeckClearance), the convergence deck, then the FAB riding above
             * the resting deck. One layout pass measures all three, so nothing jumps a frame late.
             */
            VexStageLayers(
                raised = deckFootprint.raised,
                modifier = Modifier.padding(innerPadding),
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(runtime.currentScreen.screenId),
                    ) {
                        content()
                    }
                },
                deck = {
                    deck(Modifier) { footprint ->
                        if (footprint != deckFootprint) {
                            deckFootprint = footprint
                        }
                    }
                },
                fab = {
                    // Steps aside while the deck is raised over the stage.
                    if (runtime.isPrimary() && !deckFootprint.raised) {
                        AppFabMenu(runtime)
                    }
                },
            )
        }
    }
    }
}

private enum class VexStageSlot { Content, Deck, Fab }

/**
 * [content] fills the viewport; [deck] sits at the bottom; [fab] rides above the deck's resting top.
 *
 * The resting inset is the deck's measured height while it is at Peek or Tucked, held while it is
 * [raised] (Open / Full overlay the stage rather than pushing it). [LocalVexDeckInset] carries it to
 * the content in the same pass.
 */
@Composable
private fun VexStageLayers(
    raised: Boolean,
    content: @Composable () -> Unit,
    deck: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastResting = remember { IntArray(1) }
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val deckPlaceables = subcompose(VexStageSlot.Deck, deck).map { it.measure(loose) }
        val deckHeight = deckPlaceables.maxOfOrNull { it.height } ?: 0
        val resting =
            if (raised && deckHeight > 0) {
                lastResting[0]
            } else {
                deckHeight.also { lastResting[0] = it }
            }
        val insetDp = resting.toDp()
        val contentPlaceables =
            subcompose(VexStageSlot.Content) {
                CompositionLocalProvider(LocalVexDeckInset provides insetDp, content = content)
            }.map { it.measure(constraints) }
        val fabPlaceables = subcompose(VexStageSlot.Fab, fab).map { it.measure(loose) }
        val margin = 16.dp.roundToPx()

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            deckPlaceables.forEach {
                it.place((constraints.maxWidth - it.width) / 2, constraints.maxHeight - it.height)
            }
            fabPlaceables.forEach {
                it.place(
                    constraints.maxWidth - it.width - margin,
                    constraints.maxHeight - resting - it.height - margin / 2,
                )
            }
        }
    }
}
