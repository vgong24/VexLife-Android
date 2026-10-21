package com.vextreme.vexlife.android.conversation

import android.content.Context
import com.vextreme.vexlife.android.connectedservices.AndroidKeyStoreVexHomeDevicePossessionSigner
import com.vextreme.vexlife.android.connectedservices.SharedPreferencesVexPrivateHomeGatewayRouteStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Feature-facing read capability for the current Family Conversation projection.
 *
 * App composition exposes this contract rather than the private transport/client implementation.
 * It deliberately contains no Send, route, bearer, pairing, or Home-authority vocabulary.
 */
interface VexFamilyConversationReadRepository :
    VexConversationRepository {
    suspend fun refreshOnlyCurrentFamilyRoom():
        VexConversationThreadState
}

/**
 * Android-observed Conversation repository backed by the VCS-04 feature read client.
 *
 * The remote Home remains the canonical Family/Conversation writer. This repository owns only the
 * current Android projection needed by Hybrid Relay and presentation. [refresh] is explicit: polling,
 * reconnect, session lifetime, retry policy and app-graph scheduling remain with their rightful
 * Connected Services / application owners.
 */
internal class VexFamilyConversationConnectedServiceRepository(
    private val client:
        VexFamilyConversationConnectedServiceClient,
) : VexFamilyConversationReadRepository {
    private val mutableState =
        MutableStateFlow(
            VexConversationState.Empty,
        )

    override val state:
        StateFlow<VexConversationState> =
        mutableState.asStateFlow()

    /**
     * Refreshes one exact Family thread from current server truth.
     *
     * Existing thread ordering is preserved. Equal semantic refreshes retain the exact current State
     * instance so a network poll that learned nothing cannot manufacture an Android state change.
     */
    suspend fun refresh(
        request:
            VexFamilyConversationReadRequest,
    ): VexConversationThreadState =
        publish(
            client.readThread(
                request,
            ),
        )

    /**
     * Refreshes the only currently advertised Family room.
     *
     * This exists for the first installed-read slice only. The client rejects zero/multiple rooms,
     * so product growth toward a room picker cannot silently preserve "first room wins" behavior.
     */
    override suspend fun refreshOnlyCurrentFamilyRoom():
        VexConversationThreadState =
        publish(
            client.readOnlyCurrentFamilyRoom(),
        )

    private fun publish(
        refreshed:
            VexConversationThreadState,
    ): VexConversationThreadState {
        val current =
            mutableState.value
        val existingIndex =
            current.threads
                .indexOfFirst { thread ->
                    thread.threadRef ==
                        refreshed.threadRef
                }

        val nextThreads =
            if (existingIndex < 0) {
                current.threads +
                    refreshed
            } else {
                current.threads
                    .mapIndexed { index, thread ->
                        if (index == existingIndex) {
                            refreshed
                        } else {
                            thread
                        }
                    }
            }

        val next =
            current.copy(
                threads =
                    nextThreads,
            )

        if (next != current) {
            mutableState.value =
                next
        }

        return refreshed
    }

    companion object {
        /**
         * Production Android composition.
         *
         * The route store carries reachability only. [VexFamilyConversationPrivateGatewayRuntimePort]
         * reconstructs possession/session authentication from AndroidKeyStore for the current route.
         */
        fun production(
            context:
                Context,
        ): VexFamilyConversationConnectedServiceRepository {
            val routeStore =
                SharedPreferencesVexPrivateHomeGatewayRouteStore(
                    context,
                )
            val signer =
                AndroidKeyStoreVexHomeDevicePossessionSigner(
                    context,
                )

            return VexFamilyConversationConnectedServiceRepository(
                client =
                    VexFamilyConversationConnectedServiceClient(
                        VexFamilyConversationPrivateGatewayRuntimePort(
                            routeStore =
                                routeStore,
                            signer =
                                signer,
                        ),
                    ),
            )
        }
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][READ-REPOSITORY]
