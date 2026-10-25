package com.vextreme.vexlife.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback

/**
 * Marker for one already-derived widget-facing projection.
 *
 * Product/domain State remains with its rightful owner. A widget receives only the bounded facts it
 * needs to render; this marker does not create another state machine or persistence owner.
 */
interface VexWidgetProjection

/**
 * Compose-first App Widget boundary that requires projection before Glance rendering.
 *
 * Subclasses decide which product owner supplies [Projection]. This foundation owns only the Android
 * remote-surface lifecycle and never reaches into product State from the composable body.
 */
abstract class VexGlanceAppWidget<
    Projection : VexWidgetProjection,
> : GlanceAppWidget() {
    protected abstract suspend fun projection(
        context: Context,
        glanceId: GlanceId,
    ): Projection

    @Composable
    protected abstract fun Content(
        projection: Projection,
    )

    final override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val current =
            projection(
                context = context,
                glanceId = id,
            )

        provideContent {
            Content(
                projection = current,
            )
        }
    }
}

/**
 * Stable widget interaction identity.
 *
 * The ref answers which widget doorway was activated. It does not name an Act or Effect; product
 * code resolves this bounded identity against its own current Control/State semantics.
 */
@JvmInline
value class VexWidgetActionRef(
    val value: String,
) {
    init {
        require(
            REF.matches(
                value,
            ),
        ) {
            "VexWidgetActionRef must be a lowercase semantic ref"
        }
    }

    private companion object {
        val REF =
            Regex(
                "^[a-z0-9]+(?:[.-][a-z0-9]+)*$",
            )
    }
}

val VexWidgetActionRefKey =
    ActionParameters.Key<String>(
        "vex.widget.action-ref",
    )

fun vexWidgetActionParameters(
    actionRef: VexWidgetActionRef,
): ActionParameters =
    actionParametersOf(
        VexWidgetActionRefKey to
            actionRef.value,
    )

/**
 * Android callback membrane for widget interaction.
 *
 * Glance instantiates callbacks through a public zero-argument constructor. This base class only
 * validates/extracts stable interaction identity; subclasses route that identity to the rightful
 * product Control/Connection. Long-running work remains outside this receiver-bound callback.
 */
abstract class VexWidgetActionCallback :
    ActionCallback {
    final override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val raw =
            parameters[
                VexWidgetActionRefKey
            ] ?: return
        val actionRef =
            runCatching {
                VexWidgetActionRef(
                    raw,
                )
            }.getOrNull()
                ?: return

        onWidgetAction(
            context = context,
            glanceId = glanceId,
            actionRef = actionRef,
        )
    }

    protected abstract suspend fun onWidgetAction(
        context: Context,
        glanceId: GlanceId,
        actionRef: VexWidgetActionRef,
    )
}

// [VXG RealForever]
