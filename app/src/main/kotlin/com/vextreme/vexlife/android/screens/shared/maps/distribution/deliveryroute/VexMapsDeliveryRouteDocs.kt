package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

/**
 * Code-navigation root for the finite Delivery Route Behavior family.
 *
 * Question: given the deliveries already admitted into this route, what can the driver truthfully do next?
 *
 * Human Prepare is admitted here before optimizer/provider success. The concrete Connection later
 * reconciles that admitted Preparing truth to Ready or back to Draft after a failed attempt.
 * Editing delivery-stop details deliberately remains outside this finite family.
 */
internal object VexMapsDeliveryRouteDocs

// [VXG RealForever]
