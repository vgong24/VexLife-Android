package com.vextreme.vexlife.android.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexModifierTest {
    @Test
    fun registeredElementUsesItsExactSemanticIdentity() {
        val element =
            ButtonElement(
                elementId = "element.test.action",
                labelRes = 1,
                intent = ElementIntent.LocalEffect,
            )

        assertEquals(
            element.elementId,
            VexModifier.element(element).testTag,
        )
    }

    @Test
    fun runtimeSemanticIdentityIsPreservedExactly() {
        val ref =
            "element.events.dynamic.event.example.001.attention.acknowledge"

        assertEquals(
            ref,
            VexModifier.semantic(ref).testTag,
        )
    }

    @Test
    fun blankSemanticIdentityFailsClosed() {
        val failure =
            runCatching {
                VexModifier.semantic("   ")
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("must not be blank"),
        )
    }
}

// [VXG RealForever]
