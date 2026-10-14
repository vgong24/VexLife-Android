package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue

/**
 * Current Queue facts reported by a runtime provider.
 *
 * Status is translated into the feature's Queue Case; it is not retained as a second truth owner.
 */
data class VexMediaQueueStatus(
    /** Number of media items currently owned by the provider queue. */
    val itemCount: Int,
    /** Current provider index, or -1 when no item is selected. */
    val currentIndex: Int,
    /** Provider-owned answer to whether Previous is meaningful now. */
    val hasPrevious: Boolean,
    /** Provider-owned answer to whether Next is meaningful now. */
    val hasNext: Boolean,
)

// [VXG RealForever]
