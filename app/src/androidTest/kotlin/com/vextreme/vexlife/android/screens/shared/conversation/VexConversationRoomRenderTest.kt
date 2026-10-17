package com.vextreme.vexlife.android.screens.shared.conversation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Modifier
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
import androidx.test.espresso.Espresso
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
        val expectedRoomTitle =
            "Family"
        val expectedInitialAudience =
            "Victor · Maya · Vex"
        val expectedLabReady =
            "Conversation Lab active · Auto mirror: Maya"
        val expectedSeedMessage =
            "Yep, leaving now."
        val victorMessage =
            "Are you coming?"
        val expectedVictorSpeaker =
            "Victor · you"
        val expectedMirrorMessage =
            "You said: \"Are you coming?\""
        val alexId =
            VexConversationLabFixture
                .Alex
                .participant
                .participantRef
        val expectedExpandedAudience =
            "Victor · Maya · Vex · Alex"
        val unavailableDraft =
            "Can you still hear me?"
        val expectedHomeUnavailable =
            "Conversation Lab active · Home unavailable"
        val initialCheckpointId =
            "00-initial-family-room"
        val expandedCheckpointId =
            "01-round-trip-multi-human"
        val unavailableCheckpointId =
            "02-home-unavailable"

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
                    content = expectedSeedMessage,
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
                Surface(
                    modifier =
                        Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing,
                        ),
                ) {
                    ConversationLabUsabilityRoom(
                        repository = repository,
                        lab = lab,
                        relay = relay,
                    )
                }
            }
        }

        waitForDisplayedText(expectedRoomTitle)
        waitForDisplayedText(expectedInitialAudience)
        waitForDisplayedText(expectedLabReady)
        waitForDisplayedText(expectedSeedMessage)
        captureEvidence(
            checkpoint = initialCheckpointId,
        )

        val composer =
            compose.onNodeWithTag(
                "$ROOM_ID.composer",
            )
        val send =
            compose.onNodeWithTag(
                "$ROOM_ID.composer.send",
            )

        composer.performTextClearance()
        composer.performTextInput(
            victorMessage,
        )
        send
            .assertIsEnabled()
            .performClick()

        waitForDisplayedText(expectedVictorSpeaker)
        waitForDisplayedText(victorMessage)
        waitForDisplayedText(expectedMirrorMessage)

        hideKeyboardIfShown()

        lab.addParticipant(
            alexId,
        )

        waitForDisplayedText(expectedExpandedAudience)
        waitForDisplayedText(expectedSeedMessage)
        waitForDisplayedText(victorMessage)
        captureEvidence(
            checkpoint = expandedCheckpointId,
        )

        composer.performTextInput(
            unavailableDraft,
        )

        hideKeyboardIfShown()

        lab.overrideHomeAvailability(
            VexConversationHomeAvailability.Unavailable,
        )

        waitForSendEnabled(
            enabled = false,
        )
        waitForDisplayedText(expectedHomeUnavailable)
        waitForDisplayedText(victorMessage)
        waitForDisplayedText(expectedMirrorMessage)
        captureEvidence(
            checkpoint = unavailableCheckpointId,
        )

        lab.clearHomeAvailabilityOverride()

        waitForSendEnabled(
            enabled = true,
        )
        waitForDisplayedText(expectedLabReady)
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

    private fun hideKeyboardIfShown() {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        InstrumentationRegistry
            .getInstrumentation()
            .waitForIdleSync()
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
                        "$ROOM_ID.composer.send",
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
        const val ROOM_ID =
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
        participantLabel = { participantId ->
            requireNotNull(
                labels[participantId],
            ) {
                "missing Conversation usability label for $participantId"
            }
        },
        emptyMessagesLabel = "No messages yet.",
        authenticatedSpeakerSuffix = "you",
        developerEnvironmentLabel =
            when (projection.homeAvailability) {
                VexConversationHomeAvailability.Unobserved ->
                    "Conversation Lab active · Home unobserved"

                VexConversationHomeAvailability.Available ->
                    "Conversation Lab active · Auto mirror: Maya"

                VexConversationHomeAvailability.Unavailable ->
                    "Conversation Lab active · Home unavailable"
            },
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