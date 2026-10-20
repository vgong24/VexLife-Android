package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFamilyConversationConnectedServiceClientTest {
    @Test
    fun readUsesCurrentBootstrapCoordinatesAndExistingServerProjection() =
        runBlocking {
            val port =
                RecordingPort(
                    bootstrapResult =
                        bootstrap(),
                    historyResult =
                        history(),
                )
            val client =
                VexFamilyConversationConnectedServiceClient(
                    port,
                )

            val projected =
                client.readThread(
                    request(),
                )

            assertEquals(
                1,
                port.bootstrapCalls,
            )
            assertEquals(
                listOf(
                    VexFamilyConversationThreadHistoryIntent(
                        spaceRef =
                            SPACE,
                        channelRef =
                            CHANNEL_TWO,
                        expectedMembershipGeneration =
                            2L,
                    ),
                ),
                port.historyIntents,
            )
            assertEquals(
                VexConversationThreadRef(
                    THREAD,
                ),
                projected.threadRef,
            )
            assertEquals(
                VexConversationChannelRef(
                    CHANNEL_TWO,
                ),
                projected.currentChannelRef,
            )
            assertEquals(
                VexConversationSpaceRef(
                    SPACE,
                ),
                requireNotNull(
                    projected.familyBinding,
                ).spaceRef,
            )
        }

    @Test
    fun missingRequestedRoomFailsBeforeHistoryRead() =
        runBlocking {
            val port =
                RecordingPort(
                    bootstrapResult =
                        bootstrap(
                            rooms =
                                emptyList(),
                        ),
                    historyResult =
                        history(),
                )
            val client =
                VexFamilyConversationConnectedServiceClient(
                    port,
                )

            assertThrows(
                IllegalStateException::class.java,
            ) {
                runBlocking {
                    client.readThread(
                        request(),
                    )
                }
            }
            assertTrue(
                port.historyIntents.isEmpty(),
            )
        }

    @Test
    fun duplicateRequestedRoomFailsBeforeHistoryRead() =
        runBlocking {
            val room =
                room()
            val port =
                RecordingPort(
                    bootstrapResult =
                        bootstrap(
                            rooms =
                                listOf(
                                    room,
                                    room.copy(),
                                ),
                        ),
                    historyResult =
                        history(),
                )
            val client =
                VexFamilyConversationConnectedServiceClient(
                    port,
                )

            assertThrows(
                IllegalStateException::class.java,
            ) {
                runBlocking {
                    client.readThread(
                        request(),
                    )
                }
            }
            assertTrue(
                port.historyIntents.isEmpty(),
            )
        }

    @Test
    fun contradictoryHistoryStillFailsAtExistingServerAdapterBoundary() =
        runBlocking {
            val port =
                RecordingPort(
                    bootstrapResult =
                        bootstrap(),
                    historyResult =
                        history(
                            currentChannelRef =
                                CHANNEL_ONE,
                        ),
                )
            val client =
                VexFamilyConversationConnectedServiceClient(
                    port,
                )

            assertThrows(
                IllegalArgumentException::class.java,
            ) {
                runBlocking {
                    client.readThread(
                        request(),
                    )
                }
            }
            assertEquals(
                1,
                port.historyIntents.size,
            )
        }

    @Test
    fun featureIntentContainsOnlyFamilyConversationCurrentnessMeaning() {
        val intent =
            VexFamilyConversationThreadHistoryIntent(
                spaceRef =
                    SPACE,
                channelRef =
                    CHANNEL_TWO,
                expectedMembershipGeneration =
                    2L,
            )

        assertEquals(
            SPACE,
            intent.spaceRef,
        )
        assertEquals(
            CHANNEL_TWO,
            intent.channelRef,
        )
        assertEquals(
            2L,
            intent.expectedMembershipGeneration,
        )
    }

    private fun request() =
        VexFamilyConversationReadRequest(
            spaceRef =
                VexConversationSpaceRef(
                    SPACE,
                ),
            threadRef =
                VexConversationThreadRef(
                    THREAD,
                ),
        )

    private fun bootstrap(
        rooms:
            List<VexFamilyConversationServerRoom> =
            listOf(
                room(),
            ),
    ) =
        VexFamilyConversationServerBootstrap(
            currentPrincipalRef =
                VICTOR,
            rooms =
                rooms,
        )

    private fun room() =
        VexFamilyConversationServerRoom(
            spaceRef =
                SPACE,
            threadRef =
                THREAD,
            channelRef =
                CHANNEL_TWO,
            membershipGeneration =
                2L,
            audiencePrincipalRefs =
                listOf(
                    VICTOR,
                    BRI,
                ),
            familyCompanionLineageRef =
                COMPANION,
            familyCompanionIncluded =
                true,
        )

    private fun history(
        currentChannelRef:
            String =
            CHANNEL_TWO,
    ) =
        VexFamilyConversationServerThreadHistory(
            spaceRef =
                SPACE,
            threadRef =
                THREAD,
            currentChannelRef =
                currentChannelRef,
            membershipGeneration =
                2L,
            segmentRefs =
                listOf(
                    CHANNEL_ONE,
                    CHANNEL_TWO,
                ),
            messages =
                emptyList(),
            truncated =
                false,
        )

    private class RecordingPort(
        private val bootstrapResult:
            VexFamilyConversationServerBootstrap,
        private val historyResult:
            VexFamilyConversationServerThreadHistory,
    ) : VexFamilyConversationConnectedServicePort {
        var bootstrapCalls:
            Int =
            0
            private set
        val historyIntents =
            mutableListOf<
                VexFamilyConversationThreadHistoryIntent
            >()

        override suspend fun bootstrap():
            VexFamilyConversationServerBootstrap {
            bootstrapCalls += 1
            return bootstrapResult
        }

        override suspend fun readThreadHistory(
            intent:
                VexFamilyConversationThreadHistoryIntent,
        ): VexFamilyConversationServerThreadHistory {
            historyIntents +=
                intent
            return historyResult
        }
    }

    private companion object {
        const val SPACE =
            "space.vex-family.vcs04"
        const val THREAD =
            "thread.vex-family.vcs04"
        const val CHANNEL_ONE =
            "channel.vex-family.vcs04.g1"
        const val CHANNEL_TWO =
            "channel.vex-family.vcs04.g2"
        const val VICTOR =
            "person.victor-gong"
        const val BRI =
            "person.bri"
        const val COMPANION =
            "lineage.vex.family.vcs04"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][READ-CLIENT]
