package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.execution.VexAppScope
import com.vextreme.vex.android.architecture.observability.VexLogger
import com.vextreme.vex.android.architecture.observability.VexSingleThreadLogExecutor
import org.koin.dsl.module

/**
 * Application composition for the current foundation.
 *
 * - VexLogger: one ordered application diagnostic owner.
 * - VexAppScope: one application coroutine owner using that logger.
 * - FoundationScreenRelay: one app-wide Source-of-Truth Relay using that logger.
 */
val vexLifeAndroidModule =
    module {
        single {
            VexLogger(
                executor =
                    VexSingleThreadLogExecutor(
                        threadName = "vex.logger",
                    ),
            )
        }

        single {
            VexAppScope(
                logSink = get<VexLogger>(),
            )
        }

        single {
            FoundationScreenRelay(
                logSink = get<VexLogger>(),
            )
        }
    }

// [VXG RealForever]
