package com.vextreme.vexlife.android.screens.shared.conversation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.conversation.VexConversationChannelRef
import com.vextreme.vexlife.android.conversation.VexConversationCompanionAvailability
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.VexConversationLabFixture
import com.vextreme.vexlife.android.conversation.VexConversationMessage
import com.vextreme.vexlife.android.conversation.VexConversationMessageRef
import com.vextreme.vexlife.android.conversation.VexConversationParticipantRef
import com.vextreme.vexlife.android.conversation.VexConversationSurfaceProjection
import com.vextreme.vexlife.android.conversation.VexConversationThreadState
import com.vextreme.vexlife.android.conversation.VexFamilyConversationMessageBinding
import com.vextreme.vexlife.android.conversation.VexFamilyConversationThreadBinding
import com.vextreme.vexlife.android.conversation.projectVexConversationSurface
import com.vextreme.vexlife.android.conversation.projectVexFamilyConversationSendAvailability
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSurfaceSendAvailability
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Semantic visual case: conversation.family-room.baseline
 *
 * Starts from already-projected Conversation meaning. Domain/currentness semantics remain covered by
 * JVM tests; this golden asks whether the first useful Family-room state is visually understandable.
 */
@PreviewTest
@Preview(
    name = "Conversation · Family room · baseline",
    widthDp = 412,
    heightDp = 760,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun conversationFamilyRoomBaselineGolden() {
    ConversationGolden(
        projection =
            conversationGoldenProjection(
                audienceRefs =
                    listOf(
                        victorRef,
                        mayaRef,
                        vexRef,
                    ),
                messages =
                    baselineMessages(),
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            ),
        developerEnvironmentLabel =
            "Conversation Lab active · Auto mirror: Maya",
        draft = "Are you coming?",
    )
}

/**
 * Semantic visual case: conversation.family-room.multi-human
 *
 * Pressures attribution, participant density, message wrapping and vertical history space without
 * changing the underlying Conversation ownership model.
 */
@PreviewTest
@Preview(
    name = "Conversation · Family room · multi-human",
    widthDp = 412,
    heightDp = 840,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun conversationFamilyRoomMultiHumanGolden() {
    ConversationGolden(
        projection =
            conversationGoldenProjection(
                audienceRefs =
                    listOf(
                        victorRef,
                        mayaRef,
                        vexRef,
                        alexRef,
                    ),
                messages =
                    multiHumanMessages(),
                homeAvailability =
                    VexConversationHomeAvailability.Available,
            ),
        developerEnvironmentLabel =
            "Conversation Lab active · 4 participants",
        draft =
            "Can everyone still read the conversation clearly?",
    )
}

/**
 * Semantic visual case: conversation.family-room.home-unavailable
 *
 * Keeps existing room/history visible while Family Send is not presently eligible. The golden makes
 * the disabled affordance reviewable without claiming provider rejection or production Screen
 * behavior.
 */
@PreviewTest
@Preview(
    name = "Conversation · Family room · Home unavailable",
    widthDp = 412,
    heightDp = 760,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun conversationFamilyRoomHomeUnavailableGolden() {
    ConversationGolden(
        projection =
            conversationGoldenProjection(
                audienceRefs =
                    listOf(
                        victorRef,
                        mayaRef,
                        vexRef,
                    ),
                messages =
                    baselineMessages(),
                homeAvailability =
                    VexConversationHomeAvailability.Unavailable,
            ),
        developerEnvironmentLabel =
            "Conversation Lab active · Home unavailable",
        draft =
            "This draft should remain visible while Send is unavailable.",
    )
}

@Composable
private fun ConversationGolden(
    projection:
        VexConversationSurfaceProjection,
    developerEnvironmentLabel: String,
    draft: String,
) {
    val sendAvailability =
        projectVexFamilyConversationSendAvailability(
            projection,
        )
    val canSend =
        sendAvailability is
            VexFamilyConversationSurfaceSendAvailability.Ready &&
            draft.isNotBlank()

    VexLifeTheme(
        darkTheme = true,
    ) {
        Surface {
            VexConversationRoom(
                projection = projection,
                semanticRootRef =
                    "golden.conversation.family-room",
                roomTitle = "Family",
                participantLabel = { participantRef ->
                    VexConversationLabFixture
                        .requireIdentity(
                            participantRef,
                        )
                        .displayName
                },
                emptyMessagesLabel = "No messages yet.",
                authenticatedSpeakerSuffix = "you",
                developerEnvironmentLabel =
                    developerEnvironmentLabel,
                composerContent = {
                    PreviewConversationComposer(
                        semanticRootRef =
                            "golden.conversation.family-room",
                        label = "Type a message",
                        sendLabel = "Send",
                        draft = draft,
                        enabled = canSend,
                        onDraftChange = {},
                        onSend = {},
                    )
                },
            )
        }
    }
}

private fun conversationGoldenProjection(
    audienceRefs:
        List<VexConversationParticipantRef>,
    messages:
        List<VexConversationMessage>,
    homeAvailability:
        VexConversationHomeAvailability,
): VexConversationSurfaceProjection =
    projectVexConversationSurface(
        thread =
            VexConversationThreadState(
                threadRef =
                    VexConversationLabFixture.threadRef,
                currentChannelRef =
                    channelRef,
                knownParticipants =
                    VexConversationLabFixture
                        .all
                        .map { identity ->
                            identity.participant
                        },
                currentAudienceRefs =
                    audienceRefs,
                messages =
                    messages,
                familyBinding =
                    VexFamilyConversationThreadBinding(
                        spaceRef =
                            VexConversationLabFixture.spaceRef,
                        currentMembershipGeneration =
                            1L,
                        segmentRefs =
                            listOf(
                                channelRef,
                            ),
                    ),
            ),
        authenticatedParticipantRef =
            victorRef,
        homeAvailability =
            homeAvailability,
        companionAvailabilityByParticipantRef =
            mapOf(
                vexRef to
                    VexConversationCompanionAvailability.Available,
            ),
    )

private fun baselineMessages():
    List<VexConversationMessage> {
    val audience =
        listOf(
            victorRef,
            mayaRef,
            vexRef,
        )

    return listOf(
        goldenMessage(
            sequence = 0L,
            speakerRef = victorRef,
            recipientRefs =
                listOf(
                    mayaRef,
                ),
            witnessRefs = audience,
            content =
                "Hey, are we still going?",
        ),
        goldenMessage(
            sequence = 1L,
            speakerRef = mayaRef,
            recipientRefs =
                listOf(
                    victorRef,
                ),
            witnessRefs = audience,
            content =
                "Yep, leaving now.",
        ),
        goldenMessage(
            sequence = 2L,
            speakerRef = vexRef,
            recipientRefs =
                listOf(
                    victorRef,
                    mayaRef,
                ),
            witnessRefs = audience,
            content =
                "I can keep track of the plan.",
        ),
    )
}

private fun multiHumanMessages():
    List<VexConversationMessage> {
    val audience =
        listOf(
            victorRef,
            mayaRef,
            vexRef,
            alexRef,
        )

    return listOf(
        goldenMessage(
            sequence = 0L,
            speakerRef = victorRef,
            recipientRefs =
                listOf(
                    mayaRef,
                    alexRef,
                ),
            witnessRefs = audience,
            content =
                "Dinner first, then should we stop for groceries on the way back?",
        ),
        goldenMessage(
            sequence = 1L,
            speakerRef = mayaRef,
            recipientRefs =
                listOf(
                    victorRef,
                    alexRef,
                ),
            witnessRefs = audience,
            content =
                "Works for me. I can grab the list before we leave.",
        ),
        goldenMessage(
            sequence = 2L,
            speakerRef = alexRef,
            recipientRefs =
                listOf(
                    victorRef,
                    mayaRef,
                ),
            witnessRefs = audience,
            content =
                "I may be ten minutes behind, so don't wait if everyone else is ready.",
        ),
        goldenMessage(
            sequence = 3L,
            speakerRef = vexRef,
            recipientRefs =
                listOf(
                    victorRef,
                    mayaRef,
                    alexRef,
                ),
            witnessRefs = audience,
            content =
                "I can keep the shared plan current while everyone is moving.",
        ),
        goldenMessage(
            sequence = 4L,
            speakerRef = victorRef,
            recipientRefs =
                listOf(
                    mayaRef,
                    alexRef,
                ),
            witnessRefs = audience,
            content =
                "Perfect. I'll send the final place once I confirm parking.",
        ),
    )
}

private fun goldenMessage(
    sequence: Long,
    speakerRef:
        VexConversationParticipantRef,
    recipientRefs:
        List<VexConversationParticipantRef>,
    witnessRefs:
        List<VexConversationParticipantRef>,
    content: String,
): VexConversationMessage =
    VexConversationMessage(
        messageRef =
            VexConversationMessageRef(
                "message.golden." +
                    sequence,
            ),
        threadRef =
            VexConversationLabFixture.threadRef,
        channelRef =
            channelRef,
        sequence =
            sequence,
        speakerRef =
            speakerRef,
        recipientRefs =
            recipientRefs,
        witnessRefs =
            witnessRefs,
        content =
            content,
        familyBinding =
            VexFamilyConversationMessageBinding(
                spaceRef =
                    VexConversationLabFixture.spaceRef,
                membershipGeneration =
                    1L,
            ),
    )

private val channelRef:
    VexConversationChannelRef
    get() =
        VexConversationLabFixture
            .channelRef(1L)

private val victorRef:
    VexConversationParticipantRef
    get() =
        VexConversationLabFixture
            .Victor
            .participant
            .participantRef

private val mayaRef:
    VexConversationParticipantRef
    get() =
        VexConversationLabFixture
            .Maya
            .participant
            .participantRef

private val alexRef:
    VexConversationParticipantRef
    get() =
        VexConversationLabFixture
            .Alex
            .participant
            .participantRef

private val vexRef:
    VexConversationParticipantRef
    get() =
        VexConversationLabFixture
            .Vex
            .participant
            .participantRef

// [VXG RealForever]
