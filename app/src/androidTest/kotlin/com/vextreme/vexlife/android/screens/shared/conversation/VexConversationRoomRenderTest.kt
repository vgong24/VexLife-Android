package com.vextreme.vexlife.android.screens.shared.conversation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.vextreme.vexlife.android.conversation.InMemoryVexFamilyConversationRepository
import com.vextreme.vexlife.android.conversation.VexConversationCompanionAvailability
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.VexConversationHybridRelay
import com.vextreme.vexlife.android.conversation.VexConversationIdempotencyKey
import com.vextreme.vexlife.android.conversation.VexConversationLab
import com.vextreme.vexlife.android.conversation.VexConversationLabFixture
import com.vextreme.vexlife.android.conversation.VexFamilyConversationRepositoryResult
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSendRequest
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSurfaceSendAvailability
import com.vextreme.vexlife.android.conversation.projectVexFamilyConversationSendAvailability
import com.vextreme.vexlife.android.ui.VexLifeTheme
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Rule
import org.junit.Test

/**
 * Feature-local Compose usability practicum for Conversation.
 *
 * This deliberately does not register a production Screen or duplicate the app action architecture.
 * It exercises the accepted Lab + Hybrid + generic room on Android so visual/interaction problems
 * can be found before the shared app/session integration paths become available.
 */
class VexConversationRoomRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun familyRoomSupportsReadableRoundTripAndHybridAvailabilityPressure() {
        val repository =
            InMemoryVexFamilyConversationRepository(
                admittedSpeakerRef =
                    VexConversationLabFixture
                        .Victor
                        .participant
                        .participantRef,
            )
        val lab =
            VexConversationLab(
                repository,
            ).also { conversationLab ->
                conversationLab.seedFamilyRoom()
                conversationLab.setAutoMirror(true)
                conversationLab.publishRemoteMessage(
                    content = "Yep, leaving now.",
                    speakerRef =
                        VexConversationLabFixture
                            .Maya
                            .participant
                            .participantRef,
                )
            }
        val relay =
            VexConversationHybridRelay(
                conversationRepository =
                    repository,
                conversationLab =
                    lab,
            ).also { hybrid ->
                hybrid.selectThread(
                    VexConversationLabFixture.threadRef,
                )
                hybrid.authenticatedParticipantObserved(
                    VexConversationLabFixture
                        .Victor
                        .participant
                        .participantRef,
                )
                hybrid.homeAvailabilityObserved(
                    VexConversationHomeAvailability.Available,
                )
                hybrid.companionAvailabilityObserved(
                    mapOf(
                        VexConversationLabFixture
                            .Vex
                            .participant
                            .participantRef to
                            VexConversationCompanionAvailability.Available,
                    ),
                )
            }

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    ConversationLabUsabilityRoom(
                        repository = repository,
                        lab = lab,
                        relay = relay,
                    )
                }
            }
        }

        waitForDisplayedText("Family")
        waitForDisplayedText("Victor · Maya · Vex")
        waitForDisplayedText("Conversation Lab active · Auto mirror: Maya")
        waitForDisplayedText("Yep, leaving now.")
        captureEvidence(
            checkpoint = "00-initial-family-room",
        )

        val composer =
            compose.onNodeWithTag(
                "$ROOM_REF.composer",
            )
        val send =
            compose.onNodeWithTag(
                "$ROOM_REF.composer.send",
            )

        composer.performTextClearance()
        composer.performTextInput(
            "Are you coming?",
        )
        send
            .assertIsEnabled()
            .performClick()

        waitForDisplayedText("Victor · you")
        waitForDisplayedText("Are you coming?")
        waitForDisplayedText(
            "You said: \"Are you coming?\"",
        )

        lab.addParticipant(
            VexConversationLabFixture
                .Alex
                .participant
                .participantRef,
        )

        waitForDisplayedText(
            "Victor · Maya · Vex · Alex",
        )
        waitForDisplayedText("Yep, leaving now.")
        waitForDisplayedText("Are you coming?")
        captureEvidence(
            checkpoint = "01-round-trip-multi-human",
        )

        composer.performTextInput(
            "Can you still hear me?",
        )

        lab.overrideHomeAvailability(
            VexConversationHomeAvailability.Unavailable,
        )

        waitForSendEnabled(
            enabled = false,
        )
        waitForDisplayedText("Are you coming?")
        waitForDisplayedText(
            "You said: \"Are you coming?\"",
        )
        captureEvidence(
            checkpoint = "02-home-unavailable",
        )

        lab.clearHomeAvailabilityOverride()

        waitForSendEnabled(
            enabled = true,
        )
    }

    private fun captureEvidence(
        checkpoint: String,
    ) {
        require(
            checkpoint.matches(
                Regex("[a-z0-9][a-z0-9-]*"),
            ),
        ) {
            "Conversation UI checkpoint must be lowercase kebab-case: $checkpoint"
        }

        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()

        compose.waitForIdle()
        instrumentation.waitForIdleSync()

        val bitmap =
            requireNotNull(
                instrumentation
                    .uiAutomation
                    .takeScreenshot(),
            ) {
                "Conversation UI screenshot returned null at $checkpoint"
            }

        val bytes =
            try {
                ByteArrayOutputStream()
                    .use { output ->
                        check(
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                output,
                            ),
                        ) {
                            "failed to encode Conversation UI screenshot at $checkpoint"
                        }
                        output.toByteArray()
                    }
            } finally {
                bitmap.recycle()
            }

        check(bytes.isNotEmpty()) {
            "Conversation UI screenshot encoded to zero bytes at $checkpoint"
        }

        val storage =
            PlatformTestStorageRegistry
                .getInstance()
        val path =
            "vex-conversation-ui-practicum/screenshots/" +
                "$checkpoint.png"

        storage
            .openOutputFile(path)
            .buffered()
            .use { output ->
                output.write(bytes)
            }

        val sha256 =
            MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { byte ->
                    "%02x".format(byte)
                }

        Log.i(
            UI_PRACTICUM_LOG_TAG,
            "CAPTURE checkpoint=$checkpoint " +
                "output=$path " +
                "bytes=" + bytes.size + " " +
                "sha256=$sha256 " +
                "uri=" + storage.getOutputFileUri(path),
        )
    }
    private fun waitForDisplayedText(
        text: String,
    ) {
        compose.waitUntil(
            timeoutMillis = 5_000,
        ) {
            compose
                .onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        compose
            .onNodeWithText(text)
            .assertIsDisplayed()
    }

    private fun waitForSendEnabled(
        enabled: Boolean,
    ) {
        compose.waitUntil(
            timeoutMillis = 5_000,
        ) {
            runCatching {
                val send =
                    compose.onNodeWithTag(
                        "$ROOM_REF.composer.send",
                    )

                if (enabled) {
                    send.assertIsEnabled()
                } else {
                    send.assertIsNotEnabled()
                }

                true
            }.getOrDefault(false)
        }
    }

    private companion object {
        const val ROOM_REF =
            "test.conversation.family-room"
        const val UI_PRACTICUM_LOG_TAG =
            "VexConversationUiPracticum"
    }
}

