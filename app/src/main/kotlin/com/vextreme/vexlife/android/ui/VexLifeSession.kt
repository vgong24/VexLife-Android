package com.vextreme.vexlife.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.VexApplicationDependencyModule
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.widget.vexstream.updateVexStreamPlaybackWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * One in-process VexLife app session composed from explicit live relationships.
 *
 * Production receives every session-owned object from [VexLifeSessionDependencyModule]. The
 * repository/test convenience constructor remains below for previews/tests that intentionally
 * construct an isolated session without impersonating Android Application/ViewModel lifetime.
 */
internal class VexLifeSession internal constructor(
    val repositories:
        AppRepositories,
    val scope:
        CoroutineScope,
    val logPersistence:
        LogHistoryPersistenceQueue,
    val logger:
        VexLogger,
    val themeModeFamily:
        ThemeModeFamily,
    private val themeModeConnection:
        ThemeModeConnection,
    val relays:
        VexLifeRelays,
    val appActions:
        AppActionRegistrations,
    val runtime:
        VexRuntime,
) {
    /**
     * Narrow direct-construction path for previews/tests/alternate repository environments.
     *
     * Production does not use this constructor; production dependency relationships are declared as
     * providers below. Keeping this path explicit avoids forcing fixture code through Android
     * Application lifetime merely to construct a deterministic in-memory session.
     */
    internal constructor(
        repositories:
            AppRepositories,
        scope:
            CoroutineScope,
        executionRoot:
            VexExecutionRoot,
        themeModeStore:
            ThemeModeStore? =
            null,
        initialThemeMode:
            ThemeMode =
            ThemeMode.Dark,
    ) :
        this(
            directSessionComponents(
                repositories =
                    repositories,
                scope =
                    scope,
                executionRoot =
                    executionRoot,
                themeModeStore =
                    themeModeStore,
                initialThemeMode =
                    initialThemeMode,
            ),
        )

    private constructor(
        components:
            VexLifeSessionComponents,
    ) :
        this(
            repositories =
                components.repositories,
            scope =
                components.scope,
            logPersistence =
                components.logPersistence,
            logger =
                components.logger,
            themeModeFamily =
                components.themeModeFamily,
            themeModeConnection =
                components.themeModeConnection,
            relays =
                components.relays,
            appActions =
                components.appActions,
            runtime =
                components.runtime,
        )
}

private data class VexLifeSessionComponents(
    val repositories:
        AppRepositories,
    val scope:
        CoroutineScope,
    val logPersistence:
        LogHistoryPersistenceQueue,
    val logger:
        VexLogger,
    val themeModeFamily:
        ThemeModeFamily,
    val themeModeConnection:
        ThemeModeConnection,
    val relays:
        VexLifeRelays,
    val appActions:
        AppActionRegistrations,
    val runtime:
        VexRuntime,
)

private fun directSessionComponents(
    repositories:
        AppRepositories,
    scope:
        CoroutineScope,
    executionRoot:
        VexExecutionRoot,
    themeModeStore:
        ThemeModeStore?,
    initialThemeMode:
        ThemeMode,
):
    VexLifeSessionComponents {
    val logPersistence =
        LogHistoryPersistenceQueue(
            repository =
                repositories.logHistory,
            scope =
                scope,
        )
    val logger =
        VexLogger(
            onRecord =
                logPersistence::record,
            blueprintRegistry =
                AppScreens.registry,
        )
    val themeModeFamily =
        ThemeModeFamily(
            initialState =
                initialThemeMode,
        )
    val themeModeConnection =
        ThemeModeConnection(
            family =
                themeModeFamily,
            executionRoot =
                executionRoot,
            store =
                themeModeStore,
            logger =
                logger.sink,
        )
    val relays =
        VexLifeRelays.create(
            repositories =
                repositories,
            scope =
                scope,
        )
    val appActions =
        createAppActions(
            repositories =
                repositories,
            scope =
                scope,
            logger =
                logger,
            logPersistence =
                logPersistence,
            themeModeFamily =
                themeModeFamily,
            themeModeConnection =
                themeModeConnection,
        )
    val runtime =
        VexRuntime(
            registry =
                AppScreens.registry,
            logger =
                logger,
            initialScreen =
                HomeScreen,
            primaryScreens =
                AppScreens.primary,
            currentAppActions = {
                appActions
            },
        )

    return VexLifeSessionComponents(
        repositories =
            repositories,
        scope =
            scope,
        logPersistence =
            logPersistence,
        logger =
            logger,
        themeModeFamily =
            themeModeFamily,
        themeModeConnection =
            themeModeConnection,
        relays =
            relays,
        appActions =
            appActions,
        runtime =
            runtime,
    )
}

