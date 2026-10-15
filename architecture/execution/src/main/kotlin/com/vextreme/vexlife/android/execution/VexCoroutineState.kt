package com.vextreme.vexlife.android.execution

import kotlinx.coroutines.flow.StateFlow

@JvmInline
value class VexCoroutineRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexCoroutineRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

enum class VexCoroutineStatus {
    STARTING,
    RUNNING,
    RESTARTING,
    COMPLETED,
    CANCELLED,
    FAILED,
}

internal fun VexCoroutineStatus.isTerminal(): Boolean =
    this == VexCoroutineStatus.COMPLETED ||
        this == VexCoroutineStatus.CANCELLED ||
        this == VexCoroutineStatus.FAILED

data class VexCoroutineState(
    val ref: VexCoroutineRef,
    val status: VexCoroutineStatus,
    val attempt: Int,
    val restartCount: Int,
    val failureClass: String? = null,
    val failureMessage: String? = null,
)

interface VexCoroutineHandle {
    val ref: VexCoroutineRef
    val state: StateFlow<VexCoroutineState>

    fun forceStop(
        reason: String = "force stopped",
    ): Boolean

    suspend fun joinStop(
        reason: String = "join stopped",
    ): Boolean

    suspend fun awaitCompletion()
}

// [VXG RealForever]
