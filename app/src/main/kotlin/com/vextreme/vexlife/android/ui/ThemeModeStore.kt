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

    /**
     * Attempt to persist one explicit ThemeMode selection.
     *
     * The Store owns only the provider fact: true means the physical write committed; false means
     * the provider lawfully reported that it did not. VexMethod.Result begins at the bound Method,
     * not at this outside-truth boundary.
     */
    suspend fun save(
        state:
            ThemeMode,
    ):
        Boolean

    /** Remove the explicit durable selection; return only whether the physical write committed. */
    suspend fun forget():
        Boolean
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
        Boolean {
        require(
            state !=
                ThemeMode.NotSet,
        ) {
            "ThemeMode.NotSet is absence of an explicit durable selection; use forget()"
        }

        return preferences
            .edit()
            .putString(
                KEY_THEME_MODE,
                storedValueFor(
                    state,
                ),
            )
            .commit()
    }

    override suspend fun forget():
        Boolean =
        preferences
            .edit()
            .remove(
                KEY_THEME_MODE,
            )
            .commit()

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
        Boolean {
        require(
            state !=
                ThemeMode.NotSet,
        ) {
            "ThemeMode.NotSet is absence of an explicit selection; use forget()"
        }

        mutableState.value =
            state
        return true
    }

    override suspend fun forget():
        Boolean {
        mutableState.value =
            ThemeMode.NotSet
        return true
    }
}

// [VXG RealForever]
