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
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Off-LAN transport for the already-paired VexStream mobile surface.
 *
 * A direct public route to the paired Home PC is preferred when available. The request remains
 * signed by the Android device identity and end-to-end encrypted to the desktop gateway key. A
 * content-blind VexRelay route remains fallback when direct reachability is unavailable.
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
            VexStreamRemoteProtocol.hkdf(
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
                    VexStreamRemoteProtocol.requestAad(
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
            rfc3339Now()
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
                VexStreamRemoteProtocol.requestCanonical(
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

        for (
            directBaseUrl in
                connection.remoteDirectBaseUrls
        ) {
            val direct =
                runCatching {
                    directJson(
                        connection = connection,
                        baseUrl = directBaseUrl,
                        body = envelope,
                    )
                }.getOrNull()
                    ?: continue

            if (
                direct.first ==
                HttpURLConnection.HTTP_OK
            ) {
                return decodeRemoteResponse(
                    key = key,
                    homeRef = homeRef,
                    requestRef = requestRef,
                    body = direct.second,
                )
            }
        }

        val relayBaseUrl =
            connection.remoteRelayBaseUrl
                ?: throw IOException(
                    "VexStream direct routes were unreachable and no relay fallback is configured",
                )

        val queued =
            relayJson(
                baseUrl = relayBaseUrl,
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
                rfc3339Now()
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
                    VexStreamRemoteProtocol.pollCanonical(
                        homeRef = homeRef,
                        deviceRef = deviceRef,
                        requestRef = requestRef,
                        issuedAt = pollIssuedAt,
                    ),
                ),
            )

            val polled =
                relayJson(
                    baseUrl = relayBaseUrl,
                    path = "v1/remote/response",
                    body = poll,
                )
            when (polled.first) {
                HttpURLConnection.HTTP_OK ->
                    return decodeRemoteResponse(
                        key = key,
                        homeRef = homeRef,
                        requestRef = requestRef,
                        body = polled.second,
                    )

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

    private fun directJson(
        connection: VexStreamPairedConnection,
        baseUrl: String,
        body: JSONObject,
    ): Pair<Int, String> {
        val request =
            VexStreamPinnedTls.open(
                baseUrl = baseUrl,
                certificateSha256 =
                    connection.certificateSha256,
                path = "v1/direct/request",
                method = "POST",
            )
        request.connectTimeout =
            DIRECT_CONNECT_TIMEOUT_MS
        request.readTimeout =
            DIRECT_READ_TIMEOUT_MS
        request.doOutput = true
        request.setRequestProperty(
            "Content-Type",
            "application/json",
        )
        request.outputStream
            .bufferedWriter()
            .use {
                it.write(
                    body.toString(),
                )
            }

        val status =
            request.responseCode
        val text =
            VexStreamPinnedTls.readText(
                request,
            )
        request.disconnect()
        return status to text
    }

    private fun decodeRemoteResponse(
        key: ByteArray,
        homeRef: String,
        requestRef: String,
        body: String,
    ): Response {
        val outer =
            JSONObject(
                body,
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
                    VexStreamRemoteProtocol.responseAad(
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
        private var endExclusive =
            expectedLength
                ?.let {
                    start + it
                }
        private var current =
            ByteArrayInputStream(
                ByteArray(0),
            )
        private var exhausted = false
        private var finalChunk = false

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
                if (finalChunk) {
                    exhausted = true
                    return -1
                }

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

                if (endExclusive == null) {
                    response.headers
                        .entries
                        .firstOrNull {
                            it.key.equals(
                                "Content-Range",
                                ignoreCase = true,
                            )
                        }
                        ?.value
                        ?.substringAfterLast(
                            "/",
                        )
                        ?.takeIf {
                            it != "*"
                        }
                        ?.toLongOrNull()
                        ?.takeIf {
                            it >= 0L
                        }
                        ?.let {
                            endExclusive = it
                        }
                }

                position +=
                    response.body.size
                current =
                    ByteArrayInputStream(
                        response.body,
                    )
                if (
                    response.body.size.toLong() <
                    chunkSize
                ) {
                    // Preserve this final short range for the caller; after it is consumed, the
                    // next refill terminates rather than inventing another request past EOF.
                    finalChunk = true
                }
            }

            return current.read(
                buffer,
                offset,
                length,
            )
        }

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

        return encodeBase64(
            nonce,
        ) to encodeBase64(
            cipher.doFinal(
                plaintext,
            ),
        )
    }

    private fun open(
        key: ByteArray,
        nonce: String,
        ciphertext: String,
        aad: ByteArray,
    ): ByteArray {
        val nonceBytes =
            decodeBase64(
                nonce,
            )
        require(
            nonceBytes.size ==
                GCM_NONCE_BYTES,
        ) {
            "invalid remote nonce"
        }

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
                nonceBytes,
            ),
        )
        cipher.updateAAD(aad)

        return cipher.doFinal(
            decodeBase64(
                ciphertext,
            ),
        )
    }

    private fun rfc3339Now(): String =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.US,
        ).apply {
            timeZone =
                TimeZone.getTimeZone(
                    "UTC",
                )
        }.format(
            Date(),
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

    private companion object {
        const val SCHEMA =
            "vexstream.remote-relay/v1"
        const val REMOTE_CHUNK_BYTES =
            256 * 1024
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val DIRECT_CONNECT_TIMEOUT_MS =
            3_000
        const val DIRECT_READ_TIMEOUT_MS =
            35_000
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
