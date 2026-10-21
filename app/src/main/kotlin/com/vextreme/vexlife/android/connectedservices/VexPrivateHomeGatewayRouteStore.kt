package com.vextreme.vexlife.android.connectedservices

import android.content.Context

/**
 * App-private reachability memory for the last selected private Home gateway origin.
 *
 * The stored value is deliberately only an HTTPS route. It is not a paired-device membership,
 * authentication credential, Home authorization, capability lease, or proof that the route is
 * currently reachable. Every process rebuilds possession/session authentication through
 * [VexPrivateHomeGatewayConnection] before a feature request can cross the gateway.
 */
internal interface VexPrivateHomeGatewayRouteStore {
    fun currentEndpoint():
        VexPrivateHomeGatewayEndpoint?

    fun replaceEndpoint(
        endpoint:
            VexPrivateHomeGatewayEndpoint?,
    )
}

/**
 * Production route memory.
 *
 * A stale or malformed stored value fails closed as no route. The private Home endpoint itself
 * validates HTTPS/origin-only shape before it can be persisted.
 */
internal class SharedPreferencesVexPrivateHomeGatewayRouteStore(
    context: Context,
) : VexPrivateHomeGatewayRouteStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    override fun currentEndpoint():
        VexPrivateHomeGatewayEndpoint? =
        preferences
            .getString(
                HTTPS_ORIGIN_KEY,
                null,
            )
            ?.let { stored ->
                runCatching {
                    VexPrivateHomeGatewayEndpoint(
                        stored,
                    )
                }.getOrNull()
            }

    override fun replaceEndpoint(
        endpoint:
            VexPrivateHomeGatewayEndpoint?,
    ) {
        val editor =
            preferences.edit()

        if (endpoint == null) {
            editor.remove(
                HTTPS_ORIGIN_KEY,
            )
        } else {
            editor.putString(
                HTTPS_ORIGIN_KEY,
                endpoint.httpsOrigin,
            )
        }

        check(
            editor.commit(),
        ) {
            "Private Home gateway route could not be persisted"
        }
    }

    private companion object {
        const val PREFERENCES_NAME =
            "vexlife_connected_services_private_home_route"
        const val HTTPS_ORIGIN_KEY =
            "https_origin"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][PRIVATE-HOME-ROUTE]
