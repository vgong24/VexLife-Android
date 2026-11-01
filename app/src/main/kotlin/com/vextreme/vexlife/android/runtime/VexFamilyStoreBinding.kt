@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexStore
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlin.reflect.KFunction0
import kotlin.reflect.KSuspendFunction1
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * Architecture-owned runtime binding between one exact semantic Family and its outside-truth Store.
 *
 * This removes the need for every feature to invent a <Feature>Connection merely to repeat the same
 * Control -> Act -> Effect -> Store -> observed State plumbing.
 *
 * The binding intentionally does not discover Stores by reflection/name/package convention. The
 * composition root supplies the exact typed Family and Store once; standard Execution/Logger
 * mechanics are then derived mechanically.
 *
 * Runtime policy is explicit at construction: overlap approach, typed execution relationship key,
 * and outside-State admission are all required. ThemeMode uses Control kind as its key; another
 * Family may supply a typed composite key such as Control kind + ResourceId without changing this
 * binding or inventing another feature-specific runtime layer.
 */
internal class VexFamilyStoreBinding<
    Family,
    FamilyState,
    FamilyControl,
    FamilyEffect,
    RelationshipCoordinate : Any,
>(
    val family:
        Family,
    private val store:
        VexStore<
            Family,
            FamilyState,
            FamilyControl,
            FamilyEffect
        >,
    executionRoot:
        VexExecutionRoot,
    private val logger:
        VexLogger.Sink =
        VexLogger.Sink.None,
    private val approach:
        VexExecution.Approach,
    private val relationshipCoordinate:
        (FamilyControl) -> RelationshipCoordinate,
    private val admitObservation:
        (FamilyState) -> Boolean,
) where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl,
        FamilyEffect
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family>,
    FamilyEffect : VexFamily.Effect<Family> {
    private val execution =
        StoreExecution<
            RelationshipCoordinate
        >(
            executionRoot,
        )

    private val observedStates =
        store.states

    init {
        if (
            observedStates !=
            null
        ) {
            execution.observeStore(
                source =
                    ::requiredObservedStates,
                onEach =
                    ::observe,
            )
        }
    }

    /**
     * Admit semantic meaning first, then realize any returned Effect through Store.
     */
    fun activate(
        control:
            FamilyControl,
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

    private fun requiredObservedStates():
        Flow<FamilyState> =
        requireNotNull(
            observedStates,
        )

    private fun observe(
        state:
            FamilyState,
    ) {
        if (
            !admitObservation(
                state,
            )
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
            FamilyControl,
        effect:
            FamilyEffect,
    ) {
        val operation =
            store::realize
        val operationState =
            family.currentState::class

        execution.realize(
            relationshipCoordinate =
                relationshipCoordinate(
                    control,
                ),
            approach =
                approach,
            operation =
                operation,
            effect =
                effect,
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
                    is VexStore.Result.Success -> {
                        result.observedState
                            ?.let(
                                ::observe,
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

                    is VexStore.Result.Failure ->
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
            FamilyState,
        after:
            FamilyState,
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
            // Observation failure is never semantic/runtime authority.
        }
    }

    private class StoreExecution<
        RelationshipCoordinate : Any,
    >(
        root:
            VexExecutionRoot,
    ) :
        VexExecution(
            root,
        ) {
        private data class StoreRelationshipId<
            RelationshipCoordinate : Any,
        >(
            val key:
                RelationshipCoordinate,
        ) :
            RelationshipId

        fun <Effect, State> realize(
            relationshipCoordinate:
                RelationshipCoordinate,
            approach:
                Approach,
            operation:
                KSuspendFunction1<
                    Effect,
                    VexStore.Result<State>
                >,
            effect:
                Effect,
            beforeOperation:
                () -> Unit,
            onResult:
                (VexStore.Result<State>) -> Unit,
            onFailure:
                (Throwable) -> Unit,
        ):
            Job {
            val relationshipId =
                StoreRelationshipId(
                    key =
                        relationshipCoordinate,
                )

            return when (approach) {
                Approach.Independent ->
                    begin(
                        operation =
                            operation,
                        argument =
                            effect,
                        beforeOperation =
                            beforeOperation,
                        onResult =
                            onResult,
                        onFailure =
                            onFailure,
                    )

                Approach.Earliest ->
                    beginEarliest(
                        relationshipId =
                            relationshipId,
                        operation =
                            operation,
                        argument =
                            effect,
                        beforeOperation =
                            beforeOperation,
                        onResult =
                            onResult,
                        onFailure =
                            onFailure,
                    )

                Approach.Latest ->
                    beginLatest(
                        relationshipId =
                            relationshipId,
                        operation =
                            operation,
                        argument =
                            effect,
                        beforeOperation =
                            beforeOperation,
                        onResult =
                            onResult,
                        onFailure =
                            onFailure,
                    )

                Approach.Queued ->
                    beginQueued(
                        relationshipId =
                            relationshipId,
                        operation =
                            operation,
                        argument =
                            effect,
                        beforeOperation =
                            beforeOperation,
                        onResult =
                            onResult,
                        onFailure =
                            onFailure,
                    )
            }
        }

        fun <State> observeStore(
            source:
                KFunction0<
                    Flow<State>
                >,
            onEach:
                suspend (State) -> Unit,
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


/**
 * One typed dependency declaration for the standard Family <-> Store runtime relationship.
 *
 * The feature declares exact Family + Store providers once. Standard ExecutionRoot + Logger
 * prerequisites and construction of [VexFamilyStoreBinding] are owned here rather than repeated in
 * every feature module.
 */
internal fun <
    Family,
    FamilyState,
    FamilyControl,
    FamilyEffect,
    Store,
    RelationshipCoordinate : Any,
> vexFamilyStoreBindingProvider(
    readableName:
        String,
    family:
        VexDependencyProvider<Family>,
    store:
        VexDependencyProvider<Store>,
    executionRoot:
        VexDependencyProvider<VexExecutionRoot>,
    logger:
        VexDependencyProvider<VexLogger>,
    approach:
        VexExecution.Approach,
    relationshipCoordinate:
        (FamilyControl) -> RelationshipCoordinate,
    admitObservation:
        (FamilyState) -> Boolean,
):
    VexDependencyProvider<
        VexFamilyStoreBinding<
            Family,
            FamilyState,
            FamilyControl,
            FamilyEffect,
            RelationshipCoordinate
        >
    > where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl,
        FamilyEffect
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family>,
    FamilyEffect : VexFamily.Effect<Family>,
    Store : VexStore<
        Family,
        FamilyState,
        FamilyControl,
        FamilyEffect
    > =
    VexDependencyProvider.named(
        readableName =
            readableName,
        dependencies =
            listOf(
                family,
                store,
                executionRoot,
                logger,
            ),
    ) { dependencies: VexDependencyBuildScope ->
        VexFamilyStoreBinding(
            family =
                dependencies.require(
                    family,
                ),
            store =
                dependencies.require(
                    store,
                ),
            executionRoot =
                dependencies.require(
                    executionRoot,
                ),
            logger =
                dependencies.require(
                    logger,
                )
                    .sink,
            approach =
                approach,
            relationshipCoordinate =
                relationshipCoordinate,
            admitObservation =
                admitObservation,
        )
    }

// [VXG RealForever]
