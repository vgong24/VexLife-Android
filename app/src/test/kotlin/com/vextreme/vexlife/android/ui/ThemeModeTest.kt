package com.vextreme.vexlife.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun explicitThemeModesMapToCurrentBooleanStorageWithoutChangingMeaning() {
        assertEquals(
            false,
            ThemeModeBooleanAdapter.toDarkTheme(ThemeMode.Light),
        )
        assertEquals(
            true,
            ThemeModeBooleanAdapter.toDarkTheme(ThemeMode.Dark),
        )
    }

    @Test
    fun currentBooleanStorageMapsBackToTheSameExplicitThemeModes() {
        assertEquals(
            ThemeMode.Light,
            ThemeModeBooleanAdapter.fromDarkTheme(false),
        )
        assertEquals(
            ThemeMode.Dark,
            ThemeModeBooleanAdapter.fromDarkTheme(true),
        )
    }

    @Test
    fun everyAdmittedThemeModeRoundTripsThroughCurrentBooleanStorage() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(
                mode,
                ThemeModeBooleanAdapter.fromDarkTheme(
                    ThemeModeBooleanAdapter.toDarkTheme(mode),
                ),
            )
        }
    }
}
