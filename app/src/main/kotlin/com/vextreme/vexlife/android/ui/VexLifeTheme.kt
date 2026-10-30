package com.vextreme.vexlife.android.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.vextreme.vexlife.android.humanvetting.VettedWithHuman

object VexMotionTokens {
    const val TactileMs = 90
    const val FastMs = 140
    const val SurfaceMs = 200
    const val LayoutMs = 280
    const val SpatialMs = 340
    const val SemanticExitMs = 260
    const val SemanticArriveMs = 160
}

object VexLayoutTokens {
    val ToolbarHeight = 70.dp
    val BottomBarHeight = 70.dp
}

@VettedWithHuman("VexThemePresentation", "2026-10-29T22:46:00-07:00", "Victor Approved")
internal sealed interface VexThemePresentation {
    val materialColorScheme:
        ColorScheme

    val lightSystemBars:
        Boolean

    val darkModeControlChecked:
        Boolean

    data object Unresolved :
        VexThemePresentation {
        override val materialColorScheme:
            ColorScheme
            get() =
                error(
                    "ThemeMode must be resolved before VexLifeTheme renders",
                )

        override val lightSystemBars:
            Boolean
            get() =
                error(
                    "ThemeMode must be resolved before system-bar presentation",
                )

        override val darkModeControlChecked:
            Boolean
            get() =
                error(
                    "ThemeMode must be resolved before control presentation",
                )
    }

    data object Light :
        VexThemePresentation {
        override val materialColorScheme =
            lightColorScheme(
                primary = Color(0xFF17181B),
                onPrimary = Color.White,
                secondary = Color(0xFF315D8A),
                onSecondary = Color.White,
                tertiary = Color(0xFF805500),
                background = Color(0xFFF2F2F0),
                onBackground = Color(0xFF17181B),
                surface = Color.White,
                onSurface = Color(0xFF17181B),
                surfaceVariant = Color(0xFFF5F5F3),
                onSurfaceVariant = Color(0xFF565861),
                surfaceContainer = Color(0xFFF5F5F3),
                surfaceContainerHigh = Color(0xFFECECEA),
                outline = Color(0xFFAAA9A4),
                outlineVariant = Color(0xFFD6D6D2),
            )

        override val lightSystemBars =
            true

        override val darkModeControlChecked =
            false
    }

    data object Dark :
        VexThemePresentation {
        override val materialColorScheme =
            darkColorScheme(
                primary = Color(0xFFF2F2EF),
                onPrimary = Color(0xFF111216),
                secondary = Color(0xFF9BB7D4),
                onSecondary = Color(0xFF111216),
                tertiary = Color(0xFFE9C46A),
                background = Color(0xFF0F1012),
                onBackground = Color(0xFFF5F5F4),
                surface = Color(0xFF17181C),
                onSurface = Color(0xFFF5F5F4),
                surfaceVariant = Color(0xFF202126),
                onSurfaceVariant = Color(0xFFB7B8BF),
                surfaceContainer = Color(0xFF202126),
                surfaceContainerHigh = Color(0xFF272930),
                outline = Color(0xFF5A5D68),
                outlineVariant = Color(0xFF383A42),
            )

        override val lightSystemBars =
            false

        override val darkModeControlChecked =
            true
    }
}

@VettedWithHuman("LocalVexThemePresentation", "2026-10-29T22:46:00-07:00", "Victor Approved")
private val LocalVexThemePresentation =
    staticCompositionLocalOf<VexThemePresentation> {
        error(
            "Vex theme presentation requires an enclosing VexLifeTheme",
        )
    }

@Composable
@VettedWithHuman("currentVexThemePresentation", "2026-10-29T22:46:00-07:00", "Victor Approved")
internal fun currentVexThemePresentation():
    VexThemePresentation =
    LocalVexThemePresentation.current

private val VexTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
    ),
)

/**
 * App theme owns both Compose colors and the matching Android system-bar appearance.
 *
 * The root Surface deliberately fills the raw window so transparent status/navigation areas show
 * VexLife background rather than the Activity's default white window. AppFrame then applies
 * safeDrawing to the actual app shell inside that background.
 */
@Composable
@VettedWithHuman("VexLifeTheme(ThemeModeFamily)", "2026-10-29T22:46:00-07:00", "Victor Approved")
internal fun VexLifeTheme(
    themeModeFamily: ThemeModeFamily,
    content: @Composable () -> Unit,
) {
    val currentState by
        themeModeFamily
            .states
            .collectAsState()

    VexLifeTheme(
        presentation =
            themeModeFamily
                .displayStateFor(
                    currentState,
                )
                .presentation,
        content = content,
    )
}

@Composable
@VettedWithHuman("VexLifeTheme(VexThemePresentation)", "2026-10-29T22:46:00-07:00", "Victor Approved")
internal fun VexLifeTheme(
    presentation: VexThemePresentation,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme =
            presentation.materialColorScheme,
        typography = VexTypography,
    ) {
        CompositionLocalProvider(
            LocalVexThemePresentation provides
                presentation,
        ) {
            val view = LocalView.current

            if (!view.isInEditMode) {
                SideEffect {
                    val activity =
                        view.context.findActivity()
                            ?: return@SideEffect
                    val controller =
                        WindowCompat.getInsetsController(
                            activity.window,
                            view,
                        )

                    controller.isAppearanceLightStatusBars =
                        presentation.lightSystemBars
                    controller.isAppearanceLightNavigationBars =
                        presentation.lightSystemBars

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                    ) {
                        activity.window.isNavigationBarContrastEnforced =
                            false
                    }
                }
            }

            Surface(
                modifier =
                    Modifier.fillMaxSize(),
                color =
                    MaterialTheme
                        .colorScheme
                        .background,
                content = content,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
