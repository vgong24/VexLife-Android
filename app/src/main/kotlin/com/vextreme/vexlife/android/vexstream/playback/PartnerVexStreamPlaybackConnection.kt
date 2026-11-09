package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.Job

/**
 * Playback semantic owner -> Player Partner Effect realization.
 *
 * Provider observation is independent and returns through the process Player Partner Store.
 * Accepted Act execution meaning remains intact through [VexFamily.Act.launchWith]: Approach,
 * RelationshipId and the live Family execution owner are not reconstructed or discarded here.
 */
internal class PartnerVexStreamPlaybackConnection(
    private val family:
        VexStreamPlaybackFamily,
    private val executionRoot:
        VexExecutionRoot,
    private val effectConnection:
        VexStreamPlaybackEffectConnection,
) :
    VexStreamPlaybackConnection {
    override fun activate(
        control:
            VexStreamPlaybackControl,
    ):
        Job? {
        val act =
            family
                .activatePlayback(
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
            VexStreamPlaybackEffectMethod(
                effect =
                    effect,
                effectConnection =
                    effectConnection,
            ),
        )
    }
}

private class VexStreamPlaybackEffectMethod(
    private val effect:
        VexStreamPlaybackEffect,
    private val effectConnection:
        VexStreamPlaybackEffectConnection,
) :
    VexFamily.Act.Method<
        VexStreamPlaybackFamily,
        Unit,
        Nothing
    > {
    override suspend fun invoke():
        VexMethod.Result<
            Unit,
            Nothing
        > {
        effectConnection.realize(
            effect,
        )

        return VexMethod.Result.Success(
            Unit,
        )
    }
}


// [VXG RealForever][VEXSTREAM][PLAYBACK][PLAYER-PARTNER]
