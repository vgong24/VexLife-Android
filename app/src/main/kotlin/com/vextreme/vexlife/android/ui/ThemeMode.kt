@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger

/**
 * Android realization of the ThemeMode semantic State domain.
 *
 * ThemeMode is ordinary Family State. It does not encode initialization/default policy,
 * presentation form, persistence, or platform-appearance reconciliation.
 *
 * [NotSet] preserves the explicit distinction that no effective ThemeMode has yet been resolved.
 * It is not null, not persistence failure, and not silently equivalent to [Light] or [Dark].
 */
internal enum class ThemeMode :
    VexFamily.State<ThemeModeFamily> {
    NotSet,
    Light,
    Dark,
}

/**
 * Smallest current ThemeMode Family earned by the Debug Menu patient zero.
 *
 * ThemeMode values are peer semantic choices. The Family accepts an explicit Select request rather
 * than making today's two-state Switch the semantic contract. A target may realize Light/Dark with
 * a toggle, segmented control, menu, or another presentation without changing this relationship.
 *
 * The selected mode is one Family Parameter.Mode value carried intact through Control.Select and
 * Effect.Save into the bound Act.Method.Save. The Act authors Save as Latest because overlap is part
 * of the Select consequence relationship; VexExecution later realizes that authored consequence
 * without reconstructing the mode, Approach, RelationshipId, or live Family execution owner.
 */
internal class ThemeModeFamily(
    initialState:
        ThemeMode,
) :
    VexFamily<
        ThemeModeFamily,
        ThemeMode,
        ThemeModeFamily.Control,
        ThemeModeFamily.Effect
    >(
        initialState =
            initialState,
    ) {


    sealed interface Parameter :
        VexFamily.Parameter<ThemeModeFamily> {
        data class Mode(
            val mode:
                ThemeMode,
        ) :
            Parameter
    }

    /*
     * Currentness fence for Store observations while one accepted Select consequence is still
     * settling.
     *
     * This is the SAME Family Parameter.Mode instance already carried through Control -> Effect ->
     * Method. No generation/trace/operation ID is minted. The marker exists only so an older Store
     * observation cannot overwrite a newer accepted semantic selection while that exact Method is
     * still unresolved.
     *
     * act(...) remains pure. The realization boundary enters/leaves this fence through
     * activateSelection(...) / selectionRealizationFinished(...).
     */
    private val selectionObservationLock =
        Any()
    private var unsettledSelection:
        Parameter.Mode? =
        null

    internal fun activateSelection(
        control:
            Control.Select,
    ):
        VexFamily.Act<
            ThemeModeFamily,
            ThemeMode,
            Effect
        >? =
        synchronized(
            selectionObservationLock,
        ) {
            val previous =
                unsettledSelection
            unsettledSelection =
                control.mode

            activate(
                control,
            ).also { accepted ->
                if (
                    accepted ==
                    null
                ) {
                    unsettledSelection =
                        previous
                }
            }
        }

    internal fun selectionRealizationFinished(
        parameter:
            Parameter.Mode,
    ) {
        synchronized(
            selectionObservationLock,
        ) {
            if (
                unsettledSelection ===
                parameter
            ) {
                unsettledSelection =
                    null
            }
        }
    }

    sealed interface Control :
        VexFamily.Control<ThemeModeFamily> {
        data class Select(
            val mode:
                Parameter.Mode,
        ) :
            Control {
            init {
                require(
                    mode.mode !=
                        ThemeMode.NotSet,
                ) {
                    "ThemeMode.NotSet is unresolved state, not a selectable mode"
                }
            }
        }
    }

    sealed interface Effect :
        VexFamily.Effect<ThemeModeFamily> {
        /**
         * Save the explicit semantic selection durably.
         *
         * This requests Save; it does not claim Save success or another ThemeMode State. The exact
         * Family Parameter is carried forward rather than reconstructed from a primitive later.
         */
        data class Save(
            val mode:
                Parameter.Mode,
        ) :
            Effect
    }

    /**
     * Executable continuation vocabulary belonging to ThemeMode Acts.
     *
     * Mode is Family-owned semantic input. Save is the actual executable operation. A Save Method is
     * fully bound to the concrete Store and the same Parameter.Mode instance carried by the accepted
     * Act consequence; invoke() therefore needs no loose argument or reconstruction step.
     */
    object Act {
        sealed class Method<
            out SuccessOutput,
            out FailureOutput,
        > :
            VexFamily.Act.Method<
                ThemeModeFamily,
                SuccessOutput,
                FailureOutput
            > {
            class Save(
                private val store:
                    ThemeModeStore,
                override val effect:
                    Effect.Save,
            ) :
                Method<
                    ThemeMode,
                    Unit
                >() {
                override suspend operator fun invoke():
                    VexMethod.Result<
                        ThemeMode,
                        Unit
                    > =
                    if (
                        store.save(
                            effect.mode.mode,
                        )
                    ) {
                        VexMethod.Result.Success(
                            output =
                                effect.mode.mode,
                        )
                    } else {
                        VexMethod.Result.Failure(
                            output =
                                Unit,
                        )
                    }
            }
        }
    }

    sealed interface DisplayState :
        VexFamily.DisplayState<
            ThemeModeFamily,
            ThemeMode
        > {
        val presentation:
            VexThemePresentation

        data object Unresolved :
            DisplayState {
            override val presentation =
                VexThemePresentation.Unresolved
        }

        data object Light :
            DisplayState {
            override val presentation =
                VexThemePresentation.Light
        }

        data object Dark :
            DisplayState {
            override val presentation =
                VexThemePresentation.Dark
        }
    }

    override fun displayStateFor(
        state:
            ThemeMode,
    ):
        DisplayState =
        when (state) {
            ThemeMode.NotSet ->
                DisplayState.Unresolved

            ThemeMode.Light ->
                DisplayState.Light

            ThemeMode.Dark ->
                DisplayState.Dark
        }

    override fun projectionFor(
        state:
            ThemeMode,
    ):
        VexFamily.Projection<
            ThemeModeFamily,
            ThemeMode,
            DisplayState
        > =
        VexFamily.Projection(
            state =
                state,
            displayState =
                displayStateFor(
                    state,
                ),
        )

    override val currentProjection:
        VexFamily.Projection<
            ThemeModeFamily,
            ThemeMode,
            DisplayState
        >
        get() =
            projectionFor(
                currentState,
            )

    override val displayState:
        DisplayState
        get() =
            currentProjection
                .displayState

    /**
     * Reconcile already-observed durable ThemeMode truth into the one Family State holder.
     *
     * NotSet means no explicit durable selection and therefore does not replace the currently
     * effective ThemeMode. This method owns that ThemeMode-specific interpretation; observation
     * lifetime belongs to VexExecution.
     */
    fun observeStoredThemeMode(
        observed:
            ThemeMode,
    ):
        ThemeMode =
        synchronized(
            selectionObservationLock,
        ) {
            if (
                observed ==
                ThemeMode.NotSet
            ) {
                return@synchronized currentState
            }

            val unsettled =
                unsettledSelection
            if (
                unsettled !=
                null &&
                observed !=
                unsettled.mode
            ) {
                /*
                 * A newer accepted Select is still settling. This observation may have been emitted
                 * by the prior Store state/save and carries no revision that proves it is newer than
                 * the accepted Parameter, so it cannot overwrite the newer semantic State.
                 */
                return@synchronized currentState
            }

            observe(
                observed,
            )
        }

    override fun act(
        currentState:
            ThemeMode,
        control:
            Control,
    ):
        VexFamily.Act<
            ThemeModeFamily,
            ThemeMode,
            Effect
        >? =
        when (control) {
            is Control.Select -> {
                val effect =
                    Effect.Save(
                        mode =
                            control.mode,
                    )

                when (currentState) {
                    ThemeMode.NotSet ->
                        admit(
                            control.mode.mode,
                            begin =
                                beginLatest(
                                    effect,
                                ),
                        )

                    ThemeMode.Light,
                    ThemeMode.Dark ->
                        if (
                            currentState ==
                            control.mode.mode
                        ) {
                            /*
                             * Selecting the already-current peer still means "save this explicit choice durably";
                             * same State does not erase the Effect.
                             */
                            stay(
                                beginLatest(
                                    effect,
                                ),
                            )
                        } else {
                            admit(
                                control.mode.mode,
                                begin =
                                    beginLatest(
                                        effect,
                                    ),
                            )
                        }
                }
            }
        }

}

