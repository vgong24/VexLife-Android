package com.vextreme.vexlife.android.relay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Executable Compound Relay authoring blueprint.
 *
 * This fixture is intentionally complex enough to resemble a real feature relationship:
 * provider truth + current Screen + visible targets + transient blocking -> one derived output.
 *
 * The important authoring property is inside [BlueprintRelay.processStates]:
 * there are NO deepCopy() calls in the reducer.
 *
 * Relay owns the ownership boundaries around reducer execution. Feature authors should therefore
 * spend processStates() on reconciliation and domain meaning, not defensive-copy ceremony.
 */
class VexCompoundRelayBlueprintTest {
    /**
     * Example mutable-backed source truth.
     *
     * Mutable ownership is declared once at the state boundary through deepCopy(); reducers that
     * consume this value do not need to remember to copy it again merely because they return it.
     */
    private data class ProviderSnapshot(
        val revision: Long,
        val itemRefs: MutableList<String>,
    ) : VexDeepCopyable<ProviderSnapshot> {
        override fun deepCopy(): ProviderSnapshot =
            copy(
                itemRefs = itemRefs.toMutableList(),
            )
    }

    private data class BlueprintInput(
        val screenId: String? = null,
        val source: ProviderSnapshot? = null,
        val visibleRefs: MutableSet<String>? = null,
        val transientBlocked: Boolean? = null,
    ) : VexInputState<BlueprintInput> {
        override fun deepCopy(): BlueprintInput =
            copy(
                source = source?.deepCopy(),
                visibleRefs = visibleRefs?.toMutableSet(),
            )
    }

    private enum class BlueprintAvailability {
        UNOBSERVED,
        HELD_OTHER_SCREEN,
        HELD_TRANSIENT,
        READY,
    }

    private data class BlueprintOutput(
        val visibleItemRefs: MutableList<String> = mutableListOf(),
        val availability: BlueprintAvailability =
            BlueprintAvailability.UNOBSERVED,
    ) : VexOutputState<BlueprintOutput> {
        override fun deepCopy(): BlueprintOutput =
            copy(
                visibleItemRefs = visibleItemRefs.toMutableList(),
            )
    }

