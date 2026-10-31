package com.vextreme.vexlife.android.execution

import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty0
import kotlin.reflect.KSuspendFunction0
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owner-bound execution surface for Vex runtime work.
 *
 * VexExecution owns execution mechanics only:
 *
 * SourceRef
 *     diagnostic callable identity
 *
 * InstanceId
 *     one exact admitted runtime occurrence
 *
 * CoordinationKey
 *     owner-local key for overlap policy when work intentionally coordinates
 *
 * Status / Snapshot / Handle
 *     runtime observation and cancellation
 *
 * A live VexExecution instance is itself the runtime owner boundary. Source-derived operations are
 * scoped to that exact owner instance, so two live instances of the same class may execute/observe
 * the same callable without colliding merely because their diagnostic SourceRef strings match.
 */
abstract class VexExecution protected constructor(
    private val executionRoot:
        VexExecutionRoot,
) {
    /** Diagnostic source identity derived from owner class + callable name. */
    @JvmInline
    value class SourceRef internal constructor(
        val value:
            String,
    ) {
        init {
            require(
                value.isNotBlank(),
            ) {
                "VexExecution.SourceRef must be non-blank"
            }
        }

        override fun toString():
            String =
            value
    }

    /** Exact process-runtime identity of one admitted execution occurrence. */
    @JvmInline
    value class InstanceId internal constructor(
        val value:
            Long,
    ) {
        init {
            require(
                value >
                    0L,
            ) {
                "VexExecution.InstanceId must be positive"
            }
        }

        override fun toString():
            String =
            value.toString()
    }

    /**
     * Owner-local runtime coordination key.
     *
     * Execution gives this key no product meaning. The concrete runtime owner decides what work
     * intentionally coordinates; VexExecution only applies the selected overlap behavior.
     */
    interface CoordinationKey

    /**
     * Recovery policy for one long-lived observation source.
     *
     * Only source creation/upstream collection failure is recoverable. Downstream consumer failure
     * remains an execution failure and is never rewritten as a source retry.
     */
    data class ObservationRecovery(
        val maxRestarts:
            Int =
            3,
        val restartDelayMillis:
            Long =
            250L,
    ) {
        init {
            require(
                maxRestarts >=
                    0,
            ) {
                "ObservationRecovery.maxRestarts must be non-negative"
            }
            require(
                restartDelayMillis >=
                    0L,
            ) {
                "ObservationRecovery.restartDelayMillis must be non-negative"
            }
        }

        companion object {
            val None =
                ObservationRecovery(
                    maxRestarts =
                        0,
                    restartDelayMillis =
                        0L,
                )
        }
    }

    enum class Status {
        STARTING,
        RUNNING,
        RESTARTING,
        COMPLETED,
        CANCELLED,
        FAILED,
    }

    data class Snapshot(
        val instanceId:
            InstanceId,
        val sourceRef:
            SourceRef,
        val status:
            Status,
        val attempt:
            Int,
        val restartCount:
            Int,
        val failureClass:
            String? =
            null,
        val failureMessage:
            String? =
            null,
    )

    interface Handle {
        val instanceId:
            InstanceId
        val sourceRef:
            SourceRef
        val snapshot:
            StateFlow<Snapshot>

        fun forceStop(
            reason:
                String =
                "force stopped",
        ):
            Boolean

        suspend fun joinStop(
            reason:
                String =
                "join stopped",
        ):
            Boolean

        suspend fun awaitCompletion()
    }

    /**
     * Exact live execution-owner identity.
     *
     * Internal because this is runtime custody mechanics, not semantic architecture.
     */
    internal class OwnerToken internal constructor()

    /*
     * Runtime ownership is intentionally distinct from SourceRef.
     *
     * SourceRef may be identical across two live instances of this same execution class. This token
     * scopes uniqueness, cancellation and coordination to this exact live owner.
     */
    private val ownerToken =
        OwnerToken()

    private val executionName:
        String
        get() =
            this::class.qualifiedName
                ?: this::class.simpleName
                ?: error(
                    "VexExecution requires a named owner class",
                )

    protected fun <Result> launchOnce(
        action:
            KSuspendFunction0<Result>,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchOnce(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchOnce(
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchOnce(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launchOnce(
        action:
            KSuspendFunction2<
                First,
                Second,
                Result
            >,
        first:
            First,
        second:
            Second,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchOnce(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    protected fun <Result> launch(
        action:
            KSuspendFunction0<Result>,
    ):
        Handle =
        executionRoot.coroutineLauncher.launch(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launch(
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Handle =
        executionRoot.coroutineLauncher.launch(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <First, Second, Result> launch(
        action:
            KSuspendFunction2<
                First,
                Second,
                Result
            >,
        first:
            First,
        second:
            Second,
    ):
        Handle =
        executionRoot.coroutineLauncher.launch(
            ownerToken =
                ownerToken,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                first,
                second,
            )
        }

    protected fun <Result> launchEarliest(
        key:
            CoordinationKey,
        action:
            KSuspendFunction0<Result>,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchEarliest(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchEarliest(
        key:
            CoordinationKey,
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchEarliest(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <Result> launchLatest(
        key:
            CoordinationKey,
        action:
            KSuspendFunction0<Result>,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchLatest(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchLatest(
        key:
            CoordinationKey,
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchLatest(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    protected fun <Result> launchQueued(
        key:
            CoordinationKey,
        action:
            KSuspendFunction0<Result>,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchQueued(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action()
        }

    protected fun <Argument, Result> launchQueued(
        key:
            CoordinationKey,
        action:
            KSuspendFunction1<
                Argument,
                Result
            >,
        argument:
            Argument,
    ):
        Handle =
        executionRoot.coroutineLauncher.launchQueued(
            ownerToken =
                ownerToken,
            key =
                key,
            sourceIdentity =
                action,
            sourceRef =
                sourceRef(
                    action,
                ),
        ) {
            action(
                argument,
            )
        }

    /**
     * Long-lived owner-scoped Flow observation.
     *
     * A second active observation of the same callable from this same live execution owner fails
     * closed. Another live owner may observe the same source independently.
     */
    protected fun <Value, Source : Flow<Value>> observe(
        source:
            KFunction0<Source>,
        recovery:
            ObservationRecovery =
            ObservationRecovery.None,
        onEach:
            suspend (Value) -> Unit,
    ):
        Handle =
        executionRoot.coroutineLauncher.observe(
            ownerToken =
                ownerToken,
            sourceIdentity =
                source,
            sourceRef =
                sourceRef(
                    source,
                ),
            recovery =
                recovery,
            source =
                source,
            onEach =
                onEach,
        )

    protected fun <Value, Source : Flow<Value>> observe(
        source:
            KProperty0<Source>,
        recovery:
            ObservationRecovery =
            ObservationRecovery.None,
        onEach:
            suspend (Value) -> Unit,
    ):
        Handle =
        executionRoot.coroutineLauncher.observe(
            ownerToken =
                ownerToken,
            sourceIdentity =
                source,
            sourceRef =
                sourceRef(
                    source,
                ),
            recovery =
                recovery,
            source =
                source::get,
            onEach =
                onEach,
        )

    protected fun <Argument, Value, Source : Flow<Value>> observe(
        source:
            KFunction1<
                Argument,
                Source
            >,
        argument:
            Argument,
        recovery:
            ObservationRecovery =
            ObservationRecovery.None,
        onEach:
            suspend (Value) -> Unit,
    ):
        Handle =
        executionRoot.coroutineLauncher.observe(
            ownerToken =
                ownerToken,
            sourceIdentity =
                source,
            sourceRef =
                sourceRef(
                    source,
                ),
            recovery =
                recovery,
            source = {
                source(
                    argument,
                )
            },
            onEach =
                onEach,
        )

    /** Stop active executions of this exact owner + callable. */
    protected fun forceStop(
        reference:
            KCallable<*>,
        reason:
            String =
            "force stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.forceStopSource(
            ownerToken =
                ownerToken,
            sourceIdentity =
                reference,
            reason =
                reason,
        )

    /** Stop and join active executions of this exact owner + callable. */
    protected suspend fun joinStop(
        reference:
            KCallable<*>,
        reason:
            String =
            "join stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.joinStopSource(
            ownerToken =
                ownerToken,
            sourceIdentity =
                reference,
            reason =
                reason,
        )

    /** Stop every execution currently owned by this exact VexExecution instance. */
    protected fun forceStopAll(
        reason:
            String =
            "owner force stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.forceStopOwner(
            ownerToken =
                ownerToken,
            reason =
                reason,
        )

    /** Stop and join every execution currently owned by this exact VexExecution instance. */
    protected suspend fun joinStopAll(
        reason:
            String =
            "owner join stopped",
    ):
        Boolean =
        executionRoot.coroutineLauncher.joinStopOwner(
            ownerToken =
                ownerToken,
            reason =
                reason,
        )

    private fun sourceRef(
        reference:
            KCallable<*>,
    ):
        SourceRef =
        SourceRef(
            executionName +
                "." +
                reference.name,
        )

    internal companion object {
        private val nextInstanceValue =
            AtomicLong(
                1L,
            )

        fun nextInstanceId():
            InstanceId =
            InstanceId(
                nextInstanceValue
                    .getAndIncrement(),
            )
    }
}

// [VXG RealForever]
