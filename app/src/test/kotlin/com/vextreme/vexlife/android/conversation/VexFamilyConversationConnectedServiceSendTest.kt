package com.vextreme.vexlife.android.conversation

import java.lang.reflect.Modifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFamilyConversationConnectedServiceSendTest {
    @Test
    fun sendUsesCurrentCoordinatesThenPublishesCanonicalHistory() =
        runBlocking {
            val port =
                MutablePort()
            val repository =
                repository(
                    port,
                )
            repository.refreshSingleCurrentRoom()

            val result =
                repository.send(
                    sendRequest(),
                )

            assertEquals(
                VexFamilyConversationRepositoryResult.Accepted(
                    VexConversationMessageRef(
                        MESSAGE,
                    ),
                ),
                result,
            )
            assertEquals(
                listOf(
                    VexFamilyConversationAppendIntent(
                        spaceRef = SPACE,
                        channelRef = CHANNEL,
                        expectedMembershipGeneration = 2L,
                        idempotencyKey = IDEMPOTENCY,
                        content = CONTENT,
                    ),
                ),
                port.appendIntents,
            )
            assertEquals(
                MESSAGE,
                repository.state.value
                    .thread(
                        VexConversationThreadRef(
                            THREAD,
                        ),
                    )
                    ?.messages
                    ?.single()
                    ?.messageRef
                    ?.value,
            )
        }

    @Test
    fun staleFamilyCurrentnessRejectsBeforeAppend() =
        runBlocking {
            val port =
                MutablePort()
            val repository =
                repository(
                    port,
                )
            repository.refreshSingleCurrentRoom()

            val result =
                repository.send(
                    sendRequest()
                        .copy(
                            expectedChannelRef =
                                VexConversationChannelRef(
                                    "channel.family.vcs04.stale",
                                ),
                        ),
                )

            assertEquals(
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection.CURRENTNESS_CHANGED,
                ),
                result,
            )
            assertTrue(
                port.appendIntents.isEmpty(),
            )
        }

    @Test
    fun conflictOnStillCurrentRoomIsIdempotencyConflict() =
        runBlocking {
            val port =
                MutablePort(
                    appendFailure =
                        VexFamilyConversationConnectedServiceFailureKind.CONFLICT,
                )
            val repository =
                repository(
                    port,
                )
            repository.refreshSingleCurrentRoom()

            val result =
                repository.send(
                    sendRequest(),
                )

            assertEquals(
                VexFamilyConversationRepositoryResult.Rejected(
                    VexFamilyConversationRepositoryRejection.IDEMPOTENCY_CONFLICT,
                ),
                result,
            )
            assertEquals(
                1,
                port.appendIntents.size,
            )
        }

    @Test
    fun exactRetryMayReturnIdempotentCurrentWithoutDuplicateAppend() =
        runBlocking {
            val port =
                MutablePort(
                    appendState =
                        VexFamilyConversationAppendState.IDEMPOTENT_CURRENT,
                )
            val repository =
                repository(
                    port,
                )
            repository.refreshSingleCurrentRoom()

            val result =
                repository.send(
                    sendRequest(),
                )

            assertEquals(
                VexFamilyConversationRepositoryResult.Accepted(
                    VexConversationMessageRef(
                        MESSAGE,
                    ),
                ),
                result,
            )
            assertEquals(
                1,
                port.appendIntents.size,
            )
        }

    @Test
    fun appendIntentContainsOnlyFamilySemanticSendFields() {
        val fieldNames =
            VexFamilyConversationAppendIntent::class.java
                .declaredFields
                .filterNot { field ->
                    Modifier.isStatic(
                        field.modifiers,
                    )
                }
                .map { field ->
                    field.name
                }
                .toSet()

        assertEquals(
            setOf(
                "spaceRef",
                "channelRef",
                "expectedMembershipGeneration",
                "idempotencyKey",
                "content",
            ),
            fieldNames,
        )
    }

    private fun repository(
        port: VexFamilyConversationConnectedServicePort,
    ) =
        VexFamilyConversationConnectedServiceRepository(
            VexFamilyConversationConnectedServiceClient(
                port,
            ),
        )

    private fun sendRequest() =
        VexFamilyConversationSendRequest(
            threadRef =
                VexConversationThreadRef(
                    THREAD,
                ),
            spaceRef =
                VexConversationSpaceRef(
                    SPACE,
                ),
            expectedChannelRef =
                VexConversationChannelRef(
                    CHANNEL,
                ),
            expectedMembershipGeneration =
                2L,
            idempotencyKey =
                VexConversationIdempotencyKey(
                    IDEMPOTENCY,
                ),
            content =
                CONTENT,
        )

    private class MutablePort(
        private val appendState: VexFamilyConversationAppendState =
            VexFamilyConversationAppendState.APPENDED,
        private val appendFailure: VexFamilyConversationConnectedServiceFailureKind? =
            null,
    ) : VexFamilyConversationConnectedServicePort {
        val appendIntents =
            mutableListOf<VexFamilyConversationAppendIntent>()
        private var currentHistory =
            history(
                messages =
                    emptyList(),
            )

        override suspend fun bootstrap(): VexFamilyConversationServerBootstrap =
            VexFamilyConversationConnectedServiceSendTest.bootstrap()

        override suspend fun append(
            intent: VexFamilyConversationAppendIntent,
        ): VexFamilyConversationAppendResult {
            appendIntents +=
                intent
            appendFailure
                ?.let { failure ->
                    throw VexFamilyConversationConnectedServiceException(
                        failure,
                    )
                }
            currentHistory =
                history(
                    messages =
                        listOf(
                            message(),
                        ),
                )
            return VexFamilyConversationAppendResult(
                state = appendState,
                messageRef = MESSAGE,
            )
        }

        override suspend fun readThreadHistory(
            intent: VexFamilyConversationThreadHistoryIntent,
        ): VexFamilyConversationServerThreadHistory =
            currentHistory
    }

    companion object {
        const val SPACE =
            "space.family.vcs04"
        const val THREAD =
            "thread.family.vcs04"
        const val CHANNEL =
            "channel.family.vcs04.g2"
        const val VICTOR =
            "person.victor-gong"
        const val BRI =
            "person.bri"
        const val COMPANION =
            "lineage.family.vcs04"
        const val MESSAGE =
            "message.family.vcs04.append.001"
        const val IDEMPOTENCY =
            "send.vcs04.android.001"
        const val CONTENT =
            "Durable human Send."

        private fun bootstrap() =
            VexFamilyConversationServerBootstrap(
                currentPrincipalRef = VICTOR,
                rooms =
                    listOf(
                        VexFamilyConversationServerRoom(
                            spaceRef = SPACE,
                            threadRef = THREAD,
                            channelRef = CHANNEL,
                            membershipGeneration = 2L,
                            audiencePrincipalRefs =
                                listOf(
                                    VICTOR,
                                    BRI,
                                ),
                            familyCompanionLineageRef = COMPANION,
                            familyCompanionIncluded = true,
                        ),
                    ),
            )

        private fun history(
            messages: List<VexFamilyConversationServerMessage>,
        ) =
            VexFamilyConversationServerThreadHistory(
                spaceRef = SPACE,
                threadRef = THREAD,
                currentChannelRef = CHANNEL,
                membershipGeneration = 2L,
                segmentRefs =
                    listOf(
                        CHANNEL,
                    ),
                messages = messages,
                truncated = false,
            )

        private fun message() =
            VexFamilyConversationServerMessage(
                messageRef = MESSAGE,
                spaceRef = SPACE,
                threadRef = THREAD,
                channelRef = CHANNEL,
                speakerRef = VICTOR,
                recipientRefs =
                    listOf(
                        BRI,
                    ),
                witnessRefs =
                    listOf(
                        VICTOR,
                        BRI,
                        COMPANION,
                    ),
                membershipGeneration = 2L,
                sequence = 0L,
                content = CONTENT,
            )
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][FAMILY-SEND-FOCUSED-PROOF]
