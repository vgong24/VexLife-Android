package com.vextreme.vexlife.android.conversation

/**
 * # VexLife Android Conversation
 *
 * Search this symbol when asking:
 *
 * "How does Android Conversation fit together?"
 *
 * This object is documentation/navigation only. It owns no Conversation state, persistence,
 * authority, transport, model runtime, Behavior family, or coroutine execution.
 *
 * Long-form feature home:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/conversation/README.md
 *
 * ## What is Conversation-owned Android state?
 *
 * Start at [VexConversationState] and [VexConversationThreadState].
 *
 * Important identity types:
 *
 * - [VexConversationSpaceRef]
 * - [VexConversationThreadRef]
 * - [VexConversationChannelRef]
 * - [VexConversationMessageRef]
 * - [VexConversationParticipantRef]
 *
 * Thread history is ordered immutable channel segments plus [VexConversationMessage] values.
 *
 * [VexConversationThreadState.segmentRefs] follows the canonical VexLife
 * family-membership-runtime thread-history shape. Android does not invent Joined/Left Conversation
 * message events.
 *
 * ## What is the provider boundary?
 *
 * See [VexConversationRepository].
 *
 * Test/Preview provider:
 *
 * [InMemoryVexConversationRepository]
 *
 * That in-memory implementation follows the established VexStream pattern:
 *
 * - same consumer contract;
 * - one explicit instance per scenario;
 * - state-only external/provider truth;
 * - no Home/network/filesystem/model/federation/Family-membership effect.
 *
 * ## What may Android send?
 *
 * See [VexConversationSendRequest].
 *
 * Current Family Send is aligned to VexLife VF-02C:
 *
 * space/channel currentness + content + idempotency identity.
 *
 * Android does not author trusted speaker, Family recipients, witness audience, membership, role,
 * Home/session capability, or model truth.
 *
 * ## How is currentness protected?
 *
 * [VexConversationSendRequest.expectedChannelRef] and
 * [VexConversationSendRequest.expectedMembershipGeneration] bind the request to the Conversation
 * truth observed during composition.
 *
 * A successor Family generation is rejected rather than silently upgrading the request.
 *
 * [VexConversationIdempotencyKey] preserves exact retry identity.
 *
 * ## What does the UI eventually consume?
 *
 * See [projectVexConversationSurface] and [VexConversationSurfaceProjection].
 *
 * The pure presentation projection combines already-owned inputs:
 *
 * Conversation thread
 * + authenticated participant
 * + Home availability
 * + Companion availability
 * -> Android surface projection
 *
 * It owns no source truth or effects.
 *
 * Companion unavailability does not imply ordinary human Conversation is unavailable.
 *
 * ## Why is it not called Experience?
 *
 * The repository currently has two meanings:
 *
 * - draft PR #81: broad human/product Experience root;
 * - merged PR #82: Behavior-local <Name>Experience UI membrane.
 *
 * Conversation does not choose between those foundation meanings locally.
 *
 * ## What does Conversation NOT own?
 *
 * Home Bridge / authenticated session:
 * current human principal + device/session authority.
 *
 * Family Space / membership runtime:
 * membership, role, generation, history policy and successor channel lifecycle.
 *
 * Conversation Store:
 * durable immutable Home ledger.
 *
 * Companion runtime:
 * optional AI participation.
 *
 * Identity / Relationships / product presentation:
 * friendly display names, avatars and relationship labels.
 *
 * ## Current real producer
 *
 * VexLife VF-02C issue #427 owns the model-free Family Conversation bridge.
 *
 * Current route:
 *
 * POST /api/v1/family/conversation
 *
 * The Android network adapter remains held on two producer predicates:
 *
 * 1. production Family authority resolver reconnection;
 * 2. bounded authenticated exposure of the existing cross-generation
 *    readFamilyThreadHistory(...) truth (ordered segment refs + messages with historical witnesses).
 *
 * ## Testing
 *
 * Current tests belong at:
 *
 * - [InMemoryVexConversationRepositoryTest] for repository/currentness/idempotency/history laws;
 * - [VexConversationSurfaceProjectionTest] for deterministic presentation composition.
 *
 * No VexExecution test fixture is used because Conversation has no VexExecution consumer yet.
 *
 * Production Screen/Journey integration is also held until the canonical app/test graph can inject
 * the repository without colliding with active shared-path work.
 *
 * Read the feature home before widening source.
 */
@Suppress("unused")
internal object VexConversationDocs

// [VXG RealForever]
