package com.vextreme.vexlife.android.data.vexstream

import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal object VexStreamPinnedTls {
    fun open(
        baseUrl: String,
        certificateSha256: String,
        path: String,
        method: String = "GET",
        token: String? = null,
    ): HttpsURLConnection {
        require(certificateSha256.matches(Regex("^[0-9a-f]{64}$")))

        val trustManager =
            ExactCertificateTrustManager(
                expectedSha256 = certificateSha256,
            )
        val sslContext =
            SSLContext.getInstance("TLS")
                .apply {
                    init(
                        null,
                        arrayOf(trustManager),
                        SecureRandom(),
                    )
                }

        val connection =
            URL(
                baseUrl.trimEnd('/') +
                    "/" +
                    path.trimStart('/'),
            ).openConnection() as HttpsURLConnection

        connection.sslSocketFactory =
            sslContext.socketFactory
        connection.hostnameVerifier =
            HostnameVerifier { _, _ ->
                // Host/IP names may change on a private LAN. Exact certificate pinning above is the
                // identity check for this local pairing candidate.
                true
            }
        connection.requestMethod = method
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.useCaches = false
        connection.setRequestProperty(
            "Accept",
            "application/json, audio/mpeg",
        )

        token
            ?.takeIf { it.isNotBlank() }
            ?.let {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $it",
                )
            }

        return connection
    }

    fun readText(
        connection: HttpURLConnection,
    ): String {
        val stream =
            if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        return stream
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
    }

    fun certificateSha256(
        certificate: X509Certificate,
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(separator = "") { byte ->
                "%02x".format(byte)
            }

    private class ExactCertificateTrustManager(
        private val expectedSha256: String,
    ) : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
        ) {
            throw java.security.cert.CertificateException(
                "VexStream client certificates are not accepted",
            )
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?,
        ) {
            val certificate =
                chain
                    ?.firstOrNull()
                    ?: throw java.security.cert.CertificateException(
                        "VexStream server certificate missing",
                    )

            val actual =
                certificateSha256(
                    certificate,
                )
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                throw java.security.cert.CertificateException(
                    "VexStream server certificate changed",
                )
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            emptyArray()
    }

    private const val CONNECT_TIMEOUT_MS = 3_000
    private const val READ_TIMEOUT_MS = 30_000
}

// [VXG RealForever]
