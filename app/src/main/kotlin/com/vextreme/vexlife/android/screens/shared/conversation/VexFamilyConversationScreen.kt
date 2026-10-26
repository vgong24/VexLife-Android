package com.vextreme.vexlife.android.screens.shared.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.connectedservices.privateHomeGatewayDiagnosticRefOrNull
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.VexConversationHybridRelay
import com.vextreme.vexlife.android.conversation.VexConversationIdempotencyKey
import com.vextreme.vexlife.android.conversation.VexConversationThreadState
import com.vextreme.vexlife.android.conversation.VexFamilyConversationConnectedServiceRepository
import com.vextreme.vexlife.android.conversation.VexFamilyConversationRepository
import com.vextreme.vexlife.android.conversation.VexFamilyConversationRepositoryResult
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSendRequest
import com.vextreme.vexlife.android.conversation.VexFamilyConversationSurfaceSendAvailability
import com.vextreme.vexlife.android.conversation.projectVexFamilyConversationSendAvailability
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.VexTextField
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.preview.VexRenderedScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.ui.VexLifeRelays
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Installed Family Conversation doorway backed by accepted VCS-04 private Home truth.
 *
 * The Screen owns only human presentation + one registered Send intention. Private Home remains the
 * Family writer and speaker/currentness authority. Conversation remains the canonical Android
 * projection. Companion/model attention remains a separate effect after durable human append.
 */
object VexFamilyConversationScreen : AppScreenBlueprint(
    screenKey = "family-conversation",
    titleRes = R.string.conversation_family_title,
) {
    val MessageDraft =
        textFieldRef(
            id = "body.composer.message",
            labelRes = R.string.conversation_message,
        )

    val Send =
        buttonRef(
            id = "body.composer.send",
            labelRes = R.string.conversation_send,
            intent =
                invoke(
                    AppActions.SendFamilyConversationMessage,
                ),
        )

    override val content =
        layout {
            column("body") {
                text(
                    id = "state",
                    textRes =
                        R.string.conversation_loading,
                )
                column("composer") {
                    textField(
                        MessageDraft,
                    )
                    button(
                        Send,
                    )
                }
            }
        }

    internal override fun appActionRegistrations(
        context:
            ScreenActionContext,
    ): AppActionRegistrations {
        val repository =
            context.repositories.conversation as?
                VexFamilyConversationConnectedServiceRepository

        return familyConversationActionRegistrations(
            repository =
                repository,
            refreshCurrentRoom = {
                checkNotNull(
                    repository,
                ) {
                    "Installed Family Conversation Send requires the Connected Services repository"
                }.refreshSingleCurrentRoom()
            },
            scope =
                context.scope,
        )
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relays: VexLifeRelays,
        darkTheme: Boolean,
    ) {
        VexFamilyConversationScreenContent(
            repository =
                repositories.conversation as?
                    VexFamilyConversationConnectedServiceRepository,
            relay =
                relays.conversation,
            runtime =
                runtime,
        )
    }
}

private data class PendingFamilyConversationSend(
    val content: String,
    val idempotencyKey:
        VexConversationIdempotencyKey,
)

/**
 * Session-lifetime owner of the human Send attempt identity.
 *
 * The repository contract deliberately requires caller-supplied idempotency. This registration
 * therefore retains one unresolved human attempt across transport uncertainty. A terminal repository
 * result clears that attempt; the next intentional Send receives a new key.
 */
