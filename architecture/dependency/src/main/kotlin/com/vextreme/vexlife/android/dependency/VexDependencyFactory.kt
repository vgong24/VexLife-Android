package com.vextreme.vexlife.android.dependency

import java.util.IdentityHashMap

/**
 * One readable construction declaration.
 *
 * Provider object identity is the construction coordinate. The class name is only a human-readable
 * label; two provider instances of the same class remain distinct declarations when a real graph
 * needs them.
 *
 * Dependencies are explicit provider relationships. The object being constructed does not need to
 * know VexDependencyFactory exists.
 */
abstract class VexDependencyProvider<
    Value : Any,
>(
    val dependencies:
        List<
            VexDependencyProvider<*>
        > =
        emptyList(),
) {
    val readableName:
        String
        get() =
            requireNotNull(
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
 * One dependency lifetime / memoization owner.
 *
 * Resolution is lazy: only requested roots and their declared prerequisites are built. Prerequisites
 * are resolved first, then the provider is built once and memoized for this exact factory lifetime.
 *
 * A child factory may consume parent providers but cannot shadow the same provider declaration.
 * This gives application/session/etc. composition explicit lifetime boundaries without a global
 * service locator.
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
                        provider.dependencies
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

            provider.dependencies
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
 * Read-only projection of the declared construction topology.
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
