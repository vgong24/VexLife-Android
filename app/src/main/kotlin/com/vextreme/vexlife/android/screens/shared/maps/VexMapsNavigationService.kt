package com.vextreme.vexlife.android.screens.shared.maps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Android lifetime carrier for a user-started active VexMaps navigation session.
 *
 * Domain route progress remains in [VexNavigationSession]. This service owns foreground lifetime,
 * continuous Android location collection, and the persistent system notification.
 */
class VexMapsNavigationService :
    Service() {
    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate,
        )

    private lateinit var locationSource:
        VexDeviceLocationSource
    private lateinit var navigationSpeech:
        VexNavigationSpeech

    private val speechGate =
        VexNavigationCueSpeechGate()

    private var locationJob: Job? = null
    private var stateJob: Job? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        locationSource =
            VexDeviceLocationSource(this)
        navigationSpeech =
            VexNavigationSpeech(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopNavigation()
                return START_NOT_STICKY
            }

            ACTION_START,
            null -> {
                when (
                    VexMapsNavigationProcess
                        .session
                        .current
                        .phase
                ) {
                    VexNavigationSessionPhase.READY ->
                        startPreparedNavigation()

                    VexNavigationSessionPhase.ACTIVE,
                    VexNavigationSessionPhase.OFF_ROUTE,
                    VexNavigationSessionPhase.LOCATION_HELD ->
                        Unit

                    VexNavigationSessionPhase.IDLE,
                    VexNavigationSessionPhase.ARRIVED ->
                        stopSelf()
                }
            }

            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? = null

    override fun onDestroy() {
        locationJob?.cancel()
        stateJob?.cancel()

        if (
            !stopping &&
            VexMapsNavigationProcess
                .session
                .current
                .phase !=
            VexNavigationSessionPhase.ARRIVED
        ) {
            VexMapsNavigationProcess
                .stop()
        }

        navigationSpeech.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startPreparedNavigation() {
        val session =
            VexMapsNavigationProcess.session

        if (
            session.current.phase !=
            VexNavigationSessionPhase.READY
        ) {
            stopSelf()
            return
        }

        startForegroundNow(
            session.current,
        )
        session.start()

        locationJob?.cancel()
        locationJob =
            serviceScope.launch {
                locationSource
                    .navigationUpdates()
                    .catch { error ->
                        if (
                            error !is
                            CancellationException
                        ) {
                            // Preserve the route, but make the shared session truth say that
                            // navigation currently lacks a usable location stream.
                            session.locationUnavailable()
                        }
                    }
                    .collect { sample ->
                        VexMapsNavigationProcess
                            .acceptLocation(
                                sample = sample,
                                nowElapsedRealtimeMillis =
                                    SystemClock
                                        .elapsedRealtime(),
                            )

                        session
                            .current
                            .pendingReroute
                            ?.let { request ->
                                runCatching {
                                    VexMapsNavigationProcess
                                        .reroutePending(
                                            context =
                                                this@VexMapsNavigationService,
                                            requestRef =
                                                request.requestRef,
                                        )
                                }.onFailure {
                                    session.rerouteFailed(
                                        request.requestRef,
                                    )
                                }
                            }
                    }
            }

        stateJob?.cancel()
        stateJob =
            serviceScope.launch {
                session.states.collect { state ->
                    updateNotification(state)

                    speechGate
                        .nextCue(state)
                        ?.let { cue ->
                            navigationSpeech.speak(
                                cue,
                            )
                        }

                    if (
                        state.phase ==
                            VexNavigationSessionPhase.IDLE ||
                        state.phase ==
                            VexNavigationSessionPhase.ARRIVED
                    ) {
                        finishForegroundService()
                    }
                }
            }
    }

    private fun stopNavigation() {
        stopping = true
        VexMapsNavigationProcess
            .stop()
        finishForegroundService()
    }

    private fun finishForegroundService() {
        locationJob?.cancel()
        locationJob = null
        ServiceCompat.stopForeground(
            this,
            ServiceCompat.STOP_FOREGROUND_REMOVE,
        )
        stopSelf()
    }

    private fun startForegroundNow(
        state: VexNavigationSessionState,
    ) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(state),
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
    }

    private fun updateNotification(
        state: VexNavigationSessionState,
    ) {
        getSystemService(
            NotificationManager::class.java,
        ).notify(
            NOTIFICATION_ID,
            buildNotification(state),
        )
    }

    private fun buildNotification(
        state: VexNavigationSessionState,
    ): Notification {
        val destination =
            state
                .routeResult
                ?.route
                ?.destinationLabel
                ?: getString(
                    R.string.vexmaps_title,
                )

        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(
                    this,
                    MainActivity::class.java,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(
                    this,
                    VexMapsNavigationService::class.java,
                ).setAction(
                    ACTION_STOP,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID,
        )
            .setSmallIcon(
                R.mipmap.ic_launcher,
            )
            .setContentTitle(
                getString(
                    R.string
                        .vexmaps_navigation_notification_title,
                    destination,
                ),
            )
            .setContentText(
                notificationText(state),
            )
            .setContentIntent(
                openIntent,
            )
            .setOngoing(
                state.phase !=
                    VexNavigationSessionPhase.ARRIVED,
            )
            .setOnlyAlertOnce(true)
            .setCategory(
                NotificationCompat.CATEGORY_NAVIGATION,
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW,
            )
            .addAction(
                0,
                getString(
                    R.string
                        .vexmaps_navigation_stop,
                ),
                stopIntent,
            )
            .build()
    }

    private fun notificationText(
        state: VexNavigationSessionState,
    ): String {
        if (
            state.phase ==
            VexNavigationSessionPhase.ARRIVED
        ) {
            return getString(
                R.string
                    .vexmaps_navigation_notification_arrived,
            )
        }

        if (
            state.phase ==
            VexNavigationSessionPhase.OFF_ROUTE
        ) {
            return getString(
                R.string
                    .vexmaps_navigation_notification_off_route,
            )
        }

        if (
            state.phase ==
            VexNavigationSessionPhase.LOCATION_HELD
        ) {
            return getString(
                R.string
                    .vexmaps_navigation_notification_location,
            )
        }

        val progress =
            state.convergence?.progress
        val next =
            progress?.nextManeuver

        if (
            next != null
        ) {
            return getString(
                R.string
                    .vexmaps_navigation_notification_turn,
                next
                    .maneuver
                    .instructionProjection,
                next
                    .distanceToManeuverMeters,
            )
        }

        return getString(
            R.string
                .vexmaps_navigation_notification_active,
            progress
                ?.distanceRemainingMeters
                ?: state
                    .routeResult
                    ?.route
                    ?.totalDistanceMeters
                ?: 0,
        )
    }

    private fun ensureNotificationChannel() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val manager =
            getSystemService(
                NotificationManager::class.java,
            )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(
                    R.string
                        .vexmaps_navigation_channel,
                ),
                NotificationManager
                    .IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val CHANNEL_ID =
            "vexmaps.navigation"
        private const val NOTIFICATION_ID =
            0x564D

        private const val ACTION_START =
            "com.vextreme.vexlife.android.vexmaps.START_NAVIGATION"
        private const val ACTION_STOP =
            "com.vextreme.vexlife.android.vexmaps.STOP_NAVIGATION"

        fun startPrepared(
            context: Context,
        ) {
            require(
                VexMapsNavigationProcess
                    .session
                    .current
                    .phase ==
                    VexNavigationSessionPhase.READY,
            ) {
                "navigation session is not prepared"
            }

            ContextCompat.startForegroundService(
                context,
                Intent(
                    context,
                    VexMapsNavigationService::class.java,
                ).setAction(
                    ACTION_START,
                ),
            )
        }

        fun stop(
            context: Context,
        ) {
            context.startService(
                Intent(
                    context,
                    VexMapsNavigationService::class.java,
                ).setAction(
                    ACTION_STOP,
                ),
            )
        }
    }
}

// [VXG RealForever]
