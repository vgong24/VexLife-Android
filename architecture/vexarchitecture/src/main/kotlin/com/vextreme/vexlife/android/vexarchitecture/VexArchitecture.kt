package com.vextreme.vexlife.android.vexarchitecture

import kotlin.reflect.KClass

/**
 * Global semantic-directory contract.
 *
 * "Architecture" means "start here when I know the concern but not the implementation file".
 * Only roots/groups implement this contract; Cases, Controls and platform objects intentionally do
 * not, so IDE "find implementations" remains a bounded architecture index.
 */
interface VexArchitectureContract {
    /**
     * Stable absolute semantic identity, for example vexstream.music.player.playback.
     *
     * It is explicit rather than derived from Kotlin packages because source may move while
     * diagnostic/semantic identity remains stable.
     */
    val architectureRef: String

    /**
     * Immediate semantic parent used to walk upward and to inherit realm participation.
     *
     * Null means top-level architecture root, not "unowned".
     */
    val parent: VexArchitectureContract?

    /**
     * Whether THIS architecture realm participates.
     *
     * The deliberately narrow word "enabled" does not mean runtime available, authorized, or legal
     * from the current Case.
     */
    val enabled: Boolean

    /**
     * Immediate child roots used as the next directory level.
     *
     * Empty is valid: fractal vocabulary does not require every node to invent children.
     */
    val children: List<VexArchitectureContract>
        get() = emptyList()

    /**
     * Parent-aware participation.
     *
     * Disabling VexStream can therefore close Music -> Player -> Playback without descendant
     * feature-flag conditionals.
     */
    val effectiveEnabled: Boolean
        get() = enabled && (parent?.effectiveEnabled ?: true)
}

/**
 * Type-level coordinate for one bounded truth realm.
 *
 * "Realm" is chosen instead of "Feature" because Playback and Queue can be independent truths inside
 * one feature. The marker exists only to stop cross-realm wiring at compile time.
 */
interface VexRealm

/**
 * One kind of current truth inside realm R.
 *
 * "Case" is narrower than "State": a Case names which semantic situation applies; it may still carry
 * data without turning every data mutation into a new global state-machine node.
 */
interface VexCase<R : VexRealm>

/**
 * Architecture root whose local relationships share realm R.
 *
 * Group nodes such as Music or Player do not use this contract unless they truly own current Cases.
 */
interface VexRealmArchitecture<R : VexRealm> : VexArchitectureContract

/**
 * Erased readable view of a typed transition.
 *
 * Maps and Controls do not need to carry Before/After generics, while construction remains typed in
 * VexTransition.
 */
interface VexTransitionView<R : VexRealm> {
    /** Realm that owns this relationship. */
    val architecture: VexRealmArchitecture<R>

    /** Absolute semantic identity derived from architecture root + relative name. */
    val transitionRef: String

    /** Source Case class retained for map validation and debugging. */
    val before: KClass<*>

    /** Destination Case class retained for map validation and debugging. */
    val after: KClass<*>

    /** Optional named fact that narrows availability. */
    val conditionRef: String?

    /** Read current truth only; this must never realize the external/platform effect. */
    fun isAvailableFrom(current: VexCase<R>): Boolean
}

/**
 * One typed doorway from Before to After inside exactly one realm R.
 *
 * "Transition" is relationship topology. Android/native execution belongs below architecture.
 */
class VexTransition<
    R : VexRealm,
    Before : VexCase<R>,
    After : VexCase<R>,
> @PublishedApi internal constructor(
    override val architecture: VexRealmArchitecture<R>,
    /** Relative human meaning such as play. */
    val name: String,
    override val before: KClass<Before>,
    override val after: KClass<After>,
    /** Relative condition name; null deliberately means unconditional. */
    val conditionName: String?,
    /** Current-Case predicate, kept effect-free. */
    private val whenCurrent: (Before) -> Boolean,
) : VexTransitionView<R> {
    override val transitionRef: String =
        architecture.childRef(name)

    override val conditionRef: String? =
        conditionName?.let(architecture::childRef)

    override fun isAvailableFrom(
        current: VexCase<R>,
    ): Boolean {
        if (!before.isInstance(current)) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        return whenCurrent(current as Before)
    }

    override fun toString(): String =
        transitionRef +
            ": " +
            before.simpleName +
            " -> " +
            after.simpleName +
            (conditionRef?.let { " when " + it } ?: "")
}

