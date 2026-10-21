package com.vextreme.vexlife.android.connectedservices

import android.os.Bundle
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Host-only bridge for the VCS-04 lived private-route proof.
 *
 * Ordinary device verification skips this test. An explicitly admitted host invocation may ask it
 * to expose only the Android device's PUBLIC possession identity, seed one validated private HTTPS
 * route, or clear that route. The AndroidKeyStore private key is never returned or serialized.
 */
class VexPrivateHomeGatewayHostBridgeTest {
    @Test
    fun hostBridgeExportsOnlyPublicIdentityAndOptionallyUpdatesReachability() {
        val arguments =
            InstrumentationRegistry.getArguments()
        val action =
            arguments.getString(
                ARG_ACTION,
            )

        assumeTrue(
            "VCS-04 host bridge runs only under an explicit host-proof action",
            action != null,
        )

        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val routeStore =
            SharedPreferencesVexPrivateHomeGatewayRouteStore(
                context,
            )

        when (action) {
            ACTION_EXPORT_IDENTITY -> Unit

            ACTION_SEED_ROUTE -> {
                val httpsOrigin =
                    requireNotNull(
                        arguments.getString(
                            ARG_HTTPS_ORIGIN,
                        ),
                    ) {
                        "seed-route requires an HTTPS origin"
                    }
                val endpoint =
                    VexPrivateHomeGatewayEndpoint(
                        httpsOrigin,
                    )
                routeStore.replaceEndpoint(
                    endpoint,
                )
                assertEquals(
                    endpoint,
                    routeStore.currentEndpoint(),
                )
            }

            ACTION_CLEAR_ROUTE -> {
                routeStore.replaceEndpoint(
                    null,
                )
                assertEquals(
                    null,
                    routeStore.currentEndpoint(),
                )
            }

            else ->
                error(
                    "Unsupported VCS-04 host bridge action: $action",
                )
        }

        val identity =
            AndroidKeyStoreVexHomeDevicePossessionSigner(
                context,
            ).publicIdentity()
        val payload =
            JSONObject()
                .put(
                    "deviceRef",
                    identity.deviceRef,
                )
                .put(
                    "devicePublicKeyBase64Url",
                    identity.devicePublicKeyBase64Url,
                )
                .put(
                    "devicePublicKeyFingerprintRef",
                    identity.devicePublicKeyFingerprintRef,
                )
                .put(
                    "privateKeyExported",
                    false,
                )
                .put(
                    "routeHttpsOrigin",
                    routeStore
                        .currentEndpoint()
                        ?.httpsOrigin
                        ?: JSONObject.NULL,
                )
                .toString()

        val encoded =
            Base64.encodeToString(
                payload.toByteArray(
                    StandardCharsets.UTF_8,
                ),
                Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP,
            )
        val status =
            Bundle().apply {
                putString(
                    RESULT_PUBLIC_IDENTITY_BASE64URL,
                    encoded,
                )
            }
        InstrumentationRegistry
            .getInstrumentation()
            .sendStatus(
                0,
                status,
            )
    }

    companion object {
        const val ARG_ACTION =
            "vexPrivateHomeGatewayHostAction"
        const val ARG_HTTPS_ORIGIN =
            "vexPrivateHomeGatewayHttpsOrigin"
        const val ACTION_EXPORT_IDENTITY =
            "export-identity"
        const val ACTION_SEED_ROUTE =
            "seed-route"
        const val ACTION_CLEAR_ROUTE =
            "clear-route"
        const val RESULT_PUBLIC_IDENTITY_BASE64URL =
            "vexHomeDevicePublicIdentityBase64Url"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][VCS-04][HOST-BRIDGE]
