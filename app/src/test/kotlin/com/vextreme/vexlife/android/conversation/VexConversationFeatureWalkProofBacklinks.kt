package com.vextreme.vexlife.android.conversation

import com.vextreme.vexlife.android.testing.featurewalk.ConversationFeatureWalkRefs
import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkProofBacklink

internal val vexConversationFeatureWalkProofBacklinks =
    listOf(
        VexFeatureWalkProofBacklink(
            checkpointRef =
                ConversationFeatureWalkRefs
                    .VICTOR_SEND_ADMITTED,
            testClass =
                VexConversationLabTest::class.java,
            testMethodName =
                "victorSendStaysOnRepositoryContractAndMirrorArrivesOnlyAfterAcceptance",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                ConversationFeatureWalkRefs
                    .MAYA_PROVIDER_INGRESS,
            testClass =
                VexConversationLabTest::class.java,
            testMethodName =
                "remoteSpeechPublishesProviderTruthThroughConversationRepository",
        ),
    )

// [VXG RealForever]
