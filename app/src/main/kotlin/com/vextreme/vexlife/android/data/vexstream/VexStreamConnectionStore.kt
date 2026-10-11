package com.vextreme.vexlife.android.data.vexstream

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VexStreamPairedConnection(
    val displayName: String,
    val host: String,
    val port: Int,
    val certificateSha256: String,
    val token: String,
    val remoteHomeRef: String? = null,
    val remoteGatewayPublicKey: String? = null,
    val remoteRelayBaseUrl: String? = null,
) {
    init {
        require(displayName.isNotBlank())
        require(host.isNotBlank())
        require(port in 1..65535)
        require(certificateSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(token.isNotBlank())
        require(remoteHomeRef == null || remoteHomeRef.isNotBlank())
        require(remoteGatewayPublicKey == null || remoteGatewayPublicKey.isNotBlank())
        require(remoteRelayBaseUrl == null || remoteRelayBaseUrl.startsWith("https://"))
    }

    val remoteReady: Boolean
        get() =
            !remoteHomeRef.isNullOrBlank() &&
                !remoteGatewayPublicKey.isNullOrBlank() &&
                !remoteRelayBaseUrl.isNullOrBlank()

    fun endpointBaseUrl(): String {
        val normalizedHost =
            if (":" in host && !host.startsWith("[")) {
                "[$host]"
            } else {
                host
            }
        return "https://$normalizedHost:$port"
    }
}

/**
 * App-private retained VexStream pairing.
 *
 * Endpoint identity is the exact TLS certificate digest selected/confirmed during pairing. The
 * bearer token is encrypted with an Android Keystore key before SharedPreferences persistence.
 *
 * This remains local VexStream product pairing state; it is not canonical Home/Safety membership,
 * authentication, authorization, or a capability lease.
 */
class VexStreamConnectionStore(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val preferences =
        applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun deviceRef(): String {
        preferences.getString(KEY_DEVICE_REF, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val formed = "vexlife.android." + UUID.randomUUID().toString()
        preferences.edit()
            .putString(KEY_DEVICE_REF, formed)
            .apply()
        return formed
    }

    fun deviceSigningPublicKey(): String {
        val keyStore =
            KeyStore.getInstance(ANDROID_KEY_STORE)
                .apply {
                    load(null)
                }
        val existing =
            keyStore.getCertificate(DEVICE_SIGNING_KEY_ALIAS)
                ?.publicKey

        val publicKey =
            existing
                ?: KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    ANDROID_KEY_STORE,
                ).run {
                    initialize(
                        KeyGenParameterSpec.Builder(
                            DEVICE_SIGNING_KEY_ALIAS,
                            KeyProperties.PURPOSE_SIGN or
                                KeyProperties.PURPOSE_VERIFY,
                        )
                            .setAlgorithmParameterSpec(
                                ECGenParameterSpec("secp256r1"),
                            )
                            .setDigests(
                                KeyProperties.DIGEST_SHA256,
                            )
                            .build(),
                    )
                    generateKeyPair().public
                }

        return Base64.encodeToString(
            publicKey.encoded,
            Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    fun signRemotePayload(
        payload: ByteArray,
    ): String {
        deviceSigningPublicKey()
        val keyStore =
            KeyStore.getInstance(ANDROID_KEY_STORE)
                .apply {
                    load(null)
                }
        val privateKey =
            requireNotNull(
                keyStore.getKey(
                    DEVICE_SIGNING_KEY_ALIAS,
                    null,
                ),
            )

        val signature =
            Signature.getInstance("SHA256withECDSA")
                .apply {
                    initSign(
                        privateKey as java.security.PrivateKey,
                    )
                    update(payload)
                }
                .sign()

        return Base64.encodeToString(
            signature,
            Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    fun deviceDisplayName(): String =
        buildString {
            val manufacturer = Build.MANUFACTURER
                .orEmpty()
                .trim()
            val model = Build.MODEL
                .orEmpty()
                .trim()

            if (manufacturer.isNotBlank() && !model.startsWith(manufacturer, ignoreCase = true)) {
                append(
                    manufacturer.replaceFirstChar { char ->
                        char.titlecase()
                    },
                )
                append(' ')
            }

            append(
                model.ifBlank {
                    "Android phone"
                },
            )
        }

    fun load(): VexStreamPairedConnection? =
        runCatching {
            val schema =
                preferences.getString(
                    KEY_SCHEMA,
                    null,
                )
            if (
                schema != SCHEMA_VERSION &&
                schema != LEGACY_SCHEMA_VERSION
            ) {
                return null
            }

            VexStreamPairedConnection(
                displayName = preferences.requireString(KEY_DISPLAY_NAME),
                host = preferences.requireString(KEY_HOST),
                port = preferences.getInt(KEY_PORT, -1),
                certificateSha256 = preferences.requireString(KEY_CERTIFICATE_SHA256),
                token = decrypt(
                    preferences.requireString(KEY_ENCRYPTED_TOKEN),
                ),
                remoteHomeRef =
                    preferences.getString(
                        KEY_REMOTE_HOME_REF,
                        null,
                    ),
                remoteGatewayPublicKey =
                    preferences.getString(
                        KEY_REMOTE_GATEWAY_PUBLIC_KEY,
                        null,
                    ),
                remoteRelayBaseUrl =
                    preferences.getString(
                        KEY_REMOTE_RELAY_BASE_URL,
                        null,
                    ),
            )
        }.getOrNull()

    fun save(
        connection: VexStreamPairedConnection,
    ) {
        preferences.edit()
            .putString(KEY_SCHEMA, SCHEMA_VERSION)
            .putString(KEY_DISPLAY_NAME, connection.displayName)
            .putString(KEY_HOST, connection.host)
            .putInt(KEY_PORT, connection.port)
            .putString(KEY_CERTIFICATE_SHA256, connection.certificateSha256)
            .putString(KEY_ENCRYPTED_TOKEN, encrypt(connection.token))
            .putString(KEY_REMOTE_HOME_REF, connection.remoteHomeRef)
            .putString(
                KEY_REMOTE_GATEWAY_PUBLIC_KEY,
                connection.remoteGatewayPublicKey,
            )
            .putString(
                KEY_REMOTE_RELAY_BASE_URL,
                connection.remoteRelayBaseUrl,
            )
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_SCHEMA)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_HOST)
            .remove(KEY_PORT)
            .remove(KEY_CERTIFICATE_SHA256)
            .remove(KEY_ENCRYPTED_TOKEN)
            .remove(KEY_REMOTE_HOME_REF)
            .remove(KEY_REMOTE_GATEWAY_PUBLIC_KEY)
            .remove(KEY_REMOTE_RELAY_BASE_URL)
            .apply()
    }

    private fun encrypt(
        value: String,
    ): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey(),
        )
        val ciphertext =
            cipher.doFinal(
                value.toByteArray(StandardCharsets.UTF_8),
            )

        return Base64.encodeToString(
            cipher.iv + ciphertext,
            Base64.NO_WRAP,
        )
    }

    private fun decrypt(
        encoded: String,
    ): String {
        val payload =
            Base64.decode(
                encoded,
                Base64.NO_WRAP,
            )
        require(payload.size > IV_LENGTH_BYTES)

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(
            IV_LENGTH_BYTES,
            payload.size,
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                iv,
            ),
        )

        return String(
            cipher.doFinal(ciphertext),
            StandardCharsets.UTF_8,
        )
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(ANDROID_KEY_STORE)
                .apply {
                    load(null)
                }

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
            ?.let { return it }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE,
            )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE,
                )
                .build(),
        )

        return keyGenerator.generateKey()
    }

    private fun android.content.SharedPreferences.requireString(
        key: String,
    ): String =
        requireNotNull(
            getString(key, null),
        ) {
            "missing VexStream connection field $key"
        }

    companion object {
        private const val PREFERENCES_NAME = "vexstream-mobile-connection"
        private const val LEGACY_SCHEMA_VERSION = "vexstream.android.local-pairing/v1"
        private const val SCHEMA_VERSION = "vexstream.android.local-pairing/v2"

        private const val KEY_SCHEMA = "schema"
        private const val KEY_DEVICE_REF = "deviceRef"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_CERTIFICATE_SHA256 = "certificateSha256"
        private const val KEY_ENCRYPTED_TOKEN = "encryptedToken"
        private const val KEY_REMOTE_HOME_REF = "remoteHomeRef"
        private const val KEY_REMOTE_GATEWAY_PUBLIC_KEY = "remoteGatewayPublicKey"
        private const val KEY_REMOTE_RELAY_BASE_URL = "remoteRelayBaseUrl"

        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "vexstream.mobile.connection.token.v1"
        private const val DEVICE_SIGNING_KEY_ALIAS =
            "vexstream.mobile.device.signing.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}

// [VXG RealForever]