/**
 * Human/requestable meaning inside realm R.
 *
 * "Control" intentionally does not mean Android MediaController. A Control may contain multiple
 * candidate transitions when one human request such as Play/Pause resolves differently by Case.
 */
class VexControl<R : VexRealm> internal constructor(
    /** Realm that namespaces and gates this Control. */
    val architecture: VexRealmArchitecture<R>,
    /** Relative human name such as play-pause. */
    val name: String,
    /** Candidate relationships; current Placement may resolve at most one. */
    val candidates: List<VexTransitionView<R>>,
) {
    /** Stable absolute semantic identity independent of Kotlin symbol names. */
    val controlRef: String =
        architecture.childRef(name)

    /** Read-only resolution; execution remains outside VexArchitecture. */
    internal fun resolveFrom(
        currentCase: VexCase<R>,
        currentPlacement: VexPlacementView<R>,
    ): VexTransitionView<R>? {
        check(currentPlacement.architecture === architecture) {
            "control " + controlRef + " belongs to " + architecture.architectureRef +
                ", not " + currentPlacement.architecture.architectureRef
        }

        if (!architecture.effectiveEnabled) {
            return null
        }

        val availableByRef =
            currentPlacement.transitions.associateBy {
                it.transitionRef
            }

        val matches =
            candidates.mapNotNull { candidate ->
                availableByRef[candidate.transitionRef]
                    ?.also { available ->
                        check(available === candidate) {
                            "transitionRef " + candidate.transitionRef +
                                " was rebound to another relationship"
                        }
                    }
                    ?.takeIf {
                        candidate.isAvailableFrom(currentCase)
                    }
            }

        check(matches.size <= 1) {
            "control " + controlRef + " is ambiguous for " +
                currentPlacement.caseType.simpleName
        }

        return matches.singleOrNull()
    }
}

/**
 * Erased Placement view for maps and Control resolution.
 */
interface VexPlacementView<R : VexRealm> {
    /** Realm whose local neighborhood this Placement describes. */
    val architecture: VexRealmArchitecture<R>

    /** Exact Case class whose neighborhood is described. */
    val caseType: KClass<*>

    /** Relationships that legitimately arrive here. */
    val arrivals: List<VexTransitionView<R>>

    /** Relationships that can legitimately leave here. */
    val transitions: List<VexTransitionView<R>>
}

/**
 * Local neighborhood around one exact current Case.
 *
 * "Placement" answers "how can I arrive here?" and "where can I go?" without creating another state
 * owner or requiring a global state-machine map.
 */
class VexPlacement<
    R : VexRealm,
    Current : VexCase<R>,
> internal constructor(
    override val architecture: VexRealmArchitecture<R>,
    override val caseType: KClass<Current>,
    /** Typed arrivals preserve compiler checking during declaration. */
    val typedArrivals: List<VexTransition<R, *, Current>>,
    /** Typed exits preserve compiler checking during declaration. */
    val typedTransitions: List<VexTransition<R, Current, *>>,
) : VexPlacementView<R> {
    override val arrivals: List<VexTransitionView<R>>
        get() = typedArrivals

    override val transitions: List<VexTransitionView<R>>
        get() = typedTransitions
}

/**
 * Sentence-like Placement builder.
 */
class VexPlacementBuilder<
    R : VexRealm,
    Current : VexCase<R>,
> @PublishedApi internal constructor(
    private val architecture: VexRealmArchitecture<R>,
    private val caseType: KClass<Current>,
) {
    /** Identity-keyed arrivals reject duplicate local meaning. */
    private val arrivalsByRef =
        linkedMapOf<String, VexTransition<R, *, Current>>()

    /** Identity-keyed exits reject duplicate local meaning. */
    private val transitionsByRef =
        linkedMapOf<String, VexTransition<R, Current, *>>()

    /** Declare one legitimate way to arrive at this Case. */
    fun <Before : VexCase<R>> arrivedBy(
        transition: VexTransition<R, Before, Current>,
    ) {
        check(transition.architecture === architecture)
        check(transition.after == caseType)
        check(
            arrivalsByRef.put(
                transition.transitionRef,
                transition,
            ) == null,
        ) {
            "duplicate arrival transitionRef: " + transition.transitionRef
        }
    }

    /** Declare one legitimate way to leave this Case. */
    fun <After : VexCase<R>> can(
        transition: VexTransition<R, Current, After>,
    ) {
        check(transition.architecture === architecture)
        check(transition.before == caseType)
        check(
            transitionsByRef.put(
                transition.transitionRef,
                transition,
            ) == null,
        ) {
            "duplicate outgoing transitionRef: " + transition.transitionRef
        }
    }

    /** Freeze the readable declaration into an immutable Placement. */
    @PublishedApi
    internal fun build(): VexPlacement<R, Current> =
        VexPlacement(
            architecture = architecture,
            caseType = caseType,
            typedArrivals = arrivalsByRef.values.toList(),
            typedTransitions = transitionsByRef.values.toList(),
        )
}

