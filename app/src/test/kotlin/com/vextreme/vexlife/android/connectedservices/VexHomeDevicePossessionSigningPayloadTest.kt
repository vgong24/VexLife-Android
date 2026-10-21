package com.vextreme.vexlife.android.connectedservices

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VexHomeDevicePossessionSigningPayloadTest {
    @Test
    fun acceptedSdkFixedVectorMatchesExactly() {
        val challenge =
            fixedChallenge()
        val bytes =
            canonicalVexHomeDevicePossessionSigningBytes(
                challenge,
            )

        assertEquals(
            585,
            bytes.size,
        )
        assertEquals(
            "16a22f6bbd4a7fababfa0e220b5b7c5d4d86182e12ccb1f89f3e7da78adc0bba",
            sha256LowerHex(
                bytes,
            ),
        )
        assertEquals(
            '\n'.code.toByte(),
            bytes.last(),
        )
    }

    @Test
    fun canonicalBytesAreStableForTheExactAcceptedFieldOrder() {
        val bytes =
            canonicalVexHomeDevicePossessionSigningBytes(
                fixedChallenge(),
            )
        val expected =
            """{"challengeRef":"challenge.home-possession.fixture.001","devicePublicKeyFingerprintRef":"fingerprint.sha256.${"b".repeat(64)}","deviceRef":"device.android.fixture","expiresAtMs":1760000300000,"homeRef":"home.fixture","issuedAtMs":1760000000000,"membershipHash":"${"a".repeat(64)}","membershipRef":"membership.fixture","nonceBase64Url":"AAECAwQFBgcICQ","principalRef":"principal.fixture","revocationGeneration":7,"schemaVersion":"vextreme.security.home-device-possession-signing-payload/v1"}
"""
                .toByteArray(
                    Charsets.UTF_8,
                )

        assertArrayEquals(
            expected,
            bytes,
        )
    }

    @Test
    fun invalidAcceptedIdentityFactsFailBeforeSigningBytesExist() {
        assertThrows(
            IllegalArgumentException::class.java,
        ) {
            canonicalVexHomeDevicePossessionSigningBytes(
                fixedChallenge()
                    .copy(
                        devicePublicKeyFingerprintRef =
                            "b".repeat(64),
                    ),
            )
        }
        assertThrows(
            IllegalArgumentException::class.java,
        ) {
            canonicalVexHomeDevicePossessionSigningBytes(
                fixedChallenge()
                    .copy(
                        expiresAtMs =
                            1760000000000,
                    ),
            )
        }
    }

    private fun fixedChallenge() =
        VexHomeDevicePossessionChallenge(
            schemaVersion =
                VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA,
            challengeRef =
                "challenge.home-possession.fixture.001",
            homeRef =
                "home.fixture",
            principalRef =
                "principal.fixture",
            deviceRef =
                "device.android.fixture",
            membershipRef =
                "membership.fixture",
            membershipHash =
                "a".repeat(64),
            revocationGeneration =
                7,
            devicePublicKeyFingerprintRef =
                "fingerprint.sha256.${"b".repeat(64)}",
            nonceBase64Url =
                "AAECAwQFBgcICQ",
            issuedAtMs =
                1760000000000,
            expiresAtMs =
                1760000300000,
            signingPayloadSha256 =
                "16a22f6bbd4a7fababfa0e220b5b7c5d4d86182e12ccb1f89f3e7da78adc0bba",
            credentialClass =
                VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS,
            signatureAlgorithmRef =
                VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF,
            devicePublicKeyEncoding =
                VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING,
        )
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][DEVICE-POSSESSION-PROOF]