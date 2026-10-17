package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.runtime.VexRuntime

@Composable
fun ElementsGallery(
    runtime: VexRuntime,
) {
    Column(
        modifier =
            VexModifier.fillMaxSize(),
    ) {
        /*
         * Custom Compose foundations belong in Gallery beside the registered Element tree, not in
         * feature-local demo wrappers. Keeping this sample here makes the reusable component
         * discoverable without pretending it is a new app action or a static Blueprint cell.
         */
        GalleryExplainableStatusSample()

        VexElementTree(
            placement =
                runtime.registry.placement(
                    GalleryScreen.ElementsPanel,
                ),
            registry = runtime.registry,
            handler = runtime,
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .weight(1f),
            selectedChild = runtime.presentation::selected,
        )
    }

    GalleryModalSamples(runtime)
}
