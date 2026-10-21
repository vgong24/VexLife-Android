package com.vextreme.vexlife.android.connectedservices

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

internal const val VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA =
    "vextreme.security.home-device-possession-signing-payload/v1"
internal const val VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS =
    "OS_BACKED_DEVICE_SIGNING_KEY"
internal const val VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF =
    "signature.vextreme.ecdsa-p256-sha256"
internal const val VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING =
    "X509_SPKI_DER_BASE64URL"

/**
 * Public, non-secret identity of one Android device-possession signing key.
 *
 * The private key never leaves AndroidKeyStore. [deviceRef] is a stable local identity candidate that
 * becomes trusted only after the rightful Home membership owner binds it to this exact public key.
 */
internal data class VexHomeDevicePublicIdentity(
    val deviceRef: String,
    val devicePublicKeyBase64Url: String,
    val devicePublicKeyFingerprintRef: String,
)

/**
 * Exact possession challenge fields whose canonical bytes are signed by Android.
 *
 * The SDK verifier owns this wire contract. Android mirrors only the fields required to reproduce the
 * verifier's deterministic signing bytes. Source/currentness arrays remain transport evidence and are
 * deliberately absent here because they are not part of the signed payload.
 */
internal data class VexHomeDevicePossessionChallenge(
    val schemaVersion: String,
    val challengeRef: String,
    val homeRef: String,
    val principalRef: String,
    val deviceRef: String,
    val membershipRef: String,
    val membershipHash: String,
    val revocationGeneration: Long,
    val devicePublicKeyFingerprintRef: String,
    val nonceBase64Url: String,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val signingPayloadSha256: String,
    val credentialClass: String,
    val signatureAlgorithmRef: String,
    val devicePublicKeyEncoding: String,
)

internal data class VexHomeDevicePossessionSignature(
    val challengeRef: String,
    val signatureBytesBase64Url: String,
)

/**
 * Android-owned realization of the accepted OS-backed device-possession signer boundary.
 *
 * Implementations expose only public identity and signatures. No private-key bytes or credential
 * serialization API exists.
 */
internal interface VexHomeDevicePossessionSigner {
    fun publicIdentity(): VexHomeDevicePublicIdentity

    fun sign(
        challenge: VexHomeDevicePossessionChallenge,
    ): VexHomeDevicePossessionSignature
}

/**
 * Canonical signing bytes required by the accepted SDK verifier.
 *
 * The payload is flat and every string field is constrained by the producer to safe portable/ref or
 * base64url grammar. We still JSON-escape defensively so Android byte formation remains correct if a
 * future accepted producer permits an escapable character without changing the canonical JSON law.
 */
internal fun canonicalVexHomeDevicePossessionSigningBytes(
    challenge: VexHomeDevicePossessionChallenge,
): ByteArray {
    require(
        challenge.schemaVersion ==
            VEX_HOME_DEVICE_POSSESSION_SIGNING_PAYLOAD_SCHEMA,
    ) {
        "Home device-possession signing schema is unsupported"
    }
    require(challenge.challengeRef.isNotBlank())
    require(challenge.homeRef.isNotBlank())
    require(challenge.principalRef.isNotBlank())
    require(challenge.deviceRef.isNotBlank())
    require(challenge.membershipRef.isNotBlank())
    require(SHA256.matches(challenge.membershipHash))
    require(challenge.revocationGeneration >= 0L)
    require(FINGERPRINT_REF.matches(challenge.devicePublicKeyFingerprintRef))
    require(BASE64URL.matches(challenge.nonceBase64Url))
    require(challenge.issuedAtMs >= 0L)
    require(challenge.expiresAtMs > challenge.issuedAtMs)

    val canonical =
        buildString {
            append('{')
            appendJsonStringField("challengeRef", challenge.challengeRef)
            append(',')
            appendJsonStringField(
                "devicePublicKeyFingerprintRef",
                challenge.devicePublicKeyFingerprintRef,
            )
            append(',')
            appendJsonStringField("deviceRef", challenge.deviceRef)
            append(',')
            appendJsonLongField("expiresAtMs", challenge.expiresAtMs)
            append(',')
            appendJsonStringField("homeRef", challenge.homeRef)
            append(',')
            appendJsonLongField("issuedAtMs", challenge.issuedAtMs)
            append(',')
            appendJsonStringField("membershipHash", challenge.membershipHash)
            append(',')
            appendJsonStringField("membershipRef", challenge.membershipRef)
            append(',')
            appendJsonStringField("nonceBase64Url", challenge.nonceBase64Url)
            append(',')
            appendJsonStringField("principalRef", challenge.principalRef)
            append(',')
            appendJsonLongField(
                "revocationGeneration",
                challenge.revocationGeneration,
            )
            append(',')
            appendJsonStringField("schemaVersion", challenge.schemaVersion)
            append("}\n")
        }

    return canonical.toByteArray(StandardCharsets.UTF_8)
}

