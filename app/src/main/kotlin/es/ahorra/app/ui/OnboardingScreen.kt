package es.ahorra.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.ahorra.app.R
import es.ahorra.app.ui.theme.LocalBrand
import es.ahorra.core.Limits
import es.ahorra.core.Settings
import kotlinx.coroutines.launch
import es.ahorra.app.ui.theme.OnBrand

private const val PAGES = 3

/**
 * Primera vez: tres pantallas de bienvenida (qué es, por qué es seguro, cómo te llamo) con la misma
 * estética que la pantalla de bloqueo. Se muestra una sola vez.
 */
@Composable
fun OnboardingScreen(
    settings: Settings,
    vm: AhorraViewModel,
    onSetupPin: () -> Unit,
) {
    val brand = LocalBrand.current
    val pager = rememberPagerState(pageCount = { PAGES })
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(settings.displayName) }

    fun finish(thenPin: Boolean) {
        vm.applySettings(settings.copy(displayName = name.trim().take(Limits.MAX_DISPLAY_NAME), welcomeSeen = true))
        if (thenPin) onSetupPin()
    }

    AnimatedBackdrop(brand.gradientStart, brand.gradientEnd, Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (pager.currentPage < PAGES - 1) {
                    TextButton(
                        onClick = { scope.launch { pager.animateScrollToPage(PAGES - 1) } },
                        colors = ButtonDefaults.textButtonColors(contentColor = OnBrand),
                    ) { Text(stringResource(R.string.onboarding_skip)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> HowItWorksPage()
                        else -> NamePage(name) { name = it.take(Limits.MAX_DISPLAY_NAME) }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(PAGES) { index ->
                    val selected = pager.currentPage == index
                    val width by animateFloatAsState(if (selected) 28f else 8f, tween(250), label = "indicator")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = width.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(OnBrand.copy(alpha = if (selected) 1f else 0.4f)),
                    )
                }
            }

            if (pager.currentPage < PAGES - 1) {
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OnBrand, contentColor = brand.gradientStart),
                ) { Text(stringResource(R.string.onboarding_next), style = MaterialTheme.typography.titleMedium) }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { finish(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OnBrand, contentColor = brand.gradientStart),
                    ) { Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.titleMedium) }
                    OutlinedButton(
                        onClick = { finish(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBrand),
                    ) { Text(stringResource(R.string.onboarding_protect)) }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    BrandMark(132.dp, Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.25f))
    Spacer(Modifier.height(28.dp))
    Text(
        stringResource(R.string.onboarding_welcome),
        color = OnBrand.copy(alpha = 0.85f),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        stringResource(R.string.app_name),
        color = OnBrand,
        fontSize = 52.sp,
        style = MaterialTheme.typography.displaySmall,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_tagline),
        color = OnBrand.copy(alpha = 0.9f),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun HowItWorksPage() {
    Text(
        stringResource(R.string.onboarding_how_title),
        color = OnBrand,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeatureRow("🎯", R.string.onboarding_f1_title, R.string.onboarding_f1_body)
        FeatureRow("🔒", R.string.onboarding_f2_title, R.string.onboarding_f2_body)
        FeatureRow("🌱", R.string.onboarding_f3_title, R.string.onboarding_f3_body)
    }
}

@Composable
private fun FeatureRow(emoji: String, title: Int, body: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.40f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(emoji, fontSize = 32.sp)
        Column {
            Text(stringResource(title), color = OnBrand, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), color = OnBrand.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NamePage(name: String, onName: (String) -> Unit) {
    Text("👋", fontSize = 64.sp)
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onboarding_name_title),
        color = OnBrand,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.onboarding_name_body),
        color = OnBrand.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        singleLine = true,
        label = { Text(stringResource(R.string.onboarding_name_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnBrand,
            unfocusedTextColor = OnBrand,
            focusedBorderColor = OnBrand,
            unfocusedBorderColor = OnBrand.copy(alpha = 0.55f),
            focusedLabelColor = OnBrand,
            unfocusedLabelColor = OnBrand.copy(alpha = 0.75f),
            cursorColor = OnBrand,
        ),
    )
}
