package es.ahorra.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import es.ahorra.app.ui.AhorraRoot
import kotlinx.coroutines.launch

// FragmentActivity (que extiende ComponentActivity) es necesaria para el diálogo de huella del sistema.
class MainActivity : FragmentActivity() {

    private val lock get() = (application as AhorraApp).lockController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // "Ocultar contenido": impide capturas y oculta la vista previa en apps recientes.
        lifecycleScope.launch {
            lock.config.collect { config ->
                if (config.hideContent) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        setContent { AhorraRoot() }
    }

    override fun onStart() {
        super.onStart()
        lock.onStarted(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        super.onStop()
        // Girar la pantalla recrea la actividad: no cuenta como "salir de la app".
        if (!isChangingConfigurations) lock.onStopped(SystemClock.elapsedRealtime())
    }
}
