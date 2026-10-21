package com.vextreme.vexlife.android.connectedservices

import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in host bridge for the VCS-04 lived private-route proof.
 *
 * The bridge may persist one already-selected private HTTPS origin and returns only the public
 * Android device identity required by the accepted Home pairing semantic owner. It never serializes
 * private-key bytes, a bearer, membership authority, or a capability lease.
 */
class VexPrivateHomeGatewayHostBridgeTest {
    @Test
    fun seedPrivateRouteAndExposePublicDeviceIdentity() {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()
        val arguments =
            InstrumentationRegistry
                .getArguments()

        assumeTrue(
            "VCS-04 host bridge is opt-in",
            arguments.getString(
                ARG_ENABLE,
            ) == "1",
        )

        val origin =
            requireNotNull(
                arguments.getString(
                    ARG_HTTPS_ORIGIN,
                ),
            ) {
                "VCS-04 host bridge requires $ARG_HTTPS_ORIGIN"
            }
        val endpoint =
            VexPrivateHomeGatewayEndpoint(
                httpsOrigin =
                    origin,
            )
        val context =
            instrumentation
                .targetContext
        val routeStore =
            VexPrivateHomeGatewayRouteStore(
                context,
            )

        routeStore.save(
            endpoint,
        )

        val identity =
            AndroidKeyStoreVexHomeDevicePossessionSigner(
                context,
            ).publicIdentity()
        val identityJson =
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
                .toString()

        assertEquals(
            endpoint,
            routeStore.currentEndpoint(),
        )
        assertTrue(
            identity
                .devicePublicKeyFingerprintRef
                .startsWith(
                    "fingerprint.sha256.",
                ),
        )

        instrumentation.sendStatus(
            0,
            Bundle().apply {
                putString(
                    STATUS_PUBLIC_IDENTITY,
                    identityJson,
                )
                putString(
                    STATUS_HTTPS_ORIGIN,
                    endpoint.httpsOrigin,
                )
            },
        )
        Log.i(
            LOG_TAG,
            STATUS_PUBLIC_IDENTITY + "=" + identityJson + " " +
                STATUS_HTTPS_ORIGIN + "=" + endpoint.httpsOrigin,
        )
    }

    private companion object {
        const val ARG_ENABLE =
            "vexPrivateHomeHostBridge"
        const val ARG_HTTPS_ORIGIN =
            "vexPrivateHomeHttpsOrigin"
        const val STATUS_PUBLIC_IDENTITY =
            "vexPrivateHomePublicIdentity"
        const val STATUS_HTTPS_ORIGIN =
            "vexPrivateHomeHttpsOrigin"
        const val LOG_TAG =
            "VexPrivateHomeHostBridge"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][HOST-BRIDGE]
