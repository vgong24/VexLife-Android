package com.vextreme.vexlife.android.dependency

import java.util.IdentityHashMap

/**
 * Common construction-graph node.
 *
 * This is the real heterogeneous common type for dependency topology. It is not a value-erasing
 * service locator: concrete [VexDependencyProvider] declarations still bind exact Scope + Value
 * types, while this node lets the factory traverse differently-valued prerequisites.
 */
abstract class VexDependencyNode(
    val dependencies:
        List<
            VexDependencyNode
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
                    "VexDependencyNode must have a readable named declaration"
                }

    internal abstract fun build(
        dependencies:
            VexDependencyBuildScope,
    ):
        Any
}

/** One dependency declaration whose lifetime Scope remains part of its type. */
abstract class VexScopedDependency<
    Scope : VexDependencyScope,
>(
    dependencies:
        List<
            VexDependencyNode
        > =
        emptyList(),
    declaredReadableName:
        String? =
        null,
) :
    VexDependencyNode(
        dependencies =
            dependencies,
        declaredReadableName =
            declaredReadableName,
    )

/**
 * One exact Scope + Value construction declaration.
 *
 * Provider object identity remains the construction coordinate. Scope is type-level lifetime
 * topology; readableName remains human/debug text only.
 */
abstract class VexDependencyProvider<
    Scope : VexDependencyScope,
    Value : Any,
>(
    dependencies:
        List<
            VexDependencyNode
        > =
        emptyList(),
    declaredReadableName:
        String? =
        null,
) :
    VexScopedDependency<
        Scope
    >(
        dependencies =
            dependencies,
        declaredReadableName =
            declaredReadableName,
    ) {
    protected abstract fun create(
        dependencies:
            VexDependencyBuildScope,
    ):
        Value

    internal final override fun build(
        dependencies:
            VexDependencyBuildScope,
    ):
        Value =
        create(
            dependencies,
        )

    companion object {
        fun <
            Scope : VexDependencyScope,
            Value : Any,
        > named(
            readableName:
                String,
            dependencies:
                List<
                    VexDependencyNode
                > =
                emptyList(),
            build:
                (VexDependencyBuildScope) -> Value,
        ):
            VexDependencyProvider<
                Scope,
                Value
            > {
            require(
                readableName.isNotBlank(),
            ) {
                "Named dependency provider requires a non-blank readable name"
            }

            return object :
                VexDependencyProvider<
                    Scope,
                    Value
                >(
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
 * A provider may retrieve only dependencies it explicitly declared. Exact Scope + Value type remains
 * on the provider reference supplied to require(...); internal storage is construction machinery.
 */
class VexDependencyBuildScope internal constructor(
    values:
        IdentityHashMap<
            VexDependencyNode,
            Any
        >,
) {
    private val declared =
        IdentityHashMap<
            VexDependencyNode,
            Any
        >().apply {
            putAll(
                values,
            )
        }

    fun <
        Scope : VexDependencyScope,
        Value : Any,
    > require(
        provider:
            VexDependencyProvider<
                Scope,
                Value
            >,
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
 * Source-readable grouping for declarations that all belong to one exact Scope.
 *
 * A provider cannot be moved from Application to Session or one Service sibling merely by placing it
 * in another module: the module's Scope parameter and provider's Scope parameter must agree.
 */
class VexDependencyModule<
    Scope : VexDependencyScope,
> private constructor(
    val providers:
        List<
            VexScopedDependency<
                Scope
            >
        >,
) {
    companion object {
        fun <
            Scope : VexDependencyScope,
        > of(
            vararg providers:
                VexScopedDependency<
                    Scope
                >,
        ):
            VexDependencyModule<
                Scope
            > {
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

private class VexDependencyParentAccess(
    val isRegistered:
        (VexDependencyNode) -> Boolean,
    val resolve:
        (VexDependencyNode) -> Any,
)

/**
 * Construction graph + memoization owner for one typed dependency Scope.
 *
 * Child formation is compiler-bound to [VexChildDependencyScope] of this exact Scope. Parent objects
 * are reused; child providers cannot shadow them.
 */
class VexDependencyFactory<
    Scope : VexDependencyScope,
> private constructor(
    modules:
        List<
            VexDependencyModule<
                Scope
            >
        >,
    private val parent:
        VexDependencyParentAccess?,
) {
    constructor(
        modules:
            List<
                VexDependencyModule<
                    Scope
                >
            >,
    ) :
        this(
            modules =
                modules,
            parent =
                null,
        )

    private val lock =
        Any()

    private val providers:
        List<
            VexDependencyNode
        > =
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
                                ?.isRegistered
                                ?.invoke(
                                    provider,
                                ) !=
                                true,
                        ) {
                            "Child factory cannot shadow parent provider " +
                                provider.readableName
                        }
                    }
            }

    private val prerequisitesByProvider =
        IdentityHashMap<
            VexDependencyNode,
            List<
                VexDependencyNode
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
            VexDependencyNode,
            Any
        >()

    private val resolving =
        mutableListOf<
            VexDependencyNode
        >()

    fun <
        ChildScope,
    > child(
        vararg modules:
            VexDependencyModule<
                ChildScope
            >,
    ):
        VexDependencyFactory<
            ChildScope
        >
        where
        ChildScope :
            VexChildDependencyScope<
                Scope
            > =
        VexDependencyFactory(
            modules =
                modules.toList(),
            parent =
                VexDependencyParentAccess(
                    isRegistered =
                        ::isRegistered,
                    resolve =
                        ::resolveNode,
                ),
        )

    fun <
        ProviderScope : VexDependencyScope,
        Value : Any,
    > resolve(
        provider:
            VexDependencyProvider<
                ProviderScope,
                Value
            >,
    ):
        Value =
        synchronized(
            lock,
        ) {
            @Suppress(
                "UNCHECKED_CAST",
            )
            resolveNode(
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

    private fun isRegistered(
        provider:
            VexDependencyNode,
    ):
        Boolean =
        providers.any {
            it ===
                provider
        } ||
            parent
                ?.isRegistered
                ?.invoke(
                    provider,
                ) ==
                true

    private fun resolveNode(
        provider:
            VexDependencyNode,
    ):
        Any =
        synchronized(
            lock,
        ) {
            instances[
                provider
            ]
                ?.let {
                    return@synchronized it
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
                return@synchronized parent
                    ?.resolve
                    ?.invoke(
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
                        VexDependencyNode,
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
                            resolveNode(
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

                built
            } finally {
                check(
                    resolving.removeLast() ===
                        provider,
                )
            }
        }
}

data class VexDependencyRelationship(
    val provider:
        String,
    val prerequisites:
        List<String>,
    val instantiated:
        Boolean,
)

// [VXG RealForever][VEXARCHITECTURE][DEPENDENCY][TYPED-SCOPES]
