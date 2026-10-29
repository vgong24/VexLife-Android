package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Small examples of the Family Circuit shapes.
 *
 * Start with the create...Family() functions near the bottom of this file.
 *
 *     SingleFamily
 *       one current State
 *
 *     ListFamily
 *       one current State with nested list data
 *
 *     SwitchFamily
 *       one current Boolean State
 *
 *     MultiFamily
 *       several Family currents
 *       -> Input.latest(...)
 *       -> process(...)
 *       -> Output
 *       -> MultiCompoundState
 *       -> Act
 *
 * The names are intentionally generic. This file teaches infrastructure, not a product feature.
 */
class VexBehaviorV2FamilyCircuitTestSampleSuite {
    @Test
    fun singleFamily() {
        val family =
            createSingleFamily(
                value =
                    "one",
            )

        family.activate(
            SingleFamily.Control.Set(
                value =
                    "two",
            ),
        )

        assertEquals(
            SingleFamily.State(
                value =
                    "two",
            ),
            family.current,
        )
    }

    @Test
    fun listFamily() {
        val family =
            createListFamily(
                "one",
                "two",
            )

        family.activate(
            ListFamily.Control.Add(
                value =
                    "three",
            ),
        )

        assertEquals(
            listOf(
                "one",
                "two",
                "three",
            ),
            family
                .current
                .values(),
        )
    }

    @Test
    fun multiFamily() {
        val trace =
            mutableListOf<String>()

        val singleFamily =
            createSingleFamily(
                value =
                    "two",
            )
        val listFamily =
            createListFamily(
                "one",
            )
        val switchFamily =
            createSwitchFamily(
                enabled =
                    true,
            )
        val multiFamily =
            createMultiFamily(
                singleFamily =
                    singleFamily,
                listFamily =
                    listFamily,
                switchFamily =
                    switchFamily,
                trace =
                    trace,
            )

        multiFamily.refresh()

        /*
         * MultiFamily saw:
         *
         *     single = "two"
         *     list   = ["one"]
         *     switch = true
         *
         * process(...) returned NeedsListValue("two").
         *
         * That Output became MultiFamily.current before act(...) asked ListFamily to add "two".
         * ListFamily then owned the new list State. MultiFamily observed that CURRENT State as NEW
         * input and processed again to Settled("two").
         */
        assertEquals(
            listOf(
                "one",
                "two",
            ),
            listFamily
                .current
                .values(),
        )

        assertEquals(
            MultiOutputState.Settled(
                value =
                    "two",
            ),
            multiFamily
                .current
                .outputState,
        )

        assertEquals(
            listOf(
                "process:Unobserved",
                "process:Unobserved",
                "process:NeedsListValue(two)",
                "commit:NeedsListValue(two)",
                "act:NeedsListValue(two)",
                "list-family:one,two",
                "observe:list",
                "process:Settled(two)",
                "commit:Settled(two)",
                "act:Settled(two)",
            ),
            trace,
        )
    }

    @Test
    fun multiFamilyDoesNotActAgainWhenInputChangesButOutputDoesNot() {
        val trace =
            mutableListOf<String>()

        val singleFamily =
            createSingleFamily(
                value =
                    "one",
            )
        val listFamily =
            createListFamily(
                "one",
            )
        val switchFamily =
            createSwitchFamily(
                enabled =
                    true,
            )
        val multiFamily =
            createMultiFamily(
                singleFamily =
                    singleFamily,
                listFamily =
                    listFamily,
                switchFamily =
                    switchFamily,
                trace =
                    trace,
            )

        multiFamily.refresh()

        val beforeRevision =
            multiFamily.current.revision
        val beforeActCount =
            trace.count { entry ->
                entry.startsWith(
                    "act:",
                )
            }

        /*
         * The nested list note changes, so Input changes.
         * The list still contains "one", so Output remains Settled("one").
         */
        listFamily.activate(
            ListFamily.Control.SetNote(
                value =
                    "one",
                note =
                    "changed",
            ),
        )

        val committed =
            multiFamily.observeList(
                listFamily.current,
            )

        assertNull(
            committed,
        )
        assertEquals(
            beforeRevision +
                1L,
            multiFamily.current.revision,
        )
        assertEquals(
            beforeActCount,
            trace.count { entry ->
                entry.startsWith(
                    "act:",
                )
            },
        )
    }

