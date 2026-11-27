package es.ahorra.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.OnBrand
import es.ahorra.core.Rarity
import es.ahorra.core.Species
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Color de la rareza. Nunca se usa solo: siempre va acompañado del nombre de la rareza. */
fun Rarity.color(): Color = when (this) {
    Rarity.COMMON -> Color(0xFFD8BFA8)
    Rarity.RARE -> Color(0xFF7FB6E8)
    Rarity.EPIC -> Color(0xFFB58BEA)
    Rarity.LEGENDARY -> Color(0xFFF2C14E)
}

/** Mascota con aura de su rareza y una pequeña animación de respiración. */
@Composable
fun PetAvatar(
    species: Species,
    size: Dp,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    discovered: Boolean = true,
) {
    val aura = species.rarity.color()
    val transition = rememberInfiniteTransition(label = "pet")
    val bob by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "spin",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(listOf(aura.copy(alpha = if (discovered) 0.85f else 0.35f), aura.copy(alpha = 0.0f)), c, r),
                radius = r,
                center = c,
            )
            if (species.rarity == Rarity.LEGENDARY && discovered && animate) {
                rotate(spin, c) {
                    repeat(12) { i ->
                        val a = (i * 30f) * (PI.toFloat() / 180f)
                        drawLine(
                            aura.copy(alpha = 0.55f),
                            Offset(c.x + cos(a) * r * 0.62f, c.y + sin(a) * r * 0.62f),
                            Offset(c.x + cos(a) * r * 0.95f, c.y + sin(a) * r * 0.95f),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
        Text(
            text = if (discovered) species.emoji() else "❔",
            fontSize = (size.value * 0.52f).sp,
            modifier = Modifier.offset(y = (if (animate && discovered) -4f * bob else 0f).dp),
        )
    }
}

@Composable
fun RarityChip(rarity: Rarity, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(rarity.color().copy(alpha = 0.55f))
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(rarity.labelRes()),
            color = OnBrand,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ---- Huevo ---------------------------------------------------------------------------------------------------

private fun eggPath(w: Float, h: Float): Path = Path().apply {
    // Silueta de huevo: más ancha abajo que arriba.
    moveTo(w * 0.5f, 0f)
    cubicTo(w * 0.86f, 0f, w * 1.0f, h * 0.62f, w * 0.5f, h)
    cubicTo(w * 0.0f, h * 0.62f, w * 0.14f, 0f, w * 0.5f, 0f)
    close()
}

private fun DrawScope.drawEggBody(crack: Float) {
    val w = size.width
    val h = size.height
    val path = eggPath(w, h)
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFF6EC), Color(0xFFFFD9BC), Color(0xFFF7B98E)), 0f, h))
    // manchas
    listOf(
        Triple(0.34f, 0.40f, 0.045f), Triple(0.62f, 0.32f, 0.035f), Triple(0.55f, 0.62f, 0.05f),
        Triple(0.30f, 0.70f, 0.035f), Triple(0.70f, 0.74f, 0.03f),
    ).forEach { (x, y, r) -> drawCircle(Color(0xFFE88F55).copy(alpha = 0.55f), r * w, Offset(w * x, h * y)) }
    drawPath(path, OnBrand.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    // grietas que van apareciendo con [crack] (0..1)
    if (crack > 0f) {
        val zig = listOf(0.08f to 0.50f, 0.28f to 0.44f, 0.40f to 0.56f, 0.54f to 0.42f, 0.68f to 0.55f, 0.82f to 0.46f, 0.94f to 0.52f)
        val visible = (zig.size * crack.coerceIn(0f, 1f)).toInt().coerceAtLeast(1)
        val line = Path()
        zig.take(visible).forEachIndexed { i, (x, y) -> if (i == 0) line.moveTo(w * x, h * y) else line.lineTo(w * x, h * y) }
        drawPath(line, OnBrand, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Huevo que se agita (más cuanto mayor es [intensity]) y se va agrietando. */
@Composable
fun EggView(intensity: Float, modifier: Modifier = Modifier, width: Dp = 150.dp) {
    val t = rememberInfiniteTransition(label = "egg")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble",
    )
    val angle = (phase * 2f - 1f) * 14f * intensity
    Canvas(modifier.size(width, width * 1.28f)) {
        rotate(angle, Offset(size.width / 2f, size.height)) { drawEggBody(crack = if (intensity > 0.35f) (intensity - 0.35f) / 0.65f else 0f) }
    }
}

/** El huevo se parte en dos mitades que se separan; la mascota aparece con un salto. */
@Composable
fun HatchBurst(species: Species, onShown: () -> Unit, modifier: Modifier = Modifier) {
    val open = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        open.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        onShown()
    }
    Box(modifier.size(220.dp, 260.dp), contentAlignment = Alignment.Center) {
        if (pop.value > 0f) {
            Box(Modifier.scale(0.3f + 0.7f * pop.value)) { PetAvatar(species, 190.dp) }
        }
        if (open.value < 1f) {
            val o = open.value
            Canvas(Modifier.size(150.dp, 192.dp)) {
                val split = size.height * 0.52f
                clipRect(bottom = split) {
                    translate(left = -40f * o * density, top = -70f * o * density) {
                        rotate(-28f * o, Offset(size.width / 2f, split)) { drawEggBody(1f) }
                    }
                }
                clipRect(top = split) {
                    translate(left = 36f * o * density, top = 60f * o * density) {
                        rotate(24f * o, Offset(size.width / 2f, split)) { drawEggBody(1f) }
                    }
                }
            }
        }
    }
}
