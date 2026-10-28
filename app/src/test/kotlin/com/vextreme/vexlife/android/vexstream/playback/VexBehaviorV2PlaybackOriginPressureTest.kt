package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily
import com.vextreme.vexlife.android.behavior.VexBehaviorV2Family
import com.vextreme.vexlife.android.behavior.VexFamilyLogger
import com.vextreme.vexlife.android.behavior.vexActs
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexRelay
import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Third V2 Origin-Walk reference: Playback pressure, NOT a production V2 Playback template.
 *
 * The first V2 reference classes prove the locally-owned Family circuit and stale-render activation.
 * This class deliberately starts from richer Playback facts that already exist in production-shaped
 * source and asks where the current V2 runtime stops being general.
 *
 * A passing test here means the PRESSURE remains visible:
 *
 *     provider observation
 *       reconciles with newer local intent before concrete Family admission
 *       without becoming a fake Control or generic public setter
 *
 *     payload request
 *       must retain exact identity/currentness at activation
 *
 *     Control availability
 *       must derive from current semantic/provider capability
 *
 *     runtime payload Control
 *       now materializes an Act from actual CURRENT State + actual Control payload
 *
 * The class now also carries a StateRelay candidate using the accepted V1 VexRelay law: competing
 * semantic causes enter one serialized current-State boundary and an older completion is evaluated
 * against CURRENT meaning instead of directly overwriting it.
 *
 * StateRelay is still a pressure candidate, not a confirmed Family member. The class also pressures
 * nested list currentness using stable refs + complete identity-indexed snapshots so a copied parent
 * cannot silently lose a nested display layer. In particular, these test fixtures use VexState while
 * current production Playback State does not yet implement VexState. Production Playback stays
 * unchanged.
 */
