package com.aifinance.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class IcokieSpacing(
    val pagePadding: Dp = 16.dp,
    val cardSpacing: Dp = 12.dp,
    val cardPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 24.dp,
    val itemSpacing: Dp = 8.dp,
    val elementSpacing: Dp = 4.dp,
    val largeSpacing: Dp = 32.dp,
    val extraLargeSpacing: Dp = 48.dp
)

@Immutable
data class IcokieElevation(
    val cardElevation: Dp = 4.dp,
    val elevatedCardElevation: Dp = 8.dp,
    val pressedCardElevation: Dp = 2.dp,
    val floatingActionButtonElevation: Dp = 6.dp,
    val dialogElevation: Dp = 8.dp,
    val bottomSheetElevation: Dp = 8.dp
)

val LocalIcokieSpacing = staticCompositionLocalOf { IcokieSpacing() }
val LocalIcokieElevation = staticCompositionLocalOf { IcokieElevation() }

val IcokieShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandSecondary,
    onSecondary = OnPrimary,
    secondaryContainer = BrandSecondaryLight,
    onSecondaryContainer = BrandSecondaryDark,
    tertiary = IncomeDefault,
    onTertiary = OnPrimary,
    tertiaryContainer = IncomeLight,
    onTertiaryContainer = IncomeDark,
    error = ErrorDefault,
    onError = OnPrimary,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = BackgroundPrimary,
    onBackground = OnSurfacePrimary,
    surface = SurfacePrimary,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = OnSurfaceSecondary,
    outline = BorderDefault,
    outlineVariant = BorderSubtle,
    scrim = OnSurfacePrimary.copy(alpha = 0.6f),
    inverseSurface = OnSurfacePrimary,
    inverseOnSurface = BackgroundPrimary,
    inversePrimary = BrandPrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = BrandPrimaryDark,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = BrandPrimaryLight,
    secondary = BrandSecondaryLight,
    onSecondary = BrandSecondaryDark,
    secondaryContainer = BrandSecondaryDark,
    onSecondaryContainer = BrandSecondaryLight,
    tertiary = IncomeLight,
    onTertiary = IncomeDark,
    tertiaryContainer = IncomeDark,
    onTertiaryContainer = IncomeLight,
    error = ErrorLight,
    onError = ErrorDark,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    scrim = Color(0xFF000000).copy(alpha = 0.7f),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = BrandPrimary
)

@Composable
fun IcokieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalIcokieSpacing provides IcokieSpacing(),
        LocalIcokieElevation provides IcokieElevation()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IcokieTypography,
            shapes = IcokieShapes,
            content = content
        )
    }
}

object IcokieTheme {
    val spacing: IcokieSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalIcokieSpacing.current

    val elevation: IcokieElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalIcokieElevation.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}

@Deprecated("Use IcokieTheme instead", ReplaceWith("IcokieTheme"))
@Composable
fun AiFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    IcokieTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
