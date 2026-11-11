package com.vextreme.vexlife.android

import android.app.Application
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.AppRepositoryFactory
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.observeVexStreamPlaybackPlayerStore
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.observeVexStreamQueuePlayerStore
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily
import com.vextreme.vexlife.android.vexstream.repeat.observeVexStreamRepeatPlayerStore
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.ui.SharedPreferencesThemeModeStore
import com.vextreme.vexlife.android.ui.ThemeModeStore

/**
 * Android process-lifetime composition root.
 *
 * WHAT THIS IS
 * VexApplication anchors dependencies whose rightful lifetime is the Android process and creates the
 * parent [VexDependencyFactory] used by narrower composition roots.
 *
 * WHAT IT RELATES TO
 * [VexApplicationDependencyModule] declares application-lifetime providers such as repositories,
 * Blueprint registry, and ThemeMode Store. A ViewModel/session composition root creates a child
 * factory and receives/reuses those concrete parent-lifetime objects.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * Android already supplies one process host. Reusing that host as the outer composition boundary
 * gives application-lifetime objects a visible owner without hiding access behind globals or asking
 * feature/UI code to construct process dependencies itself.
 *
 * RESPONSIBILITY FENCE
 * VexApplication is not a service locator, semantic Family owner, Logger/history owner, or feature
 * registry. Hosting an earned process-lifetime dependency does not transfer that dependency's
 * meaning here. Ordinary feature/UI consumers must not reach into it; narrower composition roots
 * such as VexStream's Service/ViewModel/platform callback resolve the exact process dependencies
 * their Android lifetime boundary requires.
 */
class VexApplication :
    Application() {
    internal lateinit var dependencyModule:
        VexApplicationDependencyModule
        private set

    internal lateinit var dependencies:
        VexDependencyFactory
        private set

    override fun onCreate() {
        super.onCreate()

        dependencyModule =
            VexApplicationDependencyModule(
                application =
                    this,
            )
        dependencies =
            VexDependencyFactory(
                modules =
                    listOf(
                        dependencyModule.module,
                    ),
            )
    }
}

/**
 * Readable application-lifetime provider declarations.
 *
 * WHAT THIS IS / RELATES TO
 * This module declares which process-lifetime objects exist and which prerequisites construct them.
 * [VexDependencyFactory] owns graph traversal, memoization, and lifetime reuse; the module itself
 * owns no runtime behavior.
 *
 * WHY THIS SHAPE EXISTS
 * The Android Application is an already-existing host object, so its provider publishes that exact
 * instance into the graph rather than pretending the dependency factory constructed Android itself.
 *
 * RESPONSIBILITY FENCE
 * Do not move session/ViewModel-lifetime objects into this module for convenience and do not treat
 * this provider grouping as permission for application-global access from ordinary feature code.
 */
internal class VexApplicationDependencyModule(
    application:
        Application,
) {
    val androidApplication =
        AndroidApplicationProvider(
            application,
        )

    val blueprintRegistry =
        BlueprintRegistryProvider()

    val repositories =
        RepositoriesProvider(
            androidApplication,
        )

    val themeModeStore =
        ThemeModeStoreProvider(
            androidApplication,
        )

    val processExecutionRoot =
        VexDependencyProvider.named(
            readableName =
                "VexStream process execution root",
        ) {
            VexExecutionRoot()
        }

    val playerStore =
        VexDependencyProvider.named(
            readableName =
                "VexStream Player Store",
        ) {
            VexStreamPlayerStore()
        }

    val playbackFamily =
        VexDependencyProvider.named(
            readableName =
                "VexStream Playback Family",
            dependencies =
                listOf(
                    playerStore,
                    processExecutionRoot,
                ),
        ) { dependencies ->
            VexStreamPlaybackFamily()
                .also { family ->
                    observeVexStreamPlaybackPlayerStore(
                        family =
                            family,
                        store =
                            dependencies.require(
                                playerStore,
                            ),
                        executionRoot =
                            dependencies.require(
                                processExecutionRoot,
                            ),
                    )
                }
        }

    val queueFamily =
        VexDependencyProvider.named(
            readableName =
                "VexStream Queue Family",
            dependencies =
                listOf(
                    playerStore,
                    processExecutionRoot,
                ),
        ) { dependencies ->
            VexStreamQueueFamily()
                .also { family ->
                    observeVexStreamQueuePlayerStore(
                        family =
                            family,
                        store =
                            dependencies.require(
                                playerStore,
                            ),
                        executionRoot =
                            dependencies.require(
                                processExecutionRoot,
                            ),
                    )
                }
        }

    val repeatFamily =
        VexDependencyProvider.named(
            readableName =
                "VexStream Repeat Family",
            dependencies =
                listOf(
                    playerStore,
                    processExecutionRoot,
                ),
        ) { dependencies ->
            VexStreamRepeatFamily()
                .also { family ->
                    observeVexStreamRepeatPlayerStore(
                        family =
                            family,
                        store =
                            dependencies.require(
                                playerStore,
                            ),
                        executionRoot =
                            dependencies.require(
                                processExecutionRoot,
                            ),
                    )
                }
        }

    val module:
        VexDependencyModule =
        VexDependencyModule.of(
            androidApplication,
            blueprintRegistry,
            repositories,
            themeModeStore,
            processExecutionRoot,
            playerStore,
            playbackFamily,
            queueFamily,
            repeatFamily,
        )

    internal class AndroidApplicationProvider(
        private val application:
            Application,
    ) :
        VexDependencyProvider<Application>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            Application =
            application
    }

    internal class BlueprintRegistryProvider :
        VexDependencyProvider<UIBlueprintRegistry>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            UIBlueprintRegistry =
            AppScreens.registry
    }

    internal class RepositoriesProvider(
        private val application:
            VexDependencyProvider<Application>,
    ) :
        VexDependencyProvider<AppRepositories>(
            dependencies =
                listOf(
                    application,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            AppRepositories =
            AppRepositoryFactory.production(
                dependencies.require(
                    application,
                ),
            )
    }

    internal class ThemeModeStoreProvider(
        private val application:
            VexDependencyProvider<Application>,
    ) :
        VexDependencyProvider<ThemeModeStore>(
            dependencies =
                listOf(
                    application,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            ThemeModeStore =
            SharedPreferencesThemeModeStore(
                dependencies.require(
                    application,
                )
                    .applicationContext,
            )
    }
}

// [VXG RealForever]
