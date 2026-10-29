package com.vextreme.vexlife.android.behavior

import java.util.ArrayDeque
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fourth V2 Origin-Walk reference: Family StateRelay / nested-currentness pressure.
 *
 * This class begins only after Playback established:
 *
 *     actual runtime State + payload Control -> materialized Act
 *     provider observation -> feature reconciliation -> semantic State proposal
 *
 * The remaining question is how one Family safely owns CURRENT semantic State when several lawful
 * causes may arrive independently, and how nested/list currentness remains complete without relying
 * on object-reference continuity.
 *
 * Nothing in this class makes StateRelay, StateListSnapshot or Compound convergence a confirmed V2
 * Family standard. Each fixture exists to expose the failure it prevents and the ownership law it
 * may justify.
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
    ) :
        VexRelay<
            TargetOwnerState
        >(
            relayRef =
                "origin-pressure.process-act.target-owner",
            initialState =
                TargetOwnerState(),
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
