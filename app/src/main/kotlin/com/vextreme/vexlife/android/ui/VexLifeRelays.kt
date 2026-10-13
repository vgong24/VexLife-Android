package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import kotlinx.coroutines.CoroutineScope

/**
 * Session-owned reusable Relay graph.
 *
 * This is composition only: each Relay remains the semantic owner of its own relationship.
 */
internal class VexLifeRelays private constructor(
    val events: EventsConvergenceRelay,
) {
    companion object {
        fun create(
            repositories: AppRepositories,
            scope: CoroutineScope,
        ): VexLifeRelays {
            val events =
                EventsConvergenceRelay(
                    eventsRepository = repositories.events,
                    attentionPreferences = repositories.eventsAttention,
                    attentionLab = repositories.eventsAttentionLab,
                    userProfile = repositories.userProfile,
                )

            events.activate(scope)

            return VexLifeRelays(
                events = events,
            )
        }
    }
}

// [VXG RealForever]
