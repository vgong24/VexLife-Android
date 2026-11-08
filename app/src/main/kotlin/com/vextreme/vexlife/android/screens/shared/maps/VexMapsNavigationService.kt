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
import com.vextreme.vexlife.android.behavior.VexService
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch

/**
 * Android lifetime carrier for a user-started active VexMaps navigation session.
 *
 * Domain route progress remains in [VexNavigationSession]. This service owns foreground lifetime,
 * continuous Android location collection, and the persistent system notification.
 */
class VexMapsNavigationService :
    Service(),
    VexService<
        VexMapsNavigationService,
        VexNavigationSessionState
    > {
    private val executionRoot =
        VexExecutionRoot(
            dispatcher =
                Dispatchers.Main.immediate,
        )

    override val current:
        VexNavigationSessionState
        get() =
            VexMapsNavigationProcess
                .session
                .current

    private lateinit var locationSource:
        VexDeviceLocationSource
    private lateinit var navigationSpeech:
        VexNavigationSpeech

    private val speechGate =
        VexNavigationCueSpeechGate()

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
                    current
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
        executionRoot.forceStopAll(
            "VexMaps NavigationService destroyed",
        )

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

        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this,
            source =
                ::locationUpdatesForExecution,
            restartOnSourceFailure =
                false,
        ) { sample ->
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

        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this,
            source =
                ::navigationStatesForExecution,
            restartOnSourceFailure =
                false,
        ) { state ->
            updateNotification(
                state,
            )

            speechGate
                .nextCue(
                    state,
                )
                ?.let { cue ->
                    navigationSpeech.speak(
                        cue,
                    )
                }

            when {
                state.phase ==
                    VexNavigationSessionPhase.IDLE ->
                    finishForegroundService(
                        retainNotification =
                            false,
                    )

                shouldRetainVexMapsNavigationNotification(
                    state.phase,
                ) ->
                    finishForegroundService(
                        retainNotification =
                            true,
                    )
            }
        }
    }

    private fun locationUpdatesForExecution():
        Flow<
            VexNavigationLocationSample
        > =
        locationSource
            .navigationUpdates()
            .catch { error ->
                if (
                    error !is
                    CancellationException
                ) {
                    /*
                     * Preserve the route, but make shared Navigation truth say that the location
                     * stream is currently unavailable. Source recovery policy remains unchanged:
                     * this maintained observation is not automatically restarted.
                     */
                    VexMapsNavigationProcess
                        .session
                        .locationUnavailable()
                }
            }

    private fun navigationStatesForExecution():
        StateFlow<
            VexNavigationSessionState
        > =
        VexMapsNavigationProcess
            .session
            .states

    private fun stopNavigation() {
        stopping = true
        VexMapsNavigationProcess
            .stop()
        finishForegroundService(
            retainNotification = false,
        )
    }

    private fun finishForegroundService(
        retainNotification: Boolean,
    ) {
        VexExecution.forceStop(
            executionRoot =
                executionRoot,
            owner =
                this,
            source =
                ::locationUpdatesForExecution,
            reason =
                "VexMaps Navigation foreground realization ending",
        )

        ServiceCompat.stopForeground(
            this,
            if (retainNotification) {
                ServiceCompat.STOP_FOREGROUND_DETACH
            } else {
                ServiceCompat.STOP_FOREGROUND_REMOVE
            },
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
                )
                    .setAction(
                        MainActivity
                            .ACTION_OPEN_ACTIVE_VEXMAPS,
                    )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
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

        val retainAfterService =
            shouldRetainVexMapsNavigationNotification(
                state.phase,
            )

        val builder =
            NotificationCompat.Builder(
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
                !retainAfterService,
            )
            .setAutoCancel(
                retainAfterService,
            )
            .setOnlyAlertOnce(true)
            .setCategory(
                NotificationCompat.CATEGORY_NAVIGATION,
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW,
            )

        if (!retainAfterService) {
            builder.addAction(
                0,
                getString(
                    R.string
                        .vexmaps_navigation_stop,
                ),
                stopIntent,
            )
        }

        return builder.build()
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

        fun dismissArrivalNotification(
            context: Context,
        ) {
            context
                .getSystemService(
                    NotificationManager::class.java,
                )
                .cancel(
                    NOTIFICATION_ID,
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

internal fun shouldRetainVexMapsNavigationNotification(
    phase: VexNavigationSessionPhase,
): Boolean =
    phase ==
        VexNavigationSessionPhase.ARRIVED

// [VXG RealForever]
