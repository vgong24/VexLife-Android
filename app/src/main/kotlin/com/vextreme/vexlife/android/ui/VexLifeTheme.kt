package com.vextreme.vexlife.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val DarkColors = darkColorScheme(
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

private val LightColors = lightColorScheme(
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

@Composable
fun VexLifeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VexTypography,
        content = content,
    )
}
