package com.vextreme.vexlife.android.connectedservices

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class VexPrivateHomeGatewayDiagnosticTransportTest {
    @Test
    fun transportFailuresProjectExactClosedRequestAndBoundaryRefs() =
        runBlocking {
            val certificateFailure =
                SSLHandshakeException(
                    "certificate detail must stay private",
                ).apply {
                    initCause(
                        CertPathValidatorException(
                            "private certificate path",
                        ),
                    )
                }

            val cases =
                listOf(
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = UnknownHostException("private-tailnet-host"),
                        expectedBoundary = "dns-resolution",
                    ),
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = NoRouteToHostException("private route"),
                        expectedBoundary = "route-unavailable",
                    ),
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = ConnectException("private endpoint"),
                        expectedBoundary = "connect-failed",
                    ),
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = SocketTimeoutException("private timeout detail"),
                        expectedBoundary = "network-timeout",
                    ),
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = SSLPeerUnverifiedException("private peer detail"),
                        expectedBoundary = "tls-peer-verification",
                    ),
                    Case(
                        path = SESSION_CHALLENGE,
                        cause = certificateFailure,
                        expectedBoundary = "tls-certificate",
                    ),
                    Case(
                        path = SESSION_ESTABLISH,
                        cause = SSLHandshakeException("private handshake detail"),
                        expectedBoundary = "tls-handshake",
                    ),
                )

            cases.forEach { case ->
                val diagnostic =
                    diagnosticFrom(
                        path =
                            case.path,
                        failure =
                            VexPrivateHomeGatewayException(
                                kind =
                                    VexPrivateHomeGatewayFailureKind
                                        .TransportUnavailable,
                                cause =
                                    case.cause,
                            ),
                    )

                assertEquals(
                    "diagnostic.vexlife.connected-services.private-home." +
                        "${stageSegment(case.path)}.${case.expectedBoundary}",
                    diagnostic.diagnostic.ref,
                )
            }
        }

    @Test
    fun httpFailuresRemainTypedWithoutProjectingServerOrBearerMaterial() =
        runBlocking {
            val session =
                diagnosticFrom(
                    path =
                        FAMILY_BOOTSTRAP,
                    failure =
                        VexPrivateHomeGatewayException(
                            kind =
                                VexPrivateHomeGatewayFailureKind
                                    .SessionUnavailable,
                            statusCode =
                                401,
                            serverCode =
                                "PRIVATE_SERVER_CODE",
                        ),
                )
            val authorization =
                diagnosticFrom(
                    path =
                        FAMILY_CONVERSATION,
                    failure =
                        VexPrivateHomeGatewayException(
                            kind =
                                VexPrivateHomeGatewayFailureKind
                                    .AuthorizationDenied,
                            statusCode =
                                403,
                            serverCode =
                                "PRIVATE_SERVER_CODE",
                        ),
                )
            val service =
                diagnosticFrom(
                    path =
                        FAMILY_CONVERSATION,
                    failure =
                        VexPrivateHomeGatewayException(
                            kind =
                                VexPrivateHomeGatewayFailureKind
                                    .ServiceUnavailable,
                            statusCode =
                                503,
                        ),
                )

            assertEquals(
                "diagnostic.vexlife.connected-services.private-home." +
                    "family-bootstrap.http-session-unavailable",
                session.diagnostic.ref,
            )
            assertEquals(
                "diagnostic.vexlife.connected-services.private-home." +
                    "family-conversation.http-authorization-denied",
                authorization.diagnostic.ref,
            )
            assertEquals(
                "diagnostic.vexlife.connected-services.private-home." +
                    "family-conversation.http-service-unavailable",
                service.diagnostic.ref,
            )
            listOf(
                session,
                authorization,
                service,
            ).forEach { diagnostic ->
                assertFalse(
                    diagnostic.message.orEmpty()
                        .contains(
                            "PRIVATE_SERVER_CODE",
                        ),
                )
            }
        }

    @Test
    fun screenProjectionFindsOnlyTheClosedRefThroughWrappedFailures() =
        runBlocking {
            val secret =
                "session.vexlife.home-bridge.remote.SECRET_BEARER"
            val diagnostic =
                diagnosticFrom(
                    path =
                        SESSION_CHALLENGE,
                    failure =
                        VexPrivateHomeGatewayException(
                            kind =
                                VexPrivateHomeGatewayFailureKind
                                    .TransportUnavailable,
                            cause =
                                UnknownHostException(
                                    "private-host $secret",
                                ),
                        ),
                )
            val wrapped =
                IllegalStateException(
                    "feature wrapper $secret",
                    diagnostic,
                )
            val projected =
                wrapped.privateHomeGatewayDiagnosticRefOrNull()

            assertEquals(
                "diagnostic.vexlife.connected-services.private-home." +
                    "session-challenge.dns-resolution",
                projected,
            )
            assertFalse(
                projected.orEmpty()
                    .contains(
                        secret,
                    ),
            )
            assertFalse(
                projected.orEmpty()
                    .contains(
                        "private-host",
                    ),
            )
        }

    @Test
    fun cancellationPassesThroughWithoutDiagnosticWrappingOrRetry() =
        runBlocking {
            val cancellation =
                java.util.concurrent.CancellationException(
                    "cancel current request",
                )
            val delegate =
                FailingTransport(
                    cancellation,
                )
            val transport =
                DiagnosticVexPrivateHomeGatewayHttpTransport(
                    delegate,
                )

            val observed =
                try {
                    transport.postJson(
                        absolutePath =
                            SESSION_CHALLENGE,
                        body =
                            "{}",
                    )
                    error(
                        "Expected cancellation",
                    )
                } catch (error: java.util.concurrent.CancellationException) {
                    error
                }

            assertSame(
                cancellation,
                observed,
            )
            assertEquals(
                1,
                delegate.calls,
            )
        }

    private suspend fun diagnosticFrom(
        path: String,
        failure: Throwable,
    ): VexPrivateHomeGatewayDiagnosticException {
        val delegate =
            FailingTransport(
                failure,
            )
        val transport =
            DiagnosticVexPrivateHomeGatewayHttpTransport(
                delegate =
                    delegate,
            )
        return try {
            transport.postJson(
                absolutePath =
                    path,
                body =
                    "{}",
            )
            error(
                "Expected diagnostic failure",
            )
        } catch (error: VexPrivateHomeGatewayDiagnosticException) {
            assertEquals(
                1,
                delegate.calls,
            )
            error
        }
    }

    private fun stageSegment(
        path: String,
    ): String =
        when (path) {
            SESSION_CHALLENGE ->
                "session-challenge"

            SESSION_ESTABLISH ->
                "session-establish"

            FAMILY_BOOTSTRAP ->
                "family-bootstrap"

            FAMILY_CONVERSATION ->
                "family-conversation"

            else ->
                "unknown-request"
        }

    private data class Case(
        val path: String,
        val cause: Throwable,
        val expectedBoundary: String,
    )

    private class FailingTransport(
        private val failure: Throwable,
    ) : VexPrivateHomeGatewayHttpTransport {
        var calls =
            0
            private set

        override suspend fun getJson(
            absolutePath: String,
            bearerSessionRef: String?,
        ): String {
            calls += 1
            throw failure
        }

        override suspend fun postJson(
            absolutePath: String,
            body: String,
            bearerSessionRef: String?,
        ): String {
            calls += 1
            throw failure
        }
    }

    private companion object {
        const val SESSION_CHALLENGE =
            "/v1/home/session/challenge"
        const val SESSION_ESTABLISH =
            "/v1/home/session/establish"
        const val FAMILY_BOOTSTRAP =
            "/v1/home/family/bootstrap"
        const val FAMILY_CONVERSATION =
            "/v1/home/family/conversation"
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][PRIVATE-HOME-DIAGNOSTIC-PROOF]
