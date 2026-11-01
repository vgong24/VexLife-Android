@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KSuspendFunction1
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * Concrete runtime owner for the ThemeMode Family.
 *
 * ThemeMode's semantic Family stays pure. This owner alone connects that meaning to outside truth,
 * active execution custody, and chronological observation.
 *
 * ThemeMode has exactly one maintained runtime relationship today: Control.Select. The selected
 * Light/Dark value is operation payload, not execution identity.
 */
internal class ThemeModeRuntime(
    val family:
        ThemeModeFamily,
    private val store:
        ThemeModeStore,
    executionRoot:
        VexExecutionRoot,
    private val logger:
        VexLogger.Sink =
        VexLogger.Sink.None,
) {
    private val execution =
        ThemeModeExecution(
            executionRoot,
        )

    init {
        execution.observeStore(
            source =
                store::states,
            onEach =
                ::observe,
        )
    }

    fun activate(
        control:
            ThemeModeFamily.Control,
    ):
        Boolean {
        val before =
            family.currentState
        val act =
            family.activate(
                control,
            )
                ?: return false

        record(
            VexLogger.Event.Family.ControlAccepted(
                family =
                    family::class,
                control =
                    control::class,
                currentState =
                    before::class,
            ),
        )

        val after =
            family.currentState
        if (
            before !=
            after
        ) {
            recordStateChange(
                before =
                    before,
                after =
                    after,
            )
        }

        act.effect
            ?.let { effect ->
                realize(
                    control =
                        control,
                    effect =
                        effect,
                )
            }

        return true
    }

    /**
     * NotSet remains valid Store truth but is not effective ThemeMode presentation State.
     */
    private fun observe(
        state:
            ThemeMode,
    ) {
        if (
            state ==
            ThemeMode.NotSet
        ) {
            return
        }

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
            recordStateChange(
                before =
                    before,
                after =
                    after,
            )
        }
    }

    private fun realize(
        control:
            ThemeModeFamily.Control,
        effect:
            ThemeModeFamily.Effect,
    ) {
        when (effect) {
            is ThemeModeFamily.Effect.Save ->
                saveLatest(
                    control =
                        control,
                    effect =
                        effect,
                )
        }
    }

    private fun saveLatest(
        control:
            ThemeModeFamily.Control,
        effect:
            ThemeModeFamily.Effect.Save,
    ) {
        val operation =
            store::save
        val operationState =
            family.currentState::class

        execution.saveLatest(
            control =
                control,
            operation =
                operation,
            mode =
                effect.mode,
            beforeOperation = {
                record(
                    VexLogger.Event.Family.OperationBegan(
                        family =
                            family::class,
                        operation =
                            operation,
                        state =
                            operationState,
                    ),
                )
            },
            onResult = { result ->
                when (result) {
                    is ThemeModeStore.Result.Success -> {
                        observe(
                            result.observedState,
                        )
                        record(
                            VexLogger.Event.Family.OperationSuccess(
                                family =
                                    family::class,
                                operation =
                                    operation,
                                state =
                                    operationState,
                            ),
                        )
                    }

                    is ThemeModeStore.Result.Failure ->
                        record(
                            VexLogger.Event.Family.OperationFailure(
                                family =
                                    family::class,
                                operation =
                                    operation,
                                state =
                                    operationState,
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
                        family =
                            family::class,
                        operation =
                            operation,
                        state =
                            operationState,
                        failure =
                            failure::class,
                    ),
                )
            },
        )
    }

    private fun recordStateChange(
        before:
            ThemeMode,
        after:
            ThemeMode,
    ) {
        record(
            VexLogger.Event.Family.StateAdmitted(
                family =
                    family::class,
                fromState =
                    before::class,
                toState =
                    after::class,
            ),
        )
    }

    private fun record(
        event:
            VexLogger.Event,
    ) {
        try {
            logger.record(
                event,
            )
        } catch (_: Throwable) {
            // Observation failure is never ThemeMode semantic/runtime authority.
        }
    }

    private class ThemeModeExecution(
        root:
            VexExecutionRoot,
    ) :
        VexExecution(
            root,
        ) {
        /**
         * Private execution address mechanically derived from the already-authored Control.
         *
         * The enclosing VexExecution object supplies the live runtime-owner scope. Control kind/name
         * supplies the semantic relationship distinction. Ordinary Control payload does not
         * participate.
         */
        private data class ControlRelationshipId(
            val controlType:
                KClass<
                    out ThemeModeFamily.Control
                >,
            val controlName:
                String,
        ) :
            RelationshipId

        private fun relationshipId(
            control:
                ThemeModeFamily.Control,
        ):
            RelationshipId {
            val controlType =
                control::class
            val controlName =
                (
                    control as?
                        Enum<*>
                )
                    ?.name
                    ?: requireNotNull(
                        controlType.simpleName,
                    ) {
                        "ThemeMode Control requires a named class or enum entry"
                    }

            return ControlRelationshipId(
                controlType =
                    controlType,
                controlName =
                    controlName,
            )
        }

        fun saveLatest(
            control:
                ThemeModeFamily.Control,
            operation:
                KSuspendFunction1<
                    ThemeMode,
                    ThemeModeStore.Result
                >,
            mode:
                ThemeMode,
            beforeOperation:
                () -> Unit,
            onResult:
                (ThemeModeStore.Result) -> Unit,
            onFailure:
                (Throwable) -> Unit,
        ):
            Job =
            beginLatest(
                relationshipId =
                    relationshipId(
                        control,
                    ),
                operation =
                    operation,
                argument =
                    mode,
                beforeOperation =
                    beforeOperation,
                onResult =
                    onResult,
                onFailure =
                    onFailure,
            )

        fun observeStore(
            source:
                KProperty0<
                    Flow<ThemeMode>
                >,
            onEach:
                suspend (ThemeMode) -> Unit,
        ) {
            launchForCollect(
                source =
                    source,
                onEach =
                    onEach,
            )
        }
    }
}

// [VXG RealForever]
