package com.vextreme.vexlife.android.ui

import android.content.Context
import android.content.SharedPreferences
import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Android-local durable realization of ThemeMode explicit selection.
 *
 * SharedPreferences is platform storage mechanics, not portable ThemeMode meaning. The Store exposes
 * the Family-owned Store contract required by production: save(State).
 * current()/forget() remain concrete ThemeMode storage mechanics for process-start restoration and
 * local reset/proof use; they are not promoted into generic Family Store architecture.
 */
internal class SharedPreferencesThemeModeStore(
    context: Context,
) :
    ThemeModeFamily.Store {
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

    fun forget() {
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
