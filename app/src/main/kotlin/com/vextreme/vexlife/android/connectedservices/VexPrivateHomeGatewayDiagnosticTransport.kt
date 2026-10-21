package com.vextreme.vexlife.android.connectedservices

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Closed, secret-free request stages for private Home gateway diagnostics.
 *
 * These values identify which accepted request was being carried. They never contain the selected
 * endpoint, bearer/session material, request body, possession signature or capability material.
 */
internal enum class VexPrivateHomeGatewayDiagnosticStage(
    val refSegment: String,
) {
    SessionChallenge(
        "session-challenge",
    ),
    SessionEstablish(
        "session-establish",
    ),
    FamilyBootstrap(
        "family-bootstrap",
    ),
    FamilyConversation(
        "family-conversation",
    ),
    UnknownRequest(
        "unknown-request",
    ),
}

/**
 * Closed failure boundaries retained from the Android HTTPS transport.
 *
 * The boundary is intentionally smaller than the original Throwable. It is enough to distinguish
 * DNS, route/connect, TLS, timeout, HTTP and local transport-contract failures without projecting raw
 * exception messages or private connection material into the Screen.
 */
internal enum class VexPrivateHomeGatewayDiagnosticBoundary(
    val refSegment: String,
) {
    DnsResolution(
        "dns-resolution",
    ),
    RouteUnavailable(
        "route-unavailable",
    ),
    ConnectFailed(
        "connect-failed",
    ),
    NetworkTimeout(
        "network-timeout",
    ),
    TlsPeerVerification(
        "tls-peer-verification",
    ),
    TlsCertificate(
        "tls-certificate",
    ),
    TlsHandshake(
        "tls-handshake",
    ),
    HttpSessionUnavailable(
        "http-session-unavailable",
    ),
    HttpAuthorizationDenied(
        "http-authorization-denied",
    ),
    HttpServiceUnavailable(
        "http-service-unavailable",
    ),
    HttpResponseRejected(
        "http-response-rejected",
    ),
    TransportContract(
        "transport-contract",
    ),
    TransportIo(
        "transport-io",
    ),
    Unclassified(
        "unclassified",
    ),
}

internal data class VexPrivateHomeGatewayDiagnostic(
    val stage: VexPrivateHomeGatewayDiagnosticStage,
    val boundary: VexPrivateHomeGatewayDiagnosticBoundary,
) {
    val ref: String =
        "diagnostic.vexlife.connected-services.private-home." +
            "${stage.refSegment}.${boundary.refSegment}"
}

/**
 * Typed carrier for one sanitized diagnostic projection.
 *
 * The original failure remains available as the cause for local engineering diagnosis. Product/UI
 * consumers receive only [diagnostic], whose vocabulary is closed and contains no raw failure text.
 */
internal class VexPrivateHomeGatewayDiagnosticException(
    val diagnostic: VexPrivateHomeGatewayDiagnostic,
    cause: Throwable,
) : IllegalStateException(
    diagnostic.ref,
    cause,
)

/**
 * Diagnostic-only decorator around the accepted private Home HTTP transport.
 *
 * The delegate remains the transport owner. This class changes no request, retry, credential, route,
 * session or authorization behavior; it only preserves the smallest closed failure boundary needed
 * by the installed debug/lived-proof surface.
 */