private fun createAppActions(
    repositories:
        AppRepositories,
    scope:
        CoroutineScope,
    logger:
        VexLogger,
    logPersistence:
        LogHistoryPersistenceQueue,
    themeModeFamily:
        ThemeModeFamily,
    themeModeConnection:
        ThemeModeConnection,
):
    AppActionRegistrations =
    AppScreens.appActionRegistrations(
        ScreenActionContext(
            repositories =
                repositories,
            scope =
                scope,
            logger =
                logger,
            logPersistence =
                logPersistence,
            toggleDarkMode = {
                toggleThemeMode(
                    family =
                        themeModeFamily,
                    connection =
                        themeModeConnection,
                )
            },
        ),
    )

private fun toggleThemeMode(
    family:
        ThemeModeFamily,
    connection:
        ThemeModeConnection,
) {
    val selectedMode =
        when (
            family.currentState
        ) {
            ThemeMode.NotSet,
            ThemeMode.Light,
            ->
                ThemeMode.Dark

            ThemeMode.Dark ->
                ThemeMode.Light
        }

    connection.activate(
        ThemeModeFamily.Control.Select(
            selectedMode,
        ),
    )
}

/**
 * ViewModel/session-lifetime dependency declarations.
 *
 * This graph exposes the actual production composition rather than hiding Logger/Family/Connection
 * construction inside one session constructor.
 */
