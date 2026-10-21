package com.vextreme.vexlife.android.connectedservices

import android.content.Context

/**
 * App-private persistence for the last selected private Home HTTPS origin.
 *
 * This is reachability only. Presence on disk does not authenticate the device, create membership,
 * grant Home authority, retain a bearer, or prove that the route is currently reachable.
 */
internal class VexPrivateHomeGatewayRouteStore(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun currentEndpoint():
        VexPrivateHomeGatewayEndpoint? =
        preferences
            .getString(
                KEY_HTTPS_ORIGIN,
                null,
            )
            ?.takeIf { origin ->
                origin.isNotBlank()
            }
            ?.let { origin ->
                runCatching {
                    VexPrivateHomeGatewayEndpoint(
                        httpsOrigin = origin,
                    )
                }.getOrNull()
            }

    fun save(
        endpoint:
            VexPrivateHomeGatewayEndpoint,
    ) {
        check(
            preferences
                .edit()
                .putString(
                    KEY_HTTPS_ORIGIN,
                    endpoint.httpsOrigin,
                )
                .commit(),
        ) {
            "Private Home gateway route could not be persisted"
        }
    }

    fun clear() {
        check(
            preferences
                .edit()
                .remove(
                    KEY_HTTPS_ORIGIN,
                )
                .commit(),
        ) {
            "Private Home gateway route could not be cleared"
        }
    }

    private companion object {
        const val PREFERENCES_NAME =
            "vexlife-connected-services-private-home-route"
        const val KEY_HTTPS_ORIGIN =
            "httpsOrigin"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][PRIVATE-HOME-ROUTE]
