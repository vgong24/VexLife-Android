package com.vextreme.vexlife.android.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Family-facing outside-truth boundary for ThemeMode.
 *
 * Store is intentionally broader than persistence. This concrete contract currently supports
 * durable Android selection plus observation/fetch semantics; tests/previews may use the in-memory
 * realization below without changing the Family relationship.
 */
internal interface ThemeModeStore {
    val states:
        Flow<ThemeMode>

    suspend fun get():
        ThemeMode

    suspend fun save(
        state:
            ThemeMode,
    ):
        Result

    suspend fun forget():
        Result

    sealed interface Result {
        data class Success(
            val observedState:
                ThemeMode,
        ) :
            Result

        data class Failure(
            val cause:
                Throwable? =
                null,
        ) :
            Result
    }
}

/**
 * Android SharedPreferences realization of [ThemeModeStore].
 *
 * SharedPreferences is implementation mechanics under Store; neither the semantic Family nor the
 * ThemeMode runtime needs to expose this platform detail upward.
 */
internal class SharedPreferencesThemeModeStore(
    context:
        Context,
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

    override val states:
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
        ThemeModeStore.Result =
        when (state) {
            ThemeMode.NotSet ->
                ThemeModeStore.Result.Failure(
                    IllegalArgumentException(
                        "ThemeMode.NotSet is absence of an explicit durable selection; use forget()",
                    ),
                )

            ThemeMode.Light,
            ThemeMode.Dark ->
                write(
                    editor =
                        preferences
                            .edit()
                            .putString(
                                KEY_THEME_MODE,
                                storedValueFor(
                                    state,
                                ),
                            ),
                    observedState =
                        state,
                )
        }

    override suspend fun forget():
        ThemeModeStore.Result =
        write(
            editor =
                preferences
                    .edit()
                    .remove(
                        KEY_THEME_MODE,
                    ),
            observedState =
                ThemeMode.NotSet,
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
        observedState:
            ThemeMode,
    ):
        ThemeModeStore.Result =
        if (
            editor.commit()
        ) {
            ThemeModeStore.Result.Success(
                observedState =
                    observedState,
            )
        } else {
            ThemeModeStore.Result.Failure()
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

/**
 * Temporary Store realization for previews/tests that intentionally have no Android persistence.
 */
internal class InMemoryThemeModeStore(
    initialState:
        ThemeMode,
) :
    ThemeModeStore {
    private val mutableState =
        MutableStateFlow(
            initialState,
        )

    override val states:
        Flow<ThemeMode> =
        mutableState

    override suspend fun get():
        ThemeMode =
        mutableState.value

    override suspend fun save(
        state:
            ThemeMode,
    ):
        ThemeModeStore.Result {
        if (
            state ==
            ThemeMode.NotSet
        ) {
            return ThemeModeStore.Result.Failure(
                IllegalArgumentException(
                    "ThemeMode.NotSet is absence of an explicit selection; use forget()",
                ),
            )
        }

        mutableState.value =
            state
        return ThemeModeStore.Result.Success(
            observedState =
                state,
        )
    }

    override suspend fun forget():
        ThemeModeStore.Result {
        mutableState.value =
            ThemeMode.NotSet
        return ThemeModeStore.Result.Success(
            observedState =
                ThemeMode.NotSet,
        )
    }
}

// [VXG RealForever]
