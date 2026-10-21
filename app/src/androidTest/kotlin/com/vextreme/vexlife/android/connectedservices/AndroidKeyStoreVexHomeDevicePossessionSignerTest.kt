package com.vextreme.vexlife.android.connectedservices

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyFactory
import java.security.KeyStore
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeyStoreVexHomeDevicePossessionSignerTest {
    private val keyAlias =
        "vexlife.test.device-possession.${System.nanoTime()}"

    @After
    fun removeTestKey() {
        KeyStore.getInstance(
            AndroidKeyStoreVexHomeDevicePossessionSigner.ANDROID_KEY_STORE,
        ).apply {
            load(null)
            if (containsAlias(keyAlias)) {
                deleteEntry(keyAlias)
            }
        }
    }

    @Test
    fun androidKeyStoreKeyIsNonExportableAndSignsAcceptedCanonicalBytes() {
        val signer =
            AndroidKeyStoreVexHomeDevicePossessionSigner(
                context =
                    InstrumentationRegistry
                        .getInstrumentation()
                        .targetContext,
                keyAlias =
                    keyAlias,
                deviceRefStore =
                    FixedDeviceRefStore(
                        DEVICE_REF,
                    ),
            )
        val identity =
            signer.publicIdentity()
        val unsigned =
            challenge(
                identity,
            )
        val canonicalBytes =
            canonicalVexHomeDevicePossessionSigningBytes(
                unsigned,
            )
        val boundChallenge =
            unsigned.copy(
                signingPayloadSha256 =
                    sha256LowerHex(
                        canonicalBytes,
                    ),
            )
        val signed =
            signer.sign(
                boundChallenge,
            )

        assertEquals(
            DEVICE_REF,
            identity.deviceRef,
        )
        assertTrue(
            identity.devicePublicKeyFingerprintRef.startsWith(
                "fingerprint.sha256.",
            ),
        )
        assertEquals(
            boundChallenge.challengeRef,
            signed.challengeRef,
        )

        val publicBytes =
            Base64.decode(
                identity.devicePublicKeyBase64Url,
                AndroidKeyStoreVexHomeDevicePossessionSigner.BASE64URL_FLAGS,
            )
        val publicKey =
            KeyFactory
                .getInstance("EC")
                .generatePublic(
                    X509EncodedKeySpec(
                        publicBytes,
                    ),
                )
        val signature =
            Signature.getInstance(
                "SHA256withECDSA",
            ).apply {
                initVerify(
                    publicKey,
                )
                update(
                    canonicalBytes,
                )
            }
        val signatureBytes =
            Base64.decode(
                signed.signatureBytesBase64Url,
                AndroidKeyStoreVexHomeDevicePossessionSigner.BASE64URL_FLAGS,
            )
        assertTrue(
            signature.verify(
                signatureBytes,
            ),
        )

        val keyStore =
            KeyStore.getInstance(
                AndroidKeyStoreVexHomeDevicePossessionSigner.ANDROID_KEY_STORE,
            ).apply {
                load(null)
            }
        assertNull(
            keyStore.getKey(
                keyAlias,
                null,
            ).encoded,
        )
    }

    private fun challenge(
        identity: VexHomeDevicePublicIdentity,
    ) =
        VexHomeDevicePossessionChallenge(
            schemaVersion =
                VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA,
            challengeRef =
                "challenge.android.keystore.001",
            homeRef =
                "home.current",
            principalRef =
                "person.victor-gong",
            deviceRef =
                identity.deviceRef,
            membershipRef =
                "membership.android.current",
            membershipHash =
                "a".repeat(64),
            revocationGeneration =
                3,
            devicePublicKeyFingerprintRef =
                identity.devicePublicKeyFingerprintRef,
            nonceBase64Url =
                "AAECAwQFBgcICQ",
            issuedAtMs =
                1_800_000_000_000,
            expiresAtMs =
                1_800_000_060_000,
            signingPayloadSha256 =
                "0".repeat(64),
            credentialClass =
                VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS,
            signatureAlgorithmRef =
                VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF,
            devicePublicKeyEncoding =
                VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING,
        )

    private class FixedDeviceRefStore(
        private val deviceRef: String,
    ) : VexHomeDeviceRefStore {
        override fun currentOrCreate(): String =
            deviceRef
    }

    private companion object {
        const val DEVICE_REF =
            "device.android.instrumented"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][ANDROID-KEYSTORE-PROOF]