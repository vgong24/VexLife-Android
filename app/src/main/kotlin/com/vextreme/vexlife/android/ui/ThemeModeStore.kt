package com.vextreme.vexlife.android.ui

import android.content.Context
import android.content.SharedPreferences
import com.vextreme.vexlife.android.behavior.VexFamily
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android-local durable realization of ThemeMode explicit selection.
 *
 * SharedPreferences is platform storage mechanics, not portable ThemeMode meaning. The Store exposes
 * the Family-owned StateStore contract required by production: save(State) + observableState.
 * Direct get()/forget() helpers remain concrete ThemeMode storage mechanics for local reset/proof use.
 * Collection lifetime belongs to VexExecution through ThemeModeFamily.observeStore(...); the Store
 * registers its listener only while that Flow is observed.
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

    override val observableState:
        Flow<ThemeMode> =
        callbackFlow {
            val listener =
                SharedPreferences
                    .OnSharedPreferenceChangeListener { _, key ->
                        if (
                            key ==
                            KEY_THEME_MODE
                        ) {
                            trySend(
                                readCurrent(),
                            )
                        }
                    }

            preferences
                .registerOnSharedPreferenceChangeListener(
                    listener,
                )

            trySend(
                readCurrent(),
            )

            awaitClose {
                preferences
                    .unregisterOnSharedPreferenceChangeListener(
                        listener,
                    )
            }
        }

    suspend fun get():
        ThemeMode =
        readCurrent()

    override suspend fun save(
        state:
            ThemeMode,
    ):
        VexFamily.Store.WriteResult =
        when (state) {
            ThemeMode.NotSet ->
                VexFamily.Store.WriteResult.Failure(
                    IllegalArgumentException(
                        "ThemeMode.NotSet is absence of an explicit durable selection; use forget()",
                    ),
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

    suspend fun forget():
        VexFamily.Store.WriteResult =
        write(
            preferences
                .edit()
                .remove(
                    KEY_THEME_MODE,
                ),
        )

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
    ):
        VexFamily.Store.WriteResult =
        if (
            editor.commit()
        ) {
            VexFamily.Store.WriteResult.Success
        } else {
            VexFamily.Store.WriteResult.Failure()
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
