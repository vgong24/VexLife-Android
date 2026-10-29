package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Small, context-neutral examples of the Family Circuit forms.
 *
 * Start with the create...Family() functions.
 */
class VexFamilyCircuitTestSampleSuite {
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
        )

    private fun createBooleanFamily(
        enabled:
            Boolean =
            false,
    ):
        BooleanFamily =
        BooleanFamily(
            initialState =
                BooleanFamily.State(
                    enabled =
                        enabled,
                ),
        )

    private fun createMultiFamily(
        singleFamily:
            SingleFamily,
        listFamily:
            ListFamily,
        booleanFamily:
            BooleanFamily,
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
            booleanFamily =
                booleanFamily,
            trace =
                trace,
        )

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
            "two",
            family.current.value,
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
            family.current.values(),
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
        val booleanFamily =
            createBooleanFamily(
                enabled =
                    true,
            )
        val multiFamily =
            createMultiFamily(
                singleFamily =
                    singleFamily,
                listFamily =
                    listFamily,
                booleanFamily =
                    booleanFamily,
                trace =
                    trace,
            )

        multiFamily.refresh()

        assertEquals(
            listOf(
                "one",
                "two",
            ),
            listFamily.current.values(),
        )
        assertEquals(
            MultiOutputState.Settled(
                value =
                    "two",
            ),
            multiFamily.current.outputState,
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
    fun inputOnlyChangeDoesNotCreateAnotherAct() {
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
        val booleanFamily =
            createBooleanFamily(
                enabled =
                    true,
            )
        val multiFamily =
            createMultiFamily(
                singleFamily =
                    singleFamily,
                listFamily =
                    listFamily,
                booleanFamily =
                    booleanFamily,
                trace =
                    trace,
            )

        multiFamily.refresh()

        val beforeRevision =
            multiFamily.current.revision
        val beforeActCount =
            trace.count { event ->
                event.startsWith(
                    "act:",
                )
            }

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
            trace.count { event ->
                event.startsWith(
                    "act:",
                )
            },
        )
    }

    /**
     * One current value + Control.
     */
    private class SingleFamily(
        initialState:
            State,
    ) :
        VexControlFamily<
            SingleFamily,
            SingleFamily.State,
            SingleFamily.Control
        >(
            initialState =
                initialState,
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

        override fun resolveAct(
            current:
                State,
            control:
                Control,
        ):
            VexFamilyAct<
                State
            > =
            when (
                control
            ) {
                is Control.Set ->
                    VexFamilyAct.Admit(
                        current.copy(
                            value =
                                control.value,
                        ),
                    )
            }
    }

    /**
     * Nested list State + Controls.
     */
    private class ListFamily(
        initialState:
            State,
    ) :
        VexControlFamily<
            ListFamily,
            ListFamily.State,
            ListFamily.Control
        >(
            initialState =
                initialState,
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

            fun deepCopy():
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

        override fun resolveAct(
            current:
                State,
            control:
                Control,
        ):
            VexFamilyAct<
                State
            >? =
            when (
                control
            ) {
                is Control.Add ->
                    if (
                        current.contains(
                            control.value,
                        )
                    ) {
                        null
                    } else {
                        val first =
                            current.groups.firstOrNull()
                                ?: Group(
                                    name =
                                        "items",
                                    items =
                                        emptyList(),
                                )

                        VexFamilyAct.Admit(
                            current.copy(
                                groups =
                                    listOf(
                                        first.copy(
                                            items =
                                                first.items +
                                                    Item(
                                                        value =
                                                            control.value,
                                                    ),
                                        ),
                                    ) +
                                        current.groups.drop(
                                            1,
                                        ),
                            ),
                        )
                    }

                is Control.SetNote ->
                    if (
                        !current.contains(
                            control.value,
                        )
                    ) {
                        null
                    } else {
                        VexFamilyAct.Admit(
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

    /**
     * One Boolean State + Control.
     */
    private class BooleanFamily(
        initialState:
            State,
    ) :
        VexControlFamily<
            BooleanFamily,
            BooleanFamily.State,
            BooleanFamily.Control
        >(
            initialState =
                initialState,
        ) {
        data class State(
            val enabled:
                Boolean,
        ) :
            VexBehaviorFamily.State<
                BooleanFamily
            >

        sealed interface Control :
            VexBehaviorFamily.Control<
                BooleanFamily
            > {
            data class Set(
                val enabled:
                    Boolean,
            ) :
                Control
        }

        override fun resolveAct(
            current:
                State,
            control:
                Control,
        ):
            VexFamilyAct<
                State
            > =
            when (
                control
            ) {
                is Control.Set ->
                    VexFamilyAct.Admit(
                        current.copy(
                            enabled =
                                control.enabled,
                        ),
                    )
            }
    }

    private data class MultiInputState(
        val singleState:
            SingleFamily.State? =
            null,
        val listState:
            ListFamily.State? =
            null,
        val booleanState:
            BooleanFamily.State? =
            null,
    ) :
        VexCompoundInputState<
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
                    )?.deepCopy(),
                booleanState =
                    (
                        incoming.booleanState
                            ?: booleanState
                    )?.copy(),
            )

        override fun deepCopy():
            MultiInputState =
            MultiInputState(
                singleState =
                    singleState?.copy(),
                listState =
                    listState?.deepCopy(),
                booleanState =
                    booleanState?.copy(),
            )
    }

    private sealed interface MultiOutputState :
        VexCompoundOutputState<
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

    private data class MultiCompoundState(
        override val inputState:
            MultiInputState,
        override val outputState:
            MultiOutputState,
        override val revision:
            Long,
    ) :
        VexCompoundFamilyState<
            MultiFamily,
            MultiInputState,
            MultiOutputState
        >

    /**
     * Several Family currents -> one CURRENT relationship State.
     */
    private class MultiFamily(
        private val singleFamily:
            SingleFamily,
        private val listFamily:
            ListFamily,
        private val booleanFamily:
            BooleanFamily,
        private val trace:
            MutableList<String>,
    ) :
        VexCompoundFamily<
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
        ) {
        fun refresh() {
            observeSingle(
                singleFamily.current,
            )
            observeList(
                listFamily.current,
            )
            observeBoolean(
                booleanFamily.current,
            )
        }

        fun observeSingle(
            state:
                SingleFamily.State,
        ):
            VexCommittedOutput<
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
            VexCommittedOutput<
                MultiOutputState
            >? =
            observe(
                MultiInputState(
                    listState =
                        state,
                ),
            )

        fun observeBoolean(
            state:
                BooleanFamily.State,
        ):
            VexCommittedOutput<
                MultiOutputState
            >? =
            observe(
                MultiInputState(
                    booleanState =
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
            val boolean =
                latestStateInput.booleanState

            val output =
                when {
                    single ==
                        null ||
                        list ==
                        null ||
                        boolean ==
                        null ->
                        MultiOutputState
                            .Unobserved

                    boolean.enabled &&
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
                VexCommittedOutput<
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
}

// [VXG RealForever]
