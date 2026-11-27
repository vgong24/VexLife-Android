package com.vextreme.vexlife.android.screens.primary.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.preview.VexRenderedScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.ui.currentVexThemePresentation
import com.vextreme.vexlife.android.ui.look.LocalVexLook
import com.vextreme.vexlife.android.ui.look.VexLookChoice
import com.vextreme.vexlife.android.ui.look.VexLookPreference
import com.vextreme.vexlife.android.ui.look.VexLookPreset
import com.vextreme.vexlife.android.ui.look.VexMotion
import com.vextreme.vexlife.android.ui.look.VexTone
import com.vextreme.vexlife.android.ui.look.family
import com.vextreme.vexlife.android.ui.look.rememberVexLook
import com.vextreme.vexlife.android.ui.look.spec

/**
 * Settings, starting with Appearance: make VexLife yours.
 *
 * Every Look tile is a true miniature rendered in that Look's own palette, ambient light and display
 * face, so a person sees what they will get before choosing it. Choices go through the Look Family
 * (admitted at once, saved durably); this Screen owns only the presentation.
 */
object SettingsScreen : AppScreenBlueprint(
    screenKey = "settings",
    titleRes = R.string.settings_title,
) {
    val AppearanceSection = columnRef(
        "body.appearance",
    )

    /** One registered identity for every Look tile; each tile activates it as a dynamic row. */
    val LookTile = buttonRef(
        id = "body.appearance.look",
        labelRes = R.string.appearance_choose_look,
        style = VexElementStyles.Default,
        intent = localEffect(),
    )

    val HueSwatch = buttonRef(
        id = "body.appearance.hue",
        labelRes = R.string.appearance_choose_hue,
        style = VexElementStyles.Default,
        intent = localEffect(),
    )

    val MotionLively = buttonRef(
        id = "body.appearance.motion.lively",
        labelRes = R.string.appearance_motion_lively,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val MotionCalm = buttonRef(
        id = "body.appearance.motion.calm",
        labelRes = R.string.appearance_motion_calm,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val AmbientToggle = buttonRef(
        id = "body.appearance.ambient",
        labelRes = R.string.appearance_ambient,
        style = VexElementStyles.Default,
        intent = localEffect(),
    )

    override val content = layout {
        scrollableColumn("body") {
            column(
                element = AppearanceSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(LookTile)
                button(HueSwatch)
                button(MotionLively)
                button(MotionCalm)
                button(AmbientToggle)
            }
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
    ) {
        val look = rememberVexLook()

        SettingsAppearanceContent(
            runtime = runtime,
            preference = look?.preference ?: LocalVexLook.current,
            onChoose = { chosen -> look?.choose?.invoke(chosen) },
        )
    }
}

/** Hues offered for "your own color", spread around the OKLCH wheel. */
internal val VexAppearanceHues = listOf(10, 40, 75, 120, 160, 195, 225, 260, 290, 330)

@Composable
internal fun SettingsAppearanceContent(
    runtime: VexRuntime,
    preference: VexLookPreference,
    onChoose: (VexLookPreference) -> Unit,
) {
    val dark = !currentVexThemePresentation().lightSystemBars
    val customHue = (preference.choice as? VexLookChoice.Custom)?.hueDegrees ?: 200
    val tiles: List<VexLookChoice> =
        VexLookPreset.entries.map { VexLookChoice.Preset(it) } + VexLookChoice.Custom(customHue)

    fun activate(element: ButtonElement, key: String, chosen: VexLookPreference) {
        runtime.activateDynamic(element, subjectRef = element.elementId + "." + key)
        onChoose(chosen)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.appearance_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        VexContainer(
            element = SettingsScreen.AppearanceSection,
            style = VexElementStyles.RaisedSurface,
        ) {
            SectionLabel(id = "look-label", text = stringResource(R.string.appearance_look))
            tiles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { choice ->
                        val key = choice.tileKey()
                        LookTile(
                            choice = choice,
                            key = key,
                            selected =
                                when (choice) {
                                    is VexLookChoice.Preset -> preference.choice == choice
                                    is VexLookChoice.Custom -> preference.choice is VexLookChoice.Custom
                                },
                            dark = dark,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activate(SettingsScreen.LookTile, key, preference.copy(choice = choice))
                            },
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            SectionLabel(id = "hue-label", text = stringResource(R.string.appearance_your_color))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .vex(SettingsScreen.HueSwatch),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                VexAppearanceHues.forEach { hue ->
                    val selected =
                        (preference.choice as? VexLookChoice.Custom)?.hueDegrees == hue
                    HueSwatch(
                        hue = hue,
                        selected = selected,
                        onClick = {
                            activate(
                                SettingsScreen.HueSwatch,
                                "hue-$hue",
                                preference.copy(choice = VexLookChoice.Custom(hue)),
                            )
                        },
                    )
                }
            }

            SectionLabel(id = "motion-label", text = stringResource(R.string.appearance_motion))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MotionChoice(
                    element = SettingsScreen.MotionLively,
                    title = stringResource(R.string.appearance_motion_lively),
                    body = stringResource(R.string.appearance_motion_lively_body),
                    selected = preference.motion == VexMotion.Lively,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activate(SettingsScreen.MotionLively, "lively", preference.copy(motion = VexMotion.Lively))
                    },
                )
                MotionChoice(
                    element = SettingsScreen.MotionCalm,
                    title = stringResource(R.string.appearance_motion_calm),
                    body = stringResource(R.string.appearance_motion_calm_body),
                    selected = preference.motion == VexMotion.Calm,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activate(SettingsScreen.MotionCalm, "calm", preference.copy(motion = VexMotion.Calm))
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .vex(SettingsScreen.AmbientToggle)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        activate(
                            SettingsScreen.AmbientToggle,
                            if (preference.ambient) "off" else "on",
                            preference.copy(ambient = !preference.ambient),
                        )
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    VexText(
                        id = "ambient-title",
                        text = stringResource(R.string.appearance_ambient),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    VexText(
                        id = "ambient-body",
                        text = stringResource(R.string.appearance_ambient_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = preference.ambient,
                    onCheckedChange = null,
                )
            }
        }

        Text(
            text = stringResource(R.string.appearance_saved_note),
            // End padding keeps the note clear of the shell FAB.
            modifier = Modifier.padding(start = 4.dp, end = 88.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Room for the shell FAB.
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun SectionLabel(
    id: String,
    text: String,
) {
    VexText(
        id = id,
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A miniature of one Look, drawn entirely in that Look's own palette, light and display face. */
@Composable
private fun LookTile(
    choice: VexLookChoice,
    key: String,
    selected: Boolean,
    dark: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val spec = remember(choice) { choice.spec() }
    val scheme = remember(spec, dark) { spec.colorScheme(dark) }
    val light = remember(spec, dark) { spec.ambientColors(dark) }
    val corner = (spec.cornerDp * 0.8f).dp
    val shape = RoundedCornerShape(corner)
    val name =
        when (choice) {
            is VexLookChoice.Preset -> stringResource(choice.preset.nameRes)
            is VexLookChoice.Custom -> stringResource(R.string.look_custom)
        }
    val mood =
        when (choice) {
            is VexLookChoice.Preset -> stringResource(choice.preset.moodRes)
            is VexLookChoice.Custom -> stringResource(R.string.look_custom_mood)
        }

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else scheme.outlineVariant,
                shape = shape,
            )
            .background(scheme.background)
            .drawBehind {
                val r = size.maxDimension * 0.75f
                listOf(
                    Offset(size.width * 0.1f, size.height * 0.05f) to light[0],
                    Offset(size.width * 1.0f, size.height * 0.75f) to light[1],
                ).forEach { (center, color) ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = 0.55f * spec.ambientIntensity), Color.Transparent),
                            center = center,
                            radius = r,
                        ),
                        radius = r,
                        center = center,
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Mini "Now" card in this Look.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerHigh, RoundedCornerShape(corner * 0.7f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(scheme.primaryContainer, RoundedCornerShape(corner * 0.35f)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .background(scheme.onSurface.copy(alpha = 0.85f), CircleShape),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(5.dp)
                        .background(scheme.onSurfaceVariant.copy(alpha = 0.7f), CircleShape),
                )
            }
            Box(
                Modifier
                    .size(18.dp)
                    .background(
                        Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer)),
                        CircleShape,
                    ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VexText(
                id = "look-$key-name",
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = spec.displayFace.family,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = scheme.onSurface,
                maxLines = 1,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                    )
                }
            }
        }
        VexText(
            id = "look-$key-mood",
            text = mood,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(scheme.primary, scheme.tertiary, scheme.secondary).forEach { accent ->
                Box(
                    Modifier
                        .size(10.dp)
                        .background(accent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun HueSwatch(
    hue: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = remember(hue) { VexTone(0.70, 0.15, hue.toDouble()).toColor() }

    Box(
        modifier = Modifier
            .size(28.dp)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape,
            )
            .padding(if (selected) 4.dp else 2.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun MotionChoice(
    element: ButtonElement,
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .vex(element)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color =
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun VexLookChoice.tileKey(): String =
    when (this) {
        is VexLookChoice.Preset -> preset.key
        is VexLookChoice.Custom -> "custom"
    }

@VexScreenPreviews
@Composable
private fun SettingsScreenPreview() {
    VexRenderedScreenPreview(SettingsScreen)
}

// [VXG RealForever]
