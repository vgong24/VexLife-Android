package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.logging.VexLogRecord
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Non-blocking bridge from synchronous VexLogger calls into a suspend repository.
 *
 * This class deliberately does NOT create/own its own application scope. The caller supplies the
 * lifecycle owner. Production now supplies VexLifeViewModel.viewModelScope so diagnostic work
 * survives Activity configuration recreation. A future VexAppScope may take over longer-lived
 * semantic operation ownership without changing the repository or logger contract.
 *
 * Diagnostic persistence is non-authoritative:
 * - append never blocks the product action that produced the log;
 * - repository failures are counted and swallowed;
 * - if the bounded queue is full, the oldest queued diagnostic append may be dropped rather than
 *   changing app semantics.
 *
 * Clear is an ordered command in the same queue. DROP_OLDEST makes room for it under pressure, so
 * later records remain after the Reset boundary instead of racing ahead of it.
 */
class LogHistoryPersistenceQueue(
    private val repository: LogHistoryRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    capacity: Int = 256,
) {
    private sealed interface Command {
        data class Append(val record: VexLogRecord) : Command
        data object Clear : Command
    }

    private val _droppedRecordCount = AtomicLong(0)
    private val commands = Channel<Command>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { command ->
            if (command is Command.Append) {
                _droppedRecordCount.incrementAndGet()
            }
        },
    )
    private val _failureCount = AtomicLong(0)

    val droppedRecordCount: Long
        get() = _droppedRecordCount.get()

    val failureCount: Long
        get() = _failureCount.get()

    init {
        require(capacity > 0) { "capacity must be positive" }

        scope.launch(dispatcher) {
            for (command in commands) {
                try {
                    when (command) {
                        is Command.Append -> repository.append(command.record)
                        Command.Clear -> repository.clear()
                    }
                } catch (_: Throwable) {
                    _failureCount.incrementAndGet()
                }
            }
        }
    }

    /**
     * Safe to call directly from VexLogger.onRecord.
     */
    fun record(record: VexLogRecord) {
        commands.trySend(Command.Append(record))
    }

    /**
     * DROP_OLDEST ensures this clear command can enter a full queue by sacrificing an older
     * diagnostic append. Later records are queued after Clear, preserving the user's Reset boundary.
     */
    fun clear() {
        commands.trySend(Command.Clear)
    }
}
