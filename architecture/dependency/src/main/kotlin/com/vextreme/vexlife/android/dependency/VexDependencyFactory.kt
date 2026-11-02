package com.vextreme.vexlife.android.dependency

import java.util.IdentityHashMap

/**
 * One explicit construction declaration in the readable dependency graph.
 *
 * WHAT THIS IS / RELATES TO
 * A provider declares the prerequisites needed to construct one Value. [VexDependencyFactory] owns
 * traversal, prerequisite ordering, memoization, cycle detection, and lifetime reuse. The created
 * Value receives concrete dependencies and does not need to know the factory exists.
 *
 * WHY THIS SHAPE EXISTS
 * Provider object identity is the construction coordinate, while [readableName] is only a human
 * label. This lets two differently configured declarations use the same provider class without
 * inventing a global qualifier/string-ID system or discovering relationships through reflection.
 *
 * RESPONSIBILITY FENCE
 * Providers declare construction only. Do not make providers semantic owners, execution
 * RelationshipIds, Logger/trace identities, lifecycle managers, or service locators. Do not infer
 * prerequisites by scanning constructors/classes/packages; relationships remain explicit source.
 */
abstract class VexDependencyProvider<
    Value : Any,
>(
    val dependencies:
        List<
            VexDependencyProvider<*>
        > =
        emptyList(),
    private val declaredReadableName:
        String? =
        null,
) {
    val readableName:
        String
        get() =
            declaredReadableName
                ?: requireNotNull(
                    this::class.qualifiedName
                        ?: this::class.simpleName,
                ) {
                    "VexDependencyProvider must have a readable named declaration"
                }

    protected abstract fun create(
        dependencies:
            VexDependencyBuildScope,
    ):
        Value

    internal fun build(
        dependencies:
            VexDependencyBuildScope,
    ):
        Value =
        create(
            dependencies,
        )

    companion object {
        /**
         * Explicit provider declaration without a one-off subclass.
         *
         * This removes naming boilerplate while preserving the same source-readable prerequisite
         * graph, object-identity construction coordinate, and narrow build scope.
         */
        fun <Value : Any> named(
            readableName:
                String,
            dependencies:
                List<
                    VexDependencyProvider<*>
                > =
                emptyList(),
            build:
                (VexDependencyBuildScope) -> Value,
        ):
            VexDependencyProvider<Value> {
            require(
                readableName.isNotBlank(),
            ) {
                "Named dependency provider requires a non-blank readable name"
            }

            return object :
                VexDependencyProvider<Value>(
                    dependencies =
                        dependencies,
                    declaredReadableName =
                        readableName,
                ) {
                override fun create(
                    dependencies:
                        VexDependencyBuildScope,
                ):
                    Value =
                    build(
                        dependencies,
                    )
            }
        }
    }
}

/**
 * Narrow provider-construction view.
 *
 * A provider may retrieve only dependencies it explicitly declared. This keeps graph traversal and
 * memoization in the factory while making each provider's prerequisite relationship readable.
 */
class VexDependencyBuildScope internal constructor(
    values:
        IdentityHashMap<
            VexDependencyProvider<*>,
            Any
        >,
) {
    private val declared =
        IdentityHashMap<
            VexDependencyProvider<*>,
            Any
        >().apply {
            putAll(
                values,
            )
        }

    fun <Value : Any> require(
        provider:
            VexDependencyProvider<Value>,
    ):
        Value {
        val value =
            declared[
                provider
            ]
                ?: error(
                    "Provider " +
                        provider.readableName +
                        " was not declared as a prerequisite of this construction",
                )

        @Suppress(
            "UNCHECKED_CAST",
        )
        return value as
            Value
    }
}

/**
 * Source-readable provider grouping.
 *
 * Modules group related declarations; they do not own object lifetime, graph traversal, or runtime
 * identity. One provider may appear in exactly one visible module for a factory lineage.
 */
class VexDependencyModule private constructor(
    val providers:
        List<
            VexDependencyProvider<*>
        >,
) {
    companion object {
        fun of(
            vararg providers:
                VexDependencyProvider<*>,
        ):
            VexDependencyModule {
            require(
                providers.isNotEmpty(),
            ) {
                "VexDependencyModule requires at least one provider"
            }

            providers
                .forEachIndexed { index, provider ->
                    check(
                        providers
                            .take(
                                index,
                            )
                            .none {
                                it ===
                                    provider
                            },
                    ) {
                        "Provider " +
                            provider.readableName +
                            " is registered more than once in one module"
                    }
                }

            return VexDependencyModule(
                providers =
                    providers.toList(),
            )
        }
    }
}

/**
 * Construction-graph and memoization owner for one explicit dependency lifetime.
 *
 * WHAT THIS IS
 * VexDependencyFactory resolves requested provider roots, recursively builds declared prerequisites
 * first, and memoizes one built Value per provider declaration for this exact factory lifetime.
 *
 * WHAT IT RELATES TO
 * [VexDependencyModule] groups visible declarations; [VexDependencyProvider] declares prerequisites;
 * [VexDependencyBuildScope] exposes only already-declared prerequisite Values during construction.
 * Child factories may reuse parent-lifetime providers while owning only their newly declared nodes.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * It makes construction order, reuse, and lifetime boundaries readable without ambient get<T>(),
 * constructor scanning, reflection-driven DI, or a global service locator. Lazy resolution builds
 * only the prerequisite closure of roots the composition owner actually requests.
 *
 * RESPONSIBILITY FENCE
 * The factory constructs/reuses objects; it does not infer semantic Family meaning, own
 * RelationshipId/Job custody, become Logger/history, decide provider business policy, or provide
 * hidden dependency lookup to ordinary feature/UI code. Dependency provider identity must never be
 * reused as execution, semantic, UI, or trace identity merely because all are "IDs".
 */
