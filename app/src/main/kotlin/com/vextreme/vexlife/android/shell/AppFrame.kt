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
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * The app's coordinate-space boundary inside the Android window.
 *
 * Android owns status bars, cutouts, gesture/navigation areas, and other system insets. AppFrame
 * first reduces the raw window to [WindowInsets.safeDrawing], then gives Scaffold ownership of the
 * app bar, page viewport, bottom navigation, and FAB slot inside that safe rectangle.
 *
 * Debugging layout:
 * - if the whole app collides with phone/system chrome, start here;
 * - if only one page lays itself out incorrectly, follow [content] into that page's PageBlueprint;
 * - [runtime.currentScreen] tells you which page should currently occupy the viewport.
 */
@Composable
fun AppFrame(
    runtime: VexRuntime,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = runtime.canGoBack) {
        runtime.goBack()
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
                AppToolbar(
                    currentScreen = runtime.currentScreen,
                    runtime = runtime,
                    showBack = runtime.showToolbarBack,
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