internal fun sha256LowerHex(
    bytes: ByteArray,
): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { value ->
            (value.toInt() and 0xff)
                .toString(16)
                .padStart(2, '0')
        }

private fun StringBuilder.appendJsonStringField(
    key: String,
    value: String,
) {
    appendJsonString(key)
    append(':')
    appendJsonString(value)
}

private fun StringBuilder.appendJsonLongField(
    key: String,
    value: Long,
) {
    appendJsonString(key)
    append(':')
    append(value)
}

private fun StringBuilder.appendJsonString(
    value: String,
) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000c' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else ->
                if (character.code < 0x20) {
                    append("\\u")
                    append(
                        character.code
                            .toString(16)
                            .padStart(4, '0'),
                    )
                } else {
                    append(character)
                }
        }
    }
    append('"')
}

internal interface VexHomeDeviceRefStore {
    fun currentOrCreate(): String
}

private class SharedPreferencesVexHomeDeviceRefStore(
    context: Context,
) : VexHomeDeviceRefStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    override fun currentOrCreate(): String =
        synchronized(preferences) {
            preferences
                .getString(DEVICE_REF_KEY, null)
                ?.takeIf(DEVICE_REF::matches)
                ?: "device.android.${UUID.randomUUID().toString().lowercase()}"
                    .also { created ->
                        require(DEVICE_REF.matches(created))
                        check(
                            preferences
                                .edit()
                                .putString(DEVICE_REF_KEY, created)
                                .commit(),
                        ) {
                            "Android Home deviceRef could not be persisted"
                        }
                    }
        }

    private companion object {
        const val PREFERENCES_NAME =
            "vexlife_connected_services_device_identity"
        const val DEVICE_REF_KEY =
            "device_ref"
    }
}

/**
 * Production AndroidKeyStore signer for the accepted VCS-04 P-256 profile.
 *
 * A test may inject a dedicated alias/ref store so its key material can be independently removed.
 * Production uses one app-owned non-exportable key alias and one stable local deviceRef.
 */
