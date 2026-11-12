package com.vextreme.vexlife.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
 * Foundation-owned currentness coordinates for the persisted SurfaceState -> Glance update-request
 * boundary.
 *
 * generation identifies only the update-request occurrence. It is not product/domain revision,
 * semantic identity or launcher-render acknowledgement. The last generation remains persisted after
 * settlement so an older in-flight completion cannot collide with a later request that happens to
 * target an equal SurfaceState value.
 *
 * pending means the latest generation still owes one successful Glance update() request.
 */
private val VexWidgetUpdateRequestGenerationKey =
    longPreferencesKey(
        "vex.widget.update-request-generation",
    )

private val VexWidgetUpdateRequestPendingKey =
    booleanPreferencesKey(
        "vex.widget.update-request-pending",
    )

private fun Preferences.vexWidgetUpdateRequestGeneration(): Long =
    this[
        VexWidgetUpdateRequestGenerationKey
    ] ?: 0L

private fun Preferences.vexWidgetUpdateRequestPending(): Boolean =
    this[
        VexWidgetUpdateRequestPendingKey
    ] ?: false

private fun MutablePreferences.beginVexWidgetUpdateRequestIfNeeded(
    stateChanged: Boolean,
    priorGeneration: Long,
    priorUpdatePending: Boolean,
): Long? {
    if (
        !vexWidgetUpdateRequestRequired(
            stateChanged = stateChanged,
            updatePending =
                priorUpdatePending,
        )
    ) {
        return null
    }

    val generation =
        vexWidgetNextUpdateRequestGeneration(
            priorGeneration,
        )

    this[
        VexWidgetUpdateRequestGenerationKey
    ] =
        generation
    this[
        VexWidgetUpdateRequestPendingKey
    ] =
        true

    return generation
}

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
 * -> structural value equality / duplicate state-write suppression
 * -> durable update-request debt when a Glance update request has not yet succeeded
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
     * Persist one exact surface snapshot to one widget instance.
     *
     * Structural equality suppresses duplicate state writes. A Glance update request is still made
     * when the same durable snapshot carries update-request debt from an earlier failed request.
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
            var persistedChanged =
                false
            var requestGeneration:
                Long? =
                null

            try {
                var transactionChanged =
                    false

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

                    val priorGeneration =
                        preferences
                            .vexWidgetUpdateRequestGeneration()
                    val priorUpdatePending =
                        preferences
                            .vexWidgetUpdateRequestPending()

                    val next =
                        transform(
                            current,
                        )
                    transactionChanged =
                        vexWidgetSurfaceStateChanged(
                            current =
                                current,
                            next =
                                next,
                        )

                    if (
                        transactionChanged
                    ) {
                        stateCodec
                            .write(
                                preferences =
                                    preferences,
                                state =
                                    next,
                            )
                    }

                    requestGeneration =
                        preferences
                            .beginVexWidgetUpdateRequestIfNeeded(
                                stateChanged =
                                    transactionChanged,
                                priorGeneration =
                                    priorGeneration,
                                priorUpdatePending =
                                    priorUpdatePending,
                            )
                }

                persistedChanged =
                    transactionChanged

                if (
                    persistedChanged
                ) {
                    changedCount +=
                        1
                }

                requestGeneration
                    ?.let { generation ->
                        update(
                            appContext,
                            glanceId,
                        )
                        updateRequestedCount +=
                            1

                        settleUpdateRequest(
                            context =
                                appContext,
                            glanceId =
                                glanceId,
                            expectedGeneration =
                                generation,
                        )
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
        var persistedChanged =
            false
        var updateRequested =
            false

        return try {
            var transactionChanged =
                false
            var requestGeneration:
                Long? =
                null

            updateAppWidgetState(
                context,
                glanceId,
            ) { preferences ->
                val current =
                    stateCodec
                        .read(
                            preferences,
                        )
                val priorGeneration =
                    preferences
                        .vexWidgetUpdateRequestGeneration()
                val priorUpdatePending =
                    preferences
                        .vexWidgetUpdateRequestPending()

                transactionChanged =
                    vexWidgetSurfaceStateChanged(
                        current =
                            current,
                        next =
                            nextState,
                    )

                if (
                    transactionChanged
                ) {
                    stateCodec
                        .write(
                            preferences =
                                preferences,
                            state =
                                nextState,
                        )
                }

                requestGeneration =
                    preferences
                        .beginVexWidgetUpdateRequestIfNeeded(
                            stateChanged =
                                transactionChanged,
                            priorGeneration =
                                priorGeneration,
                            priorUpdatePending =
                                priorUpdatePending,
                        )
            }

            persistedChanged =
                transactionChanged

            requestGeneration
                ?.let { generation ->
                    update(
                        context,
                        glanceId,
                    )
                    updateRequested =
                        true

                    settleUpdateRequest(
                        context =
                            context,
                        glanceId =
                            glanceId,
                        expectedGeneration =
                            generation,
                    )
                }

            VexWidgetPublishResult(
                instanceCount = 1,
                changedCount =
                    if (
                        persistedChanged
                    ) {
                        1
                    } else {
                        0
                    },
                updateRequestedCount =
                    if (
                        updateRequested
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
                        persistedChanged
                    ) {
                        1
                    } else {
                        0
                    },
                updateRequestedCount =
                    if (
                        updateRequested
                    ) {
                        1
                    } else {
                        0
                    },
                failedCount = 1,
            )
        }
    }

    /**
     * Clear update-request debt only when this exact request generation is still current.
     *
     * SurfaceState equality is intentionally not used as the occurrence fence: A -> B -> A is a real
     * sequence, so an older A completion must not clear the later A request's debt.
     *
     * Failure to clear is non-authoritative: leaving pending=true causes a later exact-duplicate
     * publication to request another update rather than falsely declaring convergence.
     */
    private suspend fun settleUpdateRequest(
        context: Context,
        glanceId: GlanceId,
        expectedGeneration: Long,
    ) {
        try {
            updateAppWidgetState(
                context,
                glanceId,
            ) { preferences ->
                if (
                    vexWidgetUpdateRequestCanSettle(
                        currentGeneration =
                            preferences
                                .vexWidgetUpdateRequestGeneration(),
                        updatePending =
                            preferences
                                .vexWidgetUpdateRequestPending(),
                        expectedGeneration =
                            expectedGeneration,
                    )
                ) {
                    preferences[
                        VexWidgetUpdateRequestPendingKey
                    ] =
                        false
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Keep pending debt fail-safe; a later duplicate publication will request update again.
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