    /**
     * Simplest Control-driven Family.
     */
    private class SingleFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            SingleFamily,
            SingleFamily.State,
            SingleFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        data class State(
            val value:
                String,
        ) :
            VexBehaviorFamily.State<
                SingleFamily
            >

        sealed interface Control :
            VexBehaviorFamily.Control<
                SingleFamily
            > {
            data class Set(
                val value:
                    String,
            ) :
                Control
        }

        override val acts =
            vexActs<
                SingleFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "sample.family-circuit.single",
            ) {
                state<State> {
                    on<Control.Set> { current, control ->
                        admit(
                            current.copy(
                                value =
                                    control.value,
                            ),
                        )
                    }
                }
            }
    }

    /**
     * Control-driven Family with nested list State.
     */
    private class ListFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            ListFamily,
            ListFamily.State,
            ListFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        data class Item(
            val value:
                String,
            val note:
                String =
                "",
        )

        data class Group(
            val name:
                String,
            val items:
                List<
                    Item
                >,
        )

        data class State(
            val groups:
                List<
                    Group
                >,
        ) :
            VexBehaviorFamily.State<
                ListFamily
            > {
            fun values():
                List<String> =
                groups.flatMap { group ->
                    group.items.map { item ->
                        item.value
                    }
                }

            fun contains(
                value:
                    String,
            ):
                Boolean =
                value in
                    values()

            fun snapshot():
                State =
                copy(
                    groups =
                        groups.map { group ->
                            group.copy(
                                items =
                                    group.items.map { item ->
                                        item.copy()
                                    },
                            )
                        },
                )
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ListFamily
            > {
            data class Add(
                val value:
                    String,
            ) :
                Control

            data class SetNote(
                val value:
                    String,
                val note:
                    String,
            ) :
                Control
        }

        override val acts =
            vexActs<
                ListFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "sample.family-circuit.list",
            ) {
                state<State> {
                    on<Control.Add>(
                        whenCurrent = { current, control ->
                            !current.contains(
                                control.value,
                            )
                        },
                    ) { current, control ->
                        val groups =
                            if (
                                current.groups.isEmpty()
                            ) {
                                listOf(
                                    Group(
                                        name =
                                            "items",
                                        items =
                                            listOf(
                                                Item(
                                                    value =
                                                        control.value,
                                                ),
                                            ),
                                    ),
                                )
                            } else {
                                current.groups.mapIndexed { index, group ->
                                    if (
                                        index ==
                                        0
                                    ) {
                                        group.copy(
                                            items =
                                                group.items +
                                                    Item(
                                                        value =
                                                            control.value,
                                                    ),
                                        )
                                    } else {
                                        group
                                    }
                                }
                            }

                        admit(
                            State(
                                groups =
                                    groups,
                            ),
                        )
                    }

                    on<Control.SetNote>(
                        whenCurrent = { current, control ->
                            current.contains(
                                control.value,
                            )
                        },
                    ) { current, control ->
                        admit(
                            current.copy(
                                groups =
                                    current.groups.map { group ->
                                        group.copy(
                                            items =
                                                group.items.map { item ->
                                                    if (
                                                        item.value ==
                                                        control.value
                                                    ) {
                                                        item.copy(
                                                            note =
                                                                control.note,
                                                        )
                                                    } else {
                                                        item
                                                    }
                                                },
                                        )
                                    },
                            ),
                        )
                    }
                }
            }
    }

    /**
     * Another simple current-State shape.
     */
    private class SwitchFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            SwitchFamily,
            SwitchFamily.State,
            SwitchFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        data class State(
            val enabled:
                Boolean,
        ) :
            VexBehaviorFamily.State<
                SwitchFamily
            >

        sealed interface Control :
            VexBehaviorFamily.Control<
                SwitchFamily
            > {
            data class Set(
                val enabled:
                    Boolean,
            ) :
                Control
        }

        override val acts =
            vexActs<
                SwitchFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "sample.family-circuit.switch",
            ) {
                state<State> {
                    on<Control.Set> { current, control ->
                        admit(
                            current.copy(
                                enabled =
                                    control.enabled,
                            ),
                        )
                    }
                }
            }
    }

    /**
     * Partial observations for MultiFamily.
     *
     * latest(...) owns:
     *
     *     partial Input
     *     + retained Input
     *     -> complete CURRENT Input
     */
    private data class MultiInputState(
        val singleState:
            SingleFamily.State? =
            null,
        val listState:
            ListFamily.State? =
            null,
        val switchState:
            SwitchFamily.State? =
            null,
    ) :
        VexBehaviorV2CompoundInputState<
            MultiInputState
        > {
        override fun latest(
            incoming:
                MultiInputState,
        ):
            MultiInputState =
            MultiInputState(
                singleState =
                    (
                        incoming.singleState
                            ?: singleState
                    )?.copy(),
                listState =
                    (
                        incoming.listState
                            ?: listState
                    )?.snapshot(),
                switchState =
                    (
                        incoming.switchState
                            ?: switchState
                    )?.copy(),
            )

        override fun deepCopy():
            MultiInputState =
            MultiInputState(
                singleState =
                    singleState?.copy(),
                listState =
                    listState?.snapshot(),
                switchState =
                    switchState?.copy(),
            )
    }

    /**
     * Current meaning of MultiFamily's complete Input.
     */
    private sealed interface MultiOutputState :
        VexBehaviorV2CompoundOutputState<
            MultiOutputState
        > {
        data object Unobserved :
            MultiOutputState

        data class NeedsListValue(
            val value:
                String,
        ) :
            MultiOutputState

        data class Settled(
            val value:
                String,
        ) :
            MultiOutputState

        fun traceValue():
            String =
            when (
                this
            ) {
                Unobserved ->
                    "Unobserved"

                is NeedsListValue ->
                    "NeedsListValue(" +
                        value +
                        ")"

                is Settled ->
                    "Settled(" +
                        value +
                        ")"
            }
    }

    /**
     * MultiFamily's CURRENT State.
     */
    private data class MultiCompoundState(
        override val inputState:
            MultiInputState,
        override val outputState:
            MultiOutputState,
        override val revision:
            Long,
    ) :
        VexBehaviorV2CompoundFamilyState<
            MultiFamily,
            MultiInputState,
            MultiOutputState
        >

    /**
     * Compound Family example.
     *
     * process(...) is the only relationship calculation:
     *
     *     here is everything CURRENT
     *     -> what does it mean?
     *     -> return Output
     */
    private class MultiFamily(
        private val singleFamily:
            SingleFamily,
        private val listFamily:
            ListFamily,
        private val switchFamily:
            SwitchFamily,
        private val trace:
            MutableList<String>,
    ) :
        VexBehaviorV2CompoundFamily<
            MultiFamily,
            MultiCompoundState,
            MultiInputState,
            MultiOutputState
        >(
            initialInputState =
                MultiInputState(),
            initialOutputState =
                MultiOutputState.Unobserved,
            stateFactory = { inputState, outputState, revision ->
                MultiCompoundState(
                    inputState =
                        inputState,
                    outputState =
                        outputState,
                    revision =
                        revision,
                )
            },
            logger =
                VexFamilyLogger.None,
        ) {
        fun refresh() {
            observeSingle(
                singleFamily.current,
            )
            observeList(
                listFamily.current,
            )
            observeSwitch(
                switchFamily.current,
            )
        }

        fun observeSingle(
            state:
                SingleFamily.State,
        ):
            VexBehaviorV2CommittedOutput<
                MultiOutputState
            >? =
            observe(
                MultiInputState(
                    singleState =
                        state,
                ),
            )

        fun observeList(
            state:
                ListFamily.State,
        ):
            VexBehaviorV2CommittedOutput<
                MultiOutputState
            >? =
            observe(
                MultiInputState(
                    listState =
                        state,
                ),
            )

        fun observeSwitch(
            state:
                SwitchFamily.State,
        ):
            VexBehaviorV2CommittedOutput<
                MultiOutputState
            >? =
            observe(
                MultiInputState(
                    switchState =
                        state,
                ),
            )

        override fun process(
            latestStateInput:
                MultiInputState,
        ):
            MultiOutputState {
            val single =
                latestStateInput.singleState
            val list =
                latestStateInput.listState
            val switch =
                latestStateInput.switchState

            val output =
                when {
                    single ==
                        null ||
                        list ==
                        null ||
                        switch ==
                        null ->
                        MultiOutputState
                            .Unobserved

                    switch.enabled &&
                        !list.contains(
                            single.value,
                        ) ->
                        MultiOutputState
                            .NeedsListValue(
                                value =
                                    single.value,
                            )

                    else ->
                        MultiOutputState
                            .Settled(
                                value =
                                    single.value,
                            )
                }

            trace +=
                "process:" +
                    output.traceValue()

            return output
        }

        override fun act(
            committed:
                VexBehaviorV2CommittedOutput<
                    MultiOutputState
                >,
        ) {
            val output =
                committed.outputState

            trace +=
                "commit:" +
                    output.traceValue()
            trace +=
                "act:" +
                    output.traceValue()

            when (
                output
            ) {
                MultiOutputState.Unobserved,
                is MultiOutputState.Settled,
                ->
                    Unit

                is MultiOutputState.NeedsListValue -> {
                    listFamily.activate(
                        ListFamily.Control.Add(
                            value =
                                output.value,
                        ),
                    )

                    trace +=
                        "list-family:" +
                            listFamily
                                .current
                                .values()
                                .joinToString(
                                    separator =
                                        ",",
                                )
                    trace +=
                        "observe:list"

                    observeList(
                        listFamily.current,
                    )
                }
            }
        }
    }

    /**
     * Small constructor examples.
     *
     * These are the intended first reading points for a fresh instance.
     */
    private fun createSingleFamily(
        value:
            String =
            "value",
    ):
        SingleFamily =
        SingleFamily(
            initialState =
                SingleFamily.State(
                    value =
                        value,
                ),
            executionRoot =
                VexExecutionRoot(),
        )

    private fun createListFamily(
        vararg values:
            String,
    ):
        ListFamily =
        ListFamily(
            initialState =
                ListFamily.State(
                    groups =
                        listOf(
                            ListFamily.Group(
                                name =
                                    "items",
                                items =
                                    values.map { value ->
                                        ListFamily.Item(
                                            value =
                                                value,
                                        )
                                    },
                            ),
                        ),
                ),
            executionRoot =
                VexExecutionRoot(),
        )

    private fun createSwitchFamily(
        enabled:
            Boolean =
            false,
    ):
        SwitchFamily =
        SwitchFamily(
            initialState =
                SwitchFamily.State(
                    enabled =
                        enabled,
                ),
            executionRoot =
                VexExecutionRoot(),
        )

    private fun createMultiFamily(
        singleFamily:
            SingleFamily,
        listFamily:
            ListFamily,
        switchFamily:
            SwitchFamily,
        trace:
            MutableList<String> =
            mutableListOf(),
    ):
        MultiFamily =
        MultiFamily(
            singleFamily =
                singleFamily,
            listFamily =
                listFamily,
            switchFamily =
                switchFamily,
            trace =
                trace,
        )
}

// [VXG RealForever]
