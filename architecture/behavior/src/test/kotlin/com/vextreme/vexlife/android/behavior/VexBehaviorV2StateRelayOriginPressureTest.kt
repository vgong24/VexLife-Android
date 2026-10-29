package com.vextreme.vexlife.android.behavior

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
    private data class NestedChildInputState(
        val parentOrder:
            List<String>? = null,
        val childCurrentByRef:
            Map<
                String,
                ChildCurrentState
            >? = null,
    ) :
        VexInputState<
            NestedChildInputState
        > {
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
        VexCompoundRelay<
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

        override fun processStates(
            inputState:
                NestedChildInputState,
            currentState:
                VexCompoundState<
                    NestedChildInputState,
                    NestedChildOutputState
                >,
        ):
            VexCompoundState<
                NestedChildInputState,
                NestedChildOutputState
            > {
            val previous =
                currentState.inputState

            val currentOrder =
                inputState.parentOrder
                    ?: previous.parentOrder
            val currentChildren =
                inputState.childCurrentByRef
                    ?: previous.childCurrentByRef

            val coherentInput =
                NestedChildInputState(
                    parentOrder =
                        currentOrder
                            ?.toList(),
                    childCurrentByRef =
                        currentChildren
                            ?.mapValues { (_, child) ->
                                child.copy()
                            },
                )

            val labels =
                if (
                    currentOrder ==
                    null ||
                    currentChildren ==
                    null
                ) {
                    emptyList()
                } else {
                    currentOrder.map { childRef ->
                        checkNotNull(
                            currentChildren[
                                childRef
                            ],
                        ) {
                            "current parent list ref $childRef has no current child State"
                        }.label
                    }
                }

            return currentState.copy(
                inputState =
                    coherentInput,
                outputState =
                    NestedChildOutputState(
                        labels =
                            labels,
                    ),
            )
        }
    }
}

// [VXG RealForever]
