package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.execution.VexExecutionName
import com.vextreme.vex.android.architecture.execution.VexExecutionScope
import org.koin.dsl.module

/**
 * Android composition owns Relay cardinality/lifetime.
 *
 * Source-of-Truth Relays that represent app-wide current truth are singles. Do not inject a
 * relay.current snapshot here; inject the Relay owner itself so dependent Relays can observe future
 * changes for the lifetime of the composition scope.
 *
 * The application execution scope is also a single. It owns named coroutine work for the process
 * lifetime; Relay code remains DI-framework-neutral.
 */
val vexLifeAndroidModule =
    module {
        single {
            VexExecutionScope.root(
                name = VexExecutionName("vexlife.application"),
            )
        }

        single {
            FoundationScreenRelay()
        }
    }

// [VXG RealForever]