@Composable
private fun ConversationLabUsabilityRoom(
    repository:
        InMemoryVexFamilyConversationRepository,
    lab:
        VexConversationLab,
    relay:
        VexConversationHybridRelay,
) {
    LaunchedEffect(relay) {
        relay.activate(this)
        try {
            awaitCancellation()
        } finally {
            relay.deactivate()
        }
    }

    val hybrid by
        relay.outputStates
            .collectAsState(
                initial =
                    relay.currentOutput,
            )
    val projection =
        hybrid.surface
            ?: return
    val labels =
        remember {
            VexConversationLabFixture
                .all
                .associate { identity ->
                    identity
                        .participant
                        .participantRef to
                        identity.displayName
                }
        }
    var draft by
        rememberSaveable {
            mutableStateOf("")
        }
    var sendOrdinal by
        rememberSaveable {
            mutableIntStateOf(0)
        }
    val scope =
        rememberCoroutineScope()

    VexConversationRoom(
        projection = projection,
        semanticRootRef =
            "test.conversation.family-room",
        roomTitle = "Family",
        participantLabel = { participantRef ->
            requireNotNull(
                labels[participantRef],
            ) {
                "missing Conversation usability label for $participantRef"
            }
        },
        emptyMessagesLabel = "No messages yet.",
        authenticatedSpeakerSuffix = "you",
        developerEnvironmentLabel =
            "Conversation Lab active · Auto mirror: Maya",
        composerContent = {
            val sendAvailability =
                projectVexFamilyConversationSendAvailability(
                    projection,
                )
            val canSend =
                sendAvailability is
                    VexFamilyConversationSurfaceSendAvailability.Ready &&
                    draft.isNotBlank()

            PreviewConversationComposer(
                semanticRootRef =
                    "test.conversation.family-room",
                label = "Type a message",
                sendLabel = "Send",
                draft = draft,
                enabled = canSend,
                onDraftChange = {
                    draft = it
                },
                onSend = {
                    val content =
                        draft.trim()
                    val currentThread =
                        repository
                            .state
                            .value
                            .thread(
                                VexConversationLabFixture.threadRef,
                            )
                    val family =
                        currentThread
                            ?.familyBinding

                    if (
                        content.isNotEmpty() &&
                        currentThread != null &&
                        family != null
                    ) {
                        val ordinal =
                            sendOrdinal
                        sendOrdinal += 1

                        scope.launch {
                            val result =
                                repository.send(
                                    VexFamilyConversationSendRequest(
                                        threadRef =
                                            currentThread.threadRef,
                                        spaceRef =
                                            family.spaceRef,
                                        expectedChannelRef =
                                            currentThread.currentChannelRef,
                                        expectedMembershipGeneration =
                                            family
                                                .currentMembershipGeneration,
                                        idempotencyKey =
                                            VexConversationIdempotencyKey(
                                                "ui-practicum-send-" +
                                                    ordinal,
                                            ),
                                        content =
                                            content,
                                    ),
                                )

                            if (
                                result is
                                VexFamilyConversationRepositoryResult
                                    .Accepted
                            ) {
                                draft = ""
                                lab.mirrorAcceptedVictorSend(
                                    content,
                                )
                            }
                        }
                    }
                },
            )
        },
    )
}

// [VXG RealForever]
