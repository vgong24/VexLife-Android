package com.vextreme.vexlife.android.data.vexstream

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure protocol bytes shared by the Android remote transport and JVM regression tests.
 *
 * Keep these operations Android-free so cross-repository vectors can prove that the phone and
 * desktop derive/sign exactly the same bytes before lived network proof.
 */
internal object VexStreamRemoteProtocol {
    fun hkdf(
        shared: ByteArray,
        requestRef: String,
    ): ByteArray {
        val salt =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    "vexstream.remote/v1"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
        val extract =
            Mac.getInstance(
                "HmacSHA256",
            )
                .apply {
                    init(
                        SecretKeySpec(
                            salt,
                            "HmacSHA256",
                        ),
                    )
                }
                .doFinal(shared)

        return Mac.getInstance(
            "HmacSHA256",
        )
            .apply {
                init(
                    SecretKeySpec(
                        extract,
                        "HmacSHA256",
                    ),
                )
                update(
                    requestRef.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                )
                update(
                    byteArrayOf(1),
                )
            }
            .doFinal()
    }

    fun requestCanonical(
        homeRef: String,
        deviceRef: String,
        requestRef: String,
        issuedAt: String,
        ephemeralPublicKey: String,
        nonce: String,
        ciphertext: String,
    ): ByteArray {
        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    ciphertext.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                )
                .toHex()

        return listOf(
            "VEXSTREAM_REMOTE_REQUEST_V1",
            homeRef,
            deviceRef,
            requestRef,
            issuedAt,
            ephemeralPublicKey,
            nonce,
            digest,
        ).joinToString(
            "\n",
        ).toByteArray(
            StandardCharsets.UTF_8,
        )
    }

    fun pollCanonical(
        homeRef: String,
        deviceRef: String,
        requestRef: String,
        issuedAt: String,
    ): ByteArray =
        listOf(
            "VEXSTREAM_REMOTE_POLL_V1",
            homeRef,
            deviceRef,
            requestRef,
            issuedAt,
        ).joinToString(
            "\n",
        ).toByteArray(
            StandardCharsets.UTF_8,
        )

    fun requestAad(
        homeRef: String,
        deviceRef: String,
        requestRef: String,
    ): ByteArray =
        listOf(
            "VEXSTREAM_REMOTE_REQUEST_AAD_V1",
            homeRef,
            deviceRef,
            requestRef,
        ).joinToString(
            "\n",
        ).toByteArray(
            StandardCharsets.UTF_8,
        )

    fun responseAad(
        homeRef: String,
        requestRef: String,
    ): ByteArray =
        listOf(
            "VEXSTREAM_REMOTE_RESPONSE_AAD_V1",
            homeRef,
            requestRef,
        ).joinToString(
            "\n",
        ).toByteArray(
            StandardCharsets.UTF_8,
        )

    private fun ByteArray.toHex(): String =
        joinToString(
            separator = "",
        ) { byte ->
            "%02x".format(byte)
        }
}

// [VXG RealForever]