/**
 * Realize one accepted ThemeMode Act from its already-authored relationship.
 *
 * ThemeModeFamily already authored the semantic Effect and its execution Approach. This boundary
 * binds the exact Effect to the concrete Store Method and entrusts that accepted relationship to
 * VexExecution custody.
 *
 * A successful Method Result carries the Store's physical-commit fact into ThemeMode-owned
 * reconciliation. The maintained Store Flow is a separate observation route. They may converge on
 * the same semantic ThemeMode truth only after each route has independently earned that fact;
 * Result and Observation are not interchangeable evidence.
 */
internal fun activateThemeMode(
    family:
        ThemeModeFamily,
    control:
        ThemeModeFamily.Control,
    store:
        ThemeModeStore,
    executionRoot:
        VexExecutionRoot,
    logger:
        VexLogger,
):
    Boolean {
    val before =
        family.currentState
    val act =
        when (
            control
        ) {
            is ThemeModeFamily.Control.Select ->
                family.activateSelection(
                    control,
                )
        }
            ?: return false

    recordThemeModeFact(
        logger =
            logger,
        event =
            VexLogger.Event.Semantic(
                subjectRef =
                    THEME_MODE_FAMILY_SUBJECT,
                action =
                    "ControlAccepted",
                detailsJson =
                    themeModeControlDetails(
                        control =
                            control,
                        currentState =
                            before,
                    ),
            ),
    )

    val admitted =
        family.currentState
    if (
        admitted !=
        before
    ) {
        recordThemeModeStateChange(
            logger =
                logger,
            before =
                before,
            after =
                admitted,
        )
    }

    val begin =
        act.begin
            ?: return true

    when (
        val effect =
            begin.effect
    ) {
        is ThemeModeFamily.Effect.Save -> {
            val method =
                ThemeModeFamily.Act.Method.Save(
                    store =
                        store,
                    effect =
                        effect,
                )

            act.launch(
                executionRoot =
                    executionRoot,
                method =
                    method,
                beforeOperation = {
                        recordThemeModeFact(
                            logger =
                                logger,
                            event =
                                VexLogger.Event.Runtime(
                                    subjectRef =
                                        THEME_MODE_FAMILY_SUBJECT,
                                    action =
                                        "EffectRequested",
                                    detailsJson =
                                        themeModeSaveDetails(
                                            effect,
                                        ),
                                ),
                        )
                    },
                    onResult = { result ->
                        when (
                            result
                        ) {
                            is VexMethod.Result.Success -> {
                                family.selectionRealizationFinished(
                                    effect.mode,
                                )
                                recordThemeModeFact(
                                    logger =
                                        logger,
                                    event =
                                        VexLogger.Event.Runtime(
                                            subjectRef =
                                                THEME_MODE_FAMILY_SUBJECT,
                                            action =
                                                "EffectSucceeded",
                                            detailsJson =
                                                themeModeSaveDetails(
                                                    effect,
                                                ),
                                        ),
                                )
                                observeThemeModeState(
                                    family =
                                        family,
                                    observed =
                                        result.output,
                                    logger =
                                        logger,
                                )
                            }

                            is VexMethod.Result.Failure -> {
                                family.selectionRealizationFinished(
                                    effect.mode,
                                )
                                recordThemeModeFact(
                                    logger =
                                        logger,
                                    event =
                                        VexLogger.Event.Runtime(
                                            subjectRef =
                                                THEME_MODE_FAMILY_SUBJECT,
                                            action =
                                                "EffectFailed",
                                            detailsJson =
                                                themeModeSaveDetails(
                                                    effect =
                                                        effect,
                                                ),
                                        ),
                                )
                            }

                            is VexMethod.Result.Error -> {
                                family.selectionRealizationFinished(
                                    effect.mode,
                                )
                                recordThemeModeFact(
                                    logger =
                                        logger,
                                    event =
                                        VexLogger.Event.Runtime(
                                            subjectRef =
                                                THEME_MODE_FAMILY_SUBJECT,
                                            action =
                                                "EffectErrored",
                                            detailsJson =
                                                themeModeSaveDetails(
                                                    effect =
                                                        effect,
                                                    failure =
                                                        result.cause,
                                                ),
                                        ),
                                )
                            }
                        }
                },
            )
        }
    }

    return true
}

