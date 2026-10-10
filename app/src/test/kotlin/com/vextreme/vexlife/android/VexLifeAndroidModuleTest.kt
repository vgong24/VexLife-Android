package com.vextreme.vexlife.android

import com.vextreme.vex.android.architecture.execution.VexAppScope
import com.vextreme.vex.android.architecture.observability.VexLogger
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get

class VexLifeAndroidModuleTest : KoinTest {
    @Before
    fun startComposition() {
        startKoin {
            modules(vexLifeAndroidModule)
        }
    }

    @After
    fun stopComposition() {
        runCatching {
            get<VexLogger>().close()
        }

        stopKoin()
    }

    @Test
    fun sourceOfTruthRelayIsOneApplicationCompositionInstance() {
        val first = get<FoundationScreenRelay>()
        first.screenEntered()

        val second = get<FoundationScreenRelay>()

        assertSame(first, second)
        assertSame(
            FoundationScreenVexState.ScreenPresence.ENTERED,
            second.current.screenPresence,
        )
    }

    @Test
    fun vexAppScopeIsOneApplicationExecutionOwner() {
        assertSame(
            get<VexAppScope>(),
            get<VexAppScope>(),
        )
    }

    @Test
    fun vexLoggerIsOneApplicationObservabilityOwner() {
        assertSame(
            get<VexLogger>(),
            get<VexLogger>(),
        )
    }
}

// [VXG RealForever]
