package com.vextreme.vexlife.android.conversation

import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayConnection
import org.json.JSONArray
import org.json.JSONObject

/**
 * Conversation-specific realization over the accepted private Home gateway.
 *
 * Session establishment, bearer material and HTTPS route mechanics remain below this class in
 * [VexPrivateHomeGatewayConnection]. Conversation owns only the accepted bootstrap/history wire
 * vocabulary and projection into the already-existing server DTOs.
 */
internal class VexFamilyConversationPrivateGatewayPort(
    private val gateway:
        VexPrivateHomeGatewayConnection,
) : VexFamilyConversationConnectedServicePort {
    override suspend fun bootstrap():
        VexFamilyConversationServerBootstrap {
        val response =
            JSONObject(
                gateway.authenticatedGetJson(
                    FAMILY_BOOTSTRAP_PATH,
                ),
            )
        val state =
            response.getString(
                "state",
            )
        require(
            state == "CURRENT" ||
                state == "EMPTY",
        ) {
            "Family bootstrap state is unsupported"
        }
        require(
            response.getString(
                "truthClass",
            ) == "CURRENT_LIVE_FAMILY",
        ) {
            "Family bootstrap truth class is unsupported"
        }
        val currentPrincipalRef =
            response.getString(
                "currentPrincipalRef",
            )
        require(
            currentPrincipalRef.isNotBlank(),
        ) {
            "Family bootstrap current principal must be non-blank"
        }
        val roomsJson =
            response.getJSONArray(
                "rooms",
            )
        val rooms =
            buildList {
                for (index in 0 until roomsJson.length()) {
                    add(
                        parseRoom(
                            roomsJson.getJSONObject(
                                index,
                            ),
                        ),
                    )
                }
            }

        return VexFamilyConversationServerBootstrap(
            currentPrincipalRef =
                currentPrincipalRef,
            rooms =
                rooms,
        )
    }

    override suspend fun readThreadHistory(
        intent: VexFamilyConversationThreadHistoryIntent,
    ): VexFamilyConversationServerThreadHistory {
        val body =
            JSONObject()
                .put(
                    "operation",
                    "READ_THREAD_HISTORY",
                )
                .put(
                    "intent",
                    JSONObject()
                        .put(
                            "spaceRef",
                            intent.spaceRef,
                        )
                        .put(
                            "channelRef",
                            intent.channelRef,
                        )
                        .put(
                            "expectedMembershipGeneration",
                            intent.expectedMembershipGeneration,
                        ),
                )
                .toString()
        val response =
            JSONObject(
                gateway.authenticatedPostJson(
                    absolutePath =
                        FAMILY_CONVERSATION_PATH,
                    body =
                        body,
                ),
            )
        require(
            response.getString(
                "state",
            ) == "CURRENT",
        ) {
            "Family thread history is not current"
        }

        val segmentRefs =
            response
                .getJSONArray(
                    "segmentRefs",
                )
                .stringValues()
        val messagesJson =
            response.getJSONArray(
                "messages",
            )
        val messages =
            buildList {
                for (index in 0 until messagesJson.length()) {
                    add(
                        parseMessage(
                            messagesJson.getJSONObject(
                                index,
                            ),
                        ),
                    )
                }
            }

        return VexFamilyConversationServerThreadHistory(
            spaceRef =
                response.getString(
                    "spaceRef",
                ),
            threadRef =
                response.getString(
                    "threadRef",
                ),
            currentChannelRef =
                response.getString(
                    "currentChannelRef",
                ),
            membershipGeneration =
                response.getLong(
                    "membershipGeneration",
                ),
            segmentRefs =
                segmentRefs,
            messages =
                messages,
            truncated =
                response.getBoolean(
                    "truncated",
                ),
        )
    }

    private fun parseRoom(
        room: JSONObject,
    ): VexFamilyConversationServerRoom {
        val audienceJson =
            room.getJSONArray(
                "audience",
            )
        val audiencePrincipalRefs =
            buildList {
                for (index in 0 until audienceJson.length()) {
                    val principalRef =
                        audienceJson
                            .getJSONObject(
                                index,
                            )
                            .getString(
                                "principalRef",
                            )
                    require(
                        principalRef.isNotBlank(),
                    ) {
                        "Family bootstrap audience principal must be non-blank"
                    }
                    add(
                        principalRef,
                    )
                }
            }

        return VexFamilyConversationServerRoom(
            spaceRef =
                room.getString(
                    "spaceRef",
                ),
            threadRef =
                room.getString(
                    "threadRef",
                ),
            channelRef =
                room.getString(
                    "channelRef",
                ),
            membershipGeneration =
                room.getLong(
                    "membershipGeneration",
                ),
            audiencePrincipalRefs =
                audiencePrincipalRefs,
            familyCompanionLineageRef =
                room.getString(
                    "familyCompanionLineageRef",
                ),
            familyCompanionIncluded =
                room.getBoolean(
                    "familyCompanionIncluded",
                ),
        )
    }

    private fun parseMessage(
        message: JSONObject,
    ): VexFamilyConversationServerMessage =
        VexFamilyConversationServerMessage(
            messageRef =
                message.getString(
                    "messageRef",
                ),
            spaceRef =
                message.getString(
                    "spaceRef",
                ),
            threadRef =
                message.getString(
                    "threadRef",
                ),
            channelRef =
                message.getString(
                    "channelRef",
                ),
            speakerRef =
                message.getString(
                    "speakerRef",
                ),
            recipientRefs =
                message
                    .getJSONArray(
                        "recipientRefs",
                    )
                    .stringValues(),
            witnessRefs =
                message
                    .getJSONArray(
                        "witnessRefs",
                    )
                    .stringValues(),
            membershipGeneration =
                message.getLong(
                    "membershipGeneration",
                ),
            sequence =
                message.getLong(
                    "sequence",
                ),
            content =
                message.getString(
                    "content",
                ),
        )

    private fun JSONArray.stringValues(): List<String> =
        buildList {
            for (index in 0 until length()) {
                val value =
                    getString(
                        index,
                    )
                require(
                    value.isNotBlank(),
                ) {
                    "Family server ref arrays must not contain blank values"
                }
                add(
                    value,
                )
            }
        }

    private companion object {
        const val FAMILY_BOOTSTRAP_PATH =
            "/v1/home/family/bootstrap"
        const val FAMILY_CONVERSATION_PATH =
            "/v1/home/family/conversation"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][PRIVATE-GATEWAY-PORT]