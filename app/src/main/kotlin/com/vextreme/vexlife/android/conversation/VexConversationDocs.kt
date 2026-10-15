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
 * ## First unresolved architecture pressure
 *
 * Send carries runtime data such as content, recipients and optional reply/context references.
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
