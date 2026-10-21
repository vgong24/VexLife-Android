package com.vextreme.vexlife.android.connectedservices

import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * One runtime-selected private HTTPS origin for the accepted Home gateway.
 *
 * The route is reachability only. Supplying an origin does not authenticate the device, grant Home
 * authority, or persist that route as trusted identity. No endpoint is hard-coded in repository source.
 */
internal data class VexPrivateHomeGatewayEndpoint(
    val httpsOrigin: String,
) {
    private val uri =
        URI(
            httpsOrigin,
        )

    init {
        require(
            uri.scheme == "https",
        ) {
            "Private Home gateway requires HTTPS"
        }
        require(
            uri.host != null &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null &&
                (uri.path.isNullOrEmpty() || uri.path == "/"),
        ) {
            "Private Home gateway endpoint must be one HTTPS origin without path/query/user info"
        }
        require(
            uri.port == -1 ||
                uri.port in 1..65535,
        ) {
            "Private Home gateway endpoint port is invalid"
        }
    }

    internal fun url(
        absolutePath: String,
    ): URL {
        require(
            absolutePath.startsWith('/') &&
                !absolutePath.contains('?') &&
                !absolutePath.contains('#'),
        ) {
            "Private Home gateway request path must be one absolute path without query or fragment"
        }

        return URI(
            uri.scheme,
            null,
            uri.host,
            uri.port,
            absolutePath,
            null,
            null,
        ).toURL()
    }
}

internal enum class VexPrivateHomeGatewayFailureKind {
    TransportUnavailable,
    TransportFailed,
    SessionUnavailable,
    AuthorizationDenied,
    ServiceUnavailable,
}

internal class VexPrivateHomeGatewayException(
    val kind: VexPrivateHomeGatewayFailureKind,
    val statusCode: Int? = null,
    val serverCode: String? = null,
    cause: Throwable? = null,
) : IllegalStateException(
    listOfNotNull(
        "Private Home gateway ${kind.name}",
        statusCode?.let { "HTTP $it" },
        serverCode,
    ).joinToString(separator = ": "),
    cause,
)

/**
 * Internal Android HTTP realization of the accepted private Home gateway.
 *
 * This is an implementation seam, not a product-level generic API contract. Feature-facing ports keep
 * their own semantic request/response vocabulary and never receive bearer/session/header values.
 */
internal interface VexPrivateHomeGatewayHttpTransport {
    suspend fun getJson(
        absolutePath: String,
        bearerSessionRef: String? = null,
    ): String

    suspend fun postJson(
        absolutePath: String,
        body: String,
        bearerSessionRef: String? = null,
    ): String
}

internal class HttpsVexPrivateHomeGatewayHttpTransport(
    private val endpoint: VexPrivateHomeGatewayEndpoint,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 15_000,
) : VexPrivateHomeGatewayHttpTransport {
    init {
        require(connectTimeoutMillis > 0)
        require(readTimeoutMillis > 0)
    }

    override suspend fun getJson(
        absolutePath: String,
        bearerSessionRef: String?,
    ): String =
        execute(
            method = "GET",
            absolutePath = absolutePath,
            body = null,
            bearerSessionRef = bearerSessionRef,
        )

    override suspend fun postJson(
        absolutePath: String,
        body: String,
        bearerSessionRef: String?,
    ): String =
        execute(
            method = "POST",
            absolutePath = absolutePath,
            body = body,
            bearerSessionRef = bearerSessionRef,
        )

    private suspend fun execute(
        method: String,
        absolutePath: String,
        body: String?,
        bearerSessionRef: String?,
    ): String =
        withContext(
            Dispatchers.IO,
        ) {
            if (bearerSessionRef != null) {
                require(
                    SESSION_REF.matches(
                        bearerSessionRef,
                    ),
                ) {
                    "Private Home gateway bearer session ref is malformed"
                }
            }

            val connection =
                endpoint
                    .url(
                        absolutePath,
                    )
                    .openConnection() as HttpsURLConnection
            try {
                connection.requestMethod =
                    method
                connection.instanceFollowRedirects =
                    false
                connection.connectTimeout =
                    connectTimeoutMillis
                connection.readTimeout =
                    readTimeoutMillis
                connection.setRequestProperty(
                    "Accept",
                    "application/json",
                )
                connection.setRequestProperty(
                    "Cache-Control",
                    "no-store",
                )
                bearerSessionRef
                    ?.let { sessionRef ->
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $sessionRef",
                        )
                    }

                if (body != null) {
                    val bytes =
                        body.toByteArray(
                            StandardCharsets.UTF_8,
                        )
                    connection.doOutput =
                        true
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json",
                    )
                    connection.setFixedLengthStreamingMode(
                        bytes.size,
                    )
                    connection.outputStream.use { output ->
                        output.write(
                            bytes,
                        )
                    }
                }

                val status =
                    connection.responseCode
                val responseText =
                    try {
                        val stream =
                            if (status in 200..299) {
                                connection.inputStream
                            } else {
                                connection.errorStream
                            }
                        stream
                            ?.bufferedReader(
                                StandardCharsets.UTF_8,
                            )
                            ?.use { reader ->
                                reader.readText()
                            }
                            .orEmpty()
                    } catch (error: IOException) {
                        if (status in 200..299) {
                            throw error
                        }
                        ""
                    }

                if (status !in 200..299) {
                    val serverCode =
                        responseText
                            .takeIf(String::isNotBlank)
                            ?.let { text ->
                                runCatching {
                                    JSONObject(
                                        text,
                                    ).optString(
                                        "code",
                                    )
                                }.getOrNull()
                            }
                            ?.takeIf(String::isNotBlank)
                    throw VexPrivateHomeGatewayException(
                        kind =
                            when (status) {
                                401 ->
                                    VexPrivateHomeGatewayFailureKind.SessionUnavailable
                                403 ->
                                    VexPrivateHomeGatewayFailureKind.AuthorizationDenied
                                502,
                                503,
                                504,
                                ->
                                    VexPrivateHomeGatewayFailureKind.ServiceUnavailable
                                else ->
                                    VexPrivateHomeGatewayFailureKind.TransportFailed
                            },
                        statusCode =
                            status,
                        serverCode =
                            serverCode,
                    )
                }

                val contentType =
                    connection.contentType
                        ?.substringBefore(';')
                        ?.trim()
                        ?.lowercase()
                require(
                    contentType == null ||
                        contentType == "application/json",
                ) {
                    "Private Home gateway success response must be JSON"
                }
                responseText
            } catch (error: VexPrivateHomeGatewayException) {
                throw error
            } catch (error: IOException) {
                throw VexPrivateHomeGatewayException(
                    kind =
                        VexPrivateHomeGatewayFailureKind.TransportUnavailable,
                    cause =
                        error,
                )
            } finally {
                connection.disconnect()
            }
        }
}

