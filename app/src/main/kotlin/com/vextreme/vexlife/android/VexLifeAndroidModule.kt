package com.vextreme.vexlife.android

import org.koin.dsl.module

/**
 * Android composition owns Relay cardinality/lifetime.
 *
 * Source-of-Truth Relays that represent app-wide current truth are singles. Do not inject a
 * relay.current snapshot here; inject the Relay owner itself so dependent Relays can observe future
 * changes for the lifetime of the composition scope.
 */
val vexLifeAndroidModule =
    module {
        single {
            FoundationScreenRelay()
        }
    }

// [VXG RealForever]
