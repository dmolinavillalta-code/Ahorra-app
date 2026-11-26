package es.ahorra.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.core.Accent

/** Degradado de marca para tarjetas destacadas y pantallas de bienvenida (texto blanco encima). */
@Immutable
class AhorraBrand(val gradientStart: Color, val gradientEnd: Color) {
    val gradient: Brush get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

val LocalBrand = staticCompositionLocalOf { AhorraBrand(Color(0xFF0A5C56), Color(0xFF0E7F73)) }

private class AccentPalette(
    val lightPrimary: Color,
    val darkPrimary: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private fun Accent.palette(): AccentPalette = when (this) {
    Accent.TEAL -> AccentPalette(Color(0xFF0F766E), Color(0xFF5EEAD4), Color(0xFF0A5C56), Color(0xFF0E7F73))
    Accent.INDIGO -> AccentPalette(Color(0xFF4F46E5), Color(0xFFA5B4FC), Color(0xFF3730A3), Color(0xFF5249D9))
    Accent.AMBER -> AccentPalette(Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFF8A3B0C), Color(0xFFB45309))
    Accent.ROSE -> AccentPalette(Color(0xFFBE123C), Color(0xFFFDA4AF), Color(0xFF9F1239), Color(0xFFD61F52))
    Accent.EMERALD -> AccentPalette(Color(0xFF047857), Color(0xFF6EE7B7), Color(0xFF065F46), Color(0xFF047857))
    Accent.VIOLET -> AccentPalette(Color(0xFF7C3AED), Color(0xFFC4B5FD), Color(0xFF5B21B6), Color(0xFF7C3AED))
}

/** Color con el que se muestra cada acento en el selector de Ajustes. */
fun Accent.swatch(): Color = palette().gradientEnd

private fun lightScheme(primary: Color): ColorScheme {
    val container = lerp(Color.White, primary, 0.14f)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = Color(0xFF111827),
        secondary = primary,
        secondaryContainer = container,
        onSecondaryContainer = Color(0xFF111827),
        background = Color(0xFFF6F7F4),
        onBackground = Color(0xFF111827),
        surface = Color(0xFFF6F7F4),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFE9ECE7),
        onSurfaceVariant = Color(0xFF4B5563),
        outline = Color(0xFF9CA3AF),
        outlineVariant = Color(0xFFE2E5DF),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F3EE),
        surfaceContainer = Color(0xFFECEEE8),
        surfaceContainerHigh = Color(0xFFE7EAE3),
        surfaceContainerHighest = Color(0xFFE2E5DE),
    )
}

private fun darkScheme(primary: Color): ColorScheme {
    val container = lerp(Color(0xFF111414), primary, 0.22f)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF06201D),
        primaryContainer = container,
        onPrimaryContainer = Color(0xFFF3F4F6),
        secondary = primary,
        secondaryContainer = container,
        onSecondaryContainer = Color(0xFFF3F4F6),
        background = Color(0xFF0C0F0F),
        onBackground = Color(0xFFF3F4F6),
        surface = Color(0xFF0C0F0F),
        onSurface = Color(0xFFF3F4F6),
        surfaceVariant = Color(0xFF232928),
        onSurfaceVariant = Color(0xFFB7BEBC),
        outline = Color(0xFF6B7573),
        outlineVariant = Color(0xFF2A3130),
        surfaceContainerLowest = Color(0xFF151919),
        surfaceContainerLow = Color(0xFF181D1C),
        surfaceContainer = Color(0xFF1D2322),
        surfaceContainerHigh = Color(0xFF232928),
        surfaceContainerHighest = Color(0xFF2A3130),
    )
}

private val AhorraTypography: Typography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    )
}

private val AhorraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tema claro/oscuro del sistema; el acento es un cosmético que se desbloquea con la constancia. */
@Composable
fun AhorraTheme(
    accent: Accent = Accent.TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = accent.palette()
    val brand = AhorraBrand(palette.gradientStart, palette.gradientEnd)
    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(palette.darkPrimary) else lightScheme(palette.lightPrimary),
            typography = AhorraTypography,
            shapes = AhorraShapes,
            content = content,
        )
    }
}
