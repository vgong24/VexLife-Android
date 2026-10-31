package com.vextreme.vexlife.android

import android.app.Application
import com.vextreme.vexlife.android.dependency.VexDependencyFactory
import com.vextreme.vexlife.android.dependency.VexDependencyModule
import com.vextreme.vexlife.android.dependency.VexDependencyProvider
import com.vextreme.vexlife.android.dependency.VexDependencyBuildScope
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.AppRepositoryFactory
import com.vextreme.vexlife.android.ui.SharedPreferencesThemeModeStore
import com.vextreme.vexlife.android.ui.ThemeModeStore

/**
 * Android process-lifetime composition root.
 *
 * The Application owns only application-lifetime construction. Feature/UI consumers never resolve
 * dependencies from this object directly; narrower composition roots receive concrete dependencies.
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
 * Provider object identity is the construction coordinate. Their class names are only readable
 * labels in the dependency topology.
 */
internal class VexApplicationDependencyModule(
    application:
        Application,
) {
    val repositories =
        RepositoriesProvider(
            application,
        )

    val themeModeStore =
        ThemeModeStoreProvider(
            application,
        )

    val module:
        VexDependencyModule =
        VexDependencyModule.of(
            repositories,
            themeModeStore,
        )

    internal class RepositoriesProvider(
        private val application:
            Application,
    ) :
        VexDependencyProvider<AppRepositories>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            AppRepositories =
            AppRepositoryFactory.production(
                application,
            )
    }

    internal class ThemeModeStoreProvider(
        private val application:
            Application,
    ) :
        VexDependencyProvider<ThemeModeStore>() {
        override fun create(
            dependencies:
                VexDependencyBuildScope,
        ):
            ThemeModeStore =
            SharedPreferencesThemeModeStore(
                application.applicationContext,
            )
    }
}

// [VXG RealForever]
