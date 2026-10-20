package com.vextreme.vexlife.android.data

import android.content.Context
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
)

/**
 * Creates the production repository graph from Android-local infrastructure.
 *
 * Keep construction here; keep repository use at narrower feature boundaries. Preview/tests may
 * construct [AppRepositories] with in-memory implementations instead.
 */
object AppRepositoryFactory {
    fun production(context: Context): AppRepositories {
        val database = VexLifeDatabaseFactory.get(context)

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
                context = context,
            ),
            events = RelayEventsRepository(),
            eventsAttention = RelayEventsAttentionPreferenceRepository(),
            eventsAttentionLab = RelayEventsAttentionLabRepository(),
        )
    }
}
