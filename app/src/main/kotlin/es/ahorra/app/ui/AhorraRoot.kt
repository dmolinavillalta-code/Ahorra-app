package es.ahorra.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ahorra.app.R
import es.ahorra.app.data.RepoState
import es.ahorra.app.ui.theme.AhorraTheme
import es.ahorra.core.AppState

private object Routes {
    const val HOME = "home"
    const val NEW_GOAL = "new"
    const val SETTINGS = "settings"
    const val GOAL = "goal/{id}"
    fun goal(id: String) = "goal/$id"
}

@Composable
fun AhorraRoot(vm: AhorraViewModel = viewModel()) {
    val repoState by vm.repoState.collectAsStateWithLifecycle()
    val accent = (repoState as? RepoState.Ready)?.data?.settings?.accent ?: es.ahorra.core.Accent.TEAL

    AhorraTheme(accent = accent) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val s = repoState) {
                RepoState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                RepoState.Unreadable -> UnreadableScreen()
                is RepoState.Ready -> AppNavigation(vm)
            }
        }
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
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val data = readyData(vm) ?: return@composable
            HomeScreen(
                data = data,
                onOpenGoal = { nav.navigate(Routes.goal(it)) },
                onNewGoal = { nav.navigate(Routes.NEW_GOAL) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.NEW_GOAL) { NewGoalScreen(vm) { nav.popBackStack() } }
        composable(Routes.GOAL) { entry ->
            val data = readyData(vm) ?: return@composable
            val id = entry.arguments?.getString("id").orEmpty()
            GoalDetailScreen(data, vm, id) { nav.popBackStack() }
        }
        composable(Routes.SETTINGS) {
            val data = readyData(vm) ?: return@composable
            SettingsScreen(
                data = data,
                vm = vm,
                onBack = { nav.popBackStack() },
                onWiped = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}

/**
 * Hay un archivo de datos, pero no se puede descifrar (por ejemplo, tras restaurar una copia en otro móvil,
 * donde la clave del Keystore no existe). La app NO lo sobrescribe: se ofrece una salida explícita.
 */
@Composable
private fun UnreadableScreen(vm: AhorraViewModel = viewModel()) {
    var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.unreadable_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.unreadable_body), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Button(onClick = { confirm = true }) {
            Text(stringResource(R.string.unreadable_reset))
        }
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
