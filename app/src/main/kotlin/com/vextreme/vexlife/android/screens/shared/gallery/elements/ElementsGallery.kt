package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.runtime.VexRuntime

@Composable
fun ElementsGallery(
    runtime: VexRuntime,
) {
    VexElementTree(
        placement = runtime.registry.placement(GalleryScreen.ElementsPanel),
        registry = runtime.registry,
        handler = runtime,
        modifier = Modifier.fillMaxSize(),
        selectedChild = runtime.presentation::selected,
    )

    GalleryModalSamples(runtime)
}
