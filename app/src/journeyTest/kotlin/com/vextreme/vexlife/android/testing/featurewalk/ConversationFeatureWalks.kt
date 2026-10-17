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
                                    "Accepted Conversation product source has no registered installed-app doorway.",
                                releasePredicate =
                                    "A production Screen and ordinary app navigation reach the Family room.",
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
                                    "The isolated room renderer is proved, but no installed doorway can execute the same-session round trip.",
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
