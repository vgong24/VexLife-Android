package com.vextreme.vexlife.android.conversation

/**
 * # VexLife Android Conversation
 *
 * Search this symbol when asking:
 *
 * "What is Android Conversation trying to become, and which owners already exist?"
 *
 * This object is documentation/navigation only. It owns no Conversation state, persistence,
 * authority, model runtime, transport, Behavior family, or coroutine execution.
 *
 * ## Start with meaning
 *
 * Long-form feature home:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/conversation/README.md
 *
 * The current foundation deliberately forms the human/owner grammar before product code.
 *
 * ## Android architecture to reuse
 *
 * Merged PR #82 owns the accepted Vex Behavior grammar:
 *
 * State / Control / Act / Effect
 *
 * A Vex Behavior Family is compile-time kinship only. It is NOT Vex Family membership or social
 * context.
 *
 * Merged PR #87 owns VexExecution coroutine custody. VexExecution owns execution mechanics, not
 * Conversation authority, message idempotency, business retry, or model behavior.
 *
 * ## Portable Conversation owners
 *
 * Current VexLife source owns:
 *
 * Home Bridge
 *     authenticated principal/device-session authority
 *
 * Family Space
 *     membership / role / generation / history policy
 *
 * service.conversation
 *     channel / message / context meaning
 *
 * Conversation Store
 *     durable immutable ledger inside the rightful Vex Home
 *
 * Companion runtimes
 *     optional AI participation, separate from ordinary human message persistence
 *
 * Android consumes those contracts; it does not recreate them merely because it renders the
 * conversation.
 *
 * ## Current VexLife Family HTTP producer
 *
 * Current VexLife main exposes POST /api/v1/family/conversation under VF-02C issue #427.
 *
 * APPEND accepts only spaceRef / channelRef / content / expectedMembershipGeneration /
 * idempotencyKey. Speaker, Family recipients, witness audience and Home authority remain
 * server-derived.
 *
 * Android's repository request is aligned to that authority membrane.
 *
 * Two producer predicates remain held:
 *
 * - ordinary serve-browser startup currently does not bind resolveFamilyConversationAuthority, so
 *   production-default Family requests fail closed with FAMILY_SESSION_AUTHORITY_UNAVAILABLE;
 * - the current bounded HTTP message projection omits canonical witnessRefs, so Android must not
 *   reconstruct historical audience truth from current membership.
 *

 * ## Identity laws
 *
 * threadRef
 *     stable conversational continuity
 *
 * channelRef
 *     one immutable audience/currentness generation
 *
 * messageRef
 *     one immutable message event
 *
 * recipientRefs
 *     addressed targets
 *
 * witnessRefs
 *     immutable event audience snapshot
 *
 * Addressing a subset inside a group channel does not create a private audience.
 *
 * ## Talk to Vex
 *
 * The existing TalkToVexScreen is one current entry/destination. It must not hard-code the feature
 * into a one-human / one-AI ontology because current portable Conversation source already supports
 * multi-human Family channels and distinct AI participation.
 *
 * ## Current Android consumer contract
 *
 * [VexConversationRepository] is the Android-facing contract for Conversation-owned state and Send.
 *
 * [InMemoryVexConversationRepository] is an instance-based state-only implementation for tests and
 * Preview. It mirrors the established VexStream pattern; every scenario constructs its own instance.
 * It is not a singleton, service, production Connection, Home, membership owner, or model runtime.
 *
 * [VexConversationState] contains Conversation-owned thread/channel/message/audience projection only.
 * Home reachability, authenticated principal/session truth, and companion availability stay outside
 * that state.
 *
 * Focused repository proof owns the current draft->membership-change->Send problem. The request binds
 * expected channel + membership generation + idempotency identity; a successor generation rejects
 * the stale request without silently upgrading currentness or appending a message. Family recipients
 * are provider-derived from the trusted current channel rather than authored by Android.
 *
 * No VexExecution test fixture is used because there is no Conversation VexExecution consumer yet.
 * The Test Foundation rule is to test the transformation at its actual owner.
 *
 * Production Screen/Journey integration remains held until the shared AppRepositories/runtime graph
 * can be entered without colliding with active work. When admitted, Journey should inject the same
 * in-memory repository through test composition and use journeySignal for external provider changes,
 * following VexStream rather than giving product code test-only mutation methods.
 *
 * ## Current surface composition
 *
 * projectVexConversationSurface is the pure Android presentation-projection boundary over:
 *
 * - Conversation-owned VexConversationThreadState;
 * - authenticated participant identity supplied by the Home/session owner;
 * - VexConversationHomeAvailability supplied by the Home/transport owner;
 * - VexConversationCompanionAvailability supplied by the Companion/runtime owner.
 *
 * It returns VexConversationSurfaceProjection without owning any of those truths.
 *
 * The source deliberately does not call this an Experience. Draft PR #81 uses Experience for a broad
 * product orientation root, while merged PR #82 uses <Name>Experience for a Behavior-local UI
 * membrane. Conversation preserves that unresolved foundation distinction instead of choosing one
 * definition implicitly.
 *
 * No Relay is added yet: current source only needs deterministic composition of already-observed
 * owner truth. Maintained convergence must be earned by real independently-changing provider streams.
 *
 * ## First unresolved architecture pressure
 *
 * Send carries runtime data such as content, currentness/idempotency identity and future optional
 * reply/context references. For the current Family provider, recipients are server-derived rather
 * than caller-authored.
 *
 * Merged PR #82 intentionally holds generic runtime request payload architecture, while VexPort
 * currently activates stable Control identity.
 *
 * Do not force private message content into Control identity, a static UI element, generic
 * diagnostics, or Effect merely to make the existing generic API fit.
 *
 * The first real vertical slice must pressure and explain that boundary before a production
 * Conversation Behavior family is named.
 *
 * ## Private diagnostics
 *
 * Current VexRuntime form submission diagnostics can serialize submitted field values.
 *
 * Conversation text must not enter generic diagnostic history by default. The real Send boundary
 * must preserve semantic observability without copying private message content into unrelated logs.
 *
 * ## Federation
 *
 * Portable SDK source already contains participant-owned friend/group replica comparison and
 * reconciliation semantics. Android does not yet claim a federated Conversation transport or local
 * replica store from that fact.
 *
 * Read the feature home before adding source.
 */
@Suppress("unused")
internal object VexConversationDocs

// [VXG RealForever]
