package com.vextreme.vexlife.android.screens.shared.conversation

import com.vextreme.vexlife.android.conversation.VexConversationChannelRef
import com.vextreme.vexlife.android.conversation.VexConversationIdempotencyKey
import com.vextreme.vexlife.android.conversation.VexConversationLabFixture
import com.vextreme.vexlife.android.conversation.VexConversationMessageRef
import com.vextreme.vexlife.android.conversation.VexConversationState
import com.vextreme.vexlife.android.conversation.VexConversationThreadState
import com.vextreme.vexlife.android.conversation.VexFamilyConversationRepository
import com.vextreme.vexlife.android.conversation.VexFamilyConversationRepositoryResult
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSendRequest
import com.vextreme.vexlife.android.conversation.VexFamilyConversationThreadBinding
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.runtime.AppActionRequest
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.screens.AppScreens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VexFamilyConversationScreenSendTest {
    @Test
    fun registeredComposerControlsBelongToFamilyScreen() {
        listOf(
            VexFamilyConversationScreen.MessageDraft,
            VexFamilyConversationScreen.Send,
        ).forEach { element ->
            assertSame(
                element,
                AppScreens.registry.element(
                    element.elementId,
                ),
            )
            assertEquals(
                VexFamilyConversationScreen.screenId,
                AppScreens.registry
                    .ownerScreenOf(
                        element,
                    )
                    ?.screenId,
            )
        }
    }

    @Test
    fun submittedSendUsesCurrentFamilyCoordinatesAndTrimmedContent() =
        runTest {
            val repository =
                RecordingFamilyRepository(
                    thread(),
                )
            val keys =
                mutableListOf(
                    VexConversationIdempotencyKey(
                        KEY_1,
                    ),
                )
            val registrations =
                familyConversationActionRegistrations(
                    repository =
                        repository,
                    refreshCurrentRoom = {
                        thread()
                    },
                    scope =
                        this,
                    idempotencyKeyFactory = {
                        keys.removeAt(
                            0,
                        )
                    },
                )

            registrations.dispatch(
                sendRequest(
                    "  Durable human Send.  ",
                ),
            )
            advanceUntilIdle()

            val request =
                repository.requests
                    .single()
            assertEquals(
                VexConversationLabFixture.threadRef,
                request.threadRef,
            )
            assertEquals(
                VexConversationLabFixture.spaceRef,
                request.spaceRef,
            )
            assertEquals(
                VexConversationLabFixture.channelRef(
                    GENERATION,
                ),
                request.expectedChannelRef,
            )
            assertEquals(
                GENERATION,
                request.expectedMembershipGeneration,
            )
            assertEquals(
                KEY_1,
                request.idempotencyKey.value,
            )
            assertEquals(
                "Durable human Send.",
                request.content,
            )
        }

    @Test
    fun uncertainRetryReusesTheSameHumanAttemptIdempotencyKey() =
        runTest {
            val repository =
                RecordingFamilyRepository(
                    thread(),
                ).apply {
                    failNext =
                        true
                }
            var keyCalls =
                0
            val registrations =
                familyConversationActionRegistrations(
                    repository =
                        repository,
                    refreshCurrentRoom = {
                        thread()
                    },
                    scope =
                        this,
                    idempotencyKeyFactory = {
                        keyCalls +=
                            1
                        VexConversationIdempotencyKey(
                            KEY_1,
                        )
                    },
                )

            registrations.dispatch(
                sendRequest(
                    "Retry me",
                ),
            )
            advanceUntilIdle()
            registrations.dispatch(
                sendRequest(
                    "Retry me",
                ),
            )
            advanceUntilIdle()

            assertEquals(
                1,
                keyCalls,
            )
            assertEquals(
                listOf(
                    KEY_1,
                    KEY_1,
                ),
                repository.requests
                    .map { request ->
                        request.idempotencyKey.value
                    },
            )
        }

    @Test
    fun terminalAcceptanceAllowsTheNextIntentToReceiveANewKey() =
        runTest {
            val repository =
                RecordingFamilyRepository(
                    thread(),
                )
            val keys =
                mutableListOf(
                    VexConversationIdempotencyKey(
                        KEY_1,
                    ),
                    VexConversationIdempotencyKey(
                        KEY_2,
                    ),
                )
            val registrations =
                familyConversationActionRegistrations(
                    repository =
                        repository,
                    refreshCurrentRoom = {
                        thread()
                    },
                    scope =
                        this,
                    idempotencyKeyFactory = {
                        keys.removeAt(
                            0,
                        )
                    },
                )

            registrations.dispatch(
                sendRequest(
                    "First accepted intent",
                ),
            )
            advanceUntilIdle()
            registrations.dispatch(
                sendRequest(
                    "Second accepted intent",
                ),
            )
            advanceUntilIdle()

            assertEquals(
                listOf(
                    KEY_1,
                    KEY_2,
                ),
                repository.requests
                    .map { request ->
                        request.idempotencyKey.value
                    },
            )
        }

    private fun sendRequest(
        content:
            String,
    ) =
        AppActionRequest(
            action =
                AppActions.SendFamilyConversationMessage,
            source =
                VexFamilyConversationScreen.Send,
            submission =
                FormSubmission.of(
                    VexFamilyConversationScreen.MessageDraft to
                        content,
                ),
        )

    private fun thread():
        VexConversationThreadState {
        val channel =
            VexConversationLabFixture.channelRef(
                GENERATION,
            )
        val known =
            listOf(
                VexConversationLabFixture.Victor.participant,
                VexConversationLabFixture.Maya.participant,
                VexConversationLabFixture.Vex.participant,
            )

        return VexConversationThreadState(
            threadRef =
                VexConversationLabFixture.threadRef,
            currentChannelRef =
                channel,
            knownParticipants =
                known,
            currentAudienceRefs =
                known.map { participant ->
                    participant.participantRef
                },
            messages =
                emptyList(),
            familyBinding =
                VexFamilyConversationThreadBinding(
                    spaceRef =
                        VexConversationLabFixture.spaceRef,
                    currentMembershipGeneration =
                        GENERATION,
                    segmentRefs =
                        listOf(
                            channel,
                        ),
                ),
        )
    }

    private class RecordingFamilyRepository(
        initialThread:
            VexConversationThreadState,
    ) : VexFamilyConversationRepository {
        private val mutableState =
            MutableStateFlow(
                VexConversationState(
                    threads =
                        listOf(
                            initialThread,
                        ),
                ),
            )

        override val state:
            StateFlow<VexConversationState> =
            mutableState.asStateFlow()

        val requests =
            mutableListOf<
                VexFamilyConversationSendRequest
            >()

        var failNext:
            Boolean =
            false

        override suspend fun send(
            request:
                VexFamilyConversationSendRequest,
        ): VexFamilyConversationRepositoryResult {
            requests +=
                request

            if (failNext) {
                failNext =
                    false
                throw IllegalStateException(
                    "synthetic uncertain transport",
                )
            }

            return VexFamilyConversationRepositoryResult
                .Accepted(
                    VexConversationMessageRef(
                        "message.screen-send." +
                            requests.size,
                    ),
                )
        }
    }

    private companion object {
        const val GENERATION =
            2L
        const val KEY_1 =
            "send.screen-test.001"
        const val KEY_2 =
            "send.screen-test.002"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][INSTALLED-SEND-PROOF]
