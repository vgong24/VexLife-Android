package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.Job

/**
 * Semantic owner for VexStream Queue navigation/mutation meaning.
 *
 * Player Store owns retained outside queue truth. Queue Family owns what Previous / Next / Shuffle /
 * Clear mean against that exact semantic State. Provider outcome returns later through Player Store.
 */
internal class VexStreamQueueFamily(
    initialState:
        VexStreamQueueState =
        VexStreamQueueState.Empty,
) :
    VexFamily<
        VexStreamQueueFamily,
        VexStreamQueueState,
        VexStreamQueueControl,
        VexStreamQueueEffect
    >(
        initialState =
            initialState,
    ) {

    override fun act(
        currentState:
            VexStreamQueueState,
        control:
            VexStreamQueueControl,
    ):
        VexFamily.Act<
            VexStreamQueueFamily,
            VexStreamQueueState,
            VexStreamQueueEffect
        >? =
        when (
            currentState
        ) {
            VexStreamQueueState.Empty ->
                null

            is VexStreamQueueState.NonEmpty ->
                when (
                    control
                ) {
                    VexStreamQueueControl.Previous ->
                        VexStreamQueueEffect.SeekPrevious
                            .takeIf {
                                currentState.previousAvailable
                            }

                    VexStreamQueueControl.Next ->
                        VexStreamQueueEffect.SeekNext
                            .takeIf {
                                currentState.nextAvailable
                            }

                    VexStreamQueueControl.Shuffle ->
                        VexStreamQueueEffect.ShuffleFuture
                            .takeIf {
                                currentState.shuffleAvailable
                            }

                    VexStreamQueueControl.Clear ->
                        VexStreamQueueEffect.ClearQueue
                            .takeIf {
                                currentState.clearAvailable
                            }
                }
                    ?.let { effect ->
                        stay(
                            begin(
                                effect,
                            ),
                        )
                    }
        }

    internal fun observeStore(
        queue:
            VexStreamPlayerStore.QueueSnapshot?,
    ):
        VexStreamQueueState =
        observe(
            queue
                .toVexStreamQueueState(),
        )

    fun projectionFor(
        state:
            VexStreamQueueState,
    ):
        VexStreamQueueProjection =
        vexStreamQueueProjectionFor(
            state,
        )

    val currentQueueProjection:
        VexStreamQueueProjection
        get() =
            projectionFor(
                currentState,
            )

    object Act {
        class Method(
            private val connection:
                VexStreamPlayerPartner.Connection.Request,
            private val effect:
                VexStreamQueueEffect,
        ) :
            VexFamily.Act.Method<
                VexStreamQueueFamily,
                Unit,
                Nothing
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Nothing
                > {
                when (
                    effect
                ) {
                    VexStreamQueueEffect.SeekPrevious ->
                        connection.previous()

                    VexStreamQueueEffect.SeekNext ->
                        connection.next()

                    VexStreamQueueEffect.ShuffleFuture ->
                        connection.shuffleFuture()

                    VexStreamQueueEffect.ClearQueue ->
                        connection.clearQueue()
                }

                return VexMethod.Result.Success(
                    Unit,
                )
            }
        }
    }
}

internal fun vexStreamQueueProjectionFor(
    state:
        VexStreamQueueState,
):
    VexStreamQueueProjection =
    VexStreamQueueProjection(
        state =
            state,
        previousAvailable =
            vexStreamQueueControlAvailable(
                state =
                    state,
                control =
                    VexStreamQueueControl.Previous,
            ),
        nextAvailable =
            vexStreamQueueControlAvailable(
                state =
                    state,
                control =
                    VexStreamQueueControl.Next,
            ),
        shuffleAvailable =
            vexStreamQueueControlAvailable(
                state =
                    state,
                control =
                    VexStreamQueueControl.Shuffle,
            ),
        clearAvailable =
            vexStreamQueueControlAvailable(
                state =
                    state,
                control =
                    VexStreamQueueControl.Clear,
            ),
    )

private fun vexStreamQueueControlAvailable(
    state:
        VexStreamQueueState,
    control:
        VexStreamQueueControl,
):
    Boolean =
    when (
        state
    ) {
        VexStreamQueueState.Empty ->
            false

        is VexStreamQueueState.NonEmpty ->
            when (
                control
            ) {
                VexStreamQueueControl.Previous ->
                    state.previousAvailable

                VexStreamQueueControl.Next ->
                    state.nextAvailable

                VexStreamQueueControl.Shuffle ->
                    state.shuffleAvailable

                VexStreamQueueControl.Clear ->
                    state.clearAvailable
            }
    }


internal fun activateVexStreamQueue(
    family:
        VexStreamQueueFamily,
    control:
        VexStreamQueueControl,
    connection:
        VexStreamPlayerPartner.Connection.Request,
    executionRoot:
        VexExecutionRoot,
):
    Job? {
    val act =
        family.activate(
            control,
        )
            ?: return null
    val effect =
        act.effect
            ?: return null
    val execution =
        act.launchWith(
            executionRoot,
        )
            ?: return null

    return execution.method(
        VexStreamQueueFamily.Act.Method(
            connection =
                connection,
            effect =
                effect,
        ),
    )
}

internal fun observeVexStreamQueuePlayerStore(
    family:
        VexStreamQueueFamily,
    store:
        VexStreamPlayerStore,
    executionRoot:
        VexExecutionRoot,
) {
    VexExecution.observe(
        executionRoot =
            executionRoot,
        owner =
            family,
        source =
            store::queueCurrents,
    ) { queue ->
        family.observeStore(
            queue,
        )
    }
}

internal fun VexStreamPlayerStore.QueueSnapshot?.toVexStreamQueueState():
    VexStreamQueueState =
    this
        ?.let { queue ->
            vexStreamQueueState(
                mediaItemCount =
                    queue.items
                        .mediaIds
                        .size,
                previousAvailable =
                    queue.navigation
                        .previousAvailable,
                nextAvailable =
                    queue.navigation
                        .nextAvailable,
                shuffleAvailable =
                    vexStreamQueueShuffleAvailable(
                        mediaItemCount =
                            queue.items
                                .mediaIds
                                .size,
                        currentIndex =
                            queue.navigation
                                .currentIndex,
                        canChangeMediaItems =
                            queue.navigation
                                .changeMediaItemsAvailable,
                    ),
                clearAvailable =
                    queue.items
                        .mediaIds
                        .isNotEmpty() &&
                        queue.navigation
                            .changeMediaItemsAvailable,
            )
        }
        ?: VexStreamQueueState.Empty

internal fun vexStreamQueueShuffleAvailable(
    mediaItemCount:
        Int,
    currentIndex:
        Int,
    canChangeMediaItems:
        Boolean,
):
    Boolean {
    require(
        mediaItemCount >=
            0,
    )

    if (
        !canChangeMediaItems ||
        mediaItemCount <=
            1
    ) {
        return false
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            mediaItemCount -
                1,
        )
    val futureStart =
        (lockedThrough + 1)
            .coerceIn(
                0,
                mediaItemCount,
            )

    return mediaItemCount -
        futureStart >
        1
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
