package com.vextreme.vexlife.android.elements

import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexModifierTest {
    @Test
    fun vexModifierIsARealComposeModifierRoot() {
        assertTrue(
            VexModifier is Modifier,
        )
    }

    @Test
    fun nestedPathAppendsToParentContinuity() {
        assertEquals(
            "element.gallery.page.content.elements.sample.title",
            appendVexElementPath(
                parentRef =
                    "element.gallery.page.content.elements.sample",
                childPath =
                    "title",
            ),
        )
    }

    @Test
    fun blueprintAndCustomComposeSharePathNormalization() {
        assertEquals(
            "element.gallery.body.preview.title",
            appendVexElementPath(
                parentRef =
                    "element.gallery.body",
                childPath =
                    "preview/title",
            ),
        )

        assertEquals(
            "preview.title",
            normalizeVexElementPath(
                " preview/title ",
            ),
        )
    }

    @Test
    fun blankNestedPathFailsClosed() {
        val failure =
            runCatching {
                appendVexElementPath(
                    parentRef =
                        "element.gallery.body",
                    childPath =
                        "   ",
                )
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("must not be blank"),
        )
    }
}

// [VXG RealForever]