    /**
     * Copy-paste structure for a new Compound Relay.
     *
     * Keep this order unless the domain genuinely needs something different:
     *
     * 1. REFERENCES  — name previous input/output needed for reasoning.
     * 2. RECONCILE   — merge the partial patch into one coherent current input.
     * 3. DERIVE      — calculate semantic/domain meaning from coherent input.
     * 4. PROJECT     — construct the output value consumers are allowed to depend on.
     * 5. RETURN      — return one temporary candidate; framework decides whether/what to retain.
     *
     * Do not add deepCopy() in these phases merely for Relay ownership. send() already snapshots:
     * caller -> reducer, retained current -> reducer, changed candidate branch -> retained truth,
     * retained truth -> external caller/collector.
     */
    private class BlueprintRelay :
        VexCompoundRelay<BlueprintInput, BlueprintOutput>(
            relayRef = "relay.blueprint.compound",
            initialInputState = BlueprintInput(),
            initialOutputState = BlueprintOutput(),
        ) {
        fun patch(
            screenId: String? = null,
            source: ProviderSnapshot? = null,
            visibleRefs: MutableSet<String>? = null,
            transientBlocked: Boolean? = null,
        ) {
            send(
                BlueprintInput(
                    screenId = screenId,
                    source = source,
                    visibleRefs = visibleRefs,
                    transientBlocked = transientBlocked,
                ),
            )
        }

        override fun processStates(
            inputState: BlueprintInput,
            currentState: VexCompoundState<
                BlueprintInput,
                BlueprintOutput
            >,
        ): VexCompoundState<
            BlueprintInput,
            BlueprintOutput
        > {
            // 1. REFERENCES
            // Name the previous state once so the rest reads like domain reasoning.
            val previousInput =
                currentState.inputState

            // 2. RECONCILE
            // A partial patch may supply one or several facts. Resolve all of them into exactly one
            // coherent input value before deriving any output meaning.
            val coherentInput =
                previousInput.copy(
                    screenId =
                        inputState.screenId
                            ?: previousInput.screenId,
                    source =
                        inputState.source
                            ?: previousInput.source,
                    visibleRefs =
                        inputState.visibleRefs
                            ?: previousInput.visibleRefs,
                    transientBlocked =
                        inputState.transientBlocked
                            ?: previousInput.transientBlocked,
                )

            // 3. DERIVE
            // Calculate domain meaning from coherentInput only. Avoid re-reading the partial patch
            // below this point; that prevents "some logic used old state, some used new patch" drift.
            val visibleItemRefs =
                coherentInput.source
                    ?.itemRefs
                    .orEmpty()
                    .filter { itemRef ->
                        itemRef in
                            coherentInput.visibleRefs
                                .orEmpty()
                    }
                    .toMutableList()

            val availability =
                when {
                    coherentInput.source == null ||
                        coherentInput.screenId == null ||
                        coherentInput.transientBlocked == null ->
                        BlueprintAvailability.UNOBSERVED

                    coherentInput.screenId != TARGET_SCREEN ->
                        BlueprintAvailability.HELD_OTHER_SCREEN

                    coherentInput.transientBlocked ->
                        BlueprintAvailability.HELD_TRANSIENT

                    else ->
                        BlueprintAvailability.READY
                }

            // 4. PROJECT
            // Construct exactly the semantic output consumers should observe.
            val output =
                BlueprintOutput(
                    visibleItemRefs = visibleItemRefs,
                    availability = availability,
                )

            // 5. RETURN
            // Return a candidate only. Do not deepCopy() here for Relay ownership.
            // VexCompoundRelay.send() compares it, snapshots only changed branches when necessary,
            // preserves unchanged retained branches by identity, and performs the atomic commit.
            return currentState.copy(
                inputState = coherentInput,
                outputState = output,
            )
        }
    }

    @Test
    fun blueprintReducerNeedsNoOwnershipCopiesInsideProcessStates() {
        val relay =
            BlueprintRelay()

        val callerItems =
            mutableListOf(
                "event.a",
                "event.b",
            )
        val callerVisibleRefs =
            mutableSetOf(
                "event.a",
            )

        relay.patch(
            screenId = TARGET_SCREEN,
            source =
                ProviderSnapshot(
                    revision = 41L,
                    itemRefs = callerItems,
                ),
            visibleRefs = callerVisibleRefs,
            transientBlocked = false,
        )

        // Caller mutation after send() cannot mutate retained Relay truth.
        callerItems +=
            "caller.mutation"
        callerVisibleRefs +=
            "event.b"

        assertEquals(
            listOf(
                "event.a",
                "event.b",
            ),
            relay.currentInput.source?.itemRefs,
        )
        assertEquals(
            setOf(
                "event.a",
            ),
            relay.currentInput.visibleRefs,
        )
        assertEquals(
            listOf(
                "event.a",
            ),
            relay.currentOutput.visibleItemRefs,
        )
        assertEquals(
            BlueprintAvailability.READY,
            relay.currentOutput.availability,
        )

        // Consumer mutation of an exposed mutable-backed output cannot mutate retained truth.
        val exposedOutput =
            relay.currentOutput
        exposedOutput.visibleItemRefs +=
            "consumer.mutation"

        assertFalse(
            "consumer.mutation" in
                relay.currentOutput.visibleItemRefs,
        )

        // One patch may change several facts atomically. The reducer reconciles them together.
        relay.patch(
            screenId = "home",
            transientBlocked = true,
        )

        assertEquals(
            "home",
            relay.currentInput.screenId,
        )
        assertEquals(
            true,
            relay.currentInput.transientBlocked,
        )
        assertEquals(
            2L,
            relay.current.revision,
        )
        assertEquals(
            BlueprintAvailability.HELD_OTHER_SCREEN,
            relay.currentOutput.availability,
        )
    }

    private companion object {
        const val TARGET_SCREEN: String =
            "events"
    }
}

// [VXG RealForever]
