package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.Flow

interface VexStore

/**
 * Durable Store bound to one exact Behavior Family.
 *
 * Family.current is semantic truth now. Store truth is what has been made durable for continuity.
 * Get and ObservableState are two access forms over the same durable truth.
 */
interface VexFamilyStore<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> :
    VexStore {
    val observableState:
        Flow<State>

    suspend fun get():
        State

    suspend fun save(
        state: State,
    ):
        VexOperationResult

    suspend fun forget():
        VexOperationResult
}

sealed interface VexOperationResult {
    data object Success :
        VexOperationResult

    data class Failure(
        val cause: Throwable? = null,
    ) :
        VexOperationResult
}

/**
 * Generic work admitted inside begin(...).
 *
 * Operation is the execution slot. Domain/store verbs remain readable specializations; Save stays
 * Save rather than becoming an opaque operation(...) call.
 */
interface VexOperation<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> {
    val results:
        VexBehaviorV2Results<
            Family,
            State
        >
}

interface VexStoreOperation<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> :
    VexOperation<
        Family,
        State
    >

// [VXG RealForever]
