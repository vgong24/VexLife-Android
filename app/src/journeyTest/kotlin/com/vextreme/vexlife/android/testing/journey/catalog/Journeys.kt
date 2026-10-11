package com.vextreme.vexlife.android.testing.journey

/**
 * Human-facing domain identity.
 *
 * Deliberately contains no public shardId/journeys plumbing. IDE completion should expose only the
 * named paths authored by the concrete domain object plus ordinary Kotlin Any methods.
 */
abstract class JourneyDomain internal constructor()

/**
 * Human-facing exact path identity.
 *
 * The production/testing engine mapping stays outside this object so a developer cannot accidentally
 * navigate into raw VexJourney internals from the ScreenCrawler config surface.
 */
class JourneyPath internal constructor()

/**
 * Human-discoverable root for every registered journey domain.
 *
 * Start typing "Journeys." and let IDE autocomplete be the map:
 *
 *   Journeys.userProfile.formSubmit
 *   Journeys.debug.logHistoryDismiss
 *   Journeys.gallery.elementsModalsSwap
 */
object Journeys {
    object shell : JourneyDomain() {
        val primaryPeersNativeBack = JourneyPath()
    }

    object events : JourneyDomain() {
        val externalAttention = JourneyPath()
    }

    object debug : JourneyDomain() {
        val logHistoryDismiss = JourneyPath()
        val resetFreshHistory = JourneyPath()
    }

    object gallery : JourneyDomain() {
        val elementsModalsSwap = JourneyPath()
    }

    object userProfile : JourneyDomain() {
        val formSubmit = JourneyPath()
    }

    object vexStream : JourneyDomain() {
        val connectRefreshForget = JourneyPath()
    }

    object talkToVex : JourneyDomain() {
        val placeholder = JourneyPath()
    }
}

/*
 * ENGINE BINDINGS
 *
 * These bindings are intentionally top-level/internal rather than members of Journeys/JourneyDomain/
 * JourneyPath. That keeps engine topology out of IDE completion on the human authoring path.
 */
private data class JourneyDomainBinding(
    val domain: JourneyDomain,
    val shard: VexJourneyShard,
    val paths: Map<JourneyPath, VexJourney>,
)

private val journeyDomainBindings: List<JourneyDomainBinding> =
    listOf(
        JourneyDomainBinding(
            domain = Journeys.shell,
            shard = AppShellJourneys,
            paths = mapOf(
                Journeys.shell.primaryPeersNativeBack to
                    AppShellJourneys.primaryPeersNativeBack,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.events,
            shard = EventsJourneys,
            paths = mapOf(
                Journeys.events.externalAttention to
                    EventsJourneys.externalAttention,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.debug,
            shard = DebugJourneys,
            paths = mapOf(
                Journeys.debug.logHistoryDismiss to
                    DebugJourneys.logHistoryDismiss,
                Journeys.debug.resetFreshHistory to
                    DebugJourneys.resetFreshHistory,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.gallery,
            shard = GalleryJourneys,
            paths = mapOf(
                Journeys.gallery.elementsModalsSwap to
                    GalleryJourneys.elementsModalsSwap,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.userProfile,
            shard = UserProfileJourneys,
            paths = mapOf(
                Journeys.userProfile.formSubmit to
                    UserProfileJourneys.formSubmit,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.vexStream,
            shard = VexStreamJourneys,
            paths = mapOf(
                Journeys.vexStream.connectRefreshForget to
                    VexStreamJourneys.connectRefreshForget,
            ),
        ),
        JourneyDomainBinding(
            domain = Journeys.talkToVex,
            shard = TalkToVexJourneys,
            paths = mapOf(
                Journeys.talkToVex.placeholder to
                    TalkToVexJourneys.placeholder,
            ),
        ),
    )

internal fun registeredJourneyShards(): List<VexJourneyShard> =
    journeyDomainBindings.map { it.shard }

internal fun shardFor(domain: JourneyDomain): VexJourneyShard =
    requireNotNull(
        journeyDomainBindings.firstOrNull { it.domain === domain },
    ) {
        "journey domain is not registered under Journeys"
    }.shard

internal fun journeyFor(path: JourneyPath): VexJourney =
    requireNotNull(
        journeyDomainBindings
            .asSequence()
            .flatMap { it.paths.asSequence() }
            .firstOrNull { (candidate, _) -> candidate === path },
    ) {
        "journey path is not registered under Journeys"
    }.value
