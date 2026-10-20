package com.vextreme.vexlife.android.testing.journey

/**
 * Device/configuration conditions under which the same semantic journey may be exercised.
 *
 * These values do not change the expected semantic history by themselves. They pressure Android
 * realization: layout, resource resolution, and accessibility-sized text while selectors remain
 * stable semantic element IDs.
 */
data class VexJourneyEnvironment(
    val id: String,
    val fontScale: Float? = null,
    val localeTag: String? = null,
) {
    init {
        require(id.isNotBlank()) { "journey environment id must not be blank" }
        fontScale?.let {
            require(it > 0f) { "fontScale must be positive" }
        }
        localeTag?.let {
            require(it.isNotBlank()) { "localeTag must not be blank" }
        }
    }
}

object VexJourneyEnvironments {
    val Baseline = VexJourneyEnvironment(
        id = "baseline",
    )

    val LargeText = VexJourneyEnvironment(
        id = "large-text-1.45x",
        fontScale = 1.45f,
    )

    /**
     * Pre-localization pressure case.
     *
     * Until translated resources exist this uses Android resource fallback, but it already proves
     * journeys/selectors do not depend on English visible text. When localization is admitted, keep
     * this seam and replace/add locale profiles with the supported product locales.
     */
    val AlternateLocaleFallback = VexJourneyEnvironment(
        id = "alternate-locale-fallback",
        localeTag = "es-ES",
    )
}

data class VexJourneyCase(
    val shardId: String,
    val journey: VexJourney,
    val environment: VexJourneyEnvironment,
) {
    val caseRef: String
        get() = "$shardId/${journey.id}@${environment.id}"

    override fun toString(): String =
        "$caseRef :: ${journey.title}"
}
