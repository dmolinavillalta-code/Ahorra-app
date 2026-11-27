package es.ahorra.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.ahorra.app.R
import es.ahorra.app.data.Gate
import es.ahorra.app.data.RepoState
import es.ahorra.app.ui.theme.AhorraTheme
import es.ahorra.core.AppState

private object Routes {
    const val MAIN = "main"
    const val NEW_GOAL = "new"
    const val GOAL = "goal/{id}"
    const val PIN = "pin/{mode}"
    fun goal(id: String) = "goal/$id"
    fun pin(mode: PinMode) = "pin/${mode.name}"
}

@Composable
fun AhorraRoot(vm: AhorraViewModel = viewModel()) {
    val repoState by vm.repoState.collectAsStateWithLifecycle()
    val gate by vm.gate.collectAsStateWithLifecycle()
    val lockConfig by vm.lockConfig.collectAsStateWithLifecycle()
    val name = (repoState as? RepoState.Ready)?.data?.settings?.displayName.orEmpty()

    AhorraTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // El bloqueo manda sobre todo lo demás: mientras esté cerrado no se compone ningún dato.
            Crossfade(targetState = gate, animationSpec = tween(450), label = "gate") { g ->
                when (g) {
                    Gate.Loading -> Splash()
                    Gate.Broken -> UnreadableScreen(vm, locked = true)
                    Gate.Locked -> LockScreen(vm, name, lockConfig)
                    Gate.Open -> when (val s = repoState) {
                        RepoState.Loading -> Splash()
                        RepoState.Unreadable -> UnreadableScreen(vm, locked = false)
                        is RepoState.Ready -> if (!s.data.settings.welcomeSeen) {
                            WelcomeFlow(vm, s.data)
                        } else {
                            AppNavigation(vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Primera vez: bienvenida y, si la persona quiere, creación del PIN antes de entrar. */
@Composable
private fun WelcomeFlow(vm: AhorraViewModel, data: AppState) {
    var settingPin by rememberSaveable { mutableStateOf(false) }
    if (settingPin) {
        SetPinScreen(vm, PinMode.CREATE, onDone = { settingPin = false }, onCancel = { settingPin = false })
    } else {
        OnboardingScreen(data.settings, vm, onSetupPin = { settingPin = true })
    }
}

/** Estado actual de los datos; null si en este instante no están listos (la raíz mostrará otra pantalla). */
@Composable
private fun readyData(vm: AhorraViewModel): AppState? {
    val state by vm.repoState.collectAsStateWithLifecycle()
    return (state as? RepoState.Ready)?.data
}

// El grafo de navegación solo captura `nav` y `vm` (estables): si capturase los datos, se reconstruiría
// con cada cambio. Cada destino lee el estado por su cuenta.
@Composable
private fun AppNavigation(vm: AhorraViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            val data = readyData(vm) ?: return@composable
            MainTabs(
                data = data,
                vm = vm,
                onOpenGoal = { nav.navigate(Routes.goal(it)) },
                onNewGoal = { nav.navigate(Routes.NEW_GOAL) },
                onPinMode = { nav.navigate(Routes.pin(it)) },
            )
        }
        composable(Routes.NEW_GOAL) { NewGoalScreen(vm) { nav.popBackStack() } }
        composable(Routes.GOAL) { entry ->
            val data = readyData(vm) ?: return@composable
            val id = entry.arguments?.getString("id").orEmpty()
            GoalDetailScreen(data, vm, id) { nav.popBackStack() }
        }
        composable(
            Routes.PIN,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments?.getString("mode")?.let { name -> PinMode.entries.firstOrNull { it.name == name } }
                ?: PinMode.CREATE
            SetPinScreen(vm, mode, onDone = { nav.popBackStack() }, onCancel = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainTabs(
    data: AppState,
    vm: AhorraViewModel,
    onOpenGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    onPinMode: (PinMode) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    androidx.compose.material3.Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                listOf(
                    "🏠" to R.string.tab_today,
                    "🐣" to R.string.tab_pets,
                    "🤝" to R.string.tab_friends,
                    "🏅" to R.string.tab_achievements,
                    "⚙️" to R.string.tab_settings,
                ).forEachIndexed { index, (emoji, label) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(emoji) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Crossfade(targetState = tab, animationSpec = tween(250), label = "tabs") { current ->
                when (current) {
                    0 -> HomeScreen(data, onOpenGoal, onNewGoal)
                    1 -> PetsScreen(data, vm)
                    2 -> FriendsScreen(data, vm)
                    3 -> AchievementsScreen(data, vm)
                    else -> SettingsScreen(data, vm, onPinMode, onWiped = { tab = 0 })
                }
            }
        }
    }
}

/**
 * Hay un archivo, pero no se puede descifrar (por ejemplo, tras restaurar una copia en otro móvil, donde la
 * clave del Keystore no existe). La app NO lo sobrescribe: se ofrece una salida explícita.
 */
@Composable
private fun UnreadableScreen(vm: AhorraViewModel, locked: Boolean) {
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.unreadable_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(if (locked) R.string.unreadable_lock_body else R.string.unreadable_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = { confirm = true }) { Text(stringResource(R.string.unreadable_reset)) }
    }
    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_wipe_title),
            text = stringResource(R.string.settings_wipe_body),
            confirmLabel = stringResource(R.string.settings_wipe_confirm),
            onDismiss = { confirm = false },
            onConfirm = { confirm = false; vm.wipe {} },
        )
    }
}
