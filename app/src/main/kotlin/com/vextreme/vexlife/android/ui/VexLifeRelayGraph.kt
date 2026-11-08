package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.conversation.VexConversationHybridRelay
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay

/**
 * Readable session projection of independently-owned Relay nodes.
 *
 * This graph does not construct, start observation, or semantically own its members. Their exact
 * dependency providers construct already-live Relay owners before this projection is assembled.
 */
internal class VexLifeRelayGraph(
    val events:
        EventsConvergenceRelay,
    val conversation:
        VexConversationHybridRelay,
)

// [VXG RealForever]
