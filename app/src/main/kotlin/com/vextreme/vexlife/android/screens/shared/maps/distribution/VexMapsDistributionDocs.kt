package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeDeliveryRouteConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionExecution
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSession

/**
 * Code-navigation root for the lived VexMaps Distribution feature.
 *
 * Feature home:
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/distribution/README.md
 *
 * Current design:
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/distribution/design/README.md
 *
 * Search tag for the naming refinement record: [Name Ontology]
 *
 * Read the feature by question:
 *
 * - [VexMapsDistributionModeDocs] owns whether Maps is operating as Regular or Distribution.
 * - [VexMapsDeliveryRouteDocs] owns the finite Delivery Route grammar, including admitted preparation.
 * - durable Draft/Stop repository source owns the obligations the human entered and their input order.
 * - [VexMapsDistributionOptimizer] derives one Plan from one exact preparation snapshot.
 * - [VexMapsDistributionModeDeliveryRouteConvergenceRelay] asks only whether Distribution Mode and Delivery Route are coherent.
 * - [VexMapsDeliveryRouteNavigationConvergenceRelay] relates the Delivery Route to one exact expected Navigation leg.
 * - [VexMapsDistributionSession] owns the process-lifetime lived relationship: current Draft, Plan,
 *   provider attempts, expected leg, failure/retry truth and bounded Control feedback.
 * - [VexMapsDistributionExecution] owns coroutine custody under the accepted VexExecution
 *   foundation: maintained convergence observation plus finite draft/preparation/leg operations.
 *   [VexMapsDistributionSession] still owns what those operations mean, currentness, provider
 *   result/failure meaning and retry law.
 * - [VexMapsDistributionUi] is the UI-facing boundary over the Session.
 *
 * Lower owners remain independently complete. Convergence never executes a foreign Effect; any
 * downward semantic change re-enters through the target family's Control. Navigation remains the
 * owner of one actual journey/leg.
 */
internal object VexMapsDistributionDocs {
    const val FEATURE_REF: String = "feature.vexmaps.distribution"

    const val CURRENT_FEATURE_NAME: String = "VexMaps Distribution"

    val historicalSearchNames: List<String> =
        listOf(
            "Distribution Run",
            "VexMapsDistributionRun",
        )
}

// [VXG RealForever]
