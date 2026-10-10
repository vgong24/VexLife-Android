package com.vextreme.vexlife.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vex.android.architecture.relay.VexRelay

class MainActivity : ComponentActivity() {
    private val screenPresenceRelay: VexRelay<FoundationScreenEvent, FoundationScreenPresence> =
        FoundationContract.screenPresenceRelay()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val screenState by screenPresenceRelay.state.collectAsState()

            VexLifeAndroidFoundation(
                screenPresence = screenState.output,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        screenPresenceRelay.accept(FoundationScreenEvent.ENTER)
    }

    override fun onStop() {
        screenPresenceRelay.accept(FoundationScreenEvent.EXIT)
        super.onStop()
    }
}

@Composable
private fun VexLifeAndroidFoundation(
    screenPresence: FoundationScreenPresence,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag(FoundationContract.REF),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.foundation_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.foundation_stage),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = stringResource(R.string.foundation_body))
                Text(
                    text = stringResource(
                        R.string.relay_screen_presence,
                        screenPresence.name,
                    ),
                )
            }
        }
    }
}
