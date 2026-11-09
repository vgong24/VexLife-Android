package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

enum class VexStreamQueueControl :
    VexFamily.Control<
        VexStreamQueueFamily
    > {
    Previous,
    Next,
    Shuffle,
    Clear,
}


// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
