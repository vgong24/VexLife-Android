package com.vextreme.vexlife.android

import android.app.Application
import com.vextreme.vexlife.android.background.BackgroundServiceRelay
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.AppRepositoryFactory
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsNavigationProcess
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSession
import com.vextreme.vexlife.android.ui.SharedPreferencesThemeModeStore
import com.vextreme.vexlife.android.ui.ThemeModeStore
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.observeVexStreamPlaybackPlayerStore
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.queue.observeVexStreamQueuePlayerStore
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily
import com.vextreme.vexlife.android.vexstream.repeat.observeVexStreamRepeatPlayerStore
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetRelay
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetUpdateExecution

/**
 * Android process-lifetime composition root.
 *
 * WHAT THIS IS
 * VexApplication anchors dependencies whose rightful lifetime is the Android process and creates the
 * parent [VexDependencyFactory] used by narrower composition roots.
 *
 * WHAT IT RELATES TO
 * [VexApplicationDependencyModule] declares process-lifetime providers such as repositories,
 * process-shared semantic owners, and app-scope live convergence. A ViewModel/session or Android
 * Service composition root receives/reuses those exact parent-lifetime objects rather than creating
 * semantic copies.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * Android already supplies one process host. Reusing that host as the outer composition boundary
 * gives application-lifetime objects a visible owner without hiding access behind globals or asking
 * feature/UI code to construct process dependencies itself.
 *
 * RESPONSIBILITY FENCE
 * VexApplication is not a service locator, Family/runtime owner, VexExecutionRoot, Logger/history
 * owner, or feature semantic registry. Ordinary feature/UI consumers must not reach into it to
 * resolve dependencies; narrower composition roots receive concrete dependencies explicitly.
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

        /*
         * Background coordination is process-current truth, not something a particular Android
         * Service creates by happening to start first.
         */
        dependencies.resolve(
            dependencyModule.backgroundServiceRelay,
        )
        dependencies.resolve(
            dependencyModule.playbackWidgetUpdateExecution,
        )
    }
}

