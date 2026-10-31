@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlin.reflect.KClass

/**
 * Runtime/persistence boundary around the pure [ThemeModeFamily].
 *
 * Construction owns the live objects. Family owns semantic State / Control / Act / Effect.
 * This Connection owns realization of ThemeMode runtime work and reconciliation of Store truth back
 * into Family. Neither Store, Execution nor Logger is injected into the semantic Family.
 */
internal class ThemeModeConnection(
    val family: ThemeModeFamily,
    private val executionRoot: VexExecutionRoot,
    private val store: ThemeModeStore? = null,
    private val logger: VexLogger.Sink = VexLogger.Sink.None,
) {
    /**
     * ThemeMode currently has one runtime relationship kind: Select.
     *
     * The identity is derived from the already-authored Control kind and this live Connection owner.
     * No payload value, SourceRef, InstanceId, TraceId or handwritten String key participates.
     */
    private data class RelationshipId(
        val owner: ThemeModeConnection,
        val controlType: KClass<out ThemeModeFamily.Control>,
    ) :
        VexExecution.RelationshipId

    init {
        store
            ?.let { stateStore ->
                VexExecution.observe(
                    executionRoot = executionRoot,
                    owner = this::class,
                    source = stateStore::observableState,
                ) { storedState ->
                    when (storedState) {
                        ThemeMode.NotSet ->
                            Unit

                        ThemeMode.Light,
                        ThemeMode.Dark ->
                            observe(
                                storedState,
                            )
                    }
                }
            }
    }

    /**
     * Accept one semantic Control, then realize the Effect returned by the Family.
     *
     * Semantic State admission happens synchronously inside Family. Runtime work follows afterward.
     */
    fun activate(
        control: ThemeModeFamily.Control,
    ): Boolean {
        val before =
            family.currentState
        val act =
            family.activate(
                control,
            )
                ?: return false

        record(
            VexLogger.Event.Family.ControlAccepted(
                family = family::class,
                control = control::class,
                currentState = before::class,
            ),
        )

        val after =
            family.currentState
        if (
            before !=
            after
        ) {
            record(
                VexLogger.Event.Family.StateAdmitted(
                    family = family::class,
                    fromState = before::class,
                    toState = after::class,
                ),
            )
        }

        act.effect
            ?.let { effect ->
                when (effect) {
                    is ThemeModeFamily.Effect.Save ->
                        save(
                            control = control,
                            effect = effect,
                        )
                }
            }

        return true
    }

    private fun observe(
        state: ThemeMode,
    ) {
        val before =
            family.currentState

        family.observe(
            state,
        )

        val after =
            family.currentState
        if (
            before !=
            after
        ) {
            record(
                VexLogger.Event.Family.StateAdmitted(
                    family = family::class,
                    fromState = before::class,
                    toState = after::class,
                ),
            )
        }
    }

    private fun save(
        control: ThemeModeFamily.Control,
        effect: ThemeModeFamily.Effect.Save,
    ) {
        val stateStore =
            store
                ?: return
        val operation =
            stateStore::save

        VexExecution.beginLatest(
            executionRoot = executionRoot,
            owner = this::class,
            relationshipId =
                RelationshipId(
                    owner = this,
                    controlType = control::class,
                ),
            operation = operation,
            argument = effect.mode,
            beforeOperation = {
                record(
                    VexLogger.Event.Family.OperationBegan(
                        family = family::class,
                        operation = operation,
                        state = effect.mode::class,
                    ),
                )
            },
            onResult = { result ->
                when (result) {
                    ThemeModeStore.WriteResult.Success ->
                        record(
                            VexLogger.Event.Family.OperationSuccess(
                                family = family::class,
                                operation = operation,
                                state = effect.mode::class,
                            ),
                        )

                    is ThemeModeStore.WriteResult.Failure ->
                        record(
                            VexLogger.Event.Family.OperationFailure(
                                family = family::class,
                                operation = operation,
                                state = effect.mode::class,
                                failure =
                                    result.cause
                                        ?.let { cause ->
                                            cause::class
                                        },
                            ),
                        )
                }
            },
            onFailure = { failure ->
                record(
                    VexLogger.Event.Family.OperationFailure(
                        family = family::class,
                        operation = operation,
                        state = effect.mode::class,
                        failure = failure::class,
                    ),
                )
            },
        )
    }

    private fun record(
        event: VexLogger.Event,
    ) {
        try {
            logger.record(
                event,
            )
        } catch (_: Throwable) {
            // Observation failure is not ThemeMode semantic/runtime authority.
        }
    }
}

// [VXG RealForever]
