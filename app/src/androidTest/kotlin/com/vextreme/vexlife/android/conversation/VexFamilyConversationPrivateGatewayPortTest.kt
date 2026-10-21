package com.vextreme.vexlife.android.conversation

import com.vextreme.vexlife.android.connectedservices.VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS
import com.vextreme.vexlife.android.connectedservices.VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF
import com.vextreme.vexlife.android.connectedservices.VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA
import com.vextreme.vexlife.android.connectedservices.VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING
import com.vextreme.vexlife.android.connectedservices.VexHomeDevicePossessionChallenge
import com.vextreme.vexlife.android.connectedservices.VexHomeDevicePossessionSignature
import com.vextreme.vexlife.android.connectedservices.VexHomeDevicePossessionSigner
import com.vextreme.vexlife.android.connectedservices.VexHomeDevicePublicIdentity
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayConnection
import com.vextreme.vexlife.android.connectedservices.VexPrivateHomeGatewayHttpTransport
import com.vextreme.vexlife.android.connectedservices.canonicalVexHomeDevicePossessionSigningBytes
import com.vextreme.vexlife.android.connectedservices.sha256LowerHex
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFamilyConversationPrivateGatewayPortTest {
    @Test
    fun onePossessionSessionCarriesBootstrapAndThreadHistoryWithoutLeakingBearerIntoFeatureIntent() =
        runBlocking {
            val signer =
                RecordingSigner()
            val transport =
                RecordingGatewayTransport(
                    signer.identity,
                )
            val connection =
                VexPrivateHomeGatewayConnection(
                    transport =
                        transport,
                    signer =
                        signer,
                    nowEpochMillis = {
                        T0
                    },
                )
            val port =
                VexFamilyConversationPrivateGatewayPort(
                    connection,
                )

            val bootstrap =
                port.bootstrap()
            val history =
                port.readThreadHistory(
                    VexFamilyConversationThreadHistoryIntent(
                        spaceRef =
                            SPACE,
                        channelRef =
                            CHANNEL,
                        expectedMembershipGeneration =
                            2L,
                    ),
                )

            assertEquals(
                VICTOR,
                bootstrap.currentPrincipalRef,
            )
            assertEquals(
                CHANNEL,
                bootstrap.rooms.single().channelRef,
            )
            assertEquals(
                THREAD,
                history.threadRef,
            )
            assertEquals(
                listOf(
                    "channel.family.vcs04.g1",
                    CHANNEL,
                ),
                history.segmentRefs,
            )
            assertEquals(
                1,
                signer.signCalls,
            )
            assertEquals(
                listOf(
                    "/v1/home/session/challenge",
                    "/v1/home/session/establish",
                    "/v1/home/family/conversation",
                ),
                transport.postPaths,
            )
            assertEquals(
                listOf(
                    "/v1/home/family/bootstrap",
                ),
                transport.getPaths,
            )
            assertEquals(
                listOf(
                    SESSION_REF,
                    SESSION_REF,
                ),
                transport.authenticatedBearerRefs,
            )
            assertTrue(
                transport.lastFamilyConversationBody
                    .contains(
                        "READ_THREAD_HISTORY",
                    ),
            )
            assertFalse(
                transport.lastFamilyConversationBody
                    .contains(
                        SESSION_REF,
                    ),
            )
        }

    private class RecordingSigner : VexHomeDevicePossessionSigner {
        val identity =
            VexHomeDevicePublicIdentity(
                deviceRef =
                    DEVICE_REF,
                devicePublicKeyBase64Url =
                    "AQA",
                devicePublicKeyFingerprintRef =
                    FINGERPRINT,
            )
        var signCalls =
            0
            private set

        override fun publicIdentity(): VexHomeDevicePublicIdentity =
            identity

        override fun sign(
            challenge: VexHomeDevicePossessionChallenge,
        ): VexHomeDevicePossessionSignature {
            signCalls += 1
            assertEquals(
                DEVICE_REF,
                challenge.deviceRef,
            )
            assertEquals(
                FINGERPRINT,
                challenge.devicePublicKeyFingerprintRef,
            )
            assertEquals(
                challenge.signingPayloadSha256,
                sha256LowerHex(
                    canonicalVexHomeDevicePossessionSigningBytes(
                        challenge,
                    ),
                ),
            )
            return VexHomeDevicePossessionSignature(
                challengeRef =
                    challenge.challengeRef,
                signatureBytesBase64Url =
                    "MEUCIQfixture",
            )
        }
    }

    private class RecordingGatewayTransport(
        private val identity:
            VexHomeDevicePublicIdentity,
    ) : VexPrivateHomeGatewayHttpTransport {
        val postPaths =
            mutableListOf<String>()
        val getPaths =
            mutableListOf<String>()
        val authenticatedBearerRefs =
            mutableListOf<String>()
        var lastFamilyConversationBody =
            ""
            private set

        override suspend fun getJson(
            absolutePath: String,
            bearerSessionRef: String?,
        ): String {
            getPaths +=
                absolutePath
            requireNotNull(
                bearerSessionRef,
            ).also(
                authenticatedBearerRefs::add,
            )
            check(
                absolutePath ==
                    "/v1/home/family/bootstrap",
            )
            return bootstrapJson()
        }

        override suspend fun postJson(
            absolutePath: String,
            body: String,
            bearerSessionRef: String?,
        ): String {
            postPaths +=
                absolutePath
            return when (absolutePath) {
                "/v1/home/session/challenge" -> {
                    check(
                        bearerSessionRef == null,
                    )
                    assertEquals(
                        DEVICE_REF,
                        JSONObject(
                            body,
                        ).getString(
                            "deviceRef",
                        ),
                    )
                    challengeJson(
                        identity,
                    )
                }

                "/v1/home/session/establish" -> {
                    check(
                        bearerSessionRef == null,
                    )
                    val request =
                        JSONObject(
                            body,
                        )
                    assertEquals(
                        CHALLENGE_REF,
                        request.getString(
                            "challengeRef",
                        ),
                    )
                    sessionJson()
                }

                "/v1/home/family/conversation" -> {
                    requireNotNull(
                        bearerSessionRef,
                    ).also(
                        authenticatedBearerRefs::add,
                    )
                    lastFamilyConversationBody =
                        body
                    val request =
                        JSONObject(
                            body,
                        )
                    assertEquals(
                        "READ_THREAD_HISTORY",
                        request.getString(
                            "operation",
                        ),
                    )
                    historyJson()
                }

                else ->
                    error(
                        "Unexpected POST $absolutePath",
                    )
            }
        }
    }

    companion object {
        const val T0 =
            1_800_000_000_000L
        const val DEVICE_REF =
            "device.android.vcs04"
        const val CHALLENGE_REF =
            "challenge.vcs04.android.001"
        const val FINGERPRINT =
            "fingerprint.sha256.${"b".repeat(64)}"
        val SESSION_REF =
            "session.vexlife.home-bridge.remote.${"S".repeat(43)}"
        const val SPACE =
            "space.family.vcs04"
        const val THREAD =
            "thread.family.vcs04"
        const val CHANNEL =
            "channel.family.vcs04.g2"
        const val VICTOR =
            "person.victor-gong"
        const val COMPANION =
            "lineage.family.vcs04"

        fun challengeJson(
            identity: VexHomeDevicePublicIdentity,
        ): String {
            val unsigned =
                VexHomeDevicePossessionChallenge(
                    schemaVersion =
                        VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA,
                    challengeRef =
                        CHALLENGE_REF,
                    homeRef =
                        "home.current",
                    principalRef =
                        VICTOR,
                    deviceRef =
                        identity.deviceRef,
                    membershipRef =
                        "membership.android.vcs04",
                    membershipHash =
                        "a".repeat(64),
                    revocationGeneration =
                        3L,
                    devicePublicKeyFingerprintRef =
                        identity.devicePublicKeyFingerprintRef,
                    nonceBase64Url =
                        "AAECAwQFBgcICQ",
                    issuedAtMs =
                        T0,
                    expiresAtMs =
                        T0 + 60_000L,
                    signingPayloadSha256 =
                        "0".repeat(64),
                    credentialClass =
                        VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS,
                    signatureAlgorithmRef =
                        VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF,
                    devicePublicKeyEncoding =
                        VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING,
                )
            val bound =
                unsigned.copy(
                    signingPayloadSha256 =
                        sha256LowerHex(
                            canonicalVexHomeDevicePossessionSigningBytes(
                                unsigned,
                            ),
                        ),
                )
            return JSONObject()
                .put(
                    "state",
                    "DEVICE_POSSESSION_CHALLENGE_CURRENT",
                )
                .put(
                    "homeAuthorization",
                    false,
                )
                .put(
                    "capabilityLease",
                    false,
                )
                .put(
                    "challenge",
                    JSONObject()
                        .put("schemaVersion", bound.schemaVersion)
                        .put("challengeRef", bound.challengeRef)
                        .put("homeRef", bound.homeRef)
                        .put("principalRef", bound.principalRef)
                        .put("deviceRef", bound.deviceRef)
                        .put("membershipRef", bound.membershipRef)
                        .put("membershipHash", bound.membershipHash)
                        .put("revocationGeneration", bound.revocationGeneration)
                        .put("devicePublicKeyFingerprintRef", bound.devicePublicKeyFingerprintRef)
                        .put("nonceBase64Url", bound.nonceBase64Url)
                        .put("issuedAtMs", bound.issuedAtMs)
                        .put("expiresAtMs", bound.expiresAtMs)
                        .put("signingPayloadSha256", bound.signingPayloadSha256)
                        .put("credentialClass", bound.credentialClass)
                        .put("signatureAlgorithmRef", bound.signatureAlgorithmRef)
                        .put("devicePublicKeyEncoding", bound.devicePublicKeyEncoding),
                )
                .toString()
        }

        fun sessionJson(): String =
            JSONObject()
                .put(
                    "state",
                    "REMOTE_AUTHENTICATED_SESSION_ESTABLISHED",
                )
                .put(
                    "sessionRef",
                    SESSION_REF,
                )
                .put(
                    "stableSessionBindingRef",
                    "session-binding.vcs04.android",
                )
                .put(
                    "principalRef",
                    VICTOR,
                )
                .put(
                    "deviceRef",
                    DEVICE_REF,
                )
                .put(
                    "expiresAt",
                    "2027-01-15T08:01:00.000Z",
                )
                .put(
                    "homeAuthorization",
                    false,
                )
                .put(
                    "standingHomeAuthority",
                    false,
                )
                .toString()

        fun bootstrapJson(): String =
            JSONObject()
                .put("schemaVersion", "vexlife.server-family-conversation-bootstrap/v1")
                .put("state", "CURRENT")
                .put("truthClass", "CURRENT_LIVE_FAMILY")
                .put("currentPrincipalRef", VICTOR)
                .put(
                    "rooms",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("spaceRef", SPACE)
                                .put("channelRef", CHANNEL)
                                .put("threadRef", THREAD)
                                .put("kind", "FAMILY")
                                .put("membershipGeneration", 2L)
                                .put(
                                    "audience",
                                    JSONArray()
                                        .put(JSONObject().put("principalRef", VICTOR).put("role", "OWNER"))
                                        .put(JSONObject().put("principalRef", "person.bri").put("role", "MEMBER")),
                                )
                                .put("familyCompanionLineageRef", COMPANION)
                                .put("familyCompanionIncluded", true),
                        ),
                )
                .toString()

        fun historyJson(): String =
            JSONObject()
                .put("schemaVersion", "vexlife.browser-family-thread-history/v1")
                .put("state", "CURRENT")
                .put("spaceRef", SPACE)
                .put("threadRef", THREAD)
                .put("currentChannelRef", CHANNEL)
                .put("membershipGeneration", 2L)
                .put(
                    "segmentRefs",
                    JSONArray()
                        .put("channel.family.vcs04.g1")
                        .put(CHANNEL),
                )
                .put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("messageRef", "message.family.vcs04.001")
                                .put("spaceRef", SPACE)
                                .put("threadRef", THREAD)
                                .put("channelRef", CHANNEL)
                                .put("speakerRef", VICTOR)
                                .put("recipientRefs", JSONArray().put("person.bri"))
                                .put("witnessRefs", JSONArray().put(VICTOR).put("person.bri").put(COMPANION))
                                .put("membershipGeneration", 2L)
                                .put("sequence", 0L)
                                .put("content", "Private route reached canonical Family history."),
                        ),
                )
                .put("truncated", false)
                .toString()
    }
}

// [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][VCS-04][PRIVATE-GATEWAY-PORT-PROOF]