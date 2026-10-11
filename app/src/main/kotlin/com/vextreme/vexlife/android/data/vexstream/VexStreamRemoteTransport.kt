package com.vextreme.vexlife.android.data.vexstream

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Content-blind off-LAN transport for the already-paired VexStream mobile surface.
 *
 * The public relay sees routing metadata and signed device identity, but the inner request/response
 * (including the VexStream bearer and media bytes) is encrypted end-to-end to the desktop gateway
 * key learned during the locally approved pairing.
 */
internal class VexStreamRemoteTransport(
    private val connectionStore: VexStreamConnectionStore,
) {
    data class Response(
        val status: Int,
        val headers: Map<String, String>,
        val body: ByteArray,
    )

    fun execute(
        connection: VexStreamPairedConnection,
        method: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        require(connection.remoteReady) {
            "VexStream remote route is not configured"
        }

        val requestRef =
            "remote." + UUID.randomUUID().toString()
        val ephemeral =
            KeyPairGenerator.getInstance("EC")
                .apply {
                    initialize(
                        ECGenParameterSpec("secp256r1"),
                    )
                }
                .generateKeyPair()
        val gatewayPublic =
            KeyFactory.getInstance("EC")
                .generatePublic(
                    X509EncodedKeySpec(
                        decodeBase64(
                            requireNotNull(
                                connection.remoteGatewayPublicKey,
                            ),
                        ),
                    ),
                )
        val agreement =
            KeyAgreement.getInstance("ECDH")
                .apply {
                    init(ephemeral.private)
                    doPhase(
                        gatewayPublic,
                        true,
                    )
                }
        val key =
            remoteHkdf(
                shared = agreement.generateSecret(),
                requestRef = requestRef,
            )

        val inner =
            JSONObject()
                .put(
                    "method",
                    method.uppercase(),
                )
                .put(
                    "path",
                    "/" + path.trimStart('/'),
                )
                .put(
                    "token",
                    connection.token,
                )
                .put(
                    "headers",
                    JSONObject(
                        headers,
                    ),
                )
                .toString()
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        val homeRef =
            requireNotNull(
                connection.remoteHomeRef,
            )
        val deviceRef =
            connectionStore.deviceRef()
        val sealed =
            seal(
                key = key,
                plaintext = inner,
                aad =
                    requestAad(
                        homeRef = homeRef,
                        deviceRef = deviceRef,
                        requestRef = requestRef,
                    ),
            )
        val ephemeralPublicKey =
            encodeBase64(
                ephemeral.public.encoded,
            )
        val issuedAt =
            Instant.now().toString()
        val envelope =
            JSONObject()
                .put(
                    "schemaVersion",
                    SCHEMA,
                )
                .put(
                    "homeRef",
                    homeRef,
                )
                .put(
                    "deviceRef",
                    deviceRef,
                )
                .put(
                    "requestRef",
                    requestRef,
                )
                .put(
                    "issuedAt",
                    issuedAt,
                )
                .put(
                    "ephemeralPublicKey",
                    ephemeralPublicKey,
                )
                .put(
                    "nonce",
                    sealed.first,
                )
                .put(
                    "ciphertext",
                    sealed.second,
                )

        envelope.put(
            "signature",
            connectionStore.signRemotePayload(
                requestCanonical(
                    homeRef = homeRef,
                    deviceRef = deviceRef,
                    requestRef = requestRef,
                    issuedAt = issuedAt,
                    ephemeralPublicKey = ephemeralPublicKey,
                    nonce = sealed.first,
                    ciphertext = sealed.second,
                ),
            ),
        )

        val queued =
            relayJson(
                baseUrl =
                    requireNotNull(
                        connection.remoteRelayBaseUrl,
                    ),
                path = "v1/remote/request",
                body = envelope,
            )
        if (
            queued.first !=
            HttpURLConnection.HTTP_ACCEPTED
        ) {
            throw IOException(
                "VexStream remote relay rejected request with HTTP ${queued.first}",
            )
        }

        val deadline =
            System.currentTimeMillis() +
                RESPONSE_TIMEOUT_MS
        while (
            System.currentTimeMillis() <
            deadline
        ) {
            val pollIssuedAt =
                Instant.now().toString()
            val poll =
                JSONObject()
                    .put(
                        "schemaVersion",
                        SCHEMA,
                    )
                    .put(
                        "homeRef",
                        homeRef,
                    )
                    .put(
                        "deviceRef",
                        deviceRef,
                    )
                    .put(
                        "requestRef",
                        requestRef,
                    )
                    .put(
                        "issuedAt",
                        pollIssuedAt,
                    )
            poll.put(
                "signature",
                connectionStore.signRemotePayload(
                    pollCanonical(
                        homeRef = homeRef,
                        deviceRef = deviceRef,
                        requestRef = requestRef,
                        issuedAt = pollIssuedAt,
                    ),
                ),
            )

            val polled =
                relayJson(
                    baseUrl =
                        requireNotNull(
                            connection.remoteRelayBaseUrl,
                        ),
                    path = "v1/remote/response",
                    body = poll,
                )
            when (polled.first) {
                HttpURLConnection.HTTP_OK -> {
                    val outer =
                        JSONObject(
                            polled.second,
                        )
                    check(
                        outer.getString(
                            "requestRef",
                        ) == requestRef,
                    ) {
                        "remote response request identity changed"
                    }
                    val plaintext =
                        open(
                            key = key,
                            nonce =
                                outer.getString(
                                    "nonce",
                                ),
                            ciphertext =
                                outer.getString(
                                    "ciphertext",
                                ),
                            aad =
                                responseAad(
                                    homeRef,
                                    requestRef,
                                ),
                        )
                    val response =
                        JSONObject(
                            String(
                                plaintext,
                                StandardCharsets.UTF_8,
                            ),
                        )
                    val responseHeaders =
                        mutableMapOf<String, String>()
                    response
                        .optJSONObject(
                            "headers",
                        )
                        ?.let { json ->
                            json.keys()
                                .forEach { name ->
                                    responseHeaders[name] =
                                        json.getString(
                                            name,
                                        )
                                }
                        }

                    return Response(
                        status =
                            response.getInt(
                                "status",
                            ),
                        headers =
                            responseHeaders,
                        body =
                            response.optString(
                                "body",
                            ).takeIf {
                                it.isNotBlank()
                            }?.let(
                                ::decodeBase64,
                            ) ?: ByteArray(0),
                    )
                }

                HttpURLConnection.HTTP_ACCEPTED -> {
                    Thread.sleep(
                        POLL_RETRY_MS,
                    )
                }

                else ->
                    throw IOException(
                        "VexStream remote response failed with HTTP ${polled.first}",
                    )
            }
        }

        throw IOException(
            "VexStream remote response timed out",
        )
    }

    fun openRangeStream(
        connection: VexStreamPairedConnection,
        path: String,
        start: Long = 0L,
        expectedLength: Long? = null,
    ): InputStream =
        RemoteRangeInputStream(
            fetch = { from, count ->
                val to =
                    from +
                        count -
                        1L
                execute(
                    connection = connection,
                    method = "GET",
                    path = path,
                    headers =
                        mapOf(
                            "Range" to
                                "bytes=$from-$to",
                        ),
                )
            },
            start = start,
            expectedLength = expectedLength,
        )

    private fun relayJson(
        baseUrl: String,
        path: String,
        body: JSONObject,
    ): Pair<Int, String> {
        val connection =
            URL(
                baseUrl.trimEnd('/') +
                    "/" +
                    path.trimStart('/'),
            ).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout =
            RELAY_CONNECT_TIMEOUT_MS
        connection.readTimeout =
            RELAY_READ_TIMEOUT_MS
        connection.useCaches = false
        connection.doOutput = true
        connection.setRequestProperty(
            "Content-Type",
            "application/json",
        )
        connection.outputStream
            .bufferedWriter()
            .use {
                it.write(
                    body.toString(),
                )
            }

        val status =
            connection.responseCode
        val input =
            if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
        val text =
            input
                ?.bufferedReader()
                ?.use {
                    it.readText()
                }
                .orEmpty()
        connection.disconnect()
        return status to text
    }

    private class RemoteRangeInputStream(
        private val fetch: (
            Long,
            Long,
        ) -> Response,
        start: Long,
        expectedLength: Long?,
    ) : InputStream() {
        private var position = start
        private val endExclusive =
            expectedLength
                ?.let {
                    start + it
                }
        private var current =
            ByteArrayInputStream(
                ByteArray(0),
            )
        private var exhausted = false

        override fun read(): Int {
            val one =
                ByteArray(1)
            val read =
                read(
                    one,
                    0,
                    1,
                )
            return if (read < 0) {
                -1
            } else {
                one[0].toInt() and 0xff
            }
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (length == 0) {
                return 0
            }
            if (exhausted) {
                return -1
            }

            while (
                current.available() == 0
            ) {
                val remaining =
                    endExclusive
                        ?.minus(
                            position,
                        )
                if (
                    remaining != null &&
                    remaining <= 0L
                ) {
                    exhausted = true
                    return -1
                }

                val chunkSize =
                    minOf(
                        REMOTE_CHUNK_BYTES.toLong(),
                        remaining
                            ?: REMOTE_CHUNK_BYTES
                                .toLong(),
                    )
                val response =
                    fetch(
                        position,
                        chunkSize,
                    )
                if (
                    response.status !=
                        HttpURLConnection.HTTP_PARTIAL &&
                    response.status !=
                        HttpURLConnection.HTTP_OK
                ) {
                    throw IOException(
                        "remote VexStream media request failed with HTTP ${response.status}",
                    )
                }
                if (
                    response.body.isEmpty()
                ) {
                    exhausted = true
                    return -1
                }

                position +=
                    response.body.size
                current =
                    ByteArrayInputStream(
                        response.body,
                    )
                if (
                    response.body.size <
                    chunkSize
                ) {
                    // The final short range still remains readable from current; the next refill
                    // terminates rather than fabricating bytes.
                    if (
                        endExclusive ==
                        null
                    ) {
                        exhausted =
                            position >
                                startPosition &&
                                current.available() == 0
                    }
                }
            }

            return current.read(
                buffer,
                offset,
                length,
            )
        }

        private val startPosition =
            start
    }

    private fun remoteHkdf(
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

    private fun seal(
        key: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray,
    ): Pair<String, String> {
        val nonce =
            ByteArray(
                GCM_NONCE_BYTES,
            ).also {
                SecureRandom().nextBytes(
                    it,
                )
            }
        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding",
            )
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(
                key,
                "AES",
            ),
            GCMParameterSpec(
                GCM_TAG_BITS,
                nonce,
            ),
        )
        cipher.updateAAD(aad)
        val ciphertext =
            cipher.doFinal(
                plaintext,
            )
        return encodeBase64(
            nonce,
        ) to encodeBase64(
            ciphertext,
        )
    }

    private fun open(
        key: ByteArray,
        nonce: String,
        ciphertext: String,
        aad: ByteArray,
    ): ByteArray {
        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding",
            )
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(
                key,
                "AES",
            ),
            GCMParameterSpec(
                GCM_TAG_BITS,
                decodeBase64(
                    nonce,
                ),
            ),
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(
            decodeBase64(
                ciphertext,
            ),
        )
    }

    private fun requestCanonical(
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

    private fun pollCanonical(
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

    private fun requestAad(
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

    private fun responseAad(
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

    private fun encodeBase64(
        bytes: ByteArray,
    ): String =
        Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or
                Base64.NO_PADDING,
        )

    private fun decodeBase64(
        value: String,
    ): ByteArray =
        Base64.decode(
            value,
            Base64.NO_WRAP or
                Base64.NO_PADDING,
        )

    private fun ByteArray.toHex(): String =
        joinToString(
            separator = "",
        ) { byte ->
            "%02x".format(byte)
        }

    private companion object {
        const val SCHEMA =
            "vexstream.remote-relay/v1"
        const val REMOTE_CHUNK_BYTES =
            256 * 1024
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val RELAY_CONNECT_TIMEOUT_MS =
            5_000
        const val RELAY_READ_TIMEOUT_MS =
            35_000
        const val RESPONSE_TIMEOUT_MS =
            60_000L
        const val POLL_RETRY_MS = 250L
    }
}

// [VXG RealForever]
