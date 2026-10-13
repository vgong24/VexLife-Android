package com.vextreme.vexlife.android.screens.shared.maps

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPage
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef

internal enum class VexMapsOnlineStyle(
    val styleRef: String,
    @StringRes val labelRes: Int,
    val styleUri: String,
) {
    DARK(
        styleRef = "dark",
        labelRes = R.string.vexmaps_style_dark,
        styleUri =
            "https://tiles.openfreemap.org/styles/dark",
    ),
    LIBERTY(
        styleRef = "liberty",
        labelRes = R.string.vexmaps_style_liberty,
        styleUri =
            "https://tiles.openfreemap.org/styles/liberty",
    ),
    ;

    val dockPageRef: VexContextDockPageRef
        get() =
            VexContextDockPageRef(
                "vexmaps.style.$styleRef",
            )

    companion object {
        fun default(
            darkTheme: Boolean,
        ): VexMapsOnlineStyle =
            if (darkTheme) {
                DARK
            } else {
                LIBERTY
            }

        fun restore(
            name: String?,
            darkTheme: Boolean,
        ): VexMapsOnlineStyle =
            entries.firstOrNull {
                it.name == name
            } ?: default(darkTheme)

        fun fromStyleRef(
            styleRef: String?,
        ): VexMapsOnlineStyle? =
            entries.firstOrNull {
                it.styleRef == styleRef
            }

        fun fromDockPageRef(
            pageRef: VexContextDockPageRef,
        ): VexMapsOnlineStyle? =
            entries.firstOrNull {
                it.dockPageRef == pageRef
            }
    }
}

internal fun VexMapsOnlineStyle.contextDockPage():
    VexContextDockPage =
    VexContextDockPage(
        pageRef = dockPageRef,
    ) { expanded ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text =
                    stringResource(
                        labelRes,
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
            )
            if (expanded) {
                Text(
                    text =
                        stringResource(
                            R.string
                                .vexmaps_style_online_hint,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            }
        }
    }

// [VXG RealForever]
