package com.vextreme.vexlife.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.ui.VexLifeApp
import com.vextreme.vexlife.android.ui.VexLifeViewModel

internal fun resolveVexLifeExternalScreen(
    action: String?,
): ScreenElement? =
    when (action) {
        MainActivity.ACTION_OPEN_ACTIVE_VEXMAPS ->
            VexMapsScreen

        MainActivity.ACTION_OPEN_VEXSTREAM_FULL_PLAYER ->
            VexStreamMusicScreen

        else ->
            null
    }

class MainActivity : ComponentActivity() {
    private val sessionViewModel:
        VexLifeViewModel by viewModels()

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleExternalIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            VexLifeApp()
        }
    }

    override fun onNewIntent(
        intent: Intent,
    ) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(
        intent: Intent?,
    ) {
        val action =
            intent
                ?.action
                ?: intent
                    ?.getStringExtra(
                        VEXSTREAM_WIDGET_DESTINATION_EXTRA,
                    )
                    ?.takeIf {
                        it ==
                            VEXSTREAM_WIDGET_DESTINATION_FULL_PLAYER
                    }
                    ?.let {
                        ACTION_OPEN_VEXSTREAM_FULL_PLAYER
                    }
        val screen =
            resolveVexLifeExternalScreen(
                action,
            ) ?: return

        val runtime =
            sessionViewModel
                .session
                .runtime

        /*
         * A cold platform entry reaches this before Compose's app-open effect. Record the session
         * opening first so external navigation remains semantically ordered. Warm activity delivery
         * is a no-op here because VexRuntime records AppOpened once.
         */
        runtime.appOpened()
        runtime.openExternalScreen(
            subjectRef =
                when (
                    action
                ) {
                    ACTION_OPEN_VEXSTREAM_FULL_PLAYER ->
                        VEXSTREAM_WIDGET_NOW_PLAYING_REF

                    else ->
                        VEXMAPS_NAVIGATION_NOTIFICATION_REF
                },
            screen = screen,
        )

        if (
            action ==
            ACTION_OPEN_VEXSTREAM_FULL_PLAYER
        ) {
            /*
             * External widget entry is not a synthetic click on OpenFullPlayer. Navigation remains
             * VexRuntime-owned; this only establishes the already-registered VexStream presentation
             * destination after that Screen is current.
             */
            listOf(
                VexStreamMusicScreen.SearchSheet,
                VexStreamMusicScreen.OrganizationSheet,
                VexStreamMusicScreen.ConnectionMenuSheet,
                VexStreamMusicScreen.ForgetConfirmationDialog,
            ).forEach { presentation ->
                if (
                    runtime.presentation
                        .isOpen(
                            presentation,
                        )
                ) {
                    runtime.presentation
                        .close(
                            presentation,
                        )
                }
            }

            runtime.presentation
                .select(
                    container =
                        VexStreamMusicScreen
                            .FullPlayerContent,
                    child =
                        VexStreamMusicScreen
                            .FullPlayerNowPlayingPanel,
                )
            runtime.presentation
                .open(
                    VexStreamMusicScreen
                        .FullPlayerSheet,
                )
        }
    }

    companion object {
        internal const val ACTION_OPEN_ACTIVE_VEXMAPS =
            "com.vextreme.vexlife.android.vexmaps.OPEN_ACTIVE_NAVIGATION"
        internal const val ACTION_OPEN_VEXSTREAM_FULL_PLAYER =
            "com.vextreme.vexlife.android.vexstream.OPEN_FULL_PLAYER"

        internal const val VEXMAPS_NAVIGATION_NOTIFICATION_REF =
            "presentation.vexmaps.navigation.notification"
        internal const val VEXSTREAM_WIDGET_NOW_PLAYING_REF =
            "presentation.vexstream.playback-widget.now-playing"

        internal const val VEXSTREAM_WIDGET_DESTINATION_EXTRA =
            "vexstream.widget.destination"
        internal const val VEXSTREAM_WIDGET_DESTINATION_FULL_PLAYER =
            "full-player"
    }
}
