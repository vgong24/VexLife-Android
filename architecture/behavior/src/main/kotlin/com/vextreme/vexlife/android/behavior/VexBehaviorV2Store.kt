package com.vextreme.vexlife.android.behavior

import kotlinx.coroutines.flow.Flow

/** Reusable marker for storage/custody capabilities. */
interface VexStore

/**
 * Store capability discoverable from one exact Family.
 *
 * This relationship does NOT imply that the Store persists the Family's semantic State. A Family
 * may expose media, route, artifact or other custody through its Store surface while those values
 * keep their own rightful ownership.
 */
interface VexFamilyStore<
    Family : VexBehaviorFamily<Family>,
> :
    VexStore

/**
 * Earned specialization for a Store whose durable value is exactly the Family State.
 *
 * ThemeMode/SharedPreferences is the current patient zero:
 *
 *     Family.current != durable State while Save is pending
 *
 * Get / ObservableState / Save / Forget describe the State-store capability. At the current V2
 * runtime only Save is integrated into the Act DSL; Get, ObservableState and Forget remain explicit
 * capability pressure rather than pretending the behavioral integration is already complete.
 */
interface VexFamilyStateStore<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> :
    VexFamilyStore<
        Family
    > {
    val observableState:
        Flow<State>

    suspend fun get():
        State

    suspend fun save(
        state: State,
    ):
        VexStoreWriteResult

    suspend fun forget():
        VexStoreWriteResult
}

/**
 * Result of the current State-store write patient zero.
 *
 * This is intentionally Store-specific. It must not become the universal Operation result shape:
 * richer provider/repository Operations may return typed Accepted/Rejected, Stored/Rejected or other
 * domain results when real pressure earns them.
 */
sealed interface VexStoreWriteResult {
    data object Success :
        VexStoreWriteResult

    data class Failure(
        val cause: Throwable? = null,
    ) :
        VexStoreWriteResult
}

/**
 * Emerging generic work slot admitted inside begin(...).
 *
 * Operation is the structural slot. Save is currently the only authored/runtime-realized Operation
 * in V2. Payload-bearing provider Operations are still pressure, not an advertised completed API.
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

/** Store-specialized Operation family. */
interface VexStoreOperation<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> :
    VexOperation<
        Family,
        State
    >

// [VXG RealForever]
