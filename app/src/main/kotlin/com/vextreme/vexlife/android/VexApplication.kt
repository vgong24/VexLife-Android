package com.vextreme.vexlife.android

import android.app.Application
import android.os.Looper
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
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.ui.SharedPreferencesThemeModeStore
import com.vextreme.vexlife.android.ui.ThemeModeStore
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetExecution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Android process-lifetime composition root.
 *
 * WHAT THIS IS
 * VexApplication anchors dependencies whose rightful lifetime is the Android process and creates the
 * parent [VexDependencyFactory] used by narrower composition roots.
 *
 * WHAT IT RELATES TO
 * [VexApplicationDependencyModule] declares process-lifetime providers such as repositories,
 * process-shared Families and background convergence. A ViewModel/session or Android Service
 * composition root receives/reuses those exact parent-lifetime objects rather than creating semantic
 * copies.
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
    }
}

/**
 * Readable application-lifetime provider declarations.
 *
 * WHAT THIS IS / RELATES TO
 * This module declares which process-lifetime objects exist and which prerequisites construct them:
 * durable/shared repositories, semantic Families whose meaning survives UI, and process Relays whose
 * relationship remains meaningful while background Services continue. [VexDependencyFactory] owns
 * graph traversal, memoization, and lifetime reuse; the module itself owns no semantic behavior.
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

    val processScope =
        ProcessScopeProvider()

    val playbackFamily =
        PlaybackFamilyProvider()

    val playbackWidgetExecution =
        PlaybackWidgetExecutionProvider(
            application =
                androidApplication,
            playbackFamily =
                playbackFamily,
        )

    val navigationSession =
        NavigationSessionProvider()

    val backgroundServiceRelay =
        BackgroundServiceRelayProvider(
            playbackFamily =
                playbackFamily,
            navigationSession =
                navigationSession,
            processScope =
                processScope,
        )


    val module:
        VexDependencyModule =
        VexDependencyModule.of(
            androidApplication,
            blueprintRegistry,
            repositories,
            themeModeStore,
            processScope,
            playbackFamily,
            playbackWidgetExecution,
            navigationSession,
            backgroundServiceRelay,
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
     * Process coroutine lifetime used only by process-lived dependency observation.
     *
     * Long lifetime does not imply continuous expensive work; Relay collectors suspend while their
     * sources are unchanged and die with the Android process.
     */
    internal class ProcessScopeProvider :
        VexDependencyProvider<
            CoroutineScope
        >() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            CoroutineScope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.Default,
            )
    }

    /** One semantic Playback Family shared by UI, Service, Widget projections, and process Relays. */
    internal class PlaybackFamilyProvider :
        VexDependencyProvider<
            VexStreamPlaybackFamily
        >() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlaybackFamily =
            VexStreamPlaybackFamily()
    }

    /**
     * Process-lived execution owner for bounded widget provider/filesystem work.
     *
     * Execution policy is selected here at the process composition boundary. Feature/widget code
     * receives the owner and does not manufacture Dispatchers, CoroutineScopes, or Media3 thread
     * switching ad hoc.
     */
    internal class PlaybackWidgetExecutionProvider(
        private val application:
            VexDependencyProvider<Application>,
        private val playbackFamily:
            VexDependencyProvider<
                VexStreamPlaybackFamily
            >,
    ) :
        VexDependencyProvider<
            VexStreamPlaybackWidgetExecution
        >(
            dependencies =
                listOf(
                    application,
                    playbackFamily,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamPlaybackWidgetExecution {
            val controllerLooper =
                Looper.getMainLooper()

            return VexStreamPlaybackWidgetExecution(
                root =
                    VexExecutionRoot(
                        dispatcher =
                            Dispatchers.IO,
                    ),
                context =
                    dependencies.require(
                        application,
                    )
                        .applicationContext,
                playbackFamily =
                    dependencies.require(
                        playbackFamily,
                    ),
                controllerApplicationLooper =
                    controllerLooper,
                controllerDispatcher =
                    Dispatchers.Main.immediate,
            )
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

    internal class BackgroundServiceRelayProvider(
        private val playbackFamily:
            VexDependencyProvider<
                VexStreamPlaybackFamily
            >,
        private val navigationSession:
            VexDependencyProvider<
                VexNavigationSession
            >,
        private val processScope:
            VexDependencyProvider<
                CoroutineScope
            >,
    ) :
        VexDependencyProvider<
            BackgroundServiceRelay
        >(
            dependencies =
                listOf(
                    playbackFamily,
                    navigationSession,
                    processScope,
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
                playbackStates =
                    playback
                        .states
                        .map { state ->
                            state !=
                                VexStreamPlaybackState
                                    .NoMedia
                        }
                        .distinctUntilChanged(),
                navigationStates =
                    navigation
                        .states
                        .map { state ->
                            when (
                                state.phase
                            ) {
                                VexNavigationSessionPhase.READY,
                                VexNavigationSessionPhase.ACTIVE,
                                VexNavigationSessionPhase.OFF_ROUTE,
                                VexNavigationSessionPhase.LOCATION_HELD,
                                ->
                                    true

                                VexNavigationSessionPhase.IDLE,
                                VexNavigationSessionPhase.ARRIVED,
                                ->
                                    false
                            }
                        }
                        .distinctUntilChanged(),
            )
                .also { relay ->
                    relay.activate(
                        dependencies.require(
                            processScope,
                        ),
                    )
                }
        }
    }

}

// [VXG RealForever]
