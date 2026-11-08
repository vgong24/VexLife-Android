@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.VexApplicationDependencyModule
import com.vextreme.vexlife.android.conversation.VexConversationHybridRelay
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.dependency.VexApplicationScope
import com.vextreme.vexlife.android.dependency.VexSessionScope
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.events.EventsConvergenceRelay
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.widget.vexstream.updateVexStreamPlaybackWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * One ViewModel/configuration-lifetime VexLife session composed from explicit live objects.
 *
 * WHAT THIS IS
 * VexLifeSession is the assembled session surface consumed by the UI after construction. It groups
 * already-built repositories, Logger, Family, Relay projection, actions, and VexRuntime that
 * belong to the same session lifetime.
 *
 * WHAT IT RELATES TO
 * Production construction is declared by [VexLifeSessionDependencyModule] and memoized by the child
 * VexDependencyFactory owned from the ViewModel. Application-lifetime prerequisites come from the
 * VexApplication parent factory. The direct constructor below exists only for deterministic
 * previews/tests/alternate repository environments.
 *
 * WHAT IT ADDRESSES / WHY THIS SHAPE EXISTS
 * Android can rebuild Activity/Compose surfaces while the logical app session should retain current
 * navigation, presentation, Logger session and execution roots. A ViewModel-owned
 * session makes that lifetime explicit without turning Composables into construction owners.
 *
 * RESPONSIBILITY FENCE
 * This class is not a service locator or a second dependency graph. Do not add hidden resolve/get
 * behavior, duplicate provider construction, or move Family/Execution/Logger meaning into this
 * aggregate. New objects belong here only when they are genuinely session-owned and are constructed
 * by the rightful composition module.
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
    val relayGraph:
        VexLifeRelayGraph,
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
            relayGraph =
                components.relayGraph,
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
    val relayGraph:
        VexLifeRelayGraph,
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
    val resolvedThemeModeStore =
        themeModeStore
            ?: InMemoryThemeModeStore(
                initialThemeMode,
            )
    val themeModeFamily =
        ThemeModeFamily(
            initialState =
                initialThemeMode,
        )
    observeThemeModeStore(
        family =
            themeModeFamily,
        store =
            resolvedThemeModeStore,
        executionRoot =
            executionRoot,
        logger =
            logger,
    )
    val relayGraph =
        directRelayGraph(
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
            themeModeStore =
                resolvedThemeModeStore,
            executionRoot =
                executionRoot,
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
        relayGraph =
            relayGraph,
        appActions =
            appActions,
        runtime =
            runtime,
    )
}

private fun directRelayGraph(
    repositories:
        AppRepositories,
    scope:
        CoroutineScope,
):
    VexLifeRelayGraph {
    val events =
        EventsConvergenceRelay(
            eventsRepository =
                repositories.events,
            attentionPreferences =
                repositories.eventsAttention,
            attentionLab =
                repositories.eventsAttentionLab,
            userProfile =
                repositories.userProfile,
        )
            .also { relay ->
                relay.activate(
                    scope,
                )
            }

    val conversation =
        VexConversationHybridRelay(
            conversationRepository =
                repositories.conversation,
            conversationLab =
                null,
        )
            .also { relay ->
                relay.activate(
                    scope,
                )
            }

    return VexLifeRelayGraph(
        events =
            events,
        conversation =
            conversation,
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
    themeModeStore:
        ThemeModeStore,
    executionRoot:
        VexExecutionRoot,
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
                    store =
                        themeModeStore,
                    executionRoot =
                        executionRoot,
                    logger =
                        logger,
                )
            },
        ),
    )

private fun toggleThemeMode(
    family:
        ThemeModeFamily,
    store:
        ThemeModeStore,
    executionRoot:
        VexExecutionRoot,
    logger:
        VexLogger,
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

    activateThemeMode(
        family =
            family,
        control =
            ThemeModeFamily.Control.Select(
                ThemeModeFamily.Parameter.Mode(
                    mode =
                        selectedMode,
                ),
            ),
        store =
            store,
        executionRoot =
            executionRoot,
        logger =
            logger,
    )
}


