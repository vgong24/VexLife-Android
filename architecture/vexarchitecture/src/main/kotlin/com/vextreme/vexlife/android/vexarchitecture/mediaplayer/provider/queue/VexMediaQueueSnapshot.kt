package com.vextreme.vexlife.android.vexarchitecture.mediaplayer.provider.queue

/** Platform-neutral observation returned by a Queue provider. */
data class VexMediaQueueSnapshot(
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