/**
 * Readable application-lifetime provider declarations.
 *
 * WHAT THIS IS / RELATES TO
 * This module declares which process-lifetime objects exist and which prerequisites construct them:
 * durable/shared repositories, semantic owners whose meaning survives UI, and app-scope live
 * convergence over those owners. [VexDependencyFactory] owns graph traversal, memoization, and
 * lifetime reuse; the module itself owns no semantic behavior.
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
        ProcessExecutionRootProvider()

    val playerStore =
        PlayerStoreProvider()

    val playbackFamily =
        PlaybackFamilyProvider(
            executionRoot =
                processExecutionRoot,
            playerStore =
                playerStore,
        )

    val queueFamily =
        QueueFamilyProvider(
            executionRoot =
                processExecutionRoot,
            playerStore =
                playerStore,
        )

    val repeatFamily =
        RepeatFamilyProvider(
            executionRoot =
                processExecutionRoot,
            playerStore =
                playerStore,
        )

    val navigationSession =
        NavigationSessionProvider()

    val backgroundServiceRelay =
        BackgroundServiceRelayProvider(
            executionRoot =
                processExecutionRoot,
            playbackFamily =
                playbackFamily,
            navigationSession =
                navigationSession,
        )

    val playbackWidgetRelay =
        PlaybackWidgetRelayProvider(
            executionRoot =
                processExecutionRoot,
            playerStore =
                playerStore,
            playbackFamily =
                playbackFamily,
            queueFamily =
                queueFamily,
        )

    val playerReceiver =
        PlayerReceiverProvider(
            playerStore =
                playerStore,
        )

    val playbackWidgetUpdateExecution =
        PlaybackWidgetUpdateExecutionProvider(
            application =
                androidApplication,
            executionRoot =
                processExecutionRoot,
            playbackWidgetRelay =
                playbackWidgetRelay,
        )

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
            navigationSession,
            backgroundServiceRelay,
            playbackWidgetRelay,
            playerReceiver,
            playbackWidgetUpdateExecution,
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

    /**
     * Canonical execution lifetime for process-scoped observation/custody.
     *
     * This is not an ambient coroutine scope. Process-owned observers such as
     * [BackgroundServiceRelay] receive this root explicitly through dependency construction.
     */
    internal class ProcessExecutionRootProvider :
        VexDependencyProvider<
            VexExecutionRoot
        >() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexExecutionRoot =
            VexExecutionRoot()
    }

    /** Process-current outside truth for the independently-owned VexStream Player Partner. */
    internal class PlayerStoreProvider :
        VexDependencyProvider<
            VexStreamPlayerStore
        >() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlayerStore =
            VexStreamPlayerStore()
    }

    /** One semantic Playback Family, with Player Store observation bound at process lifetime. */
    internal class PlaybackFamilyProvider(
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playerStore:
            VexDependencyProvider<
                VexStreamPlayerStore
            >,
    ) :
        VexDependencyProvider<
            VexStreamPlaybackFamily
        >(
            dependencies =
                listOf(
                    executionRoot,
                    playerStore,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlaybackFamily {
            val family =
                VexStreamPlaybackFamily()

            observeVexStreamPlaybackPlayerStore(
                family =
                    family,
                store =
                    dependencies.require(
                        playerStore,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
            )

            return family
        }
    }

    /** Queue semantic owner maintained from Player Store outside truth. */
    internal class QueueFamilyProvider(
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playerStore:
            VexDependencyProvider<
                VexStreamPlayerStore
            >,
    ) :
        VexDependencyProvider<
            VexStreamQueueFamily
        >(
            dependencies =
                listOf(
                    executionRoot,
                    playerStore,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamQueueFamily {
            val family =
                VexStreamQueueFamily()

            observeVexStreamQueuePlayerStore(
                family =
                    family,
                store =
                    dependencies.require(
                        playerStore,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
            )

            return family
        }
    }

    /** Repeat semantic owner maintained from Player Store outside truth. */
    internal class RepeatFamilyProvider(
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playerStore:
            VexDependencyProvider<
                VexStreamPlayerStore
            >,
    ) :
        VexDependencyProvider<
            VexStreamRepeatFamily
        >(
            dependencies =
                listOf(
                    executionRoot,
                    playerStore,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamRepeatFamily {
            val family =
                VexStreamRepeatFamily()

            observeVexStreamRepeatPlayerStore(
                family =
                    family,
                store =
                    dependencies.require(
                        playerStore,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
            )

            return family
        }
    }

    /**
     * Publish the existing process-local Navigation owner into the explicit dependency topology.
     *
     * This does not claim Navigation has already migrated to direct VexFamily; it removes hidden
     * singleton lookup for consumers that need its current process truth.
     */
    internal class NavigationSessionProvider :
        VexDependencyProvider<
            VexNavigationSession
        >() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexNavigationSession =
            VexMapsNavigationProcess
                .session
    }

    /**
     * One app-scope live convergence snapshot for truths that sustained/background realizations may
     * need to coordinate around. The Relay carries typed current meaning from rightful owners, not
     * Service IDs or an active-Service registry.
     */
    internal class BackgroundServiceRelayProvider(
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playbackFamily:
            VexDependencyProvider<
                VexStreamPlaybackFamily
            >,
        private val navigationSession:
            VexDependencyProvider<
                VexNavigationSession
            >,
    ) :
        VexDependencyProvider<
            BackgroundServiceRelay
        >(
            dependencies =
                listOf(
                    executionRoot,
                    playbackFamily,
                    navigationSession,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            BackgroundServiceRelay {
            val playback =
                dependencies.require(
                    playbackFamily,
                )
            val navigation =
                dependencies.require(
                    navigationSession,
                )

            return BackgroundServiceRelay(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                playbackStates =
                    playback.states,
                navigationStates =
                    navigation.states,
            )
        }
    }

    /**
     * Process-lived Widget relationship convergence over bounded Player + Playback + Queue truth.
     *
     * The Relay owns only the Widget-facing relationship current. Player Store, Playback Family and
     * Queue Family remain the rightful owners of their source truths.
     */
    internal class PlaybackWidgetRelayProvider(
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playerStore:
            VexDependencyProvider<
                VexStreamPlayerStore
            >,
        private val playbackFamily:
            VexDependencyProvider<
                VexStreamPlaybackFamily
            >,
        private val queueFamily:
            VexDependencyProvider<
                VexStreamQueueFamily
            >,
    ) :
        VexDependencyProvider<
            VexStreamPlaybackWidgetRelay
        >(
            dependencies =
                listOf(
                    executionRoot,
                    playerStore,
                    playbackFamily,
                    queueFamily,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlaybackWidgetRelay =
            VexStreamPlaybackWidgetRelay(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                playerStore =
                    dependencies.require(
                        playerStore,
                    ),
                playbackFamily =
                    dependencies.require(
                        playbackFamily,
                    ),
                queueFamily =
                    dependencies.require(
                        queueFamily,
                    ),
            )
    }

    /** Concrete Player Partner Connection Receiver bound to the process Player Store. */
    internal class PlayerReceiverProvider(
        private val playerStore:
            VexDependencyProvider<
                VexStreamPlayerStore
            >,
    ) :
        VexDependencyProvider<
            VexStreamPlayerPartner.Connection.Receiver
        >(
            dependencies =
                listOf(
                    playerStore,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlayerPartner.Connection.Receiver =
            VexStreamPlayerPartner.Connection.Receiver(
                store =
                    dependencies.require(
                        playerStore,
                    ),
            )
    }

    /**
     * Process-lived publication of already-converged Widget Relay output.
     *
     * This execution does not belong to MediaSessionService lifetime. Service teardown changes the
     * upstream truths; the Widget Relay owns the bounded relationship current consumed here.
     */
    internal class PlaybackWidgetUpdateExecutionProvider(
        private val application:
            VexDependencyProvider<
                Application
            >,
        private val executionRoot:
            VexDependencyProvider<
                VexExecutionRoot
            >,
        private val playbackWidgetRelay:
            VexDependencyProvider<
                VexStreamPlaybackWidgetRelay
            >,
    ) :
        VexDependencyProvider<
            VexStreamPlaybackWidgetUpdateExecution
        >(
            dependencies =
                listOf(
                    application,
                    executionRoot,
                    playbackWidgetRelay,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlaybackWidgetUpdateExecution =
            VexStreamPlaybackWidgetUpdateExecution(
                root =
                    dependencies.require(
                        executionRoot,
                    ),
                context =
                    dependencies.require(
                        application,
                    )
                        .applicationContext,
                relayStates =
                    dependencies.require(
                        playbackWidgetRelay,
                    )
                        .outputStates,
            )
    }

}

// [VXG RealForever]
