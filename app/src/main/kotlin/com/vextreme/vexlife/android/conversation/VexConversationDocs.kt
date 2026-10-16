package com.vextreme.vexlife.android.conversation

/**
 * # VexLife Android Conversation
 *
 * Long-form feature map:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/conversation/README.md
 *
 * Current experiential design:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/conversation/design/README.md
 *
 * ## Generic Conversation
 *
 * Start at [VexConversationState], [VexConversationThreadState], [VexConversationMessage] and
 * [VexConversationRepository].
 *
 * Generic Conversation does not require Vex Family membership generation.
 *
 * [VexConversationMessage.sequence] is zero-based; Conversation Store assigns sequence 0 to the
 * first canonical message in a channel.
 *
 * ## Vex Family Conversation
 *
 * Family-specific currentness is layered through:
 *
 * - [VexFamilyConversationThreadBinding]
 * - [VexFamilyConversationMessageBinding]
 * - [VexFamilyConversationSendRequest]
 * - [VexFamilyConversationRepository]
 *
 * Test/Preview implementation:
 *
 * [InMemoryVexFamilyConversationRepository]
 *
 * Canonical Family Space generation begins at 1; message sequence is independently zero-based.
 *
 * VF-02C message retry identity is scoped by spaceRef + channelRef + authenticated principalRef +
 * idempotencyKey. The in-memory Family provider mirrors that semantic scope and resolves retries from
 * deterministic message identity already present in current Conversation state rather than a
 * process-local idempotency map.
 *
 * The Family caller supplies routing/content/currentness/idempotency only. Trusted speaker,
 * recipients, witnesses, membership/role, Home authority and model truth remain provider-owned.
 *
 * ## Surface projection
 *
 * Generic presentation composition:
 *
 * [projectVexConversationSurface]
 *
 * Family-only Send eligibility:
 *
 * [projectVexFamilyConversationSendAvailability]
 *
 * A generic direct Conversation does not silently gain Family Send semantics.
 *
 * ## Hybrid
 *
 * Independently changing Conversation/runtime/Lab facts form one mixed-provenance relationship in:
 *
 * [VexConversationHybridRelay]
 *
 * Read its `dependencies()`, public observation methods, `processStates()`, then `outputStates`
 * to see the current relationship.
 *
 * Hybrid is a specialized Compound convergence role: the Relay owns which Home/Companion facts are
 * effective when Lab overrides exist while canonical ownership and provenance remain intact. The existing
 * [projectVexConversationSurface] function remains a pure projection over already-effective facts;
 * it does not interpret Lab controls.
 *
 * ~~~text
 * CONVERGENCE != PROJECTION
 * SOURCE_BINDING != LAB_PERTURBATION
 * LAB_OVERRIDE != REPOSITORY_TRUTH
 * ~~~
 *
 * Synthetic provider messages still enter through [VexConversationRepository] before Convergence
 * can observe them. Convergence never manufactures Conversation messages or Family currentness.
 *
 * ## Conversation Lab / room presentation
 *
 * Feature-owned synthetic-provider conditions:
 *
 * [VexConversationLab]
 *
 * Generic room renderer:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/conversation/VexConversationRoom.kt
 *
 * Lab/Family preview pressure:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/conversation/VexConversationRoomPreview.kt
 *
 * The Lab publishes synthetic external truth through the existing in-memory provider seam and may
 * also hold explicit Home/Companion availability overrides. Those overrides do not rewrite
 * Conversation repository truth; [VexConversationHybridRelay] reconciles them with current
 * observations.
 *
 * The Lab does not perform Victor's product Send or become another Conversation state owner. The
 * room renderer consumes [VexConversationSurfaceProjection] and adds no parallel room-state model.
 * It also does not choose Family Send semantics; the future registered Screen supplies its durable
 * composer/Send controls and action contract.
 *
 * ## Real Family producer
 *
 * VexLife VF-02C issue #427 owns the current model-free Family Conversation bridge.
 *
 * A truthful Android adapter composes:
 *
 * Family bootstrap
 *     current principal / room / current audience / Family Companion lineage
 *
 * plus authenticated readFamilyThreadHistory(...) projection
 *     ordered immutable channel segments + historical messages/witnesses
 *
 * into generic Android Conversation state + explicit Family binding.
 *
 * ## Current producer status / hold
 *
 * The VexLife producer chain for authenticated Family conversation is now accepted through:
 *
 * VF-02C bridge
 * -> durable Family channel binding
 * -> same-origin server composition
 * -> real VEX_CORE / Home session-authority reconnection
 * -> lived Family HTTP APPEND + READ proof.
 *
 * The remaining producer hold is narrower: current HTTP READ is current-channel scoped, while the
 * accepted Family membership runtime already owns cross-generation readFamilyThreadHistory(...)
 * with ordered segments and historical witness truth.
 *
 * A truthful Android Home-backed adapter therefore remains held only until that canonical
 * cross-generation history is exposed through the authenticated server boundary (or an exactly
 * equivalent safe projection).
 *
 * Feature-local Lab and room rendering are accepted on main through PR #99. Conversation
 * Convergence and explicit Lab availability overrides are accepted on main through PR #104.
 * Registered production Screen, app-session repository wiring, Debug Menu doorway and Journey
 * integration remain separate shared composition effects until their exact paths are safely
 * reconciled.
 *
 * No generic/direct Send contract is claimed yet.
 */
@Suppress("unused")
internal object VexConversationDocs {
    /**
     * Stable semantic identity for this feature.
     *
     * The human/product/package name may evolve; this ref is intentionally not derived from that
     * spelling so design records and source history remain searchable across a rename.
     */
    const val FEATURE_REF: String = "feature.vexlife.conversation"

    const val CURRENT_FEATURE_NAME: String = "Conversation"

    val historicalSearchNames: List<String> = listOf(
        "VexConversation",
    )
}

// [VXG RealForever]
