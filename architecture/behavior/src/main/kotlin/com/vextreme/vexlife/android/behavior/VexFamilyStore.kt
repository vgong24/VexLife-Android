package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.Flow

/** Reusable marker for storage/custody capabilities. */
interface VexStore

/**
 * Store capability discoverable from a Family.
 *
 * This does not imply that the Store persists semantic Family State.
 */
interface VexFamilyStore :
    VexStore

/**
 * Store whose durable value is exactly the Family State.
 */
interface VexFamilyStateStore<
    State : Any,
> :
    VexFamilyStore {
    val observableState:
        Flow<State>

    suspend fun get():
        State

    suspend fun save(
        state:
            State,
    ):
        VexStoreWriteResult

    suspend fun forget():
        VexStoreWriteResult
}

sealed interface VexStoreWriteResult {
    data object Success :
        VexStoreWriteResult

    data class Failure(
        val cause:
            Throwable? =
            null,
    ) :
        VexStoreWriteResult
}

internal interface VexOperation<
    State : Any,
> {
    val results:
        VexResults<
            State
        >
}

internal interface VexStoreOperation<
    State : Any,
> :
    VexOperation<
        State
    >

// [VXG RealForever]
