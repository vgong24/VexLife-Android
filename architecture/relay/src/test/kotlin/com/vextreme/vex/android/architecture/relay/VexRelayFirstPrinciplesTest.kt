package com.vextreme.vex.android.architecture.relay

import kotlin.test.Test
import kotlin.test.assertEquals

class VexRelayFirstPrinciplesTest {
    private enum class ScreenEvent {
        ENTER,
        EXIT,
    }

    private enum class ScreenPresence {
        ENTERED,
        EXITED,
    }

    @Test
    fun screenEnterExitIsTheSmallestRelay() {
        val relay = VexRelay(
            initialInput = ScreenEvent.EXIT,
        ) { event ->
            when (event) {
                ScreenEvent.ENTER -> ScreenPresence.ENTERED
                ScreenEvent.EXIT -> ScreenPresence.EXITED
            }
        }

        assertEquals(ScreenPresence.EXITED, relay.current.output)
        assertEquals(0L, relay.current.revision)

        val entered = relay.accept(ScreenEvent.ENTER)
        assertEquals(ScreenPresence.ENTERED, entered.output)
        assertEquals(1L, entered.revision)

        val exited = relay.accept(ScreenEvent.EXIT)
        assertEquals(ScreenPresence.EXITED, exited.output)
        assertEquals(2L, exited.revision)
    }

    @Test
    fun identicalRelationshipDoesNotInventAnotherRevision() {
        val relay = VexRelay(
            initialInput = ScreenEvent.EXIT,
        ) { event ->
            when (event) {
                ScreenEvent.ENTER -> ScreenPresence.ENTERED
                ScreenEvent.EXIT -> ScreenPresence.EXITED
            }
        }

        val unchanged = relay.accept(ScreenEvent.EXIT)

        assertEquals(0L, unchanged.revision)
        assertEquals(ScreenPresence.EXITED, unchanged.output)
    }

    private data class TooltipInputState(
        val screenPresence: ScreenPresence,
        val targetAvailable: Boolean,
    )

    private data class TooltipOutputState(
        val visible: Boolean,
    )

    @Test
    fun richerComponentStateDoesNotRequireARicherRelayApi() {
        val relay = VexRelay(
            initialInput = TooltipInputState(
                screenPresence = ScreenPresence.EXITED,
                targetAvailable = false,
            ),
        ) { input ->
            TooltipOutputState(
                visible =
                    input.screenPresence == ScreenPresence.ENTERED &&
                    input.targetAvailable,
            )
        }

        val targetAppeared = relay.evolveInput {
            it.copy(targetAvailable = true)
        }

        // The component relationship changed even though the visible output did not.
        assertEquals(1L, targetAppeared.revision)
        assertEquals(false, targetAppeared.output.visible)

        val screenEntered = relay.evolveInput {
            it.copy(screenPresence = ScreenPresence.ENTERED)
        }

        assertEquals(2L, screenEntered.revision)
        assertEquals(true, screenEntered.output.visible)
    }
}

// [VXG RealForever]
