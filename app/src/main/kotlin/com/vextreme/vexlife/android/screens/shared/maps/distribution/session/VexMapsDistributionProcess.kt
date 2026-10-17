package com.vextreme.vexlife.android.screens.shared.maps.distribution.session

import android.content.Context
import com.vextreme.vexlife.android.data.maps.distribution.RoomVexMapsDistributionRepository
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsNavigationProcess
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsNavigationService
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsOfflineRoutingProcess
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionState
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteRequest
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteResult
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionOptimizer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPlan
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPreparationSnapshot
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionValhallaCostProvider
import java.util.IdentityHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-local Distribution session allocation.
 *
 * Navigation already has process-local ownership because its foreground service can outlive one
 * rendered Maps composition. Distribution sessions use the same minimum lifetime while remaining
 * separate from Navigation ownership.
 */
internal object VexMapsDistributionProcess {
    private val lock =
        Any()
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate,
        )
    private var productionSession:
        VexMapsDistributionSession? =
        null

    private val isolatedSessions =
        IdentityHashMap<
            VexMapsDistributionRepository,
            VexMapsDistributionSession
        >()

    fun session(
        context: Context,
        repository:
            VexMapsDistributionRepository,
    ): VexMapsDistributionSession =
        synchronized(lock) {
            if (
                repository is
                RoomVexMapsDistributionRepository
            ) {
                productionSession
                    ?: newSession(
                        context =
                            context,
                        repository =
                            repository,
                    ).also {
                        productionSession =
                            it
                    }
            } else {
                isolatedSessions[
                    repository
                ] ?: newSession(
                    context =
                        context,
                    repository =
                        repository,
                ).also {
                    isolatedSessions[
                        repository
                    ] =
                        it
                }
            }
        }

    private fun newSession(
        context: Context,
        repository:
            VexMapsDistributionRepository,
    ): VexMapsDistributionSession =
        VexMapsDistributionSession(
            repository =
                repository,
            routing =
                AndroidDistributionRouting(
                    context.applicationContext,
                ),
            navigation =
                AndroidDistributionNavigation(
                    context.applicationContext,
                ),
            executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Main.immediate,
                ),
            relayScope =
                scope,
        )
}

private class AndroidDistributionRouting(
    context: Context,
) :
    VexMapsDistributionRouting {
    private val appContext =
        context.applicationContext

    override suspend fun optimize(
        draft:
            com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft,
        preparation:
            VexMapsDistributionPreparationSnapshot,
        pack:
            VexMapsRegionPack,
    ): VexMapsDistributionPlan =
        VexMapsDistributionOptimizer(
            VexMapsDistributionValhallaCostProvider(
                context =
                    appContext,
                pack =
                    pack,
            ),
        ).optimize(
            draft =
                draft,
            preparation =
                preparation,
        )

    override suspend fun route(
        pack: VexMapsRegionPack,
        request: VexOfflineRouteRequest,
    ): VexOfflineRouteResult =
        VexMapsOfflineRoutingProcess
            .planner
            .route(
                context =
                    appContext,
                pack =
                    pack,
                request =
                    request,
            )
}

private class AndroidDistributionNavigation(
    private val context:
        Context,
) :
    VexMapsDistributionNavigationPort {
    override val states:
        StateFlow<VexNavigationSessionState>
        get() =
            VexMapsNavigationProcess
                .session
                .states

    override val current:
        VexNavigationSessionState
        get() =
            VexMapsNavigationProcess
                .session
                .current

    override fun prepare(
        routeResult:
            VexOfflineRouteResult,
        routingPack:
            VexMapsRegionPack,
    ) {
        VexMapsNavigationProcess
            .prepare(
                routeResult =
                    routeResult,
                routingPack =
                    routingPack,
            )
    }

    override fun startPrepared() {
        VexMapsNavigationService
            .startPrepared(
                context,
            )
    }

    override fun stop() {
        val serviceFailure =
            runCatching {
                VexMapsNavigationService
                    .stop(
                        context,
                    )
            }.exceptionOrNull()

        /*
         * Distribution convergence is driven by NavigationSession truth. Even if Android rejects the
         * best-effort service stop request, clear process-local Navigation truth so the old route
         * cannot remain a foreign ARRIVED/READY leg and block lawful recovery.
         */
        VexMapsNavigationProcess
            .stop()

        if (serviceFailure != null) {
            throw serviceFailure
        }
    }

    override fun dismissArrival() {
        VexMapsNavigationService
            .dismissArrivalNotification(
                context,
            )
    }
}

// [VXG RealForever]