/**
 * Maintain the exact Store source observation under the same live Family execution scope.
 *
 * The source reference is appropriate here because the question is exactly "is this Store source
 * already observed?". Observation custody adds no second semantic owner or feature execution
 * vocabulary.
 */
internal fun observeThemeModeStore(
    family:
        ThemeModeFamily,
    store:
        ThemeModeStore,
    executionRoot:
        VexExecutionRoot,
    logger:
        VexLogger,
) {
    VexExecution.observe(
        executionRoot =
            executionRoot,
        owner =
            family,
        source =
            store::states,
    ) { observed ->
        observeThemeModeState(
            family =
                family,
            observed =
                observed,
            logger =
                logger,
        )
    }

}

private fun observeThemeModeState(
    family:
        ThemeModeFamily,
    observed:
        ThemeMode,
    logger:
        VexLogger,
) {
    val before =
        family.currentState
    val after =
        family.observeStoredThemeMode(
            observed,
        )

    if (
        after !=
        before
    ) {
        recordThemeModeStateChange(
            logger =
                logger,
            before =
                before,
            after =
                after,
        )
    }
}

private fun recordThemeModeStateChange(
    logger:
        VexLogger,
    before:
        ThemeMode,
    after:
        ThemeMode,
) {
    recordThemeModeFact(
        logger =
            logger,
        event =
            VexLogger.Event.Semantic(
                subjectRef =
                    THEME_MODE_FAMILY_SUBJECT,
                action =
                    "StateAdmitted",
                detailsJson =
                    "{\"fromState\":\"" +
                        before.name +
                        "\",\"toState\":\"" +
                        after.name +
                        "\"}",
            ),
    )
}

private fun themeModeControlDetails(
    control:
        ThemeModeFamily.Control,
    currentState:
        ThemeMode,
):
    String =
    when (
        control
    ) {
        is ThemeModeFamily.Control.Select ->
            "{\"control\":\"Select\",\"mode\":\"" +
                control.mode.mode.name +
                "\",\"currentState\":\"" +
                currentState.name +
                "\"}"
    }

private fun themeModeSaveDetails(
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
            effect.mode.mode.name,
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

private fun recordThemeModeFact(
    logger:
        VexLogger,
    event:
        VexLogger.Event,
) {
    try {
        logger.sink.record(
            event,
        )
    } catch (_: Throwable) {
        // Observation failure never becomes ThemeMode semantic/runtime authority.
    }
}

private const val THEME_MODE_FAMILY_SUBJECT =
    "family.ThemeModeFamily"


// [VXG RealForever]
