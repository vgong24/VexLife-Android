package com.vextreme.vexlife.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CancellationException

/**
 * Marker for one already-derived widget-facing projection.
 *
 * Product/domain State remains with its rightful owner. A widget receives only bounded facts needed
 * to render; this marker does not create another State machine or persistence owner.
 */
interface VexWidgetProjection

/**
 * Compose-first App Widget boundary for typed durable surface state.
 *
 * [SurfaceState] is a remote-surface snapshot, not SCAE/domain State. Product/provider owners map
 * their current truth into this bounded value. The foundation then owns:
 *
 * durable Preferences persistence
 * -> structural value equality / duplicate suppression
 * -> active Glance composition observation
 * -> suspendable SurfaceState -> Projection recomputation
 * -> bounded update fan-out
 *
 * Feature widgets keep ownership of the codec, initial bootstrap and visual Projection.
 */
abstract class VexGlanceAppWidget<
    SurfaceState : VexWidgetSurfaceState,
    Projection : VexWidgetProjection,
> : GlanceAppWidget() {
    final override val stateDefinition =
        PreferencesGlanceStateDefinition

    protected abstract val stateCodec:
        VexWidgetPreferenceCodec<
            SurfaceState
        >

    /**
     * Bootstrap only when no durable surface snapshot exists yet.
     *
     * This may read the rightful provider, repository or a bounded fallback. Once publication has
     * persisted state, active and restarted Glance sessions consume that durable snapshot instead.
     */
    protected abstract suspend fun initialSurfaceState(
        context: Context,
        glanceId: GlanceId,
    ): SurfaceState

    /**
     * Transform one typed surface snapshot into render-facing facts.
     *
     * This is suspendable so bounded asset decoding / filesystem reads do not have to happen inside
     * the Glance composable body.
     */
    protected abstract suspend fun projection(
        context: Context,
        glanceId: GlanceId,
        state: SurfaceState,
    ): Projection

    @Composable
    protected abstract fun Content(
        projection: Projection,
    )

    final override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appContext =
            context.applicationContext
        val initialPreferences =
            getAppWidgetState<
                Preferences
            >(
                appContext,
                id,
            )
        val initialState =
            stateCodec
                .read(
                    initialPreferences,
                )
                ?: initialSurfaceState(
                    context =
                        appContext,
                    glanceId = id,
                )
        val initialProjection =
            projection(
                context = appContext,
                glanceId = id,
                state = initialState,
            )

        provideContent {
            val preferences =
                currentState<
                    Preferences
                >()
            val currentState =
                stateCodec
                    .read(
                        preferences,
                    )
                    ?: initialState

            val currentProjection by
                produceState(
                    initialValue =
                        initialProjection,
                    key1 =
                        currentState,
                ) {
                    value =
                        projection(
                            context =
                                appContext,
                            glanceId = id,
                            state =
                                currentState,
                        )
                }

            Content(
                projection =
                    currentProjection,
            )
        }
    }

    /**
     * Persist one exact surface snapshot to one widget instance and request Glance update only when
     * structural value equality says the durable snapshot changed.
     */
    suspend fun publish(
        context: Context,
        glanceId: GlanceId,
        state: SurfaceState,
    ): VexWidgetPublishResult =
        publishOne(
            context =
                context.applicationContext,
            glanceId = glanceId,
            nextState = state,
        )

    /**
     * Publish the same surface snapshot to every installed instance of this widget class.
     */
    suspend fun publishAll(
        context: Context,
        state: SurfaceState,
    ): VexWidgetPublishResult {
        val appContext =
            context.applicationContext
        val glanceIds =
            GlanceAppWidgetManager(
                appContext,
            ).getGlanceIds(
                javaClass,
            )

        var changedCount =
            0
        var updateRequestedCount =
            0
        var failedCount =
            0

        for (glanceId in glanceIds) {
            val result =
                publishOne(
                    context =
                        appContext,
                    glanceId =
                        glanceId,
                    nextState =
                        state,
                )
            changedCount +=
                result.changedCount
            updateRequestedCount +=
                result.updateRequestedCount
            failedCount +=
                result.failedCount
        }

        return VexWidgetPublishResult(
            instanceCount =
                glanceIds.size,
            changedCount =
                changedCount,
            updateRequestedCount =
                updateRequestedCount,
            failedCount =
                failedCount,
        )
    }

    /**
     * Transform already-persisted surface state for every installed instance.
     *
     * This is for remote-surface facts whose rightful owner changes outside the main product/provider
     * observation stream (for example an artwork asset becoming locally available). Missing durable
     * state is skipped rather than invented.
     */
    suspend fun transformAll(
        context: Context,
        transform:
            suspend (
                SurfaceState,
            ) -> SurfaceState,
    ): VexWidgetPublishResult {
        val appContext =
            context.applicationContext
        val glanceIds =
            GlanceAppWidgetManager(
                appContext,
            ).getGlanceIds(
                javaClass,
            )

        var changedCount =
            0
        var updateRequestedCount =
            0
        var failedCount =
            0

        for (glanceId in glanceIds) {
            var changed =
                false
            try {
                updateAppWidgetState(
                    appContext,
                    glanceId,
                ) { preferences ->
                    val current =
                        stateCodec
                            .read(
                                preferences,
                            )
                            ?: return@updateAppWidgetState

                    val next =
                        transform(
                            current,
                        )
                    changed =
                        vexWidgetSurfaceStateChanged(
                            current =
                                current,
                            next =
                                next,
                        )
                    if (
                        changed
                    ) {
                        stateCodec
                            .write(
                                preferences =
                                    preferences,
                                state =
                                    next,
                            )
                    }
                }

                if (
                    changed
                ) {
                    changedCount +=
                        1
                    update(
                        appContext,
                        glanceId,
                    )
                    updateRequestedCount +=
                        1
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failedCount +=
                    1
            }
        }

        return VexWidgetPublishResult(
            instanceCount =
                glanceIds.size,
            changedCount =
                changedCount,
            updateRequestedCount =
                updateRequestedCount,
            failedCount =
                failedCount,
        )
    }

    private suspend fun publishOne(
        context: Context,
        glanceId: GlanceId,
        nextState: SurfaceState,
    ): VexWidgetPublishResult {
        var changed =
            false

        return try {
            updateAppWidgetState(
                context,
                glanceId,
            ) { preferences ->
                val current =
                    stateCodec
                        .read(
                            preferences,
                        )
                changed =
                    vexWidgetSurfaceStateChanged(
                        current =
                            current,
                        next =
                            nextState,
                    )

                if (
                    changed
                ) {
                    stateCodec
                        .write(
                            preferences =
                                preferences,
                            state =
                                nextState,
                        )
                }
            }

            var requested =
                false
            if (
                changed
            ) {
                update(
                    context,
                    glanceId,
                )
                requested =
                    true
            }

            VexWidgetPublishResult(
                instanceCount = 1,
                changedCount =
                    if (
                        changed
                    ) {
                        1
                    } else {
                        0
                    },
                updateRequestedCount =
                    if (
                        requested
                    ) {
                        1
                    } else {
                        0
                    },
                failedCount = 0,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VexWidgetPublishResult(
                instanceCount = 1,
                changedCount =
                    if (
                        changed
                    ) {
                        1
                    } else {
                        0
                    },
                updateRequestedCount = 0,
                failedCount = 1,
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