internal class VexLifeSessionDependencyModule(
    applicationDependencies:
        VexApplicationDependencyModule,
    scope:
        CoroutineScope,
    initialThemeMode:
        ThemeMode =
        ThemeMode.Dark,
) {
    val sessionScope =
        SessionScopeProvider(
            scope,
        )

    val executionRoot =
        ExecutionRootProvider()

    val logPersistence =
        LogPersistenceProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
        )

    val logger =
        LoggerProvider(
            logPersistence =
                logPersistence,
            blueprintRegistry =
                applicationDependencies.blueprintRegistry,
        )

    val themeModeFamily =
        ThemeModeFamilyProvider(
            initialThemeMode,
        )

    val themeModeConnection =
        ThemeModeConnectionProvider(
            family =
                themeModeFamily,
            executionRoot =
                executionRoot,
            store =
                applicationDependencies.themeModeStore,
            logger =
                logger,
        )

    val relays =
        RelaysProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
        )

    val appActions =
        AppActionsProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
            logger =
                logger,
            logPersistence =
                logPersistence,
            themeModeFamily =
                themeModeFamily,
            themeModeConnection =
                themeModeConnection,
        )

    val runtime =
        RuntimeProvider(
            blueprintRegistry =
                applicationDependencies.blueprintRegistry,
            logger =
                logger,
            appActions =
                appActions,
        )

    val session =
        SessionProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
            logPersistence =
                logPersistence,
            logger =
                logger,
            themeModeFamily =
                themeModeFamily,
            themeModeConnection =
                themeModeConnection,
            relays =
                relays,
            appActions =
                appActions,
            runtime =
                runtime,
        )

    val vexStreamStartup =
        VexStreamStartupProvider(
            repositories =
                applicationDependencies.repositories,
            executionRoot =
                executionRoot,
        )

    val vexStreamArtwork =
        VexStreamArtworkProvider(
            repositories =
                applicationDependencies.repositories,
            executionRoot =
                executionRoot,
            application =
                applicationDependencies.androidApplication,
        )

    val module:
        VexDependencyModule =
        VexDependencyModule.of(
            sessionScope,
            executionRoot,
            logPersistence,
            logger,
            themeModeFamily,
            themeModeConnection,
            relays,
            appActions,
            runtime,
            session,
            vexStreamStartup,
            vexStreamArtwork,
        )

    internal class SessionScopeProvider(
        private val scope:
            CoroutineScope,
    ) :
        VexDependencyProvider<CoroutineScope>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            CoroutineScope =
            scope
    }

    internal class ExecutionRootProvider :
        VexDependencyProvider<VexExecutionRoot>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexExecutionRoot =
            VexExecutionRoot(
                dispatcher =
                    Dispatchers.IO,
            )
    }

    internal class LogPersistenceProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val scope:
            VexDependencyProvider<CoroutineScope>,
    ) :
        VexDependencyProvider<LogHistoryPersistenceQueue>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            LogHistoryPersistenceQueue =
            LogHistoryPersistenceQueue(
                repository =
                    dependencies.require(
                        repositories,
                    )
                        .logHistory,
                scope =
                    dependencies.require(
                        scope,
                    ),
            )
    }

    internal class LoggerProvider(
        private val logPersistence:
            VexDependencyProvider<LogHistoryPersistenceQueue>,
        private val blueprintRegistry:
            VexDependencyProvider<UIBlueprintRegistry>,
    ) :
        VexDependencyProvider<VexLogger>(
            dependencies =
                listOf(
                    logPersistence,
                    blueprintRegistry,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexLogger =
            VexLogger(
                onRecord =
                    dependencies.require(
                        logPersistence,
                    )::record,
                blueprintRegistry =
                    dependencies.require(
                        blueprintRegistry,
                    ),
            )
    }

    internal class ThemeModeFamilyProvider(
        private val initialThemeMode:
            ThemeMode,
    ) :
        VexDependencyProvider<ThemeModeFamily>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            ThemeModeFamily =
            ThemeModeFamily(
                initialState =
                    initialThemeMode,
            )
    }

    internal class ThemeModeConnectionProvider(
        private val family:
            VexDependencyProvider<ThemeModeFamily>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
        private val store:
            VexDependencyProvider<ThemeModeStore>,
        private val logger:
            VexDependencyProvider<VexLogger>,
    ) :
        VexDependencyProvider<ThemeModeConnection>(
            dependencies =
                listOf(
                    family,
                    executionRoot,
                    store,
                    logger,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            ThemeModeConnection =
            ThemeModeConnection(
                family =
                    dependencies.require(
                        family,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                store =
                    dependencies.require(
                        store,
                    ),
                logger =
                    dependencies.require(
                        logger,
                    )
                        .sink,
            )
    }

    internal class RelaysProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val scope:
            VexDependencyProvider<CoroutineScope>,
    ) :
        VexDependencyProvider<VexLifeRelays>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexLifeRelays =
            VexLifeRelays.create(
                repositories =
                    dependencies.require(
                        repositories,
                    ),
                scope =
                    dependencies.require(
                        scope,
                    ),
            )
    }

    internal class AppActionsProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val scope:
            VexDependencyProvider<CoroutineScope>,
        private val logger:
            VexDependencyProvider<VexLogger>,
        private val logPersistence:
            VexDependencyProvider<LogHistoryPersistenceQueue>,
        private val themeModeFamily:
            VexDependencyProvider<ThemeModeFamily>,
        private val themeModeConnection:
            VexDependencyProvider<ThemeModeConnection>,
    ) :
        VexDependencyProvider<AppActionRegistrations>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                    logger,
                    logPersistence,
                    themeModeFamily,
                    themeModeConnection,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            AppActionRegistrations =
            createAppActions(
                repositories =
                    dependencies.require(
                        repositories,
                    ),
                scope =
                    dependencies.require(
                        scope,
                    ),
                logger =
                    dependencies.require(
                        logger,
                    ),
                logPersistence =
                    dependencies.require(
                        logPersistence,
                    ),
                themeModeFamily =
                    dependencies.require(
                        themeModeFamily,
                    ),
                themeModeConnection =
                    dependencies.require(
                        themeModeConnection,
                    ),
            )
    }

    internal class RuntimeProvider(
        private val blueprintRegistry:
            VexDependencyProvider<UIBlueprintRegistry>,
        private val logger:
            VexDependencyProvider<VexLogger>,
        private val appActions:
            VexDependencyProvider<AppActionRegistrations>,
    ) :
        VexDependencyProvider<VexRuntime>(
            dependencies =
                listOf(
                    blueprintRegistry,
                    logger,
                    appActions,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexRuntime {
            val actions =
                dependencies.require(
                    appActions,
                )

            return VexRuntime(
                registry =
                    dependencies.require(
                        blueprintRegistry,
                    ),
                logger =
                    dependencies.require(
                        logger,
                    ),
                initialScreen =
                    HomeScreen,
                primaryScreens =
                    AppScreens.primary,
                currentAppActions = {
                    actions
                },
            )
        }
    }

    internal class SessionProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val scope:
            VexDependencyProvider<CoroutineScope>,
        private val logPersistence:
            VexDependencyProvider<LogHistoryPersistenceQueue>,
        private val logger:
            VexDependencyProvider<VexLogger>,
        private val themeModeFamily:
            VexDependencyProvider<ThemeModeFamily>,
        private val themeModeConnection:
            VexDependencyProvider<ThemeModeConnection>,
        private val relays:
            VexDependencyProvider<VexLifeRelays>,
        private val appActions:
            VexDependencyProvider<AppActionRegistrations>,
        private val runtime:
            VexDependencyProvider<VexRuntime>,
    ) :
        VexDependencyProvider<VexLifeSession>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                    logPersistence,
                    logger,
                    themeModeFamily,
                    themeModeConnection,
                    relays,
                    appActions,
                    runtime,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexLifeSession =
            VexLifeSession(
                repositories =
                    dependencies.require(
                        repositories,
                    ),
                scope =
                    dependencies.require(
                        scope,
                    ),
                logPersistence =
                    dependencies.require(
                        logPersistence,
                    ),
                logger =
                    dependencies.require(
                        logger,
                    ),
                themeModeFamily =
                    dependencies.require(
                        themeModeFamily,
                    ),
                themeModeConnection =
                    dependencies.require(
                        themeModeConnection,
                    ),
                relays =
                    dependencies.require(
                        relays,
                    ),
                appActions =
                    dependencies.require(
                        appActions,
                    ),
                runtime =
                    dependencies.require(
                        runtime,
                    ),
            )
    }

    internal class VexStreamStartupProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
    ) :
        VexDependencyProvider<VexStreamStartupExecution>(
            dependencies =
                listOf(
                    repositories,
                    executionRoot,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamStartupExecution =
            VexStreamStartupExecution(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                repository =
                    dependencies.require(
                        repositories,
                    )
                        .vexStream,
            )
                .also {
                    it.start()
                }
    }

    internal class VexStreamArtworkProvider(
        private val repositories:
            VexDependencyProvider<AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexExecutionRoot>,
        private val application:
            VexDependencyProvider<Application>,
    ) :
        VexDependencyProvider<VexStreamArtworkExecution>(
            dependencies =
                listOf(
                    repositories,
                    executionRoot,
                    application,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexStreamArtworkExecution =
            VexStreamArtworkExecution(
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                repository =
                    dependencies.require(
                        repositories,
                    )
                        .vexStream,
                context =
                    dependencies.require(
                        application,
                    )
                        .applicationContext,
            )
                .also {
                    it.start()
                }
    }
}

/**
 * Android configuration-lifetime owner for [VexLifeSession].
 *
 * Activity rotation/recreation receives the same ViewModel instance, so the active Screen, open
 * modal, Gallery selection and logger session do not restart merely because Android rebuilt the UI.
 */
internal class VexLifeViewModel(
    application:
        Application,
) :
    AndroidViewModel(
        application,
    ) {
    private val vexApplication =
        application as?
            VexApplication
            ?: error(
                "VexLifeViewModel requires VexApplication as the Android process composition root",
            )

    private val dependencyModule =
        VexLifeSessionDependencyModule(
            applicationDependencies =
                vexApplication.dependencyModule,
            scope =
                viewModelScope,
        )

    private val dependencies:
        VexDependencyFactory =
        vexApplication.dependencies
            .child(
                dependencyModule.module,
            )

    private val executionRoot =
        dependencies.resolve(
            dependencyModule.executionRoot,
        )

    /*
     * These resolutions are the readable startup policy. Everything else in the graph remains lazy
     * until one of these roots requires it.
     */
    private val vexStreamStartupExecution =
        dependencies.resolve(
            dependencyModule.vexStreamStartup,
        )

    private val vexStreamArtworkExecution =
        dependencies.resolve(
            dependencyModule.vexStreamArtwork,
        )

    val session =
        dependencies.resolve(
            dependencyModule.session,
        )

    override fun onCleared() {
        executionRoot.forceStopAll(
            "VexLife ViewModel cleared",
        )
        super.onCleared()
    }
}

internal class VexStreamStartupExecution(
    executionRoot:
        VexExecutionRoot,
    private val repository:
        VexStreamMusicRepository,
) :
    VexExecution(
        executionRoot,
    ) {
    fun start() {
        launchOnce(
            repository::restorePersistedState,
        )
    }
}

/**
 * Session-owned background custody for ancillary downloaded artwork.
 *
 * The repository owns which current downloaded tracks need artwork and how those bytes are stored.
 * This Execution only gives that finite network/filesystem work an explicit non-UI lifetime whenever
 * current Hub/catalog truth becomes available.
 */
internal class VexStreamArtworkExecution(
    executionRoot:
        VexExecutionRoot,
    private val repository:
        VexStreamMusicRepository,
    private val context:
        android.content.Context,
) :
    VexExecution(
        executionRoot,
    ) {
    fun start() {
        launchForCollect(
            repository::state,
        ) { state ->
            if (
                state.hubState is
                    VexStreamHubState.Ready &&
                state.tracks.any {
                    it.localCopy is
                        VexStreamLocalCopyState.Downloaded
                }
            ) {
                repository
                    .backfillOfflineArtwork()
                updateVexStreamPlaybackWidgets(
                    context,
                )
            }
        }
    }
}

// [VXG RealForever]
