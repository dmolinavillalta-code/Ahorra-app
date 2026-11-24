package es.ahorra.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import es.ahorra.core.Accent

private data class AccentPair(val light: Color, val dark: Color)

private fun Accent.colors(): AccentPair = when (this) {
    Accent.TEAL -> AccentPair(Color(0xFF0F766E), Color(0xFF5EEAD4))
    Accent.INDIGO -> AccentPair(Color(0xFF4F46E5), Color(0xFFA5B4FC))
    Accent.AMBER -> AccentPair(Color(0xFFB45309), Color(0xFFFBBF24))
    Accent.ROSE -> AccentPair(Color(0xFFBE123C), Color(0xFFFDA4AF))
}

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
        background = Color(0xFFFAFAF7),
        onBackground = Color(0xFF111827),
        surface = Color.White,
        onSurface = Color(0xFF111827),
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
        background = Color(0xFF0E1111),
        onBackground = Color(0xFFF3F4F6),
        surface = Color(0xFF171B1B),
        onSurface = Color(0xFFF3F4F6),
    )
}

/** Tema claro/oscuro del sistema; el acento es un cosmético que se desbloquea con la constancia. */
@Composable
fun AhorraTheme(
    accent: Accent = Accent.TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val pair = accent.colors()
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(pair.dark) else lightScheme(pair.light),
        content = content,
    )
}
