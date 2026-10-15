package com.vextreme.vexlife.android.conversation

/**
 * # VexLife Android Conversation
 *
 * Long-form feature map:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/conversation/README.md
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
 * ## Current holds
 *
 * Real Family adapter remains held on:
 *
 * 1. production Family session-authority reconnection;
 * 2. authenticated exposure of the existing Family thread-history projection.
 *
 * Production Screen/Journey integration also remains held while shared Android app/test paths are
 * occupied.
 *
 * No generic/direct Send contract is claimed yet.
 */
@Suppress("unused")
internal object VexConversationDocs

// [VXG RealForever]