class VexBehaviorV2PlaybackOriginPressureTest {
    @Test
    fun providerObservationMustReconcileWithNewerIntentBeforeBecomingSemanticState() {
        val pending =
            VexStreamPlaybackPending(
                playRequested = true,
            )

        val staleProviderObservation =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            )

        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    staleProviderObservation,
                pending =
                    pending,
            )

        val semanticState =
            reconciled.state as
                VexStreamPlaybackState.Media

        /*
         * Media3 owns the observed provider fact, but that observation is not sufficient by itself to
         * replace semantic current truth. The newer admitted intent remains current until the provider
         * acknowledges it or an incompatible terminal observation invalidates it.
         */
        assertEquals(
            true,
            semanticState.playRequested,
        )
        assertTrue(
            semanticState.requestPending,
        )
        assertEquals(
            true,
            reconciled.pending.playRequested,
        )
    }

    @Test
    fun payloadSeekRequiresExactCurrentMediaCoordinateAtActivation() {
        val request =
            VexStreamPlaybackSeekRequest(
                mediaId =
                    "track.alpha",
                mediaItemIndex =
                    1,
                positionMs =
                    60_000L,
            )

        assertEquals(
            60_000L,
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    1,
                seekAvailable =
                    true,
            ),
        )

        /*
         * Payload identity is semantic currentness, not UI decoration. The same track in a different
         * queue occurrence is not the same activation coordinate.
         */
        assertNull(
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    2,
                seekAvailable =
                    true,
            ),
        )

        assertNull(
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    1,
                seekAvailable =
                    false,
            ),
        )
    }

    @Test
    fun controlAvailabilityComesFromCurrentSemanticAndProviderCapability() {
        val behavior =
            vexStreamPlaybackBehavior()
        val controls =
            behavior
                .vexStreamPlaybackControls()

        val unavailable =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable = false,
            )

        val available =
            unavailable.copy(
                playPauseAvailable = true,
            )

        val unavailableProjection =
            controls.projection(
                unavailable,
            )
        val availableProjection =
            controls.projection(
                available,
            )

        assertFalse(
            unavailableProjection[
                VexStreamPlaybackControl.PlayPause
            ].isAvailable,
        )

        assertTrue(
            availableProjection[
                VexStreamPlaybackControl.PlayPause
            ].isAvailable,
        )
        assertSame(
            VexStreamPlaybackAct.Play,
            availableProjection[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )

        /*
         * A future V2 DisplayState/availability design must not duplicate this law in UI code merely
         * because activate(Control) can return null. Availability is part of current semantic meaning.
         */
    }

    @Test
    fun reconciledProviderTruthCanEnterFamilyWithoutBecomingFakeControl() {
        val root =
            VexExecutionRoot()

        try {
            val initialSemantic =
                VexStreamPlaybackState.Media(
                    playRequested =
                        true,
                    requestPending =
                        true,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable =
                        true,
                )

            val family =
                ProviderObservationPressureFamily(
                    executionRoot =
                        root,
                    initialState =
                        ProviderObservationPressureFamily.State.Playback(
                            initialSemantic,
                        ),
                )

            val connection =
                ProviderObservationPressureConnection(
                    family =
                        family,
                    initialPending =
                        VexStreamPlaybackPending(
                            playRequested =
                                true,
                        ),
                )

            /*
             * Media3 reports the older false value. Existing Playback reconciliation preserves the
             * newer local play intent before the concrete Family admits semantic truth.
             */
            connection.providerObserved(
                VexStreamPlaybackState.Media(
                    playRequested =
                        false,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable =
                        true,
                ),
            )

            assertEquals(
                ProviderObservationPressureFamily.State.Playback(
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        requestPending =
                            true,
                        phase =
                            VexStreamPlaybackPhase.Buffering,
                        playPauseAvailable =
                            true,
                    ),
                ),
                family.current,
            )

            /*
             * When the provider finally acknowledges the requested value, the same reconciliation
             * clears pending and the Family admits the reconciled semantic State. No Control was
             * fabricated for either provider callback.
             */
            connection.providerObserved(
                VexStreamPlaybackState.Media(
                    playRequested =
                        true,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable =
                        true,
                ),
            )

            assertEquals(
                ProviderObservationPressureFamily.State.Playback(
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        requestPending =
                            false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    ),
                ),
                family.current,
            )
        } finally {
            root.forceStopAll()
        }
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
            "Alpha One",
            relay.current
                .sections[
                    "section.alpha"
                ]
                .items[
                    "item.alpha.one"
                ]
                .label,
        )
        assertEquals(
            "Alpha Two — updated",
            relay.current
                .sections[
                    "section.alpha"
                ]
                .items[
                    "item.alpha.two"
                ]
                .label,
        )
        assertEquals(
            "Beta One",
            relay.current
                .sections[
                    "section.beta"
                ]
                .items[
                    "item.beta.one"
                ]
                .label,
        )

        /*
         * Display walks semantic refs from the CURRENT snapshot. If a copy/update ever drops a list
         * layer or item value, StateListSnapshot construction/resolution fails rather than silently
         * rendering a shorter list.
         */
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

        /*
         * Mutate every caller-owned collection after VexRelay has accepted the initial State.
         * The retained current snapshot must not lose a nested layer merely because the caller still
         * owns the original mutable containers.
         */
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
    fun stateRelayReevaluatesOlderCompletionAgainstCurrentSemanticState() {
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

        /*
         * A newer human/semantic intent enters first.
         *
         * This is intentionally sequential rather than a thread-race test: V1 VexRelay already owns
         * the concurrency proof that update(...) is serialized. The Origin question here is what the
         * serialized boundary should DO with a completion that was authored against older meaning.
         */
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
         * The older async completion arrives after the newer intent. It receives CURRENT State at
         * the Relay boundary rather than directly writing the State it wanted when it began.
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

        /*
         * The completion belonging to the CURRENT intent remains lawful and may advance State.
         */
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
    fun runtimePayloadControlMaterializesActFromActualCurrentStateAndRequest() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                PayloadPressureFamily(
                    executionRoot =
                        root,
                    initialState =
                        PayloadPressureFamily.State.Ready(
                            mediaId =
                                "track.alpha",
                            mediaItemIndex =
                                1,
                            seekAvailable =
                                true,
                            requestedPositionMs =
                                null,
                        ),
                )

            val runtimeRequest =
                VexStreamPlaybackSeekRequest(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                    positionMs =
                        60_000L,
                )
            val control =
                PayloadPressureFamily.Control.Seek(
                    runtimeRequest,
                )

            /*
             * Availability and activation resolve from the same semantic rule. The actual payload
             * value does not need to exist when the Family graph is constructed.
             */
            assertTrue(
                family.isAvailable(
                    control,
                ),
            )

            val act =
                family.activate(
                    control,
                )

            assertEquals(
                control,
                act?.control,
            )
            assertEquals(
                PayloadPressureFamily.State.Ready(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                    seekAvailable =
                        true,
                    requestedPositionMs =
                        60_000L,
                ),
                family.current,
            )

            /*
             * The same Control shape with a stale occurrence does not become available merely because
             * the type matches. Runtime State + runtime Control payload participate in the Act law.
             */
            val staleOccurrence =
                PayloadPressureFamily.Control.Seek(
                    runtimeRequest.copy(
                        mediaItemIndex =
                            2,
                    ),
                )

            assertFalse(
                family.isAvailable(
                    staleOccurrence,
                ),
            )
            assertNull(
                family.activate(
                    staleOccurrence,
                ),
            )


            val providerUnavailable =
                PayloadPressureFamily(
                    executionRoot =
                        root,
                    initialState =
                        PayloadPressureFamily.State.Ready(
                            mediaId =
                                "track.alpha",
                            mediaItemIndex =
                                1,
                            seekAvailable =
                                false,
                            requestedPositionMs =
                                null,
                        ),
                )

            assertFalse(
                providerUnavailable.isAvailable(
                    control,
                ),
            )
            assertNull(
                providerUnavailable.activate(
                    control,
                ),
            )
        } finally {
            root.forceStopAll()
        }
    }

    /**
     * Test-only identity-first list snapshot.
     *
     * refs owns semantic ordering. valuesByRef owns the complete value snapshot for those exact refs.
     * The constructor refuses a dangling/missing layer instead of letting presentation degrade into a
     * shorter list. A parent may nest another StateListSnapshot recursively.
     *
     * This is pressure evidence, not yet a proposed universal collection type.
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
        /*
         * V1 Relay deepCopy protects ownership detachment; stable refs protect semantic identity.
         *
         * Copy from CURRENT and replace only the nested ownership-bearing collection field. Using
         * data-class copy preserves any future root siblings by default instead of reconstructing a
         * selected subset of constructor arguments.
         */
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
     * Origin candidate only: StateRelay.
     *
     * The State-forward name is intentional:
     *
     *     DisplayState
     *       -> State qualified for outward/display meaning
     *
     *     StateRelay
     *       -> Relay/custody relationship whose primary concern is CURRENT State
     *
     * This test reuses the accepted V1 VexRelay currentness law directly, but not merely because a
     * Relay class already exists.
     *
     * The transferable law is:
     *
     *     incoming cause
     *     + exact CURRENT retained State
     *     -> one non-suspending serialized decision
     *     -> publish only changed semantic meaning
     *
     * V1 deepCopy is a separate ownership guarantee: callers/collectors cannot mutate retained truth
     * by alias. Family State may or may not need that exact contract. Likewise V1 source freshness
     * remains source-owned; serialization alone never makes an older provider/result semantically new.
     *
     * This is not yet a V2 Family member and does not imply every Behavior State must implement
     * VexState. Current production Playback State does not yet carry VexState/deepCopy.
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
     * Provider-currentness pressure fixture.
     *
     * The wrapper State satisfies V2 Family kinship while carrying the real current production-shaped
     * Playback semantic State as evidence. It does not replace VexStreamPlaybackFamily.
     */
    private class ProviderObservationPressureFamily(
        executionRoot:
            VexExecutionRoot,
        initialState:
            State,
    ) :
        VexBehaviorV2Family<
            ProviderObservationPressureFamily,
            ProviderObservationPressureFamily.State,
            ProviderObservationPressureFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        sealed interface State :
            VexBehaviorFamily.State<
                ProviderObservationPressureFamily
            > {
            data class Playback(
                val value:
                    VexStreamPlaybackState,
            ) :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ProviderObservationPressureFamily
            > {
            data object None :
                Control
        }

        override val acts =
            vexActs<
                ProviderObservationPressureFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "origin-pressure.playback.provider-observation",
            ) {
                state<State.Playback>()
            }

        /**
         * This is intentionally concrete-Family vocabulary rather than a generic public setter.
         * The Connection may call it only after provider truth has already been reconciled.
         */
        fun providerReconciled(
            state:
                VexStreamPlaybackState,
        ) {
            admit(
                State.Playback(
                    state,
                ),
            )
        }
    }

    /**
     * Keeps the existing Playback responsibility split visible:
     *
     * Media3 observation
     * -> Connection-owned pending reconciliation
     * -> reconciled semantic State
     * -> concrete Family admission
     */
    private class ProviderObservationPressureConnection(
        private val family:
            ProviderObservationPressureFamily,
        initialPending:
            VexStreamPlaybackPending,
    ) {
        private var pending =
            initialPending

        fun providerObserved(
            observed:
                VexStreamPlaybackState,
        ) {
            val reconciled =
                reconcileVexStreamPlaybackObservation(
                    observed =
                        observed,
                    pending =
                        pending,
                )

            pending =
                reconciled.pending

            family.providerReconciled(
                reconciled.state,
            )
        }
    }

    /**
     * Minimal V2 comparison fixture proving runtime payload materialization only.
     *
     * requestedPositionMs is deliberately fixture-local pressure data. This does NOT claim production
     * Playback should store seek position as Behavior State; real Media3 Seek remains provider-owned.
     * The fixture only proves the reusable relationship:
     *
     *     actual CURRENT State value
     *     + actual incoming payload Control
     *     -> materialized Act
     *     -> value-dependent immediate consequence
     */
    private class PayloadPressureFamily(
        executionRoot:
            VexExecutionRoot,
        initialState:
            State,
    ) :
        VexBehaviorV2Family<
            PayloadPressureFamily,
            PayloadPressureFamily.State,
            PayloadPressureFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        sealed interface State :
            VexBehaviorFamily.State<
                PayloadPressureFamily
            > {
            data class Ready(
                val mediaId: String,
                val mediaItemIndex: Int,
                val seekAvailable: Boolean,
                val requestedPositionMs: Long?,
            ) :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                PayloadPressureFamily
            > {
            data class Seek(
                val request:
                    VexStreamPlaybackSeekRequest,
            ) :
                Control
        }

        override val acts =
            vexActs<
                PayloadPressureFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "origin-pressure.playback.payload-control",
            ) {
                state<State.Ready> {
                    on<Control.Seek>(
                        conditionName =
                            "seek-matches-current-media-and-provider-is-available",
                        whenCurrent = { current, seek ->
                            current.seekAvailable &&
                                current.mediaId ==
                                    seek.request.mediaId &&
                                current.mediaItemIndex ==
                                    seek.request.mediaItemIndex
                        },
                    ) { current, seek ->
                        admit(
                            current.copy(
                                requestedPositionMs =
                                    seek.request.positionMs,
                            ),
                        )
                    }
                }
            }
    }
}


// [VXG RealForever]