/**
 * Session-bound transport consumer of the accepted Home gateway challenge/session contract.
 *
 * The current bearer exists only in memory. Expiry may trigger a fresh possession proof for the same
 * already-paired device, but a route/session failure never re-pairs, changes deviceRef, or manufactures
 * Home authority.
 */
internal class VexPrivateHomeGatewayConnection(
    private val transport: VexPrivateHomeGatewayHttpTransport,
    private val signer: VexHomeDevicePossessionSigner,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    private data class Session(
        val sessionRef: String,
        val expiresAtEpochMillis: Long,
    )

    private val sessionMutex =
        Mutex()
    private var currentSession:
        Session? = null

    suspend fun authenticatedGetJson(
        absolutePath: String,
    ): String {
        val session =
            currentSession()
        return try {
            transport.getJson(
                absolutePath =
                    absolutePath,
                bearerSessionRef =
                    session.sessionRef,
            )
        } catch (error: VexPrivateHomeGatewayException) {
            if (error.statusCode == 401) {
                invalidate(
                    session,
                )
            }
            throw error
        }
    }

    suspend fun authenticatedPostJson(
        absolutePath: String,
        body: String,
    ): String {
        val session =
            currentSession()
        return try {
            transport.postJson(
                absolutePath =
                    absolutePath,
                body =
                    body,
                bearerSessionRef =
                    session.sessionRef,
            )
        } catch (error: VexPrivateHomeGatewayException) {
            if (error.statusCode == 401) {
                invalidate(
                    session,
                )
            }
            throw error
        }
    }

    fun publicDeviceIdentity(): VexHomeDevicePublicIdentity =
        signer.publicIdentity()

    private suspend fun currentSession(): Session =
        sessionMutex.withLock {
            val now =
                nowEpochMillis()
            currentSession
                ?.takeIf { session ->
                    now + SESSION_EXPIRY_SKEW_MILLIS <
                        session.expiresAtEpochMillis
                }
                ?: establishSession()
                    .also { established ->
                        currentSession =
                            established
                    }
        }

    private suspend fun establishSession(): Session {
        val identity =
            signer.publicIdentity()
        val challengeResponse =
            transport.postJson(
                absolutePath =
                    SESSION_CHALLENGE_PATH,
                body =
                    JSONObject()
                        .put(
                            "deviceRef",
                            identity.deviceRef,
                        )
                        .toString(),
            )
        val challenge =
            parseChallenge(
                challengeResponse,
            )
        val signature =
            signer.sign(
                challenge,
            )
        val establishResponse =
            transport.postJson(
                absolutePath =
                    SESSION_ESTABLISH_PATH,
                body =
                    JSONObject()
                        .put(
                            "challengeRef",
                            signature.challengeRef,
                        )
                        .put(
                            "signatureBytesBase64Url",
                            signature.signatureBytesBase64Url,
                        )
                        .toString(),
            )
        return parseSession(
            establishResponse =
                establishResponse,
            expectedDeviceRef =
                identity.deviceRef,
        )
    }

    private fun parseChallenge(
        responseText: String,
    ): VexHomeDevicePossessionChallenge {
        val response =
            JSONObject(
                responseText,
            )
        require(
            response.getString("state") ==
                "DEVICE_POSSESSION_CHALLENGE_CURRENT",
        ) {
            "Private Home gateway did not return a current possession challenge"
        }
        require(
            !response.getBoolean("homeAuthorization") &&
                !response.getBoolean("capabilityLease"),
        ) {
            "Possession challenge must not grant Home authorization or capability lease"
        }
        val challenge =
            response.getJSONObject(
                "challenge",
            )
        return VexHomeDevicePossessionChallenge(
            schemaVersion =
                challenge.getString(
                    "schemaVersion",
                ),
            challengeRef =
                challenge.getString(
                    "challengeRef",
                ),
            homeRef =
                challenge.getString(
                    "homeRef",
                ),
            principalRef =
                challenge.getString(
                    "principalRef",
                ),
            deviceRef =
                challenge.getString(
                    "deviceRef",
                ),
            membershipRef =
                challenge.getString(
                    "membershipRef",
                ),
            membershipHash =
                challenge.getString(
                    "membershipHash",
                ),
            revocationGeneration =
                challenge.getLong(
                    "revocationGeneration",
                ),
            devicePublicKeyFingerprintRef =
                challenge.getString(
                    "devicePublicKeyFingerprintRef",
                ),
            nonceBase64Url =
                challenge.getString(
                    "nonceBase64Url",
                ),
            issuedAtMs =
                challenge.getLong(
                    "issuedAtMs",
                ),
            expiresAtMs =
                challenge.getLong(
                    "expiresAtMs",
                ),
            signingPayloadSha256 =
                challenge.getString(
                    "signingPayloadSha256",
                ),
            credentialClass =
                challenge.getString(
                    "credentialClass",
                ),
            signatureAlgorithmRef =
                challenge.getString(
                    "signatureAlgorithmRef",
                ),
            devicePublicKeyEncoding =
                challenge.getString(
                    "devicePublicKeyEncoding",
                ),
        )
    }

    private fun parseSession(
        establishResponse: String,
        expectedDeviceRef: String,
    ): Session {
        val response =
            JSONObject(
                establishResponse,
            )
        require(
            response.getString("state") ==
                "REMOTE_AUTHENTICATED_SESSION_ESTABLISHED",
        ) {
            "Private Home gateway did not establish an authenticated remote session"
        }
        require(
            response.getString("deviceRef") ==
                expectedDeviceRef,
        ) {
            "Private Home gateway session belongs to another deviceRef"
        }
        require(
            !response.getBoolean("homeAuthorization") &&
                !response.getBoolean("standingHomeAuthority"),
        ) {
            "Remote authenticated session must not grant standing Home authority"
        }
        val sessionRef =
            response.getString(
                "sessionRef",
            )
        require(
            SESSION_REF.matches(
                sessionRef,
            ),
        ) {
            "Private Home gateway returned a malformed session ref"
        }
        val expiresAt =
            parseIsoUtcMillis(
                response.getString(
                    "expiresAt",
                ),
            )
        require(
            expiresAt >
                nowEpochMillis(),
        ) {
            "Private Home gateway returned an already-expired session"
        }
        return Session(
            sessionRef =
                sessionRef,
            expiresAtEpochMillis =
                expiresAt,
        )
    }

    private suspend fun invalidate(
        session: Session,
    ) {
        sessionMutex.withLock {
            if (currentSession == session) {
                currentSession =
                    null
            }
        }
    }

    private fun parseIsoUtcMillis(
        value: String,
    ): Long {
        val parser =
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US,
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "UTC",
                    )
                isLenient =
                    false
            }
        return requireNotNull(
            parser.parse(
                value,
            ),
        ) {
            "Private Home gateway session expiry is invalid"
        }.time
    }

    private companion object {
        const val SESSION_CHALLENGE_PATH =
            "/v1/home/session/challenge"
        const val SESSION_ESTABLISH_PATH =
            "/v1/home/session/establish"
        const val SESSION_EXPIRY_SKEW_MILLIS =
            1_000L
    }
}

private val SESSION_REF =
    Regex(
        "^session\\.vexlife\\.home-bridge\\.remote\\.[A-Za-z0-9_-]{43}$",
    )

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][PRIVATE-HOME-GATEWAY]