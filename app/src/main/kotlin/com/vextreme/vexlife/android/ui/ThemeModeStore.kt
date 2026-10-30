package com.vextreme.vexlife.android.ui

import android.content.Context
import android.content.SharedPreferences

/**
 * Android-local durable realization of ThemeMode explicit selection.
 *
 * SharedPreferences is platform storage mechanics, not portable ThemeMode meaning.
 *
 * ThemeModeStore is feature-runtime infrastructure consumed by ThemeModeConnection. It is not part of
 * the semantic Family contract. current() seeds fresh-process truth; save() realizes the
 * PersistSelection Effect; forget() is concrete reset/proof mechanics.
 */
internal interface ThemeModeStore {
    override fun current():
        ThemeMode

    suspend fun save(
        state:
            ThemeMode,
    )

    fun forget()
}

internal class SharedPreferencesThemeModeStore(
    context: Context,
) :
    ThemeModeStore {
    private val preferences:
        SharedPreferences =
        context
            .applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )



    fun current():
        ThemeMode =
        readCurrent()

    override suspend fun save(
        state:
            ThemeMode,
    ) {
        when (state) {
            ThemeMode.NotSet ->
                throw IllegalArgumentException(
                    "ThemeMode.NotSet is absence of an explicit durable selection; use forget()",
                )

            ThemeMode.Light,
            ThemeMode.Dark ->
                write(
                    preferences
                        .edit()
                        .putString(
                            KEY_THEME_MODE,
                            storedValueFor(
                                state,
                            ),
                        ),
                )
        }
    }

    override fun forget() {
        write(
            preferences
                .edit()
                .remove(
                    KEY_THEME_MODE,
                ),
        )
    }

    private fun readCurrent():
        ThemeMode =
        when (
            preferences.getString(
                KEY_THEME_MODE,
                null,
            )
        ) {
            STORED_LIGHT ->
                ThemeMode.Light

            STORED_DARK ->
                ThemeMode.Dark

            else ->
                ThemeMode.NotSet
        }

    private fun storedValueFor(
        state:
            ThemeMode,
    ):
        String =
        when (state) {
            ThemeMode.Light ->
                STORED_LIGHT

            ThemeMode.Dark ->
                STORED_DARK

            ThemeMode.NotSet ->
                error(
                    "ThemeMode.NotSet is never persisted as an explicit durable selection",
                )
        }

    private fun write(
        editor:
            SharedPreferences.Editor,
    ) {
        check(
            editor.commit(),
        ) {
            "ThemeMode durable write failed"
        }
    }

    private companion object {
        const val PREFERENCES_NAME =
            "vexlife-theme-mode"
        const val KEY_THEME_MODE =
            "explicit-selection"
        const val STORED_LIGHT =
            "LIGHT"
        const val STORED_DARK =
            "DARK"
    }
}

// [VXG RealForever]
