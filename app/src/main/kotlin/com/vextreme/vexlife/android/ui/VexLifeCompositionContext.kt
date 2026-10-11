package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * Read-only dependency projection for the app composition/routing layer.
 *
 * This exists to keep composition functions from growing one parameter per repository/source while
 * preserving narrow dependencies below that boundary.
 *
 * It is deliberately NOT:
 * - a semantic state owner;
 * - a mutable service locator;
 * - permission for ordinary Screens/features to reach every repository;
 * - a replacement for a domain facade when several repositories form one real relationship.
 *
 * VexLifeScaffold may unpack this context because it is the composition router. Screen renderers
 * continue receiving only the repositories/facades they actually consume. A future Relay/service
 * composition may join this context once a real consumer earns that relationship.
 */
internal data class VexLifeCompositionContext(
    val runtime: VexRuntime,
    val repositories: AppRepositories,
    val darkTheme: Boolean,
)
