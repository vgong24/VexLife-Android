package com.vextreme.vexlife.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.ui.VexLifeApp
import com.vextreme.vexlife.android.ui.VexLifeViewModel

internal fun resolveVexLifeExternalScreen(
    action: String?,
): ScreenElement? =
    when (action) {
        MainActivity.ACTION_OPEN_ACTIVE_VEXMAPS ->
            VexMapsScreen

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
        val screen =
            resolveVexLifeExternalScreen(
                intent?.action,
            ) ?: return

        val runtime =
            sessionViewModel
                .session
                .runtime

        /*
         * A cold notification entry reaches this before Compose's app-open effect. Record the
         * session opening first so the external navigation remains semantically ordered. Warm
         * activity delivery is a no-op here because VexRuntime records AppOpened once.
         */
        runtime.appOpened()
        runtime.openExternalScreen(
            subjectRef =
                VEXMAPS_NAVIGATION_NOTIFICATION_REF,
            screen = screen,
        )
    }

    companion object {
        internal const val ACTION_OPEN_ACTIVE_VEXMAPS =
            "com.vextreme.vexlife.android.vexmaps.OPEN_ACTIVE_NAVIGATION"
        internal const val VEXMAPS_NAVIGATION_NOTIFICATION_REF =
            "presentation.vexmaps.navigation.notification"
    }
}
