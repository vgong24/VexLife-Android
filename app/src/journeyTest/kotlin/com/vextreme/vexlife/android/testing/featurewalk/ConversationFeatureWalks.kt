package com.vextreme.vexlife.android.testing.featurewalk

import com.vextreme.vexlife.android.conversation.VexConversationDocs

object ConversationFeatureWalkRefs {
    const val FEATURE: String =
        VexConversationDocs.FEATURE_REF
    const val SIMULATED_FAMILY: String =
        "$FEATURE/W01"
    const val ROUND_TRIP: String =
        "$SIMULATED_FAMILY/C01"
    const val INSTALLED_DOORWAY: String =
        "$ROUND_TRIP/P01"
    const val VICTOR_SEND_ADMITTED: String =
        "$ROUND_TRIP/P02"
    const val MAYA_PROVIDER_INGRESS: String =
        "$ROUND_TRIP/P03"
    const val INSTALLED_ROOM_REACTS: String =
        "$ROUND_TRIP/P04"
}

internal object ConversationFeatureWalks {
    val roundTrip =
        VexFeatureWalkCase(
            ref =
                ConversationFeatureWalkRefs.ROUND_TRIP,
            key =
                "round-trip",
            title =
                "Victor sends and Maya responds in the simulated Family room",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            ConversationFeatureWalkRefs
                                .INSTALLED_DOORWAY,
                        key =
                            "installed-doorway",
                        title =
                            "The installed app exposes a human doorway into the Conversation Family room",
                        hold =
                            VexFeatureWalkHold(
                                reason =
                                    "The installed doorway is registered (Home -> VexFamilyConversationScreen in " +
                                        "AppScreens, reading Connected Services Family truth), but no lived private " +
                                        "route, bounded Home membership or bound Journey has proved it in an " +
                                        "installed session.",
                                releasePredicate =
                                    "A fresh installed app process reaches the Family room from Home over a proven " +
                                        "private route with bounded Home membership, and a bound Conversation " +
                                        "Journey observes the canonical room.",
                            ),
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            ConversationFeatureWalkRefs
                                .VICTOR_SEND_ADMITTED,
                        key =
                            "victor-send-admitted",
                        title =
                            "Victor's message is admitted through the Conversation repository contract",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            ConversationFeatureWalkRefs
                                .MAYA_PROVIDER_INGRESS,
                        key =
                            "maya-provider-ingress",
                        title =
                            "Maya's simulated provider speech enters canonical Conversation truth",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            ConversationFeatureWalkRefs
                                .INSTALLED_ROOM_REACTS,
                        key =
                            "installed-room-reacts",
                        title =
                            "The reached installed room renders both sides of the round trip",
                        hold =
                            VexFeatureWalkHold(
                                reason =
                                    "The isolated room renderer and the installed Send source exist, but the round " +
                                        "trip has not been observed in one installed session: Conversation Journey " +
                                        "integration is held because the canonical test graph cannot yet inject " +
                                        "Conversation without overlap.",
                                releasePredicate =
                                    "A bound Conversation Journey reaches the room and observes the round trip in one installed session.",
                            ),
                    ),
                ),
        )

    val feature =
        VexFeatureWalkFeature(
            featureRef =
                VexConversationDocs.FEATURE_REF,
            currentName =
                VexConversationDocs.CURRENT_FEATURE_NAME,
            walks =
                listOf(
                    VexFeatureWalk(
                        ref =
                            ConversationFeatureWalkRefs
                                .SIMULATED_FAMILY,
                        key =
                            "simulated-family",
                        title =
                            "Simulated Family Conversation",
                        cases =
                            listOf(
                                roundTrip,
                            ),
                    ),
                ),
        )
}

// [VXG RealForever]
