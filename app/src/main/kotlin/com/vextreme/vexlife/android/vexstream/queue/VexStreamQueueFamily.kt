package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Semantic owner for the finite Queue controls.
 *
 * Media3 remains queue/provider truth. Queue-row Play and Reorder keep their typed occurrence
 * request boundaries; they are not folded into this finite Control family.
 */
internal class VexStreamQueueFamily(
    initialState:
        State,
) :
    VexFamily<
        VexStreamQueueFamily,
        VexStreamQueueFamily.State,
        VexStreamQueueFamily.Control,
        VexStreamQueueFamily.Effect
    >(
        initialState =
            initialState,
    ) {
    sealed interface State :
        VexFamily.State<
            VexStreamQueueFamily
        > {
        fun isAvailable(
            control:
                Control,
        ):
            Boolean =
            when (
                this
            ) {
                Empty ->
                    false

                is NonEmpty ->
                    when (
                        control
                    ) {
                        Control.Previous ->
                            previousAvailable

                        Control.Next ->
                            nextAvailable

                        Control.Shuffle ->
                            shuffleAvailable

                        Control.Clear ->
                            clearAvailable
                    }
            }

        data object Empty :
            State

        data class NonEmpty(
            val previousAvailable:
                Boolean,
            val nextAvailable:
                Boolean,
            val shuffleAvailable:
                Boolean,
            val clearAvailable:
                Boolean,
        ) :
            State
    }

    enum class Control :
        VexFamily.Control<
            VexStreamQueueFamily
        > {
        Previous,
        Next,
        Shuffle,
        Clear,
    }

    sealed interface Effect :
        VexFamily.Effect<
            VexStreamQueueFamily
        > {
        data object SeekPrevious :
            Effect

        data object SeekNext :
            Effect

        data object ShuffleFuture :
            Effect

        data object ClearQueue :
            Effect
    }

    override fun act(
        currentState:
            State,
        control:
            Control,
    ):
        VexFamily.Act<
            VexStreamQueueFamily,
            State,
            Effect
        >? {
        if (
            !currentState
                .isAvailable(
                    control,
                )
        ) {
            return null
        }

        val effect =
            when (
                control
            ) {
                Control.Previous ->
                    Effect.SeekPrevious

                Control.Next ->
                    Effect.SeekNext

                Control.Shuffle ->
                    Effect.ShuffleFuture

                Control.Clear ->
                    Effect.ClearQueue
            }

        return stay(
            begin(
                effect,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
