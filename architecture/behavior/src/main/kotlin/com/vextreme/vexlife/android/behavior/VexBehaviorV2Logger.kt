package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Family-facing accountant contract.
 *
 * The live app may supply its one VexLogger instance. A Family never creates another logger.
 * Source identities are carried as KClass/KCallable values rather than a second diagnostic ID list.
 */
fun interface VexFamilyLogger {
    fun record(
        event: VexFamilyLogEvent,
    )

    companion object {
        val None:
            VexFamilyLogger =
            VexFamilyLogger {
            }
    }
}

sealed interface VexFamilyLogEvent {
    val family:
        KClass<*>

    data class ControlAccepted(
        override val family:
            KClass<*>,
        val control:
            KClass<*>,
        val currentState:
            KClass<*>,
    ) :
        VexFamilyLogEvent

    data class StateAdmitted(
        override val family:
            KClass<*>,
        val fromState:
            KClass<*>,
        val toState:
            KClass<*>,
    ) :
        VexFamilyLogEvent

    data class OperationBegan(
        override val family:
            KClass<*>,
        val operation:
            KCallable<*>,
        val state:
            KClass<*>?,
    ) :
        VexFamilyLogEvent

    data class OperationSuccess(
        override val family:
            KClass<*>,
        val operation:
            KCallable<*>,
        val state:
            KClass<*>?,
    ) :
        VexFamilyLogEvent

    data class OperationFailure(
        override val family:
            KClass<*>,
        val operation:
            KCallable<*>,
        val state:
            KClass<*>?,
        val failure:
            KClass<out Throwable>?,
    ) :
        VexFamilyLogEvent
}

// [VXG RealForever]