class VexDependencyFactory(
    modules:
        List<
            VexDependencyModule
        >,
    private val parent:
        VexDependencyFactory? =
        null,
) {
    private val lock =
        Any()

    private val providers =
        modules
            .flatMap {
                it.providers
            }
            .also { declarations ->
                declarations
                    .forEachIndexed { index, provider ->
                        check(
                            declarations
                                .take(
                                    index,
                                )
                                .none {
                                    it ===
                                        provider
                                },
                        ) {
                            "Provider " +
                                provider.readableName +
                                " is registered more than once in this factory"
                        }

                        check(
                            parent
                                ?.isRegistered(
                                    provider,
                                ) !=
                                true,
                        ) {
                            "Child factory cannot shadow parent provider " +
                                provider.readableName
                        }
                    }
            }

    /*
     * Freeze the readable graph at lifetime construction. Provider declarations may have been formed
     * from caller-owned list objects; later mutation of those lists must not silently rewrite an
     * already-established dependency lifetime.
     */
    private val prerequisitesByProvider =
        IdentityHashMap<
            VexDependencyProvider<*>,
            List<
                VexDependencyProvider<*>
            >
        >().apply {
            providers.forEach { provider ->
                put(
                    provider,
                    provider.dependencies
                        .toList(),
                )
            }
        }

    private val instances =
        IdentityHashMap<
            VexDependencyProvider<*>,
            Any
        >()

    private val resolving =
        mutableListOf<
            VexDependencyProvider<*>
        >()

    fun child(
        vararg modules:
            VexDependencyModule,
    ):
        VexDependencyFactory =
        VexDependencyFactory(
            modules =
                modules.toList(),
            parent =
                this,
        )

    fun <Value : Any> resolve(
        provider:
            VexDependencyProvider<Value>,
    ):
        Value =
        synchronized(
            lock,
        ) {
            @Suppress(
                "UNCHECKED_CAST",
            )
            resolveAny(
                provider,
            ) as
                Value
        }

    fun relationships():
        List<
            VexDependencyRelationship
        > =
        synchronized(
            lock,
        ) {
            providers.map { provider ->
                VexDependencyRelationship(
                    provider =
                        provider.readableName,
                    prerequisites =
                        requireNotNull(
                            prerequisitesByProvider[
                                provider
                            ],
                        )
                            .map {
                                it.readableName
                            },
                    instantiated =
                        instances.containsKey(
                            provider,
                        ),
                )
            }
        }

    private fun resolveFromChild(
        provider:
            VexDependencyProvider<*>,
    ):
        Any =
        synchronized(
            lock,
        ) {
            resolveAny(
                provider,
            )
        }

    private fun isRegistered(
        provider:
            VexDependencyProvider<*>,
    ):
        Boolean =
        providers.any {
            it ===
                provider
        } ||
            parent
                ?.isRegistered(
                    provider,
                ) ==
                true

    private fun resolveAny(
        provider:
            VexDependencyProvider<*>,
    ):
        Any {
        instances[
            provider
        ]
            ?.let {
                return it
            }

        val local =
            providers.firstOrNull {
                it ===
                    provider
            }

        if (
            local ==
            null
        ) {
            return parent
                ?.resolveFromChild(
                    provider,
                )
                ?: error(
                    "Provider " +
                        provider.readableName +
                        " is not registered in this dependency lifetime",
                )
        }

        val cycleIndex =
            resolving.indexOfFirst {
                it ===
                    provider
            }

        check(
            cycleIndex <
                0,
        ) {
            (
                resolving
                    .drop(
                        cycleIndex
                            .coerceAtLeast(
                                0,
                            ),
                    ) +
                    provider
            )
                .joinToString(
                    prefix =
                        "Dependency cycle: ",
                    separator =
                        " -> ",
                ) {
                    it.readableName
                }
        }

        resolving +=
            provider

        try {
            val prerequisites =
                IdentityHashMap<
                    VexDependencyProvider<*>,
                    Any
                >()

            requireNotNull(
                prerequisitesByProvider[
                    provider
                ],
            )
                .forEach { dependency ->
                    prerequisites[
                        dependency
                    ] =
                        resolveAny(
                            dependency,
                        )
                }

            val built =
                provider.build(
                    VexDependencyBuildScope(
                        prerequisites,
                    ),
                )

            instances[
                provider
            ] =
                built

            return built
        } finally {
            check(
                resolving.removeLast() ===
                    provider,
            )
        }
    }
}

/**
 * Read-only human/debug projection of declared construction topology.
 *
 * Provider/prerequisite Strings are readable labels only. This projection must not be fed back into
 * construction as identity or treated as a second dependency registry.
 */
data class VexDependencyRelationship(
    val provider:
        String,
    val prerequisites:
        List<String>,
    val instantiated:
        Boolean,
)

// [VXG RealForever]