internal fun familyConversationActionRegistrations(
    repository:
        VexFamilyConversationRepository?,
    refreshCurrentRoom:
        suspend () -> VexConversationThreadState,
    scope:
        CoroutineScope,
    idempotencyKeyFactory:
        () -> VexConversationIdempotencyKey = {
            VexConversationIdempotencyKey(
                "send.vexlife-android.family." +
                    UUID.randomUUID(),
            )
        },
): AppActionRegistrations {
    val pendingLock =
        Any()
    val sendMutex =
        Mutex()
    var pending:
        PendingFamilyConversationSend? =
        null

    fun clearTerminal(
        attempt:
            PendingFamilyConversationSend,
    ) {
        synchronized(
            pendingLock,
        ) {
            if (pending == attempt) {
                pending =
                    null
            }
        }
    }

    return AppActionRegistrations.of(
        appAction(
            action =
                AppActions.SendFamilyConversationMessage,
            ownerRef =
                VexFamilyConversationScreen.screenId,
        ) { request ->
            val content =
                request.submission
                    ?.value(
                        VexFamilyConversationScreen.MessageDraft,
                    )
                    ?.trim()
                    .orEmpty()

            if (
                repository == null ||
                content.isEmpty()
            ) {
                return@appAction
            }

            val attempt =
                synchronized(
                    pendingLock,
                ) {
                    val current =
                        pending
                    when {
                        current == null ->
                            PendingFamilyConversationSend(
                                content =
                                    content,
                                idempotencyKey =
                                    idempotencyKeyFactory(),
                            ).also { formed ->
                                pending =
                                    formed
                            }

                        current.content ==
                            content ->
                            current

                        else ->
                            null
                    }
                } ?: return@appAction

            scope.launch {
                sendMutex.withLock {
                    try {
                        val thread =
                            refreshCurrentRoom()
                        val family =
                            thread.familyBinding
                                ?: run {
                                    clearTerminal(
                                        attempt,
                                    )
                                    return@withLock
                                }

                        when (
                            repository.send(
                                VexFamilyConversationSendRequest(
                                    threadRef =
                                        thread.threadRef,
                                    spaceRef =
                                        family.spaceRef,
                                    expectedChannelRef =
                                        thread.currentChannelRef,
                                    expectedMembershipGeneration =
                                        family
                                            .currentMembershipGeneration,
                                    idempotencyKey =
                                        attempt.idempotencyKey,
                                    content =
                                        attempt.content,
                                ),
                            )
                        ) {
                            is VexFamilyConversationRepositoryResult.Accepted,
                            is VexFamilyConversationRepositoryResult.Rejected,
                            ->
                                clearTerminal(
                                    attempt,
                                )
                        }
                    } catch (
                        error:
                            CancellationException,
                    ) {
                        throw error
                    } catch (
                        _: Exception,
                    ) {
                        /*
                         * Transport/host uncertainty retains the same human attempt identity. The
                         * next submission of the same content therefore retries with the same key.
                         */
                    }
                }
            }
        },
    )
}

