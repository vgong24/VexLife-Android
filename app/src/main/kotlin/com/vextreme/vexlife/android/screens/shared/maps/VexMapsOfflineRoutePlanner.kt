package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reuses one native Valhalla actor for the current installed region pack.
 */
internal class VexMapsOfflineRoutePlanner {
    private val mutex = Mutex()

    private var providerIdentity:
        String? = null
    private var provider:
        VexValhallaOfflineRouteProvider? = null

    suspend fun route(
        context: Context,
        pack: VexMapsRegionPack,
        request: VexOfflineRouteRequest,
    ): VexOfflineRouteResult =
        mutex.withLock {
            val activeProvider =
                if (
                    provider == null ||
                    providerIdentity !=
                    pack.identity
                ) {
                    val replacement =
                        withContext(
                            Dispatchers.IO,
                        ) {
                            VexValhallaOfflineRouteProvider(
                                context,
                                pack.tileExtract,
                            )
                        }

                    withContext(
                        Dispatchers.IO,
                    ) {
                        provider?.close()
                    }
                    provider =
                        replacement
                    providerIdentity =
                        pack.identity
                    replacement
                } else {
                    requireNotNull(provider)
                }

            activeProvider.route(
                request,
            )
        }

    suspend fun close() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                provider?.close()
            }
            provider = null
            providerIdentity = null
        }
    }
}

internal object VexMapsOfflineRoutingProcess {
    val planner =
        VexMapsOfflineRoutePlanner()
}

internal object VexMapsRouteIdentity {
    private val nextRevision =
        AtomicLong(0L)

    fun nextRevision(): Long =
        nextRevision.incrementAndGet()
}

// [VXG RealForever]
