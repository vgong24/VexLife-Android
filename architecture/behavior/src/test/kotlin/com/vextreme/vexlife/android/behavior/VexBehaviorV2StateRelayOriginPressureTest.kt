package com.vextreme.vexlife.android.behavior

import java.util.ArrayDeque
import java.util.Collections
import com.vextreme.vexlife.android.execution.VexCoroutineHandle
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fourth V2 Origin-Walk pressure reference.
 *
 * Fresh readers who need the complete construction shape should read
 * [VexBehaviorV2FamilyCircuitTestSampleSuite] first. That teaching suite builds the candidate
 * Family Circuit Architecture one layer at a time:
 *
 *     independently-owned CURRENT truths
 *     -> partial ComplexInputState observations
 *     -> latest complete input
 *     -> process(latestStateInput)
 *     -> committed ComplexOutputState
 *     -> Act
 *     -> rightful target owner
 *     -> observed target CURRENT truth
 *     -> NEW input
 *
 * This pressure class then explores the harder failure modes around that shape: stale async
 * completion, nested identity/currentness, arbitrary depth, process-only Compound reuse and
 * Replaceable / Sequenced committed-Output execution custody.
 *
 * Nothing in either class makes StateRelay, StateListSnapshot, Compound convergence or the candidate
 * Family Circuit name a confirmed V2 production standard. Each fixture exists to expose the failure
 * it prevents and the ownership law it may justify.
 */
class VexBehaviorV2StateRelayOriginPressureTest {
    @Test
    fun olderCompletionIsReevaluatedAgainstCurrentSemanticState() {
        val relay =
            StateRelay(
                initialState =
                    StateRelayPressureState.Pending(
                        intentRef =
                            "intent.old",
                        semanticValue =
                            "old-intent",
                    ),
            )

        relay.admit { current ->
            when (current) {
                is StateRelayPressureState.Pending ->
                    StateRelayPressureState.Pending(
                        intentRef =
                            "intent.new",
                        semanticValue =
                            "new-intent",
                    )

                is StateRelayPressureState.Settled ->
                    current
            }
        }

        /*
         * Older completion has no direct write authority. At the serialized boundary it receives
         * exact CURRENT State and must prove its consequence is still lawful.
         */
        relay.admit { current ->
            when (current) {
                is StateRelayPressureState.Pending ->
                    if (
                        current.intentRef ==
                        "intent.old"
                    ) {
                        StateRelayPressureState.Settled(
                            semanticValue =
                                "old-result",
                        )
                    } else {
                        current
                    }

                is StateRelayPressureState.Settled ->
                    current
            }
        }

        assertEquals(
            StateRelayPressureState.Pending(
                intentRef =
                    "intent.new",
                semanticValue =
                    "new-intent",
            ),
            relay.current,
        )

        relay.admit { current ->
            when (current) {
                is StateRelayPressureState.Pending ->
                    if (
                        current.intentRef ==
                        "intent.new"
                    ) {
                        StateRelayPressureState.Settled(
                            semanticValue =
                                "new-result",
                        )
                    } else {
                        current
                    }

                is StateRelayPressureState.Settled ->
                    current
            }
        }

        assertEquals(
            StateRelayPressureState.Settled(
                semanticValue =
                    "new-result",
            ),
            relay.current,
        )
    }

    @Test
    fun liveFamilyProcessesControlInsideStateRelayThenDisplayReadsSameCurrentTree() {
        val family =
            LiveTreeFamily(
                initialState =
                    LiveTreeFamily.State.Ready(
                        tree =
                            StateTreeSnapshot.of(
                                rootRefs =
                                    listOf(
                                        "A1",
                                        "A2",
                                    ),
                                nodes =
                                    listOf(
                                        StateTreeNode(
                                            ref = "A1",
                                            label = "A1",
                                            childRefs = listOf("B1"),
                                        ),
                                        StateTreeNode(
                                            ref = "B1",
                                            label = "B1",
                                            childRefs = listOf("C1"),
                                        ),
                                        StateTreeNode(
                                            ref = "C1",
                                            label = "C1",
                                            childRefs = emptyList(),
                                        ),
                                        StateTreeNode(
                                            ref = "A2",
                                            label = "A2",
                                            childRefs = listOf("B2"),
                                        ),
                                        StateTreeNode(
                                            ref = "B2",
                                            label = "B2",
                                            childRefs = emptyList(),
                                        ),
                                    ),
                            ),
                    ),
            )

        assertEquals(
            listOf(
                "A1",
                "B1",
                "C1",
                "A2",
                "B2",
            ),
            family.displayState.labels,
        )

        family.activate(
            LiveTreeFamily.Control.RenameNode(
                ref = "C1",
                label = "C1-current",
            ),
        )

        assertEquals(
            listOf(
                "A1",
                "B1",
                "C1-current",
                "A2",
                "B2",
            ),
            family.displayState.labels,
        )

        val current =
            family.current as
                LiveTreeFamily.State.Ready

        assertEquals(
            listOf("B1"),
            current.tree["A1"].childRefs,
        )
        assertEquals(
            listOf("B2"),
            current.tree["A2"].childRefs,
        )
    }
    @Test
    fun arbitraryDepthNestedCurrentTreeUpdatesOneNodeWithoutRememberingAncestors() {
        val relay =
            NestedTreeStateRelay(
                initialState =
                    NestedTreeState(
                        tree =
                            StateTreeSnapshot.of(
                                rootRefs =
                                    listOf(
                                        "A1",
                                        "A2",
                                    ),
                                nodes =
                                    listOf(
                                        StateTreeNode(
                                            ref = "A1",
                                            label = "A1",
                                            childRefs = listOf("B1"),
                                        ),
                                        StateTreeNode(
                                            ref = "B1",
                                            label = "B1",
                                            childRefs = listOf("C1"),
                                        ),
                                        StateTreeNode(
                                            ref = "C1",
                                            label = "C1",
                                            childRefs = emptyList(),
                                        ),
                                        StateTreeNode(
                                            ref = "A2",
                                            label = "A2",
                                            childRefs = listOf("B2"),
                                        ),
                                        StateTreeNode(
                                            ref = "B2",
                                            label = "B2",
                                            childRefs = emptyList(),
                                        ),
                                    ),
                            ),
                    ),
            )

        /*
         * Exact concern:
         *
         *     [A1[B1[C1]]], [A2[B2]]
         *
         * Updating C1 does not rebuild A1 or B1 and does not need feature code to remember A2/B2.
         * Their hierarchy survives through stable semantic refs inside one coherent current snapshot.
         */
        relay.updateLabel(
            ref =
                "C1",
            label =
                "C1-current",
        )

        assertEquals(
            listOf(
                "A1",
                "B1",
                "C1-current",
                "A2",
                "B2",
            ),
            relay.current
                .tree
                .depthFirstLabels(),
        )
        assertEquals(
            listOf(
                "B1",
            ),
            relay.current
                .tree[
                    "A1"
                ]
                .childRefs,
        )
        assertEquals(
            listOf(
                "C1",
            ),
            relay.current
                .tree[
                    "B1"
                ]
                .childRefs,
        )
        assertEquals(
            listOf(
                "B2",
            ),
            relay.current
                .tree[
                    "A2"
                ]
                .childRefs,
        )
    }

    @Test
    fun nestedTreeDepthIsDataNotFeatureSpecificCopyCode() {
        val depth =
            48
        val nodes =
            (0 until depth).map { nodeIndex ->
                val ref =
                    "N" +
                        nodeIndex
                StateTreeNode(
                    ref =
                        ref,
                    label =
                        ref,
                    childRefs =
                        if (
                            nodeIndex + 1 <
                            depth
                        ) {
                            listOf(
                                "N" +
                                    (
                                        nodeIndex +
                                            1
                                    ),
                            )
                        } else {
                            emptyList()
                        },
                )
            }
        val relay =
            NestedTreeStateRelay(
                initialState =
                    NestedTreeState(
                        tree =
                            StateTreeSnapshot.of(
                                rootRefs =
                                    listOf(
                                        "N0",
                                    ),
                                nodes =
                                    nodes,
                            ),
                    ),
            )

        relay.updateLabel(
            ref =
                "N47",
            label =
                "N47-current",
        )

        assertEquals(
            depth,
            relay.current
                .tree
                .depthFirstLabels()
                .size,
        )
        assertEquals(
            "N47-current",
            relay.current
                .tree
                .depthFirstLabels()
                .last(),
        )
    }

