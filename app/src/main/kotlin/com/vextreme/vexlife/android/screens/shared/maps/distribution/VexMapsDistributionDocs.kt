package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContextRelay
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeDocs
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunDocs

/**
 * Code-navigation root for the first VexMaps Distribution architecture slice.
 *
 * The feature begins with two independent Behavior Experiences:
 *
 * [VexMapsDistributionModeDocs] -- ordinary Maps versus Distribution operating context.
 * [VexMapsDistributionRunDocs] -- finite run lifecycle over an externally-owned dynamic stop draft.
 *
 * They remain independent "tissues". [VexMapsDistributionControlContextRelay] fuses their complete
 * projections into one higher-order relational "muscle".
 *
 * Feedback is allowed to become circular only through Controls:
 *
 * Run truth changes
 * -> Control Context convergence
 * -> bounded Mode Control request
 * -> Mode Experience re-resolves CURRENT Mode State
 * -> Mode Behavior selects Mode Effect
 * -> new Mode Projection
 * -> Control Context converges again
 *
 * No convergence output directly executes another family's Effect.
 */
internal object VexMapsDistributionDocs

// [VXG RealForever]
