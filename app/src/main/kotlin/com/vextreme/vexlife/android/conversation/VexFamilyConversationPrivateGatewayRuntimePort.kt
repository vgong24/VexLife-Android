package com.vextreme.vexlife.android.conversation

import com.vextreme.vexlife.android.connectedservices.HttpsVexPrivateHomeGatewayHttpTransport
import com.vextreme.vexlife.android.connectedservices.VexHomeDevicePossessionSigner
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayConnection
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayEndpoint
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayException
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayFailureKind
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayHttpTransport
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayRouteStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Session/runtime carriage from the current private route into the existing Family Conversation port.
 *
 * Route persistence remains reachability only. A new route gets a new
 * [VexPrivateHomeGatewayConnection], which must establish possession/session authentication before
 * the first feature request. No bearer or Home authority is persisted here.
 */
internal class VexFamilyConversationPrivateGatewayRuntimePort(
    private val routeStore:
        VexPrivateHomeGatewayRouteStore,
    private val signer:
        VexHomeDevicePossessionSigner,
    private val transportFactory:
        (VexPrivateHomeGatewayEndpoint) ->
            VexPrivateHomeGatewayHttpTransport = { endpoint ->
                HttpsVexPrivateHomeGatewayHttpTransport(
                    endpoint,
                )
            },
) : VexFamilyConversationConnectedServicePort {
    private data class BoundPort(
        val httpsOrigin: String,
        val port:
            VexFamilyConversationPrivateGatewayPort,
    )

    private val bindingMutex =
        Mutex()

    private var boundPort:
        BoundPort? = null

    override suspend fun bootstrap():
        VexFamilyConversationServerBootstrap =
        currentPort()
            .bootstrap()

    override suspend fun readThreadHistory(
        intent:
            VexFamilyConversationThreadHistoryIntent,
    ): VexFamilyConversationServerThreadHistory =
        currentPort()
            .readThreadHistory(
                intent,
            )

    private suspend fun currentPort():
        VexFamilyConversationPrivateGatewayPort {
        val endpoint =
            routeStore.currentEndpoint()
                ?: throw VexPrivateHomeGatewayException(
                    kind =
                        VexPrivateHomeGatewayFailureKind
                            .TransportUnavailable,
                    serverCode =
                        "PRIVATE_ROUTE_UNAVAILABLE",
                )

        return bindingMutex.withLock {
            boundPort
                ?.takeIf { current ->
                    current.httpsOrigin ==
                        endpoint.httpsOrigin
                }
                ?.port
                ?: VexFamilyConversationPrivateGatewayPort(
                    VexPrivateHomeGatewayConnection(
                        transport =
                            transportFactory(
                                endpoint,
                            ),
                        signer =
                            signer,
                    ),
                ).also { port ->
                    boundPort =
                        BoundPort(
                            httpsOrigin =
                                endpoint.httpsOrigin,
                            port =
                                port,
                        )
                }
        }
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][PRIVATE-ROUTE-RUNTIME]
