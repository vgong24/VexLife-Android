package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.Flow

/**
 * Family-facing boundary to truth that is not owned by the pure semantic Family.
 *
 * Store is intentionally broader than persistence. A concrete Store may use temporary memory,
 * caches, repositories, databases, services/backends, platform APIs, third-party SDK objects, or
 * any composition of those mechanics.
 *
 * The exact Family/State/Control/Effect coordinate prevents a Store from being silently attached to
 * an unrelated Family merely because method signatures happen to look compatible.
 */
interface VexStore<
    Family,
    FamilyState,
    FamilyControl,
    FamilyEffect,
> where
    Family : VexFamily<
        Family,
        FamilyState,
        FamilyControl,
        FamilyEffect
    >,
    FamilyState : VexFamily.State<Family>,
    FamilyControl : VexFamily.Control<Family>,
    FamilyEffect : VexFamily.Effect<Family> {
    /**
     * Optional outside-truth observation surface.
     *
     * Fetch-only Stores may return null and return an observed State with [Result.Success] instead.
     * A Store must not fabricate semantic State merely to satisfy this optional surface.
     */
    val states:
        Flow<FamilyState>?

    /**
     * Realize one Effect against outside truth.
     *
     * Success may carry already-reconciled semantic State when the operation itself observed it.
     * Otherwise outside truth may arrive independently through [states].
     */
    suspend fun realize(
        effect:
            FamilyEffect,
    ):
        Result<FamilyState>

    sealed interface Result<
        out State,
    > {
        data class Success<
            State,
        >(
            val observedState:
                State? =
                null,
        ) :
            Result<State>

        data class Failure(
            val cause:
                Throwable? =
                null,
        ) :
            Result<Nothing>
    }
}

// [VXG RealForever]