internal class AndroidKeyStoreVexHomeDevicePossessionSigner(
    context: Context,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val deviceRefStore: VexHomeDeviceRefStore =
        SharedPreferencesVexHomeDeviceRefStore(
            context,
        ),
) : VexHomeDevicePossessionSigner {
    override fun publicIdentity(): VexHomeDevicePublicIdentity {
        val keyStore = loadKeyStore()
        ensureKeyExists(keyStore)
        val certificate =
            requireNotNull(
                keyStore.getCertificate(keyAlias),
            ) {
                "AndroidKeyStore possession certificate is unavailable"
            }
        val publicKeyBytes =
            requireNotNull(
                certificate.publicKey.encoded,
            ) {
                "AndroidKeyStore possession public key is not exportable"
            }
        val fingerprint =
            sha256LowerHex(
                publicKeyBytes,
            )

        return VexHomeDevicePublicIdentity(
            deviceRef =
                deviceRefStore.currentOrCreate(),
            devicePublicKeyBase64Url =
                Base64.encodeToString(
                    publicKeyBytes,
                    BASE64URL_FLAGS,
                ),
            devicePublicKeyFingerprintRef =
                "fingerprint.sha256.$fingerprint",
        )
    }

    override fun sign(
        challenge: VexHomeDevicePossessionChallenge,
    ): VexHomeDevicePossessionSignature {
        val identity =
            publicIdentity()
        require(
            challenge.deviceRef ==
                identity.deviceRef,
        ) {
            "Home device-possession challenge targets another Android deviceRef"
        }
        require(
            challenge.devicePublicKeyFingerprintRef ==
                identity.devicePublicKeyFingerprintRef,
        ) {
            "Home device-possession challenge targets another Android public key"
        }
        require(
            challenge.credentialClass ==
                VEX_HOME_DEVICE_POSSESSION_CREDENTIAL_CLASS,
        ) {
            "Home device-possession credential class is unsupported"
        }
        require(
            challenge.signatureAlgorithmRef ==
                VEX_HOME_DEVICE_POSSESSION_SIGNATURE_ALGORITHM_REF,
        ) {
            "Home device-possession signature algorithm is unsupported"
        }
        require(
            challenge.devicePublicKeyEncoding ==
                VEX_HOME_DEVICE_PUBLIC_KEY_ENCODING,
        ) {
            "Home device-possession public-key encoding is unsupported"
        }

        val canonicalBytes =
            canonicalVexHomeDevicePossessionSigningBytes(
                challenge,
            )
        require(
            sha256LowerHex(
                canonicalBytes,
            ) == challenge.signingPayloadSha256,
        ) {
            "Home device-possession challenge does not bind the canonical signing bytes"
        }

        val keyStore =
            loadKeyStore()
        val privateKey =
            keyStore.getKey(
                keyAlias,
                null,
            ) as? PrivateKey
                ?: error(
                    "AndroidKeyStore possession private key is unavailable",
                )
        val signer =
            Signature.getInstance(
                "SHA256withECDSA",
            )
        signer.initSign(
            privateKey,
        )
        signer.update(
            canonicalBytes,
        )
        val signatureBytes =
            signer.sign()

        return VexHomeDevicePossessionSignature(
            challengeRef =
                challenge.challengeRef,
            signatureBytesBase64Url =
                Base64.encodeToString(
                    signatureBytes,
                    BASE64URL_FLAGS,
                ),
        )
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(
            ANDROID_KEY_STORE,
        ).apply {
            load(null)
        }

    private fun ensureKeyExists(
        keyStore: KeyStore,
    ) {
        if (keyStore.containsAlias(keyAlias)) {
            return
        }

        synchronized(KEY_CREATION_LOCK) {
            val current =
                loadKeyStore()
            if (current.containsAlias(keyAlias)) {
                return
            }
            val generator =
                KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    ANDROID_KEY_STORE,
                )
            generator.initialize(
                KeyGenParameterSpec
                    .Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_SIGN,
                    )
                    .setAlgorithmParameterSpec(
                        ECGenParameterSpec(
                            "secp256r1",
                        ),
                    )
                    .setDigests(
                        KeyProperties.DIGEST_SHA256,
                    )
                    .build(),
            )
            generator.generateKeyPair()
        }
    }

    internal companion object {
        const val DEFAULT_KEY_ALIAS =
            "vexlife.connected-services.home-device-possession.v1"
        const val ANDROID_KEY_STORE =
            "AndroidKeyStore"
        val BASE64URL_FLAGS =
            Base64.URL_SAFE or
                Base64.NO_PADDING or
                Base64.NO_WRAP
        private val KEY_CREATION_LOCK =
            Any()
    }
}

private val SHA256 =
    Regex("^[a-f0-9]{64}$")
private val FINGERPRINT_REF =
    Regex("^fingerprint\\.sha256\\.[a-f0-9]{64}$")
private val BASE64URL =
    Regex("^[A-Za-z0-9_-]+$")
private val DEVICE_REF =
    Regex("^[a-z0-9](?:[a-z0-9._-]{0,126}[a-z0-9])?$")

// [VXG RealForever][VEXLIFE-ANDROID][CONNECTED-SERVICES][DEVICE-POSSESSION]