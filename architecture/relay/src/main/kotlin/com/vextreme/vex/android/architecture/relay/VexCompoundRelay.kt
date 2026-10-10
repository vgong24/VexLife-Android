package com.vextreme.vex.android.architecture.relay

import com.vextreme.vex.android.architecture.execution.VexCoroutineHandle
import com.vextreme.vex.android.architecture.execution.VexExecutionOwner
import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogRef
import com.vextreme.vex.android.architecture.observability.VexLogSink
import com.vextreme.vex.android.architecture.observability.logSafely
import java.util.Collections
import java.util.IdentityHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class VexCompoundActivationState {
    INACTIVE,
    ACTIVATING,
    ACTIVE,
    DEACTIVATING,
}

/**
 * Relay for one organized component relationship with a separate input family and output family.
 *
 * Construction is inert. Dependency observation begins only after explicit [activate].
 */
abstract class VexCompoundRelay<
    InputState : VexInputState<InputState>,
    OutputState : VexOutputState<OutputState>,
>(
    override val relayRef: VexRelayRef,
    initialInputState: InputState,
    initialOutputState: OutputState,
    private val logSink: VexLogSink = VexLogSink.None,
) : VexObservableRelay<VexCompoundState<InputState, OutputState>> {
    private val stateLock = Any()
    private var processing = false
    private var transitionSequence = 0L

    private val activationMutex = Mutex()
    private var dependencyHandles: List<VexCoroutineHandle> =
        emptyList()
    private var leasedCompoundDependencies: List<VexCompoundRelay<*, *>> =
        emptyList()

    private val activeDependentRelays =
        Collections.newSetFromMap(
            IdentityHashMap<VexCompoundRelay<*, *>, Boolean>(),
        )

    private val mutableActivationState =
        MutableStateFlow(
            VexCompoundActivationState.INACTIVE,
        )

    val activationState: StateFlow<VexCompoundActivationState> =
        mutableActivationState.asStateFlow()

    private val mutableCurrentState =
        MutableStateFlow(
            VexCompoundState(
                inputState = initialInputState.deepCopy(),
                outputState = initialOutputState.deepCopy(),
            ),
        )

    override val currentState: StateFlow<VexCompoundState<InputState, OutputState>> =
        DefensiveVexStateFlow(mutableCurrentState.asStateFlow())

    /**
     * Declares this instance's stable child-Relay topology.
     *
     * The declaration is evaluated lazily once, after subclass construction, and then retained for
     * this Compound Relay's lifetime. Reactivation restarts observation of the same relationship; it
     * does not silently replace the relationship graph.
     */
    protected abstract fun relayDependencies(): List<VexRelayDependency<InputState>>

    private val declaredDependencies: List<VexRelayDependency<InputState>> by lazy {
        relayDependencies().toList()
    }

    /**
     * Bounded relationship derivation only.
     *
     * Do not dispatch effects or imperatively mutate child/peer Relays here. Declared dependencies
     * own incoming relationships; hidden write edges would bypass topology/cycle validation.
     */
    protected abstract fun processStates(
        inputState: InputState,
        currentState: VexCompoundState<InputState, OutputState>,
    ): VexCompoundState<InputState, OutputState>

    suspend fun activate(
        executionOwner: VexExecutionOwner,
    ) {
        var activatedDependencyCount = 0

        try {
            activationMutex.withLock {
                check(mutableActivationState.value == VexCompoundActivationState.INACTIVE) {
                    "VexCompoundRelay can only activate from INACTIVE"
                }

                val dependencies =
                    declaredDependencies

                validateDependencyPreflight(dependencies)

                mutableActivationState.value =
                    VexCompoundActivationState.ACTIVATING

                val activationGate =
                    CompletableDeferred<Unit>()
                val started =
                    mutableListOf<VexCoroutineHandle>()
                val leasedCompounds =
                    mutableListOf<VexCompoundRelay<*, *>>()

                try {
                    for (compoundDependency in compoundDependencies(dependencies)) {
                        compoundDependency.acquireDependentLease(this)
                        leasedCompounds += compoundDependency
                    }

                    for (dependency in dependencies) {
                        started +=
                            dependency.observe(
                                executionOwner = executionOwner,
                                activationGate = activationGate,
                                sendInput = { inputState, originRef ->
                                    send(
                                        inputState = inputState,
                                        originRef = originRef,
                                    )
                                },
                            )
                    }

                    dependencyHandles =
                        started.toList()
                    leasedCompoundDependencies =
                        leasedCompounds.toList()

                    mutableActivationState.value =
                        VexCompoundActivationState.ACTIVE
                    activatedDependencyCount =
                        dependencyHandles.size

                    activationGate.complete(Unit)
                } catch (failure: Throwable) {
                    releaseOwnedDependencies(
                        handles = started,
                        compoundDependencies = leasedCompounds,
                        reason = "compound relay activation rollback",
                    ) {
                        dependencyHandles =
                            emptyList()
                        leasedCompoundDependencies =
                            emptyList()

                        mutableActivationState.value =
                            VexCompoundActivationState.INACTIVE
                    }

                    throw failure
                }
            }
        } catch (failure: Throwable) {
            logSink.logSafely(
                VexLogEvent(
                    sourceRef = VexLogRef(relayRef.value),
                    eventRef = VexLogRef("compound.activation.failed"),
                    fields =
                        mapOf(
                            "failureClass" to
                                (failure::class.simpleName ?: "unknown"),
                        ),
                ),
            )
            throw failure
        }

        logSink.logSafely(
            VexLogEvent(
                sourceRef = VexLogRef(relayRef.value),
                eventRef = VexLogRef("compound.activated"),
                fields =
                    mapOf(
                        "dependencyCount" to
                            activatedDependencyCount.toString(),
                    ),
            ),
        )
    }

    suspend fun deactivate(
        reason: String = "compound relay deactivated",
    ) {
        val deactivated =
            activationMutex.withLock {
                if (mutableActivationState.value == VexCompoundActivationState.INACTIVE) {
                    return@withLock false
                }

                check(mutableActivationState.value == VexCompoundActivationState.ACTIVE) {
                    "VexCompoundRelay can only deactivate from ACTIVE"
                }

                check(activeDependentRelays.isEmpty()) {
                    "VexCompoundRelay cannot deactivate while active parents depend on it: " +
                        activeDependentRelays.joinToString { dependent ->
                            dependent.relayRef.value
                        }
                }

                mutableActivationState.value =
                    VexCompoundActivationState.DEACTIVATING

                val handles =
                    dependencyHandles

                releaseOwnedDependencies(
                    handles = handles,
                    compoundDependencies = leasedCompoundDependencies,
                    reason = reason,
                ) {
                    dependencyHandles =
                        emptyList()
                    leasedCompoundDependencies =
                        emptyList()

                    mutableActivationState.value =
                        VexCompoundActivationState.INACTIVE
                }

                true
            }

        if (deactivated) {
            logSink.logSafely(
                VexLogEvent(
                    sourceRef = VexLogRef(relayRef.value),
                    eventRef = VexLogRef("compound.deactivated"),
                ),
            )
        }
    }

    private fun send(
        inputState: InputState,
        originRef: VexLogRef? = null,
    ): VexCompoundState<InputState, OutputState> {
        val processed =
            synchronized(stateLock) {
                processLocked(
                    inputState = inputState.deepCopy(),
                    originRef = originRef,
                )
            }

        processed.logs.forEach { event ->
            logSink.logSafely(event)
        }

        return processed.state
    }

    private fun processLocked(
        inputState: InputState,
        originRef: VexLogRef?,
    ): ProcessedCompound<InputState, OutputState> {
        check(!processing) {
            "VexCompoundRelay cannot recursively send state from processStates"
        }

        val currentState =
            mutableCurrentState.value.deepCopy()

        processing = true
        return try {
            val processed =
                processStates(
                    inputState.deepCopy(),
                    currentState.deepCopy(),
                ).deepCopy()

            check(processed.revision == currentState.revision) {
                "VexCompoundState revision is owned by VexCompoundRelay; " +
                    "processStates must not mutate it"
            }

            val inputSameMeaning =
                processed.inputState.hasSameMeaningAs(currentState.inputState)
            val outputSameMeaning =
                processed.outputState.hasSameMeaningAs(currentState.outputState)

            check(processed.inputState != currentState.inputState || inputSameMeaning) {
                "Compound input semantic equality contradicts StateFlow equals() conflation"
            }
            check(processed.outputState != currentState.outputState || outputSameMeaning) {
                "Compound output semantic equality contradicts StateFlow equals() conflation"
            }

            val meaningChanged =
                !inputSameMeaning ||
                    !outputSameMeaning

            check(transitionSequence < Long.MAX_VALUE) {
                "VexCompoundRelay transitionSequence exhausted"
            }
            val nextTransitionSequence =
                transitionSequence + 1L
            val completedTransitionSequence =
                nextTransitionSequence.toString()

            if (!meaningChanged) {
                val exposedState =
                    currentState.deepCopy()
                val logs =
                    listOf(
                        VexLogEvent(
                            sourceRef = VexLogRef(relayRef.value),
                            eventRef = VexLogRef("relay.input.received"),
                            originRef = originRef,
                            fields =
                                mapOf(
                                    "revision" to currentState.revision.toString(),
                                    "transitionSequence" to completedTransitionSequence,
                                ),
                        ),
                        VexLogEvent(
                            sourceRef = VexLogRef(relayRef.value),
                            eventRef = VexLogRef("relay.state.unchanged"),
                            originRef = originRef,
                            fields =
                                mapOf(
                                    "revision" to currentState.revision.toString(),
                                    "transitionSequence" to completedTransitionSequence,
                                ),
                        ),
                    )

                transitionSequence =
                    nextTransitionSequence

                return ProcessedCompound(
                    state = exposedState,
                    logs = logs,
                )
            }

            check(currentState.revision < Long.MAX_VALUE) {
                "VexCompoundState revision exhausted"
            }

            val nextState =
                VexCompoundState(
                    inputState = processed.inputState.deepCopy(),
                    outputState = processed.outputState.deepCopy(),
                    revision = currentState.revision + 1L,
                )
            val retainedState =
                nextState.deepCopy()
            val exposedState =
                nextState.deepCopy()
            val logs =
                listOf(
                    VexLogEvent(
                        sourceRef = VexLogRef(relayRef.value),
                        eventRef = VexLogRef("relay.input.received"),
                        originRef = originRef,
                        fields =
                            mapOf(
                                "revision" to currentState.revision.toString(),
                                "transitionSequence" to completedTransitionSequence,
                            ),
                    ),
                    VexLogEvent(
                        sourceRef = VexLogRef(relayRef.value),
                        eventRef = VexLogRef("relay.state.changed"),
                        originRef = originRef,
                        fields =
                            mapOf(
                                "revision" to nextState.revision.toString(),
                                "transitionSequence" to completedTransitionSequence,
                            ),
                    ),
                )

            mutableCurrentState.value =
                retainedState
            transitionSequence =
                nextTransitionSequence

            ProcessedCompound(
                state = exposedState,
                logs = logs,
            )
        } finally {
            processing = false
        }
    }

    private fun compoundDependencies(
        dependencies: List<VexRelayDependency<InputState>>,
    ): List<VexCompoundRelay<*, *>> {
        val seen =
            Collections.newSetFromMap(
                IdentityHashMap<VexCompoundRelay<*, *>, Boolean>(),
            )

        return dependencies
            .mapNotNull { dependency ->
                dependency.sourceRelay as? VexCompoundRelay<*, *>
            }
            .filter { compound ->
                seen.add(compound)
            }
    }

    private suspend fun acquireDependentLease(
        dependent: VexCompoundRelay<*, *>,
    ) {
        activationMutex.withLock {
            check(mutableActivationState.value == VexCompoundActivationState.ACTIVE) {
                "Compound Relay dependency must remain ACTIVE while a parent acquires it: " +
                    relayRef.value
            }

            check(activeDependentRelays.add(dependent)) {
                "Compound Relay dependent lease is already held by: " +
                    dependent.relayRef.value
            }
        }
    }

    private suspend fun releaseDependentLease(
        dependent: VexCompoundRelay<*, *>,
    ) {
        activationMutex.withLock {
            check(activeDependentRelays.remove(dependent)) {
                "Compound Relay dependent lease was not held by: " +
                    dependent.relayRef.value
            }
        }
    }

    private fun validateDependencyPreflight(
        dependencies: List<VexRelayDependency<InputState>>,
    ) {
        check(dependencies.isNotEmpty()) {
            "VexCompoundRelay requires at least one declared dependency; use VexRelay for one truth"
        }

        val visiting =
            Collections.newSetFromMap(
                IdentityHashMap<VexObservableRelay<*>, Boolean>(),
            )
        val visited =
            Collections.newSetFromMap(
                IdentityHashMap<VexObservableRelay<*>, Boolean>(),
            )

        val identities =
            linkedMapOf<VexRelayRef, VexObservableRelay<*>>(
                relayRef to this,
            )

        visiting += this

        dependencies.forEach { dependency ->
            validateNoDependencyCycle(
                relay = dependency.sourceRelay,
                visiting = visiting,
                visited = visited,
                identities = identities,
                path = mutableListOf(relayRef),
            )
        }

        visiting -= this

        validateObserverRefUniquenessAcrossGraph()
    }

    private fun validateObserverRefUniquenessAcrossGraph() {
        val observerOwners =
            linkedMapOf<VexCoroutineRef, VexRelayRef>()
        val visitedCompounds =
            Collections.newSetFromMap(
                IdentityHashMap<VexCompoundRelay<*, *>, Boolean>(),
            )

        collectObserverRefs(
            compound = this,
            observerOwners = observerOwners,
            visitedCompounds = visitedCompounds,
        )
    }

    private fun collectObserverRefs(
        compound: VexCompoundRelay<*, *>,
        observerOwners: MutableMap<VexCoroutineRef, VexRelayRef>,
        visitedCompounds: MutableSet<VexCompoundRelay<*, *>>,
    ) {
        if (!visitedCompounds.add(compound)) {
            return
        }

        compound.dependencyDescriptorsForTopology()
            .forEach { dependency ->
                val existingOwner =
                    observerOwners.putIfAbsent(
                        dependency.coroutineRef,
                        compound.relayRef,
                    )

                check(existingOwner == null) {
                    "VexCoroutineRef must be unique within one active Relay graph: " +
                        dependency.coroutineRef.value +
                        " used by " +
                        existingOwner?.value +
                        " and " +
                        compound.relayRef.value
                }

                val childCompound =
                    dependency.sourceRelay as? VexCompoundRelay<*, *>

                if (childCompound != null) {
                    collectObserverRefs(
                        compound = childCompound,
                        observerOwners = observerOwners,
                        visitedCompounds = visitedCompounds,
                    )
                }
            }
    }

    private fun validateNoDependencyCycle(
        relay: VexObservableRelay<*>,
        visiting: MutableSet<VexObservableRelay<*>>,
        visited: MutableSet<VexObservableRelay<*>>,
        identities: MutableMap<VexRelayRef, VexObservableRelay<*>>,
        path: MutableList<VexRelayRef>,
    ) {
        val existingIdentityOwner =
            identities[relay.relayRef]

        check(existingIdentityOwner == null || existingIdentityOwner === relay) {
            "VexRelayRef must be unique within one dependency graph: " +
                relay.relayRef.value
        }

        if (existingIdentityOwner == null) {
            identities[relay.relayRef] =
                relay
        }

        if (relay in visited) {
            return
        }

        if (!visiting.add(relay)) {
            val cycle =
                (path + relay.relayRef)
                    .joinToString(" -> ") { ref ->
                        ref.value
                    }

            error(
                "VexCompoundRelay dependency cycle is not allowed: " +
                    cycle,
            )
        }

        path += relay.relayRef

        if (relay is VexCompoundRelay<*, *>) {
            relay.dependencySourcesForTopology()
                .forEach { child ->
                    validateNoDependencyCycle(
                        relay = child,
                        visiting = visiting,
                        visited = visited,
                        identities = identities,
                        path = path,
                    )
                }
        }

        path.removeAt(path.lastIndex)
        visiting -= relay
        visited += relay
    }

    private fun dependencyDescriptorsForTopology(): List<VexRelayDependency<*>> =
        declaredDependencies

    private fun dependencySourcesForTopology(): List<VexObservableRelay<*>> =
        dependencyDescriptorsForTopology().map { dependency ->
            dependency.sourceRelay
        }

    /**
     * One complete ownership-release primitive for rollback and normal deactivation.
     *
     * Once this Compound Relay owns dependency observers, caller cancellation must not strand only
     * part of that owned set or interrupt the owner's own release bookkeeping.
     */
    private suspend fun releaseOwnedDependencies(
        handles: List<VexCoroutineHandle>,
        compoundDependencies: List<VexCompoundRelay<*, *>>,
        reason: String,
        onReleased: () -> Unit,
    ) {
        withContext(NonCancellable) {
            handles.forEach { handle ->
                handle.forceStop(reason)
            }

            handles.forEach { handle ->
                handle.join()
            }

            compoundDependencies.forEach { compound ->
                compound.releaseDependentLease(this@VexCompoundRelay)
            }

            onReleased()
        }
    }

    private data class ProcessedCompound<
        InputState : VexInputState<InputState>,
        OutputState : VexOutputState<OutputState>,
    >(
        val state: VexCompoundState<InputState, OutputState>,
        val logs: List<VexLogEvent>,
    )
}

// [VXG RealForever]
