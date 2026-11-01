package com.vextreme.vexlife.android.dependency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexDependencyFactoryTest {
    private data class Leaf(
        val value: String,
    )

    private data class Root(
        val leaf: Leaf,
    )

    private class LeafProvider(
        private val onBuild: () -> Unit = {},
    ) :
        VexDependencyProvider<Leaf>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            Leaf {
            onBuild()
            return Leaf(
                value = "leaf",
            )
        }
    }

    private class RootProvider(
        private val leaf:
            VexDependencyProvider<Leaf>,
        private val onBuild: () -> Unit = {},
    ) :
        VexDependencyProvider<Root>(
            dependencies =
                listOf(
                    leaf,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            Root {
            onBuild()
            return Root(
                leaf =
                    dependencies.require(
                        leaf,
                    ),
            )
        }
    }

    @Test
    fun requestedRootBuildsPrerequisitesFirstAndMemoizesEachProviderOnce() {
        val order =
            mutableListOf<String>()
        val leaf =
            LeafProvider {
                order +=
                    "leaf"
            }
        val root =
            RootProvider(
                leaf,
            ) {
                order +=
                    "root"
            }
        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            root,
                            leaf,
                        ),
                    ),
            )

        val first =
            factory.resolve(
                root,
            )
        val second =
            factory.resolve(
                root,
            )

        assertEquals(
            listOf(
                "leaf",
                "root",
            ),
            order,
        )
        assertSame(
            first,
            second,
        )
        assertSame(
            first.leaf,
            factory.resolve(
                leaf,
            ),
        )
    }

    @Test
    fun unrequestedProvidersStayUnbuilt() {
        var leafBuilt =
            false
        val leaf =
            LeafProvider {
                leafBuilt =
                    true
            }
        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            leaf,
                        ),
                    ),
            )

        assertFalse(
            leafBuilt,
        )
        assertFalse(
            factory.relationships()
                .single()
                .instantiated,
        )
    }

    @Test
    fun childLifetimeReusesParentProviderWithoutRebuildingIt() {
        var builds =
            0
        val leaf =
            LeafProvider {
                builds +=
                    1
            }
        val root =
            RootProvider(
                leaf,
            )
        val application =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            leaf,
                        ),
                    ),
            )
        val session =
            application.child(
                VexDependencyModule.of(
                    root,
                ),
            )

        val first =
            session.resolve(
                root,
            )
        val second =
            session.resolve(
                root,
            )

        assertEquals(
            1,
            builds,
        )
        assertSame(
            first,
            second,
        )
        assertSame(
            application.resolve(
                leaf,
            ),
            first.leaf,
        )
    }

    @Test
    fun providerCannotReachAnUndeclaredDependency() {
        val declared =
            LeafProvider()
        val hidden =
            LeafProvider()

        class InvalidProvider :
            VexDependencyProvider<Root>(
                dependencies =
                    listOf(
                        declared,
                    ),
            ) {
            override fun create(
                dependencies:
                    VexDependencyBuildScope,
            ):
                Root =
                Root(
                    leaf =
                        dependencies.require(
                            hidden,
                        ),
                )
        }

        val invalid =
            InvalidProvider()
        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            declared,
                            hidden,
                            invalid,
                        ),
                    ),
            )

        val failure =
            runCatching {
                factory.resolve(
                    invalid,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is
                IllegalStateException,
        )
        assertTrue(
            failure
                ?.message
                .orEmpty()
                .contains(
                    "was not declared as a prerequisite",
                ),
        )
    }

    @Test
    fun factorySnapshotsDeclaredPrerequisitesAtLifetimeFormation() {
        val leaf =
            LeafProvider()
        val mutablePrerequisites =
            mutableListOf<
                VexDependencyProvider<*>
            >(
                leaf,
            )

        class SnapshotProvider :
            VexDependencyProvider<Root>(
                dependencies =
                    mutablePrerequisites,
            ) {
            override fun create(
                dependencies:
                    VexDependencyBuildScope,
            ):
                Root =
                Root(
                    leaf =
                        dependencies.require(
                            leaf,
                        ),
                )
        }

        val root =
            SnapshotProvider()
        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            leaf,
                            root,
                        ),
                    ),
            )

        mutablePrerequisites.clear()

        val resolved =
            factory.resolve(
                root,
            )

        assertSame(
            factory.resolve(
                leaf,
            ),
            resolved.leaf,
        )
        assertEquals(
            listOf(
                leaf.readableName,
            ),
            factory.relationships()
                .first {
                    it.provider ==
                        root.readableName
                }
                .prerequisites,
        )
    }

    @Test
    fun namedProviderKeepsReadableIdentityAndExplicitPrerequisitesWithoutSubclassBoilerplate() {
        val leaf =
            VexDependencyProvider.named(
                readableName =
                    "test.leaf",
            ) {
                Leaf(
                    value =
                        "leaf",
                )
            }
        val root =
            VexDependencyProvider.named(
                readableName =
                    "test.root",
                dependencies =
                    listOf(
                        leaf,
                    ),
            ) { dependencies ->
                Root(
                    leaf =
                        dependencies.require(
                            leaf,
                        ),
                )
            }
        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            leaf,
                            root,
                        ),
                    ),
            )

        val resolved =
            factory.resolve(
                root,
            )

        assertEquals(
            "leaf",
            resolved.leaf.value,
        )
        assertEquals(
            listOf(
                "test.leaf",
            ),
            factory.relationships()
                .single {
                    it.provider ==
                        "test.root"
                }
                .prerequisites,
        )
    }

    @Test
    fun cycleFailsClosedBeforeConstruction() {
        val cycle =
            mutableListOf<
                VexDependencyProvider<*>
            >()

        class SelfProvider :
            VexDependencyProvider<Leaf>(
                dependencies =
                    cycle,
            ) {
            override fun create(
                dependencies:
                    VexDependencyBuildScope,
            ):
                Leaf =
                Leaf(
                    value = "never",
                )
        }

        val provider =
            SelfProvider()
        cycle +=
            provider

        val factory =
            VexDependencyFactory(
                modules =
                    listOf(
                        VexDependencyModule.of(
                            provider,
                        ),
                    ),
            )

        val failure =
            runCatching {
                factory.resolve(
                    provider,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is
                IllegalStateException,
        )
        assertTrue(
            failure
                ?.message
                .orEmpty()
                .contains(
                    "Dependency cycle:",
                ),
        )
    }
}

// [VXG RealForever]
