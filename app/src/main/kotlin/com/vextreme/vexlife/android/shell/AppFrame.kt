package com.vextreme.vexlife.android.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.runtime.VexRuntime

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
fun AppFrame(
    runtime: VexRuntime,
    toolbarActions: List<ButtonElement> = emptyList(),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
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
            floatingActionButton = {
                if (runtime.isPrimary()) {
                    AppFabMenu(runtime)
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                content()
            }
        }
    }
}
