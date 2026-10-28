package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexObservableRelay
import com.vextreme.vexlife.android.relay.VexState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Canonical fresh-entry reference for the current V2 SCAE/Behavior Origin Walk.
 *
 * Read this file when the question is:
 *
 *     "How does a UI interaction enter one Family, use CURRENT observed State, resolve Behavior,
 *      realize an Effect, observe resulting truth, and become visible in UI again?"
 *
 * Why this is a reference test instead of production UI:
 *
 * - :architecture:behavior must remain independent of Compose/app/runtime machinery;
 * - AGENTS.md says proof must follow the claim rather than importing a stronger proof surface;
 * - this JVM test proves the semantic round trip and currentness contract;
 * - actual Compose rendering / Journey / ScreenCrawler / device proof remains owned by the app proof
 *   foundations.
 *
 * The test-side [ReferenceComposableUi] therefore models only what a Composable is allowed to do:
 *
 *     observe the live State projection
 *     emit a semantic Control from onClick
 *
 * It deliberately does NOT pass rendered State back into activation. The live boundary reads the
 * current snapshot from the same observable semantic State owner that feeds UI observation.
 *
 * POC lineage:
 *
 * 1. V1 split Control resolution and Behavior resolution.
 * 2. Early V2 copied that split and created a Family.Act value vocabulary.
 * 3. ThemeMode removal pressure proved the intermediate Act values redundant.
 * 4. V2 now treats the authored State + Control on(...) relationship itself as the Act.
 * 5. This reference closes the next edge: observation and activation share one current-State owner.
 *
 * AppActions / Journey ActionElements intentionally do not appear here. They own app/UI identity
 * above this foundation. A production handler may map that UI identity to [ThemeModeFamily.Control],
 * but Behavior must not duplicate the app action registry.
 */
class VexBehaviorUiRoundTripReferenceTest {
    /**
     * Patient-zero Family.
     *
     * A fresh Family author should be able to discover the semantic vocabulary and Behavior from one
     * root instead of creating unrelated top-level State / Control / Act registries.
     */
    private object ThemeModeFamily :
        VexBehaviorFamily<
            ThemeModeFamily
        > {
        sealed interface State :
            VexBehaviorFamily.State<
                ThemeModeFamily
            >,
            VexState<
                State
            > {
            data object NotSet :
                State

            data object Light :
                State

            data object Dark :
                State

            override fun deepCopy():
                State =
                this
        }

        /**
         * Semantic input to the Family.
         *
         * This is sealed, not enum-only. Another Family can use payload-bearing shapes such as
         * Click(songId) without creating a second registration list.
         */
        sealed interface Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            data object Toggle :
                Control
        }

