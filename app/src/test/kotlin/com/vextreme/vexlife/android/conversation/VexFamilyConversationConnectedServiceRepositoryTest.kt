package com.vextreme.vexlife.android.conversation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class VexFamilyConversationConnectedServiceRepositoryTest {
    @Test
    fun refreshPublishesServerProjectionWithoutBecomingTheSendRepository() =
        runBlocking {
            val repository =
                repository(
                    port =
                        MutablePort(
                            bootstrapValue =
                                bootstrap(),
                            historyValue =
                                history(),
                        ),
                )

            val refreshed =
                repository.refresh(
                    request(),
                )

            assertEquals(
                refreshed,
                repository.state.value
                    .thread(
                        VexConversationThreadRef(
                            THREAD,
                        ),
                    ),
            )
            assertFalse(
                repository as Any is
                    VexFamilyConversationRepository,
            )
        }

    @Test
    fun equalRefreshRetainsTheExactCurrentStateInstance() =
        runBlocking {
            val repository =
                repository(
                    port =
                        MutablePort(
                            bootstrapValue =
                                bootstrap(),
                            historyValue =
                                history(),
                        ),
                )

            repository.refresh(
                request(),
            )
            val firstState =
                repository.state.value

            repository.refresh(
                request(),
            )

            assertSame(
                firstState,
                repository.state.value,
            )
        }

    @Test
    fun refreshReplacesOnlyTheRequestedThreadAndPreservesThreadOrder() =
        runBlocking {
            val port =
                MutablePort(
                    bootstrapValue =
                        bootstrap(
                            rooms =
                                listOf(
                                    room(
                                        spaceRef =
                                            OTHER_SPACE,
                                        threadRef =
                                            OTHER_THREAD,
                                        channelRef =
                                            OTHER_CHANNEL,
                                        membershipGeneration =
                                            1L,
                                    ),
                                ),
                        ),
                    historyValue =
                        history(
                            spaceRef =
                                OTHER_SPACE,
                            threadRef =
                                OTHER_THREAD,
                            currentChannelRef =
                                OTHER_CHANNEL,
                            membershipGeneration =
                                1L,
                            segmentRefs =
                                listOf(
                                    OTHER_CHANNEL,
                                ),
                        ),
                )
            val repository =
                repository(
                    port =
                        port,
                )

            repository.refresh(
                request(
                    spaceRef =
                        OTHER_SPACE,
                    threadRef =
                        OTHER_THREAD,
                ),
            )

            port.bootstrapValue =
                bootstrap(
                    rooms =
                        listOf(
                            room(
                                spaceRef =
                                    SPACE,
                                threadRef =
                                    THREAD,
                                channelRef =
                                    CHANNEL_ONE,
                                membershipGeneration =
                                    1L,
                            ),
                        ),
                )
            port.historyValue =
                history(
                    currentChannelRef =
                        CHANNEL_ONE,
                    membershipGeneration =
                        1L,
                    segmentRefs =
                        listOf(
                            CHANNEL_ONE,
                        ),
                )
            repository.refresh(
                request(),
            )

            port.bootstrapValue =
                bootstrap()
            port.historyValue =
                history()
            repository.refresh(
                request(),
            )

            assertEquals(
                listOf(
                    VexConversationThreadRef(
                        OTHER_THREAD,
                    ),
                    VexConversationThreadRef(
                        THREAD,
                    ),
                ),
                repository.state.value
                    .threads
                    .map { thread ->
                        thread.threadRef
                    },
            )
            assertEquals(
                2L,
                requireNotNull(
                    repository.state.value
                        .thread(
                            VexConversationThreadRef(
                                THREAD,
                            ),
                        )
                        ?.familyBinding,
                ).currentMembershipGeneration,
            )
        }

    @Test
    fun failedReadDoesNotMutateExistingAndroidProjection() =
        runBlocking {
            val port =
                MutablePort(
                    bootstrapValue =
                        bootstrap(
                            rooms =
                                listOf(
                                    room(
                                        spaceRef =
                                            OTHER_SPACE,
                                        threadRef =
                                            OTHER_THREAD,
                                        channelRef =
                                            OTHER_CHANNEL,
                                        membershipGeneration =
                                            1L,
                                    ),
                                ),
                        ),
                    historyValue =
                        history(
                            spaceRef =
                                OTHER_SPACE,
                            threadRef =
                                OTHER_THREAD,
                            currentChannelRef =
                                OTHER_CHANNEL,
                            membershipGeneration =
                                1L,
                            segmentRefs =
                                listOf(
                                    OTHER_CHANNEL,
                                ),
                        ),
                )
            val repository =
                repository(
                    port =
                        port,
                )
            repository.refresh(
                request(
                    spaceRef =
                        OTHER_SPACE,
                    threadRef =
                        OTHER_THREAD,
                ),
            )
            val before =
                repository.state.value

            port.bootstrapValue =
                bootstrap(
                    rooms =
                        emptyList(),
                )
            port.historyValue =
                history()

            assertThrows(
                IllegalStateException::class.java,
            ) {
                runBlocking {
                    repository.refresh(
                        request(),
                    )
                }
            }

            assertSame(
                before,
                repository.state.value,
            )
        }

    private fun repository(
        port:
            VexFamilyConversationConnectedServicePort,
    ) =
        VexFamilyConversationConnectedServiceRepository(
            client =
                VexFamilyConversationConnectedServiceClient(
                    port,
                ),
        )

    private fun request(
        spaceRef:
            String =
            SPACE,
        threadRef:
            String =
            THREAD,
    ) =
        VexFamilyConversationReadRequest(
            spaceRef =
                VexConversationSpaceRef(
                    spaceRef,
                ),
            threadRef =
                VexConversationThreadRef(
                    threadRef,
                ),
        )

    private fun bootstrap(
        rooms:
            List<VexFamilyConversationServerRoom> =
            listOf(
                room(
                    spaceRef =
                        SPACE,
                    threadRef =
                        THREAD,
                    channelRef =
                        CHANNEL_TWO,
                    membershipGeneration =
                        2L,
                ),
            ),
    ) =
        VexFamilyConversationServerBootstrap(
            currentPrincipalRef =
                VICTOR,
            rooms =
                rooms,
        )

    private fun history(
        spaceRef:
            String =
            SPACE,
        threadRef:
            String =
            THREAD,
        currentChannelRef:
            String =
            CHANNEL_TWO,
        membershipGeneration:
            Long =
            2L,
        segmentRefs:
            List<String> =
            listOf(
                CHANNEL_ONE,
                CHANNEL_TWO,
            ),
    ) =
        VexFamilyConversationServerThreadHistory(
            spaceRef =
                spaceRef,
            threadRef =
                threadRef,
            currentChannelRef =
                currentChannelRef,
            membershipGeneration =
                membershipGeneration,
            segmentRefs =
                segmentRefs,
            messages =
                emptyList(),
            truncated =
                false,
        )

    private fun room(
        spaceRef:
            String,
        threadRef:
            String,
        channelRef:
            String,
        membershipGeneration:
            Long,
    ) =
        VexFamilyConversationServerRoom(
            spaceRef =
                spaceRef,
            threadRef =
                threadRef,
            channelRef =
                channelRef,
            membershipGeneration =
                membershipGeneration,
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

    private class MutablePort(
        var bootstrapValue:
            VexFamilyConversationServerBootstrap,
        var historyValue:
            VexFamilyConversationServerThreadHistory,
    ) : VexFamilyConversationConnectedServicePort {
        override suspend fun bootstrap():
            VexFamilyConversationServerBootstrap =
            bootstrapValue

        override suspend fun readThreadHistory(
            intent:
                VexFamilyConversationThreadHistoryIntent,
        ): VexFamilyConversationServerThreadHistory =
            historyValue
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
        const val OTHER_SPACE =
            "space.vex-family.other"
        const val OTHER_THREAD =
            "thread.vex-family.other"
        const val OTHER_CHANNEL =
            "channel.vex-family.other.g1"
        const val VICTOR =
            "person.victor-gong"
        const val BRI =
            "person.bri"
        const val COMPANION =
            "lineage.vex.family.vcs04"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][READ-REPOSITORY]