@Composable
internal fun VexFamilyConversationScreenContent(
    repository:
        VexFamilyConversationConnectedServiceRepository?,
    relay:
        VexConversationHybridRelay,
    runtime:
        VexRuntime? = null,
) {
    var refreshFailed by
        rememberSaveable(
            repository,
        ) {
            mutableStateOf(
                false,
            )
        }
    var refreshDiagnosticRef by
        rememberSaveable(
            repository,
        ) {
            mutableStateOf<String?>(
                null,
            )
        }
    var draft by
        rememberSaveable(
            repository,
        ) {
            mutableStateOf(
                "",
            )
        }
    var submittedContent by
        rememberSaveable(
            repository,
        ) {
            mutableStateOf<String?>(
                null,
            )
        }
    var submittedAfterSequence by
        rememberSaveable(
            repository,
        ) {
            mutableLongStateOf(
                -1L,
            )
        }

    LaunchedEffect(
        repository,
        relay,
    ) {
        if (repository == null) {
            relay.authenticatedParticipantObserved(
                null,
            )
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Unavailable,
            )
            return@LaunchedEffect
        }

        runCatching {
            repository
                .refreshSingleCurrentRoomObservation()
        }.onSuccess { observation ->
            relay.authenticatedParticipantObserved(
                observation
                    .authenticatedParticipantRef,
            )
            relay.selectThread(
                observation
                    .thread
                    .threadRef,
            )
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Available,
            )
            refreshFailed =
                false
            refreshDiagnosticRef =
                null
        }.onFailure { error ->
            relay.authenticatedParticipantObserved(
                null,
            )
            relay.homeAvailabilityObserved(
                VexConversationHomeAvailability
                    .Unavailable,
            )
            refreshFailed =
                true
            refreshDiagnosticRef =
                error.privateHomeGatewayDiagnosticRefOrNull()
                    ?: FAMILY_CONVERSATION_REFRESH_UNCLASSIFIED_DIAGNOSTIC_REF
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

    LaunchedEffect(
        projection?.thread?.messages,
        projection?.authenticatedParticipantRef,
        submittedContent,
        submittedAfterSequence,
    ) {
        val content =
            submittedContent
                ?: return@LaunchedEffect
        val surface =
            projection
                ?: return@LaunchedEffect
        val authenticatedParticipantRef =
            surface.authenticatedParticipantRef
                ?: return@LaunchedEffect

        val canonicalAcceptanceObserved =
            surface.thread.messages
                .any { message ->
                    message.channelRef ==
                        surface.thread.currentChannelRef &&
                        message.sequence >
                            submittedAfterSequence &&
                        message.speakerRef ==
                            authenticatedParticipantRef &&
                        message.content ==
                            content
                }

        if (canonicalAcceptanceObserved) {
            draft =
                ""
            submittedContent =
                null
            submittedAfterSequence =
                -1L
        }
    }

    if (projection == null) {
        VexComposeScope(
            parentRef =
                VexFamilyConversationScreen.screenId,
        ) {
            VexColumn(
                id = "content",
                modifier =
                    VexModifier
                        .fillMaxSize()
                        .padding(
                            16.dp,
                        ),
            ) {
                VexText(
                    id = "state",
                    text =
                        stringResource(
                            when {
                                repository == null ->
                                    R.string
                                        .conversation_route_unavailable

                                refreshFailed ->
                                    R.string
                                        .conversation_read_unavailable

                                else ->
                                    R.string
                                        .conversation_loading
                            },
                        ),
                    role =
                        TextRole.Supporting,
                )
                if (refreshFailed) {
                    refreshDiagnosticRef
                        ?.let { diagnosticRef ->
                            VexText(
                                id = "diagnostic",
                                text =
                                    diagnosticRef,
                                role =
                                    TextRole.Supporting,
                            )
                        }
                }
            }
        }
        return
    }

    val sendAvailability =
        projectVexFamilyConversationSendAvailability(
            projection,
        )
    val canSend =
        runtime != null &&
            sendAvailability is
                VexFamilyConversationSurfaceSendAvailability.Ready &&
            draft.trim().isNotEmpty()

    val actionHandler =
        ElementActionHandler { element ->
            val currentRuntime =
                runtime
                    ?: return@ElementActionHandler

            if (
                element ===
                VexFamilyConversationScreen.Send
            ) {
                val content =
                    draft.trim()
                if (content.isNotEmpty()) {
                    submittedContent =
                        content
                    submittedAfterSequence =
                        projection.thread.messages
                            .filter { message ->
                                message.channelRef ==
                                    projection.thread.currentChannelRef
                            }
                            .maxOfOrNull { message ->
                                message.sequence
                            }
                            ?: -1L

                    currentRuntime.submit(
                        element =
                            VexFamilyConversationScreen.Send,
                        submission =
                            FormSubmission.of(
                                VexFamilyConversationScreen.MessageDraft to
                                    content,
                            ),
                    )
                }
            } else {
                currentRuntime.activate(
                    element,
                )
            }
        }

    VexConversationRoom(
        projection =
            projection,
        semanticRootRef =
            VexFamilyConversationScreen.screenId,
        roomTitle =
            stringResource(
                R.string.conversation_family_title,
            ),
        participantLabel = { participantRef ->
            participantRef.value
        },
        emptyMessagesLabel =
            stringResource(
                R.string.conversation_empty,
            ),
        authenticatedSpeakerSuffix =
            stringResource(
                R.string.conversation_authenticated_suffix,
            ),
        developerEnvironmentLabel =
            if (
                projection.homeAvailability ==
                VexConversationHomeAvailability
                    .Unavailable
            ) {
                stringResource(
                    R.string.conversation_home_unavailable,
                )
            } else {
                null
            },
        composerContent = {
            if (runtime != null) {
                VexColumn(
                    id = "composer",
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp,
                        ),
                ) {
                    VexTextField(
                        element =
                            VexFamilyConversationScreen.MessageDraft,
                        value =
                            draft,
                        onValueChange = { value ->
                            draft =
                                value
                        },
                    )
                    VexButton(
                        element =
                            VexFamilyConversationScreen.Send,
                        handler =
                            actionHandler,
                        enabled =
                            canSend,
                    )
                }
            }
        },
    )
}

private const val FAMILY_CONVERSATION_REFRESH_UNCLASSIFIED_DIAGNOSTIC_REF =
    "diagnostic.vexlife.connected-services.family-conversation.refresh.unclassified"

@VexScreenPreviews
@Composable
private fun VexFamilyConversationScreenPreview() {
    VexRenderedScreenPreview(
        VexFamilyConversationScreen,
    )
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][INSTALLED-SEND]
