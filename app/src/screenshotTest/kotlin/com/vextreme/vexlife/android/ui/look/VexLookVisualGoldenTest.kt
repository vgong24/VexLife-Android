package com.vextreme.vexlife.android.ui.look

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreenContent
import com.vextreme.vexlife.android.screens.primary.settings.SettingsAppearanceContent
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreenContent
import com.vextreme.vexlife.android.shell.VexDeckFixture
import com.vextreme.vexlife.android.shell.fixtureDriving
import com.vextreme.vexlife.android.shell.fixtureFamily
import com.vextreme.vexlife.android.shell.fixtureJourneyStage
import com.vextreme.vexlife.android.shell.fixtureNavigation
import com.vextreme.vexlife.android.shell.fixturePlaying

/*
 * Semantic visual cases: Looks.
 *
 * The same driving scenario rendered in every Look, plus the Appearance screen itself, so a reviewer
 * can see the personality layer change while meaning and layout stay identical.
 */

private val live =
    VexAmbientLiveness(
        musicPlaying = true,
        navigating = true,
    )

@Composable
private fun DrivingHome(
    id: String,
    look: VexLookPreference,
) {
    VexPreviewHost(
        id = id,
        startScreen = HomeScreen,
        look = look,
        ambientLiveness = live,
        deck = VexDeckFixture(live = fixtureDriving),
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            live = fixtureDriving,
            journey = fixtureJourneyStage,
            musicPaired = true,
        )
    }
}

@Composable
private fun Appearance(
    id: String,
    look: VexLookPreference,
) {
    VexPreviewHost(
        id = id,
        startScreen = SettingsScreen,
        look = look,
    ) { context ->
        SettingsAppearanceContent(
            runtime = context.runtime,
            preference = look,
            onChoose = {},
        )
    }
}

@PreviewTest
@Preview(name = "Appearance · Nocturne", widthDp = 412, heightDp = 1180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun appearanceNocturneGolden() =
    Appearance("golden-appearance-nocturne", VexLookPreference())

@PreviewTest
@Preview(name = "Appearance · Paper · light", widthDp = 412, heightDp = 1180, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun appearancePaperLightGolden() =
    Appearance(
        "golden-appearance-paper",
        VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Paper)),
    )

@PreviewTest
@Preview(name = "Look · Aurora · driving", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun lookAuroraDrivingGolden() =
    DrivingHome("golden-look-aurora", VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Aurora)))

@PreviewTest
@Preview(name = "Look · Ember · driving", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun lookEmberDrivingGolden() =
    DrivingHome("golden-look-ember", VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Ember)))

@PreviewTest
@Preview(name = "Look · Tide · driving", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun lookTideDrivingGolden() =
    DrivingHome("golden-look-tide", VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Tide)))

@PreviewTest
@Preview(name = "Look · Paper · driving · light", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun lookPaperDrivingLightGolden() =
    DrivingHome("golden-look-paper", VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Paper)))

@PreviewTest
@Preview(name = "Look · Your color 330 · driving", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun lookCustomDrivingGolden() =
    DrivingHome("golden-look-custom", VexLookPreference(choice = VexLookChoice.Custom(330)))

@PreviewTest
@Preview(name = "Look · Aurora · Talk to Vex", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun lookAuroraTalkToVexGolden() {
    VexPreviewHost(
        id = "golden-look-aurora-lens",
        startScreen = TalkToVexScreen,
        look = VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Aurora)),
        ambientLiveness = live,
    ) {
        TalkToVexScreenContent(
            nowPlaying = fixturePlaying,
            navigation = fixtureNavigation,
            family = fixtureFamily,
        )
    }
}

// [VXG RealForever][UI][LOOK][VISUAL-GOLDEN]
