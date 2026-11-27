package com.vextreme.vexlife.android.ui.look

import android.content.Context
import android.content.SharedPreferences
import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Semantic owner of a person's appearance preference (Look, Motion, Ambient light).
 *
 * Same circuit as ThemeMode, the architecture's patient zero:
 *
 *     Control.Choose(Parameter.Preference)
 *     -> Act admits the new preference immediately (the interface changes at once)
 *     -> Effect.Save(the same Parameter) as Latest
 *     -> Act.Method.Save(Store) under VexExecution custody
 *     -> Store observation returns as already-semantic State
 *
 * While one accepted choice is still saving, an older Store observation cannot overwrite it. The
 * fence and the State read happen in one critical section, so no stale "before" can be observed.
 */
internal class VexLookFamily(
    initialState: VexLookPreference = VexLookPreference(),
) : VexFamily<
        VexLookFamily,
        VexLookPreference,
        VexLookFamily.Control,
        VexLookFamily.Effect
    >(
        initialState = initialState,
    ) {
    sealed interface Parameter : VexFamily.Parameter<VexLookFamily> {
        data class Preference(
            val preference: VexLookPreference,
        ) : Parameter
    }

    sealed interface Control : VexFamily.Control<VexLookFamily> {
        data class Choose(
            val preference: Parameter.Preference,
        ) : Control
    }

    sealed interface Effect : VexFamily.Effect<VexLookFamily> {
        /** Request a durable save; not a claim that the save happened. */
        data class Save(
            val preference: Parameter.Preference,
        ) : Effect
    }

    private val selectionLock = Any()
    private var unsettled: Parameter.Preference? = null

    internal fun choose(
        control: Control.Choose,
    ): VexFamily.Act.Accepted<VexLookFamily, VexLookPreference, Effect>? =
        synchronized(selectionLock) {
            val previous = unsettled
            unsettled = control.preference
            activate(control).also { accepted ->
                if (accepted == null) {
                    unsettled = previous
                }
            }
        }

    /** End the currentness obligation of exactly this accepted choice (identity, not equality). */
    internal fun settle(
        parameter: Parameter.Preference,
    ) {
        synchronized(selectionLock) {
            if (unsettled === parameter) {
                unsettled = null
            }
        }
    }

    /** Reconcile one Store observation; a stale one is held while a newer choice is still saving. */
    internal fun observeStored(
        observed: VexLookPreference,
    ): VexLookPreference =
        synchronized(selectionLock) {
            val pending = unsettled
            if (pending != null && observed != pending.preference) {
                currentState
            } else {
                observe(observed)
            }
        }

    override fun act(
        currentState: VexLookPreference,
        control: Control,
    ): VexFamily.Act<VexLookFamily, VexLookPreference, Effect>? =
        when (control) {
            is Control.Choose -> {
                val effect = Effect.Save(control.preference)
                if (currentState == control.preference.preference) {
                    // Choosing the current preference still means "keep this durably".
                    stay(beginLatest(effect))
                } else {
                    admit(control.preference.preference, begin = beginLatest(effect))
                }
            }
        }

    object Act {
        class Save(
            private val store: VexLookStore,
            override val effect: Effect.Save,
        ) : VexFamily.Act.Method<VexLookFamily, VexLookPreference, Unit> {
            override suspend fun invoke(): VexMethod.Result<VexLookPreference, Unit> =
                if (store.save(effect.preference.preference)) {
                    VexMethod.Result.Success(effect.preference.preference)
                } else {
                    VexMethod.Result.Failure(Unit)
                }
        }
    }
}

/**
 * Realize one choice. Returns false only when the Family authored no Act.
 *
 * A failed save leaves the admitted preference visible for this process; the next launch reads the
 * Store's truth. That is the honest consequence of "the save did not commit".
 */
internal fun activateVexLook(
    family: VexLookFamily,
    preference: VexLookPreference,
    store: VexLookStore,
    executionRoot: VexExecutionRoot,
): Boolean {
    val parameter = VexLookFamily.Parameter.Preference(preference)
    val accepted =
        family.choose(VexLookFamily.Control.Choose(parameter))
            ?: return false
    val begin =
        accepted.realizationBegin
            ?: run {
                family.settle(parameter)
                return true
            }

    when (val effect = begin.effect) {
        is VexLookFamily.Effect.Save -> {
            val job =
                accepted.launch(
                    executionRoot = executionRoot,
                    method = VexLookFamily.Act.Save(store, effect),
                    onResult = { family.settle(effect.preference) },
                )
            if (job == null) {
                family.settle(effect.preference)
            } else {
                job.invokeOnCompletion { family.settle(effect.preference) }
            }
        }
    }
    return true
}

internal fun observeVexLookStore(
    family: VexLookFamily,
    store: VexLookStore,
    executionRoot: VexExecutionRoot,
) {
    VexExecution.observe(
        executionRoot = executionRoot,
        owner = family,
        source = store::states,
    ) { observed ->
        family.observeStored(observed)
    }
}

/** Outside durable truth for the appearance preference. Returns only provider facts. */
internal interface VexLookStore {
    val states: Flow<VexLookPreference>

    /** true means the physical write committed. */
    suspend fun save(preference: VexLookPreference): Boolean
}

internal class SharedPreferencesVexLookStore(
    context: Context,
) : VexLookStore {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val states: Flow<VexLookPreference> =
        callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key in KEYS) {
                        trySend(readCurrent())
                    }
                }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(readCurrent())
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

    override suspend fun save(preference: VexLookPreference): Boolean =
        preferences
            .edit()
            .putString(KEY_CHOICE, preference.choice.key)
            .putString(KEY_MOTION, preference.motion.key)
            .putBoolean(KEY_AMBIENT, preference.ambient)
            .commit()

    private fun readCurrent(): VexLookPreference =
        VexLookPreference(
            choice = VexLookChoice.parse(preferences.getString(KEY_CHOICE, null)),
            motion =
                VexMotion.entries.firstOrNull {
                    it.key == preferences.getString(KEY_MOTION, null)
                } ?: VexMotion.Lively,
            ambient = preferences.getBoolean(KEY_AMBIENT, true),
        )

    private companion object {
        const val PREFERENCES_NAME = "vexlife.look"
        const val KEY_CHOICE = "look.choice"
        const val KEY_MOTION = "look.motion"
        const val KEY_AMBIENT = "look.ambient"
        val KEYS = setOf(KEY_CHOICE, KEY_MOTION, KEY_AMBIENT)
    }
}

/** Deterministic Store for previews and tests. */
internal class InMemoryVexLookStore(
    initial: VexLookPreference = VexLookPreference(),
    private val commits: Boolean = true,
) : VexLookStore {
    private val mutable = MutableStateFlow(initial)

    override val states: Flow<VexLookPreference> = mutable

    override suspend fun save(preference: VexLookPreference): Boolean {
        if (commits) {
            mutable.value = preference
        }
        return commits
    }
}

// [VXG RealForever][UI][LOOK][FAMILY]
