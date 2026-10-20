package com.vextreme.vexlife.android.behavior.example.maps

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily
import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow
import com.vextreme.vexlife.android.behavior.vexBehaviorBlueprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure pressure example only.
 *
 * This is NOT a VexMaps production implementation. It exists so reviewers can see how the generic
 * behavior grammar handles location permission, admitted Finding state, several eventual provider
 * outcomes, and a concrete latest-request gate without teaching VexPort to own async currentness.
 */
internal sealed interface MapsLocationFamily :
    VexBehaviorFamily<
        MapsLocationFamily
    >

internal enum class MapsLocationAcquisition {
    Idle,
    Finding,
}

internal data class MapsLocationFix(
    val latitudeE6: Int,
    val longitudeE6: Int,
)

internal sealed interface MapsLocationState :
    VexBehaviorFamily.State<
        MapsLocationFamily
    > {
    data object NoPrecisePermission :
        MapsLocationState

    data class PrecisePermission(
        val acquisition:
            MapsLocationAcquisition,
        val currentFix:
            MapsLocationFix? = null,
        val requestGeneration:
            Long? = null,
    ) : MapsLocationState

    data object ServicesDisabled :
        MapsLocationState

    data class Failed(
        val reason: String,
    ) : MapsLocationState
}

internal sealed interface MapsLocationAct :
    VexBehaviorFamily.Act<
        MapsLocationFamily
    > {
    data object Locate :
        MapsLocationAct
}

internal sealed interface MapsLocationEffect :
    VexBehaviorFamily.Effect<
        MapsLocationFamily
    > {
    data object RequestCurrentLocation :
        MapsLocationEffect
}

internal enum class MapsLocationControl :
    VexBehaviorFamily.Control<
        MapsLocationFamily
    > {
    UsePreciseLocation,
}

private fun mapsLocationBehavior() =
    vexBehaviorBlueprint<
        MapsLocationFamily,
        MapsLocationState,
        MapsLocationAct,
        MapsLocationEffect
    >(
        behaviorRef =
            "example.maps.location",
    ) {
        state<
            MapsLocationState.NoPrecisePermission
        >()

        state<
            MapsLocationState.PrecisePermission
        > {
            on(
                act =
                    MapsLocationAct.Locate,
                effect =
                    MapsLocationEffect
                        .RequestCurrentLocation,
                conditionName =
                    "not-already-finding",
            ) { current ->
                current.acquisition !=
                    MapsLocationAcquisition
                        .Finding
            }.stay()
        }

        state<
            MapsLocationState.ServicesDisabled
        >()

        state<
            MapsLocationState.Failed
        > {
            on(
                MapsLocationAct.Locate,
                MapsLocationEffect
                    .RequestCurrentLocation,
            ).admit<
                MapsLocationState
                    .PrecisePermission
            >()
        }
    }

private fun mapsLocationControls(
    behavior:
        com.vextreme.vexlife.android.behavior
            .VexBehaviorBlueprint<
                MapsLocationFamily,
                MapsLocationState,
                MapsLocationAct,
                MapsLocationEffect
            >,
) =
    behavior.controls<
        MapsLocationControl
    > {
        control(
            MapsLocationControl
                .UsePreciseLocation,
            MapsLocationAct.Locate,
        )
    }

/**
 * The concrete Connection owns request-generation admission.
 *
 * VexPort re-resolves Control meaning at activation; [providerResult] separately protects against
 * stale asynchronous completion after activation.
 */
internal class MapsLocationConnectionExample(
    initialState: MapsLocationState,
) :
    VexPortFlow<
        MapsLocationFamily,
        MapsLocationState,
        MapsLocationControl,
        MapsLocationAct
    > {
    private val behavior =
        mapsLocationBehavior()

    private val controls =
        mapsLocationControls(
            behavior,
        )

    private var state =
        initialState

    private var latestGeneration =
        0L

    private val mutableProjections =
        MutableStateFlow(
            controls.projection(
                state,
            ),
        )

    override val projections:
        StateFlow<
            VexBehaviorProjection<
                MapsLocationFamily,
                MapsLocationState,
                MapsLocationControl,
                MapsLocationAct
            >
        > =
        mutableProjections.asStateFlow()

    var requestedGeneration:
        Long? =
        null
        private set

    override fun activate(
        control: MapsLocationControl,
    ) {
        val act =
            controls.resolve(
                current = state,
                control = control,
            )?.act
                ?: return

        val relationship =
            behavior.resolve(
                current = state,
                act = act,
            )
                ?: return

        check(
            relationship.effect ==
                MapsLocationEffect
                    .RequestCurrentLocation,
        )

        val generation =
            ++latestGeneration

        val admitted =
            MapsLocationState
                .PrecisePermission(
                    acquisition =
                        MapsLocationAcquisition
                            .Finding,
                    currentFix =
                        (
                            state as?
                                MapsLocationState
                                    .PrecisePermission
                        )?.currentFix,
                    requestGeneration =
                        generation,
                )

        check(
            relationship.admittedStateKind
                .isInstance(
                    admitted,
                ),
        )

        requestedGeneration =
            generation
        observe(
            admitted,
        )
    }

    /**
     * Concrete async-currentness gate.
     *
     * The behavior graph does not own this. Old provider completions are simply not admitted.
     */
    fun providerResult(
        generation: Long,
        observed:
            MapsLocationState,
    ): Boolean {
        if (
            generation !=
            latestGeneration
        ) {
            return false
        }

        observe(
            observed,
        )
        return true
    }

    private fun observe(
        observed:
            MapsLocationState,
    ) {
        state =
            observed
        mutableProjections.value =
            controls.projection(
                observed,
            )
    }
}

// [VXG RealForever]