        /**
         * Pure Family Behavior.
         *
         * Behavior does not fetch State, own observation, know Compose, or know the durable Store.
         * It answers only:
         *
         *     CURRENT State + Control
         *     -> Act scenario
         *
         * The selected Act carries Effect + immediate State consequence.
         */
        val Behavior =
            vexBehaviorV2(
                family =
                    this,
                behaviorRef =
                    "reference.theme-mode",
            ) {
                state<State.NotSet> {
                    // Durability-gated comparison: stay now; observed durable truth may admit later.
                    on(
                        control =
                            Control.Toggle,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode.Sequenced,
                            ) {
                                save(
                                    State.Dark,
                                ).results {
                                    success {
                                        admit<
                                            State.Dark
                                        >()
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).stay()
                }

                state<State.Light> {
                    // This complete State + Control scenario is the Act.
                    on(
                        control =
                            Control.Toggle,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode.Replaceable,
                            ) {
                                save(
                                    State.Dark,
                                ).results {
                                    success {
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).admit<
                        State.Dark
                    >()
                }

                state<State.Dark> {
                    on(
                        control =
                            Control.Toggle,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode.Replaceable,
                            ) {
                                save(
                                    State.Light,
                                ).results {
                                    success {
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).admit<
                        State.Light
                    >()
                }
            }
    }

    /**
     * Durable source fixture for the reference.
     *
     * [observableState] and [current] are two reads of the SAME backing truth. [observe] is a
     * synchronous test stand-in for collecting that observable source; it exists to keep the test
     * causal/readable instead of turning this foundation example into a coroutine-lifetime test.
     *
     * Production collection mechanics belong to the concrete Connection/execution owner.
     */
    private class ReferenceFamilyStore(
        initial: ThemeModeFamily.State,
        private val trace: MutableList<String>,
    ) {
        private val mutableState =
            MutableStateFlow(
                initial,
            )

        val observableState:
            StateFlow<
                ThemeModeFamily.State
            > =
            mutableState.asStateFlow()

        val current:
            ThemeModeFamily.State
            get() =
                observableState.value

        private val observers =
            mutableListOf<
                (
                    ThemeModeFamily.State,
                ) -> Unit
            >()

        fun observe(
            observer:
                (
                    ThemeModeFamily.State,
                ) -> Unit,
        ) {
            observers +=
                observer

            // A StateFlow collector receives the current value when observation begins.
            observer(
                current,
            )
        }

        fun save(
            state: ThemeModeFamily.State,
        ) {
            trace +=
                "Store.Save(${state.label()})"

            publish(
                state,
            )
        }

        fun externalObservation(
            state: ThemeModeFamily.State,
        ) {
            trace +=
                "Store.externalObservation(${state.label()})"

            publish(
                state,
            )
        }

        private fun publish(
            state: ThemeModeFamily.State,
        ) {
            if (
                mutableState.value ==
                state
            ) {
                return
            }

            mutableState.value =
                state

            observers.forEach { observer ->
                observer(
                    state,
                )
            }
        }
    }

    /**
     * One semantic current-State owner.
     *
     * UI observation reads [states]; activation reads [current]. Both come from this exact owner.
     * This is the invariant that prevents "rendered State" and "activation State" becoming separate
     * truths.
     */
    private class ReferenceSemanticStateOwner(
        initial: ThemeModeFamily.State,
        private val trace: MutableList<String>,
    ) :
        VexObservableRelay<
            ThemeModeFamily.State
        > {
        private val mutableStates =
            MutableStateFlow(
                initial,
            )

        override val states:
            StateFlow<
                ThemeModeFamily.State
            > =
            mutableStates.asStateFlow()

        override val current:
            ThemeModeFamily.State
            get() =
                states.value

        fun admit(
            state: ThemeModeFamily.State,
        ) {
            if (
                current ==
                state
            ) {
                return
            }

            trace +=
                "SemanticState.admit(${state.label()})"

            mutableStates.value =
                state
        }
    }

    /**
     * Small live surface used by the reference Composable.
     *
     * This is intentionally test-local. The Origin Walk has not yet earned the final production
     * name/shape for the UI-facing live Family surface.
     */
    private interface ThemeModeLiveSurface {
        val states:
            StateFlow<
                ThemeModeFamily.State
            >

        val current:
            ThemeModeFamily.State

        fun activate(
            control:
                ThemeModeFamily.Control,
        )
    }

    /**
     * Reality/currentness boundary for the reference.
     *
     * Responsibilities intentionally kept here:
     *
     * - observe Store truth;
     * - own the CURRENT semantic State snapshot;
     * - on activation, re-read CURRENT State;
     * - ask pure Family Behavior for the Act;
     * - realize the Effect;
     * - reconcile observed truth back into the semantic State owner.
     *
     * Behavior itself does not perform any of these runtime responsibilities.
     */
    private class ThemeModeConnection(
        private val store:
            ReferenceFamilyStore,
        private val trace:
            MutableList<String>,
    ) :
        ThemeModeLiveSurface {
        private val semanticState =
            ReferenceSemanticStateOwner(
                initial =
                    store.current,
                trace =
                    trace,
            )

        override val states:
            StateFlow<
                ThemeModeFamily.State
            >
            get() =
                semanticState.states

        override val current:
            ThemeModeFamily.State
            get() =
                semanticState.current

        init {
            store.observe { observed ->
                trace +=
                    "Connection.ObservableState=${observed.label()}"

                semanticState.admit(
                    observed,
                )
            }
        }

        override fun activate(
            control:
                ThemeModeFamily.Control,
        ) {
            val current =
                semanticState.current

            trace +=
                "Connection.current=${current.label()}"

            val act =
                ThemeModeFamily.Behavior
                    .resolve(
                        current =
                            current,
                        control =
                            control,
                    )
                    ?: return

            trace +=
                "Behavior.Act=${current.label()}+${control.label()}"

            when (
                act.immediateDisposition
                    .kind
            ) {
                VexBehaviorV2StateDispositionKind.Stay ->
                    trace +=
                        "Behavior.immediate=Stay"

                VexBehaviorV2StateDispositionKind.Admit -> {
                    // ThemeMode's current POC admits the same concrete State value that Save carries.
                    // A future generic runtime must not assume that equation without earning it.
                    val admitted =
                        act.effect
                            .save
                            .state

                    check(
                        act.immediateDisposition
                            .stateKind
                            .isInstance(
                                admitted,
                            ),
                    )

                    trace +=
                        "Behavior.immediate=Admit(${admitted.label()})"

                    semanticState.admit(
                        admitted,
                    )
                }
            }

            realize(
                act.effect,
            )
        }

        private fun realize(
            effect:
                VexBehaviorV2Begin<
                    ThemeModeFamily,
                    ThemeModeFamily.State
                >,
        ) {
            // This reference proves the Save/ObservableState round trip. It intentionally does not
            // invent a generic async executor for Success/Failure; that remains separate Origin
            // pressure until a real runtime consumer earns it.
            store.save(
                effect.save
                    .state,
            )
        }
    }

    private data class ReferenceUiState(
        val visibleMode: String,
        val toggleChecked: Boolean,
    )

    /**
     * Test-side stand-in for a Composable boundary.
     *
     * A real Composable would collect [ThemeModeLiveSurface.states] (for example through Compose
     * state collection) and wire its click handler to [ThemeModeLiveSurface.activate].
     *
     * This JVM reference reads StateFlow.value so the Behavior foundation stays Compose-free. It
     * proves semantic dataflow, NOT rendering correctness.
     */
    private class ReferenceComposableUi(
        private val live:
            ThemeModeLiveSurface,
        private val trace:
            MutableList<String>,
    ) {
        fun render():
            ReferenceUiState {
            val observed =
                live.states.value

            trace +=
                "UI.render=${observed.label()}"

            return ReferenceUiState(
                visibleMode =
                    observed.label(),
                toggleChecked =
                    observed ==
                        ThemeModeFamily.State.Dark,
            )
        }

        fun onToggleClick() {
            trace +=
                "UI.onClick=Toggle"

            // The UI emits only Control. It does not pass its rendered State snapshot back as
            // authority; activation re-reads live CURRENT State.
            live.activate(
                ThemeModeFamily.Control.Toggle,
            )
        }
    }

    @Test
    fun composableClickRoundTripsThroughScaeAndObservedTruthBackToUi() {
        val trace =
            mutableListOf<String>()

        val store =
            ReferenceFamilyStore(
                initial =
                    ThemeModeFamily.State.Light,
                trace =
                    trace,
            )

        val live =
            ThemeModeConnection(
                store =
                    store,
                trace =
                    trace,
            )

        val ui =
            ReferenceComposableUi(
                live =
                    live,
                trace =
                    trace,
            )

        // Ignore initial StateFlow-style subscription delivery; begin at the human-visible screen.
        trace.clear()

        assertEquals(
            ReferenceUiState(
                visibleMode =
                    "Light",
                toggleChecked =
                    false,
            ),
            ui.render(),
        )

        ui.onToggleClick()

        assertSame(
            ThemeModeFamily.State.Dark,
            store.observableState.value,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            live.current,
        )
        assertSame(
            live.states.value,
            live.current,
        )

        assertEquals(
            ReferenceUiState(
                visibleMode =
                    "Dark",
                toggleChecked =
                    true,
            ),
            ui.render(),
        )

        assertEquals(
            listOf(
                "UI.render=Light",
                "UI.onClick=Toggle",
                "Connection.current=Light",
                "Behavior.Act=Light+Toggle",
                "Behavior.immediate=Admit(Dark)",
                "SemanticState.admit(Dark)",
                "Store.Save(Dark)",
                "Connection.ObservableState=Dark",
                "UI.render=Dark",
            ),
            trace,
        )
    }

    @Test
    fun activationUsesCurrentObservedStateInsteadOfStaleRenderedState() {
        val trace =
            mutableListOf<String>()

        val store =
            ReferenceFamilyStore(
                initial =
                    ThemeModeFamily.State.Light,
                trace =
                    trace,
            )

        val live =
            ThemeModeConnection(
                store =
                    store,
                trace =
                    trace,
            )

        val ui =
            ReferenceComposableUi(
                live =
                    live,
                trace =
                    trace,
            )

        trace.clear()

        // UI rendered Light.
        assertEquals(
            "Light",
            ui.render()
                .visibleMode,
        )

        // Reality changes before the user clicks. We intentionally do NOT re-render first.
        store.externalObservation(
            ThemeModeFamily.State.Dark,
        )

        assertSame(
            ThemeModeFamily.State.Dark,
            live.current,
        )

        // A stale rendered Light snapshot must not be passed into Behavior. Activation re-reads
        // CURRENT Dark, so Toggle now means the Dark->Light Act.
        ui.onToggleClick()

        assertSame(
            ThemeModeFamily.State.Light,
            live.current,
        )

        assertEquals(
            "Light",
            ui.render()
                .visibleMode,
        )

        assertTrue(
            trace.contains(
                "Connection.current=Dark",
            ),
        )
        assertTrue(
            trace.contains(
                "Behavior.Act=Dark+Toggle",
            ),
        )
    }

    private fun ThemeModeFamily.State.label():
        String =
        when (this) {
            ThemeModeFamily.State.NotSet ->
                "NotSet"

            ThemeModeFamily.State.Light ->
                "Light"

            ThemeModeFamily.State.Dark ->
                "Dark"
        }

    private fun ThemeModeFamily.Control.label():
        String =
        when (this) {
            ThemeModeFamily.Control.Toggle ->
                "Toggle"
        }
}

// [VXG RealForever]
