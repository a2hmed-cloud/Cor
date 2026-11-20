package com.example.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppThemeMode
import com.example.core.i18n.ArabicStrings
import com.example.core.i18n.EnglishStrings
import com.example.core.i18n.LocalCreovaStrings

@Immutable
data class CreovaGlassPalette(
    val isDark: Boolean,
    val canvas: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val glassSpecular: Color,
    val ambientViolet: Color,
    val ambientCyan: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val violetAccent: Color,
    val cyanAccent: Color
)

private val DarkGlassPalette = CreovaGlassPalette(
    isDark = true,
    canvas = DarkCanvas,
    glassSurface = DarkGlassSurface.copy(alpha = 0.78f),
    glassElevated = DarkGlassElevated.copy(alpha = 0.86f),
    glassBorderStart = Color(0x4DFFFFFF),
    glassBorderEnd = Color(0x2E22D3EE),
    glassSpecular = Color(0x1AFFFFFF),
    ambientViolet = LuminousViolet.copy(alpha = 0.22f),
    ambientCyan = ElectricCyan.copy(alpha = 0.16f),
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    violetAccent = LuminousVioletBright,
    cyanAccent = ElectricCyan
)

private val LightGlassPalette = CreovaGlassPalette(
    isDark = false,
    canvas = LightCanvas,
    glassSurface = LightGlassSurface.copy(alpha = 0.84f),
    glassElevated = LightGlassElevated.copy(alpha = 0.94f),
    glassBorderStart = Color(0xCCFFFFFF),
    glassBorderEnd = Color(0x386D28D9),
    glassSpecular = Color(0xCCFFFFFF),
    ambientViolet = LuminousViolet.copy(alpha = 0.12f),
    ambientCyan = ElectricCyan.copy(alpha = 0.11f),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    violetAccent = DeepViolet,
    cyanAccent = DeepTealCyan
)

val LocalCreovaGlass = staticCompositionLocalOf { DarkGlassPalette }
val LocalReducedMotion = staticCompositionLocalOf { false }

private val CreovaDarkColorScheme = darkColorScheme(
    primary = LuminousVioletBright,
    onPrimary = Color(0xFF140A2E),
    primaryContainer = Color(0xFF2D1B69),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF04222B),
    secondaryContainer = Color(0xFF0E3A47),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = EmeraldFree,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkGlassSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkGlassElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkGlassBorder,
    error = RosePaid,
    onError = Color.White
)

private val CreovaLightColorScheme = lightColorScheme(
    primary = DeepViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = DeepTealCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = EmeraldFree,
    background = LightCanvas,
    onBackground = LightTextPrimary,
    surface = LightGlassSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceSoft,
    onSurfaceVariant = LightTextSecondary,
    outline = LightGlassBorder,
    error = RosePaid,
    onError = Color.White
)

val CreovaShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun CreovaTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.AR,
    userReducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val systemAnimatorScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    } catch (_: Exception) {
        1f
    }
    val reduceMotion = userReducedMotion || systemAnimatorScale == 0f

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    val glassPalette = if (useDark) DarkGlassPalette else LightGlassPalette
    val colorScheme = if (useDark) CreovaDarkColorScheme else CreovaLightColorScheme
    val strings = if (language == AppLanguage.AR) ArabicStrings else EnglishStrings
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalCreovaGlass provides glassPalette,
        LocalCreovaStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        LocalReducedMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CreovaTypography,
            shapes = CreovaShapes,
            content = content
        )
    }
}
