package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

fun buildThemeColorScheme(
    palette: AppColorPalette,
    isDark: Boolean,
    context: Context
): ColorScheme {
    if (palette == AppColorPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        val filledBg = if (isDark) dynamicScheme.surfaceContainer else dynamicScheme.surfaceContainerLow
        return dynamicScheme.copy(
            background = filledBg,
            surface = filledBg
        )
    }

    val primary = if (isDark) palette.darkPrimary else palette.lightPrimary
    val primaryContainer = if (isDark) palette.darkContainer else palette.lightContainer
    val filledBg = if (isDark) palette.darkFilledBackground else palette.lightFilledBackground

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF001B10),
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = Color(0xFF1B1B1F),
            secondaryContainer = primaryContainer.copy(alpha = 0.55f),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = primary,
            onTertiary = Color(0xFF000000),
            tertiaryContainer = primaryContainer,
            onTertiaryContainer = Color(0xFFFFFFFF),
            background = filledBg,
            onBackground = Color(0xFFE2E2E8),
            surface = filledBg,
            onSurface = Color(0xFFE2E2E8),
            surfaceVariant = Color(0xFF282B33),
            onSurfaceVariant = Color(0xFFC6C5D2),
            surfaceContainerLowest = Color(0xFF0D0F13),
            surfaceContainerLow = filledBg,
            surfaceContainer = Color(0xFF21252E),
            surfaceContainerHigh = Color(0xFF2A2E39),
            surfaceContainerHighest = Color(0xFF353A47),
            outline = Color(0xFF8F939E),
            outlineVariant = Color(0xFF434751)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color(0xFF051B38),
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = primaryContainer.copy(alpha = 0.45f),
            onSecondaryContainer = Color(0xFF141C2B),
            tertiary = primary,
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = primaryContainer,
            onTertiaryContainer = Color(0xFF141C2B),
            background = filledBg,
            onBackground = Color(0xFF1B1B1F),
            surface = filledBg,
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFDFE2EC),
            onSurfaceVariant = Color(0xFF44474F),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = filledBg,
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFECEAF2),
            surfaceContainerHighest = Color(0xFFE3E0EA),
            outline = Color(0xFF74777F),
            outlineVariant = Color(0xFFC4C6D0)
        )
    }
}

@Composable
fun FreshCartTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorPalette: AppColorPalette = AppColorPalette.DYNAMIC,
    fontFamily: FontFamily = FontFamily.SansSerif,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = buildThemeColorScheme(colorPalette, isDark, context)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val dynamicTypography = createExpressiveTypography(fontFamily)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}
