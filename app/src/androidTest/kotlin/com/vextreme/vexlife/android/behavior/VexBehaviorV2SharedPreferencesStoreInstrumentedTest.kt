package com.vextreme.vexlife.android.behavior

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android target pressure for real Family Store persistence.
 *
 * SharedPreferences is only the first Store realization. Preference keys/values are derived from
 * Family/State source identity rather than a second hand-maintained semantic string catalog.
 */
class VexBehaviorV2SharedPreferencesStoreInstrumentedTest {
    private class ThemeModeFamily(
        initialState: State,
        executionRoot: VexExecutionRoot,
        override val store: Store,
        logger: VexFamilyLogger,
    ) :
        VexBehaviorV2Family<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                logger,
        ) {
        sealed interface State :
            VexBehaviorFamily.State<
                ThemeModeFamily
            > {
            data object NotSet :
                State

            data object Light :
                State

            data object Dark :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            data object Toggle :
                Control
        }

        interface Store :
            VexFamilyStore<
                ThemeModeFamily,
                State
            >

        sealed interface DisplayState :
            VexBehaviorV2DisplayState<
                State
            > {
            data class Empty(
                override val state:
                    State,
            ) :
                DisplayState

            data class Active(
                override val state:
                    State,
            ) :
                DisplayState

            data class Inactive(
                override val state:
                    State,
            ) :
                DisplayState
        }

        override val acts =
            vexActs<
                ThemeModeFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "android.shared-preferences.theme-mode.v2",
            ) {
                state<State.NotSet>()

                state<State.Light> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Dark,
                        )

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Replaceable,
                        ) {
                            save(
                                State.Dark,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        }
                    }
                }

                state<State.Dark> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Light,
                        )

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Replaceable,
                        ) {
                            save(
                                State.Light,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        }
                    }
                }
            }

        override val displayState:
            DisplayState
            get() =
                when (
                    val state =
                        current
                ) {
                    State.NotSet ->
                        DisplayState.Empty(
                            state,
                        )

                    State.Light ->
                        DisplayState.Inactive(
                            state,
                        )

                    State.Dark ->
                        DisplayState.Active(
                            state,
                        )
                }
    }

    private class SharedPreferencesThemeModeStore(
        context: Context,
    ) :
        ThemeModeFamily.Store {
        private val preferences =
            context.applicationContext
                .getSharedPreferences(
                    requireNotNull(
                        ThemeModeFamily::class.qualifiedName,
                    ),
                    Context.MODE_PRIVATE,
                )

        private val stateKey =
            requireNotNull(
                ThemeModeFamily.State::class.qualifiedName,
            )

        private val knownStates =
            listOf(
                ThemeModeFamily.State.NotSet,
                ThemeModeFamily.State.Light,
                ThemeModeFamily.State.Dark,
            )

        override val observableState:
            Flow<
                ThemeModeFamily.State
            > =
            callbackFlow {
                trySend(
                    read(),
                )

                val listener =
                    android.content.SharedPreferences
                        .OnSharedPreferenceChangeListener { _, changedKey ->
                            if (
                                changedKey ==
                                stateKey
                            ) {
                                trySend(
                                    read(),
                                )
                            }
                        }

                preferences
                    .registerOnSharedPreferenceChangeListener(
                        listener,
                    )

                awaitClose {
                    preferences
                        .unregisterOnSharedPreferenceChangeListener(
                            listener,
                        )
                }
            }

        override suspend fun get():
            ThemeModeFamily.State =
            read()

        override suspend fun save(
            state:
                ThemeModeFamily.State,
        ):
            VexOperationResult {
            if (
                state ==
                ThemeModeFamily.State.NotSet
            ) {
                return forget()
            }

            return if (
                preferences
                    .edit()
                    .putString(
                        stateKey,
                        stateIdentity(
                            state,
                        ),
                    )
                    .commit()
            ) {
                VexOperationResult.Success
            } else {
                VexOperationResult.Failure()
            }
        }

        override suspend fun forget():
            VexOperationResult =
            if (
                preferences
                    .edit()
                    .remove(
                        stateKey,
                    )
                    .commit()
            ) {
                VexOperationResult.Success
            } else {
                VexOperationResult.Failure()
            }

        fun clearForTest() {
            check(
                preferences
                    .edit()
                    .clear()
                    .commit(),
            )
        }

        private fun read():
            ThemeModeFamily.State {
            val storedIdentity =
                preferences.getString(
                    stateKey,
                    null,
                )
                    ?: return ThemeModeFamily.State.NotSet

            return knownStates
                .singleOrNull { state ->
                    stateIdentity(
                        state,
                    ) ==
                        storedIdentity
                }
                ?: ThemeModeFamily.State.NotSet
        }

        private fun stateIdentity(
            state:
                ThemeModeFamily.State,
        ): String =
            requireNotNull(
                state::class.qualifiedName,
            )
    }

    @Test
    fun saveCommitSuccessIsWitnessedAndRestoresStateAfterFamilyReconstruction() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val store =
            SharedPreferencesThemeModeStore(
                context,
            )
        store.clearForTest()

        val root =
            VexExecutionRoot(
                dispatcher =
                    Dispatchers.IO,
            )
        val success =
            CountDownLatch(
                1,
            )
        val logger =
            VexLogger(
                onRecord = { record ->
                    if (
                        record.kind.name ==
                        "Family" &&
                        record.action ==
                        "Save(Dark) Success"
                    ) {
                        success.countDown()
                    }
                },
            )

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
                    store =
                        store,
                    logger =
                        logger,
                )

            family.activate(
                ThemeModeFamily.Control.Toggle,
            )

            assertSame(
                ThemeModeFamily.State.Dark,
                family.current,
            )

            assertTrue(
                "SharedPreferences Save(Dark) Success was not witnessed",
                success.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            val restored =
                runBlocking {
                    store.get()
                }

            assertSame(
                ThemeModeFamily.State.Dark,
                restored,
            )

            val reconstructedFamily =
                ThemeModeFamily(
                    initialState =
                        restored,
                    executionRoot =
                        root,
                    store =
                        store,
                    logger =
                        logger,
                )

            assertEquals(
                ThemeModeFamily.DisplayState.Active(
                    ThemeModeFamily.State.Dark,
                ),
                reconstructedFamily.displayState,
            )
            assertTrue(
                logger.renderFamilyHistory()
                    .contains(
                        "Save(Dark) Success",
                    ),
            )
        } finally {
            root.forceStopAll()
            store.clearForTest()
        }
    }

    @Test
    fun forgetReturnsDurableTruthToNotSetUsingTheSameFamilyIdentityCoordinate() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val store =
            SharedPreferencesThemeModeStore(
                context,
            )
        store.clearForTest()

        try {
            assertEquals(
                VexOperationResult.Success,
                runBlocking {
                    store.save(
                        ThemeModeFamily.State.Dark,
                    )
                },
            )
            assertSame(
                ThemeModeFamily.State.Dark,
                runBlocking {
                    store.get()
                },
            )

            assertEquals(
                VexOperationResult.Success,
                runBlocking {
                    store.forget()
                },
            )
            assertSame(
                ThemeModeFamily.State.NotSet,
                runBlocking {
                    store.get()
                },
            )
        } finally {
            store.clearForTest()
        }
    }
}

// [VXG RealForever]
