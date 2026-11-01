@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlin.reflect.KClass
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
            root =
                executionRoot,
            store =
                store,
        )

    init {
        execution.observeStore(
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
            VexLogger.Event.Semantic(
                subjectRef =
                    familySubjectRef,
                action =
                    "ControlAccepted",
                detailsJson =
                    controlAcceptedDetails(
                        control =
                            control,
                        currentState =
                            before,
                    ),
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
        execution.saveLatest(
            control =
                control,
            mode =
                effect.mode,
            beforeOperation = {
                record(
                    VexLogger.Event.Runtime(
                        subjectRef =
                            familySubjectRef,
                        action =
                            "EffectRequested",
                        detailsJson =
                            saveEffectDetails(
                                effect,
                            ),
                    ),
                )
            },
            onResult = { result ->
                when (result) {
                    is ThemeModeStore.Result.Success -> {
                        record(
                            VexLogger.Event.Runtime(
                                subjectRef =
                                    familySubjectRef,
                                action =
                                    "EffectSucceeded",
                                detailsJson =
                                    saveEffectDetails(
                                        effect,
                                    ),
                            ),
                        )
                        observe(
                            result.observedState,
                        )
                    }

                    is ThemeModeStore.Result.Failure ->
                        record(
                            VexLogger.Event.Runtime(
                                subjectRef =
                                    familySubjectRef,
                                action =
                                    "EffectFailed",
                                detailsJson =
                                    saveEffectDetails(
                                        effect =
                                            effect,
                                        failure =
                                            result.cause,
                                    ),
                            ),
                        )
                }
            },
            onFailure = { failure ->
                record(
                    VexLogger.Event.Runtime(
                        subjectRef =
                            familySubjectRef,
                        action =
                            "EffectFailed",
                        detailsJson =
                            saveEffectDetails(
                                effect =
                                    effect,
                                failure =
                                    failure,
                            ),
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
            VexLogger.Event.Semantic(
                subjectRef =
                    familySubjectRef,
                action =
                    "StateAdmitted",
                detailsJson =
                    stateAdmittedDetails(
                        before =
                            before,
                        after =
                            after,
                    ),
            ),
        )
    }

    private val familySubjectRef:
        String =
        "family." +
            requireNotNull(
                family::class.qualifiedName
                    ?: family::class.simpleName,
            )

    private fun controlAcceptedDetails(
        control:
            ThemeModeFamily.Control,
        currentState:
            ThemeMode,
    ):
        String =
        when (control) {
            is ThemeModeFamily.Control.Select ->
                "{\"control\":\"Select\",\"mode\":\"" +
                    control.mode.name +
                    "\",\"currentState\":\"" +
                    currentState.name +
                    "\"}"
        }

    private fun stateAdmittedDetails(
        before:
            ThemeMode,
        after:
            ThemeMode,
    ):
        String =
        "{\"fromState\":\"" +
            before.name +
            "\",\"toState\":\"" +
            after.name +
            "\"}"

    private fun saveEffectDetails(
        effect:
            ThemeModeFamily.Effect.Save,
        failure:
            Throwable? =
            null,
    ):
        String =
        buildString {
            append(
                "{\"effect\":\"Save\",\"mode\":\"",
            )
            append(
                effect.mode.name,
            )
            append(
                "\"",
            )
            failure
                ?.let { cause ->
                    append(
                        ",\"failure\":\"",
                    )
                    append(
                        cause::class.qualifiedName
                            ?: cause::class.simpleName
                            ?: "Throwable",
                    )
                    append(
                        "\"",
                    )
                }
            append(
                "}",
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
        private val store:
            ThemeModeStore,
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
                    store::save,
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
            onEach:
                suspend (ThemeMode) -> Unit,
        ) {
            launchForCollect(
                source =
                    store::states,
                onEach =
                    onEach,
            )
        }
    }
}

// [VXG RealForever]
