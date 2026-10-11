package com.vextreme.vexlife.android.data.vexstream

import android.content.Context
import android.net.nsd.DiscoveryRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

data class VexStreamSelectedService(
    val displayName: String,
    val host: String,
    val port: Int,
    val certificateSha256: String,
) {
    init {
        require(displayName.isNotBlank())
        require(host.isNotBlank())
        require(port in 1..65535)
        require(certificateSha256.matches(Regex("^[0-9a-f]{64}$")))
    }
}

/**
 * Human-mediated nearby VexStream selection.
 *
 * Android 17+ uses the platform NSD picker so VexLife gets access only to the service the human
 * selects, without requesting broad ACCESS_LOCAL_NETWORK permission. Older Android releases use
 * ordinary NSD discovery and resolve the first compatible VexStream service.
 */
class VexStreamNearbyDiscovery(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val nsdManager =
        applicationContext.getSystemService(
            NsdManager::class.java,
        )

    suspend fun selectService(): VexStreamSelectedService =
        withTimeout(DISCOVERY_TIMEOUT_MS) {
            if (Build.VERSION.SDK_INT >= 37) {
                selectWithSystemPicker()
            } else {
                selectWithClassicDiscovery()
            }
        }

    @RequiresApi(37)
    private suspend fun selectWithSystemPicker(): VexStreamSelectedService =
        suspendCancellableCoroutine { continuation ->
            val request =
                DiscoveryRequest.Builder(
                    SERVICE_TYPE,
                )
                    .setFlags(
                        DiscoveryRequest.FLAG_SHOW_PICKER,
                    )
                    .build()

            lateinit var callback: NsdManager.ServiceInfoCallback
            callback =
                object : NsdManager.ServiceInfoCallback {
                    override fun onServiceUpdated(
                        serviceInfo: NsdServiceInfo,
                    ) {
                        val selected =
                            serviceInfo.toSelectedService()
                                ?: return

                        if (continuation.isActive) {
                            continuation.resume(selected)
                        }
                        runCatching {
                            nsdManager.unregisterServiceInfoCallback(
                                callback,
                            )
                        }
                    }

                    override fun onServiceLost() = Unit

                    override fun onServiceInfoCallbackRegistrationFailed(
                        errorCode: Int,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "VexStream service picker unavailable: $errorCode",
                                ),
                            )
                        }
                    }

                    override fun onServiceInfoCallbackUnregistered() {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "VexStream service selection ended",
                                ),
                            )
                        }
                    }
                }

            continuation.invokeOnCancellation {
                runCatching {
                    nsdManager.unregisterServiceInfoCallback(
                        callback,
                    )
                }
            }

            runCatching {
                nsdManager.registerServiceInfoCallback(
                    request,
                    applicationContext.mainExecutor,
                    callback,
                )
            }.onFailure { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun selectWithClassicDiscovery(): VexStreamSelectedService =
        suspendCancellableCoroutine { continuation ->
            lateinit var discoveryListener: NsdManager.DiscoveryListener

            fun stopDiscovery() {
                runCatching {
                    nsdManager.stopServiceDiscovery(
                        discoveryListener,
                    )
                }
            }

            discoveryListener =
                object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(
                        serviceType: String,
                    ) = Unit

                    override fun onServiceFound(
                        serviceInfo: NsdServiceInfo,
                    ) {
                        if (
                            !serviceInfo.serviceType
                                .startsWith(SERVICE_TYPE)
                        ) {
                            return
                        }

                        nsdManager.resolveService(
                            serviceInfo,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(
                                    failedServiceInfo: NsdServiceInfo,
                                    errorCode: Int,
                                ) = Unit

                                override fun onServiceResolved(
                                    resolvedServiceInfo: NsdServiceInfo,
                                ) {
                                    val selected =
                                        resolvedServiceInfo.toSelectedService()
                                            ?: return

                                    if (continuation.isActive) {
                                        continuation.resume(selected)
                                        stopDiscovery()
                                    }
                                }
                            },
                        )
                    }

                    override fun onServiceLost(
                        serviceInfo: NsdServiceInfo,
                    ) = Unit

                    override fun onDiscoveryStopped(
                        serviceType: String,
                    ) = Unit

                    override fun onStartDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) {
                        stopDiscovery()
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "VexStream discovery could not start: $errorCode",
                                ),
                            )
                        }
                    }

                    override fun onStopDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) = Unit
                }

            continuation.invokeOnCancellation {
                stopDiscovery()
            }

            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener,
            )
        }

    private fun NsdServiceInfo.toSelectedService(): VexStreamSelectedService? {
        val fingerprint =
            attributes[CERTIFICATE_ATTRIBUTE]
                ?.toString(Charsets.UTF_8)
                ?.lowercase()
                ?.takeIf {
                    it.matches(
                        Regex("^[0-9a-f]{64}$"),
                    )
                }
                ?: return null

        val hostAddress =
            resolvedHostAddresses()
                .sortedWith(
                    compareByDescending<InetAddress> {
                        it is Inet4Address
                    },
                )
                .firstOrNull()
                ?.hostAddress
                ?.substringBefore('%')
                ?: return null

        return VexStreamSelectedService(
            displayName = serviceName
                .takeIf { it.isNotBlank() }
                ?: "VexStream",
            host = hostAddress,
            port = port,
            certificateSha256 = fingerprint,
        )
    }

    @Suppress("DEPRECATION")
    private fun NsdServiceInfo.resolvedHostAddresses(): List<InetAddress> =
        if (Build.VERSION.SDK_INT >= 34) {
            hostAddresses
        } else {
            listOfNotNull(host)
        }

    companion object {
        const val SERVICE_TYPE = "_vexstream._tcp"
        private const val CERTIFICATE_ATTRIBUTE = "certificateSha256"
        private const val DISCOVERY_TIMEOUT_MS = 45_000L
    }
}

// [VXG RealForever]
