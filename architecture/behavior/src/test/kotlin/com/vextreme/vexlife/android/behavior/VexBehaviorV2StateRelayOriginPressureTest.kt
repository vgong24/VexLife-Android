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
    fun nestedCurrentListsPreserveEveryLayerByStableIdentity() {
        val relay =
            NestedListStateRelay(
                initialState =
                    NestedListState.fixture(),
            )

        relay.updateItem(
            sectionRef =
                "section.alpha",
            itemRef =
                "item.alpha.two",
        ) { item ->
            item.copy(
                label =
                    "Alpha Two — updated",
            )
        }

        assertEquals(
            listOf(
                "section.alpha",
                "section.beta",
            ),
            relay.current.sections.refs,
        )
        assertEquals(
            listOf(
                "item.alpha.one",
                "item.alpha.two",
            ),
            relay.current
                .sections[
                    "section.alpha"
                ]
                .items
                .refs,
        )
        assertEquals(
            listOf(
                "Alpha One",
                "Alpha Two — updated",
                "Beta One",
            ),
            relay.current.displayLabels(),
        )
    }

    @Test
    fun nestedCurrentSnapshotDetachesCallerOwnedMutableCollectionsWithoutLosingLayers() {
        val itemRefs =
            mutableListOf(
                "item.alpha.one",
                "item.alpha.two",
            )
        val itemValues =
            mutableMapOf(
                "item.alpha.one" to
                    NestedItemState(
                        itemRef =
                            "item.alpha.one",
                        label =
                            "Alpha One",
                    ),
                "item.alpha.two" to
                    NestedItemState(
                        itemRef =
                            "item.alpha.two",
                        label =
                            "Alpha Two",
                    ),
            )
        val sectionRefs =
            mutableListOf(
                "section.alpha",
            )
        val sectionValues =
            mutableMapOf(
                "section.alpha" to
                    NestedSectionState(
                        sectionRef =
                            "section.alpha",
                        items =
                            StateListSnapshot(
                                refs =
                                    itemRefs,
                                valuesByRef =
                                    itemValues,
                            ),
                    ),
            )

        val relay =
            NestedListStateRelay(
                initialState =
                    NestedListState(
                        sections =
                            StateListSnapshot(
                                refs =
                                    sectionRefs,
                                valuesByRef =
                                    sectionValues,
                            ),
                    ),
            )

        itemRefs.clear()
        itemValues.clear()
        sectionRefs.clear()
        sectionValues.clear()

        assertEquals(
            listOf(
                "section.alpha",
            ),
            relay.current.sections.refs,
        )
        assertEquals(
            listOf(
                "item.alpha.one",
                "item.alpha.two",
            ),
            relay.current
                .sections[
                    "section.alpha"
                ]
                .items
                .refs,
        )
        assertEquals(
            listOf(
                "Alpha One",
                "Alpha Two",
            ),
            relay.current.displayLabels(),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun nestedCurrentListRejectsDanglingDisplayIdentity() {
        StateListSnapshot(
            refs =
                listOf(
                    "item.present",
                    "item.missing",
                ),
            valuesByRef =
                mapOf(
                    "item.present" to
                        NestedItemState(
                            itemRef =
                                "item.present",
                            label =
                                "Present",
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
     * Identity-first current list pressure.
     *
     * refs owns traversal/order. valuesByRef must contain exactly those identities. Deep-copy
     * detachment and semantic identity are deliberately different concerns.
     */
    private data class StateListSnapshot<
        Ref,
        Value,
    >(
        val refs:
            List<Ref>,
        val valuesByRef:
            Map<
                Ref,
                Value
            >,
    ) {
        init {
            require(
                refs.distinct().size ==
                    refs.size,
            ) {
                "StateListSnapshot refs must be unique"
            }
            require(
                refs.toSet() ==
                    valuesByRef.keys,
            ) {
                "StateListSnapshot refs and valuesByRef must describe the same complete current list"
            }
        }

        operator fun get(
            ref: Ref,
        ): Value =
            requireNotNull(
                valuesByRef[
                    ref
                ],
            ) {
                "StateListSnapshot has no current value for ref $ref"
            }

        fun update(
            ref: Ref,
            transform:
                (
                    Value,
                ) ->
                    Value,
        ):
            StateListSnapshot<
                Ref,
                Value
            > {
            require(
                ref in
                    valuesByRef,
            ) {
                "StateListSnapshot cannot update unknown ref $ref"
            }

            return copy(
                refs =
                    refs.toList(),
                valuesByRef =
                    valuesByRef
                        .toMap() +
                        (
                            ref to
                                transform(
                                    get(
                                        ref,
                                    ),
                                )
                        ),
            )
        }

        fun snapshot(
            copyValue:
                (
                    Value,
                ) ->
                    Value,
        ):
            StateListSnapshot<
                Ref,
                Value
            > =
            StateListSnapshot(
                refs =
                    refs.toList(),
                valuesByRef =
                    refs.associateWith { ref ->
                        copyValue(
                            get(
                                ref,
                            ),
                        )
                    },
            )
    }

    private data class NestedItemState(
        val itemRef: String,
        val label: String,
    )

    private data class NestedSectionState(
        val sectionRef: String,
        val items:
            StateListSnapshot<
                String,
                NestedItemState
            >,
    )

    private data class NestedListState(
        val sections:
            StateListSnapshot<
                String,
                NestedSectionState
            >,
    ) :
        VexState<
            NestedListState
        > {
        override fun deepCopy():
            NestedListState =
            copy(
                sections =
                    sections.snapshot { section ->
                        section.copy(
                            items =
                                section.items.snapshot { item ->
                                    item.copy()
                                },
                        )
                    },
            )

        fun displayLabels():
            List<String> =
            sections.refs.flatMap { sectionRef ->
                val section =
                    sections[
                        sectionRef
                    ]

                section.items.refs.map { itemRef ->
                    section.items[
                        itemRef
                    ].label
                }
            }

        companion object {
            fun fixture():
                NestedListState =
                NestedListState(
                    sections =
                        StateListSnapshot(
                            refs =
                                listOf(
                                    "section.alpha",
                                    "section.beta",
                                ),
                            valuesByRef =
                                mapOf(
                                    "section.alpha" to
                                        NestedSectionState(
                                            sectionRef =
                                                "section.alpha",
                                            items =
                                                StateListSnapshot(
                                                    refs =
                                                        listOf(
                                                            "item.alpha.one",
                                                            "item.alpha.two",
                                                        ),
                                                    valuesByRef =
                                                        mapOf(
                                                            "item.alpha.one" to
                                                                NestedItemState(
                                                                    itemRef =
                                                                        "item.alpha.one",
                                                                    label =
                                                                        "Alpha One",
                                                                ),
                                                            "item.alpha.two" to
                                                                NestedItemState(
                                                                    itemRef =
                                                                        "item.alpha.two",
                                                                    label =
                                                                        "Alpha Two",
                                                                ),
                                                        ),
                                                ),
                                        ),
                                    "section.beta" to
                                        NestedSectionState(
                                            sectionRef =
                                                "section.beta",
                                            items =
                                                StateListSnapshot(
                                                    refs =
                                                        listOf(
                                                            "item.beta.one",
                                                        ),
                                                    valuesByRef =
                                                        mapOf(
                                                            "item.beta.one" to
                                                                NestedItemState(
                                                                    itemRef =
                                                                        "item.beta.one",
                                                                    label =
                                                                        "Beta One",
                                                                ),
                                                        ),
                                                ),
                                        ),
                                ),
                        ),
                )
        }
    }

    private class NestedListStateRelay(
        initialState:
            NestedListState,
    ) :
        VexRelay<
            NestedListState
        >(
            relayRef =
                "origin-pressure.family.state-relay.nested-list",
            initialState =
                initialState,
        ) {
        fun updateItem(
            sectionRef: String,
            itemRef: String,
            transform:
                (
                    NestedItemState,
                ) ->
                    NestedItemState,
        ) {
            update { current ->
                current.copy(
                    sections =
                        current.sections.update(
                            sectionRef,
                        ) { section ->
                            section.copy(
                                items =
                                    section.items.update(
                                        itemRef,
                                        transform,
                                    ),
                            )
                        },
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
