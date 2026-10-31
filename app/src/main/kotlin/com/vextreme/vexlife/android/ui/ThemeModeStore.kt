package com.vextreme.vexlife.android.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android-local durable realization of ThemeMode explicit selection.
 *
 * Durable selection is runtime/platform truth owned outside [ThemeModeFamily]. The semantic Family
 * never receives this Store and never performs persistence itself.
 */
internal interface ThemeModeStore {
    val observableState: Flow<ThemeMode>

    suspend fun get(): ThemeMode

    suspend fun save(
        state: ThemeMode,
    ): WriteResult

    suspend fun forget(): WriteResult

    sealed interface WriteResult {
        data object Success :
            WriteResult

        data class Failure(
            val cause: Throwable? = null,
        ) :
            WriteResult
    }
}

/**
 * Android SharedPreferences realization of [ThemeModeStore].
 *
 * SharedPreferences is platform storage mechanics, not ThemeMode semantic meaning. Listener lifetime
 * is active only while [observableState] is collected by the runtime Connection.
 */
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

    override suspend fun get():
        ThemeMode =
        readCurrent()

    override suspend fun save(
        state:
            ThemeMode,
    ):
        ThemeModeStore.WriteResult =
        when (state) {
            ThemeMode.NotSet ->
                ThemeModeStore.WriteResult.Failure(
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

    override suspend fun forget():
        ThemeModeStore.WriteResult =
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
        ThemeModeStore.WriteResult =
        if (
            editor.commit()
        ) {
            ThemeModeStore.WriteResult.Success
        } else {
            ThemeModeStore.WriteResult.Failure()
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