/**
 * Current truth paired with the local relationships that explain it.
 *
 * A debugger can therefore inspect "what is true?" and "what can happen?" at one coordinate.
 */
class VexCurrent<
    R : VexRealm,
    Current : VexCase<R>,
> internal constructor(
    /** Exact current domain truth. */
    val case: Current,
    /** Exact local neighborhood for that Case type. */
    val placement: VexPlacement<R, Current>,
) {
    /** Resolve one requestable meaning without performing its effect. */
    fun availableTransition(
        control: VexControl<R>,
    ): VexTransitionView<R>? =
        control.resolveFrom(
            currentCase = case,
            currentPlacement = placement,
        )
}

/**
 * Derived orientation map.
 *
 * Placements remain the source declarations. This map validates/projects them; it never becomes a
 * reducer or execution authority.
 */
class VexArchitectureMap<R : VexRealm> internal constructor(
    /** Realm root represented by this map. */
    val architecture: VexRealmArchitecture<R>,
    /** One Placement per represented Case type. */
    val placements: List<VexPlacementView<R>>,
    /** Requestable meanings known to the realm. */
    val controls: List<VexControl<R>>,
    /** Unique outgoing relationships derived from Placements. */
    val transitions: List<VexTransitionView<R>>,
) {
    /** Case types are derived rather than separately registered. */
    val cases: List<KClass<*>>
        get() =
            placements.map {
                it.caseType
            }

    /** Human/debug projection of the local architecture; never an execution surface. */
    fun describe(): String =
        buildString {
            appendLine(
                architecture.architectureRef +
                    " enabled=" +
                    architecture.effectiveEnabled,
            )
            placements.forEach { placement ->
                appendLine(
                    placement.caseType.simpleName.orEmpty(),
                )
                placement.arrivals.forEach {
                    appendLine("  arrivedBy " + it)
                }
                placement.transitions.forEach {
                    appendLine("  can " + it)
                }
            }
            if (controls.isNotEmpty()) {
                appendLine("controls")
                controls.forEach { control ->
                    appendLine(
                        "  " +
                            control.controlRef +
                            " -> " +
                            control.candidates.joinToString {
                                it.transitionRef
                            },
                    )
                }
            }
        }.trimEnd()
}

/**
 * Derive an absolute child ref from one architecture-local name.
 *
 * Kebab-case keeps semantic refs visually distinct from Kotlin symbol naming.
 */
@PublishedApi
internal fun VexArchitectureContract.childRef(
    name: String,
): String {
    require(
        name.matches(
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*"),
        ),
    ) {
        "architecture-relative names must be lowercase kebab-case: " + name
    }

    return architectureRef + "." + name
}

/** Create one unconditional typed relationship using a relative human name. */
inline fun <
    R : VexRealm,
    reified Before : VexCase<R>,
    reified After : VexCase<R>,
> VexRealmArchitecture<R>.transition(
    name: String,
): VexTransition<R, Before, After> =
    VexTransition(
        architecture = this,
        name = name,
        before = Before::class,
        after = After::class,
        conditionName = null,
        whenCurrent = {
            true
        },
    )

/** Create one typed relationship narrowed by a named fact of the exact current Case. */
inline fun <
    R : VexRealm,
    reified Before : VexCase<R>,
    reified After : VexCase<R>,
> VexRealmArchitecture<R>.transition(
    name: String,
    conditionName: String,
    noinline whenCurrent: (Before) -> Boolean,
): VexTransition<R, Before, After> =
    VexTransition(
        architecture = this,
        name = name,
        before = Before::class,
        after = After::class,
        conditionName = conditionName,
        whenCurrent = whenCurrent,
    )