internal class DiagnosticVexPrivateHomeGatewayHttpTransport(
    private val delegate: VexPrivateHomeGatewayHttpTransport,
) : VexPrivateHomeGatewayHttpTransport {
    override suspend fun getJson(
        absolutePath: String,
        bearerSessionRef: String?,
    ): String =
        execute(
            absolutePath =
                absolutePath,
        ) {
            delegate.getJson(
                absolutePath =
                    absolutePath,
                bearerSessionRef =
                    bearerSessionRef,
            )
        }

    override suspend fun postJson(
        absolutePath: String,
        body: String,
        bearerSessionRef: String?,
    ): String =
        execute(
            absolutePath =
                absolutePath,
        ) {
            delegate.postJson(
                absolutePath =
                    absolutePath,
                body =
                    body,
                bearerSessionRef =
                    bearerSessionRef,
            )
        }

    private suspend fun execute(
        absolutePath: String,
        request: suspend () -> String,
    ): String =
        try {
            request()
        } catch (error: VexPrivateHomeGatewayDiagnosticException) {
            throw error
        } catch (error: CancellationException) {
            throw error
        } catch (error: VexPrivateHomeGatewayException) {
            throw VexPrivateHomeGatewayDiagnosticException(
                diagnostic =
                    VexPrivateHomeGatewayDiagnostic(
                        stage =
                            diagnosticStage(
                                absolutePath,
                            ),
                        boundary =
                            diagnosticBoundary(
                                error,
                            ),
                    ),
                cause =
                    error,
            )
        } catch (error: IllegalArgumentException) {
            throw VexPrivateHomeGatewayDiagnosticException(
                diagnostic =
                    VexPrivateHomeGatewayDiagnostic(
                        stage =
                            diagnosticStage(
                                absolutePath,
                            ),
                        boundary =
                            VexPrivateHomeGatewayDiagnosticBoundary
                                .TransportContract,
                    ),
                cause =
                    error,
            )
        } catch (error: Exception) {
            throw VexPrivateHomeGatewayDiagnosticException(
                diagnostic =
                    VexPrivateHomeGatewayDiagnostic(
                        stage =
                            diagnosticStage(
                                absolutePath,
                            ),
                        boundary =
                            VexPrivateHomeGatewayDiagnosticBoundary
                                .Unclassified,
                    ),
                cause =
                    error,
            )
        }
}

/** Returns the first closed diagnostic ref in a wrapped failure chain, never raw failure text. */
internal fun Throwable.privateHomeGatewayDiagnosticRefOrNull(): String? =
    causeChain()
        .filterIsInstance<
            VexPrivateHomeGatewayDiagnosticException
        >()
        .firstOrNull()
        ?.diagnostic
        ?.ref

private fun diagnosticStage(
    absolutePath: String,
): VexPrivateHomeGatewayDiagnosticStage =
    when (absolutePath) {
        "/v1/home/session/challenge" ->
            VexPrivateHomeGatewayDiagnosticStage
                .SessionChallenge

        "/v1/home/session/establish" ->
            VexPrivateHomeGatewayDiagnosticStage
                .SessionEstablish

        "/v1/home/family/bootstrap" ->
            VexPrivateHomeGatewayDiagnosticStage
                .FamilyBootstrap

        "/v1/home/family/conversation" ->
            VexPrivateHomeGatewayDiagnosticStage
                .FamilyConversation

        else ->
            VexPrivateHomeGatewayDiagnosticStage
                .UnknownRequest
    }

private fun diagnosticBoundary(
    error: VexPrivateHomeGatewayException,
): VexPrivateHomeGatewayDiagnosticBoundary {
    val causes =
        error
            .causeChain()
            .toList()

    return when {
        causes.any { it is UnknownHostException } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .DnsResolution

        causes.any { it is NoRouteToHostException } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .RouteUnavailable

        causes.any { it is ConnectException } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .ConnectFailed

        causes.any { it is SocketTimeoutException } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .NetworkTimeout

        causes.any { it is SSLPeerUnverifiedException } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .TlsPeerVerification

        causes.any {
            it is CertPathValidatorException ||
                it is CertificateException
        } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .TlsCertificate

        causes.any {
            it is SSLHandshakeException ||
                it is SSLException
        } ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .TlsHandshake

        error.kind ==
            VexPrivateHomeGatewayFailureKind
                .SessionUnavailable ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .HttpSessionUnavailable

        error.kind ==
            VexPrivateHomeGatewayFailureKind
                .AuthorizationDenied ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .HttpAuthorizationDenied

        error.kind ==
            VexPrivateHomeGatewayFailureKind
                .ServiceUnavailable ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .HttpServiceUnavailable

        error.kind ==
            VexPrivateHomeGatewayFailureKind
                .TransportFailed ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .HttpResponseRejected

        error.kind ==
            VexPrivateHomeGatewayFailureKind
                .TransportUnavailable ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .TransportIo

        else ->
            VexPrivateHomeGatewayDiagnosticBoundary
                .Unclassified
    }
}

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(
        seed =
            this,
    ) { current ->
        current.cause
            ?.takeUnless { cause ->
                cause === current
            }
    }.take(
        16,
    )

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][PRIVATE-HOME-DIAGNOSTIC]
