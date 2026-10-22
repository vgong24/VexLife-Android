package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
) : VexFamilyConversationRepository {
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
    suspend fun refreshSingleCurrentRoom():
        VexConversationThreadState =
        publish(
            client.readSingleCurrentRoom(),
        )

    suspend fun refresh(
        request:
            VexFamilyConversationReadRequest,
    ): VexConversationThreadState =
        publish(
            client.readThread(
                request,
            ),
        )

    override suspend fun send(
        request: VexFamilyConversationSendRequest,
    ): VexFamilyConversationRepositoryResult {
        val currentThread =
            mutableState.value
                .thread(
                    request.threadRef,
                )
                ?: return VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection.THREAD_UNKNOWN,
                )
        val family =
            currentThread.familyBinding
                ?: return VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection.THREAD_NOT_FAMILY_BOUND,
                )
        if (
            family.spaceRef != request.spaceRef ||
            currentThread.currentChannelRef != request.expectedChannelRef ||
            family.currentMembershipGeneration != request.expectedMembershipGeneration
        ) {
            return VexFamilyConversationRepositoryResult.Rejected(
                VexFamilyConversationRepositoryRejection.CURRENTNESS_CHANGED,
            )
        }

        return when (
            val result = client.send(
                request,
            )
        ) {
            is VexFamilyConversationConnectedServiceSendResult.Accepted -> {
                publish(
                    result.refreshedThread,
                )
                VexFamilyConversationRepositoryResult.Accepted(
                    result.messageRef,
                )
            }

            is VexFamilyConversationConnectedServiceSendResult.Rejected ->
                VexFamilyConversationRepositoryResult.Rejected(
                    result.reason,
                )
        }
    }

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
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][READ-SEND-REPOSITORY]
