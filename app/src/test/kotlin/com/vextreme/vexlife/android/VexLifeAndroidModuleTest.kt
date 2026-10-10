package com.vextreme.vexlife.android

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
    fun startRelayComposition() {
        startKoin {
            modules(vexLifeAndroidModule)
        }
    }

    @After
    fun stopRelayComposition() {
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
}

// [VXG RealForever]
