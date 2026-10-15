package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContextRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.execution.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunDocs

/**
 * Code-navigation root for VexMaps Distribution.
 *
 * Independent Behavior Experiences remain locally complete:
 *
 * [VexMapsDistributionModeDocs] -- Regular Maps versus Distribution operating context.
 * [VexMapsDistributionRunDocs] -- finite run lifecycle over durable, payload-owned obligations.
 *
 * [VexMapsDistributionControlContextRelay] fuses their complete projections into a higher-order
 * control context. [VexMapsDistributionNavigationConvergenceRelay] separately relates Run truth to
 * the existing Navigation session. Downward semantic feedback always returns through a target
 * Control rather than bypassing directly to a foreign Effect.
 *
 * Durable draft input order remains source truth. [VexMapsDistributionOptimizer] derives a
 * revision-bound provider-priced Plan without rewriting the Draft. The Plan then feeds one existing
 * Navigation leg at a time; an optional return-to-start is represented as Run truth and completes
 * only after an observed Navigation arrival.
 */
internal object VexMapsDistributionDocs

// [VXG RealForever]