/**
 * ViewModel/session-lifetime dependency declarations.
 *
 * WHAT THIS IS / RELATES TO
 * This module declares the child-lifetime graph layered on application-lifetime providers:
 * VexExecutionRoot, Logger persistence, VexLogger, ThemeMode Family, Relay owners/projection,
 * app actions, VexRuntime, and the assembled VexLifeSession.
 *
 * WHY THIS SHAPE EXISTS
 * Keeping these prerequisites as explicit provider edges makes startup/lifetime composition readable
 * and lets the child VexDependencyFactory memoize each session object exactly once without hiding
 * construction inside the session constructor.
 *
 * RESPONSIBILITY FENCE
 * The module declares construction only. It must not become semantic Family policy, execution
 * identity, Logger chronology, Relay processing, or an ambient dependency API. Do not move
 * application-lifetime providers here merely because a session consumes them, and do not create a
 * parallel construction path for production objects already declared in this graph.
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
            initialThemeMode =
                initialThemeMode,
            store =
                applicationDependencies.themeModeStore,
            executionRoot =
                executionRoot,
            logger =
                logger,
        )

    val eventsRelay =
        EventsRelayProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
        )

    val conversationRelay =
        ConversationRelayProvider(
            repositories =
                applicationDependencies.repositories,
            scope =
                sessionScope,
        )

    val relayGraph =
        RelayGraphProvider(
            events =
                eventsRelay,
            conversation =
                conversationRelay,
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
            themeModeStore =
                applicationDependencies.themeModeStore,
            executionRoot =
                executionRoot,
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
            relayGraph =
                relayGraph,
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
        VexDependencyModule<
            VexSessionScope
        > =
        VexDependencyModule.of(
            sessionScope,
            executionRoot,
            logPersistence,
            logger,
            themeModeFamily,
            eventsRelay,
            conversationRelay,
            relayGraph,
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
        VexDependencyProvider<VexSessionScope, CoroutineScope>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            CoroutineScope =
            scope
    }

    internal class ExecutionRootProvider :
        VexDependencyProvider<VexSessionScope, VexExecutionRoot>() {
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
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val scope:
            VexDependencyProvider<VexSessionScope, CoroutineScope>,
    ) :
        VexDependencyProvider<VexSessionScope, LogHistoryPersistenceQueue>(
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
            VexDependencyProvider<VexSessionScope, LogHistoryPersistenceQueue>,
        private val blueprintRegistry:
            VexDependencyProvider<VexApplicationScope, UIBlueprintRegistry>,
    ) :
        VexDependencyProvider<VexSessionScope, VexLogger>(
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
        private val store:
            VexDependencyProvider<VexApplicationScope, ThemeModeStore>,
        private val executionRoot:
            VexDependencyProvider<VexSessionScope, VexExecutionRoot>,
        private val logger:
            VexDependencyProvider<VexSessionScope, VexLogger>,
    ) :
        VexDependencyProvider<VexSessionScope, ThemeModeFamily>(
            dependencies =
                listOf(
                    store,
                    executionRoot,
                    logger,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            ThemeModeFamily {
            val family =
                ThemeModeFamily(
                    initialState =
                        initialThemeMode,
                )

            observeThemeModeStore(
                family =
                    family,
                store =
                    dependencies.require(
                        store,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
                logger =
                    dependencies.require(
                        logger,
                    ),
            )

            return family
        }
    }

    internal class EventsRelayProvider(
        private val repositories:
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val scope:
            VexDependencyProvider<VexSessionScope, CoroutineScope>,
    ) :
        VexDependencyProvider<VexSessionScope, EventsConvergenceRelay>(
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
            EventsConvergenceRelay =
            EventsConvergenceRelay(
                eventsRepository =
                    dependencies.require(
                        repositories,
                    )
                        .events,
                attentionPreferences =
                    dependencies.require(
                        repositories,
                    )
                        .eventsAttention,
                attentionLab =
                    dependencies.require(
                        repositories,
                    )
                        .eventsAttentionLab,
                userProfile =
                    dependencies.require(
                        repositories,
                    )
                        .userProfile,
            )
                .also { relay ->
                    relay.activate(
                        dependencies.require(
                            scope,
                        ),
                    )
                }
    }

    internal class ConversationRelayProvider(
        private val repositories:
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val scope:
            VexDependencyProvider<VexSessionScope, CoroutineScope>,
    ) :
        VexDependencyProvider<VexSessionScope, VexConversationHybridRelay>(
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
            VexConversationHybridRelay =
            VexConversationHybridRelay(
                conversationRepository =
                    dependencies.require(
                        repositories,
                    )
                        .conversation,
                conversationLab =
                    null,
            )
                .also { relay ->
                    relay.activate(
                        dependencies.require(
                            scope,
                        ),
                    )
                }
    }

    internal class RelayGraphProvider(
        private val events:
            VexDependencyProvider<VexSessionScope, EventsConvergenceRelay>,
        private val conversation:
            VexDependencyProvider<VexSessionScope, VexConversationHybridRelay>,
    ) :
        VexDependencyProvider<VexSessionScope, VexLifeRelayGraph>(
            dependencies =
                listOf(
                    events,
                    conversation,
                ),
        ) {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            VexLifeRelayGraph =
            VexLifeRelayGraph(
                events =
                    dependencies.require(
                        events,
                    ),
                conversation =
                    dependencies.require(
                        conversation,
                    ),
            )
    }

    internal class AppActionsProvider(
        private val repositories:
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val scope:
            VexDependencyProvider<VexSessionScope, CoroutineScope>,
        private val logger:
            VexDependencyProvider<VexSessionScope, VexLogger>,
        private val logPersistence:
            VexDependencyProvider<VexSessionScope, LogHistoryPersistenceQueue>,
        private val themeModeFamily:
            VexDependencyProvider<VexSessionScope, ThemeModeFamily>,
        private val themeModeStore:
            VexDependencyProvider<VexApplicationScope, ThemeModeStore>,
        private val executionRoot:
            VexDependencyProvider<VexSessionScope, VexExecutionRoot>,
    ) :
        VexDependencyProvider<VexSessionScope, AppActionRegistrations>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                    logger,
                    logPersistence,
                    themeModeFamily,
                    themeModeStore,
                    executionRoot,
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
                themeModeStore =
                    dependencies.require(
                        themeModeStore,
                    ),
                executionRoot =
                    dependencies.require(
                        executionRoot,
                    ),
            )
    }

    internal class RuntimeProvider(
        private val blueprintRegistry:
            VexDependencyProvider<VexApplicationScope, UIBlueprintRegistry>,
        private val logger:
            VexDependencyProvider<VexSessionScope, VexLogger>,
        private val appActions:
            VexDependencyProvider<VexSessionScope, AppActionRegistrations>,
    ) :
        VexDependencyProvider<VexSessionScope, VexRuntime>(
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
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val scope:
            VexDependencyProvider<VexSessionScope, CoroutineScope>,
        private val logPersistence:
            VexDependencyProvider<VexSessionScope, LogHistoryPersistenceQueue>,
        private val logger:
            VexDependencyProvider<VexSessionScope, VexLogger>,
        private val themeModeFamily:
            VexDependencyProvider<VexSessionScope, ThemeModeFamily>,
        private val relayGraph:
            VexDependencyProvider<VexSessionScope, VexLifeRelayGraph>,
        private val appActions:
            VexDependencyProvider<VexSessionScope, AppActionRegistrations>,
        private val runtime:
            VexDependencyProvider<VexSessionScope, VexRuntime>,
    ) :
        VexDependencyProvider<VexSessionScope, VexLifeSession>(
            dependencies =
                listOf(
                    repositories,
                    scope,
                    logPersistence,
                    logger,
                    themeModeFamily,
                    relayGraph,
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
                relayGraph =
                    dependencies.require(
                        relayGraph,
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
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexSessionScope, VexExecutionRoot>,
    ) :
        VexDependencyProvider<VexSessionScope, VexStreamStartupExecution>(
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
            VexDependencyProvider<VexApplicationScope, AppRepositories>,
        private val executionRoot:
            VexDependencyProvider<VexSessionScope, VexExecutionRoot>,
        private val application:
            VexDependencyProvider<VexApplicationScope, Application>,
    ) :
        VexDependencyProvider<VexSessionScope, VexStreamArtworkExecution>(
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
 * Android configuration-lifetime host for [VexLifeSession] and its child dependency lifetime.
 *
 * WHAT IT RELATES TO / WHY IT EXISTS
 * Activity rotation/recreation receives the same ViewModel instance, so active Screen/presentation,
 * Logger session and the session VexExecutionRoot survive UI reconstruction while
 * still ending when the ViewModel lifetime ends.
 *
 * RESPONSIBILITY FENCE
 * The ViewModel hosts lifetime and resolves declared session roots; it is not a semantic Family,
 * service locator for Composables, Logger owner of truth, or substitute for VexExecution. Feature/UI
 * code should consume [session] rather than resolving arbitrary providers from this host.
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
        VexDependencyFactory<
            VexSessionScope
        > =
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
