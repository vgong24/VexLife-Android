package com.vextreme.vexlife.android.elements.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTrailTest {
    @Test
    fun pushAndBackOwnOppositeDirections() {
        val trail = NavigationTrail("root")

        trail.push("child")

        assertEquals("child", trail.current)
        assertEquals(1, trail.depth)
        assertEquals(NavigationDirection.Forward, trail.direction)
        assertTrue(trail.canGoBack)

        assertTrue(trail.goBack())

        assertEquals("root", trail.current)
        assertEquals(0, trail.depth)
        assertEquals(NavigationDirection.Backward, trail.direction)
        assertFalse(trail.canGoBack)
    }

    @Test
    fun rootBackFallsThrough() {
        val trail = NavigationTrail("root")

        assertFalse(trail.goBack())
        assertEquals("root", trail.current)
    }
}
