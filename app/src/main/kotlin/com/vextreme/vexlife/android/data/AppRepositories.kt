package com.vextreme.vexlife.android.data

import android.content.Context
import com.vextreme.vexlife.android.connectedservices.AndroidKeyStoreVexHomeDevicePossessionSigner
import com.vextreme.vexlife.android.connectedservices.HttpsVexPrivateHomeGatewayHttpTransport
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayConnection
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayRouteStore
import com.vextreme.vexlife.android.conversation.VexConversationRepository
import com.vextreme.vexlife.android.conversation.VexFamilyConversationConnectedServiceClient
import com.vextreme.vexlife.android.conversation.VexFamilyConversationConnectedServiceRepository
import com.vextreme.vexlife.android.conversation.VexFamilyConversationPrivateGatewayPort
import com.vextreme.vexlife.android.data.events.EventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.data.loghistory.RoomLogHistoryRepository
import com.vextreme.vexlife.android.data.maps.RoomVexMapsRepository
import com.vextreme.vexlife.android.data.maps.VexMapsRepository
import com.vextreme.vexlife.android.data.maps.distribution.RoomVexMapsDistributionRepository
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionRepository
import com.vextreme.vexlife.android.data.userprofile.RoomUserProfileRepository
import com.vextreme.vexlife.android.data.userprofile.UserProfileRepository
import com.vextreme.vexlife.android.data.vexstream.PairedVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository

/**
 * Small composition surface for data repositories used by the app.
 *
 * This is NOT a service locator for arbitrary behavior and there is intentionally no generic
 * BaseRepository<T>. Each data area earns its own repository contract from real domain needs.
 */
data class AppRepositories(
    val logHistory: LogHistoryRepository,
    val userProfile: UserProfileRepository,
    val vexMaps: VexMapsRepository,
    val vexMapsDistribution: VexMapsDistributionRepository,
    val vexStream: VexStreamMusicRepository,
    val events: EventsRepository,
    val eventsAttention: EventsAttentionPreferenceRepository,
    val eventsAttentionLab: EventsAttentionLabRepository,
    val conversation: VexConversationRepository? = null,
)

/**
 * Creates the production repository graph from Android-local infrastructure.
 *
 * Keep construction here; keep repository use at narrower feature boundaries. Preview/tests may
 * construct [AppRepositories] with in-memory implementations instead.
 */
object AppRepositoryFactory {
    fun production(context: Context): AppRepositories {
        val applicationContext =
            context.applicationContext
        val database =
            VexLifeDatabaseFactory.get(
                applicationContext,
            )
        val conversation =
            VexPrivateHomeGatewayRouteStore(
                applicationContext,
            ).currentEndpoint()
                ?.let { endpoint ->
                    VexFamilyConversationConnectedServiceRepository(
                        client =
                            VexFamilyConversationConnectedServiceClient(
                                port =
                                    VexFamilyConversationPrivateGatewayPort(
                                        gateway =
                                            VexPrivateHomeGatewayConnection(
                                                transport =
                                                    HttpsVexPrivateHomeGatewayHttpTransport(
                                                        endpoint,
                                                    ),
                                                signer =
                                                    AndroidKeyStoreVexHomeDevicePossessionSigner(
                                                        applicationContext,
                                                    ),
                                            ),
                                    ),
                            ),
                    )
                }

        return AppRepositories(
            logHistory = RoomLogHistoryRepository(
                dao = database.logHistoryDao(),
            ),
            userProfile = RoomUserProfileRepository(
                dao = database.userProfileDao(),
            ),
            vexMaps = RoomVexMapsRepository(
                dao = database.vexMapsDao(),
            ),
            vexMapsDistribution = RoomVexMapsDistributionRepository(
                dao = database.vexMapsDistributionDao(),
            ),
            vexStream = PairedVexStreamMusicRepository.production(
                context = applicationContext,
            ),
            events = RelayEventsRepository(),
            eventsAttention = RelayEventsAttentionPreferenceRepository(),
            eventsAttentionLab = RelayEventsAttentionLabRepository(),
            conversation = conversation,
        )
    }
}