/** Create one semantic Control from relationships in this exact realm. */
fun <R : VexRealm> VexRealmArchitecture<R>.control(
    name: String,
    vararg candidates: VexTransitionView<R>,
): VexControl<R> {
    require(candidates.isNotEmpty()) {
        "control must contain at least one transition"
    }
    require(
        candidates.all {
            it.architecture === this
        },
    ) {
        "every control transition must belong to " + architectureRef
    }
    require(
        candidates
            .map {
                it.transitionRef
            }
            .distinct()
            .size == candidates.size,
    ) {
        "control transition refs must be unique"
    }

    return VexControl(
        architecture = this,
        name = name,
        candidates = candidates.toList(),
    )
}

/** Create one Placement constrained to the exact Current Case type. */
inline fun <
    R : VexRealm,
    reified Current : VexCase<R>,
> VexRealmArchitecture<R>.placement(
    block: VexPlacementBuilder<R, Current>.() -> Unit,
): VexPlacement<R, Current> =
    VexPlacementBuilder(
        architecture = this,
        caseType = Current::class,
    ).apply(block)
        .build()

/** Pair exact current truth with its exact local neighborhood. */
fun <
    R : VexRealm,
    Current : VexCase<R>,
> vexCurrent(
    case: Current,
    placement: VexPlacement<R, Current>,
): VexCurrent<R, Current> =
    VexCurrent(
        case = case,
        placement = placement,
    )

/**
 * Derive and validate a realm map from local Placements and Controls.
 *
 * The same Transition object must appear as the source exit and destination arrival. Control refs
 * are also unique because semantic identity must never silently mean two things in one realm.
 */
fun <R : VexRealm> VexRealmArchitecture<R>.architectureMap(
    placements: List<VexPlacementView<R>>,
    controls: List<VexControl<R>>,
): VexArchitectureMap<R> {
    require(architectureRef.isNotBlank()) {
        "architectureRef must be non-blank"
    }
    require(placements.isNotEmpty()) {
        "architecture map requires at least one Placement"
    }
    require(
        placements.all {
            it.architecture === this
        },
    ) {
        "every Placement must belong to " + architectureRef
    }
    require(
        controls.all {
            it.architecture === this
        },
    ) {
        "every Control must belong to " + architectureRef
    }
    require(
        controls
            .map {
                it.controlRef
            }
            .distinct()
            .size == controls.size,
    ) {
        "architecture " + architectureRef + " contains duplicate controlRef"
    }

    val byCase =
        placements.associateBy {
            it.caseType
        }
    check(byCase.size == placements.size) {
        "architecture " + architectureRef + " contains duplicate Case placements"
    }

    val occurrences =
        placements.flatMap {
            it.arrivals + it.transitions
        }
    occurrences
        .groupBy {
            it.transitionRef
        }
        .forEach { (ref, relationships) ->
            val first =
                relationships.first()
            check(
                relationships.all {
                    it === first
                },
            ) {
                "architecture " +
                    architectureRef +
                    " re-declared transitionRef " +
                    ref
            }
        }

    val outgoing =
        placements
            .flatMap {
                it.transitions
            }
            .distinctBy {
                it.transitionRef
            }

    placements.forEach { placement ->
        placement.transitions.forEach { transition ->
            val destination =
                byCase[transition.after]
                    ?: error(
                        "architecture " +
                            architectureRef +
                            " has no Placement for destination " +
                            transition.after.simpleName,
                    )
            check(
                destination.arrivals.any {
                    it === transition
                },
            ) {
                transition.transitionRef +
                    " leaves " +
                    transition.before.simpleName +
                    " but " +
                    transition.after.simpleName +
                    " does not name the same arrival"
            }
        }

        placement.arrivals.forEach { transition ->
            val source =
                byCase[transition.before]
                    ?: error(
                        "architecture " +
                            architectureRef +
                            " has no Placement for source " +
                            transition.before.simpleName,
                    )
            check(
                source.transitions.any {
                    it === transition
                },
            ) {
                transition.transitionRef +
                    " arrives at " +
                    transition.after.simpleName +
                    " but " +
                    transition.before.simpleName +
                    " does not name the same exit"
            }
        }
    }

    controls.forEach { control ->
        control.candidates.forEach { candidate ->
            check(
                outgoing.any {
                    it === candidate
                },
            ) {
                "control " +
                    control.controlRef +
                    " references transition " +
                    candidate.transitionRef +
                    " that is not in the architecture map"
            }
        }
    }

    return VexArchitectureMap(
        architecture = this,
        placements = placements.toList(),
        controls = controls.toList(),
        transitions = outgoing,
    )
}

// [VXG RealForever]
