package com.vextreme.vexlife.android.testing.journey

/**
 * Human-discoverable root for every registered journey domain.
 *
 * Start here:
 *
 *   Journeys.userProfile.formSubmit
 *   Journeys.debug.logHistoryDismiss
 *   Journeys.gallery.elementsModalsSwap
 *
 * Each property is the canonical shard object itself. VexJourneyCatalog derives from [shards], so
 * ScreenCrawler/tests do not maintain another journey registry.
 */
object Journeys {
    val shell = AppShellJourneys
    val debug = DebugJourneys
    val gallery = GalleryJourneys
    val userProfile = UserProfileJourneys
    val vexStream = VexStreamJourneys
    val talkToVex = TalkToVexJourneys

    /**
     * Canonical registered journey domains.
     *
     * When a genuinely new domain/shard is created, expose it once above and add that same property
     * here. Consumers discover journeys through Journeys.<domain> and never copy IDs.
     */
    val shards: List<VexJourneyShard> = listOf(
        shell,
        debug,
        gallery,
        userProfile,
        vexStream,
        talkToVex,
    )
}