    @Test
    fun nestedTreeSnapshotDetachesCallerOwnedStructureWithoutRecursiveFeatureCopy() {
        val rootRefs =
            mutableListOf(
                "A1",
                "A2",
            )
        val a1Children =
            mutableListOf(
                "B1",
            )
        val b1Children =
            mutableListOf(
                "C1",
            )
        val nodes =
            mutableListOf(
                StateTreeNode(
                    ref = "A1",
                    label = "A1",
                    childRefs = a1Children,
                ),
                StateTreeNode(
                    ref = "B1",
                    label = "B1",
                    childRefs = b1Children,
                ),
                StateTreeNode(
                    ref = "C1",
                    label = "C1",
                    childRefs = emptyList(),
                ),
                StateTreeNode(
                    ref = "A2",
                    label = "A2",
                    childRefs = listOf("B2"),
                ),
                StateTreeNode(
                    ref = "B2",
                    label = "B2",
                    childRefs = emptyList(),
                ),
            )

        val relay =
            NestedTreeStateRelay(
                initialState =
                    NestedTreeState(
                        tree =
                            StateTreeSnapshot.of(
                                rootRefs =
                                    rootRefs,
                                nodes =
                                    nodes,
                            ),
                    ),
            )

        rootRefs.clear()
        a1Children.clear()
        b1Children.clear()
        nodes.clear()

        assertEquals(
            listOf(
                "A1",
                "B1",
                "C1",
                "A2",
                "B2",
            ),
            relay.current
                .tree
                .depthFirstLabels(),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun nestedTreeRejectsDanglingChildIdentityInsteadOfRenderingShorterTree() {
        StateTreeSnapshot.of(
            rootRefs =
                listOf(
                    "A1",
                ),
            nodes =
                listOf(
                    StateTreeNode(
                        ref =
                            "A1",
                        label =
                            "A1",
                        childRefs =
                            listOf(
                                "B1",
                            ),
                    ),
                ),
        )
    }

    @Test
    fun committedOutputActsOnRightfulOwnerThenObservedTruthReturnsAsNewInput() {
        val trace =
            mutableListOf<String>()
        val targetOwner =
            TargetOwnerRelay(
                trace =
                    trace,
            )
        val relationship =
            DesiredActualConvergenceRelay(
                trace =
                    trace,
            )
        val circuit =
            DesiredActualCircuit(
                relationship =
                    relationship,
                targetOwner =
                    targetOwner,
                trace =
                    trace,
            )

        circuit.desiredChanged(
            "Dark",
        )

        assertEquals(
            TargetOwnerState(
                value =
                    "Dark",
            ),
            targetOwner.current,
        )
        assertEquals(
            DesiredActualInputState(
                desired =
                    "Dark",
                actual =
                    "Dark",
            ),
            relationship.currentInput,
        )
        assertEquals(
            DesiredActualOutputState.Settled(
                value =
                    "Dark",
            ),
            relationship.currentOutput,
        )

        /*
         * The important order:
         *
         * 1. process derives NeedsApply from latest input;
         * 2. CompoundState is committed and returned;
         * 3. Act consumes that committed output;
         * 4. rightful target owner changes;
         * 5. target owner's observed current truth returns as NEW input;
         * 6. process derives Settled.
         *
         * OutputState is never recycled as hidden process input.
         */
        assertEquals(
            listOf(
                "process:NeedsApply(Dark)",
                "commit:NeedsApply(Dark)",
                "act:NeedsApply(Dark)",
                "target:Dark",
                "process:Settled(Dark)",
                "commit:Settled(Dark)",
                "act:Settled(Dark)",
            ),
            trace,
        )
    }

    @Test
    fun replaceableActRunnerCancelsOlderEpisodeWhenNewerCommittedOutputNeedsNoAct() =
        runBlocking {
            val trace =
                Collections.synchronizedList(
                    mutableListOf<String>(),
                )
            val root =
                VexExecutionRoot()

            try {
                val targetOwner =
                    TargetOwnerRelay(
                        trace =
                            trace,
                        initialValue =
                            "Light",
                    )
                val relationship =
                    DesiredActualConvergenceRelay(
                        trace =
                            trace,
                    )
                val targetOperation =
                    GatedTargetOwnerOperation(
                        targetOwner =
                            targetOwner,
                        trace =
                            trace,
                        gatedValue =
                            "Dark",
                    )
                val runner =
                    CommittedOutputActRunner(
                        relationship =
                            relationship,
                        targetOperation =
                            targetOperation,
                        executionRoot =
                            root,
                        mode =
                            VexBehaviorV2BeginMode.Replaceable,
                        trace =
                            trace,
                    )

                /*
                 * Input can advance retained Compound currentness without creating a new Output
                 * consequence. The runner intentionally receives no receipt for this revision.
                 */
                assertEquals(
                    null,
                    relationship.actualOutputCommitted(
                        "Light",
                    ),
                )

                val needsDark =
                    checkNotNull(
                        relationship.desiredOutputCommitted(
                            "Dark",
                        ),
                    )

                runner.accept(
                    needsDark,
                )
                runner.accept(
                    needsDark,
                )

                /*
                 * Duplicate delivery of one exact committed Output is ignored by commit identity.
                 * The one admitted episode has started but has not reached the target owner yet.
                 */
                targetOperation.gatedStarted.await()

                val settledLight =
                    checkNotNull(
                        relationship.desiredOutputCommitted(
                            "Light",
                        ),
                    )

                assertEquals(
                    DesiredActualOutputState.Settled(
                        value =
                            "Light",
                    ),
                    settledLight.outputState,
                )

                /*
                 * The newer committed relationship truth requires no target mutation, but it still
                 * supersedes the older Replaceable reaction. Cancellation does not "uncommit" either
                 * OutputState; it only stops the now-obsolete realization episode.
                 */
                runner.accept(
                    settledLight,
                )
                runner.awaitAll()

                assertEquals(
                    TargetOwnerState(
                        value =
                            "Light",
                    ),
                    targetOwner.current,
                )
                assertEquals(
                    DesiredActualOutputState.Settled(
                        value =
                            "Light",
                    ),
                    relationship.currentOutput,
                )

                val recorded =
                    synchronized(
                        trace,
                    ) {
                        trace.toList()
                    }

                assertEquals(
                    1,
                    recorded.count { event ->
                        event ==
                            "execute:start:Dark"
                    },
                )
                assertEquals(
                    0,
                    recorded.count { event ->
                        event ==
                            "target:Dark"
                    },
                )
            } finally {
                root.forceStopAll()
            }
        }

    @Test
    fun replaceableActRunnerIgnoresInputOnlyRevisionWhenOutputMeaningIsStillCurrent() =
        runBlocking {
            val trace =
                Collections.synchronizedList(
                    mutableListOf<String>(),
                )
            val root =
                VexExecutionRoot()

            try {
                val targetOwner =
                    TargetOwnerRelay(
                        trace =
                            trace,
                        initialValue =
                            "A",
                    )
                val relationship =
                    DesiredActualConvergenceRelay(
                        trace =
                            trace,
                    )
                val targetOperation =
                    GatedTargetOwnerOperation(
                        targetOwner =
                            targetOwner,
                        trace =
                            trace,
                        gatedValue =
                            "Dark",
                    )
                val runner =
                    CommittedOutputActRunner(
                        relationship =
                            relationship,
                        targetOperation =
                            targetOperation,
                        executionRoot =
                            root,
                        mode =
                            VexBehaviorV2BeginMode.Replaceable,
                        trace =
                            trace,
                    )

                assertEquals(
                    null,
                    relationship.actualOutputCommitted(
                        "A",
                    ),
                )

                val needsDark =
                    checkNotNull(
                        relationship.desiredOutputCommitted(
                            "Dark",
                        ),
                    )

                runner.accept(
                    needsDark,
                )
                targetOperation.gatedStarted.await()

                /*
                 * actual=A -> actual=B advances the Compound revision, but desired is still Dark and
                 * Output remains NeedsApply(Dark). This is NOT a new Act consequence and must not
                 * cancel/replay the already-current Replaceable episode.
                 */
                assertEquals(
                    null,
                    relationship.actualOutputCommitted(
                        "B",
                    ),
                )

                assertEquals(
                    needsDark.compoundRevision +
                        1L,
                    relationship.current.revision,
                )

                targetOperation.releaseGated.complete(
                    Unit,
                )
                runner.awaitAll()

                assertEquals(
                    TargetOwnerState(
                        value =
                            "Dark",
                    ),
                    targetOwner.current,
                )
                assertEquals(
                    DesiredActualOutputState.Settled(
                        value =
                            "Dark",
                    ),
                    relationship.currentOutput,
                )

                val recorded =
                    synchronized(
                        trace,
                    ) {
                        trace.toList()
                    }

                assertEquals(
                    1,
                    recorded.count { event ->
                        event ==
                            "target:Dark"
                    },
                )
            } finally {
                root.forceStopAll()
            }
        }

    @Test
    fun sequencedActRunnerPreservesAcceptedOutputOrderWithoutDuplicatingOnInputOnlyCommit() =
        runBlocking {
            val trace =
                Collections.synchronizedList(
                    mutableListOf<String>(),
                )
            val root =
                VexExecutionRoot()

            try {
                val targetOwner =
                    TargetOwnerRelay(
                        trace =
                            trace,
                        initialValue =
                            "Initial",
                    )
                val relationship =
                    DesiredActualConvergenceRelay(
                        trace =
                            trace,
                    )
                val targetOperation =
                    GatedTargetOwnerOperation(
                        targetOwner =
                            targetOwner,
                        trace =
                            trace,
                        gatedValue =
                            "Dark",
                    )
                val runner =
                    CommittedOutputActRunner(
                        relationship =
                            relationship,
                        targetOperation =
                            targetOperation,
                        executionRoot =
                            root,
                        mode =
                            VexBehaviorV2BeginMode.Sequenced,
                        trace =
                            trace,
                    )

                assertEquals(
                    null,
                    relationship.actualOutputCommitted(
                        "Initial",
                    ),
                )

                val needsDark =
                    checkNotNull(
                        relationship.desiredOutputCommitted(
                            "Dark",
                        ),
                    )

                runner.accept(
                    needsDark,
                )
                targetOperation.gatedStarted.await()

                val needsLight =
                    checkNotNull(
                        relationship.desiredOutputCommitted(
                            "Light",
                        ),
                    )

                runner.accept(
                    needsLight,
                )
                runner.accept(
                    needsLight,
                )

                targetOperation.releaseGated.complete(
                    Unit,
                )
                runner.awaitAll()

                assertEquals(
                    TargetOwnerState(
                        value =
                            "Light",
                    ),
                    targetOwner.current,
                )
                assertEquals(
                    DesiredActualInputState(
                        desired =
                            "Light",
                        actual =
                            "Light",
                    ),
                    relationship.currentInput,
                )
                assertEquals(
                    DesiredActualOutputState.Settled(
                        value =
                            "Light",
                    ),
                    relationship.currentOutput,
                )

                /*
                 * The Dark result observation advances the full Compound revision while Output is
                 * still NeedsApply(Light). No second Light receipt is formed, so Sequenced custody
                 * contains exactly the two accepted output-changing consequences.
                 */
                assertEquals(
                    5L,
                    relationship.current.revision,
                )

                val recorded =
                    synchronized(
                        trace,
                    ) {
                        trace.toList()
                    }

                assertEquals(
                    listOf(
                        "target:Dark",
                        "target:Light",
                    ),
                    recorded.filter { event ->
                        event.startsWith(
                            "target:",
                        )
                    },
                )
                assertEquals(
                    1,
                    recorded.count { event ->
                        event ==
                            "execute:start:Light"
                    },
                )
            } finally {
                root.forceStopAll()
            }
        }

    @Test
    fun independentlyOwnedChildCurrentnessConvergesByStableRefWithoutParentAbsorption() {
        val relay =
            NestedChildConvergenceRelay()

        relay.parentOrderChanged(
            listOf(
                "child.alpha",
                "child.beta",
            ),
        )
        relay.childCurrentChanged(
            mapOf(
                "child.alpha" to
                    ChildCurrentState(
                        childRef =
                            "child.alpha",
                        label =
                            "Alpha",
                    ),
                "child.beta" to
                    ChildCurrentState(
                        childRef =
                            "child.beta",
                        label =
                            "Beta",
                    ),
            ),
        )

        assertEquals(
            listOf(
                "Alpha",
                "Beta",
            ),
            relay.currentOutput.labels,
        )

        /*
         * Child truth changes without rewriting parent membership/order. The convergence relationship
         * receives a new coherent child-current index and re-derives display order by stable ref.
         */
        relay.childCurrentChanged(
            mapOf(
                "child.alpha" to
                    ChildCurrentState(
                        childRef =
                            "child.alpha",
                        label =
                            "Alpha",
                    ),
                "child.beta" to
                    ChildCurrentState(
                        childRef =
                            "child.beta",
                        label =
                            "Beta — current",
                    ),
            ),
        )

        assertEquals(
            listOf(
                "Alpha",
                "Beta — current",
            ),
            relay.currentOutput.labels,
        )

        relay.parentOrderChanged(
            listOf(
                "child.beta",
                "child.alpha",
            ),
        )

        assertEquals(
            listOf(
                "Beta — current",
                "Alpha",
            ),
            relay.currentOutput.labels,
        )
    }

    @Test(
        expected =
            IllegalStateException::class,
    )
    fun readyNestedConvergenceRejectsMissingChildCurrentStateInsteadOfRenderingShorterList() {
        val relay =
            NestedChildConvergenceRelay()

        relay.parentOrderChanged(
            listOf(
                "child.alpha",
                "child.missing",
            ),
        )

        relay.childCurrentChanged(
            mapOf(
                "child.alpha" to
                    ChildCurrentState(
                        childRef =
                            "child.alpha",
                        label =
                            "Alpha",
                    ),
            ),
        )
    }

    /**
     * Integrated candidate showing where StateRelay belongs in the live Family loop.
     *
     * Act resolution happens inside StateRelay.process(current -> ...). StateRelay therefore owns
     * the exact CURRENT snapshot used for both Act selection and immediate State advancement.
     * The normalized tree remains ordinary semantic State logic called by the Act consequence.
     */
    private class LiveTreeFamily(
        initialState:
            State,
    ) :
        VexBehaviorFamily<
            LiveTreeFamily
        > {
        sealed interface State :
            VexBehaviorFamily.State<
                LiveTreeFamily
            >,
            VexState<
                State
            > {
            data class Ready(
                val tree:
                    StateTreeSnapshot,
            ) :
                State {
                override fun deepCopy():
                    State =
                    copy(
                        tree =
                            tree.detached(),
                    )
            }
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                LiveTreeFamily
            > {
            data class RenameNode(
                val ref: String,
                val label: String,
            ) :
                Control
        }

        data class DisplayState(
            val labels:
                List<String>,
        )

        private val stateRelay =
            FamilyStateRelay(
                relayRef =
                    "origin-pressure.family.state-relay.live-tree",
                initialState =
                    initialState,
            )

        val current:
            State
            get() =
                stateRelay.current

        val displayState:
            DisplayState
            get() =
                when (
                    val currentState =
                        current
                ) {
                    is State.Ready ->
                        DisplayState(
                            labels =
                                currentState
                                    .tree
                                    .depthFirstLabels(),
                        )
                }

        val acts =
            vexActs<
                LiveTreeFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "origin-pressure.family.state-relay.live-tree",
            ) {
                state<State.Ready> {
                    on<Control.RenameNode>(
                        whenCurrent = { currentState, rename ->
                            runCatching {
                                currentState.tree[rename.ref]
                            }.isSuccess
                        },
                    ) { currentState, rename ->
                        admit(
                            currentState.copy(
                                tree =
                                    currentState.tree.updateLabel(
                                        ref =
                                            rename.ref,
                                        label =
                                            rename.label,
                                    ),
                            ),
                        )
                    }
                }
            }

        fun activate(
            control:
                Control,
        ):
            VexBehaviorV2Act<
                LiveTreeFamily,
                State,
                Control
            >? {
            var selected:
                VexBehaviorV2Act<
                    LiveTreeFamily,
                    State,
                    Control
                >? =
                null

            stateRelay.process { currentState ->
                val act =
                    acts.resolve(
                        current =
                            currentState,
                        control =
                            control,
                    )
                        ?: return@process currentState

                selected =
                    act

                act.immediateDisposition
                    .admittedState
                    ?: currentState
            }

            return selected
        }
    }

    private class FamilyStateRelay<
        State : VexState<
            State
        >,
    >(
        relayRef: String,
        initialState: State,
    ) :
        VexRelay<
            State
        >(
            relayRef =
                relayRef,
            initialState =
                initialState,
        ) {
        fun process(
            transform:
                (
                    State,
                ) ->
                    State,
        ):
            State =
            update(
                transform,
            )
    }
    /**
     * Origin candidate only: StateRelay.
     *
     * Reuse is based on V1 Relay's design reason, not existence:
     *
     *     incoming cause
     *     + exact CURRENT retained State
     *     -> one non-suspending serialized decision
     *     -> publish only changed semantic meaning
     *
     * V1 deepCopy is a separate ownership-detachment law. Source freshness remains source-owned.
     */
    private class StateRelay(
        initialState:
            StateRelayPressureState,
    ) :
        VexRelay<
            StateRelayPressureState
        >(
            relayRef =
                "origin-pressure.family.state-relay",
            initialState =
                initialState,
        ) {
        fun admit(
            transition:
                (
                    StateRelayPressureState,
                ) ->
                    StateRelayPressureState,
        ):
            StateRelayPressureState =
            update(
                transition,
            )
    }

    private sealed interface StateRelayPressureState :
        VexState<
            StateRelayPressureState
        > {
        data class Pending(
            val intentRef: String,
            val semanticValue: String,
        ) :
            StateRelayPressureState

        data class Settled(
            val semanticValue: String,
        ) :
            StateRelayPressureState

        override fun deepCopy():
            StateRelayPressureState =
            when (this) {
                is Pending ->
                    copy()

                is Settled ->
                    copy()
            }
    }

    /**
     * Identity-normalized current tree pressure.
     *
     * Hierarchy is data:
     *
     *     rootRefs
     *     node.ref
     *     node.childRefs
     *
     * A feature does not recursively rebuild ancestors merely to preserve descendants. Snapshot
     * construction detaches the structural collections once; later node updates replace one node in
     * the index while every untouched relationship remains addressable by ref.
     *
     * Node labels are immutable fixture values. A real domain value that owns mutable internals would
     * still need its own immutable/snapshot boundary; StateTreeSnapshot solves structural identity and
     * hierarchy preservation, not arbitrary mutable-value aliasing.
     */
    private data class StateTreeNode(
        val ref: String,
        val label: String,
        val childRefs:
            List<String>,
    )

    private class StateTreeSnapshot private constructor(
        rootRefs:
            List<String>,
        nodes:
            Map<
                String,
                StateTreeNode
            >,
    ) {
        private val retainedRootRefs =
            rootRefs.toList()
        private val retainedNodes =
            nodes.mapValues { (_, node) ->
                node.copy(
                    childRefs =
                        node.childRefs.toList(),
                )
            }

        val rootRefs:
            List<String>
            get() =
                retainedRootRefs.toList()

        init {
            require(
                retainedRootRefs.distinct().size ==
                    retainedRootRefs.size,
            ) {
                "StateTreeSnapshot root refs must be unique"
            }

            require(
                retainedNodes.all { (ref, node) ->
                    ref ==
                        node.ref
                },
            ) {
                "StateTreeSnapshot node keys must match node refs"
            }

            val allChildRefs =
                retainedNodes.values
                    .flatMap { node ->
                        node.childRefs
                    }

            require(
                (
                    retainedRootRefs +
                        allChildRefs
                ).all(
                    retainedNodes::containsKey,
                ),
            ) {
                "StateTreeSnapshot every root/child ref must resolve to one current node"
            }

            val reachable =
                linkedSetOf<String>()
            val active =
                linkedSetOf<String>()

            fun visit(
                ref: String,
            ) {
                require(
                    ref !in
                        active,
                ) {
                    "StateTreeSnapshot cannot contain a child cycle at $ref"
                }

                if (
                    ref in
                    reachable
                ) {
                    return
                }

                active +=
                    ref
                retainedNodes
                    .getValue(
                        ref,
                    )
                    .childRefs
                    .forEach(
                        ::visit,
                    )
                active -=
                    ref
                reachable +=
                    ref
            }

            retainedRootRefs.forEach(
                ::visit,
            )

            require(
                reachable ==
                    retainedNodes.keys,
            ) {
                "StateTreeSnapshot nodes must belong to the current rooted tree"
            }
        }

        operator fun get(
            ref: String,
        ):
            StateTreeNode =
            retainedNodes
                .getValue(
                    ref,
                )
                .let { node ->
                    node.copy(
                        childRefs =
                            node.childRefs.toList(),
                    )
                }

        fun updateLabel(
            ref: String,
            label: String,
        ):
            StateTreeSnapshot {
            require(
                ref in
                    retainedNodes,
            ) {
                "StateTreeSnapshot cannot update unknown ref $ref"
            }

            val current =
                retainedNodes.getValue(
                    ref,
                )

            return StateTreeSnapshot(
                rootRefs =
                    retainedRootRefs,
                nodes =
                    retainedNodes +
                        (
                            ref to
                                current.copy(
                                    label =
                                        label,
                                )
                        ),
            )
        }

        fun detached():
            StateTreeSnapshot =
            StateTreeSnapshot(
                rootRefs =
                    retainedRootRefs,
                nodes =
                    retainedNodes,
            )

        fun depthFirstLabels():
            List<String> {
            val labels =
                mutableListOf<String>()
            val stack =
                ArrayDeque<String>()

            retainedRootRefs
                .asReversed()
                .forEach(
                    stack::addLast,
                )

            while (
                stack.isNotEmpty()
            ) {
                val ref =
                    stack.removeLast()
                val node =
                    retainedNodes.getValue(
                        ref,
                    )

                labels +=
                    node.label

                node.childRefs
                    .asReversed()
                    .forEach(
                        stack::addLast,
                    )
            }

            return labels
        }

        companion object {
            fun of(
                rootRefs:
                    List<String>,
                nodes:
                    List<
                        StateTreeNode
                    >,
            ):
                StateTreeSnapshot {
                require(
                    nodes
                        .map(
                            StateTreeNode::ref,
                        )
                        .distinct()
                        .size ==
                        nodes.size,
                ) {
                    "StateTreeSnapshot node refs must be unique"
                }

                return StateTreeSnapshot(
                    rootRefs =
                        rootRefs,
                    nodes =
                        nodes.associateBy(
                            StateTreeNode::ref,
                        ),
                )
            }
        }
    }

    private data class NestedTreeState(
        val tree:
            StateTreeSnapshot,
    ) :
        VexState<
            NestedTreeState
        > {
        /*
         * No descendant-specific copy logic lives here. StateTreeSnapshot owns structural detachment
         * for arbitrary depth; the Family/root State only knows that it owns one snapshot value.
         */
        override fun deepCopy():
            NestedTreeState =
            copy(
                tree =
                    tree.detached(),
            )
    }

    private class NestedTreeStateRelay(
        initialState:
            NestedTreeState,
    ) :
        VexRelay<
            NestedTreeState
        >(
            relayRef =
                "origin-pressure.family.state-relay.nested-tree",
            initialState =
                initialState,
        ) {
        fun updateLabel(
            ref: String,
            label: String,
        ) {
            update { current ->
                current.copy(
                    tree =
                        current.tree.updateLabel(
                            ref =
                                ref,
                            label =
                                label,
                        ),
                )
            }
        }
    }

    /**
     * Independently-owned child currentness should converge by stable identity rather than being
     * recursively absorbed into parent State merely so presentation can render a list.
     */
    private interface LatestInputState<
        Self : LatestInputState<
            Self
        >,
    > :
        VexInputState<
            Self
        > {
        /**
         * Materialize the complete latest process input from one incoming partial patch.
         *
         * This is input-shape ownership, not feature relationship calculation. Once this returns,
         * the Relay process should be able to reason from one complete latestStateInput.
         */
        fun latest(
            incoming:
                Self,
        ):
            Self
    }

    /**
     * Test-only commit receipt for the exact Compound commit whose Output meaning changed.
     *
     * [compoundRevision] already identifies one committed CompoundState. The receipt does not create
     * another event ledger or Output revision counter. It is formed only when OutputState changes,
     * because input-only currentness movement is not a new outward consequence to realize.
     */
    private data class CommittedOutput<
        OutputState,
    >(
        val compoundRevision:
            Long,
        val outputState:
            OutputState,
    ) where OutputState :
        VexOutputState<
            OutputState
        >

    /**
     * Test-only reusable Compound shape matching the Origin readability pressure:
     *
     * incoming patch + retained current input
     * -> latestStateInput
     * -> ONE feature-specific process(latestStateInput)
     * -> generic CompoundState reassembly
     *
     * Concrete Relays do not repeat currentState.copy(...) or input/output plumbing.
     */
    private abstract class LatestInputCompoundRelay<
        InputState,
        OutputState,
    >(
        relayRef: String,
        initialInputState:
            InputState,
        initialOutputState:
            OutputState,
    ) :
        VexCompoundRelay<
            InputState,
            OutputState
        >(
            relayRef =
                relayRef,
            initialInputState =
                initialInputState,
            initialOutputState =
                initialOutputState,
        )
        where InputState :
            LatestInputState<
                InputState
            >,
              OutputState :
            VexOutputState<
                OutputState
            > {
        final override fun processStates(
            inputState:
                InputState,
            currentState:
                VexCompoundState<
                    InputState,
                    OutputState
                >,
        ):
            VexCompoundState<
                InputState,
                OutputState
            > {
            val latestStateInput =
                currentState
                    .inputState
                    .latest(
                        inputState,
                    )

            val latestStateOutput =
                process(
                    latestStateInput,
                )

            return currentState.copy(
                inputState =
                    latestStateInput,
                outputState =
                    latestStateOutput,
            )
        }

        /**
         * The one relationship-specific surface.
         *
         * At this point input patching/currentness plumbing is finished. The feature reads the whole
         * latest input and answers only: "what does this current relationship mean?"
         */
        protected abstract fun process(
            latestStateInput:
                InputState,
        ):
            OutputState

        /**
         * One complete synchronous relationship admission.
         *
         * send(...) returns only after VexCompoundRelay has committed the resulting CompoundState.
         * Any Act/Effect belongs outside this method and therefore outside processStates().
         */
        protected fun receive(
            inputState:
                InputState,
        ):
            VexCompoundState<
                InputState,
                OutputState
            > =
            send(
                inputState,
            )

        /*
         * This wrapper is pressure-only. A production post-commit seam would need to live at the
         * actual Relay commit boundary so dependency-driven send(...) cannot bypass it.
         */
        private val outputCommitLock =
            Any()

        protected fun receiveOutputCommit(
            inputState:
                InputState,
        ):
            CommittedOutput<
                OutputState
            >? =
            synchronized(
                outputCommitLock,
            ) {
                val beforeOutput =
                    currentOutput
                val committed =
                    send(
                        inputState,
                    )

                if (
                    committed.outputState ==
                    beforeOutput
                ) {
                    null
                } else {
                    CommittedOutput(
                        compoundRevision =
                            committed.revision,
                        outputState =
                            committed
                                .outputState
                                .deepCopy(),
                    )
                }
            }
    }

    private data class DesiredActualInputState(
        val desired:
            String? = null,
        val actual:
            String? = null,
    ) :
        LatestInputState<
            DesiredActualInputState
        > {
        override fun latest(
            incoming:
                DesiredActualInputState,
        ):
            DesiredActualInputState =
            copy(
                desired =
                    incoming.desired
                        ?: desired,
                actual =
                    incoming.actual
                        ?: actual,
            )

        override fun deepCopy():
            DesiredActualInputState =
            copy()
    }

    private sealed interface DesiredActualOutputState :
        VexOutputState<
            DesiredActualOutputState
        > {
        data object Unobserved :
            DesiredActualOutputState

        data class NeedsApply(
            val value: String,
        ) :
            DesiredActualOutputState

        data class Settled(
            val value: String,
        ) :
            DesiredActualOutputState

        override fun deepCopy():
            DesiredActualOutputState =
            when (
                this
            ) {
                Unobserved ->
                    Unobserved

                is NeedsApply ->
                    copy()

                is Settled ->
                    copy()
            }
    }

    /**
     * Pure relationship owner.
     *
     * It never mutates TargetOwnerRelay. It only answers what desired + actual CURRENT truth means.
     */
    private class DesiredActualConvergenceRelay(
        private val trace:
            MutableList<String>,
    ) :
        LatestInputCompoundRelay<
            DesiredActualInputState,
            DesiredActualOutputState
        >(
            relayRef =
                "origin-pressure.process-act.desired-actual",
            initialInputState =
                DesiredActualInputState(),
            initialOutputState =
                DesiredActualOutputState.Unobserved,
        ) {
        fun desiredChanged(
            value: String,
        ):
            VexCompoundState<
                DesiredActualInputState,
                DesiredActualOutputState
            > =
            receive(
                DesiredActualInputState(
                    desired =
                        value,
                ),
            )

        fun actualChanged(
            value: String,
        ):
            VexCompoundState<
                DesiredActualInputState,
                DesiredActualOutputState
            > =
            receive(
                DesiredActualInputState(
                    actual =
                        value,
                ),
            )

        fun desiredOutputCommitted(
            value: String,
        ):
            CommittedOutput<
                DesiredActualOutputState
            >? =
            receiveOutputCommit(
                DesiredActualInputState(
                    desired =
                        value,
                ),
            )

        fun actualOutputCommitted(
            value: String,
        ):
            CommittedOutput<
                DesiredActualOutputState
            >? =
            receiveOutputCommit(
                DesiredActualInputState(
                    actual =
                        value,
                ),
            )

        override fun process(
            latestStateInput:
                DesiredActualInputState,
        ):
            DesiredActualOutputState {
            val desired =
                latestStateInput.desired
            val actual =
                latestStateInput.actual

            val output =
                when {
                    desired == null ->
                        DesiredActualOutputState
                            .Unobserved

                    actual ==
                        desired ->
                        DesiredActualOutputState
                            .Settled(
                                desired,
                            )

                    else ->
                        DesiredActualOutputState
                            .NeedsApply(
                                desired,
                            )
                }

            trace +=
                "process:" +
                    output.traceValue()

            return output
        }
    }

    private data class TargetOwnerState(
        val value:
            String? = null,
    ) :
        VexState<
            TargetOwnerState
        > {
        override fun deepCopy():
            TargetOwnerState =
            copy()
    }

    /**
     * Separate rightful current-State owner.
     *
     * The convergence relationship may ask it to change only after a committed OutputState says that
     * change is currently required.
     */
    private class TargetOwnerRelay(
        private val trace:
            MutableList<String>,
        initialValue:
            String? = null,
    ) :
        VexRelay<
            TargetOwnerState
        >(
            relayRef =
                "origin-pressure.process-act.target-owner",
            initialState =
                TargetOwnerState(
                    value =
                        initialValue,
                ),
        ) {
        fun apply(
            value: String,
        ) {
            update { current ->
                trace +=
                    "target:" +
                        value

                current.copy(
                    value =
                        value,
                )
            }
        }
    }

    /**
     * Test-only target Operation used to pressure execution-lifetime semantics.
     *
     * The gate is intentionally before [TargetOwnerRelay.apply]. Replaceable cancellation can
     * therefore prove that an obsolete realization may be stopped without pretending the already-
     * committed relationship Output disappeared. Once the target owner really changes, observation
     * must still return that truth through the relationship input circuit.
     */
    private class GatedTargetOwnerOperation(
        private val targetOwner:
            TargetOwnerRelay,
        private val trace:
            MutableList<String>,
        private val gatedValue:
            String?,
    ) {
        val gatedStarted =
            CompletableDeferred<Unit>()
        val releaseGated =
            CompletableDeferred<Unit>()

        suspend fun apply(
            value: String,
        ):
            String {
            trace +=
                "execute:start:" +
                    value

            if (
                value ==
                gatedValue
            ) {
                gatedStarted.complete(
                    Unit,
                )
                releaseGated.await()
            }

            targetOwner.apply(
                value,
            )

            trace +=
                "execute:return:" +
                    value

            return value
        }
    }

    private data class ActEpisode(
        val compoundRevision:
            Long,
        val value:
            String,
    )

    /**
     * Test-only production-runner pressure.
     *
     * It is deliberately NOT a proposed generic class. It exists to expose the minimum laws a
     * production post-commit runner must preserve:
     *
     * 1. one exact output-changing Compound commit produces at most one accepted episode;
     * 2. input-only Compound revisions do not replay or stale the current Output consequence;
     * 3. Replaceable supersession is driven by newer committed Output meaning, including a newer
     *    no-op/Settled output that has no Operation of its own;
     * 4. Sequenced custody preserves every accepted effect-bearing Output transition in order;
     * 5. cancellation changes execution lifetime, never the already-committed relationship truth;
     * 6. target-owner truth returns through observation, never by writing Output back into process.
     */
    private class CommittedOutputActRunner(
        private val relationship:
            DesiredActualConvergenceRelay,
        private val targetOperation:
            GatedTargetOwnerOperation,
        private val executionRoot:
            VexExecutionRoot,
        private val mode:
            VexBehaviorV2BeginMode,
        private val trace:
            MutableList<String>,
    ) {
        private val lock =
            Any()
        private val seenCommitRevisions =
            linkedSetOf<Long>()
        private val handles =
            mutableListOf<
                VexCoroutineHandle
            >()

        @Volatile
        private var latestOutputCommitRevision =
            -1L

        private var activeReplaceableRevision:
            Long? =
            null
        private var activeReplaceableHandle:
            VexCoroutineHandle? =
            null

        fun accept(
            committed:
                CommittedOutput<
                    DesiredActualOutputState
                >?,
        ) {
            if (
                committed ==
                null
            ) {
                return
            }

            val episode =
                synchronized(
                    lock,
                ) {
                    if (
                        !seenCommitRevisions.add(
                            committed.compoundRevision,
                        )
                    ) {
                        return
                    }

                    latestOutputCommitRevision =
                        committed.compoundRevision

                    trace +=
                        "runner:commit:" +
                            committed.compoundRevision +
                            ":" +
                            committed.outputState
                                .traceValue()

                    if (
                        mode ==
                        VexBehaviorV2BeginMode.Replaceable
                    ) {
                        activeReplaceableHandle
                            ?.forceStop(
                                "superseded by committed Output revision " +
                                    committed.compoundRevision,
                            )

                        activeReplaceableHandle =
                            null
                        activeReplaceableRevision =
                            null
                    }

                    when (
                        val output =
                            committed.outputState
                    ) {
                        DesiredActualOutputState.Unobserved,
                        is DesiredActualOutputState.Settled,
                        ->
                            null

                        is DesiredActualOutputState.NeedsApply -> {
                            if (
                                mode ==
                                VexBehaviorV2BeginMode.Replaceable
                            ) {
                                activeReplaceableRevision =
                                    committed.compoundRevision
                            }

                            ActEpisode(
                                compoundRevision =
                                    committed.compoundRevision,
                                value =
                                    output.value,
                            )
                        }
                    }
                }

            if (
                episode ==
                null
            ) {
                return
            }

            val handle =
                when (
                    mode
                ) {
                    VexBehaviorV2BeginMode.Replaceable ->
                        VexExecution.beginReplaceable(
                            executionRoot =
                                executionRoot,
                            owner =
                                this::class,
                            operation =
                                this::executeEpisode,
                            argument =
                                episode,
                            onResult = { observed ->
                                onEpisodeResult(
                                    episode =
                                        episode,
                                    observed =
                                        observed,
                                )
                            },
                            onFailure = { failure ->
                                trace +=
                                    "runner:failure:" +
                                        (
                                            failure::class.simpleName
                                                ?: "Unknown"
                                        )
                            },
                        )

                    VexBehaviorV2BeginMode.Sequenced ->
                        VexExecution.beginSequenced(
                            executionRoot =
                                executionRoot,
                            owner =
                                this::class,
                            operation =
                                this::executeEpisode,
                            argument =
                                episode,
                            onResult = { observed ->
                                onEpisodeResult(
                                    episode =
                                        episode,
                                    observed =
                                        observed,
                                )
                            },
                            onFailure = { failure ->
                                trace +=
                                    "runner:failure:" +
                                        (
                                            failure::class.simpleName
                                                ?: "Unknown"
                                        )
                            },
                        )
                }

            synchronized(
                lock,
            ) {
                handles +=
                    handle

                if (
                    mode ==
                        VexBehaviorV2BeginMode.Replaceable
                ) {
                    if (
                        activeReplaceableRevision ==
                        episode.compoundRevision
                    ) {
                        activeReplaceableHandle =
                            handle
                    } else {
                        handle.forceStop(
                            "Output was superseded before execution custody completed",
                        )
                    }
                }
            }
        }

        private suspend fun executeEpisode(
            episode:
                ActEpisode,
        ):
            String? {
            /*
             * Replaceable currentness is tied to the latest OUTPUT-CHANGING commit, not the latest
             * Compound revision. A newer input-only revision does not invalidate this consequence.
             *
             * Sequenced episodes use accepted queue custody instead: an older accepted episode may
             * still execute after newer relationship Output has been committed.
             */
            if (
                mode ==
                    VexBehaviorV2BeginMode.Replaceable &&
                latestOutputCommitRevision !=
                    episode.compoundRevision
            ) {
                trace +=
                    "runner:stale:" +
                        episode.compoundRevision
                return null
            }

            return targetOperation.apply(
                episode.value,
            )
        }

        private fun onEpisodeResult(
            episode:
                ActEpisode,
            observed:
                String?,
        ) {
            if (
                mode ==
                VexBehaviorV2BeginMode.Replaceable
            ) {
                synchronized(
                    lock,
                ) {
                    if (
                        activeReplaceableRevision ==
                        episode.compoundRevision
                    ) {
                        activeReplaceableRevision =
                            null
                        activeReplaceableHandle =
                            null
                    }
                }
            }

            if (
                observed ==
                null
            ) {
                return
            }

            accept(
                relationship.actualOutputCommitted(
                    observed,
                ),
            )
        }

        suspend fun awaitAll() {
            while (
                true
            ) {
                val snapshot =
                    synchronized(
                        lock,
                    ) {
                        handles.toList()
                    }

                snapshot.forEach { handle ->
                    handle.awaitCompletion()
                }

                val complete =
                    synchronized(
                        lock,
                    ) {
                        handles.size ==
                            snapshot.size
                    }

                if (
                    complete
                ) {
                    return
                }
            }
        }
    }

    /**
     * Act/realization side of the circuit.
     *
     * It receives a CompoundState only AFTER relationship processing has committed it. If the output
     * requests another owner to change, the Act addresses that owner. The resulting owner truth then
     * returns as a fresh InputState observation; OutputState itself is never reused as process input.
     */
    private class DesiredActualCircuit(
        private val relationship:
            DesiredActualConvergenceRelay,
        private val targetOwner:
            TargetOwnerRelay,
        private val trace:
            MutableList<String>,
    ) {
        fun desiredChanged(
            value: String,
        ) {
            val committed =
                relationship
                    .desiredChanged(
                        value,
                    )

            afterCommit(
                committed,
            )
        }

        private fun afterCommit(
            committed:
                VexCompoundState<
                    DesiredActualInputState,
                    DesiredActualOutputState
                >,
        ) {
            check(
                relationship.current ==
                    committed
            ) {
                "Act must receive the relationship's already-committed current CompoundState"
            }

            trace +=
                "commit:" +
                    committed
                        .outputState
                        .traceValue()

            act(
                committed.outputState,
            )
        }

        private fun act(
            outputState:
                DesiredActualOutputState,
        ) {
            trace +=
                "act:" +
                    outputState
                        .traceValue()

            when (
                outputState
            ) {
                DesiredActualOutputState.Unobserved,
                is DesiredActualOutputState.Settled,
                ->
                    Unit

                is DesiredActualOutputState.NeedsApply -> {
                    targetOwner.apply(
                        outputState.value,
                    )

                    /*
                     * This models observation crossing the ownership boundary:
                     *
                     * target owner CURRENT
                     * -> NEW relationship InputState
                     * -> process again
                     *
                     * Not:
                     * previous OutputState -> process input.
                     */
                    val nextCommitted =
                        relationship
                            .actualChanged(
                                requireNotNull(
                                    targetOwner
                                        .current
                                        .value,
                                ),
                            )

                    afterCommit(
                        nextCommitted,
                    )
                }
            }
        }
    }

    private fun DesiredActualOutputState.traceValue():
        String =
        when (
            this
        ) {
            DesiredActualOutputState.Unobserved ->
                "Unobserved"

            is DesiredActualOutputState.NeedsApply ->
                "NeedsApply(" +
                    value +
                    ")"

            is DesiredActualOutputState.Settled ->
                "Settled(" +
                    value +
                    ")"
        }

    private data class NestedChildInputState(
        val parentOrder:
            List<String>? = null,
        val childCurrentByRef:
            Map<
                String,
                ChildCurrentState
            >? = null,
    ) :
        LatestInputState<
            NestedChildInputState
        > {
        override fun latest(
            incoming:
                NestedChildInputState,
        ):
            NestedChildInputState =
            copy(
                parentOrder =
                    incoming.parentOrder
                        ?: parentOrder,
                childCurrentByRef =
                    incoming.childCurrentByRef
                        ?: childCurrentByRef,
            )
                .deepCopy()

        override fun deepCopy():
            NestedChildInputState =
            copy(
                parentOrder =
                    parentOrder?.toList(),
                childCurrentByRef =
                    childCurrentByRef
                        ?.mapValues { (_, child) ->
                            child.copy()
                        },
            )
    }

    private data class ChildCurrentState(
        val childRef: String,
        val label: String,
    )

    private data class NestedChildOutputState(
        val labels:
            List<String> =
            emptyList(),
    ) :
        VexOutputState<
            NestedChildOutputState
        > {
        override fun deepCopy():
            NestedChildOutputState =
            copy(
                labels =
                    labels.toList(),
            )
    }

    private class NestedChildConvergenceRelay :
        LatestInputCompoundRelay<
            NestedChildInputState,
            NestedChildOutputState
        >(
            relayRef =
                "origin-pressure.family.state-relay.nested-child-convergence",
            initialInputState =
                NestedChildInputState(),
            initialOutputState =
                NestedChildOutputState(),
        ) {
        fun parentOrderChanged(
            refs:
                List<String>,
        ) {
            send(
                NestedChildInputState(
                    parentOrder =
                        refs.toList(),
                ),
            )
        }

        fun childCurrentChanged(
            currentByRef:
                Map<
                    String,
                    ChildCurrentState
                >,
        ) {
            send(
                NestedChildInputState(
                    childCurrentByRef =
                        currentByRef
                            .mapValues { (_, child) ->
                                child.copy()
                            },
                ),
            )
        }

        override fun process(
            latestStateInput:
                NestedChildInputState,
        ):
            NestedChildOutputState {
            val currentOrder =
                latestStateInput.parentOrder
            val currentChildren =
                latestStateInput
                    .childCurrentByRef

            if (
                currentOrder ==
                null ||
                currentChildren ==
                null
            ) {
                return NestedChildOutputState()
            }

            return NestedChildOutputState(
                labels =
                    currentOrder.map { childRef ->
                        checkNotNull(
                            currentChildren[
                                childRef
                            ],
                        ) {
                            "current parent list ref $childRef has no current child State"
                        }.label
                    },
            )
        }
    }
}

// [VXG RealForever]

/**
 * Construction-teaching suite for the candidate Family Circuit Architecture.
 *
 * This is deliberately a SAMPLE SUITE, not a product feature and not a completed template to copy
 * mechanically. Read the tests from top to bottom. Every step introduces one additional ownership
 * responsibility so a fresh human or AI can see WHY the full circuit has the shape it does.
 *
 * The simulated domain uses three independently-owned CURRENT truths:
 *
 *     ScreenState
 *     MusicQueueState
 *     ThemeModeState
 *
 * They stay outside [ComplexFamily]. ComplexFamily does not absorb their ownership. It receives
 * snapshots of those truths as partial input observations and answers one question:
 *
 *     "What does the complete latest relationship mean?"
 *
 * The progression is:
 *
 *     STEP 1  rightful owners keep their own CURRENT State
 *     STEP 2  ComplexInputState.latest(...) forms one complete current input
 *     STEP 3  ComplexFamily.process(...) calculates relationship meaning only
 *     STEP 4  an Output-changing Compound commit becomes an Act consequence
 *     STEP 5  Act asks the rightful owner to change; observation returns as NEW input
 *     STEP 6  input-only currentness changes do not fabricate a second Act consequence
 *
 * Important naming note:
 *
 *     VexCompoundState<ComplexInputState, ComplexOutputState>
 *
 * already IS the Compound State container. The sample intentionally does not invent another
 * ComplexCompoundState wrapper, because feature authors should own Input / Output meaning rather
 * than duplicate framework assembly.
 */
class VexBehaviorV2FamilyCircuitTestSampleSuite {
    @Test
    fun step1_independentOwnersKeepTheirOwnCurrentTruth() {
        val callerGroups =
            mutableListOf(
                QueueGroup(
                    groupName =
                        "Up Next",
                    items =
                        mutableListOf(
                            QueueItem(
                                trackRef =
                                    "track.alpha",
                                title =
                                    "Alpha",
                            ),
                            QueueItem(
                                trackRef =
                                    "track.beta",
                                title =
                                    "Beta",
                            ),
                        ),
                ),
            )

        val screenOwner =
            ScreenOwner(
                initial =
                    ScreenState(
                        screenName =
                            "BrowseSurface",
                        transientBlocked =
                            false,
                    ),
            )
        val queueOwner =
            MusicQueueOwner(
                initial =
                    MusicQueueState(
                        groups =
                            callerGroups,
                        currentTrackRef =
                            "track.alpha",
                    ),
            )
        val themeOwner =
            ThemeModeOwner(
                initial =
                    ThemeModeState(
                        mode =
                            SampleThemeMode.Dark,
                        followsSystem =
                            false,
                    ),
            )

        /*
         * Each owner exposes its own CURRENT truth. ComplexFamily does not exist yet.
         */
        assertEquals(
            "BrowseSurface",
            screenOwner.current.screenName,
        )
        assertEquals(
            "track.alpha",
            queueOwner.current.currentTrackRef,
        )
        assertEquals(
            SampleThemeMode.Dark,
            themeOwner.current.mode,
        )

        /*
         * MusicQueueState deliberately contains a mutable nested list so the ownership rule is
         * visible. The caller may mutate its original containers after construction without changing
         * the queue owner's retained CURRENT truth.
         */
        callerGroups
            .first()
            .items
            .clear()
        callerGroups +=
            QueueGroup(
                groupName =
                    "Caller Mutation",
                items =
                    mutableListOf(),
            )

        assertEquals(
            listOf(
                "track.alpha",
                "track.beta",
            ),
            queueOwner
                .current
                .allTrackRefs(),
        )
    }

    @Test
    fun step2_latestBuildsOneCompleteCurrentInputFromPartialObservations() {
        val retainedCurrentInput =
            ComplexInputState(
                screen =
                    ScreenState(
                        screenName =
                            "BrowseSurface",
                        transientBlocked =
                            false,
                    ),
                musicQueue =
                    musicQueue(
                        currentTrackRef =
                            "track.alpha",
                        "track.alpha" to "Alpha",
                    ),
                themeMode =
                    ThemeModeState(
                        mode =
                            SampleThemeMode.Light,
                        followsSystem =
                            true,
                    ),
            )

        val incomingQueueGroups =
            mutableListOf(
                QueueGroup(
                    groupName =
                        "Replacement",
                    items =
                        mutableListOf(
                            QueueItem(
                                trackRef =
                                    "track.beta",
                                title =
                                    "Beta",
                            ),
                        ),
                ),
            )

        /*
         * Only the queue changed. The incoming value is intentionally partial.
         */
        val latestStateInput =
            retainedCurrentInput.latest(
                ComplexInputState(
                    musicQueue =
                        MusicQueueState(
                            groups =
                                incomingQueueGroups,
                            currentTrackRef =
                                "track.beta",
                        ),
                ),
            )

        /*
         * latest(...) owns partial-input -> complete-current-input formation.
         *
         * It is NOT relationship calculation. It does not decide whether navigation should happen or
         * what the output consequence is. It only guarantees process(...) receives everything CURRENT.
         */
        assertEquals(
            "BrowseSurface",
            latestStateInput
                .screen
                ?.screenName,
        )
        assertEquals(
            "track.beta",
            latestStateInput
                .musicQueue
                ?.currentTrackRef,
        )
        assertEquals(
            SampleThemeMode.Light,
            latestStateInput
                .themeMode
                ?.mode,
        )

        /*
         * latest(...) also owns the input snapshot boundary. Mutation of the incoming nested queue
         * after currentization cannot mutate the completed latest input.
         */
        incomingQueueGroups
            .first()
            .items
            .clear()

        assertEquals(
            listOf(
                "track.beta",
            ),
            latestStateInput
                .musicQueue
                ?.allTrackRefs(),
        )
    }

    @Test
    fun step3_complexFamilyProcessReadsCompleteInputAndReturnsOnlyRelationshipMeaning() {
        val trace =
            mutableListOf<String>()
        val family =
            ComplexFamily(
                trace =
                    trace,
            )

        /*
         * Each observation is partial. The reusable Compound foundation currentizes it before
         * ComplexFamily.process(...) is called.
         */
        family.screenChanged(
            ScreenState(
                screenName =
                    "BrowseSurface",
                transientBlocked =
                    false,
            ),
        )
        assertEquals(
            ComplexOutputState.Unobserved,
            family.currentOutput,
        )

        family.musicQueueChanged(
            musicQueue(
                currentTrackRef =
                    "track.alpha",
                "track.alpha" to "Alpha",
            ),
        )
        assertEquals(
            ComplexOutputState.Unobserved,
            family.currentOutput,
        )

        val committed =
            family.themeModeChanged(
                ThemeModeState(
                    mode =
                        SampleThemeMode.Dark,
                    followsSystem =
                        false,
                ),
            )

        /*
         * process(...) sees all three CURRENT inputs together and returns only relationship meaning.
         * It does not mutate ScreenOwner, MusicQueueOwner or ThemeModeOwner.
         */
        assertEquals(
            ComplexOutputState.NeedsPlayerScreen(
                trackRef =
                    "track.alpha",
                targetScreenName =
                    PLAYER_SCREEN,
                themeMode =
                    SampleThemeMode.Dark,
            ),
            committed.outputState,
        )

        assertEquals(
            "BrowseSurface",
            committed
                .inputState
                .screen
                ?.screenName,
        )
        assertEquals(
            "track.alpha",
            committed
                .inputState
                .musicQueue
                ?.currentTrackRef,
        )
        assertEquals(
            SampleThemeMode.Dark,
            committed
                .inputState
                .themeMode
                ?.mode,
        )
    }

    @Test
    fun step4_and_5_commitThenActThenObserveNewOwnerTruthBackAsInput() {
        val trace =
            mutableListOf<String>()
        val screenOwner =
            ScreenOwner(
                initial =
                    ScreenState(
                        screenName =
                            "BrowseSurface",
                        transientBlocked =
                            false,
                    ),
                trace =
                    trace,
            )
        val queueOwner =
            MusicQueueOwner(
                initial =
                    musicQueue(
                        currentTrackRef =
                            "track.alpha",
                        "track.alpha" to "Alpha",
                    ),
            )
        val themeOwner =
            ThemeModeOwner(
                initial =
                    ThemeModeState(
                        mode =
                            SampleThemeMode.Dark,
                        followsSystem =
                            false,
                    ),
            )
        val family =
            ComplexFamily(
                trace =
                    trace,
            )
        val circuit =
            ComplexFamilyCircuit(
                family =
                    family,
                screenOwner =
                    screenOwner,
                trace =
                    trace,
            )

        /*
         * Observe each rightful owner. The first two observations leave Output Unobserved. The final
         * observation completes the input and creates the first Output-changing commit.
         */
        circuit.accept(
            family.screenOutputCommitted(
                screenOwner.current,
            ),
        )
        circuit.accept(
            family.musicQueueOutputCommitted(
                queueOwner.current,
            ),
        )
        circuit.accept(
            family.themeModeOutputCommitted(
                themeOwner.current,
            ),
        )

        /*
         * The first committed consequence requested PlayerSurface. Act asked ScreenOwner to change.
         * ScreenOwner then owned the new truth. The circuit OBSERVED screenOwner.current and fed that
         * value back as NEW input; process(...) then derived Settled.
         */
        assertEquals(
            PLAYER_SCREEN,
            screenOwner.current.screenName,
        )
        assertEquals(
            ComplexOutputState.Settled(
                screenName =
                    PLAYER_SCREEN,
                currentTrackRef =
                    "track.alpha",
                themeMode =
                    SampleThemeMode.Dark,
            ),
            family.currentOutput,
        )

        assertEquals(
            listOf(
                "process:Unobserved",
                "process:Unobserved",
                "process:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "commit:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "act:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "screen-owner:PlayerSurface",
                "observe:screen:PlayerSurface",
                "process:Settled(PlayerSurface,track.alpha,Dark)",
                "commit:Settled(PlayerSurface,track.alpha,Dark)",
                "act:Settled(PlayerSurface,track.alpha,Dark)",
            ),
            trace,
        )
    }

    @Test
    fun step6_inputOnlyCurrentnessChangeDoesNotCreateAnotherActConsequence() {
        val trace =
            mutableListOf<String>()
        val family =
            ComplexFamily(
                trace =
                    trace,
            )

        family.screenOutputCommitted(
            ScreenState(
                screenName =
                    "BrowseSurface",
                transientBlocked =
                    false,
            ),
        )
        family.musicQueueOutputCommitted(
            musicQueue(
                currentTrackRef =
                    "track.alpha",
                "track.alpha" to "Old title",
            ),
        )

        val firstConsequence =
            family.themeModeOutputCommitted(
                ThemeModeState(
                    mode =
                        SampleThemeMode.Dark,
                    followsSystem =
                        false,
                ),
            )

        val firstRevision =
            checkNotNull(
                firstConsequence,
            ).compoundRevision

        /*
         * The queue title is CURRENT input truth, so the Compound revision advances.
         *
         * But the relationship still means exactly NeedsPlayerScreen(track.alpha, PlayerSurface,
         * Dark). Because Output meaning did not change, this is NOT a second Act consequence.
         */
        val duplicateConsequence =
            family.musicQueueOutputCommitted(
                musicQueue(
                    currentTrackRef =
                        "track.alpha",
                    "track.alpha" to "New title",
                ),
            )

        assertEquals(
            null,
            duplicateConsequence,
        )
        assertEquals(
            firstRevision +
                1L,
            family.current.revision,
        )
        assertEquals(
            ComplexOutputState.NeedsPlayerScreen(
                trackRef =
                    "track.alpha",
                targetScreenName =
                    PLAYER_SCREEN,
                themeMode =
                    SampleThemeMode.Dark,
            ),
            family.currentOutput,
        )
    }

    /**
     * STEP 1 — three unrelated owners.
     *
     * These types intentionally live OUTSIDE ComplexFamily. They are complete current truths in
     * their own right. ComplexFamily may observe them; it does not become their owner.
     */
    private data class ScreenState(
        val screenName:
            String,
        val transientBlocked:
            Boolean,
    ) :
        VexState<
            ScreenState
        > {
        override fun deepCopy():
            ScreenState =
            copy()
    }

    private data class QueueItem(
        val trackRef:
            String,
        val title:
            String,
    )

    private data class QueueGroup(
        val groupName:
            String,
        val items:
            MutableList<
                QueueItem
            >,
    ) {
        fun deepCopy():
            QueueGroup =
            copy(
                items =
                    items
                        .map { item ->
                            item.copy()
                        }
                        .toMutableList(),
            )
    }

    private data class MusicQueueState(
        val groups:
            MutableList<
                QueueGroup
            >,
        val currentTrackRef:
            String?,
    ) :
        VexState<
            MusicQueueState
        > {
        /*
         * This deepCopy() is deliberately non-trivial. It demonstrates the ownership boundary for a
         * mutable nested list without forcing process(...) to repeat defensive-copy ceremony.
         */
        override fun deepCopy():
            MusicQueueState =
            copy(
                groups =
                    groups
                        .map { group ->
                            group.deepCopy()
                        }
                        .toMutableList(),
            )

        fun currentTrack():
            QueueItem? =
            currentTrackRef
                ?.let { ref ->
                    groups
                        .asSequence()
                        .flatMap { group ->
                            group
                                .items
                                .asSequence()
                        }
                        .firstOrNull { item ->
                            item.trackRef ==
                                ref
                        }
                        ?.copy()
                }

        fun allTrackRefs():
            List<String> =
            groups
                .flatMap { group ->
                    group
                        .items
                        .map { item ->
                            item.trackRef
                        }
                }
    }

    private enum class SampleThemeMode {
        Light,
        Dark,
    }

    private data class ThemeModeState(
        val mode:
            SampleThemeMode,
        val followsSystem:
            Boolean,
    ) :
        VexState<
            ThemeModeState
        > {
        override fun deepCopy():
            ThemeModeState =
            copy()
    }

    private class ScreenOwner(
        initial:
            ScreenState,
        private val trace:
            MutableList<String>? =
            null,
    ) :
        VexRelay<
            ScreenState
        >(
            relayRef =
                "sample.family-circuit.screen-owner",
            initialState =
                initial,
        ) {
        fun apply(
            screenName:
                String,
        ):
            ScreenState {
            val next =
                send(
                    current.copy(
                        screenName =
                            screenName,
                    ),
                )

            trace?.add(
                "screen-owner:" +
                    next.screenName,
            )

            return next
        }
    }

    private class MusicQueueOwner(
        initial:
            MusicQueueState,
    ) :
        VexRelay<
            MusicQueueState
        >(
            relayRef =
                "sample.family-circuit.music-queue-owner",
            initialState =
                initial,
        )

    private class ThemeModeOwner(
        initial:
            ThemeModeState,
    ) :
        VexRelay<
            ThemeModeState
        >(
            relayRef =
                "sample.family-circuit.theme-mode-owner",
            initialState =
                initial,
        )

    /**
     * STEP 2 — Input owns CURRENT fact formation.
     *
     * Every field is nullable because one observation may carry only one changed owner. latest(...)
     * merges that partial observation with retained input so process(...) never has to guess which
     * values are old, new or absent from this patch.
     */
    private interface LatestInputState<
        Self : LatestInputState<
            Self
        >,
    > :
        VexInputState<
            Self
        > {
        fun latest(
            incoming:
                Self,
        ):
            Self
    }

    private data class ComplexInputState(
        val screen:
            ScreenState? =
            null,
        val musicQueue:
            MusicQueueState? =
            null,
        val themeMode:
            ThemeModeState? =
            null,
    ) :
        LatestInputState<
            ComplexInputState
        > {
        override fun latest(
            incoming:
                ComplexInputState,
        ):
            ComplexInputState =
            ComplexInputState(
                screen =
                    (
                        incoming.screen
                            ?: screen
                    )?.deepCopy(),
                musicQueue =
                    (
                        incoming.musicQueue
                            ?: musicQueue
                    )?.deepCopy(),
                themeMode =
                    (
                        incoming.themeMode
                            ?: themeMode
                    )?.deepCopy(),
            )

        override fun deepCopy():
            ComplexInputState =
            ComplexInputState(
                screen =
                    screen?.deepCopy(),
                musicQueue =
                    musicQueue?.deepCopy(),
                themeMode =
                    themeMode?.deepCopy(),
            )
    }

    /**
     * STEP 3 — Output owns CONSEQUENCE, never previous-process memory.
     */
    private sealed interface ComplexOutputState :
        VexOutputState<
            ComplexOutputState
        > {
        data object Unobserved :
            ComplexOutputState

        data class HeldTransient(
            val screenName:
                String,
        ) :
            ComplexOutputState

        data class NeedsPlayerScreen(
            val trackRef:
                String,
            val targetScreenName:
                String,
            val themeMode:
                SampleThemeMode,
        ) :
            ComplexOutputState

        data class Settled(
            val screenName:
                String,
            val currentTrackRef:
                String?,
            val themeMode:
                SampleThemeMode,
        ) :
            ComplexOutputState

        override fun deepCopy():
            ComplexOutputState =
            when (
                this
            ) {
                Unobserved ->
                    Unobserved

                is HeldTransient ->
                    copy()

                is NeedsPlayerScreen ->
                    copy()

                is Settled ->
                    copy()
            }
    }

    /**
     * STEP 3 infrastructure — reusable process-only Compound formation.
     *
     * Concrete relationships do not re-author:
     *
     *     partial input merging
     *     current Compound assembly
     *
     * They author only process(latestStateInput).
     */
    private abstract class ProcessOnlyCompoundRelationship<
        InputState,
        OutputState,
    >(
        relayRef:
            String,
        initialInputState:
            InputState,
        initialOutputState:
            OutputState,
    ) :
        VexCompoundRelay<
            InputState,
            OutputState
        >(
            relayRef =
                relayRef,
            initialInputState =
                initialInputState,
            initialOutputState =
                initialOutputState,
        )
        where InputState :
            LatestInputState<
                InputState
            >,
              OutputState :
            VexOutputState<
                OutputState
            > {
        final override fun processStates(
            inputState:
                InputState,
            currentState:
                VexCompoundState<
                    InputState,
                    OutputState
                >,
        ):
            VexCompoundState<
                InputState,
                OutputState
            > {
            /*
             * OVERRIDE 1 — framework plumbing:
             *
             * current retained input + incoming partial observation
             * -> one complete latestStateInput
             */
            val latestStateInput =
                currentState
                    .inputState
                    .latest(
                        inputState,
                    )

            /*
             * OVERRIDE 2 — the ONLY concrete relationship calculation:
             *
             * here is everything CURRENT
             * -> what does it mean?
             * -> here is the Output
             */
            val latestStateOutput =
                process(
                    latestStateInput,
                )

            /*
             * OVERRIDE 3 — generic Compound reassembly.
             *
             * The concrete relationship does not need to repeat this.
             */
            return currentState.copy(
                inputState =
                    latestStateInput,
                outputState =
                    latestStateOutput,
            )
        }

        protected abstract fun process(
            latestStateInput:
                InputState,
        ):
            OutputState

        protected fun receive(
            inputState:
                InputState,
        ):
            VexCompoundState<
                InputState,
                OutputState
            > =
            send(
                inputState,
            )

        /*
         * STEP 4 teaching seam.
         *
         * This wrapper exists ONLY so the sample can teach "Output changed after commit -> Act".
         * It is not the accepted production seam: dependency-driven send(...) could bypass an outer
         * wrapper. Production still needs the smallest post-commit hook at the atomic Relay boundary.
         */
        private val outputCommitLock =
            Any()

        protected fun receiveOutputCommit(
            inputState:
                InputState,
        ):
            CommittedOutput<
                OutputState
            >? =
            synchronized(
                outputCommitLock,
            ) {
                val beforeOutput =
                    currentOutput
                val committed =
                    send(
                        inputState,
                    )

                if (
                    committed.outputState ==
                    beforeOutput
                ) {
                    null
                } else {
                    CommittedOutput(
                        compoundRevision =
                            committed.revision,
                        outputState =
                            committed
                                .outputState
                                .deepCopy(),
                    )
                }
            }
    }

    private data class CommittedOutput<
        OutputState,
    >(
        val compoundRevision:
            Long,
        val outputState:
            OutputState,
    ) where OutputState :
        VexOutputState<
            OutputState
        >

    /**
     * The first deliberately-complex Family Circuit relationship.
     *
     * Important:
     *
     *     ComplexFamily does NOT own ScreenState.
     *     ComplexFamily does NOT own MusicQueueState.
     *     ComplexFamily does NOT own ThemeModeState.
     *
     * Its Input contains observed snapshots of those independent CURRENT truths.
     *
     * Its only feature-specific override is process(latestStateInput).
     */
    private class ComplexFamily(
        private val trace:
            MutableList<String>,
    ) :
        ProcessOnlyCompoundRelationship<
            ComplexInputState,
            ComplexOutputState
        >(
            relayRef =
                "sample.family-circuit.complex-family",
            initialInputState =
                ComplexInputState(),
            initialOutputState =
                ComplexOutputState.Unobserved,
        ) {
        fun screenChanged(
            state:
                ScreenState,
        ):
            VexCompoundState<
                ComplexInputState,
                ComplexOutputState
            > =
            receive(
                ComplexInputState(
                    screen =
                        state,
                ),
            )

        fun musicQueueChanged(
            state:
                MusicQueueState,
        ):
            VexCompoundState<
                ComplexInputState,
                ComplexOutputState
            > =
            receive(
                ComplexInputState(
                    musicQueue =
                        state,
                ),
            )

        fun themeModeChanged(
            state:
                ThemeModeState,
        ):
            VexCompoundState<
                ComplexInputState,
                ComplexOutputState
            > =
            receive(
                ComplexInputState(
                    themeMode =
                        state,
                ),
            )

        fun screenOutputCommitted(
            state:
                ScreenState,
        ):
            CommittedOutput<
                ComplexOutputState
            >? =
            receiveOutputCommit(
                ComplexInputState(
                    screen =
                        state,
                ),
            )
                .also(::traceCommit)

        fun musicQueueOutputCommitted(
            state:
                MusicQueueState,
        ):
            CommittedOutput<
                ComplexOutputState
            >? =
            receiveOutputCommit(
                ComplexInputState(
                    musicQueue =
                        state,
                ),
            )
                .also(::traceCommit)

        fun themeModeOutputCommitted(
            state:
                ThemeModeState,
        ):
            CommittedOutput<
                ComplexOutputState
            >? =
            receiveOutputCommit(
                ComplexInputState(
                    themeMode =
                        state,
                ),
            )
                .also(::traceCommit)

        /**
         * This is the line fresh readers should be able to find immediately.
         *
         * No currentState parameter.
         * No previousOutput parameter.
         * No target mutation.
         * No Effect execution.
         *
         * Just:
         *
         *     here is everything CURRENT
         *     -> what does it mean?
         *     -> return the consequence
         */
        override fun process(
            latestStateInput:
                ComplexInputState,
        ):
            ComplexOutputState {
            val screen =
                latestStateInput.screen
            val queue =
                latestStateInput.musicQueue
            val theme =
                latestStateInput.themeMode

            val output =
                when {
                    screen ==
                        null ||
                        queue ==
                        null ||
                        theme ==
                        null ->
                        ComplexOutputState
                            .Unobserved

                    screen.transientBlocked ->
                        ComplexOutputState
                            .HeldTransient(
                                screenName =
                                    screen.screenName,
                            )

                    queue.currentTrack() !=
                        null &&
                        screen.screenName !=
                        PLAYER_SCREEN ->
                        ComplexOutputState
                            .NeedsPlayerScreen(
                                trackRef =
                                    checkNotNull(
                                        queue.currentTrack(),
                                    ).trackRef,
                                targetScreenName =
                                    PLAYER_SCREEN,
                                themeMode =
                                    theme.mode,
                            )

                    else ->
                        ComplexOutputState
                            .Settled(
                                screenName =
                                    screen.screenName,
                                currentTrackRef =
                                    queue
                                        .currentTrack()
                                        ?.trackRef,
                                themeMode =
                                    theme.mode,
                            )
                }

            trace +=
                "process:" +
                    output.traceValue()

            return output
        }

        private fun traceCommit(
            committed:
                CommittedOutput<
                    ComplexOutputState
                >?,
        ) {
            if (
                committed !=
                null
            ) {
                trace +=
                    "commit:" +
                        committed
                            .outputState
                            .traceValue()
            }
        }
    }

    /**
     * STEPS 4 + 5 — realization after established relationship truth.
     *
     * Act receives an ALREADY-COMMITTED Output. If that output asks for a screen change, Act calls
     * ScreenOwner. It never writes ComplexFamily Output directly.
     *
     * After ScreenOwner changes, the circuit observes ScreenOwner.current and returns it as NEW
     * ComplexInputState. That new input goes through latest(...) and process(...) again.
     */
    private class ComplexFamilyCircuit(
        private val family:
            ComplexFamily,
        private val screenOwner:
            ScreenOwner,
        private val trace:
            MutableList<String>,
    ) {
        fun accept(
            committed:
                CommittedOutput<
                    ComplexOutputState
                >?,
        ) {
            if (
                committed ==
                null
            ) {
                return
            }

            val output =
                committed.outputState

            trace +=
                "act:" +
                    output.traceValue()

            when (
                output
            ) {
                ComplexOutputState.Unobserved,
                is ComplexOutputState.HeldTransient,
                is ComplexOutputState.Settled,
                ->
                    Unit

                is ComplexOutputState.NeedsPlayerScreen -> {
                    screenOwner.apply(
                        output.targetScreenName,
                    )

                    val observed =
                        screenOwner.current

                    trace +=
                        "observe:screen:" +
                            observed.screenName

                    /*
                     * Observation re-enters as NEW input. We recursively accept only if that new input
                     * creates another Output-changing committed consequence.
                     */
                    accept(
                        family.screenOutputCommitted(
                            observed,
                        ),
                    )
                }
            }
        }
    }

    private fun ComplexOutputState.traceValue():
        String =
        when (
            this
        ) {
            ComplexOutputState.Unobserved ->
                "Unobserved"

            is ComplexOutputState.HeldTransient ->
                "HeldTransient(" +
                    screenName +
                    ")"

            is ComplexOutputState.NeedsPlayerScreen ->
                "NeedsPlayerScreen(" +
                    trackRef +
                    "," +
                    targetScreenName +
                    "," +
                    themeMode.name +
                    ")"

            is ComplexOutputState.Settled ->
                "Settled(" +
                    screenName +
                    "," +
                    (
                        currentTrackRef
                            ?: "none"
                    ) +
                    "," +
                    themeMode.name +
                    ")"
        }

    private fun musicQueue(
        currentTrackRef:
            String?,
        vararg tracks:
            Pair<
                String,
                String
            >,
    ):
        MusicQueueState =
        MusicQueueState(
            groups =
                mutableListOf(
                    QueueGroup(
                        groupName =
                            "Teaching Queue",
                        items =
                            tracks
                                .map { (
                                    trackRef,
                                    title,
                                ) ->
                                    QueueItem(
                                        trackRef =
                                            trackRef,
                                        title =
                                            title,
                                    )
                                }
                                .toMutableList(),
                    ),
                ),
            currentTrackRef =
                currentTrackRef,
        )

    private companion object {
        const val PLAYER_SCREEN:
            String =
            "PlayerSurface"
    }
}

