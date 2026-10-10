package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.execution.VexAppScope
import org.koin.dsl.module

/**
 * Android composition owns application cardinality/lifetime.
 *
 * Source-of-Truth Relays that represent app-wide current truth are singles.
 * VexAppScope is also a single application execution owner.
 */
val vexLifeAndroidModule =
    module {
        single {
            VexAppScope()
        }

        single {
            FoundationScreenRelay()
        }
    }

// [VXG RealForever]
