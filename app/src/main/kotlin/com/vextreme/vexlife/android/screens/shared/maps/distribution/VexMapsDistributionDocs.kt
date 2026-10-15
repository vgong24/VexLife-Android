package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionModeRunConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationConvergenceRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSession

/**
 * Code-navigation root for the lived VexMaps Distribution feature.
 *
 * Read the feature by question:
 *
 * - [VexMapsDistributionModeDocs] owns whether Maps is operating as Regular or Distribution.
 * - [VexMapsDistributionRunDocs] owns the finite human run grammar, including admitted preparation.
 * - durable Draft/Stop repository source owns the obligations the human entered and their input order.
 * - [VexMapsDistributionOptimizer] derives one Plan from one exact preparation snapshot.
 * - [VexMapsDistributionModeRunConvergenceRelay] asks only whether Mode and Run are coherent.
 * - [VexMapsDistributionNavigationConvergenceRelay] relates Run to one exact expected Navigation leg.
 * - [VexMapsDistributionSession] owns the process-lifetime lived relationship: current Draft, Plan,
 *   provider attempts, expected leg, failure/retry truth and bounded Control feedback.
 * - [VexMapsDistributionExperience] is the UI-facing membrane over that session.
 *
 * Lower owners remain independently complete. Convergence never executes a foreign Effect; any
 * downward semantic change re-enters through the target family's Control. Navigation remains the
 * owner of one actual journey/leg.
 */
// [VXG RealForever]
internal object VexMapsDistributionDocs
