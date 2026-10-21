package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFamilyConversationInstalledReadTest {
    @Test
    fun singleCurrentBootstrapRoomPublishesThroughExistingRepository() =
        runBlocking {
            val port =
                RecordingPort(
                    bootstrapValue =
                        bootstrap(
                            listOf(
                                room(),
                            ),
                        ),
                    historyValue =
                        history(),
                )
            val repository =
                VexFamilyConversationConnectedServiceRepository(
                    VexFamilyConversationConnectedServiceClient(
                        port,
                    ),
                )

            val refreshed =
                repository
                    .refreshSingleCurrentRoom()

            assertEquals(
                THREAD,
                refreshed.threadRef.value,
            )
            assertEquals(
                refreshed,
                repository
                    .state
                    .value
                    .threads
                    .single(),
            )
            assertEquals(
                listOf(
                    VexFamilyConversationThreadHistoryIntent(
                        spaceRef =
                            SPACE,
                        channelRef =
                            CHANNEL,
                        expectedMembershipGeneration =
                            2L,
                    ),
                ),
                port.historyIntents,
            )
        }

    @Test
    fun multipleCurrentRoomsFailBeforeAndroidGuessesOne() =
        runBlocking {
            val port =
                RecordingPort(
                    bootstrapValue =
                        bootstrap(
                            listOf(
                                room(),
                                room(
                                    threadRef =
                                        "thread.family.other",
                                    channelRef =
                                        "channel.family.other.g1",
                                ),
                            ),
                        ),
                    historyValue =
                        history(),
                )
            val repository =
                VexFamilyConversationConnectedServiceRepository(
                    VexFamilyConversationConnectedServiceClient(
                        port,
                    ),
                )

            assertThrows(
                IllegalStateException::class.java,
            ) {
                runBlocking {
                    repository
                        .refreshSingleCurrentRoom()
                }
            }
            assertTrue(
                port.historyIntents.isEmpty(),
            )
            assertTrue(
                repository
                    .state
                    .value
                    .threads
                    .isEmpty(),
            )
        }

    private fun bootstrap(
        rooms:
            List<VexFamilyConversationServerRoom>,
    ) =
        VexFamilyConversationServerBootstrap(
            currentPrincipalRef =
                VICTOR,
            rooms =
                rooms,
        )

    private fun room(
        threadRef: String = THREAD,
        channelRef: String = CHANNEL,
    ) =
        VexFamilyConversationServerRoom(
            spaceRef =
                SPACE,
            threadRef =
                threadRef,
            channelRef =
                channelRef,
            membershipGeneration =
                2L,
            audiencePrincipalRefs =
                listOf(
                    VICTOR,
                    "person.bri",
                ),
            familyCompanionLineageRef =
                COMPANION,
            familyCompanionIncluded =
                true,
        )

    private fun history() =
        VexFamilyConversationServerThreadHistory(
            spaceRef =
                SPACE,
            threadRef =
                THREAD,
            currentChannelRef =
                CHANNEL,
            membershipGeneration =
                2L,
            segmentRefs =
                listOf(
                    CHANNEL,
                ),
            messages =
                emptyList(),
            truncated =
                false,
        )

    private class RecordingPort(
        private val bootstrapValue:
            VexFamilyConversationServerBootstrap,
        private val historyValue:
            VexFamilyConversationServerThreadHistory,
    ) : VexFamilyConversationConnectedServicePort {
        val historyIntents =
            mutableListOf<
                VexFamilyConversationThreadHistoryIntent
            >()

        override suspend fun bootstrap():
            VexFamilyConversationServerBootstrap =
            bootstrapValue

        override suspend fun readThreadHistory(
            intent:
                VexFamilyConversationThreadHistoryIntent,
        ): VexFamilyConversationServerThreadHistory {
            historyIntents +=
                intent
            return historyValue
        }
    }

    private companion object {
        const val SPACE =
            "space.family.vcs04"
        const val THREAD =
            "thread.family.vcs04"
        const val CHANNEL =
            "channel.family.vcs04.g2"
        const val VICTOR =
            "person.victor-gong"
        const val COMPANION =
            "lineage.family.vcs04"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][INSTALLED-READ-PROOF]
