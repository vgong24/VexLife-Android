package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

/**
 * Code-navigation root for the finite Distribution Run Behavior family.
 *
 * Question: given the currently admitted delivery obligations, what can the driver do next?
 *
 * Human Prepare is admitted here before optimizer/provider success. The concrete Connection later
 * reconciles that admitted Preparing truth to Ready or back to Draft after a failed attempt.
 * Stop payload editing deliberately remains outside this finite family.
 */
internal object VexMapsDistributionRunDocs

// [VXG RealForever]
