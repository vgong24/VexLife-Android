package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Teaching suite for the candidate Family Circuit Architecture.
 *
 * This class is intentionally not a real app feature. It demonstrates how to BUILD the reusable
 * Family circuit from simple pieces without making a fresh reader reverse-engineer one completed
 * production specimen.
 *
 * Read the tests from top to bottom:
 *
 *     STEP 1
 *       establish several rightful CURRENT owners
 *
 *     STEP 2
 *       build partial Input -> latest complete Input
 *
 *     STEP 3
 *       make ComplexFamily itself own the Compound relationship State
 *
 *     STEP 4
 *       process(latestStateInput) returns relationship meaning only
 *
 *     STEP 5
 *       Output-changing commit -> Act -> rightful target Family Control
 *
 *     STEP 6
 *       observe target Family CURRENT truth -> NEW input -> process again
 *
 *     STEP 7
 *       input-only currentness movement does not fabricate another Act
 *
 * Family Circuit is the umbrella being pressured here. [VexFamily] is the common current-State
 * root. A Control-driven Family uses [VexBehaviorV2Family]. A Family whose semantic State is the
 * current relationship between several independently-owned truths uses
 * [VexBehaviorV2CompoundFamily].
 */
class VexBehaviorV2FamilyCircuitTestSampleSuite {
    @Test
    fun step1_rightfulOwnersKeepTheirOwnCurrentTruth() {
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

        val screenFamily =
            ScreenFamily(
                initialState =
                    ScreenFamily.State(
                        screenName =
                            BROWSE_SCREEN,
                        transientBlocked =
                            false,
                    ),
                executionRoot =
                    VexExecutionRoot(),
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
         * Screen is a real Family because another semantic circuit may REQUEST it to change.
         *
         * Queue and Theme are observation-only current owners in this sample. They do not receive fake
         * Controls merely so every input looks symmetrical.
         */
        assertEquals(
            BROWSE_SCREEN,
            screenFamily.current.screenName,
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
         * The queue contains a genuinely mutable nested list. VexRelay owns a detached retained copy,
         * so later caller mutation does not mutate CURRENT queue truth.
         *
         * This is why deepCopy exists. It is ownership protection, not semantic identity.
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
    fun step2_inputOwnsPartialObservationToCompleteCurrentInputFormation() {
        val retained =
            ComplexInputState(
                screen =
                    ScreenFamily.State(
                        screenName =
                            BROWSE_SCREEN,
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

        val incomingGroups =
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
         * One source changed, so the incoming observation carries only that source.
         */
        val latestStateInput =
            retained.latest(
                ComplexInputState(
                    musicQueue =
                        MusicQueueState(
                            groups =
                                incomingGroups,
                            currentTrackRef =
                                "track.beta",
                        ),
                ),
            )

        /*
         * latest(...) owns currentization only.
         *
         * It answers:
         *
         *     "Given what I already knew and this new partial observation,
         *      what is the complete CURRENT input now?"
         *
         * It does NOT calculate relationship meaning.
         */
        assertEquals(
            BROWSE_SCREEN,
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

        incomingGroups
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
    fun step3_and_4_complexFamilyOwnsCompoundStateAndProcessOnlyCalculatesMeaning() {
        val trace =
            mutableListOf<String>()

        val screenFamily =
            ScreenFamily(
                initialState =
                    ScreenFamily.State(
                        screenName =
                            PLAYER_SCREEN,
                        transientBlocked =
                            false,
                    ),
                executionRoot =
                    VexExecutionRoot(),
            )

        val family =
            ComplexFamily(
                screenFamily =
                    screenFamily,
                trace =
                    trace,
            )

        /*
         * ComplexFamily CURRENT State is the Compound relationship State itself.
         *
         * There is no sibling Relay that secretly owns Input/Output currentness.
         */
        assertEquals(
            0L,
            family.current.revision,
        )
        assertEquals(
            ComplexOutputState.Unobserved,
            family.current.outputState,
        )

        family.observeScreen(
            screenFamily.current,
        )
        family.observeMusicQueue(
            musicQueue(
                currentTrackRef =
                    "track.alpha",
                "track.alpha" to "Alpha",
            ),
        )
        family.observeThemeMode(
            ThemeModeState(
                mode =
                    SampleThemeMode.Dark,
                followsSystem =
                    false,
            ),
        )

        /*
         * Screen is already the target screen, so process(...) settles without asking another owner
         * to change. The important part is that process(...) saw one COMPLETE latestStateInput and
         * returned only relationship meaning.
         */
        assertEquals(
            ComplexOutputState.Settled(
                screenName =
                    PLAYER_SCREEN,
                currentTrackRef =
                    "track.alpha",
                themeMode =
                    SampleThemeMode.Dark,
            ),
            family.current.outputState,
        )

        assertEquals(
            PLAYER_SCREEN,
            family
                .current
                .inputState
                .screen
                ?.screenName,
        )
        assertEquals(
            "track.alpha",
            family
                .current
                .inputState
                .musicQueue
                ?.currentTrackRef,
        )
        assertEquals(
            SampleThemeMode.Dark,
            family
                .current
                .inputState
                .themeMode
                ?.mode,
        )

        assertEquals(
            listOf(
                "process:Unobserved",
                "process:Unobserved",
                "process:Settled(PlayerSurface,track.alpha,Dark)",
                "commit:Settled(PlayerSurface,track.alpha,Dark)",
                "act:Settled(PlayerSurface,track.alpha,Dark)",
            ),
            trace,
        )
    }

    @Test
    fun step5_and_6_committedOutputActsThroughTargetFamilyThenObservedTruthReturnsAsNewInput() {
        val trace =
            mutableListOf<String>()

        val screenFamily =
            ScreenFamily(
                initialState =
                    ScreenFamily.State(
                        screenName =
                            BROWSE_SCREEN,
                        transientBlocked =
                            false,
                    ),
                executionRoot =
                    VexExecutionRoot(),
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
                screenFamily =
                    screenFamily,
                trace =
                    trace,
            )

        family.observeScreen(
            screenFamily.current,
        )
        family.observeMusicQueue(
            queueOwner.current,
        )
        family.observeThemeMode(
            themeOwner.current,
        )

        /*
         * End-to-end circuit:
         *
         *     complete latest input
         *     -> NeedsPlayerScreen
         *     -> COMMIT ComplexCompoundState
         *     -> Act
         *     -> ScreenFamily.Control.Show(PlayerSurface)
         *     -> ScreenFamily admits its own CURRENT State
         *     -> ComplexFamily observes ScreenFamily.current as NEW input
         *     -> process(...)
         *     -> Settled
         */
        assertEquals(
            PLAYER_SCREEN,
            screenFamily.current.screenName,
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
            family.current.outputState,
        )

        assertEquals(
            listOf(
                "process:Unobserved",
                "process:Unobserved",
                "process:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "commit:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "act:NeedsPlayerScreen(track.alpha,PlayerSurface,Dark)",
                "screen-family:PlayerSurface",
                "observe:screen:PlayerSurface",
                "process:Settled(PlayerSurface,track.alpha,Dark)",
                "commit:Settled(PlayerSurface,track.alpha,Dark)",
                "act:Settled(PlayerSurface,track.alpha,Dark)",
            ),
            trace,
        )
    }

    @Test
    fun step7_inputOnlyCurrentnessMovementDoesNotFabricateAnotherAct() {
        val trace =
            mutableListOf<String>()

        val screenFamily =
            ScreenFamily(
                initialState =
                    ScreenFamily.State(
                        screenName =
                            PLAYER_SCREEN,
                        transientBlocked =
                            false,
                    ),
                executionRoot =
                    VexExecutionRoot(),
            )

        val family =
            ComplexFamily(
                screenFamily =
                    screenFamily,
                trace =
                    trace,
            )

        family.observeScreen(
            screenFamily.current,
        )
        family.observeMusicQueue(
            musicQueue(
                currentTrackRef =
                    "track.alpha",
                "track.alpha" to "Old title",
            ),
        )
        family.observeThemeMode(
            ThemeModeState(
                mode =
                    SampleThemeMode.Dark,
                followsSystem =
                    false,
            ),
        )

        val beforeRevision =
            family.current.revision
        val beforeActCount =
            trace.count { entry ->
                entry.startsWith(
                    "act:",
                )
            }

        /*
         * Title changes are CURRENT queue truth, so Input meaning changes and the Compound revision
         * advances. But the relationship consequence remains exactly Settled(PlayerSurface,
         * track.alpha, Dark), so no new committed Output / Act is formed.
         */
        val committed =
            family.observeMusicQueue(
                musicQueue(
                    currentTrackRef =
                        "track.alpha",
                    "track.alpha" to "New title",
                ),
            )

        assertNull(
            committed,
        )
        assertEquals(
            beforeRevision +
                1L,
            family.current.revision,
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
     * Target Family used by the teaching circuit.
     *
     * The important point is the request boundary:
     *
     *     another Family needs a screen change
     *     -> ScreenFamily.Control.Show(...)
     *     -> ScreenFamily.activate(Control)
     *     -> ScreenFamily re-reads its own CURRENT State
     *     -> Act admits ScreenFamily.State
     *
     * There is no apply(...), setCurrent(...), or cross-owner State mutation escape hatch.
     */
    private class ScreenFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            ScreenFamily,
            ScreenFamily.State,
            ScreenFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        data class State(
            val screenName:
                String,
            val transientBlocked:
                Boolean,
        ) :
            VexBehaviorFamily.State<
                ScreenFamily
            >

        sealed interface Control :
            VexBehaviorFamily.Control<
                ScreenFamily
            > {
            data class Show(
                val screenName:
                    String,
            ) :
                Control
        }

        override val acts =
            vexActs<
                ScreenFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "sample.family-circuit.screen",
            ) {
                state<State> {
                    on<Control.Show> { current, control ->
                        admit(
                            current.copy(
                                screenName =
                                    control.screenName,
                            ),
                        )
                    }
                }
            }
    }

    /**
     * Observation-only owner with deliberately mutable-backed nested data.
     */
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

    /**
     * Observation-only owner. No fake Control is introduced because this sample never asks Theme to
     * change.
     */
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
            groups.flatMap { group ->
                group.items.map { item ->
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
        >

    /**
     * Input owns partial-observation -> complete-current-input formation.
     *
     * Each nullable field means only:
     *
     *     "this incoming observation did not supply that owner"
     *
     * It does NOT mean the retained current owner is absent.
     */
    private data class ComplexInputState(
        val screen:
            ScreenFamily.State? =
            null,
        val musicQueue:
            MusicQueueState? =
            null,
        val themeMode:
            ThemeModeState? =
            null,
    ) :
        VexBehaviorV2CompoundInputState<
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
                    )?.copy(),
                musicQueue =
                    (
                        incoming.musicQueue
                            ?: musicQueue
                    )?.deepCopy(),
                themeMode =
                    (
                        incoming.themeMode
                            ?: themeMode
                    )?.copy(),
            )

        override fun deepCopy():
            ComplexInputState =
            ComplexInputState(
                screen =
                    screen?.copy(),
                musicQueue =
                    musicQueue?.deepCopy(),
                themeMode =
                    themeMode?.copy(),
            )
    }

    /**
     * Output owns the CURRENT consequence of the relationship.
     *
     * It is never:
     * - previous process memory;
     * - ScreenFamily.State;
     * - MusicQueueState;
     * - ThemeModeState.
     */
    private sealed interface ComplexOutputState :
        VexBehaviorV2CompoundOutputState<
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

        fun traceValue():
            String =
            when (
                this
            ) {
                Unobserved ->
                    "Unobserved"

                is HeldTransient ->
                    "HeldTransient(" +
                        screenName +
                        ")"

                is NeedsPlayerScreen ->
                    "NeedsPlayerScreen(" +
                        trackRef +
                        "," +
                        targetScreenName +
                        "," +
                        themeMode.name +
                        ")"

                is Settled ->
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
    }

    /**
     * The Family's actual CURRENT semantic State.
     *
     * Fresh readers should be able to see the whole relationship shape from this declaration:
     *
     *     complete current Input
     *     + current Output consequence
     *     + relationship revision/currentness
     */
    private data class ComplexCompoundState(
        override val inputState:
            ComplexInputState,
        override val outputState:
            ComplexOutputState,
        override val revision:
            Long,
    ) :
        VexBehaviorV2CompoundFamilyState<
            ComplexFamily,
            ComplexInputState,
            ComplexOutputState
        >

    /**
     * The deliberately generic "complex" teaching Family.
     *
     * This is not a renamed feature specimen. It exists to demonstrate the reusable Family Circuit
     * structure directly. It is a Family because ComplexCompoundState is its CURRENT semantic State;
     * it does not inherit a Control API merely because ScreenFamily does.
     *
     * ComplexFamily owns:
     * - its Compound CURRENT State;
     * - currentization through ComplexInputState.latest(...);
     * - relationship calculation through process(...);
     * - the consequence Act after an Output-changing commit.
     *
     * It does NOT own Screen, Queue or Theme truth. Those arrive as observations from their rightful
     * owners.
     */
    private class ComplexFamily(
        private val screenFamily:
            ScreenFamily,
        private val trace:
            MutableList<String>,
    ) :
        VexBehaviorV2CompoundFamily<
            ComplexFamily,
            ComplexCompoundState,
            ComplexInputState,
            ComplexOutputState
        >(
            initialInputState =
                ComplexInputState(),
            initialOutputState =
                ComplexOutputState.Unobserved,
            stateFactory = { inputState, outputState, revision ->
                ComplexCompoundState(
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
        fun observeScreen(
            state:
                ScreenFamily.State,
        ):
            VexBehaviorV2CommittedOutput<
                ComplexOutputState
            >? =
            observe(
                ComplexInputState(
                    screen =
                        state,
                ),
            )

        fun observeMusicQueue(
            state:
                MusicQueueState,
        ):
            VexBehaviorV2CommittedOutput<
                ComplexOutputState
            >? =
            observe(
                ComplexInputState(
                    musicQueue =
                        state,
                ),
            )

        fun observeThemeMode(
            state:
                ThemeModeState,
        ):
            VexBehaviorV2CommittedOutput<
                ComplexOutputState
            >? =
            observe(
                ComplexInputState(
                    themeMode =
                        state,
                ),
            )

        /**
         * The one relationship-specific calculation.
         *
         * No currentState parameter.
         * No previousOutput parameter.
         * No target mutation.
         * No Effect execution.
         *
         *     here is everything CURRENT
         *     -> what does it mean?
         *     -> return the Output consequence
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

        /**
         * Act reacts only after the Output consequence is already carried by Family.current.
         */
        override fun act(
            committed:
                VexBehaviorV2CommittedOutput<
                    ComplexOutputState
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
                ComplexOutputState.Unobserved,
                is ComplexOutputState.HeldTransient,
                is ComplexOutputState.Settled,
                ->
                    Unit

                is ComplexOutputState.NeedsPlayerScreen -> {
                    /*
                     * Another Family owns Screen truth, so the request crosses its Control boundary.
                     */
                    screenFamily.activate(
                        ScreenFamily.Control.Show(
                            screenName =
                                output.targetScreenName,
                        ),
                    )

                    val observed =
                        screenFamily.current

                    trace +=
                        "screen-family:" +
                            observed.screenName
                    trace +=
                        "observe:screen:" +
                            observed.screenName

                    /*
                     * The target's resulting CURRENT truth returns as NEW input.
                     */
                    observeScreen(
                        observed,
                    )
                }
            }
        }
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
        const val BROWSE_SCREEN:
            String =
            "BrowseSurface"

        const val PLAYER_SCREEN:
            String =
            "PlayerSurface"
    }
}

// [VXG RealForever]
