package es.ahorra.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ahorra.core.GoalSeries
import java.time.LocalDate
import kotlin.math.sin
import kotlin.random.Random

/** Anillo de progreso con degradado que se dibuja con una animación suave. */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = Color.White.copy(alpha = 0.22f),
    startColor: Color = Color.White.copy(alpha = 0.75f),
    endColor: Color = Color.White,
    durationMillis: Int = 1100,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                rotate(degrees = -90f) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(startColor, endColor)),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}

/** Barra de progreso redondeada, animada, sin "punto final" ni sorpresas visuales. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    description: String,
    trackColor: Color,
    barBrush: Brush,
    thickness: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bar",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(barBrush),
        )
    }
}

/** Cantidad que "cuenta" desde el valor anterior hasta el nuevo (en unidades menores, sin errores de coma flotante). */
@Composable
fun AnimatedMoney(
    minor: Long,
    currencyCode: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    var shown by remember { mutableLongStateOf(0L) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(minor) {
        val from = shown
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) {
            shown = from + ((minor - from).toDouble() * value).toLong()
        }
        shown = minor
    }
    Text(text = money(shown, currencyCode), style = style, color = color, modifier = modifier)
}

/** Logo de Ahorra: tres barras que crecen dentro de un círculo con degradado. */
@Composable
fun BrandMark(
    diameter: Dp,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    animateGrowth: Boolean = true,
) {
    val grow = remember { Animatable(if (animateGrowth) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateGrowth) grow.animateTo(1f, tween(1100, delayMillis = 250, easing = FastOutSlowInEasing))
    }
    Canvas(modifier.size(diameter)) {
        val w = this.size.width
        drawCircle(brush = Brush.linearGradient(listOf(startColor, endColor), Offset.Zero, Offset(w, w)))
        val bar = w * 0.13f
        val gap = w * 0.07f
        val baseY = w * 0.70f
        val heights = listOf(0.18f, 0.30f, 0.44f)
        val left = w / 2f - (1.5f * bar + gap)
        heights.forEachIndexed { i, hFraction ->
            val h = w * hFraction * grow.value
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left + i * (bar + gap), baseY - h),
                size = Size(bar, h.coerceAtLeast(0.1f)),
                cornerRadius = CornerRadius(bar / 2f),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.9f * grow.value), radius = w * 0.035f, center = Offset(w * 0.74f, w * 0.26f))
    }
}

/** Fondo con degradado y destellos que se mueven despacio. */
@Composable
fun AnimatedBackdrop(
    start: Color,
    end: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(modifier.background(Brush.linearGradient(listOf(start, end)))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            fun glow(cx: Float, cy: Float, radius: Float, alpha: Float) {
                val center = Offset(cx, cy)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), center, radius),
                    radius = radius,
                    center = center,
                )
            }
            glow(w * (0.15f + 0.20f * drift), h * (0.18f + 0.06f * drift), w * 0.60f, 0.20f)
            glow(w * (0.90f - 0.25f * drift), h * (0.55f - 0.10f * drift), w * 0.70f, 0.14f)
            glow(w * (0.30f + 0.30f * drift), h * (0.95f - 0.08f * drift), w * 0.65f, 0.16f)
        }
        content()
    }
}

private class Particle(
    val x: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val spin: Float,
    val phase: Float,
)

private val ConfettiColors = listOf(
    Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF29B6F6),
    Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFFFF7043),
)
private const val CONFETTI_SECONDS = 3.2f

/** Lluvia de confeti breve. Cada vez que [trigger] sube, se lanza otra. No captura toques. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(-1f) }
    val particles = remember(trigger) {
        val random = Random(trigger)
        List(120) {
            Particle(
                x = random.nextFloat(),
                vx = (random.nextFloat() - 0.5f) * 0.30f,
                vy = 0.20f + random.nextFloat() * 0.35f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                width = 6f + random.nextFloat() * 8f,
                height = 10f + random.nextFloat() * 10f,
                spin = (random.nextFloat() - 0.5f) * 3f,
                phase = random.nextFloat() * 6.28f,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger <= 0) {
            t = -1f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            val seconds = (withFrameNanos { it } - start) / 1_000_000_000f
            t = seconds
            if (seconds >= CONFETTI_SECONDS) break
        }
        t = -1f
    }
    if (t >= 0f) {
        Canvas(modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val fade = 1f - ((t / CONFETTI_SECONDS - 0.70f) / 0.30f).coerceIn(0f, 1f)
            particles.forEach { p ->
                val y = (-0.06f + p.vy * t + 0.35f * t * t * 0.18f) * h
                val x = (p.x + p.vx * t) * w + sin(t * 3f + p.phase) * 14f
                if (y in -40f..(h + 40f)) {
                    rotate(degrees = p.spin * t * 360f, pivot = Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = fade),
                            topLeft = Offset(x - p.width / 2f, y - p.height / 2f),
                            size = Size(p.width, p.height),
                        )
                    }
                }
            }
        }
    }
}

/** Evolución real frente al plan. Se revela de izquierda a derecha al aparecer. */
@Composable
fun EvolutionChart(
    series: GoalSeries,
    targetMinor: Long,
    description: String,
    lineColor: Color,
    planColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    val points = series.actual + series.plan
    if (points.size < 2) return
    val minDay = points.minOf { it.date.toEpochDay() }
    val maxDay = points.maxOf { it.date.toEpochDay() }.coerceAtLeast(minDay + 1)
    val maxValue = maxOf(targetMinor, points.maxOf { it.savedMinor }).coerceAtLeast(1L)
    val reveal = remember(series) { Animatable(0f) }
    LaunchedEffect(series) { reveal.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }

    Canvas(modifier.semantics { contentDescription = description }) {
        val w = this.size.width
        val h = this.size.height
        val padTop = 8.dp.toPx()
        val padBottom = 6.dp.toPx()
        fun px(date: LocalDate): Float = (date.toEpochDay() - minDay).toFloat() / (maxDay - minDay) * w
        fun py(value: Long): Float = padTop + (1f - value.toFloat() / maxValue) * (h - padTop - padBottom)

        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = padTop + (1f - f) * (h - padTop - padBottom)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }

        if (series.plan.size >= 2) {
            val plan = Path()
            series.plan.forEachIndexed { i, p ->
                if (i == 0) plan.moveTo(px(p.date), py(p.savedMinor)) else plan.lineTo(px(p.date), py(p.savedMinor))
            }
            drawPath(
                plan,
                color = planColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                ),
            )
        }

        clipRect(right = w * reveal.value) {
            val line = Path()
            series.actual.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(px(p.date), py(p.savedMinor)) else line.lineTo(px(p.date), py(p.savedMinor))
            }
            val area = Path().apply {
                addPath(line)
                lineTo(px(series.actual.last().date), h - padBottom)
                lineTo(px(series.actual.first().date), h - padBottom)
                close()
            }
            drawPath(area, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.32f), Color.Transparent), 0f, h))
            drawPath(line, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (reveal.value > 0.98f) {
            val last = series.actual.last()
            drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(px(last.date), py(last.savedMinor)))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(px(last.date), py(last.savedMinor)))
        }
    }
}
