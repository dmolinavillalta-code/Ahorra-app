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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Texto y trazos sobre las superficies con degradado de marca (marrón oscuro: contraste alto sobre el pastel). */
val OnBrand = Color(0xFF3A1C0C)

/** Degradado de marca para tarjetas destacadas y pantallas de bienvenida. [accentText] sirve para texto de énfasis sobre el fondo normal. */
@Immutable
class AhorraBrand(val gradientStart: Color, val gradientEnd: Color, val accentText: Color) {
    val gradient: Brush get() = Brush.linearGradient(listOf(gradientStart, gradientEnd))
}

val LocalBrand = staticCompositionLocalOf { AhorraBrand(Color(0xFFFFD3B0), Color(0xFFF7A56F), Color(0xFFA24A12)) }

/** Una sola identidad de color en toda la app: naranja pastel. */
private val PastelStart = Color(0xFFFFD6B8)
private val PastelEnd = Color(0xFFF6A36B)
private val Primary = Color(0xFFEE8F4B)
private val PrimaryDark = Color(0xFFF7B488)

private fun lightScheme(): ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnBrand,
    primaryContainer = Color(0xFFFFE4D1),
    onPrimaryContainer = Color(0xFF3A1C0C),
    secondary = Primary,
    secondaryContainer = Color(0xFFFFE4D1),
    onSecondaryContainer = Color(0xFF3A1C0C),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF2B1A10),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF2B1A10),
    surfaceVariant = Color(0xFFF6E7DA),
    onSurfaceVariant = Color(0xFF6B5444),
    outline = Color(0xFFB59A88),
    outlineVariant = Color(0xFFEBD9CA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFCEADD),
    surfaceContainerHigh = Color(0xFFF8E2D2),
    surfaceContainerHighest = Color(0xFFF3DACA),
)

private fun darkScheme(): ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnBrand,
    primaryContainer = Color(0xFF5A3419),
    onPrimaryContainer = Color(0xFFFFE9DA),
    secondary = PrimaryDark,
    secondaryContainer = Color(0xFF5A3419),
    onSecondaryContainer = Color(0xFFFFE9DA),
    background = Color(0xFF1B1310),
    onBackground = Color(0xFFF7EBE2),
    surface = Color(0xFF1B1310),
    onSurface = Color(0xFFF7EBE2),
    surfaceVariant = Color(0xFF3A2D26),
    onSurfaceVariant = Color(0xFFD0BBAD),
    outline = Color(0xFF8E7867),
    outlineVariant = Color(0xFF45352B),
    surfaceContainerLowest = Color(0xFF241A15),
    surfaceContainerLow = Color(0xFF2A1F19),
    surfaceContainer = Color(0xFF30241D),
    surfaceContainerHigh = Color(0xFF392B23),
    surfaceContainerHighest = Color(0xFF43332A),
)

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

/** Tema claro/oscuro del sistema con un único color de marca (naranja pastel). */
@Composable
fun AhorraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val brand = AhorraBrand(PastelStart, PastelEnd, if (darkTheme) Color(0xFFFFC9A0) else Color(0xFFA24A12))
    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme() else lightScheme(),
            typography = AhorraTypography,
            shapes = AhorraShapes,
            content = content,
        )
    }
}
