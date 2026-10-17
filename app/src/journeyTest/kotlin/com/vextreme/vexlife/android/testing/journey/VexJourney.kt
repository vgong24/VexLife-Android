package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement

data class VexJourney(
    val id: String,
    val title: String,
    val environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    val walk: VexJourneyScope.() -> Unit,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9-]*"))) {
            "journey id must be lowercase kebab-case: $id"
        }
        require(environments.isNotEmpty()) {
            "journey must declare at least one environment"
        }
        require(
            environments.map { it.id }.distinct().size ==
                environments.size,
        ) {
            "journey environments must have unique IDs: $title"
        }
    }

    override fun toString(): String = title
}

fun vexJourney(
    id: String,
    title: String,
    environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    walk: VexJourneyScope.() -> Unit,
): VexJourney =
    VexJourney(
        id = id,
        title = title,
        environments = environments,
        walk = walk,
    )

/**
 * Typed Journey handle for one repository already owned by the app/session composition.
 *
 * This is test vocabulary, not a production service locator. A Journey names the exact dependency it
 * intends to arrange/change; the Android test engine resolves that handle against its own
 * [AppRepositories] instance when the step executes.
 */
class VexJourneyRepository<Repository> internal constructor(
    val repositoryRef: String,
    internal val resolve:
        (AppRepositories) -> Repository,
) {
    init {
        require(repositoryRef.isNotBlank()) {
            "journey repository ref must not be blank"
        }
    }
}

fun <Repository> journeyRepository(
    repositoryRef: String,
    resolve: (AppRepositories) -> Repository,
): VexJourneyRepository<Repository> =
    VexJourneyRepository(
        repositoryRef = repositoryRef,
        resolve = resolve,
    )

/**
 * Current repository handles available to Journey authors.
 *
 * Feature-owned test seams may define a narrower typed handle when mutation requires a concrete test
 * implementation rather than the production-facing repository interface.
 */
object Repositories {
    val logHistory =
        journeyRepository(
            repositoryRef = "repository.log-history",
        ) { repositories ->
            repositories.logHistory
        }

    val userProfile =
        journeyRepository(
            repositoryRef = "repository.user-profile",
        ) { repositories ->
            repositories.userProfile
        }

    val vexMaps =
        journeyRepository(
            repositoryRef = "repository.vex-maps",
        ) { repositories ->
            repositories.vexMaps
        }

    val vexStream =
        journeyRepository(
            repositoryRef = "repository.vexstream",
        ) { repositories ->
            repositories.vexStream
        }

    val events =
        journeyRepository(
            repositoryRef = "repository.events",
        ) { repositories ->
            repositories.events
        }

    val eventsAttention =
        journeyRepository(
            repositoryRef = "repository.events.attention",
        ) { repositories ->
            repositories.eventsAttention
        }

    val eventsAttentionLab =
        journeyRepository(
            repositoryRef = "repository.events.attention-lab",
        ) { repositories ->
            repositories.eventsAttentionLab
        }
}

/**
 * Makes repository acquisition explicit at the Journey call site.
 *
 * The returned value is a typed handle resolved only when the Journey step executes; this does not
 * expose a global repository instance or bypass app/session ownership.
 */
fun <Repository> getRepository(
    repository: VexJourneyRepository<Repository>,
): VexJourneyRepository<Repository> =
    repository

data class VexDynamicElementRef(
    val elementId: String,
    val actionTemplate: ActionElement? = null,
) {
    init {
        require(elementId.isNotBlank()) {
            "dynamic journey element ID must not be blank"
        }
    }
}

fun dynamicElement(
    elementId: String,
    actionTemplate: ActionElement? = null,
): VexDynamicElementRef =
    VexDynamicElementRef(
        elementId = elementId,
        actionTemplate = actionTemplate,
    )

class VexJourneySignal internal constructor(
    val signalRef: String,
    internal val applyTo: (AppRepositories) -> Unit,
) {
    init {
        require(signalRef.isNotBlank()) {
            "journey signal ref must not be blank"
        }
    }
}

fun journeySignal(
    signalRef: String,
    applyTo: (AppRepositories) -> Unit,
): VexJourneySignal =
    VexJourneySignal(
        signalRef = signalRef,
        applyTo = applyTo,
    )

/**
 * One human-readable semantic walk.
 *
 * Preferred authoring language separates:
 *
 * - human action: [tap], [tapElementById], [enterText], [submit], [dismiss], [back];
 * - non-human source change: [externalCause] through one explicitly named repository owner;
 * - observation: [assertVisible], [assertVisibleById].
 *
 * The older [signal], [tapDynamic], [expectVisible] and [type] methods remain implementation
 * primitives while the Android execution owner is reconciled by its active lane. New Journey source
 * should use the intention-level front door above.
 *
 * A Journey may arrange/change source truth and observe resulting UI. It must not author a
 * Convergence/Hybrid output directly.
 */
interface VexJourneyScope {
    fun start(screen: ScreenElement)

    /**
     * Applies one non-human change through its explicit source repository.
     *
     * "External" means external to the Journey's human interaction stream, not necessarily external
     * to the application process. The supplied repository remains the rightful source owner; this
     * method never exposes a writable Convergence/Hybrid Relay.
     */
    fun <Repository> externalCause(
        repository: VexJourneyRepository<Repository>,
        causeRef: String,
        change: Repository.() -> Unit,
    ) {
        require(causeRef.isNotBlank()) {
            "Journey external cause ref must not be blank"
        }

        signal(
            journeySignal(
                signalRef = causeRef,
            ) { repositories ->
                val source =
                    repository.resolve(
                        repositories,
                    )
                source.change()
            },
        )
    }

    /**
     * Legacy execution primitive retained beneath [externalCause].
     */
    fun signal(signal: VexJourneySignal)

    fun tap(element: ActionElement)

    /**
     * Taps one runtime-rendered element through its stable production semantic element ID.
     *
     * [behavesLike] is optional expected-walk meaning only. It does not execute the registered
     * element; it lets the independent expected interpreter understand the runtime instance using an
     * already-declared static intent when that relationship is truthful.
     */
    fun tapElementById(
        elementId: String,
        behavesLike: ActionElement? = null,
    ) {
        require(elementId.isNotBlank()) {
            "Journey runtime element ID must not be blank"
        }

        tapDynamic(
            VexDynamicElementRef(
                elementId = elementId,
                actionTemplate = behavesLike,
            ),
        )
    }

    /**
     * Legacy execution primitive retained beneath [tapElementById].
     */
    fun tapDynamic(element: VexDynamicElementRef)

    fun assertVisible(
        element: UIElement,
    ) {
        expectVisible(
            element,
        )
    }

    fun assertVisibleById(
        elementId: String,
    ) {
        require(elementId.isNotBlank()) {
            "Journey runtime element ID must not be blank"
        }

        expectVisible(
            VexDynamicElementRef(
                elementId = elementId,
            ),
        )
    }

    /**
     * Legacy execution primitive retained beneath [assertVisible].
     */
    fun expectVisible(element: UIElement)

    /**
     * Legacy execution primitive retained beneath [assertVisibleById].
     */
    fun expectVisible(element: VexDynamicElementRef)

    /**
     * Replaces the current editable field contents with [value].
     */
    fun enterText(
        field: TextFieldElement,
        value: String,
    ) {
        type(
            field = field,
            value = value,
        )
    }

    /**
     * Legacy execution primitive retained beneath [enterText].
     */
    fun type(
        field: TextFieldElement,
        value: String,
    )

    fun submit(element: ActionElement)

    fun dismiss(element: UIElement)

    fun back()
}

// [VXG RealForever]
