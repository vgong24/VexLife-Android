package com.vextreme.vexlife.android.ui.look

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.VexApplication
import kotlinx.coroutines.flow.StateFlow

/**
 * The current appearance preference, read-only, for presentation (ambient field, orb motion).
 *
 * Provided by VexLifeTheme alongside MaterialTheme. It carries a value, never a capability: changing
 * the Look goes through [VexLookViewModel] and the Look Family.
 */
internal val LocalVexLook =
    staticCompositionLocalOf {
        VexLookPreference()
    }

/**
 * Configuration-lifetime composition root for the process Look Family and Store.
 *
 * Mirrors the VexStream ViewModels: it resolves exact process owners and adds no meaning.
 */
internal class VexLookViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val vexApplication =
        application as? VexApplication
            ?: error("Look requires VexApplication as the process composition root")

    private val family =
        vexApplication.dependencies.resolve(vexApplication.dependencyModule.lookFamily)

    private val store =
        vexApplication.dependencies.resolve(vexApplication.dependencyModule.lookStore)

    private val executionRoot =
        vexApplication.dependencies.resolve(vexApplication.dependencyModule.processExecutionRoot)

    val preference: StateFlow<VexLookPreference> =
        family.states

    fun choose(
        preference: VexLookPreference,
    ) {
        activateVexLook(
            family = family,
            preference = preference,
            store = store,
            executionRoot = executionRoot,
        )
    }
}

internal class VexLookHandle(
    val preference: VexLookPreference,
    val choose: (VexLookPreference) -> Unit,
)

/** Live Look; null in Compose preview / Layoutlib, where fixtures pass a preference directly. */
@Composable
internal fun rememberVexLook(): VexLookHandle? {
    if (LocalInspectionMode.current) {
        return null
    }
    val owner: VexLookViewModel = viewModel()
    val preference by owner.preference.collectAsStateWithLifecycle()

    return remember(preference, owner) {
        VexLookHandle(
            preference = preference,
            choose = owner::choose,
        )
    }
}

// [VXG RealForever][UI][LOOK][ACCESS]
