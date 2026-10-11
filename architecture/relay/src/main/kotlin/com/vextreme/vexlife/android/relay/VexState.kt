package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self>
interface VexInputState<Self : VexInputState<Self>> : VexState<Self>
interface VexOutputState<Self : VexOutputState<Self>> : VexState<Self>

interface VexObservableRelay<State : VexState<State>> {
    val states: Flow<State>
    val current: State
}

// [VXG RealForever]
